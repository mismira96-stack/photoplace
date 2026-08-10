package com.example.gallerysorter;

final class MemoryItem {
    final String albumName;
    final String relativePath;
    final int itemCount;
    final String startDate;
    final String endDate;
    final String thumbnailUri;
    final String countryCode;
    final String countryName;
    final String adminArea;
    final String addressLine;

    MemoryItem(
            String albumName,
            String relativePath,
            int itemCount,
            String startDate,
            String endDate,
            String thumbnailUri,
            String countryName,
            String adminArea,
            String addressLine) {
        this(albumName, relativePath, itemCount, startDate, endDate, thumbnailUri, "", countryName, adminArea, addressLine);
    }

    MemoryItem(
            String albumName,
            String relativePath,
            int itemCount,
            String startDate,
            String endDate,
            String thumbnailUri,
            String countryCode,
            String countryName,
            String adminArea,
            String addressLine) {
        this.albumName = albumName == null ? "" : albumName;
        this.relativePath = relativePath == null ? "" : relativePath;
        this.itemCount = itemCount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.thumbnailUri = thumbnailUri;
        this.countryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        this.countryName = CountryIdentityNormalizer.displayName(this.countryCode, countryName);
        this.adminArea = adminArea == null ? "" : adminArea;
        this.addressLine = addressLine == null ? "" : addressLine;
    }
}
