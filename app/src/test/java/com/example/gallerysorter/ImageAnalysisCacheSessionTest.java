package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Collections;

public class ImageAnalysisCacheSessionTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void analyzedEntryRestoresTheNormalLocationResult() {
        MediaAnalysisEntry entry = analyzedEntry("photo-1");
        ImageAnalysisCacheSession session = new ImageAnalysisCacheSession(
                Collections.singletonMap(entry.signature, entry));

        LocationResult result = session.cachedResult(entry.signature, false);

        assertEquals("삿포로", result.folderKey);
        assertEquals("JP", result.countryCode);
        assertEquals("Hokkaido", result.adminArea);
        assertEquals(1786000000000L, result.takenAt.getTime());
    }

    @Test
    public void noLocationEntryStillRestoresAUsableNoLocationResult() {
        MediaAnalysisEntry entry = new MediaAnalysisEntry("photo-2", MediaAnalysisEntry.STATUS_NO_LOCATION,
                1786000000001L, "위치없음", "", "", "", "", ImageAnalysisCacheSession.POLICY_VERSION);
        ImageAnalysisCacheSession session = new ImageAnalysisCacheSession(
                Collections.singletonMap(entry.signature, entry));

        LocationResult result = session.cachedResult(entry.signature, false);

        assertEquals(LocationLookupResult.LOCATION_NONE, result.folderKey);
        assertEquals(1786000000001L, result.takenAt.getTime());
    }

    @Test
    public void currentMediaStoreGpsInvalidatesNoLocationEntry() {
        MediaAnalysisEntry entry = new MediaAnalysisEntry("photo-3", MediaAnalysisEntry.STATUS_NO_LOCATION,
                0L, "위치없음", "", "", "", "", ImageAnalysisCacheSession.POLICY_VERSION);
        ImageAnalysisCacheSession session = new ImageAnalysisCacheSession(
                Collections.singletonMap(entry.signature, entry));

        assertNull(session.cachedResult(entry.signature, true));
        assertTrue(session.cachedResult(entry.signature, false) != null);
    }

    @Test
    public void changedSignatureIsAMissWhileExistingEntriesRemainAvailable() {
        MediaAnalysisEntry entry = analyzedEntry("before");
        ImageAnalysisCacheSession session = new ImageAnalysisCacheSession(
                Collections.singletonMap(entry.signature, entry));

        assertNull(session.cachedResult("after", false));
        assertEquals("삿포로", session.cachedResult("before", false).folderKey);
    }

    @Test
    public void policyVersionMismatchIsACacheMiss() {
        MediaAnalysisEntry outdated = new MediaAnalysisEntry("photo-4", MediaAnalysisEntry.STATUS_ANALYZED,
                1786000000000L, "삿포로", "JP", "Japan", "Hokkaido", "Sapporo",
                ImageAnalysisCacheSession.POLICY_VERSION + 1);
        ImageAnalysisCacheSession session = new ImageAnalysisCacheSession(
                Collections.singletonMap(outdated.signature, outdated));

        assertNull(session.cachedResult(outdated.signature, false));
    }

    @Test
    public void emptySignaturesAreIgnoredSafely() {
        ImageAnalysisCacheSession session = new ImageAnalysisCacheSession(Collections.<String, MediaAnalysisEntry>emptyMap());

        assertNull(session.cachedResult(null, false));
        session.remember("", new LocationResult(null, "성남", "KR", "대한민국", "경기", "성남"));
        assertTrue(session.entriesForSave().isEmpty());
    }

    @Test
    public void stagedEntriesAreNotPersistedUntilTheCallerCommitsThem() throws Exception {
        MediaAnalysisStore store = new MediaAnalysisStore(temporaryFolder.newFolder("staging"));
        ImageAnalysisCacheSession session = new ImageAnalysisCacheSession(store.readAll());
        session.remember("new-photo", new LocationResult(null, "성남", "KR", "대한민국", "경기", "성남"));

        assertTrue(store.readAll().isEmpty());
        assertFalse(session.entriesForSave().isEmpty());
        assertTrue(store.saveAll(session.entriesForSave()));
        assertEquals("성남", store.get("new-photo").folderKey);
    }

    private static MediaAnalysisEntry analyzedEntry(String signature) {
        return new MediaAnalysisEntry(signature, MediaAnalysisEntry.STATUS_ANALYZED,
                1786000000000L, "삿포로", "JP", "Japan", "Hokkaido", "Sapporo",
                ImageAnalysisCacheSession.POLICY_VERSION);
    }
}
