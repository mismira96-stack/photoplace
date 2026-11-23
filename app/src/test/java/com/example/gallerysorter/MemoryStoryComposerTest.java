package com.example.gallerysorter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryStoryComposerTest {


    @Test public void singleCardFollowsLandscapePhotoWithoutTallEmptyPaper() {
        MemoryStoryConfig config = new MemoryStoryConfig(MemoryStoryConfig.LayoutMode.SINGLE_FRAME,
                true, "성남", "11월 8일", MemoryStoryConfig.FontKind.SANS,
                MemoryStoryConfig.ThemeTone.CREAM, 1f);
        MemoryStoryComposer.Box photo = MemoryStoryComposer.computeSinglePhotoBox(1600, 900, config);
        MemoryStoryComposer.Box card = MemoryStoryComposer.computeSingleCardBox(1600, 900, config);
        assertEquals(16f/9f, photo.width()/photo.height(), .001f);
        assertTrue(photo.width() >= 940f);
        assertTrue(card.width() > card.height());
        assertEquals(24f, photo.top-card.top, .001f);
        assertEquals(MemoryStoryComposer.singleFooterHeight(config), card.bottom-photo.bottom, .001f);
        assertTrue(card.left >= 0 && card.right <= 1080 && card.top >= 0 && card.bottom <= 1920);
        MemoryStoryComposer.Box portrait = MemoryStoryComposer.computeSingleCardBox(600, 900, config);
        assertTrue(portrait.height() > card.height());
    }

    @Test
    public void singlePhotoBoxPreservesAspectRatioAndLeavesFooterSpaceWhenTextEnabled() {
        MemoryStoryConfig photoOnly = new MemoryStoryConfig(
                MemoryStoryConfig.LayoutMode.SINGLE_FRAME,
                false,
                "성남 나들이",
                "성남 · 2026.11.08",
                MemoryStoryConfig.FontKind.SANS,
                MemoryStoryConfig.ThemeTone.CREAM,
                1.0f);
        MemoryStoryConfig withText = photoOnly.withIncludeContext(true);

        MemoryStoryComposer.Box landscapeOnly = MemoryStoryComposer.computeSinglePhotoBox(4000, 3000, photoOnly);
        MemoryStoryComposer.Box landscapeWithText = MemoryStoryComposer.computeSinglePhotoBox(4000, 3000, withText);

        assertEquals(4f / 3f, landscapeOnly.width() / landscapeOnly.height(), 0.01f);
        assertEquals(4f / 3f, landscapeWithText.width() / landscapeWithText.height(), 0.01f);
        assertTrue(landscapeWithText.bottom <= 1540f);
    }

    @Test
    public void photoStripSlotsFitInsideOuterStripAndReserveCaptionArea() {
        MemoryStoryConfig stripConfig = new MemoryStoryConfig(
                MemoryStoryConfig.LayoutMode.PHOTO_STRIP,
                true,
                "2026 삿포로 여행",
                "Sapporo · 2026.08.06",
                MemoryStoryConfig.FontKind.HAND,
                MemoryStoryConfig.ThemeTone.CREAM,
                1.2f);

        for (int count = 2; count <= 4; count++) {
            MemoryStoryComposer.Box outer = MemoryStoryComposer.computeStripOuterBox(count, stripConfig);
            List<MemoryStoryComposer.Box> slots = MemoryStoryComposer.computeStripPhotoSlots(count, stripConfig);
            assertEquals(count, slots.size());
            for (int i = 0; i < slots.size(); i++) {
                MemoryStoryComposer.Box s = slots.get(i);
                assertTrue(s.left > outer.left && s.right < outer.right);
                assertTrue(s.top > outer.top && s.bottom < outer.bottom - 150f);
                if (i > 0) {
                    assertTrue(s.top > slots.get(i - 1).bottom);
                }
            }
        }
    }

    @Test public void fourStripPhotosFillInnerWidthAndStackInOrderWithoutLetterboxing() {
        MemoryStoryConfig c=new MemoryStoryConfig(MemoryStoryConfig.LayoutMode.PHOTO_STRIP,true,"성남에서","11월 8일",MemoryStoryConfig.FontKind.SANS,MemoryStoryConfig.ThemeTone.CREAM,1f);
        int[] w={600,1600,600,900},h={900,900,800,600};
        MemoryStoryComposer.StripLayout layout=MemoryStoryComposer.computeStripLayout(w,h,c);
        assertEquals(4,layout.slots.size());
        for(int i=0;i<4;i++){MemoryStoryComposer.Box slot=layout.slots.get(i);assertEquals(w[i]/(float)h[i],slot.width()/slot.height(),.001f);}
        for(int i=0;i<4;i++) {
            assertEquals(layout.card.width()-48,layout.slots.get(i).width(),.001f);
            assertEquals(layout.card.left+24,layout.slots.get(i).left,.001f);
            if(i>0)assertTrue(layout.slots.get(i).top>layout.slots.get(i-1).bottom);
        }
        assertEquals(24,layout.slots.get(0).left-layout.card.left,.001f);
        assertEquals(MemoryStoryComposer.singleFooterHeight(c),layout.card.bottom-layout.slots.get(3).bottom,.001f);
        assertTrue(layout.card.height()<=1760.1f);
    }
    @Test public void twoAndThreePortraitsPackAtNaturalRatioWithinStoryBounds() {
        MemoryStoryConfig c=new MemoryStoryConfig(MemoryStoryConfig.LayoutMode.PHOTO_STRIP,true,"성남에서","11월 8일",MemoryStoryConfig.FontKind.HAND,MemoryStoryConfig.ThemeTone.CREAM,1.6f);
        for(int count=2;count<=3;count++) {
            int[] w=new int[count],h=new int[count];java.util.Arrays.fill(w,600);java.util.Arrays.fill(h,900);
            MemoryStoryComposer.StripLayout layout=MemoryStoryComposer.computeStripLayout(w,h,c);
            for(MemoryStoryComposer.Box slot:layout.slots)assertEquals(2f/3f,slot.width()/slot.height(),.001f);
            assertTrue(layout.card.height()<=1760.1f);assertTrue(layout.card.top>=0&&layout.card.bottom<=1920);
        }
    }

    @Test
    public void configNormalizesKoreanDateAndClampsTextScale() {
        assertEquals("성남 · 2026.11.08",
                MemoryStoryConfig.formatPlaceAndDate("성남", "2026년 11월 8일"));
        assertEquals("Sapporo · 2026.08.06",
                MemoryStoryConfig.formatPlaceAndDate("Sapporo", "2026-08-06"));
        assertEquals(0.60f, MemoryStoryConfig.clampScale(0.20f), 0.001f);
        assertEquals(1.60f, MemoryStoryConfig.clampScale(3.00f), 0.001f);
    }



}
