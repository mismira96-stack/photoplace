package com.example.gallerysorter;

import java.util.Date;

final class LocationResult {
    final String folderKey;
    final Date takenAt;
    final String countryName;
    final String adminArea;
    final String addressLine;

    LocationResult(Date takenAt, String folderKey, String countryName, String adminArea, String addressLine) {
        this.takenAt = takenAt;
        this.folderKey = folderKey;
        this.countryName = countryName == null ? "" : countryName;
        this.adminArea = adminArea == null ? "" : adminArea;
        this.addressLine = addressLine == null ? "" : addressLine;
    }
}
