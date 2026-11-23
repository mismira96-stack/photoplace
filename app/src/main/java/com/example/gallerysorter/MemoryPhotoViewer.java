package com.example.gallerysorter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * In-app Memory Viewer V2.1 (Samsung Gallery-inspired clean layout):
 * - Fullscreen immersive photo & inline video viewer with one-tap Chrome toggle
 * - Top bar: Back (‹), Date & Place heading + capture time, Counter pill, and Top Trash Icon (휴지통)
 * - Bottom Chrome: Inline video controls (when viewing a video) + Mini filmstrip (when count > 1)
 * - No cluttered bottom buttons (no Gallery/External open, no Prev/Next buttons, no Memo editor in viewer)
 * - Pinch zoom (1.0x~4.0x), double-tap zoom (2.5x), edge-clamped pan, horizontal swipe, and swipe-to-dismiss
 */
final class MemoryPhotoViewer {
    interface Host {
        Context context();
        default String managementMemoryKey() {
            return "";
        }
        int dp(int value);
        void loadThumbnail(ImageView target, Uri uri, int sizePx);
        default void loadGridThumbnail(ImageView target, Uri uri, int sizePx) {
            loadThumbnail(target, uri, sizePx);
        }
        default void prefetchThumbnail(Uri uri, int sizePx) {
        }
        void clearThumbnail(ImageView target);
        void openExternal(MemoryPhotoItem item);
        default String dateNoteText() {
            return "";
        }
        default void editDateNote(Runnable onNoteUpdated) {
        }
        /** Drop the cached detail so Back reloads after a viewer delete or place change. */
        default void onViewerMediaChanged() {
        }
    }

    interface Listener {
        void onClose();
    }

    private MemoryPhotoViewer() {
    }

    static View create(final String title,
                       final MemoryPhotoSection section,
                       int initialIndex,
                       final Host host,
                       final Listener listener) {
        final Context context = host.context();
        final List<MemoryPhotoItem> photos = section == null || section.photos == null
                ? new ArrayList<>()
                : new ArrayList<>(section.photos);
        final int count = photos.size();
        final int firstIndex = clamp(initialIndex, count);

        final FrameLayout root = new FrameLayout(context) {
            @Override
            protected void onWindowVisibilityChanged(int visibility) {
                super.onWindowVisibilityChanged(visibility);
                if (visibility != View.VISIBLE) {
                    Object tag = getTag();
                    if (tag instanceof MemoryViewerVideoPlayer) {
                        ((MemoryViewerVideoPlayer) tag).stopAndRelease();
                    }
                }
            }
        };
        root.setBackgroundColor(Color.rgb(12, 14, 20));
        MediaScreenInsets.bind(root);

        // 1. Center Media Stage (fills entire viewer)
        final FrameLayout photoViewport = new FrameLayout(context);
        photoViewport.setClickable(true);
        photoViewport.setContentDescription("사진 넘기기 또는 동영상 재생");
        root.addView(photoViewport, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        final MemoryZoomableImageView image = new MemoryZoomableImageView(context);
        image.setContentDescription("Memory 사진");
        photoViewport.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        final MemoryViewerVideoPlayer videoPlayer = new MemoryViewerVideoPlayer(context, image, host.dp(1));
        root.setTag(videoPlayer);
        photoViewport.addView(videoPlayer.videoContainer(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));

        FrameLayout.LayoutParams playBadgeParams = new FrameLayout.LayoutParams(
                host.dp(68), host.dp(68), Gravity.CENTER);
        photoViewport.addView(videoPlayer.centerPlayBadge(), playBadgeParams);

        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                videoPlayer.stopAndRelease();
            }
        });

        // 2. Top Chrome Overlay Header (‹ Back | Date · Place + Time | Counter | 🗑 Trash Icon)
        final LinearLayout topChrome = new LinearLayout(context);
        topChrome.setOrientation(LinearLayout.HORIZONTAL);
        topChrome.setGravity(Gravity.CENTER_VERTICAL);
        topChrome.setPadding(host.dp(10), host.dp(12), host.dp(12), host.dp(14));
        GradientDrawable topGradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(215, 10, 12, 18), Color.argb(120, 10, 12, 18), Color.TRANSPARENT});
        topChrome.setBackground(topGradient);
        root.addView(topChrome, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP));

        TextView close = new TextView(context);
        close.setText("‹");
        close.setTextSize(34.0f);
        close.setTextColor(Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Memory 상세로 돌아가기");
        close.setOnClickListener(v -> {
            videoPlayer.stopAndRelease();
            listener.onClose();
        });
        topChrome.addView(close, new LinearLayout.LayoutParams(host.dp(42), host.dp(42)));

        LinearLayout titleColumn = new LinearLayout(context);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        titleColumn.setPadding(host.dp(4), 0, host.dp(8), 0);
        topChrome.addView(titleColumn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        final TextView heading = new TextView(context);
        String baseDate = section == null || section.dateText.isEmpty()
                ? (title == null || title.isEmpty() ? "사진" : title)
                : section.dateText;
        String placePart = section == null || section.placeText.isEmpty() ? "" : " · " + section.placeText;
        heading.setText(baseDate + placePart);
        heading.setTextSize(16.0f);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextColor(Color.WHITE);
        heading.setSingleLine(true);
        heading.setEllipsize(TextUtils.TruncateAt.END);
        titleColumn.addView(heading, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        final TextView subHeading = new TextView(context);
        subHeading.setTextSize(12.0f);
        subHeading.setTextColor(Color.rgb(200, 210, 226));
        subHeading.setSingleLine(true);
        subHeading.setEllipsize(TextUtils.TruncateAt.END);
        titleColumn.addView(subHeading, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        final TextView counter = new TextView(context);
        counter.setTextColor(Color.WHITE);
        counter.setTextSize(12.0f);
        counter.setTypeface(Typeface.DEFAULT_BOLD);
        counter.setGravity(Gravity.CENTER);
        counter.setPadding(host.dp(10), host.dp(5), host.dp(10), host.dp(5));
        GradientDrawable counterBg = new GradientDrawable();
        counterBg.setColor(Color.argb(165, 28, 34, 48));
        counterBg.setCornerRadius(host.dp(14));
        counterBg.setStroke(host.dp(1), Color.argb(55, 255, 255, 255));
        counter.setBackground(counterBg);
        LinearLayout.LayoutParams counterParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        counterParams.rightMargin = host.dp(6);
        topChrome.addView(counter, counterParams);

        // Top-right Share Icon (Instagram Story / Photo Strip share)
        final FrameLayout shareAction = createTopIconAction(
                context, host, MediaVectorIconDrawable.share(Color.WHITE, host.dp(18)), "스토리 공유");
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(host.dp(38), host.dp(38));
        shareParams.rightMargin = host.dp(6);
        topChrome.addView(shareAction, shareParams);

        // Top-right Trash Icon (1-tap OS Trash confirmation)
        final FrameLayout manageAction = createTopIconAction(
                context, host, MediaVectorIconDrawable.trash(Color.WHITE, host.dp(18)), "사진 정리");
        LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(host.dp(38), host.dp(38));
        manageParams.rightMargin = host.dp(6);
        topChrome.addView(manageAction, manageParams);

        // Top-right location icon — existing place change / unlink actions.
        final FrameLayout moreAction = createTopIconAction(
                context, host, MediaVectorIconDrawable.place(Color.WHITE, host.dp(18)), "장소 변경");
        topChrome.addView(moreAction, new LinearLayout.LayoutParams(host.dp(38), host.dp(38)));

        // 3. Bottom Chrome Overlay (Video Control Bar + Mini Filmstrip only!)
        final LinearLayout bottomChrome = new LinearLayout(context);
        bottomChrome.setOrientation(LinearLayout.VERTICAL);
        bottomChrome.setPadding(host.dp(14), host.dp(12), host.dp(14), host.dp(14));
        GradientDrawable bottomGradient = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Color.argb(210, 10, 12, 18), Color.argb(115, 10, 12, 18), Color.TRANSPARENT});
        bottomChrome.setBackground(bottomGradient);
        root.addView(bottomChrome, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        // 3a. Inline Video Control Bar
        LinearLayout.LayoutParams videoControlParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        videoControlParams.bottomMargin = count > 1 ? host.dp(10) : 0;
        bottomChrome.addView(videoPlayer.controlBar(), videoControlParams);

        // 3b. Mini Filmstrip (shown when section has > 1 item)
        final HorizontalScrollView filmstripScroll = new HorizontalScrollView(context);
        filmstripScroll.setHorizontalScrollBarEnabled(false);
        filmstripScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        final LinearLayout filmstripRow = new LinearLayout(context);
        filmstripRow.setOrientation(LinearLayout.HORIZONTAL);
        filmstripRow.setGravity(Gravity.CENTER_VERTICAL);
        filmstripScroll.addView(filmstripRow, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        final List<FrameLayout> filmstripCells = new ArrayList<>();

        if (count > 1) {
            LinearLayout.LayoutParams stripParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    host.dp(56));
            bottomChrome.addView(filmstripScroll, stripParams);
        }

        final int[] indexHolder = {firstIndex};
        final boolean[] chromeVisible = {true};

        final Runnable toggleChrome = () -> {
            chromeVisible[0] = !chromeVisible[0];
            applyChromeVisibility(topChrome, bottomChrome, chromeVisible[0]);
        };

        final Runnable updatePage = new Runnable() {
            @Override
            public void run() {
                bindPage(
                        photos,
                        indexHolder[0],
                        image,
                        videoPlayer,
                        subHeading,
                        counter,
                        bottomChrome,
                        chromeVisible[0],
                        filmstripScroll,
                        filmstripCells,
                        host);
            }
        };

        final Runnable rebuildFilmstrip = new Runnable() {
            @Override
            public void run() {
                filmstripRow.removeAllViews();
                filmstripCells.clear();
                int currentCount = photos.size();
                filmstripScroll.setVisibility(currentCount > 1 ? View.VISIBLE : View.GONE);
                if (currentCount <= 1) {
                    return;
                }
                int thumbSizePx = host.dp(50);
                for (int i = 0; i < currentCount; i++) {
                    final int itemIndex = i;
                    MemoryPhotoItem item = photos.get(i);
                    FrameLayout cell = new FrameLayout(context);
                    LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(host.dp(46), host.dp(46));
                    cellParams.rightMargin = i + 1 < currentCount ? host.dp(6) : 0;
                    cell.setPadding(host.dp(2), host.dp(2), host.dp(2), host.dp(2));

                    ImageView thumb = new ImageView(context);
                    thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    GradientDrawable thumbClip = new GradientDrawable();
                    thumbClip.setCornerRadius(host.dp(8));
                    thumbClip.setColor(Color.rgb(30, 34, 46));
                    thumb.setBackground(thumbClip);
                    thumb.setClipToOutline(true);
                    cell.addView(thumb, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));

                    if (item != null && item.mediaKind == MediaKind.VIDEO) {
                        ImageView badge = new ImageView(context);
                        badge.setScaleType(ImageView.ScaleType.CENTER);
                        badge.setImageDrawable(MediaVectorIconDrawable.play(Color.WHITE, host.dp(8)));
                        GradientDrawable badgeBg = new GradientDrawable();
                        badgeBg.setShape(GradientDrawable.OVAL);
                        badgeBg.setColor(Color.argb(180, 14, 18, 28));
                        badge.setBackground(badgeBg);
                        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                                host.dp(16), host.dp(16), Gravity.BOTTOM | Gravity.END);
                        bp.rightMargin = host.dp(2);
                        bp.bottomMargin = host.dp(2);
                        cell.addView(badge, bp);
                    }

                    if (item != null && !item.sourceUri.isEmpty()) {
                        try {
                            host.loadGridThumbnail(thumb, Uri.parse(item.sourceUri), thumbSizePx);
                        } catch (Exception ignored) {
                        }
                    }

                    cell.setOnClickListener(v -> {
                        if (indexHolder[0] != itemIndex) {
                            indexHolder[0] = itemIndex;
                            updatePage.run();
                        }
                    });
                    filmstripCells.add(cell);
                    filmstripRow.addView(cell, cellParams);
                }
            }
        };
        rebuildFilmstrip.run();

        final String[] pendingSingleUri = {""};
        final long[] pendingManualRev = {-1L};

        final MemoryManagementEntry.ViewerReturnHandler returnHandler = (returnKey, exitToDetail) -> {
            final String targetUri = pendingSingleUri[0];
            final long prevRev = pendingManualRev[0];
            pendingSingleUri[0] = "";
            if (targetUri == null || targetUri.isEmpty() || !root.isAttachedToWindow()) {
                return false;
            }
            Executors.newSingleThreadExecutor().execute(() -> {
                boolean removedOrMoved = false;
                try {
                    boolean stillExists = !new MemoryMediaAccess(context)
                            .observeUris(Collections.singletonList(targetUri)).isEmpty();
                    long nowRev = new ManualPlaceStore(context).revision();
                    removedOrMoved = !stillExists || (prevRev >= 0L && nowRev != prevRev);
                } catch (Exception ignored) {
                }
                final boolean mutated = removedOrMoved;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!mutated) {
                        return;
                    }
                    if (!root.isAttachedToWindow()) {
                        host.onViewerMediaChanged();
                        return;
                    }
                    int removeIdx = -1;
                    for (int i = 0; i < photos.size(); i++) {
                        MemoryPhotoItem p = photos.get(i);
                        if (p != null && targetUri.equals(p.sourceUri)) {
                            removeIdx = i;
                            break;
                        }
                    }
                    if (removeIdx >= 0) {
                        photos.remove(removeIdx);
                    }
                    host.onViewerMediaChanged();
                    if (photos.isEmpty()) {
                        videoPlayer.stopAndRelease();
                        exitToDetail.accept(returnKey);
                        return;
                    }
                    indexHolder[0] = clamp(indexHolder[0], photos.size());
                    rebuildFilmstrip.run();
                    updatePage.run();
                });
            });
            return true;
        };

        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                MemoryManagementEntry.bindViewerReturnHandler(returnHandler);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                MemoryManagementEntry.unbindViewerReturnHandler(returnHandler);
            }
        });
        MemoryManagementEntry.bindViewerReturnHandler(returnHandler);

        shareAction.setOnClickListener(v -> {
            videoPlayer.stopAndRelease();
            MemoryPhotoItem currentItem = current(photos, indexHolder[0]);
            if (currentItem == null) {
                return;
            }
            MemoryPhotoSection liveSection = MemoryPhotoSection.fromCollectionPlace(
                    section == null ? "" : section.dateKey,
                    section == null ? "" : section.dateText,
                    section == null ? "" : section.placeText,
                    new ArrayList<>(photos));
            MemoryStoryShareSheet.showForViewerPhoto(
                    context,
                    title,
                    liveSection,
                    currentItem,
                    host.dateNoteText());
        });

        manageAction.setOnClickListener(v -> {
            videoPlayer.stopAndRelease();
            MemoryPhotoItem currentItem = current(photos, indexHolder[0]);
            if (currentItem == null) {
                return;
            }
            pendingSingleUri[0] = currentItem.sourceUri;
            try {
                pendingManualRev[0] = new ManualPlaceStore(context).revision();
            } catch (Exception ignored) {
                pendingManualRev[0] = -1L;
            }
            MemoryManagementEntry.openSingle(
                    host.context(),
                    currentItem,
                    host.managementMemoryKey());
        });

        moreAction.setOnClickListener(v -> {
            MemoryPhotoItem currentItem = current(photos, indexHolder[0]);
            if (currentItem == null) {
                return;
            }
            MemoryPlaceActionsPopup.show(moreAction, () -> {
                videoPlayer.stopAndRelease();
                pendingSingleUri[0] = currentItem.sourceUri;
                try {
                    pendingManualRev[0] = new ManualPlaceStore(context).revision();
                } catch (Exception ignored) {
                    pendingManualRev[0] = -1L;
                }
                MemoryManagementEntry.openSinglePlaceChange(
                        host.context(),
                        currentItem,
                        host.managementMemoryKey(),
                        false);
            }, currentItem.manualAssigned ? () -> {
                    videoPlayer.stopAndRelease();
                    pendingSingleUri[0] = currentItem.sourceUri;
                    try {
                        pendingManualRev[0] = new ManualPlaceStore(context).revision();
                    } catch (Exception ignored) {
                        pendingManualRev[0] = -1L;
                    }
                    MemoryManagementEntry.openSinglePlaceChange(
                            host.context(),
                            currentItem,
                            host.managementMemoryKey(),
                            true);
            } : null);
        });

        // Accessibility / fallback click handler on photoViewport
        photoViewport.setOnClickListener(v -> {
            MemoryPhotoItem currentItem = current(photos, indexHolder[0]);
            if (currentItem != null && currentItem.mediaKind == MediaKind.VIDEO && !videoPlayer.isPlaying()) {
                videoPlayer.togglePlayPause(() -> MemoryViewerVideoAction.open(currentItem, host::openExternal));
            } else {
                toggleChrome.run();
            }
        });

        // 4. Unified Gesture Handling (Pinch Zoom, Double-Tap Zoom, Zoomed Pan, Horizontal Page Swipe, Vertical Dismiss)
        final int tapSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        final float swipeThresholdPx = host.dp(42);
        final float dismissThresholdPx = host.dp(92);
        final float dismissMaxRangePx = host.dp(260);

        final GestureDetector tapDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleChrome.run();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                MemoryPhotoItem item = current(photos, indexHolder[0]);
                if (item != null && item.mediaKind != MediaKind.VIDEO) {
                    image.toggleDoubleTapZoom(e.getX(), e.getY());
                    return true;
                }
                return false;
            }
        });

        final MemoryViewerGesturePolicy.Mode[] gestureMode = {MemoryViewerGesturePolicy.Mode.NONE};
        final float[] downX = {0.0f};
        final float[] downY = {0.0f};
        final float[] lastX = {0.0f};
        final float[] lastY = {0.0f};

        photoViewport.setOnTouchListener((view, event) -> {
            image.onScaleEvent(event);
            tapDetector.onTouchEvent(event);

            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX[0] = event.getRawX();
                downY[0] = event.getRawY();
                lastX[0] = downX[0];
                lastY[0] = downY[0];
                gestureMode[0] = image.isZoomed()
                        ? MemoryViewerGesturePolicy.Mode.PAN_ZOOMED
                        : MemoryViewerGesturePolicy.Mode.NONE;
                return true;
            }

            if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
                gestureMode[0] = MemoryViewerGesturePolicy.Mode.PINCH_ZOOM;
                resetViewportTransform(photoViewport, root);
                return true;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                float rawX = event.getRawX();
                float rawY = event.getRawY();
                float totalDx = rawX - downX[0];
                float totalDy = rawY - downY[0];
                float stepDx = rawX - lastX[0];
                float stepDy = rawY - lastY[0];
                lastX[0] = rawX;
                lastY[0] = rawY;

                gestureMode[0] = MemoryViewerGesturePolicy.evaluate(
                        gestureMode[0],
                        event.getPointerCount(),
                        image.currentScale(),
                        totalDx,
                        totalDy,
                        tapSlop);

                if (gestureMode[0] == MemoryViewerGesturePolicy.Mode.PAN_ZOOMED && !image.isPinching()) {
                    image.panBy(stepDx, stepDy);
                } else if (gestureMode[0] == MemoryViewerGesturePolicy.Mode.SWIPE_HORIZONTAL) {
                    float dampedDx = totalDx * 0.35f;
                    photoViewport.setTranslationX(dampedDx);
                    photoViewport.setTranslationY(0.0f);
                } else if (gestureMode[0] == MemoryViewerGesturePolicy.Mode.DISMISS_VERTICAL) {
                    float clampedDy = Math.max(0.0f, totalDy);
                    photoViewport.setTranslationY(clampedDy * 0.75f);
                    float alpha = MemoryViewerGesturePolicy.computeDismissAlpha(clampedDy, dismissMaxRangePx);
                    photoViewport.setAlpha(alpha);
                }
                return true;
            }

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                float totalDx = event.getRawX() - downX[0];
                float totalDy = event.getRawY() - downY[0];
                MemoryViewerGesturePolicy.Mode endMode = gestureMode[0];
                gestureMode[0] = MemoryViewerGesturePolicy.Mode.NONE;

                if (action == MotionEvent.ACTION_UP) {
                    if (MemoryViewerGesturePolicy.shouldDismissVertical(endMode, totalDy, dismissThresholdPx)) {
                        resetViewportTransform(photoViewport, root);
                        videoPlayer.stopAndRelease();
                        listener.onClose();
                        return true;
                    }
                    int delta = MemoryViewerGesturePolicy.resolvePageDelta(
                            endMode, totalDx, swipeThresholdPx, indexHolder[0], photos.size());
                    if (delta != 0) {
                        resetViewportTransform(photoViewport, root);
                        indexHolder[0] += delta;
                        updatePage.run();
                        return true;
                    }
                }
                animateViewportSnapBack(photoViewport);
                return true;
            }
            return true;
        });

        updatePage.run();
        return root;
    }

    private static FrameLayout createTopIconAction(Context context,
                                                   Host host,
                                                   android.graphics.drawable.Drawable iconDrawable,
                                                   String contentDesc) {
        FrameLayout button = new FrameLayout(context);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.argb(165, 28, 34, 48));
        circle.setStroke(host.dp(1), Color.argb(55, 255, 255, 255));
        button.setBackground(circle);
        button.setContentDescription(contentDesc);
        ImageView icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        icon.setImageDrawable(iconDrawable);
        button.addView(icon, new FrameLayout.LayoutParams(
                host.dp(22), host.dp(22), Gravity.CENTER));
        return button;
    }

    private static void bindPage(List<MemoryPhotoItem> photos,
                                 int index,
                                 MemoryZoomableImageView image,
                                 MemoryViewerVideoPlayer videoPlayer,
                                 TextView subHeading,
                                 TextView counter,
                                 LinearLayout bottomChrome,
                                 boolean chromeVisible,
                                 HorizontalScrollView filmstripScroll,
                                 List<FrameLayout> filmstripCells,
                                 Host host) {
        MemoryPhotoItem item = current(photos, index);
        int count = photos == null ? 0 : photos.size();
        counter.setText(count == 0 ? "0 / 0" : (index + 1) + " / " + count);

        // Reset zoom state before loading new item
        image.resetZoom();

        // Update sub-heading metadata (capture time + video indicator + manual placement badge)
        String meta = buildSubHeadingText(item);
        subHeading.setText(meta);
        subHeading.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        // Update mini filmstrip active highlight
        updateFilmstripHighlight(filmstripScroll, filmstripCells, index, host);

        if (item == null || item.sourceUri.isEmpty()) {
            videoPlayer.bind(null, null);
            host.clearThumbnail(image);
            bottomChrome.setVisibility(count > 1 && chromeVisible ? View.VISIBLE : View.GONE);
            return;
        }

        boolean isVideo = item.mediaKind == MediaKind.VIDEO;
        image.setZoomEnabled(!isVideo);
        boolean hasBottomContent = isVideo || count > 1;
        bottomChrome.setVisibility(hasBottomContent && chromeVisible ? View.VISIBLE : View.GONE);

        // Always load high-res poster/photo (never wipe video thumbnail to black!)
        int targetDecodePx = host.dp(1080);
        host.loadThumbnail(image, Uri.parse(item.sourceUri), targetDecodePx);

        // Bind inline video player (shows central ▶ badge + bottom seekbar for videos, hides for photos)
        videoPlayer.bind(item, () -> MemoryViewerVideoAction.open(item, host::openExternal));

        // Prefetch +/- 1 neighbor into LruCache for smooth swiping
        prefetchNeighbor(photos, index - 1, targetDecodePx, host);
        prefetchNeighbor(photos, index + 1, targetDecodePx, host);
    }

    private static void prefetchNeighbor(List<MemoryPhotoItem> photos, int neighborIndex, int sizePx, Host host) {
        MemoryPhotoItem neighbor = current(photos, neighborIndex);
        if (neighbor != null && !neighbor.sourceUri.isEmpty()) {
            try {
                host.prefetchThumbnail(Uri.parse(neighbor.sourceUri), sizePx);
            } catch (Exception ignored) {
            }
        }
    }

    private static String buildSubHeadingText(MemoryPhotoItem item) {
        if (item == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (item.takenAtMillis > 0L) {
            try {
                String timeText = new SimpleDateFormat("a h:mm", Locale.KOREA)
                        .format(new Date(item.takenAtMillis));
                sb.append(timeText);
            } catch (Exception ignored) {
            }
        }
        if (item.mediaKind == MediaKind.VIDEO) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("동영상");
        }
        if (item.manualAssigned) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("직접 지정됨");
        }
        return sb.toString();
    }

    private static void updateFilmstripHighlight(HorizontalScrollView scroll,
                                                 List<FrameLayout> cells,
                                                 int activeIndex,
                                                 Host host) {
        if (cells == null || cells.isEmpty()) {
            return;
        }
        for (int i = 0; i < cells.size(); i++) {
            FrameLayout cell = cells.get(i);
            boolean active = i == activeIndex;
            GradientDrawable border = new GradientDrawable();
            border.setCornerRadius(host.dp(10));
            if (active) {
                border.setStroke(host.dp(2), Color.rgb(164, 142, 255));
                border.setColor(Color.argb(90, 164, 142, 255));
            } else {
                border.setColor(Color.TRANSPARENT);
            }
            cell.setBackground(border);
            cell.setAlpha(active ? 1.0f : 0.62f);
        }
        if (activeIndex >= 0 && activeIndex < cells.size()) {
            final View activeCell = cells.get(activeIndex);
            scroll.post(() -> {
                int scrollX = activeCell.getLeft() - (scroll.getWidth() - activeCell.getWidth()) / 2;
                scroll.smoothScrollTo(Math.max(0, scrollX), 0);
            });
        }
    }

    private static void applyChromeVisibility(View topChrome, View bottomChrome, boolean visible) {
        for (View bar : new View[]{topChrome, bottomChrome}) {
            bar.animate().cancel();
            if (visible) {
                bar.setVisibility(View.VISIBLE);
                bar.animate()
                        .alpha(1.0f)
                        .setDuration(160L)
                        .withEndAction(null)
                        .start();
            } else {
                bar.animate()
                        .alpha(0.0f)
                        .setDuration(160L)
                        .withEndAction(() -> bar.setVisibility(View.GONE))
                        .start();
            }
        }
    }

    private static void animateViewportSnapBack(View viewport) {
        viewport.animate().cancel();
        viewport.animate()
                .translationX(0.0f)
                .translationY(0.0f)
                .alpha(1.0f)
                .setDuration(150L)
                .start();
    }

    private static void resetViewportTransform(View viewport, View root) {
        viewport.animate().cancel();
        viewport.setTranslationX(0.0f);
        viewport.setTranslationY(0.0f);
        viewport.setAlpha(1.0f);
        root.setAlpha(1.0f);
    }

    private static MemoryPhotoItem current(List<MemoryPhotoItem> photos, int index) {
        return photos == null || index < 0 || index >= photos.size() ? null : photos.get(index);
    }

    private static int clamp(int index, int count) {
        if (count <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(index, count - 1));
    }
}
