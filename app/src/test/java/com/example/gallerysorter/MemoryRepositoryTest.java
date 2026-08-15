package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemoryRepositoryTest {
    @Test
    public void discoveryGroupBecomesDiscoveryOnlyMemoryRecord() {
        DiscoveryMemoryGroup group = discoveryGroup(
                "discovery:삿포로",
                "삿포로",
                "삿포로",
                2,
                1,
                1,
                1785600000000L,
                1785945600000L,
                0);
        MemoryRepository repository = new MemoryRepository(snapshot(group), null);

        List<MemoryRecord> records = repository.memories();

        assertEquals(1, records.size());
        MemoryRecord record = records.get(0);
        assertEquals("discovery:삿포로", record.memoryKey);
        assertEquals("삿포로", record.placeKey);
        assertEquals("삿포로", record.title);
        assertEquals("JP", record.countryCode);
        assertEquals("일본", record.countryName);
        assertEquals("Hokkaido", record.adminArea);
        assertEquals(2, record.itemCount);
        assertEquals(1, record.photoCount);
        assertEquals(1, record.videoCount);
        assertEquals(1785600000000L, record.startDateMillis);
        assertEquals(1785945600000L, record.endDateMillis);
        assertEquals("content://media/external/images/media/101", record.coverUri);
        assertEquals(MemorySourceType.DISCOVERED_ONLY, record.sourceType);
        assertSame(group, record.discoveryGroup);
        assertNull(record.organizedAlbum);
        assertEquals(2, record.availableCount);
        assertTrue(record.canOpenPhotos);
        assertFalse(record.canOpenGalleryAlbum);
        assertTrue(record.canOrganize);
        assertFalse(record.canAddNewItems);
        assertEquals(2, repository.discoveryPhotoRefs("discovery:삿포로").size());
    }

    @Test
    public void organizedAlbumBecomesOrganizedAlbumMemoryRecord() {
        StoredAlbumSummary summary = organizedAlbum(
                "삿포로에서",
                "Pictures/삿포로에서/",
                7,
                "2026-08-02",
                "2026-08-06");
        MemoryRepository repository = new MemoryRepository(null, Collections.singletonList(summary));

        MemoryRecord record = repository.memories().get(0);

        assertEquals("path:Pictures/삿포로에서/", record.memoryKey);
        assertEquals("JP|삿포로", record.placeKey);
        assertEquals("삿포로에서", record.title);
        assertEquals("삿포로", record.canonicalPlaceName);
        assertEquals("JP", record.countryCode);
        assertEquals("일본", record.countryName);
        assertEquals(7, record.itemCount);
        assertEquals("content://thumbnail", record.coverUri);
        assertEquals(MemorySourceType.ORGANIZED_ALBUM, record.sourceType);
        assertNull(record.discoveryGroup);
        assertNotNull(record.organizedAlbum);
        assertEquals("Pictures/삿포로에서/", record.organizedAlbum.relativePath);
        assertFalse(record.canOpenPhotos);
        assertTrue(record.canOpenGalleryAlbum);
        assertFalse(record.canOrganize);
        assertFalse(record.canAddNewItems);
        assertTrue(record.startDateMillis > 0L);
        assertTrue(record.endDateMillis >= record.startDateMillis);
    }

    @Test
    public void sameMemoryKeyMergesDiscoveryAndOrganizedDataDeterministically() {
        StoredAlbumSummary summary = organizedAlbum(
                "삿포로에서",
                "Pictures/삿포로에서/",
                7,
                "2026-08-02",
                "2026-08-06");
        DiscoveryMemoryGroup group = discoveryGroup(
                "path:Pictures/삿포로에서/",
                "삿포로",
                "삿포로",
                3,
                3,
                0,
                1785600000000L,
                1785945600000L,
                0);
        MemoryRepository repository = new MemoryRepository(
                snapshot(group),
                Collections.singletonList(summary));

        List<MemoryRecord> records = repository.memories();

        assertEquals(1, records.size());
        MemoryRecord record = records.get(0);
        assertEquals("path:Pictures/삿포로에서/", record.memoryKey);
        assertEquals(MemorySourceType.MIXED, record.sourceType);
        assertSame(group, record.discoveryGroup);
        assertNotNull(record.organizedAlbum);
        assertEquals(7, record.itemCount);
        assertEquals(7, record.photoCount);
        assertTrue(record.canOpenPhotos);
        assertTrue(record.canOpenGalleryAlbum);
        assertFalse(record.canOrganize);
        assertTrue(record.canAddNewItems);
        assertEquals(3, repository.discoveryPhotoRefs(record.memoryKey).size());
    }

    @Test
    public void returnsNullAndEmptyRefsForMissingMemory() {
        MemoryRepository repository = new MemoryRepository(null, null);

        assertNull(repository.memory("missing"));
        assertTrue(repository.discoveryPhotoRefs("missing").isEmpty());
    }

    private static DiscoverySnapshot snapshot(DiscoveryMemoryGroup... groups) {
        return new DiscoverySnapshot(
                DiscoverySnapshot.CURRENT_SCHEMA_VERSION,
                3L,
                1786000000000L,
                "test-source",
                groups.length,
                Arrays.asList(groups),
                DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION,
                DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);
    }

    private static DiscoveryMemoryGroup discoveryGroup(String memoryKey,
                                                       String placeKey,
                                                       String placeName,
                                                       int itemCount,
                                                       int photoCount,
                                                       int videoCount,
                                                       long startDateMillis,
                                                       long endDateMillis,
                                                       int staleCount) {
        return new DiscoveryMemoryGroup(
                memoryKey,
                placeKey,
                placeName,
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan",
                itemCount,
                photoCount,
                videoCount,
                startDateMillis,
                endDateMillis,
                "content://media/external/images/media/101",
                photoRefs(itemCount, videoCount),
                staleCount,
                3L);
    }

    private static List<DiscoveryPhotoRef> photoRefs(int itemCount, int videoCount) {
        ArrayList<DiscoveryPhotoRef> refs = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            boolean video = i < videoCount;
            String collection = video ? "video" : "images";
            String id = String.valueOf(101 + i);
            refs.add(photoRef("content://media/external/" + collection + "/media/" + id,
                    video ? MediaKind.VIDEO : MediaKind.PHOTO));
        }
        return refs;
    }

    private static DiscoveryPhotoRef photoRef(String uri, MediaKind kind) {
        return new DiscoveryPhotoRef(
                uri,
                DiscoveryPhotoRef.UNKNOWN_ID,
                kind,
                kind == MediaKind.VIDEO ? "video/mp4" : "image/jpeg",
                "item",
                1785600000000L,
                "삿포로",
                "삿포로",
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan",
                "",
                3L,
                3L,
                false);
    }

    private static StoredAlbumSummary organizedAlbum(String albumName,
                                                     String relativePath,
                                                     int itemCount,
                                                     String startDate,
                                                     String endDate) {
        return new StoredAlbumSummary(
                albumName,
                relativePath,
                itemCount,
                startDate,
                endDate,
                "content://thumbnail",
                "2026-08-15 10:00:00",
                1786000000000L,
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan");
    }
}
