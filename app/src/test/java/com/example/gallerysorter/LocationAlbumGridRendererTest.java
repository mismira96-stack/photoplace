package com.example.gallerysorter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LocationAlbumGridRendererTest {

    private StoredAlbumSummary summary(String start, String end) {
        return new StoredAlbumSummary("Test", "path", 10, start, end, null, null, 0L, "", "", "");
    }

    @Test
    public void formatMonthRange_handlesNullAndEmpty() {
        assertEquals("날짜 정보 없음", LocationAlbumGridRenderer.formatMonthRange(null));
        assertEquals("날짜 정보 없음", LocationAlbumGridRenderer.formatMonthRange(summary("", "")));
    }

    @Test
    public void formatMonthRange_formatsSingleMonth() {
        assertEquals("2026.5", LocationAlbumGridRenderer.formatMonthRange(summary("2026-05-10", "2026-05-10")));
    }

    @Test
    public void formatMonthRange_formatsMonthSpan() {
        assertEquals("2025.1~2026.11", LocationAlbumGridRenderer.formatMonthRange(summary("2025-01-01", "2026-11-16")));
    }

    @Test
    public void formatMonthRange_handlesOneSidedDates() {
        assertEquals("2024.8", LocationAlbumGridRenderer.formatMonthRange(summary("2024-08-15", null)));
        assertEquals("2024.9", LocationAlbumGridRenderer.formatMonthRange(summary(null, "2024-09-01")));
    }
}
