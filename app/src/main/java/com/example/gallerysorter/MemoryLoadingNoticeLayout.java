package com.example.gallerysorter;

/** Positions the notice inside the actual content viewport, above navigation rather than system edges. */
final class MemoryLoadingNoticeLayout {
    final int width, leftMargin, bottomMargin;

    private MemoryLoadingNoticeLayout(int width, int left, int bottom) {
        this.width = width; leftMargin = left; bottomMargin = bottom;
    }

    static MemoryLoadingNoticeLayout calculate(int parentWidth, int parentHeight, int viewportLeft,
                                                int viewportWidth, int viewportBottom,
                                                int maxContentWidth, int sideInset, int gap) {
        int left = Math.max(0, Math.min(viewportLeft, Math.max(0, parentWidth - 1)));
        int available = Math.max(1, Math.min(viewportWidth, parentWidth - left));
        int width = Math.max(1, Math.min(available, maxContentWidth) - 2 * sideInset);
        return new MemoryLoadingNoticeLayout(width, left + (available - width) / 2,
                Math.max(0, parentHeight - Math.max(0, Math.min(viewportBottom, parentHeight))) + gap);
    }
}
