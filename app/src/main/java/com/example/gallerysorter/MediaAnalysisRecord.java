package com.example.gallerysorter;

import java.util.Date;

final class MediaAnalysisRecord {
    static final String STATUS_ANALYZED = "ANALYZED";
    static final String STATUS_NO_LOCATION = "NO_LOCATION";

    final String status;
    final long takenAtMillis;
    final String folderKey;
    final String countryCode;
    final String countryName;
    final String adminArea;
    final String addressLine;

    MediaAnalysisRecord(String status,
                        long takenAtMillis,
                        String folderKey,
                        String countryCode,
                        String countryName,
                        String adminArea,
                        String addressLine) {
        this.status = clean(status);
        this.takenAtMillis = Math.max(0L, takenAtMillis);
        this.folderKey = clean(folderKey);
        this.countryCode = clean(countryCode);
        this.countryName = clean(countryName);
        this.adminArea = clean(adminArea);
        this.addressLine = clean(addressLine);
    }

    static MediaAnalysisRecord from(LocationResult result) {
        if (result == null) {
            return null;
        }
        String status = PlaceNamePolicy.LOCATION_NONE.equals(result.folderKey)
                ? STATUS_NO_LOCATION
                : STATUS_ANALYZED;
        return new MediaAnalysisRecord(
                status,
                result.takenAt == null ? 0L : result.takenAt.getTime(),
                result.folderKey,
                result.countryCode,
                result.countryName,
                result.adminArea,
                result.addressLine);
    }

    LocationResult toLocationResult() {
        Date takenAt = takenAtMillis > 0L ? new Date(takenAtMillis) : null;
        return new LocationResult(takenAt, folderKey, countryCode, countryName, adminArea, addressLine);
    }

    boolean isNoLocation() {
        return STATUS_NO_LOCATION.equals(status);
    }

    private static String clean(String value) {
        return value == null ? "" : value;
    }
}
