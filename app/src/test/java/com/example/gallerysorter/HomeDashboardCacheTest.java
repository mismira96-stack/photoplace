package com.example.gallerysorter;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class HomeDashboardCacheTest {
    @Test public void unavailableCardCannotValidateEvenWithMatchingRevision() throws Exception {
        HomeDashboardSnapshot snapshot = new HomeDashboardSnapshot("same", Collections.singletonList(
                new HomeRecentPlacesResolver.Card("discovery:성남", "성남", "", "불러오기 필요", false)), Collections.emptyList());
        HomeDashboardSnapshot restored = HomeDashboardSnapshot.fromJson(snapshot.toJson());
        assertFalse(restored.complete());
        assertFalse(HomeDashboardPolicy.canSkip(restored, "same"));
    }
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private HomeDashboardSnapshot sample(String revision) {
        return new HomeDashboardSnapshot(revision,
                Arrays.asList(new HomeRecentPlacesResolver.Card("path:Pictures/하남에서/", "하남", "content://photo/1", "사진 105장"),
                        new HomeRecentPlacesResolver.Card("discovery:성남", "성남", "content://photo/2", "사진 5장 · 동영상 1개")),
                Collections.singletonList(new HomeDashboardSnapshot.CountryCard("JP", "일본", "content://photo/3", 7, 6)));
    }

    @Test public void diskRoundTripKeepsDestinationsCountsAndCountrySummaries() throws Exception {
        HomeDashboardStore store = new HomeDashboardStore(folder.newFolder());
        assertTrue(store.save(sample("revision1")));
        HomeDashboardSnapshot result = store.read();
        assertEquals("revision1", result.revision);
        assertEquals(sample("revision1").displayKey(), result.displayKey());
        assertEquals("path:Pictures/하남에서/", result.places.get(0).memoryKey);
        assertTrue(result.places.get(0).includeOrganizedSources);
        assertEquals("JP", result.countries.get(0).code);
        assertEquals("발견 7곳\n위치 앨범 6개", result.countries.get(0).summary());
    }

    @Test public void missingCorruptOrNewSchemaCacheIsDisposable() throws Exception {
        File dir = folder.newFolder();
        HomeDashboardStore store = new HomeDashboardStore(dir);
        assertNull(store.read());
        File file = new File(dir, "home_dashboard_cache.json");
        Files.write(file.toPath(), "{broken".getBytes(StandardCharsets.UTF_8));
        assertNull(store.read());
        Files.write(file.toPath(), sample("x").toJson().put("schema", 99).toString().getBytes(StandardCharsets.UTF_8));
        assertNull(store.read());
        assertTrue(store.save(sample("restored")));
        assertEquals("restored", store.read().revision);
    }

    @Test public void matchingRevisionSkipsWhileUnknownAndChangedRevisionsRefresh() {
        assertTrue(HomeDashboardPolicy.canSkip(sample("same"), "same"));
        assertFalse(HomeDashboardPolicy.canSkip(sample("before"), "after"));
        assertFalse(HomeDashboardPolicy.canSkip(sample(""), ""));
        assertFalse(HomeDashboardPolicy.canSkip(sample("before"), null));
        assertFalse(HomeDashboardPolicy.canSkip(null, "same"));
    }

    @Test public void concurrentSourceChangesNeverValidateTheResult() {
        assertEquals("same", HomeDashboardPolicy.stableRevision("same", "same"));
        assertEquals("", HomeDashboardPolicy.stableRevision("before", "after"));
        assertEquals("", HomeDashboardPolicy.stableRevision(null, null));
    }

    @Test public void identicalSummariesDoNotRebuildViewsEvenIfRevisionChanged() {
        assertFalse(HomeDashboardPolicy.shouldRender(sample("before").displayKey(), sample("after")));
        assertTrue(HomeDashboardPolicy.shouldRender(null, sample("after")));
        assertFalse(HomeDashboardPolicy.shouldRender("shown", null));
        HomeDashboardSnapshot different = new HomeDashboardSnapshot("after", Collections.emptyList(), Collections.emptyList());
        assertTrue(HomeDashboardPolicy.shouldRender(sample("before").displayKey(), different));
    }

    @Test public void emptySummaryIsAValidCachedResult() throws Exception {
        HomeDashboardStore store = new HomeDashboardStore(folder.newFolder());
        assertTrue(store.save(new HomeDashboardSnapshot("empty", Collections.emptyList(), Collections.emptyList())));
        assertTrue(HomeDashboardPolicy.canSkip(store.read(), "empty"));
        assertTrue(store.read().places.isEmpty());
    }

    @Test public void cacheContainsOnlySummaryFieldsNotPhotosOrNotes() throws Exception {
        String json = sample("revision").toJson().toString();
        assertFalse(json.contains("photoRefs"));
        assertFalse(json.contains("stableMemoryId"));
        assertFalse(json.contains("date_notes"));
        assertTrue(json.getBytes(StandardCharsets.UTF_8).length < 4096);
        JSONObject malformed = sample("revision").toJson();
        malformed.getJSONArray("places").getJSONObject(0).put("key", "");
        try { HomeDashboardSnapshot.fromJson(malformed); fail("Invalid navigation key accepted"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void repeatedRefreshRequestsCoalesceButKeepOneFollowUp() {
        HomeRefreshRequests requests = new HomeRefreshRequests();
        assertTrue(requests.begin());
        assertFalse(requests.begin());
        assertFalse(requests.begin());
        assertTrue(requests.complete());
        assertTrue(requests.begin());
        assertFalse(requests.complete());
    }

    @Test public void oversizedFileIsIgnored() throws Exception {
        File dir = folder.newFolder();
        Files.write(new File(dir, "home_dashboard_cache.json").toPath(), new byte[65537]);
        assertNull(new HomeDashboardStore(dir).read());
    }

    @Test public void failedTemporaryWritePreservesLastValidSummary() throws Exception {
        File dir = folder.newFolder();
        HomeDashboardStore store = new HomeDashboardStore(dir);
        assertTrue(store.save(sample("last-good")));
        byte[] original = Files.readAllBytes(new File(dir, "home_dashboard_cache.json").toPath());
        // A directory at the temp path reliably fails the write on Windows and Linux.
        assertTrue(new File(dir, "home_dashboard_cache.json.tmp").mkdir());
        assertFalse(store.save(sample("not-saved")));
        assertArrayEquals(original, Files.readAllBytes(new File(dir, "home_dashboard_cache.json").toPath()));
        assertEquals("last-good", store.read().revision);
    }

    @Test public void interruptedTemporaryWriteIsIgnoredAndNextSaveRecovers() throws Exception {
        File dir = folder.newFolder();
        HomeDashboardStore store = new HomeDashboardStore(dir);
        assertTrue(store.save(sample("last-good")));
        Files.write(new File(dir, "home_dashboard_cache.json.tmp").toPath(),
                "{partial".getBytes(StandardCharsets.UTF_8));
        assertEquals("last-good", store.read().revision);
        assertTrue(store.save(sample("recovered")));
        assertEquals("recovered", store.read().revision);
    }

    @Test public void invalidCountryCountsDiscardWholeCacheAndAllowRecovery() throws Exception {
        File dir = folder.newFolder();
        HomeDashboardStore store = new HomeDashboardStore(dir);
        JSONObject invalid = sample("bad").toJson();
        invalid.getJSONArray("countries").getJSONObject(0).put("albums", -1);
        Files.write(new File(dir, "home_dashboard_cache.json").toPath(),
                invalid.toString().getBytes(StandardCharsets.UTF_8));
        assertNull(store.read());
        assertTrue(store.save(sample("recovered")));
        assertEquals("recovered", store.read().revision);
    }
}
