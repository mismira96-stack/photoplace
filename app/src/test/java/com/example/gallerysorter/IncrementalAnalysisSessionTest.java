package com.example.gallerysorter;

import org.junit.Test;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class IncrementalAnalysisSessionTest {
    private LocationResult located() { return new LocationResult(new Date(100), "성남", "KR", "대한민국", "경기", ""); }

    @Test public void newPhotoAlongsideCachedPhotoAndVideoOnlyAnalyzesTheNewPhoto() throws Exception {
        Map<String, MediaAnalysisEntry> saved = new LinkedHashMap<>();
        IncrementalAnalysisSession first = new IncrementalAnalysisSession(saved);
        first.resolve("i|old", "row1", false, this::located);
        first.resolve("v|old", "row2", false, this::located);
        first.finish(true, entries -> { for (MediaAnalysisEntry e : entries) saved.put(e.signature, e); return true; });
        IncrementalAnalysisSession next = new IncrementalAnalysisSession(saved);
        assertEquals("성남", next.resolve("i|old", "row1", false, () -> { fail("Old photo reanalyzed"); return null; }).folderKey);
        next.resolve("v|old", "row2", false, () -> { fail("Old video reanalyzed"); return null; });
        next.resolve("i|new", "row3", false, this::located);
        assertEquals(2, next.hits);
        assertEquals(1, next.analyzed);
    }

    @Test public void fingerprintChangeReanalyzesWithoutFallingBackToLegacy() throws Exception {
        Map<String, MediaAnalysisEntry> saved = new LinkedHashMap<>();
        MediaAnalysisEntry old = MediaAnalysisEntry.fromLocationResult("i|old", located(), 1);
        saved.put(old.signature, old);
        IncrementalAnalysisSession migration = new IncrementalAnalysisSession(saved);
        migration.resolve("i|old", "size10-gen1", false, () -> { fail("Should migrate"); return null; });
        assertEquals(1, migration.hits);
        migration.finish(true, entries -> { saved.clear(); for (MediaAnalysisEntry e : entries) saved.put(e.signature, e); return true; });
        IncrementalAnalysisSession changed = new IncrementalAnalysisSession(saved);
        changed.resolve("i|old", "size11-gen2", false, this::located);
        assertEquals(1, changed.analyzed);
        assertEquals(0, changed.hits);
    }

    @Test public void cancelledOrFailedRunDoesNotWriteStagedResults() throws Exception {
        IncrementalAnalysisSession session = new IncrementalAnalysisSession(Collections.emptyMap());
        session.resolve("v|new", "row", false, this::located);
        assertFalse(session.finish(false, entries -> { fail("Incomplete run wrote cache"); return true; }));
        try { session.resolve("v|failed", "row", false, () -> { throw new Exception("read failed"); }); fail(); }
        catch (Exception expected) { assertEquals("read failed", expected.getMessage()); }
    }

    @Test public void noLocationVideoIsCachedButNewGpsForcesReanalysis() throws Exception {
        Map<String, MediaAnalysisEntry> saved = new LinkedHashMap<>();
        IncrementalAnalysisSession first = new IncrementalAnalysisSession(saved);
        first.resolve("v|old", "row", false, () -> new LocationResult(null, LocationLookupResult.LOCATION_NONE, "", "", ""));
        first.finish(true, entries -> { for (MediaAnalysisEntry e : entries) saved.put(e.signature, e); return true; });
        IncrementalAnalysisSession next = new IncrementalAnalysisSession(saved);
        assertEquals(LocationLookupResult.LOCATION_NONE, next.resolve("v|old", "row", false, () -> { fail(); return null; }).folderKey);
        assertEquals("성남", next.resolve("v|old", "row", true, this::located).folderKey);
        assertEquals(1, next.analyzed);
    }

    @Test public void unchangedCacheDoesNotRewriteAndSaveFailureIsReported() throws Exception {
        Map<String, MediaAnalysisEntry> entries = new LinkedHashMap<>();
        entries.put("i|old|row-v2=row", MediaAnalysisEntry.fromLocationResult("i|old|row-v2=row", located(), 1));
        IncrementalAnalysisSession session = new IncrementalAnalysisSession(entries);
        session.resolve("i|old", "row", false, this::located);
        assertTrue(session.finish(true, data -> { fail("Unchanged cache rewritten"); return false; }));
        session.resolve("i|new", "row", false, this::located);
        assertFalse(session.finish(true, data -> false));
    }

    @Test public void obsoletePolicyCannotReuseFingerprintOrLegacyAnalysis() throws Exception {
        Map<String, MediaAnalysisEntry> saved = new LinkedHashMap<>();
        int obsoletePolicy = ImageAnalysisCacheSession.POLICY_VERSION + 1;
        String base = "i|old";
        saved.put(base, MediaAnalysisEntry.fromLocationResult(base, located(), obsoletePolicy));
        saved.put(base + "|row-v2=row", MediaAnalysisEntry.fromLocationResult(
                base + "|row-v2=row", located(), obsoletePolicy));
        IncrementalAnalysisSession session = new IncrementalAnalysisSession(saved);
        LocationResult corrected = new LocationResult(new Date(200), "하남", "KR", "대한민국", "경기", "");
        assertEquals("하남", session.resolve(base, "row", false, () -> corrected).folderKey);
        assertEquals(1, session.analyzed);
        assertEquals(0, session.hits);
        assertTrue(session.finish(true, entries -> {
            for (MediaAnalysisEntry entry : entries) {
                assertEquals(ImageAnalysisCacheSession.POLICY_VERSION, entry.policyVersion);
            }
            return true;
        }));
    }
}
