package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class OverseasMemoryGrouperTest {
    @Test
    public void hokkaidoFoldersAreGroupedAsJapanWithoutCountryMetadata() {
        List<MemoryGroup> groups = OverseasMemoryGrouper.buildOverseasGroups(Arrays.asList(
                summary("Sapporo에서", "Pictures/Sapporo에서/", 12),
                summary("札幌市에서", "Pictures/札幌市에서/", 4),
                summary("Biei에서", "Pictures/Biei에서/", 6),
                summary("Otaru에서", "Pictures/Otaru에서/", 8),
                summary("Nakafurano에서", "Pictures/Nakafurano에서/", 3)
        ));

        assertEquals(1, groups.size());
        assertEquals("일본", groups.get(0).countryName);
        assertEquals(33, groups.get(0).itemCount);
    }

    @Test
    public void countryMetadataIsNormalizedForDisplay() {
        List<MemoryGroup> groups = OverseasMemoryGrouper.buildOverseasGroups(Arrays.asList(
                summaryWithCountry("札幌市에서", "Pictures/札幌市에서/", 2, "日本"),
                summaryWithCountry("NewYork에서", "Pictures/NewYork에서/", 3, "United States")
        ));

        assertEquals(2, groups.size());
        assertEquals("미국", groups.get(0).countryName);
        assertEquals("일본", groups.get(1).countryName);
    }

    @Test
    public void strongPlaceHintBeatsMisleadingShortCountryAlias() {
        List<MemoryGroup> groups = OverseasMemoryGrouper.buildOverseasGroups(Arrays.asList(
                summaryWithCountry("Fukuoka에서", "Pictures/Fukuoka에서/", 53, "Fukuoka")
        ));

        assertEquals(1, groups.size());
        assertEquals("일본", groups.get(0).countryName);
    }

    @Test
    public void koreanAdministrativeFoldersAreExcluded() {
        List<MemoryGroup> groups = OverseasMemoryGrouper.buildOverseasGroups(Arrays.asList(
                summary("Songpa-gu에서", "Pictures/Songpa-gu에서/", 10),
                summary("Mapo-gu에서", "Pictures/Mapo-gu에서/", 5),
                summary("Sapporo에서", "Pictures/Sapporo에서/", 7)
        ));

        assertEquals(1, groups.size());
        assertEquals("일본", groups.get(0).countryName);
        assertEquals(7, groups.get(0).itemCount);
    }

    @Test
    public void existingAustraliaFallbackStillWorks() {
        List<MemoryGroup> groups = OverseasMemoryGrouper.buildOverseasGroups(Arrays.asList(
                summary("Melbourne에서", "Pictures/Melbourne에서/", 9),
                summary("Southbank에서", "Pictures/Southbank에서/", 11)
        ));

        assertEquals(1, groups.size());
        assertEquals("호주", groups.get(0).countryName);
        assertEquals(20, groups.get(0).itemCount);
    }

    @Test
    public void unknownOverseasCityIsNotGuessedBlindly() {
        List<MemoryGroup> groups = OverseasMemoryGrouper.buildOverseasGroups(Arrays.asList(
                summary("UnknownCity에서", "Pictures/UnknownCity에서/", 4)
        ));

        assertTrue(groups.isEmpty());
    }

    private static StoredAlbumSummary summary(String albumName, String relativePath, int count) {
        return summaryWithCountry(albumName, relativePath, count, "");
    }

    private static StoredAlbumSummary summaryWithCountry(String albumName, String relativePath, int count, String country) {
        return new StoredAlbumSummary(
                albumName,
                relativePath,
                count,
                "2026-08-02",
                "2026-08-06",
                "",
                null,
                0L,
                country,
                "",
                "");
    }
}
