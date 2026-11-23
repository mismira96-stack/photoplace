package com.example.gallerysorter;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class CleanupHistoricalScannerTest {

    @Test
    public void normalizePath_handlesVariousPathFormats() {
        assertEquals("", CleanupHistoricalScanner.normalizePath(null));
        assertEquals("", CleanupHistoricalScanner.normalizePath(""));
        assertEquals("", CleanupHistoricalScanner.normalizePath("   "));
        assertEquals("Pictures/용인에서/", CleanupHistoricalScanner.normalizePath("Pictures/용인에서"));
        assertEquals("Pictures/용인에서/", CleanupHistoricalScanner.normalizePath("Pictures/용인에서/"));
        assertEquals("Pictures/용인에서/", CleanupHistoricalScanner.normalizePath("/Pictures/용인에서/"));
        assertEquals("Pictures/용인에서/", CleanupHistoricalScanner.normalizePath("Pictures\\용인에서\\"));
        assertEquals("Download/", CleanupHistoricalScanner.normalizePath("Download"));
    }

    @Test
    public void isTargetAlbumPath_identifiesKnownAndPatternAlbums() {
        List<String> known = Arrays.asList("Pictures/용인에서/", "Pictures/성남에서/");

        // Target album matches
        assertTrue(CleanupHistoricalScanner.isTargetAlbumPath("Pictures/용인에서/", known));
        assertTrue(CleanupHistoricalScanner.isTargetAlbumPath("Pictures/용인에서", known));
        assertTrue(CleanupHistoricalScanner.isTargetAlbumPath("Pictures/성남에서/", known));
        assertTrue(CleanupHistoricalScanner.isTargetAlbumPath("Pictures/태백에서/", known)); // Pattern match Pictures/...에서/
        assertTrue(CleanupHistoricalScanner.isTargetAlbumPath("Pictures/강남구에서/", Collections.emptyList()));
        assertTrue(CleanupHistoricalScanner.isTargetAlbumPath("Pictures/Kurume에서/", Collections.emptyList()));
        assertTrue(CleanupHistoricalScanner.isTargetAlbumPath("Pictures/서초에서/", Collections.emptyList()));

        // Non-target albums (potential original sources) must return false
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("DCIM/Camera/", known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("DCIM/Camera", known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("Download/", known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("Download/Quick Share/", known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("DCIM/앨범 3/", known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("Pictures/band/", known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("Pictures/Screenshots/", known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("PhotoGrid/", known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("SNOW/", known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath(null, known));
        assertFalse(CleanupHistoricalScanner.isTargetAlbumPath("", known));
    }
}
