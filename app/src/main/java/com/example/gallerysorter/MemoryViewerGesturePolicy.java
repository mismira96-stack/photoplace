package com.example.gallerysorter;

/**
 * Pure state machine for Memory viewer touch gestures.
 * Prevents conflicts between pinch-zoom, zoomed panning, horizontal page swipe, and vertical swipe-to-dismiss.
 */
final class MemoryViewerGesturePolicy {
    enum Mode {
        NONE,
        PINCH_ZOOM,
        PAN_ZOOMED,
        SWIPE_HORIZONTAL,
        DISMISS_VERTICAL
    }

    static final float MIN_SCALE = 1.0f;
    static final float DOUBLE_TAP_SCALE = 2.5f;
    static final float MAX_SCALE = 4.0f;
    private static final float ZOOMED_THRESHOLD = 1.01f;

    private MemoryViewerGesturePolicy() {
    }

    static boolean isZoomed(float scale) {
        return scale > ZOOMED_THRESHOLD;
    }

    static float clampScale(float scale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    static float nextDoubleTapScale(float currentScale) {
        return isZoomed(currentScale) ? MIN_SCALE : DOUBLE_TAP_SCALE;
    }

    /**
     * Resolves or preserves the active gesture mode for the current touch stream.
     * Once locked into PINCH_ZOOM, PAN_ZOOMED, SWIPE_HORIZONTAL, or DISMISS_VERTICAL,
     * the mode will not flip between horizontal swipe and vertical dismiss mid-gesture.
     */
    static Mode evaluate(Mode currentMode,
                         int pointerCount,
                         float currentScale,
                         float dx,
                         float dy,
                         int touchSlopPx) {
        if (pointerCount >= 2) {
            return Mode.PINCH_ZOOM;
        }
        if (currentMode == Mode.PINCH_ZOOM) {
            return Mode.PINCH_ZOOM;
        }
        if (isZoomed(currentScale)) {
            // Never allow vertical swipe-to-dismiss or unguarded page swipe while zoomed in.
            return Mode.PAN_ZOOMED;
        }
        if (currentMode == Mode.SWIPE_HORIZONTAL || currentMode == Mode.DISMISS_VERTICAL) {
            return currentMode;
        }
        int slop = Math.max(1, touchSlopPx);
        float absDx = Math.abs(dx);
        float absDy = Math.abs(dy);
        if (absDx > slop && absDx > absDy * 1.2f) {
            return Mode.SWIPE_HORIZONTAL;
        }
        if (dy > slop * 2.0f && absDy > absDx * 1.8f) {
            return Mode.DISMISS_VERTICAL;
        }
        return Mode.NONE;
    }

    /**
     * Returns -1 (previous), +1 (next), or 0 (stay) when a gesture ends.
     */
    static int resolvePageDelta(Mode mode,
                                float dx,
                                float minSwipeDistancePx,
                                int currentIndex,
                                int totalCount) {
        if (mode != Mode.SWIPE_HORIZONTAL || totalCount <= 1) {
            return 0;
        }
        float threshold = Math.max(1.0f, minSwipeDistancePx);
        if (Math.abs(dx) < threshold) {
            return 0;
        }
        int delta = dx < 0.0f ? 1 : -1;
        int nextIndex = currentIndex + delta;
        if (nextIndex < 0 || nextIndex >= totalCount) {
            return 0;
        }
        return delta;
    }

    static boolean shouldDismissVertical(Mode mode, float dy, float dismissThresholdPx) {
        return mode == Mode.DISMISS_VERTICAL && dy >= Math.max(1.0f, dismissThresholdPx);
    }

    static float computeDismissAlpha(float dy, float maxRangePx) {
        if (dy <= 0.0f || maxRangePx <= 0.0f) {
            return 1.0f;
        }
        float fraction = Math.min(1.0f, dy / maxRangePx);
        return Math.max(0.35f, 1.0f - fraction * 0.65f);
    }
}
