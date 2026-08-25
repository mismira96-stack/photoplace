package com.example.gallerysorter;

import java.util.Date;

final class MediaAnalysisEntry {
    static final String STATUS_ANALYZED = "ANALYZED";
    static final String STATUS_NO_LOCATION = "NO_LOCATION";

    final String signature;
    final String status;
    final long takenAtMillis;
    final String folderKey;
    final String countryCode;
    final String countryName;
    final String adminArea;
    final String addressLine;
    final int policyVersion;

    MediaAnalysisEntry(String signature,
                       String status,
                       long takenAtMillis,
                       String folderKey,
                       String countryCode,
                       String countryName,
                       String adminArea,
                       String addressLine,
                       int policyVersion) {
        this.signature = clean(signature);
        this.status = STATUS_NO_LOCATION.equals(status) ? STATUS_NO_LOCATION : STATUS_ANALYZED;
        this.takenAtMillis = Math.max(0L, takenAtMillis);
        this.folderKey = clean(folderKey);
        this.countryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        this.countryName = CountryIdentityNormalizer.displayName(this.countryCode, countryName);
        this.adminArea = clean(adminArea);
        this.addressLine = clean(addressLine);
        this.policyVersion = Math.max(1, policyVersion);
    }

    static MediaAnalysisEntry fromLocationResult(String signature, LocationResult result, int policyVersion) {
        LocationResult safe = result == null
                ? new LocationResult(null, LocationLookupResult.LOCATION_NONE, "", "", "")
                : result;
        boolean noLocation = LocationLookupResult.LOCATION_NONE.equals(safe.folderKey);
        Date takenAt = safe.takenAt;
        return new MediaAnalysisEntry(
                signature,
                noLocation ? STATUS_NO_LOCATION : STATUS_ANALYZED,
                takenAt == null ? 0L : takenAt.getTime(),
                safe.folderKey,
                safe.countryCode,
                safe.countryName,
                safe.adminArea,
                safe.addressLine,
                policyVersion);
    }

    LocationResult toLocationResult() {
        Date takenAt = takenAtMillis > 0L ? new Date(takenAtMillis) : null;
        String key = STATUS_NO_LOCATION.equals(status) ? LocationLookupResult.LOCATION_NONE : folderKey;
        return new LocationResult(takenAt, key, countryCode, countryName, adminArea, addressLine);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
