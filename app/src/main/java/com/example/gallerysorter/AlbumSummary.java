package com.example.gallerysorter;

final class AlbumSummary {
    final String albumName;
    final String relativePath;
    String thumbnailUri;
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
        if ((this.countryName == null || this.countryName.isEmpty()) && countryName != null && !countryName.trim().isEmpty()) {
            this.countryName = countryName.trim();
        }
        if ((this.adminArea == null || this.adminArea.isEmpty()) && adminArea != null && !adminArea.trim().isEmpty()) {
            this.adminArea = adminArea.trim();
        }
        if ((this.addressLine == null || this.addressLine.isEmpty()) && addressLine != null && !addressLine.trim().isEmpty()) {
            this.addressLine = addressLine.trim();
        }
    }
}
