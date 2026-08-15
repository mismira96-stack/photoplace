package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class OrganizePlaceServiceTest {
    @Test
    public void planForDiscoveryMemoryKeepsOnlyAvailableRefs() {
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup(
                "discovery:삿포로",
                "삿포로",
                "삿포로",
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan",
                3,
                3,
                0,
                1785600000000L,
                1785945600000L,
                "content://media/external/images/media/101",
                Arrays.asList(
                        ref("content://media/external/images/media/101", false),
                        ref("content://media/external/images/media/102", true),
                        ref("", false)),
                1,
                7L);
        MemoryRecord record = MemoryRepository.fromDiscoveryGroup(group);

        OrganizePlaceService.Plan plan = OrganizePlaceService.planFor(record);

        assertTrue(plan.canOrganize());
        assertEquals("discovery:삿포로", plan.memoryKey);
        assertEquals("삿포로", plan.placeName);
        assertEquals("Pictures/삿포로에서/", plan.targetRelativePath);
        assertEquals(1, plan.refs.size());
        assertEquals("content://media/external/images/media/101", plan.refs.get(0).sourceUri);
        assertEquals(2, plan.skippedRefCount);
    }

    @Test
    public void planForOrganizedOnlyMemoryIsEmpty() {
        MemoryRecord record = MemoryRepository.fromOrganizedAlbum(new StoredAlbumSummary(
                "삿포로에서",
                "Pictures/삿포로에서/",
                7,
                "2026-08-02",
                "2026-08-06",
                "content://thumbnail",
                "2026-08-15 10:00:00",
                1786000000000L,
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan"));

        OrganizePlaceService.Plan plan = OrganizePlaceService.planFor(record);

        assertFalse(plan.canOrganize());
        assertEquals(0, plan.refs.size());
    }

    @Test
    public void planUsesDisplayNameBeforeCanonicalName() {
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup(
                "discovery:wide",
                "송파구",
                "송파구",
                "KR",
                "대한민국",
                "서울특별시",
                "서울특별시 송파구",
                1,
                1,
                0,
                1785600000000L,
                1785600000000L,
                "content://media/external/images/media/201",
                Collections.singletonList(ref("content://media/external/images/media/201", false)),
                0,
                7L);
        MemoryRecord record = new MemoryRecord(
                "discovery:wide",
                "송파구",
                "송파구",
                "송파구",
                "라비에벨 발레",
                "KR",
                "대한민국",
                "서울특별시",
                "서울특별시 송파구",
                1,
                1,
                0,
                1785600000000L,
                1785600000000L,
                "content://media/external/images/media/201",
                MemorySourceType.DISCOVERED_ONLY,
                group,
                null,
                0,
                1,
                true,
                false,
                true,
                false);

        OrganizePlaceService.Plan plan = OrganizePlaceService.planFor(record);

        assertEquals("라비에벨 발레", plan.placeName);
        assertEquals("Pictures/라비에벨 발레에서/", plan.targetRelativePath);
    }

    @Test
    public void planDoesNotDuplicateLocationSuffix() {
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup(
                "discovery:custom",
                "삿포로",
                "삿포로",
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan",
                1,
                1,
                0,
                1785600000000L,
                1785600000000L,
                "content://media/external/images/media/301",
                Collections.singletonList(ref("content://media/external/images/media/301", false)),
                0,
                7L);
        MemoryRecord record = new MemoryRecord(
                "discovery:custom",
                "삿포로",
                "삿포로",
                "삿포로",
                "삿포로에서",
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan",
                1,
                1,
                0,
                1785600000000L,
                1785600000000L,
                "content://media/external/images/media/301",
                MemorySourceType.DISCOVERED_ONLY,
                group,
                null,
                0,
                1,
                true,
                false,
                true,
                false);

        OrganizePlaceService.Plan plan = OrganizePlaceService.planFor(record);

        assertEquals("Pictures/삿포로에서/", plan.targetRelativePath);
    }

    private static DiscoveryPhotoRef ref(String uri, boolean stale) {
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
                7L,
                7L,
                stale);
    }
}
