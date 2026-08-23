package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

public class DiscoverySnapshotMergerTest {
    @Test
    public void preservesItemsFromPreviouslyAnalyzedFolder() {
        DiscoverySnapshot first = snapshot(100L, source("content://media/1", "수원", false, false));
        DiscoverySnapshot second = snapshot(200L, source("content://media/2", "성남", false, false));

        DiscoverySnapshot merged = DiscoverySnapshotMerger.replaceAnalyzedItems(
                first,
                second,
                uris("content://media/2"));

        assertEquals(2, merged.groupCount());
        assertEquals(2, merged.sourceItemCount);
    }

    @Test
    public void combinesSamePlaceAcrossPreviouslyAnalyzedFolders() {
        DiscoverySnapshot first = snapshot(100L, source("content://media/1", "수원", false, false));
        DiscoverySnapshot second = snapshot(200L, source("content://media/2", "수원", false, false));

        DiscoverySnapshot merged = DiscoverySnapshotMerger.replaceAnalyzedItems(
                first,
                second,
                uris("content://media/2"));

        assertEquals(1, merged.groupCount());
        assertEquals(2, merged.groups.get(0).itemCount);
        assertEquals(2, merged.groups.get(0).photoRefs.size());
    }

    @Test
    public void replacesLocationForReanalyzedUri() {
        DiscoverySnapshot first = snapshot(100L, source("content://media/1", "송파동", false, false));
        DiscoverySnapshot second = snapshot(200L, source("content://media/1", "송파구", false, false));

        DiscoverySnapshot merged = DiscoverySnapshotMerger.replaceAnalyzedItems(
                first,
                second,
                uris("content://media/1"));

        assertEquals(1, merged.groupCount());
        assertEquals("송파구", merged.groups.get(0).placeKey);
        assertEquals(1, merged.groups.get(0).itemCount);
    }

    @Test
    public void removesPreviouslyDiscoveredUriWhenItBecomesDuplicate() {
        DiscoverySnapshot first = snapshot(100L, source("content://media/1", "수원", false, false));
        DiscoverySnapshot second = snapshot(200L, source("content://media/1", "수원", false, true));

        DiscoverySnapshot merged = DiscoverySnapshotMerger.replaceAnalyzedItems(
                first,
                second,
                uris("content://media/1"));

        assertEquals(0, merged.groupCount());
        assertEquals(0, merged.sourceItemCount);
    }

    @Test
    public void removesPreviouslyDiscoveredUriWhenLocationIsNoLongerAvailable() {
        DiscoverySnapshot first = snapshot(100L, source("content://media/1", "수원", false, false));
        DiscoverySnapshot second = snapshot(200L, source("content://media/1", "위치 정보 없음", true, false));

        DiscoverySnapshot merged = DiscoverySnapshotMerger.replaceAnalyzedItems(
                first,
                second,
                uris("content://media/1"));

        assertEquals(0, merged.groupCount());
    }

    private static DiscoverySnapshot snapshot(long version, DiscoverySnapshotMapper.SourceItem... items) {
        return DiscoverySnapshotMapper.fromSourceItems(
                Arrays.asList(items),
                items.length,
                version,
                version,
                "source-" + version,
                DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION,
                DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);
    }

    private static DiscoverySnapshotMapper.SourceItem source(String uri,
                                                              String place,
                                                              boolean noLocation,
                                                              boolean duplicate) {
        return new DiscoverySnapshotMapper.SourceItem(
                uri,
                "IMG.jpg",
                "image/jpeg",
                1786000000000L,
                place,
                noLocation,
                duplicate,
                false,
                "KR",
                "대한민국",
                "",
                "");
    }

    private static LinkedHashSet<String> uris(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
