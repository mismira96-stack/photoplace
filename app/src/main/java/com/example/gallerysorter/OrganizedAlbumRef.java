package com.example.gallerysorter;

final class OrganizedAlbumRef {
    final String relativePath;
    final String albumName;
    final int itemCount;
    final String thumbnailUri;
    final String countryCode;
    final String countryName;
    final long startDateMillis;
    final long endDateMillis;
    final int photoCount;
    final int videoCount;
    final boolean mediaSplitKnown;

    OrganizedAlbumRef(String relativePath,
                      String albumName,
                      int itemCount,
                      String thumbnailUri,
                      String countryCode,
                      String countryName,
                      long startDateMillis,
                      long endDateMillis) {
        this(relativePath, albumName, itemCount, thumbnailUri, countryCode, countryName,
                startDateMillis, endDateMillis, 0, 0, false);
    }

    OrganizedAlbumRef(String relativePath,
                      String albumName,
                      int itemCount,
                      String thumbnailUri,
                      String countryCode,
                      String countryName,
                      long startDateMillis,
                      long endDateMillis,
                      int photoCount,
                      int videoCount,
                      boolean mediaSplitKnown) {
        this.relativePath = clean(relativePath);
        this.albumName = clean(albumName);
        this.itemCount = Math.max(0, itemCount);
        this.thumbnailUri = clean(thumbnailUri);
        this.countryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        this.countryName = CountryIdentityNormalizer.displayName(this.countryCode, countryName);
        this.startDateMillis = startDateMillis;
        this.endDateMillis = endDateMillis;
        this.photoCount = Math.max(0, photoCount);
        this.videoCount = Math.max(0, videoCount);
        this.mediaSplitKnown = mediaSplitKnown;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
