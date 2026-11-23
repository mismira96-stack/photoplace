package com.example.gallerysorter;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.view.animation.DecelerateInterpolator;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Restores browser position after rebuilding; holds no previous root, bitmaps or media list. */
final class MemoryBrowserScrollController {
    private WeakReference<ScrollView> active = new WeakReference<>(null);
    private MemoryScrollAnchor pending;

    static void markPlace(View card, String key) { card.setTag(new CardKey("place|" + key)); }
    static void markCollection(View card, String key) { card.setTag(new CardKey("collection|" + key)); }
    void capturePlace(String key, boolean albums, String query) { capture("place|" + key, albums, query); }
    void captureCollection(String key, boolean albums, String query) { capture("collection|" + key, albums, query); }

    void capturePosition(boolean albums, String query) { capture("", albums, query); }

    private void capture(String key, boolean albums, String query) {
        ScrollView scroll = active.get();
        if (scroll == null || !scroll.isAttachedToWindow()) return;
        View card = find(scroll, key);
        int top = card == null ? 0 : relativeTop(card, scroll);
        pending = new MemoryScrollAnchor(key, MemoryScrollAnchor.scope(albums, query), top, scroll.getScrollY());
    }

    void bound(ScrollView scroll, boolean albums, String query) {
        active = new WeakReference<>(scroll);
        MemoryScrollAnchor restore = pending;
        pending = null;
        if (restore == null || !restore.matches(albums, query)) return;
        scroll.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                if (scroll.getViewTreeObserver().isAlive()) scroll.getViewTreeObserver().removeOnPreDrawListener(this);
                if (active.get() != scroll || !scroll.isAttachedToWindow() || scroll.getChildCount() == 0) return true;
                View card = find(scroll, restore.key);
                Integer cardTop = card == null ? null : relativeTop(card, scroll) + scroll.getScrollY();
                int maximum = scroll.getChildAt(0).getHeight() + scroll.getPaddingTop() + scroll.getPaddingBottom() - scroll.getHeight();
                scroll.scrollTo(0, restore.restoreY(cardTop, maximum));
                return true;
            }
        });
    }

    static void aroundGridChange(ScrollView scroll, float focusX, float focusY, Runnable render) {
        int[] origin = new int[2];
        scroll.getLocationOnScreen(origin);
        View before = cardAt(scroll, origin[0] + (int) focusX, origin[1] + (int) focusY, new Rect());
        String key = before == null ? "" : ((CardKey) before.getTag()).key;
        int offset = before == null ? 0 : relativeTop(before, scroll);
        MemoryScrollAnchor anchor = new MemoryScrollAnchor(key, "", offset, scroll.getScrollY());
        Map<String, VisibleCard> positions = new HashMap<>();
        captureVisible(scroll, positions, new Rect(), new int[2]);
        render.run();
        scroll.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                scroll.getViewTreeObserver().removeOnPreDrawListener(this);
                if (!scroll.isAttachedToWindow() || scroll.getChildCount() == 0) return true;
                View after = find(scroll, anchor.key);
                Integer top = after == null ? null : relativeTop(after, scroll) + scroll.getScrollY();
                int maximum = scroll.getChildAt(0).getHeight() + scroll.getPaddingTop()
                        + scroll.getPaddingBottom() - scroll.getHeight();
                scroll.scrollTo(0, anchor.restoreY(top, maximum));
                if (!positions.isEmpty()) restoreVisible(scroll, positions, new Rect(), new int[2],
                        ValueAnimator.areAnimatorsEnabled());
                return true;
            }
        });
    }

    private static void captureVisible(View view, Map<String, VisibleCard> positions, Rect visible, int[] xy) {
        if (!view.getGlobalVisibleRect(visible)) return;
        if (view.getTag() instanceof CardKey) {
            view.getLocationOnScreen(xy);
            positions.put(((CardKey) view.getTag()).key, new VisibleCard(xy[0], xy[1], findCover(view)));
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) captureVisible(group.getChildAt(i), positions, visible, xy);
        }
    }

    private static void restoreVisible(View view, Map<String, VisibleCard> positions, Rect visible,
                                       int[] xy, boolean animate) {
        if (!view.getGlobalVisibleRect(visible)) return;
        if (view.getTag() instanceof CardKey) {
            VisibleCard previous = positions.get(((CardKey) view.getTag()).key);
            if (previous == null) return;
            previous.reuseCover(findCover(view));
            if (!animate) return;
            view.getLocationOnScreen(xy);
            float dx = previous.x - xy[0];
            float dy = previous.y - xy[1];
            float limit = CleanupUiStyle.dp(view.getContext(), 18);
            view.setTranslationX(Math.max(-limit, Math.min(limit, dx)));
            view.setTranslationY(Math.max(-limit, Math.min(limit, dy)));
            view.animate().translationX(0f).translationY(0f).setDuration(140)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) restoreVisible(group.getChildAt(i), positions, visible, xy, animate);
        }
    }

    private static ImageView findCover(View card) {
        if (card instanceof ImageView) return (ImageView) card;
        if (!(card instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) card;
        if (group.getChildCount() == 0) return null;
        return findCover(group.getChildAt(0));
    }

    private static final class VisibleCard {
        final int x, y;
        final Object coverKey;
        final Bitmap cover;

        VisibleCard(int x, int y, ImageView image) {
            this.x = x;
            this.y = y;
            coverKey = image == null ? null : image.getTag();
            cover = image != null && image.getDrawable() instanceof BitmapDrawable
                    ? ((BitmapDrawable) image.getDrawable()).getBitmap() : null;
        }

        void reuseCover(ImageView image) {
            if (image != null && cover != null && !cover.isRecycled() && coverKey != null
                    && Objects.equals(coverKey, image.getTag()) && !(image.getDrawable() instanceof BitmapDrawable)) {
                image.setImageBitmap(cover);
            }
        }
    }

    private static View cardAt(View view, int x, int y, Rect visible) {
        if (!view.getGlobalVisibleRect(visible) || !visible.contains(x, y)) return null;
        if (view.getTag() instanceof CardKey) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View hit = cardAt(group.getChildAt(i), x, y, visible);
                if (hit != null) return hit;
            }
        }
        return null;
    }

    void clear() { pending = null; active.clear(); }

    private static int relativeTop(View child, View viewport) {
        int[] childPosition = new int[2], viewportPosition = new int[2];
        child.getLocationInWindow(childPosition);
        viewport.getLocationInWindow(viewportPosition);
        return childPosition[1] - viewportPosition[1];
    }

    private static View find(View view, String key) {
        Object tag = view.getTag();
        if (tag instanceof CardKey && ((CardKey) tag).key.equals(key)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View found = find(parent.getChildAt(i), key);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static final class CardKey {
        final String key;
        CardKey(String key) { this.key = key; }
    }
}
