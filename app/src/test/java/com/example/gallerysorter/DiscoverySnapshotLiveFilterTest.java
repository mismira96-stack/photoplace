package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class DiscoverySnapshotLiveFilterTest {
    @Test
    public void onlyRecordedLocationAlbumPathsAreExcludedFromDiscovery() {
        java.util.Set<String> organizedPaths = DiscoverySnapshotLiveFilter.organizedRelativePaths(Arrays.asList(
                summary("성남에서", "Pictures/성남에서/"),
                summary("여행", "Pictures/2026 여름 일본 여행/")));

        assertTrue(DiscoverySnapshotLiveFilter.isOrganizedLocationPath("Pictures/성남에서/", organizedPaths));
        assertTrue(DiscoverySnapshotLiveFilter.isOrganizedLocationPath("Pictures\\2026 여름 일본 여행\\", organizedPaths));
        assertFalse(DiscoverySnapshotLiveFilter.isOrganizedLocationPath("Pictures/수원에서/", organizedPaths));
        assertFalse(DiscoverySnapshotLiveFilter.isOrganizedLocationPath("DCIM/Camera/", organizedPaths));
    }

    @Test
    public void removesDeletedRefsAndRecalculatesGroupMetadata() {
        DiscoveryPhotoRef deleted = ref(1L, MediaKind.PHOTO, 100L);
        DiscoveryPhotoRef liveVideo = ref(2L, MediaKind.VIDEO, 200L);
        DiscoveryPhotoRef livePhoto = ref(3L, MediaKind.PHOTO, 300L);
        DiscoverySnapshot snapshot = snapshot(group("성남", deleted, liveVideo, livePhoto));

        DiscoverySnapshot filtered = DiscoverySnapshotLiveFilter.filter(
                snapshot,
                new HashSet<>(Collections.singletonList(3L)),
                new HashSet<>(Collections.singletonList(2L)));

        assertEquals(1, filtered.groups.size());
        DiscoveryMemoryGroup group = filtered.groups.get(0);
        assertEquals(2, group.itemCount);
        assertEquals(1, group.photoCount);
        assertEquals(1, group.videoCount);
        assertEquals(200L, group.startDateMillis);
        assertEquals(300L, group.endDateMillis);
        assertEquals("content://media/2", group.coverUri);
    }

    @Test
    public void removesGroupWhenEveryRefWasDeleted() {
        DiscoverySnapshot snapshot = snapshot(group("빈장소", ref(10L, MediaKind.PHOTO, 100L)));

        DiscoverySnapshot filtered = DiscoverySnapshotLiveFilter.filter(
                snapshot,
                Collections.<Long>emptySet(),
                Collections.<Long>emptySet());

        assertEquals(0, filtered.groups.size());
    }

    private static DiscoverySnapshot snapshot(DiscoveryMemoryGroup group) {
        return new DiscoverySnapshot(1, 1L, 1L, "source", 3,
                Collections.singletonList(group), "analysis", "country");
    }

    private static DiscoveryMemoryGroup group(String place, DiscoveryPhotoRef... refs) {
        return new DiscoveryMemoryGroup(
                "discovery:" + place,
                place,
                place,
                "KR",
                "대한민국",
                "경기도",
                place,
                refs.length,
                refs.length,
                0,
                100L,
                300L,
                refs[0].sourceUri,
                Arrays.asList(refs),
                0,
                1L);
    }

    private static DiscoveryPhotoRef ref(long id, MediaKind kind, long takenAt) {
        return new DiscoveryPhotoRef(
                "content://media/" + id,
                id,
                kind,
                kind == MediaKind.VIDEO ? "video/mp4" : "image/jpeg",
                "media-" + id,
                takenAt,
                "성남",
                "성남",
                "KR",
                "대한민국",
                "경기도",
                "성남",
                "DCIM/Test/",
                1L,
                1L,
                false);
    }

    private static StoredAlbumSummary summary(String albumName, String relativePath) {
        return new StoredAlbumSummary(albumName, relativePath, 1, "2026-08-01", "2026-08-01",
                "", "", 0L, "KR", "대한민국", "", "");
    }
}
