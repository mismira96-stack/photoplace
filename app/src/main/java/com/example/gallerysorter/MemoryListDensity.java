package com.example.gallerysorter;

/** Stores the chosen columns independently for narrow and wide windows. */
final class MemoryListDensity {
    private static final float SHRINK_THRESHOLD = 0.82f;
    private static final float EXPAND_THRESHOLD = 1f / SHRINK_THRESHOLD;
    private final int minNarrow;
    private final int maxNarrow;
    private final int minWide;
    private final int maxWide;
    private int narrowColumns;
    private int wideColumns;
    private float scale = 1f;

    MemoryListDensity() {
        this(2, 4, 3, 4);
    }

    MemoryListDensity(int minNarrow, int maxNarrow, int minWide, int maxWide) {
        this.minNarrow = minNarrow;
        this.maxNarrow = maxNarrow;
        this.minWide = minWide;
        this.maxWide = maxWide;
        this.narrowColumns = minNarrow;
        this.wideColumns = minWide;
    }

    static MemoryListDensity forDetail() {
        return new MemoryListDensity(3, 4, 3, 5);
    }

    int columns(boolean wide) {
        return wide ? wideColumns : narrowColumns;
    }

    void beginPinch() {
        scale = 1f;
    }

    boolean scaleBy(float factor, boolean wide) {
        if (!Float.isFinite(factor) || factor <= 0f) return false;
        int current = columns(wide);
        int minimum = wide ? minWide : minNarrow;
        int maximum = wide ? maxWide : maxNarrow;
        scale *= factor;
        if ((current == minimum && scale > 1f) || (current == maximum && scale < 1f)) scale = 1f;
        int next = current;
        if (scale <= SHRINK_THRESHOLD && current < maximum) next++;
        else if (scale >= EXPAND_THRESHOLD && current > minimum) next--;
        if (next == current) return false;
        scale = 1f;
        if (wide) wideColumns = next;
        else narrowColumns = next;
        return true;
    }
}
