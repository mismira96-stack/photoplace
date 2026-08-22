package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemoryBrowserSearchTest {
    @Test
    public void searchesPlaceCountryAliasAdminAndAddress() {
        MemoryRecord sapporo = record("삿포로", "JP", "Japan", "Hokkaido", "Sapporo, Hokkaido, Japan", 1785600000000L);
        MemoryRecord songpa = record("송파구", "KR", "대한민국", "서울특별시", "Songpa-gu, Seoul", 1754006400000L);
        List<MemoryRecord> records = Arrays.asList(sapporo, songpa);

        assertSame(sapporo, MemoryBrowserSearch.filter(records, "삿포로").get(0));
        assertSame(sapporo, MemoryBrowserSearch.filter(records, "sapporo").get(0));
        assertSame(sapporo, MemoryBrowserSearch.filter(records, "일본").get(0));
        assertSame(sapporo, MemoryBrowserSearch.filter(records, "japan").get(0));
        assertSame(sapporo, MemoryBrowserSearch.filter(records, "JP").get(0));
        assertSame(songpa, MemoryBrowserSearch.filter(records, "songpa").get(0));
    }

    @Test
    public void searchesYearMonthAndKoreanMonth() {
        MemoryRecord sapporo = record("삿포로", "JP", "Japan", "Hokkaido", "Sapporo", 1785600000000L);

        assertEquals(1, MemoryBrowserSearch.filter(Collections.singletonList(sapporo), "2026").size());
        assertEquals(1, MemoryBrowserSearch.filter(Collections.singletonList(sapporo), "2026.08").size());
        assertEquals(1, MemoryBrowserSearch.filter(Collections.singletonList(sapporo), "8월").size());
    }

    @Test
    public void emptyQueryReturnsOriginalList() {
        List<MemoryRecord> records = Collections.singletonList(record("수원", "KR", "대한민국", "경기도", "수원시", 0L));
        assertSame(records, MemoryBrowserSearch.filter(records, "  "));
    }

    private static MemoryRecord record(String title,
                                       String countryCode,
                                       String countryName,
                                       String adminArea,
                                       String addressLine,
                                       long dateMillis) {
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup(
                "discovery:" + title,
                title,
                title,
                countryCode,
                countryName,
                adminArea,
                addressLine,
                1,
                1,
                0,
                dateMillis,
                dateMillis,
                "content://media/1",
                Collections.<DiscoveryPhotoRef>emptyList(),
                0,
                1L);
        return MemoryRepository.fromDiscoveryGroup(group);
    }
}
