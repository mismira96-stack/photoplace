package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class MemoryBrowserSummaryTest {
    @Test
    public void emptyRecordsUseZeroCountsAndUnknownDate() {
        MemoryBrowserSummary summary = MemoryBrowserSummary.from(Collections.<MemoryRecord>emptyList());

        assertEquals(0, summary.placeCount);
        assertEquals(0, summary.photoCount);
        assertEquals("날짜 정보 없음", summary.yearRange);
    }

    @Test
    public void recordsInSameYearUseSingleYear() {
        MemoryBrowserSummary summary = MemoryBrowserSummary.from(Arrays.asList(
                record(1234, 1785600000000L, 1785945600000L),
                record(2, 1785945600000L, 1786000000000L)));

        assertEquals(2, summary.placeCount);
        assertEquals(1236, summary.photoCount);
        assertEquals("2026", summary.yearRange);
    }

    @Test
    public void recordsAcrossYearsUseFullRange() {
        MemoryBrowserSummary summary = MemoryBrowserSummary.from(Arrays.asList(
                record(1, 1672531200000L, 1672531200000L),
                record(1, 1786000000000L, 1786000000000L)));

        assertEquals("2023 ~ 2026", summary.yearRange);
    }

    private static MemoryRecord record(int itemCount, long startDateMillis, long endDateMillis) {
        return new MemoryRecord(
                "discovery:test-" + itemCount + "-" + startDateMillis,
                "test",
                "test",
                "test",
                "",
                "KR",
                "대한민국",
                "",
                "",
                itemCount,
                itemCount,
                0,
                startDateMillis,
                endDateMillis,
                "",
                MemorySourceType.DISCOVERED_ONLY,
                null,
                null,
                0,
                itemCount,
                true,
                false,
                true,
                false);
    }
}
