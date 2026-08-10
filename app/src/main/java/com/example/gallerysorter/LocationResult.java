package com.example.gallerysorter;

import java.util.Date;

final class LocationResult {
    final String folderKey;
    final Date takenAt;
    final String countryCode;
    final String countryName;
    final String adminArea;
    final String addressLine;

    LocationResult(Date takenAt, String folderKey, String countryName, String adminArea, String addressLine) {
        this(takenAt, folderKey, "", countryName, adminArea, addressLine);
    }

    LocationResult(Date takenAt, String folderKey, String countryCode, String countryName, String adminArea, String addressLine) {
        this.takenAt = takenAt;
        this.folderKey = folderKey;
        this.countryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        this.countryName = CountryIdentityNormalizer.displayName(this.countryCode, countryName);
        this.adminArea = adminArea == null ? "" : adminArea;
        this.addressLine = addressLine == null ? "" : addressLine;
    }
}
