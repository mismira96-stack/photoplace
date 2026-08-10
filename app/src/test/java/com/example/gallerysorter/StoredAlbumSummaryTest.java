package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StoredAlbumSummaryTest {
    @Test
    public void legacyCountryNameBackfillsCountryCode() {
        StoredAlbumSummary summary = summary("", "Turkey");

        assertEquals("TR", summary.countryCode);
        assertEquals("튀르키예", summary.countryName);
    }

    @Test
    public void czechLegacyCountryNameBackfillsCountryCode() {
        StoredAlbumSummary summary = summary("", "Czech Republic");

        assertEquals("CZ", summary.countryCode);
        assertEquals("체코", summary.countryName);
    }

    @Test
    public void countryCodeWinsWhenCountryNameConflicts() {
        StoredAlbumSummary summary = summary("TR", "Czechia");

        assertEquals("TR", summary.countryCode);
        assertEquals("튀르키예", summary.countryName);
    }

    private static StoredAlbumSummary summary(String countryCode, String countryName) {
        return new StoredAlbumSummary(
                "Travel에서",
                "Pictures/Travel에서/",
                1,
                "2026-08-02",
                "2026-08-06",
                "content://thumbnail",
                null,
                123L,
                countryCode,
                countryName,
                "",
                "");
    }
}
