package com.example.gallerysorter;

final class LocationLookupResult {
    static final String LOCATION_NONE = "위치없음";

    final String folderKey;
    final String countryCode;
    final String countryName;
    final String adminArea;
    final String addressLine;

    LocationLookupResult(String folderKey, String countryName, String adminArea, String addressLine) {
        this(folderKey, "", countryName, adminArea, addressLine);
    }

    LocationLookupResult(String folderKey, String countryCode, String countryName, String adminArea, String addressLine) {
        this.folderKey = folderKey == null || folderKey.isEmpty() ? LOCATION_NONE : folderKey;
        this.countryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        this.countryName = CountryIdentityNormalizer.displayName(this.countryCode, countryName);
        this.adminArea = adminArea == null ? "" : adminArea;
        this.addressLine = addressLine == null ? "" : addressLine;
    }

    static LocationLookupResult empty() {
        return new LocationLookupResult(LOCATION_NONE, "", "", "");
    }
}
