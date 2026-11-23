package com.example.gallerysorter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryStoryTemplatesTest {
    @Test public void landscapeRowsGetCompactReadableFooterWithoutCroppingPhotos() {
        for (MemoryStoryConfig.LayoutMode mode : new MemoryStoryConfig.LayoutMode[]{
                MemoryStoryConfig.LayoutMode.PORTRAIT_ROW, MemoryStoryConfig.LayoutMode.SCRAPBOOK}) {
            for (int count = 2; count <= 3; count++) {
                int[] widths = new int[count], heights = new int[count];
                Arrays.fill(widths, 900); Arrays.fill(heights, 600);
                MemoryStoryConfig c = config(mode);
                MemoryStoryComposer.StripLayout layout = MemoryStoryRowLayout.compute(widths, heights, c);
                float scale = MemoryStoryRowLayout.footerScale(layout.slots);
                assertTrue(scale >= .76f && scale < 1f);
                assertTrue(MemoryStoryRowLayout.footerHeight(c, scale) < MemoryStoryRowLayout.footerHeight(c));
                assertTrue(MemoryStoryRowLayout.dateTextSize(scale) >= 22f);
                assertEquals(1012f, layout.card.width(), .01f);
                MemoryStoryComposer.Box footer = MemoryStoryComposer.footerBox(layout.card, layout.slots, c);
                for (int i = 0; i < count; i++) {
                    MemoryStoryComposer.Box slot = layout.slots.get(i);
                    assertEquals(1.5f, slot.width() / slot.height(), .001f);
                    double angle = Math.toRadians(Math.abs(MemoryStoryRowLayout.angle(c, i)));
                    double rh = slot.height() * Math.cos(angle) + slot.width() * Math.sin(angle);
                    assertTrue((slot.top + slot.bottom + rh) / 2 < footer.top);
                }
            }
        }
    }

    @Test public void portraitMixedAndFourPhotoFooterSizesStayUnchanged() {
        for (int count = 2; count <= 4; count++) {
            int[] widths = new int[count], heights = new int[count];
            Arrays.fill(widths, 600); Arrays.fill(heights, 900);
            MemoryStoryConfig c = config(MemoryStoryConfig.LayoutMode.SCRAPBOOK);
            assertEquals(1f, MemoryStoryRowLayout.footerScale(MemoryStoryRowLayout.compute(widths, heights, c).slots), 0);
            Arrays.fill(widths, 900); Arrays.fill(heights, 600);
            heights[0] = 1200;
            assertEquals(1f, MemoryStoryRowLayout.footerScale(MemoryStoryRowLayout.compute(widths, heights, c).slots), 0);
        }
        assertEquals(1f, MemoryStoryRowLayout.footerScale(200f, 4, true), 0);
    }

    private MemoryStoryConfig config(MemoryStoryConfig.LayoutMode mode) {
        return new MemoryStoryConfig(mode, true, "예술의전당", "11월 21일",
                MemoryStoryConfig.FontKind.SANS, MemoryStoryConfig.ThemeTone.CREAM, 1.6f);
    }

    @Test public void twoToFourPortraitsDefaultToRowAndOtherSelectionsKeepLegacyDefaults() {
        MemoryStoryTemplateSelection selection = new MemoryStoryTemplateSelection();
        for (int count = 2; count <= 4; count++) {
            int[] widths = new int[count], heights = new int[count];
            Arrays.fill(widths, 300); Arrays.fill(heights, 400);
            assertEquals(MemoryStoryConfig.LayoutMode.PORTRAIT_ROW, selection.resolve(widths, heights));
        }
        assertEquals(MemoryStoryConfig.LayoutMode.PHOTO_STRIP, selection.resolve(new int[]{400, 500}, new int[]{300, 300}));
        assertEquals(MemoryStoryConfig.LayoutMode.PHOTO_STRIP, selection.resolve(new int[]{300, 500}, new int[]{400, 300}));
        assertEquals(MemoryStoryConfig.LayoutMode.SINGLE_FRAME, selection.resolve(new int[]{300}, new int[]{400}));
    }

    @Test public void manualLayoutSurvivesReorderAndCompatibleChangesAndResetsWhenIncompatible() {
        MemoryStoryTemplateSelection selection = new MemoryStoryTemplateSelection();
        selection.select(MemoryStoryConfig.LayoutMode.SCRAPBOOK);
        assertEquals(MemoryStoryConfig.LayoutMode.SCRAPBOOK, selection.resolve(new int[]{300, 200}, new int[]{400, 500}));
        assertEquals(MemoryStoryConfig.LayoutMode.SCRAPBOOK, selection.resolve(new int[]{200, 300, 200}, new int[]{500, 400, 400}));
        assertEquals(MemoryStoryConfig.LayoutMode.SCRAPBOOK, selection.resolve(new int[]{600, 300}, new int[]{400, 500}));
        assertEquals(MemoryStoryConfig.LayoutMode.SINGLE_FRAME, selection.resolve(new int[]{300}, new int[]{400}));
        assertEquals(MemoryStoryConfig.LayoutMode.PORTRAIT_ROW, selection.resolve(new int[]{200, 300}, new int[]{500, 400}));
        selection.select(MemoryStoryConfig.LayoutMode.PHOTO_STRIP);
        assertEquals(MemoryStoryConfig.LayoutMode.PHOTO_STRIP, selection.resolve(new int[]{200, 300}, new int[]{500, 400}));
        assertTrue(MemoryStoryTemplateSelection.options(new int[]{300, 600, 200}, new int[]{400, 400, 400})
                .contains(MemoryStoryConfig.LayoutMode.SCRAPBOOK));
        assertTrue(MemoryStoryTemplateSelection.options(new int[]{300, 600}, new int[]{400, 600})
                .contains(MemoryStoryConfig.LayoutMode.PORTRAIT_ROW));
        assertFalse(MemoryStoryTemplateSelection.options(new int[]{300, 300}, new int[]{400, 400})
                .contains(MemoryStoryConfig.LayoutMode.SINGLE_FRAME));
    }

    @Test public void portraitCardsUseWidthAndKeepCaptionCloseForTwoToFourPhotos() {
        for (int count = 2; count <= 4; count++) {
            int[] widths = new int[count], heights = new int[count];
            Arrays.fill(widths, 600); Arrays.fill(heights, 900);
            MemoryStoryConfig config = config(MemoryStoryConfig.LayoutMode.SCRAPBOOK);
            MemoryStoryComposer.StripLayout layout = MemoryStoryRowLayout.compute(widths, heights, config);
            MemoryStoryComposer.Box footer = MemoryStoryComposer.footerBox(layout.card, layout.slots, config);
            double bottom = 0;
            float photoWidth = 0;
            for (int i = 0; i < count; i++) {
                MemoryStoryComposer.Box slot = layout.slots.get(i);
                double a = Math.toRadians(Math.abs(MemoryStoryRowLayout.angle(config, i)));
                double rh = slot.height() * Math.cos(a) + slot.width() * Math.sin(a);
                bottom = Math.max(bottom, (slot.top + slot.bottom + rh) / 2);
                photoWidth += slot.width();
            }
            assertTrue(photoWidth / layout.card.width() > .86f);
            assertTrue(footer.top - bottom >= 0 && footer.top - bottom < 45);
            assertTrue(layout.slots.get(0).height() > layout.slots.get(0).width());
        }
    }

    @Test public void photosKeepAspectAndOrderAndRotatedFramesStayAboveFooter() {
        for (MemoryStoryConfig.LayoutMode mode : new MemoryStoryConfig.LayoutMode[]{
                MemoryStoryConfig.LayoutMode.PORTRAIT_ROW, MemoryStoryConfig.LayoutMode.SCRAPBOOK}) {
            for (int count = 2; count <= 4; count++) {
                int[] w = Arrays.copyOf(new int[]{900, 500, 1200, 400}, count);
                int[] h = Arrays.copyOf(new int[]{400, 900, 1100, 600}, count);
                MemoryStoryConfig config = config(mode);
                assertTrue(MemoryStoryRowLayout.footerHeight(config) < MemoryStoryComposer.singleFooterHeight(config));
                MemoryStoryComposer.StripLayout layout = MemoryStoryComposer.computeStripLayout(w, h, config);
                MemoryStoryComposer.Box footer = MemoryStoryComposer.footerBox(layout.card, layout.slots, config);
                float previousRight = layout.card.left;
                double previousPhotoRight = layout.card.left;
                assertTrue(layout.card.top >= 0 && layout.card.bottom <= 1920);
                assertTrue(layout.card.left >= 0 && layout.card.right <= 1080);
                if (count == 4) {
                    assertTrue(layout.slots.get(2).top > Math.max(layout.slots.get(0).bottom, layout.slots.get(1).bottom));
                    assertTrue(layout.slots.get(3).top > Math.max(layout.slots.get(0).bottom, layout.slots.get(1).bottom));
                }
                for (int i = 0; i < count; i++) {
                    MemoryStoryComposer.Box slot = layout.slots.get(i);
                    if (count == 4 && i == 2) {
                        previousRight = layout.card.left;
                        previousPhotoRight = layout.card.left;
                    }
                    assertEquals(w[i] / (float) h[i], slot.width() / slot.height(), .001f);
                    float edge = 0;
                    double angle = Math.toRadians(Math.abs(MemoryStoryRowLayout.angle(config, i)));
                    double rw = (slot.width() + edge * 2) * Math.cos(angle) + (slot.height() + edge * 2) * Math.sin(angle);
                    double rh = (slot.height() + edge * 2) * Math.cos(angle) + (slot.width() + edge * 2) * Math.sin(angle);
                    double left = (slot.left + slot.right - rw) / 2, right = left + rw;
                    // Compare neighbours within each row, allowing floating point rounding.
                    float overlap = .01f;
                    assertTrue(left >= previousRight - overlap);
                    double photoHalfWidth = (slot.width() * Math.cos(angle) + slot.height() * Math.sin(angle)) / 2;
                    double photoCenter = (slot.left + slot.right) / 2;
                    assertTrue(photoCenter - photoHalfWidth >= previousPhotoRight - overlap);
                    previousPhotoRight = photoCenter + photoHalfWidth;
                    assertTrue(right < layout.card.right);
                    assertTrue((slot.top + slot.bottom - rh) / 2 > layout.card.top);
                    assertTrue((slot.top + slot.bottom + rh) / 2 < footer.top);
                    previousRight = (float) right;
                }
            }
        }
    }
}
