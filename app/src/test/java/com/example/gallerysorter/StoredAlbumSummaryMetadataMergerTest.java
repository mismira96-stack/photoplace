package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class StoredAlbumSummaryMetadataMergerTest {
    @Test
    public void preservesStoredCountryMetadataWhenLiveSummaryIsMissingIt() {
        StoredAlbumSummary live = summary("Fatih에서", "", "", "", "", 1);
        StoredAlbumSummary stored = summary("Fatih에서", "TR", "Turkey", "İstanbul", "Fatih, Türkiye", 9);

        StoredAlbumSummary merged = StoredAlbumSummaryMetadataMerger.mergeLiveWithStoredMetadata(live, stored);

        assertEquals("TR", merged.countryCode);
        assertEquals("튀르키예", merged.countryName);
        assertEquals("İstanbul", merged.adminArea);
        assertEquals("Fatih, Türkiye", merged.addressLine);
        assertEquals(1, merged.itemCount);
    }

    @Test
    public void keepsLiveCountryMetadataWhenItExists() {
        StoredAlbumSummary live = summary("Fatih에서", "TR", "튀르키예", "İstanbul", "Live address", 1);
        StoredAlbumSummary stored = summary("Fatih에서", "CZ", "Czechia", "Karlovarský kraj", "Stored address", 9);

        StoredAlbumSummary merged = StoredAlbumSummaryMetadataMerger.mergeLiveWithStoredMetadata(live, stored);

        assertSame(live, merged);
        assertEquals("TR", merged.countryCode);
        assertEquals("튀르키예", merged.countryName);
        assertEquals("İstanbul", merged.adminArea);
        assertEquals("Live address", merged.addressLine);
    }

    @Test
    public void mergesPartialMetadataFromStoredSummary() {
        StoredAlbumSummary live = summary("KarlovyVary에서", "CZ", "", "", "", 2);
        StoredAlbumSummary stored = summary("KarlovyVary에서", "", "Czechia", "Karlovarský kraj", "Karlovy Vary, Czechia", 2);

        StoredAlbumSummary merged = StoredAlbumSummaryMetadataMerger.mergeLiveWithStoredMetadata(live, stored);

        assertEquals("CZ", merged.countryCode);
        assertEquals("체코", merged.countryName);
        assertEquals("Karlovarský kraj", merged.adminArea);
        assertEquals("Karlovy Vary, Czechia", merged.addressLine);
    }

    private static StoredAlbumSummary summary(String albumName, String countryCode, String countryName,
                                             String adminArea, String addressLine, int count) {
        return new StoredAlbumSummary(
                albumName,
                "Pictures/" + albumName + "/",
                count,
                "2026-08-02",
                "2026-08-06",
                "content://thumbnail",
                null,
                123L,
                countryCode,
                countryName,
                adminArea,
                addressLine);
    }
}
