package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;

public class MediaAnalysisStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void savesAndReadsAnalyzedAndNoLocationEntries() throws Exception {
        MediaAnalysisStore store = new MediaAnalysisStore(temporaryFolder.newFolder("cache"));
        MediaAnalysisEntry analyzed = new MediaAnalysisEntry("image-signature", MediaAnalysisEntry.STATUS_ANALYZED,
                1786000000000L, "삿포로", "JP", "Japan", "Hokkaido", "Sapporo", 1);
        MediaAnalysisEntry noLocation = new MediaAnalysisEntry("video-signature", MediaAnalysisEntry.STATUS_NO_LOCATION,
                1786000000001L, "위치없음", "", "", "", "", 1);

        assertTrue(store.saveAll(Arrays.asList(analyzed, noLocation)));
        assertEquals("삿포로", store.get("image-signature").folderKey);
        assertEquals(MediaAnalysisEntry.STATUS_NO_LOCATION, store.get("video-signature").status);
        assertEquals(2, store.readAll().size());
    }

    @Test
    public void changedSignatureDoesNotReusePriorAnalysis() throws Exception {
        MediaAnalysisStore store = new MediaAnalysisStore(temporaryFolder.newFolder("signature"));
        String before = MediaAnalysisSignature.build("content://media/1", "IMG.jpg", 100L, 90L, 80L, false, "Camera");
        String after = MediaAnalysisSignature.build("content://media/1", "IMG.jpg", 101L, 90L, 80L, false, "Camera");
        assertTrue(store.saveAll(Arrays.asList(new MediaAnalysisEntry(before, MediaAnalysisEntry.STATUS_ANALYZED,
                80L, "성남", "KR", "대한민국", "경기", "", 1))));

        assertEquals("성남", store.get(before).folderKey);
        assertNull(store.get(after));
    }
}
