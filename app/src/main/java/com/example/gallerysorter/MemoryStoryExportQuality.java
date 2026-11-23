package com.example.gallerysorter;

/** Export resolution and bounded source decoding, independent of the editor preview. */
final class MemoryStoryExportQuality {
    static final int OUTPUT_WIDTH = 2160;
    private static final int MIN_SOURCE_EDGE = 1440;
    private static final double RESAMPLE_HEADROOM = 1.5;
    private static final int MAX_SOURCE_EDGE = 4096;
    private static final long SOURCE_PIXEL_BUDGET = 12_000_000L;

    static int decodeEdge(int width, int height, MemoryStoryComposer.Box slot,
                          float zoom, int count) {
        double longest = Math.max(1, Math.max(width, height));
        double shortest = Math.max(1, Math.min(width, height));
        double fit = Math.min(slot.width() / Math.max(1, width), slot.height() / Math.max(1, height));
        double needed = longest * fit * zoom * OUTPUT_WIDTH / MemoryStoryComposer.STORY_WIDTH;
        // Keep detail for the final scale/rotation pass instead of decoding exactly to the slot.
        // Small collages must not read less detail than the previous 1440px input path.
        needed = Math.max(MIN_SOURCE_EDGE, needed * RESAMPLE_HEADROOM);
        double pixelLimit = Math.sqrt(SOURCE_PIXEL_BUDGET / Math.max(1, count) * longest / shortest);
        return Math.max(1, (int) Math.floor(Math.min(needed, Math.min(MAX_SOURCE_EDGE, pixelLimit))));
    }
}
