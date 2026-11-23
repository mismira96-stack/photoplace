package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.List;

/** Keeps user choices until the selected photos no longer support that layout. */
final class MemoryStoryTemplateSelection {
    private MemoryStoryConfig.LayoutMode selected;
    private boolean manual;

    MemoryStoryConfig.LayoutMode resolve(int[] widths, int[] heights) {
        if (!manual || !compatible(selected, widths, heights)) {
            manual = false;
            selected = allPortrait(widths, heights) ? MemoryStoryConfig.LayoutMode.PORTRAIT_ROW
                    : widths.length >= 2 ? MemoryStoryConfig.LayoutMode.PHOTO_STRIP
                    : MemoryStoryConfig.LayoutMode.SINGLE_FRAME;
        }
        return selected;
    }

    void select(MemoryStoryConfig.LayoutMode mode) {
        selected = mode;
        manual = true;
    }

    static boolean allPortrait(int[] widths, int[] heights) {
        if (widths.length < 2 || widths.length > 4 || widths.length != heights.length) return false;
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] <= 0 || heights[i] <= widths[i]) return false;
        }
        return true;
    }

    static boolean compatible(MemoryStoryConfig.LayoutMode mode, int[] widths, int[] heights) {
        if (mode == null || widths.length == 0 || widths.length != heights.length) return false;
        switch (mode) {
            case PORTRAIT_ROW:
            case SCRAPBOOK: return widths.length >= 2 && widths.length <= 4;
            case PHOTO_STRIP: return widths.length >= 2 && widths.length <= 4;
            default: return widths.length == 1;
        }
    }

    static List<MemoryStoryConfig.LayoutMode> options(int[] widths, int[] heights) {
        List<MemoryStoryConfig.LayoutMode> result = new ArrayList<>();
        for (MemoryStoryConfig.LayoutMode mode : new MemoryStoryConfig.LayoutMode[]{
                MemoryStoryConfig.LayoutMode.PHOTO_STRIP, MemoryStoryConfig.LayoutMode.PORTRAIT_ROW,
                MemoryStoryConfig.LayoutMode.SCRAPBOOK, MemoryStoryConfig.LayoutMode.SINGLE_FRAME}) {
            if (compatible(mode, widths, heights)) result.add(mode);
        }
        return result;
    }

    static String label(MemoryStoryConfig.LayoutMode mode) {
        switch (mode) {
            case PORTRAIT_ROW: return "나란히";
            case SCRAPBOOK: return "스크랩북";
            case PHOTO_STRIP: return "사진띠";
            default: return "한 장";
        }
    }
}
