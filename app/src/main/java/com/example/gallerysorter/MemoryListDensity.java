package com.example.gallerysorter;

/** Stores the chosen columns independently for narrow and wide windows. */
final class MemoryListDensity {
    private static final float SHRINK_THRESHOLD = 0.82f;
    private static final float EXPAND_THRESHOLD = 1f / SHRINK_THRESHOLD;
    private int narrowColumns = 2;
    private int wideColumns = 3;
    private float scale = 1f;

    int columns(boolean wide) {
        return wide ? wideColumns : narrowColumns;
    }

    void beginPinch() {
        scale = 1f;
    }

    boolean scaleBy(float factor, boolean wide) {
        if (!Float.isFinite(factor) || factor <= 0f) return false;
        int current = columns(wide);
        int minimum = wide ? 3 : 2;
        scale *= factor;
        if ((current == minimum && scale > 1f) || (current == 4 && scale < 1f)) scale = 1f;
        int next = current;
        if (scale <= SHRINK_THRESHOLD && current < 4) next++;
        else if (scale >= EXPAND_THRESHOLD && current > minimum) next--;
        if (next == current) return false;
        scale = 1f;
        if (wide) wideColumns = next;
        else narrowColumns = next;
        return true;
    }
}
