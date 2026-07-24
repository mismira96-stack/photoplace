package com.example.gallerysorter;

final class LocationLookupResult {
    static final String LOCATION_NONE = "위치없음";

    final String folderKey;
    final String countryName;
    final String adminArea;
    final String addressLine;

    LocationLookupResult(String folderKey, String countryName, String adminArea, String addressLine) {
        this.folderKey = folderKey == null || folderKey.isEmpty() ? LOCATION_NONE : folderKey;
        this.countryName = countryName == null ? "" : countryName;
        this.adminArea = adminArea == null ? "" : adminArea;
        this.addressLine = addressLine == null ? "" : addressLine;
    }

    static LocationLookupResult empty() {
        return new LocationLookupResult(LOCATION_NONE, "", "", "");
    }
}
