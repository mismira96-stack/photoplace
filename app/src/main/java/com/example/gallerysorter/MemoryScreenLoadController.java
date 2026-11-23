package com.example.gallerysorter;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/** Shared async screen loading with bounded revision-scoped results and origin/lifecycle guards. */
final class MemoryScreenLoadController<T> {
    private final Activity activity;
    private final String loadingText, logTag;
    private final HomeRevisionReader revisions;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final DelayedLoadingNotice delayedNotice = new DelayedLoadingNotice(new DelayedLoadingNotice.Scheduler() {
        @Override public void after(long delay, Runnable action) { main.postDelayed(action, delay); }
        @Override public void remove(Runnable action) { main.removeCallbacks(action); }
    }, 250);
    private final MemoryDetailLoadSession<T> requests;
    private View origin, notice;
    private PendingLoad<T> pendingLoad;
    private ViewGroup noticeParent;
    private View noticeViewport;
    private boolean closed;

    MemoryScreenLoadController(Activity activity, Predicate<T> reusable, String loadingText, String logTag) {
        this(activity, reusable, loadingText, logTag, 1, 1, value -> 1);
    }

    MemoryScreenLoadController(Activity activity, Predicate<T> reusable, String loadingText, String logTag,
                               int maxEntries, long maxWeight, ToLongFunction<T> weight) {
        this.activity = activity;
        this.loadingText = loadingText;
        this.logTag = logTag;
        requests = new MemoryDetailLoadSession<>(reusable, maxEntries, maxWeight, weight);
        revisions = new HomeRevisionReader(activity.getApplicationContext());
    }

    void load(String key, Callable<T> source, BiConsumer<T, String> ready,
              Consumer<String> error) {
        load(key, ignored -> source.call(), new PendingLoad<>(null, ready, error, true));
    }

    void load(String key, MemoryDetailLoadSession.RevisionSource<T> source, BiConsumer<T, String> ready,
              Consumer<String> error) {
        load(key, source, new PendingLoad<>(null, ready, error, true));
    }

    void loadQuietly(String key, Callable<T> source, BiConsumer<T, String> ready) {
        load(key, ignored -> source.call(), new PendingLoad<>(null, ready, ignored -> { }, false));
    }

    void preload(String key, Callable<T> source, BiConsumer<T, String> ready) {
        load(key, ignored -> source.call(), new PendingLoad<>(key, ready, ignored -> { }, false));
    }

    boolean isPreloading() { return pendingLoad != null && pendingLoad.preloadKey != null; }

    T getCached(String key) {
        return requests.get(key);
    }

    boolean promotePreload(String key, BiConsumer<T, String> ready, Consumer<String> error) {
        PendingLoad<T> current = pendingLoad;
        if (current == null) return false;
        boolean alreadyForeground = current.foreground;
        if (!current.promote(key, ready, error)) return false;
        if (!alreadyForeground) {
            ViewGroup content = activity.findViewById(android.R.id.content);
            delayedNotice.start(() -> { if (pendingLoad == current) showNotice(content); });
        }
        return true;
    }

    private void load(String key, MemoryDetailLoadSession.RevisionSource<T> source, PendingLoad<T> callbacks) {
        if (closed) return;
        cancel();
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View requestedOrigin = content.getChildAt(0);
        origin = requestedOrigin;
        pendingLoad = callbacks;
        origin.addOnAttachStateChangeListener(detachment);
        long request = requests.begin();
        if (callbacks.foreground) {
            delayedNotice.start(() -> { if (canPublish(request, requestedOrigin)) showNotice(content); });
        }
        long started = SystemClock.elapsedRealtime();
        worker.execute(() -> {
            try {
                MemoryDetailLoadSession.Result<T> result = requests.read(request, key, revisions::read, source);
                long resolved = SystemClock.elapsedRealtime();
                main.post(() -> {
                    if (!canPublish(request, requestedOrigin)) return;
                    clearNotice();
                    if (result == null || result.value == null) {
                        callbacks.error.accept("기록을 찾지 못했어요.");
                        return;
                    }
                    if (callbacks.preloadKey != null && callbacks.foreground && result.sourceChanged) {
                        load(key, source, callbacks.ready, callbacks.error);
                        return;
                    }
                    long bindingStarted = SystemClock.elapsedRealtime();
                    callbacks.ready.accept(result.value, result.revision);
                    Log.i(logTag, "sourceMs=" + (resolved - started)
                            + " uiWaitMs=" + (bindingStarted - resolved)
                            + " bindMs=" + (SystemClock.elapsedRealtime() - bindingStarted)
                            + " reused=" + result.reused
                            + " joinedPreload=" + (callbacks.preloadKey != null && callbacks.foreground));
                });
            } catch (Exception failure) {
                main.post(() -> {
                    if (!canPublish(request, requestedOrigin)) return;
                    clearNotice();
                    Log.w(logTag, "screen load failed", failure);
                    callbacks.error.accept("기록을 불러오지 못했어요.");
                });
            }
        });
    }

    private boolean canPublish(long request, View requestedOrigin) {
        return !closed && requests.current(request) && origin == requestedOrigin
                && requestedOrigin.isAttachedToWindow();
    }

    private final View.OnAttachStateChangeListener detachment = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View view) { }
        @Override public void onViewDetachedFromWindow(View view) { if (origin == view) cancel(); }
    };

    private void showNotice(ViewGroup content) {
        if (!(content instanceof FrameLayout)) return;
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(244, 242, 255));
        background.setCornerRadius(dp(14));
        row.setBackground(background);
        row.addView(new ProgressBar(activity), new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView label = new TextView(activity);
        label.setText(loadingText);
        label.setTextSize(14);
        label.setTextColor(Color.rgb(64, 53, 106));
        label.setPadding(dp(12), 0, 0, 0);
        row.addView(label);
        row.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1, -2, Gravity.BOTTOM | Gravity.LEFT);
        noticeParent = content;
        noticeViewport = origin;
        if (origin instanceof ViewGroup && ((ViewGroup) origin).getChildCount() > 0
                && ((ViewGroup) origin).getChildAt(0) instanceof ScrollView) {
            noticeViewport = ((ViewGroup) origin).getChildAt(0);
        }
        content.addOnLayoutChangeListener(noticeLayoutChanged);
        if (noticeViewport != null) noticeViewport.addOnLayoutChangeListener(noticeLayoutChanged);
        content.addView(row, params);
        notice = row;
        positionNotice();
    }

    private final View.OnLayoutChangeListener noticeLayoutChanged = (view, l, t, r, b, oldL, oldT, oldR, oldB) -> positionNotice();

    private void positionNotice() {
        if (notice == null || noticeParent == null || noticeViewport == null) return;
        int[] parentLocation = new int[2], viewportLocation = new int[2];
        noticeParent.getLocationInWindow(parentLocation);
        noticeViewport.getLocationInWindow(viewportLocation);
        MemoryLoadingNoticeLayout layout = MemoryLoadingNoticeLayout.calculate(
                noticeParent.getWidth(), noticeParent.getHeight(), viewportLocation[0] - parentLocation[0],
                noticeViewport.getWidth(), viewportLocation[1] - parentLocation[1] + noticeViewport.getHeight(),
                dp(600), dp(18), dp(12));
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) notice.getLayoutParams();
        if (params.width != layout.width || params.leftMargin != layout.leftMargin || params.bottomMargin != layout.bottomMargin) {
            params.width = layout.width;
            params.leftMargin = layout.leftMargin;
            params.bottomMargin = layout.bottomMargin;
            notice.setLayoutParams(params);
        }
    }

    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }

    private void clearNotice() {
        delayedNotice.cancel();
        pendingLoad = null;
        if (noticeParent != null) noticeParent.removeOnLayoutChangeListener(noticeLayoutChanged);
        if (noticeViewport != null) noticeViewport.removeOnLayoutChangeListener(noticeLayoutChanged);
        noticeParent = null;
        noticeViewport = null;
        if (origin != null) origin.removeOnAttachStateChangeListener(detachment);
        origin = null;
        if (notice != null && notice.getParent() instanceof ViewGroup) {
            ((ViewGroup) notice.getParent()).removeView(notice);
        }
        notice = null;
    }

    void cancel() { requests.cancel(); clearNotice(); }
    boolean isPending() { return origin != null; }

    void close() {
        closed = true;
        cancel();
        requests.clear();
        worker.shutdownNow();
    }

    static final class PendingLoad<T> {
        final String preloadKey;
        BiConsumer<T, String> ready;
        Consumer<String> error;
        boolean foreground;

        PendingLoad(String preloadKey, BiConsumer<T, String> ready, Consumer<String> error, boolean foreground) {
            this.preloadKey = preloadKey;
            this.ready = ready;
            this.error = error;
            this.foreground = foreground;
        }

        boolean promote(String key, BiConsumer<T, String> ready, Consumer<String> error) {
            if (!key.equals(preloadKey)) return false;
            this.ready = ready;
            this.error = error;
            foreground = true;
            return true;
        }
    }
}
