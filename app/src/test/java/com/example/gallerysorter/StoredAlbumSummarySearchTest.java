package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class StoredAlbumSummarySearchTest {
    @Test
    public void emptyQueryReturnsOriginalList() {
        List<StoredAlbumSummary> summaries = Arrays.asList(
                summary("삿포로에서", "일본", "홋카이도", "札幌市中央区", "2026-08-02", "2026-08-06"),
                summary("수원에서", "대한민국", "경기도", "수원시", "2024-01-01", "2024-02-01")
        );

        assertSame(summaries, StoredAlbumSummarySearch.filter(summaries, null));
        assertSame(summaries, StoredAlbumSummarySearch.filter(summaries, "   "));
    }

    @Test
    public void searchesAlbumCountryAdminAndAddressFields() {
        StoredAlbumSummary sapporo = summary("삿포로에서", "일본", "홋카이도", "札幌市中央区", "2026-08-02", "2026-08-06");
        StoredAlbumSummary songpa = summary("송파구에서", "대한민국", "서울특별시", "Songpa-gu", "2025-01-01", "2025-01-02");
        List<StoredAlbumSummary> summaries = Arrays.asList(sapporo, songpa);

        assertSame(sapporo, StoredAlbumSummarySearch.filter(summaries, "삿포로").get(0));
        assertSame(sapporo, StoredAlbumSummarySearch.filter(summaries, "일본").get(0));
        assertSame(sapporo, StoredAlbumSummarySearch.filter(summaries, "홋카이도").get(0));
        assertSame(sapporo, StoredAlbumSummarySearch.filter(summaries, "札幌市").get(0));
        assertSame(songpa, StoredAlbumSummarySearch.filter(summaries, "songpa").get(0));
    }

    @Test
    public void searchesDatesWithDashOrDotSeparators() {
        StoredAlbumSummary sapporo = summary("삿포로에서", "일본", "", "", "2026-08-02", "2026-08-06");
        StoredAlbumSummary sydney = summary("시드니에서", "호주", "", "", "2019-10-01", "2019-12-31");
        List<StoredAlbumSummary> summaries = Arrays.asList(sapporo, sydney);

        assertSame(sapporo, StoredAlbumSummarySearch.filter(summaries, "2026-08").get(0));
        assertSame(sapporo, StoredAlbumSummarySearch.filter(summaries, "2026.08").get(0));
        assertSame(sydney, StoredAlbumSummarySearch.filter(summaries, "2019.12").get(0));
    }

    @Test
    public void preservesInputOrderAndObjects() {
        StoredAlbumSummary first = summary("일본에서", "일본", "", "", "2026-08-02", "2026-08-06");
        StoredAlbumSummary second = summary("오사카에서", "일본", "", "", "2024-05-01", "2024-05-02");
        StoredAlbumSummary third = summary("괌에서", "괌", "", "", "2015-08-01", "2015-08-02");

        List<StoredAlbumSummary> filtered = StoredAlbumSummarySearch.filter(Arrays.asList(first, second, third), "일본");

        assertEquals(2, filtered.size());
        assertSame(first, filtered.get(0));
        assertSame(second, filtered.get(1));
    }

    @Test
    public void handlesNullAndEmptyInputs() {
        assertTrue(StoredAlbumSummarySearch.filter(null, "삿포로").isEmpty());
        assertTrue(StoredAlbumSummarySearch.filter(Arrays.asList(
                summary("", null, null, null, null, null)
        ), "없는값").isEmpty());
    }

    private static StoredAlbumSummary summary(String albumName, String countryName, String adminArea,
                                             String addressLine, String startDate, String endDate) {
        return new StoredAlbumSummary(
                albumName,
                "Pictures/" + albumName + "/",
                12,
                startDate,
                endDate,
                "content://thumbnail",
                null,
                123L,
                countryName,
                adminArea,
                addressLine);
    }
}
