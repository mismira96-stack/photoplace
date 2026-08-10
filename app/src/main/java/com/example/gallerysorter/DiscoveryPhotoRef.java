package com.example.gallerysorter;

final class DiscoveryPhotoRef {
    static final long UNKNOWN_ID = -1L;
    static final long UNKNOWN_TIME = 0L;

    final String sourceUri;
    final long mediaStoreId;
    final MediaKind mediaKind;
    final String mimeType;
    final String displayName;
    final long takenAtMillis;
    final String locationKey;
    final String placeName;
    final String countryCode;
    final String countryName;
    final String adminArea;
    final String addressLine;
    final String sourceRelativePath;
    final long firstSeenSnapshotVersion;
    final long lastSeenSnapshotVersion;
    final boolean stale;

    DiscoveryPhotoRef(String sourceUri,
                      long mediaStoreId,
                      MediaKind mediaKind,
                      String mimeType,
                      String displayName,
                      long takenAtMillis,
                      String locationKey,
                      String placeName,
                      String countryCode,
                      String countryName,
                      String adminArea,
                      String addressLine,
                      String sourceRelativePath,
                      long firstSeenSnapshotVersion,
                      long lastSeenSnapshotVersion,
                      boolean stale) {
        this.sourceUri = clean(sourceUri);
        this.mediaStoreId = mediaStoreId;
        this.mediaKind = mediaKind == null ? MediaKind.PHOTO : mediaKind;
        this.mimeType = clean(mimeType);
        this.displayName = clean(displayName);
        this.takenAtMillis = takenAtMillis;
        this.locationKey = clean(locationKey);
        this.placeName = clean(placeName);
        this.countryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        this.countryName = CountryIdentityNormalizer.displayName(this.countryCode, countryName);
        this.adminArea = clean(adminArea);
        this.addressLine = clean(addressLine);
        this.sourceRelativePath = clean(sourceRelativePath);
        this.firstSeenSnapshotVersion = firstSeenSnapshotVersion;
        this.lastSeenSnapshotVersion = lastSeenSnapshotVersion;
        this.stale = stale;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
