package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryCardCountPresentationTest {
    @Test public void compactPhotoVideoLabelsKeepExactLargeNumbersAndFullDescription() {
        MemoryCardCountPresentation p = MemoryCardCountPresentation.from("사진 2252장 · 동영상 31개");
        assertEquals("2,252", p.photos);
        assertEquals("31", p.videos);
        assertEquals("사진 2252장 · 동영상 31개", p.description);
        MemoryCardCountPresentation large = MemoryCardCountPresentation.from("사진 120123장 · 동영상 1005개");
        assertEquals("120,123", large.photos);
        assertEquals("1,005", large.videos);
    }

    @Test public void singleKindAndZeroCountsDoNotInventAnotherKind() {
        assertEquals("246", MemoryCardCountPresentation.from("사진 246장").photos);
        assertEquals("", MemoryCardCountPresentation.from("사진 246장").videos);
        assertEquals("11", MemoryCardCountPresentation.from("동영상 11개").videos);
        assertEquals("", MemoryCardCountPresentation.from("동영상 11개").photos);
        assertEquals("0", MemoryCardCountPresentation.from("사진 0장").photos);
    }

    @Test public void unknownKindTotalsAndUnavailableMediaStayHonest() {
        MemoryCardCountPresentation total = MemoryCardCountPresentation.from("사진·동영상 2107개");
        assertEquals("사진·동영상 합계 2,107개", total.fallback);
        assertEquals("", total.photos);
        assertEquals("", total.videos);
        assertEquals("불러오기 필요", MemoryCardCountPresentation.from("불러오기 필요").fallback);
        assertEquals("custom count", MemoryCardCountPresentation.from("custom count").fallback);
        assertEquals("", MemoryCardCountPresentation.from(null).fallback);
    }

    @Test public void visibleTextIncludesPhotoAndVideoUnitsWithoutIcons() {
        assertEquals("사진 996장 · 동영상 12개",
                MemoryCardCountPresentation.from("사진 996장 · 동영상 12개").displayText());
        assertEquals("사진 1,234장", MemoryCardCountPresentation.from("사진 1234장").displayText());
        assertEquals("동영상 9개", MemoryCardCountPresentation.from("동영상 9개").displayText());
        assertEquals("사진·동영상 합계 121개",
                MemoryCardCountPresentation.from("사진·동영상 121개").displayText());
    }

    @Test public void iconLabelsRetainUnitsWhileTalkBackRetainsMediaNames() {
        MemoryCardCountPresentation mixed = MemoryCardCountPresentation.from("사진 996장 · 동영상 12개");
        assertEquals("996장", mixed.photoLabel());
        assertEquals("12개", mixed.videoLabel());
        assertEquals("사진 996장 · 동영상 12개", mixed.displayText());
        assertEquals("", MemoryCardCountPresentation.from("동영상 3개").photoLabel());
        assertEquals("", MemoryCardCountPresentation.from("사진 4장").videoLabel());
        assertEquals("", MemoryCardCountPresentation.from("사진·동영상 121개").photoLabel());
    }
}
