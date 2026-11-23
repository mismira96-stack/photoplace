package com.example.gallerysorter;
import org.junit.Test;
import static org.junit.Assert.*;
public class MemoryStoryCaptionTest {
    @Test public void captionDoesNotRepeatPlaceOrChangeDates() {
        assertEquals("성남에서", MemoryStoryCaption.place("성남"));
        assertEquals("성남에서", MemoryStoryCaption.place("성남에서"));
        assertEquals("11월 8일", MemoryStoryCaption.date("2026년 11월 8일"));
        assertEquals("11월 8일", MemoryStoryCaption.date("2026.11.08"));
        assertEquals("11월 8일", MemoryStoryCaption.date("2026-11-08"));
        assertEquals("11월 8일", MemoryStoryCaption.date("11월 8일"));
        assertEquals("", MemoryStoryCaption.date(null));
        assertEquals("날짜 없음", MemoryStoryCaption.date("날짜 없음"));
    }
    @Test public void cardAlwaysCentersAndPreservesNativeAspect() {
        MemoryStoryConfig config = new MemoryStoryConfig(MemoryStoryConfig.LayoutMode.SINGLE_FRAME,
                true, "성남에서", "11월 8일", MemoryStoryConfig.FontKind.HAND,
                MemoryStoryConfig.ThemeTone.CREAM, 1.6f);
        for (int[] dim : new int[][]{{1600,900},{900,1600},{1000,1000},{100,2000},{2000,100}}) {
            MemoryStoryComposer.Box photo = MemoryStoryComposer.computeSinglePhotoBox(dim[0],dim[1],config);
            MemoryStoryComposer.Box card = MemoryStoryComposer.computeSingleCardBox(dim[0],dim[1],config);
            assertEquals(dim[0]/(float)dim[1],photo.width()/photo.height(), .001f);
            assertEquals(540f,(card.left+card.right)/2,.001f);
            assertEquals(960f,(card.top+card.bottom)/2,.001f);
            assertTrue(card.left >= 0 && card.right <= 1080 && card.top >= 0 && card.bottom <= 1920);
        }
    }
}
