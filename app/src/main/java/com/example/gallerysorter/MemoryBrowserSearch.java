package com.example.gallerysorter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class MemoryBrowserSearch {
    private MemoryBrowserSearch() {
    }

    static List<MemoryRecord> filter(List<MemoryRecord> records, String query) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return records;
        }
        ArrayList<MemoryRecord> filtered = new ArrayList<>();
        for (MemoryRecord record : records) {
            if (record != null && matches(record, normalizedQuery)) {
                filtered.add(record);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    private static boolean matches(MemoryRecord record, String query) {
        return contains(record.title, query)
                || contains(record.displayName, query)
                || contains(record.canonicalPlaceName, query)
                || contains(record.placeKey, query)
                || contains(record.countryCode, query)
                || contains(record.countryName, query)
                || CountryIdentityNormalizer.matchesSearchQuery(record.countryCode, record.countryName, query)
                || contains(record.adminArea, query)
                || contains(record.addressLine, query)
                || containsDate(record.startDateMillis, query)
                || containsDate(record.endDateMillis, query);
    }

    private static boolean containsDate(long millis, String query) {
        if (millis <= 0L) {
            return false;
        }
        Date date = new Date(millis);
        String[] patterns = {"yyyy-MM-dd", "yyyy.MM.dd", "yyyy년 M월 d일", "yyyy년 M월", "M월"};
        for (String pattern : patterns) {
            if (contains(new SimpleDateFormat(pattern, Locale.KOREA).format(date), query)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(String value, String query) {
        return normalize(value).contains(query);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
