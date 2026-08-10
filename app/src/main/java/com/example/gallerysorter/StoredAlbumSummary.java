package com.example.gallerysorter;

import org.json.JSONObject;

final class StoredAlbumSummary {
    final String albumName;
    final String createdAt;
    final long createdAtMillis;
    final String endDate;
    final int itemCount;
    final String relativePath;
    final String startDate;
    final String thumbnailUri;
    final String countryCode;
    final String countryName;
    final String adminArea;
    final String addressLine;

    StoredAlbumSummary(String albumName, String relativePath, int itemCount, String startDate,
                       String endDate, String thumbnailUri, String createdAt,
                       long createdAtMillis, String countryName, String adminArea,
                       String addressLine) {
        this(albumName, relativePath, itemCount, startDate, endDate, thumbnailUri, createdAt,
                createdAtMillis, "", countryName, adminArea, addressLine);
    }

    StoredAlbumSummary(String albumName, String relativePath, int itemCount, String startDate,
                       String endDate, String thumbnailUri, String createdAt,
                       long createdAtMillis, String countryCode, String countryName,
                       String adminArea, String addressLine) {
        this.albumName = albumName == null ? "" : albumName;
        this.relativePath = relativePath == null ? "" : relativePath;
        this.itemCount = itemCount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.thumbnailUri = thumbnailUri;
        this.createdAt = createdAt;
        this.createdAtMillis = createdAtMillis;
        this.countryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        this.countryName = CountryIdentityNormalizer.displayName(this.countryCode, countryName);
        this.adminArea = PhotoItemJson.clean(adminArea);
        this.addressLine = PhotoItemJson.clean(addressLine);
    }

    static StoredAlbumSummary fromJson(JSONObject json) {
        return new StoredAlbumSummary(
                json.optString("albumName", ""),
                json.optString("relativePath", ""),
                json.optInt("itemCount", 0),
                json.optString("startDate", null),
                json.optString("endDate", null),
                json.optString("thumbnailUri", null),
                json.optString("createdAt", null),
                json.optLong("createdAtMillis", 0L),
                json.optString("countryCode", ""),
                json.optString("countryName", ""),
                json.optString("adminArea", ""),
                json.optString("addressLine", ""));
    }
}
