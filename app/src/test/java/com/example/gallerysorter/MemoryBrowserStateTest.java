package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemoryBrowserStateTest {
    @Test
    public void buildsDiscoveryOnlyItemForAlbumBeforeOrganize() {
        MemoryRepository repository = new MemoryRepository(
                snapshot(discoveryGroup("discovery:삿포로", "삿포로", 2, 0)),
                null);

        MemoryBrowserState state = MemoryBrowserState.from(repository);

        assertFalse(state.isEmpty());
        assertEquals(1, state.items.size());
        MemoryBrowserItem item = state.items.get(0);
        assertEquals("discovery:삿포로", item.memoryKey);
        assertEquals("삿포로", item.title);
        assertEquals("앨범 만들기 전", item.subtitle);
        assertEquals("2개", item.countText);
        assertEquals("2026.08.02 ~ 2026.08.06", item.dateText);
        assertEquals("content://media/external/images/media/101", item.coverUri);
        assertEquals(MemorySourceType.DISCOVERED_ONLY, item.sourceType);
        assertTrue(item.discoveryOnly);
        assertFalse(item.organizedAlbum);
        assertTrue(item.canOpenPhotos);
        assertFalse(item.canOpenGalleryAlbum);
        assertTrue(item.canOrganize);
    }

    @Test
    public void buildsOrganizedAlbumItemWithoutSourceUris() {
        StoredAlbumSummary summary = organizedAlbum("오타루에서", "Pictures/오타루에서/", 5);
        MemoryRepository repository = new MemoryRepository(null, Collections.singletonList(summary));

        MemoryBrowserState state = MemoryBrowserState.from(repository);
        MemoryBrowserItem item = state.items.get(0);
        MemoryBrowserDetail detail = state.detail(item.memoryKey, repository);

        assertEquals("오타루에서", item.title);
        assertEquals("정리된 앨범", item.subtitle);
        assertEquals("5개", item.countText);
        assertEquals(MemorySourceType.ORGANIZED_ALBUM, item.sourceType);
        assertFalse(item.discoveryOnly);
        assertTrue(item.organizedAlbum);
        assertFalse(item.canOpenPhotos);
        assertTrue(item.canOpenGalleryAlbum);
        assertFalse(item.canOrganize);
        assertNotNull(detail);
        assertTrue(detail.sourceUris.isEmpty());
        assertFalse(detail.canOpenPhotos);
        assertTrue(detail.canOpenGalleryAlbum);
    }

    @Test
    public void sortsRecentMemoriesBeforeOlderMemories() {
        MemoryRecord older = record(
                "discovery:오타루",
                "오타루",
                1785600000000L,
                1785600000000L);
        MemoryRecord newer = record(
                "discovery:삿포로",
                "삿포로",
                1785945600000L,
                1785945600000L);

        MemoryBrowserState state = MemoryBrowserState.fromRecords(Arrays.asList(older, newer));

        assertEquals("삿포로", state.items.get(0).title);
        assertEquals("오타루", state.items.get(1).title);
    }

    @Test
    public void detailReturnsNonStaleSourceUrisForDiscoveryMemory() {
        DiscoveryMemoryGroup group = discoveryGroup("discovery:삿포로", "삿포로", 3, 1);
        MemoryRepository repository = new MemoryRepository(snapshot(group), null);
        MemoryBrowserState state = MemoryBrowserState.from(repository);

        MemoryBrowserDetail detail = state.detail("discovery:삿포로", repository);

        assertNotNull(detail);
        assertEquals(2, detail.sourceUris.size());
        assertEquals("content://media/external/images/media/101", detail.sourceUris.get(0));
        assertEquals("content://media/external/images/media/102", detail.sourceUris.get(1));
        assertEquals(1, detail.photoSections.size());
        assertEquals(2, detail.photoSections.get(0).photos.size());
        assertTrue(detail.canOpenPhotos);
        assertTrue(detail.canOrganize);
    }

    @Test
    public void detailIgnoresEmptySourceUris() {
        DiscoveryMemoryGroup group = discoveryGroupWithRefs(
                "discovery:삿포로",
                "삿포로",
                Arrays.asList(
                        photoRef("", false),
                        photoRef("content://media/external/images/media/201", false)));
        MemoryRepository repository = new MemoryRepository(snapshot(group), null);
        MemoryBrowserState state = MemoryBrowserState.from(repository);

        MemoryBrowserDetail detail = state.detail("discovery:삿포로", repository);

        assertNotNull(detail);
        assertEquals(1, detail.sourceUris.size());
        assertEquals("content://media/external/images/media/201", detail.sourceUris.get(0));
        assertEquals(1, detail.photoSections.size());
        assertEquals("content://media/external/images/media/201", detail.photoSections.get(0).photos.get(0).sourceUri);
        assertTrue(detail.canOpenPhotos);
        assertTrue(detail.canOrganize);
    }

    @Test
    public void detailDisablesPhotoActionsWhenEverySourceUriIsStaleOrMissing() {
        DiscoveryMemoryGroup group = discoveryGroupWithRefs(
                "discovery:삿포로",
                "삿포로",
                Arrays.asList(
                        photoRef("content://media/external/images/media/301", true),
                        photoRef("", false)));
        MemoryRepository repository = new MemoryRepository(snapshot(group), null);
        MemoryBrowserState state = MemoryBrowserState.from(repository);

        MemoryBrowserDetail detail = state.detail("discovery:삿포로", repository);

        assertNotNull(detail);
        assertTrue(detail.sourceUris.isEmpty());
        assertTrue(detail.photoSections.isEmpty());
        assertFalse(detail.canOpenPhotos);
        assertFalse(detail.canOrganize);
    }

    @Test
    public void missingRepositoryOrKeyReturnsEmptyStateOrNullDetail() {
        MemoryBrowserState empty = MemoryBrowserState.from(null);

        assertTrue(empty.isEmpty());
        assertNull(empty.item("missing"));
        assertNull(empty.detail("missing", null));
    }

    private static MemoryRecord record(String memoryKey, String title, long startMillis, long endMillis) {
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup(
                memoryKey,
                title,
                title,
                "JP",
                "Japan",
                "Hokkaido",
                "",
                1,
                1,
                0,
                startMillis,
                endMillis,
                "content://cover",
                null,
                0,
                1L);
        return MemoryRepository.fromDiscoveryGroup(group);
    }

    private static DiscoverySnapshot snapshot(DiscoveryMemoryGroup group) {
        return new DiscoverySnapshot(
                DiscoverySnapshot.CURRENT_SCHEMA_VERSION,
                1L,
                1786000000000L,
                "test",
                group.itemCount,
                Collections.singletonList(group),
                DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION,
                DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);
    }

    private static DiscoveryMemoryGroup discoveryGroup(String memoryKey,
                                                       String placeName,
                                                       int itemCount,
                                                       int staleCount) {
        return discoveryGroupWithRefs(memoryKey, placeName, photoRefs(itemCount, staleCount));
    }

    private static DiscoveryMemoryGroup discoveryGroupWithRefs(String memoryKey,
                                                               String placeName,
                                                               List<DiscoveryPhotoRef> refs) {
        int itemCount = refs == null ? 0 : refs.size();
        int staleCount = 0;
        if (refs != null) {
            for (DiscoveryPhotoRef ref : refs) {
                if (ref != null && ref.stale) {
                    staleCount++;
                }
            }
        }
        return new DiscoveryMemoryGroup(
                memoryKey,
                placeName,
                placeName,
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan",
                itemCount,
                itemCount,
                0,
                1785600000000L,
                1785945600000L,
                "content://media/external/images/media/101",
                refs,
                staleCount,
                1L);
    }

    private static List<DiscoveryPhotoRef> photoRefs(int itemCount, int staleCount) {
        ArrayList<DiscoveryPhotoRef> refs = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            refs.add(photoRef(
                    "content://media/external/images/media/" + (101 + i),
                    i >= itemCount - staleCount));
        }
        return refs;
    }

    private static DiscoveryPhotoRef photoRef(String uri, boolean stale) {
        return new DiscoveryPhotoRef(
                uri,
                DiscoveryPhotoRef.UNKNOWN_ID,
                MediaKind.PHOTO,
                "image/jpeg",
                "IMG.jpg",
                1785600000000L,
                "삿포로",
                "삿포로",
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan",
                "",
                1L,
                1L,
                stale);
    }

    private static StoredAlbumSummary organizedAlbum(String albumName, String relativePath, int itemCount) {
        return new StoredAlbumSummary(
                albumName,
                relativePath,
                itemCount,
                "2026-08-02",
                "2026-08-06",
                "content://thumbnail",
                "2026-08-15 10:00:00",
                1786000000000L,
                "JP",
                "Japan",
                "Hokkaido",
                "Otaru, Hokkaido, Japan");
    }
}
