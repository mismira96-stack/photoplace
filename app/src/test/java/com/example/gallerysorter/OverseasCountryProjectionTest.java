package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OverseasCountryProjectionTest {
    @Test
    public void includesDiscoveryOnlyCountryWithoutGalleryAlbum() {
        List<OverseasCountryProjection> projections = OverseasCountryProjection.build(
                Collections.singletonList(discoveryRecord("삿포로", "JP")), null);

        assertEquals(1, projections.size());
        assertTrue(projections.get(0).hasDiscovery());
        assertFalse(projections.get(0).hasOrganizedAlbums());
        assertEquals("JP", projections.get(0).countryCode);
    }

    @Test
    public void keepsOrganizedOnlyCountryForExistingAlbumUsers() {
        List<OverseasCountryProjection> projections = OverseasCountryProjection.build(null,
                Collections.singletonList(album("삿포로에서", "JP")));

        assertEquals(1, projections.size());
        assertFalse(projections.get(0).hasDiscovery());
        assertTrue(projections.get(0).hasOrganizedAlbums());
    }

    @Test
    public void usesOneCountryEntryWhileKeepingBothSourcesSeparate() {
        List<OverseasCountryProjection> projections = OverseasCountryProjection.build(
                Arrays.asList(discoveryRecord("삿포로", "JP"), discoveryRecord("오타루", "JP")),
                Collections.singletonList(album("삿포로에서", "JP")));

        assertEquals(1, projections.size());
        OverseasCountryProjection projection = projections.get(0);
        assertEquals(2, projection.discoveryRecords.size());
        assertEquals(1, projection.organizedAlbums.size());
        assertEquals(3, projection.sourceCount());
    }

    @Test
    public void ignoresKoreanDiscoveryAndOrganizedEntries() {
        List<OverseasCountryProjection> projections = OverseasCountryProjection.build(
                Collections.singletonList(discoveryRecord("성남", "KR")),
                Collections.singletonList(album("성남에서", "KR")));

        assertTrue(projections.isEmpty());
    }

    private static MemoryRecord discoveryRecord(String place, String countryCode) {
        DiscoveryPhotoRef ref = new DiscoveryPhotoRef(
                "content://media/" + place,
                1L,
                MediaKind.PHOTO,
                "image/jpeg",
                "photo.jpg",
                1785888000000L,
                place,
                place,
                countryCode,
                countryCode.equals("JP") ? "Japan" : "대한민국",
                "area",
                "address",
                "",
                1L,
                1L,
                false);
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup(
                "discovery:" + place, place, place, countryCode,
                countryCode.equals("JP") ? "Japan" : "대한민국", "area", "address",
                1, 1, 0, ref.takenAtMillis, ref.takenAtMillis, ref.sourceUri,
                Collections.singletonList(ref), 0, 1L);
        return MemoryRepository.fromDiscoveryGroup(group);
    }

    private static StoredAlbumSummary album(String name, String countryCode) {
        return new StoredAlbumSummary(
                name,
                "Pictures/" + name + "/",
                2,
                "2026-08-01",
                "2026-08-05",
                "content://thumbnail/" + name,
                null,
                0L,
                countryCode,
                countryCode.equals("JP") ? "Japan" : "대한민국",
                "area",
                "address");
    }
}
