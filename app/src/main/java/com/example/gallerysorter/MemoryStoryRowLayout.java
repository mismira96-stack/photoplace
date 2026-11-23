package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.List;

/** Whole-photo layouts: one row for two or three photos; two rows for four. */
final class MemoryStoryRowLayout {
    private static final float[] ANGLES = {-3.4f, 2.1f, -1.8f, 2.8f};
    private static final float[] GAPS = {10f, 16f, 12f, 0f};
    private static final float[] OFFSETS = {10f, 28f, 0f, 18f};

    static float angle(MemoryStoryConfig config, int index) {
        if (config.layoutMode != MemoryStoryConfig.LayoutMode.SCRAPBOOK) return 0;
        return ANGLES[index];
    }

    static boolean usesRow(MemoryStoryConfig config, int count) {
        return count >= 2 && (config.layoutMode == MemoryStoryConfig.LayoutMode.PORTRAIT_ROW
                || config.layoutMode == MemoryStoryConfig.LayoutMode.SCRAPBOOK);
    }

    static float footerHeight(MemoryStoryConfig config) {
        return footerHeight(config, 1f);
    }

    static float footerHeight(MemoryStoryConfig config, float scale) {
        if (!config.hasVisibleText()) return 16f;
        float captionSize = config.captionText.isEmpty() ? 0
                : captionTextSize(config, scale);
        return 24f * scale + Math.max(dateTextSize(scale), captionSize) * 1.35f;
    }

    static float captionTextSize(MemoryStoryConfig config, float scale) {
        return (config.fontKind == MemoryStoryConfig.FontKind.HAND ? 68f : 52f) * config.textScale * scale;
    }

    static float dateTextSize(float scale) {
        return Math.max(22f, 28f * scale);
    }

    static float footerScale(float photoHeight, int count, boolean allLandscape) {
        if (!allLandscape || count < 2 || count > 3) return 1f;
        // Shallow landscape rows need a smaller footer, with a readable lower bound.
        return Math.max(.76f, Math.min(1f, (float) Math.sqrt(photoHeight / 500f)));
    }

    static float footerScale(List<MemoryStoryComposer.Box> slots) {
        boolean landscape = true;
        float height = Float.MAX_VALUE;
        for (MemoryStoryComposer.Box slot : slots) {
            landscape &= slot.width() > slot.height();
            height = Math.min(height, slot.height());
        }
        return footerScale(height, slots.size(), landscape);
    }

    static MemoryStoryComposer.StripLayout compute(int[] widths, int[] heights, MemoryStoryConfig config) {
        int count = Math.min(4, Math.min(widths.length, heights.length));
        if (count < 2) throw new IllegalArgumentException("Row requires 2–4 photos");
        if (count == 4) return computeTwoRows(widths, heights, count, config);
        boolean scrapbook = config.layoutMode == MemoryStoryConfig.LayoutMode.SCRAPBOOK;
        // The accepted scrapbook has no individual paper frames; reserve only shadow space.
        float border = 0f;
        float padding = 24f;
        float footer = footerHeight(config);
        float[] ratios = new float[count];
        float[] gaps = new float[count];
        float widthFactor = 0, frameWidth = 0, maxHeightFactor = 0, maxFrameHeight = 0;
        float gapTotal = 0;
        for (int i = 0; i < count; i++) {
            ratios[i] = Math.max(1, widths[i]) / (float) Math.max(1, heights[i]);
            double radians = Math.toRadians(Math.abs(angle(config, i)));
            float cos = (float) Math.cos(radians), sin = (float) Math.sin(radians);
            widthFactor += ratios[i] * cos + sin;
            frameWidth += border * 2 * (cos + sin);
            maxHeightFactor = Math.max(maxHeightFactor, cos + ratios[i] * sin);
            maxFrameHeight = Math.max(maxFrameHeight, border * 2 * (cos + sin));
            gaps[i] = scrapbook ? GAPS[i] : 24f;
            if (i < count - 1) gapTotal += gaps[i];
        }
        float stagger = scrapbook ? 28f : 0;
        float photoHeight = Math.min((1012f - padding * 2 - gapTotal - frameWidth) / widthFactor,
                (1760f - padding * 2 - footer - stagger - maxFrameHeight) / maxHeightFactor);
        photoHeight = Math.max(1, photoHeight);
        boolean allLandscape = true;
        for (float ratio : ratios) allLandscape &= ratio > 1f;
        footer = footerHeight(config, footerScale(photoHeight, count, allLandscape));
        float totalWidth = widthFactor * photoHeight + frameWidth + gapTotal + padding * 2;
        float rowHeight = maxHeightFactor * photoHeight + maxFrameHeight + stagger;
        float totalHeight = padding * 2 + rowHeight + footer;
        float left = (1080f - totalWidth) / 2, top = (1920f - totalHeight) / 2;
        List<MemoryStoryComposer.Box> slots = new ArrayList<>();
        float x = left + padding;
        for (int i = 0; i < count; i++) {
            double radians = Math.toRadians(Math.abs(angle(config, i)));
            float rotatedWidth = (float) ((ratios[i] * photoHeight + border * 2) * Math.cos(radians)
                    + (photoHeight + border * 2) * Math.sin(radians));
            float rotatedHeight = (float) ((photoHeight + border * 2) * Math.cos(radians)
                    + (ratios[i] * photoHeight + border * 2) * Math.sin(radians));
            float centerX = x + rotatedWidth / 2;
            float offset = scrapbook ? OFFSETS[i] : 0;
            float centerY = top + padding + rotatedHeight / 2 + offset;
            float w = ratios[i] * photoHeight;
            slots.add(new MemoryStoryComposer.Box(centerX - w / 2, centerY - photoHeight / 2,
                    centerX + w / 2, centerY + photoHeight / 2));
            x += rotatedWidth + gaps[i];
        }
        return new MemoryStoryComposer.StripLayout(
                new MemoryStoryComposer.Box(left, top, left + totalWidth, top + totalHeight), slots);
    }

    private static MemoryStoryComposer.StripLayout computeTwoRows(
            int[] widths, int[] heights, int count, MemoryStoryConfig config) {
        final float padding = 24f, rowGap = 24f;
        boolean scrapbook = config.layoutMode == MemoryStoryConfig.LayoutMode.SCRAPBOOK;
        float gap = scrapbook ? 10f : 24f;
        float footer = footerHeight(config);
        float[] ratios = new float[count], rotatedWidths = new float[count], rotatedHeights = new float[count];
        float[] offsets = new float[count];
        float height = Float.MAX_VALUE;
        for (int row = 0; row < 2; row++) {
            float widthFactor = 0;
            int start = row * 2, end = Math.min(count, start + 2);
            for (int i = start; i < end; i++) {
                ratios[i] = Math.max(1, widths[i]) / (float) Math.max(1, heights[i]);
                double angle = Math.toRadians(Math.abs(angle(config, i)));
                rotatedWidths[i] = (float) (ratios[i] * Math.cos(angle) + Math.sin(angle));
                rotatedHeights[i] = (float) (Math.cos(angle) + ratios[i] * Math.sin(angle));
                offsets[i] = scrapbook && end - start == 2 ? OFFSETS[i] - Math.min(OFFSETS[start], OFFSETS[start + 1]) : 0;
                widthFactor += rotatedWidths[i];
            }
            height = Math.min(height, (1012f - padding * 2 - gap * (end - start - 1)) / widthFactor);
        }
        // A shared photo height keeps the single photo in a three-photo layout in proportion.
        float heightFactor = 0, stagger = 0;
        for (int row = 0; row < 2; row++) {
            float factor = 0, offset = 0;
            for (int i = row * 2; i < Math.min(count, row * 2 + 2); i++) {
                factor = Math.max(factor, rotatedHeights[i]);
                offset = Math.max(offset, offsets[i]);
            }
            heightFactor += factor;
            stagger += offset;
        }
        height = Math.max(1, Math.min(height, (1760f - padding * 2 - footer - rowGap - stagger) / heightFactor));
        float[] rowWidths = new float[2], rowHeights = new float[2];
        for (int i = 0; i < count; i++) {
            rowWidths[i / 2] += rotatedWidths[i] * height + (i % 2 == 1 ? gap : 0);
            rowHeights[i / 2] = Math.max(rowHeights[i / 2], rotatedHeights[i] * height + offsets[i]);
        }
        float cardWidth = Math.max(rowWidths[0], rowWidths[1]) + padding * 2;
        float cardHeight = rowHeights[0] + rowGap + rowHeights[1] + padding * 2 + footer;
        float left = (1080f - cardWidth) / 2, top = (1920f - cardHeight) / 2;
        List<MemoryStoryComposer.Box> slots = new ArrayList<>();
        float rowTop = top + padding;
        for (int row = 0; row < 2; row++) {
            float x = left + (cardWidth - rowWidths[row]) / 2;
            for (int i = row * 2; i < Math.min(count, row * 2 + 2); i++) {
                float cx = x + rotatedWidths[i] * height / 2;
                float cy = rowTop + offsets[i] + rotatedHeights[i] * height / 2;
                float w = ratios[i] * height;
                slots.add(new MemoryStoryComposer.Box(cx - w / 2, cy - height / 2, cx + w / 2, cy + height / 2));
                x += rotatedWidths[i] * height + gap;
            }
            rowTop += rowHeights[row] + rowGap;
        }
        return new MemoryStoryComposer.StripLayout(
                new MemoryStoryComposer.Box(left, top, left + cardWidth, top + cardHeight), slots);
    }
}
