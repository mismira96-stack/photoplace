package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryLoadingNoticeLayoutTest {
    @Test public void unfoldedNoticeUsesCenteredContentWidthAndClearsActualTabBoundary() {
        MemoryLoadingNoticeLayout p = MemoryLoadingNoticeLayout.calculate(932, 704, 0, 932, 580, 600, 18, 12);
        assertEquals(564, p.width);
        assertEquals(184, p.leftMargin);
        assertEquals(136, p.bottomMargin);
        assertEquals(568, 704 - p.bottomMargin); // 12px above the 580px tab boundary.
    }

    @Test public void foldedNoticeFitsViewportAndFollowsChangingNavigationHeight() {
        MemoryLoadingNoticeLayout old = MemoryLoadingNoticeLayout.calculate(475, 751, 0, 475, 628, 600, 18, 12);
        MemoryLoadingNoticeLayout resized = MemoryLoadingNoticeLayout.calculate(475, 751, 0, 475, 590, 600, 18, 12);
        assertEquals(439, old.width);
        assertEquals(18, old.leftMargin);
        assertEquals(135, old.bottomMargin);
        assertEquals(173, resized.bottomMargin);
    }

    @Test public void sideInsetOrSplitWindowKeepsNoticeWithinItsOwnViewport() {
        MemoryLoadingNoticeLayout p = MemoryLoadingNoticeLayout.calculate(1000, 700, 80, 800, 600, 600, 18, 12);
        assertEquals(198, p.leftMargin);
        assertEquals(564, p.width);
        assertEquals(112, p.bottomMargin);
        MemoryLoadingNoticeLayout tiny = MemoryLoadingNoticeLayout.calculate(20, 30, 0, 20, 90, 600, 18, 12);
        assertEquals(1, tiny.width);
        assertTrue(tiny.leftMargin >= 0 && tiny.leftMargin + tiny.width <= 20);
    }
}
