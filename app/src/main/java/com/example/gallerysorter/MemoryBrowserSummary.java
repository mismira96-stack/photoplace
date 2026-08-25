package com.example.gallerysorter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class MemoryBrowserSummary {
    final int placeCount;
    final int photoCount;
    final String yearRange;

    private MemoryBrowserSummary(int placeCount, int photoCount, String yearRange) {
        this.placeCount = Math.max(0, placeCount);
        this.photoCount = Math.max(0, photoCount);
        this.yearRange = yearRange == null ? "날짜 정보 없음" : yearRange;
    }

    static MemoryBrowserSummary from(List<MemoryRecord> records) {
        int placeCount = 0;
        int photoCount = 0;
        long firstDate = 0L;
        long lastDate = 0L;
        if (records != null) {
            for (MemoryRecord record : records) {
                if (record == null) {
                    continue;
                }
                placeCount++;
                photoCount += Math.max(0, record.itemCount);
                long start = record.startDateMillis > 0L ? record.startDateMillis : record.endDateMillis;
                long end = record.endDateMillis > 0L ? record.endDateMillis : record.startDateMillis;
                if (start > 0L && (firstDate <= 0L || start < firstDate)) {
                    firstDate = start;
                }
                if (end > 0L && end > lastDate) {
                    lastDate = end;
                }
            }
        }
        return new MemoryBrowserSummary(placeCount, photoCount, yearRange(firstDate, lastDate));
    }

    private static String yearRange(long firstDate, long lastDate) {
        if (firstDate <= 0L && lastDate <= 0L) {
            return "날짜 정보 없음";
        }
        long start = firstDate > 0L ? firstDate : lastDate;
        long end = lastDate > 0L ? lastDate : firstDate;
        String startYear = new SimpleDateFormat("yyyy", Locale.KOREA).format(new Date(start));
        String endYear = new SimpleDateFormat("yyyy", Locale.KOREA).format(new Date(end));
        return startYear.equals(endYear) ? startYear : startYear + " ~ " + endYear;
    }
}
