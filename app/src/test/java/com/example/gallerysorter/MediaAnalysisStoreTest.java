package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;

public class MediaAnalysisStoreTest {
    @Test
    public void savesAndRestoresAnalyzedLocation() throws Exception {
        File directory = Files.createTempDirectory("media-analysis").toFile();
        MediaAnalysisStore store = new MediaAnalysisStore(directory);
        String signature = "analysis|sample";

        store.remember(signature, new LocationResult(
                new Date(1710000000000L), "삿포로", "JP", "Japan", "Hokkaido", "Sapporo"));
        assertTrue(store.flush());

        LocationResult restored = new MediaAnalysisStore(directory).cachedResult(signature, false);
        assertEquals("삿포로", restored.folderKey);
        assertEquals("JP", restored.countryCode);
        assertEquals(1710000000000L, restored.takenAt.getTime());
    }

    @Test
    public void noLocationIsNotReusedWhenMediaStoreNowHasCoordinates() throws Exception {
        File directory = Files.createTempDirectory("media-analysis").toFile();
        MediaAnalysisStore store = new MediaAnalysisStore(directory);
        store.remember("analysis|no-location", new LocationResult(null, PlaceNamePolicy.LOCATION_NONE, "", "", ""));

        assertEquals(PlaceNamePolicy.LOCATION_NONE,
                store.cachedResult("analysis|no-location", false).folderKey);
        assertNull(store.cachedResult("analysis|no-location", true));
    }

    @Test
    public void signatureChangeDoesNotReuseOlderResult() throws Exception {
        File directory = Files.createTempDirectory("media-analysis").toFile();
        MediaAnalysisStore store = new MediaAnalysisStore(directory);
        store.remember("analysis|old", new LocationResult(null, "성남", "KR", "대한민국", "경기도", "성남시"));

        assertNull(store.cachedResult("analysis|changed", false));
    }
}
