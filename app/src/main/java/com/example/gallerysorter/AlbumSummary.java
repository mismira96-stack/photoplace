package com.example.gallerysorter;

final class AlbumSummary {
    final String albumName;
    final String relativePath;
    String thumbnailUri;
    String countryCode;
    String countryName;
    String adminArea;
    String addressLine;
    final DateRange dateRange = new DateRange();
    int itemCount = 0;
    long thumbnailDateMillis = 0;

    AlbumSummary(String albumName, String relativePath, String thumbnailUri) {
        this.albumName = albumName;
        this.relativePath = relativePath;
        this.thumbnailUri = thumbnailUri;
    }

    void includeLocationMetadata(String countryName, String adminArea, String addressLine) {
        includeLocationMetadata("", countryName, adminArea, addressLine);
    }

    void includeLocationMetadata(String countryCode, String countryName, String adminArea, String addressLine) {
        String normalizedCountryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        if ((this.countryCode == null || this.countryCode.isEmpty()) && !normalizedCountryCode.isEmpty()) {
            this.countryCode = normalizedCountryCode;
        }
        String displayCountry = CountryIdentityNormalizer.displayName(normalizedCountryCode, countryName);
        if ((this.countryName == null || this.countryName.isEmpty()) && displayCountry != null && !displayCountry.trim().isEmpty()) {
            this.countryName = displayCountry.trim();
        }
        if ((this.adminArea == null || this.adminArea.isEmpty()) && adminArea != null && !adminArea.trim().isEmpty()) {
            this.adminArea = adminArea.trim();
        }
        if ((this.addressLine == null || this.addressLine.isEmpty()) && addressLine != null && !addressLine.trim().isEmpty()) {
            this.addressLine = addressLine.trim();
        }
    }
}
