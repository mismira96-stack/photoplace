package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryScrollAnchorTest {
    @Test public void unchangedCardRestoresSameScrollPositionIncludingPartialVisibility() {
        MemoryScrollAnchor saved = new MemoryScrollAnchor("place|a", "albums|", -80, 1200);
        assertEquals(1200, saved.restoreY(1120, 9000));
    }

    @Test public void newBadgeReorderKeepsSameCardAtItsPreviousViewportOffset() {
        MemoryScrollAnchor saved = new MemoryScrollAnchor("place|a", "albums|", 150, 800);
        assertEquals(3850, saved.restoreY(4000, 9000));
        assertEquals(150, 4000 - saved.restoreY(4000, 9000));
    }

    @Test public void removedOrHiddenCardFallsBackAndShrunkenContentClampsSafely() {
        MemoryScrollAnchor saved = new MemoryScrollAnchor("place|a", "albums|", 150, 800);
        assertEquals(800, saved.restoreY(null, 9000));
        assertEquals(200, saved.restoreY(null, 200));
        assertEquals(0, saved.restoreY(30, 9000));
        assertEquals(0, saved.restoreY(null, -10));
    }

    @Test public void sourceScopeAndChangedSearchCannotInheritAnUnrelatedPosition() {
        MemoryScrollAnchor saved = new MemoryScrollAnchor("place|a", MemoryScrollAnchor.scope(true, "2017"), 1, 2);
        assertTrue(saved.matches(true, " 2017 "));
        assertFalse(saved.matches(false, "2017"));
        assertFalse(saved.matches(true, "2015"));
        assertEquals(MemoryScrollAnchor.scope(true, null), MemoryScrollAnchor.scope(true, ""));
    }
}
