package com.example.gallerysorter;

import java.util.Objects;

final class StoredAlbumSummaryMetadataMerger {
    private StoredAlbumSummaryMetadataMerger() {
    }

    static StoredAlbumSummary mergeLiveWithStoredMetadata(StoredAlbumSummary liveSummary, StoredAlbumSummary storedSummary) {
        if (liveSummary == null || storedSummary == null) {
            return liveSummary;
        }
        String countryCode = firstNonEmpty(liveSummary.countryCode, storedSummary.countryCode);
        String countryName = firstNonEmpty(liveSummary.countryName, storedSummary.countryName);
        String adminArea = firstNonEmpty(liveSummary.adminArea, storedSummary.adminArea);
        String addressLine = firstNonEmpty(liveSummary.addressLine, storedSummary.addressLine);
        if (Objects.equals(countryCode, liveSummary.countryCode)
                && Objects.equals(countryName, liveSummary.countryName)
                && Objects.equals(adminArea, liveSummary.adminArea)
                && Objects.equals(addressLine, liveSummary.addressLine)) {
            return liveSummary;
        }
        return new StoredAlbumSummary(
                liveSummary.albumName,
                liveSummary.relativePath,
                liveSummary.itemCount,
                liveSummary.startDate,
                liveSummary.endDate,
                liveSummary.thumbnailUri,
                liveSummary.createdAt,
                liveSummary.createdAtMillis,
                countryCode,
                countryName,
                adminArea,
                addressLine);
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }
}
