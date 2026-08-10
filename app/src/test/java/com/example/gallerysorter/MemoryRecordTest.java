package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MemoryRecordTest {
    @Test
    public void nullSourceTypeInfersOrganizedAlbum() {
        MemoryRecord record = record(null, null, organizedAlbum());

        assertEquals(MemorySourceType.ORGANIZED_ALBUM, record.sourceType);
    }

    @Test
    public void nullSourceTypeInfersMixedWhenBothSourcesExist() {
        MemoryRecord record = record(null, discoveryGroup(), organizedAlbum());

        assertEquals(MemorySourceType.MIXED, record.sourceType);
    }

    @Test
    public void explicitSourceTypeWins() {
        MemoryRecord record = record(MemorySourceType.DISCOVERED_ONLY, discoveryGroup(), organizedAlbum());

        assertEquals(MemorySourceType.DISCOVERED_ONLY, record.sourceType);
    }

    private static MemoryRecord record(MemorySourceType sourceType,
                                       DiscoveryMemoryGroup discoveryGroup,
                                       OrganizedAlbumRef organizedAlbum) {
        return new MemoryRecord(
                "memory:JP|Hokkaido|Sapporo",
                "JP|Hokkaido|Sapporo",
                "삿포로",
                "삿포로",
                "",
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Japan",
                1,
                1,
                0,
                0L,
                0L,
                "content://cover",
                sourceType,
                discoveryGroup,
                organizedAlbum,
                0,
                1,
                true,
                organizedAlbum != null,
                discoveryGroup != null,
                false);
    }

    private static DiscoveryMemoryGroup discoveryGroup() {
        return new DiscoveryMemoryGroup(
                "memory:JP|Hokkaido|Sapporo",
                "JP|Hokkaido|Sapporo",
                "삿포로",
                "JP",
                "Japan",
                "Hokkaido",
                "",
                1,
                1,
                0,
                0L,
                0L,
                "",
                null,
                0,
                1L);
    }

    private static OrganizedAlbumRef organizedAlbum() {
        return new OrganizedAlbumRef(
                "Pictures/삿포로/",
                "삿포로에서",
                1,
                "content://thumbnail",
                "JP",
                "Japan",
                0L,
                0L);
    }
}
