package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class StoredAlbumSummarySearch {
    private StoredAlbumSummarySearch() {
    }

    static List<StoredAlbumSummary> filter(List<StoredAlbumSummary> summaries, String query) {
        if (summaries == null || summaries.isEmpty()) {
            return Collections.emptyList();
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return summaries;
        }
        ArrayList<StoredAlbumSummary> filtered = new ArrayList<>();
        for (StoredAlbumSummary summary : summaries) {
            if (summary != null && matches(summary, normalizedQuery)) {
                filtered.add(summary);
            }
        }
        return filtered;
    }

    private static boolean matches(StoredAlbumSummary summary, String normalizedQuery) {
        return contains(summary.albumName, normalizedQuery)
                || contains(summary.countryCode, normalizedQuery)
                || contains(summary.countryName, normalizedQuery)
                || CountryIdentityNormalizer.matchesSearchQuery(summary.countryCode, summary.countryName, normalizedQuery)
                || contains(summary.adminArea, normalizedQuery)
                || contains(summary.addressLine, normalizedQuery)
                || containsDate(summary.startDate, normalizedQuery)
                || containsDate(summary.endDate, normalizedQuery)
                || contains(summary.relativePath, normalizedQuery);
    }

    private static boolean containsDate(String value, String normalizedQuery) {
        return contains(value, normalizedQuery) || contains(value == null ? "" : value.replace('-', '.'), normalizedQuery);
    }

    private static boolean contains(String value, String normalizedQuery) {
        return normalize(value).contains(normalizedQuery);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
