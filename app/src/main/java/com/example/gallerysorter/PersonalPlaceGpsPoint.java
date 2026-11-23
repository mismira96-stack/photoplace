package com.example.gallerysorter;

/**
 * Raw GPS coordinate observation for a single media item.
 * Distinguishes confirmed absence of GPS tags from transient/permission failures.
 */
final class PersonalPlaceGpsPoint {
    enum Status {
        HAS_GPS,
        NO_GPS_TAG,
        READ_DEFERRED_OR_DENIED
    }

    final String sourceUri;
    final String fingerprint;
    final Status status;
    final double latitude;
    final double longitude;

    private PersonalPlaceGpsPoint(String sourceUri,
                                  String fingerprint,
                                  Status status,
                                  double latitude,
                                  double longitude) {
        this.sourceUri = sourceUri == null ? "" : sourceUri.trim();
        this.fingerprint = fingerprint == null ? "" : fingerprint.trim();
        this.status = status == null ? Status.READ_DEFERRED_OR_DENIED : status;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    static PersonalPlaceGpsPoint withCoordinates(String sourceUri,
                                                 String fingerprint,
                                                 double latitude,
                                                 double longitude) {
        if (!PersonalPlace.isUsableCoordinate(latitude, longitude)) {
            return noGps(sourceUri, fingerprint);
        }
        return new PersonalPlaceGpsPoint(sourceUri, fingerprint, Status.HAS_GPS, latitude, longitude);
    }

    static PersonalPlaceGpsPoint noGps(String sourceUri, String fingerprint) {
        return new PersonalPlaceGpsPoint(sourceUri, fingerprint, Status.NO_GPS_TAG, 0.0d, 0.0d);
    }

    static PersonalPlaceGpsPoint deferred(String sourceUri, String fingerprint) {
        return new PersonalPlaceGpsPoint(sourceUri, fingerprint, Status.READ_DEFERRED_OR_DENIED, 0.0d, 0.0d);
    }

    boolean hasCoordinates() {
        return status == Status.HAS_GPS && PersonalPlace.isUsableCoordinate(latitude, longitude);
    }

    boolean isCacheableOnDisk() {
        return status == Status.HAS_GPS || status == Status.NO_GPS_TAG;
    }

    static String buildFingerprint(DiscoveryPhotoRef ref) {
        if (ref == null) {
            return "";
        }
        String name = ref.displayName == null ? "" : ref.displayName.trim();
        String base = ref.mediaStoreId + ":" + ref.takenAtMillis + ":" + name;
        if (name.regionMatches(true, 0, "Beauty_", 0, 7)) {
            return base + ":qb2";
        }
        return base;
    }
}
