package com.example.gallerysorter;

import android.net.Uri;

import java.util.Date;

final class PhotoItem {
    final boolean duplicateInTarget;
    final String locationKey;
    final String mimeType;
    final String name;
    final boolean noLocation;
    final Date takenAt;
    final boolean targetExists;
    final String targetRelativePath;
    final Uri uri;
    final boolean video;
    final String countryName;
    final String adminArea;
    final String addressLine;

    PhotoItem(Uri uri, String name, String mimeType, Date takenAt, String locationKey,
              boolean noLocation, boolean targetExists, boolean duplicateInTarget,
              String targetRelativePath, boolean video, String countryName,
              String adminArea, String addressLine) {
        this.uri = uri;
        this.name = name;
        this.mimeType = mimeType;
        this.takenAt = takenAt;
        this.locationKey = locationKey;
        this.noLocation = noLocation;
        this.targetExists = targetExists;
        this.duplicateInTarget = duplicateInTarget;
        this.targetRelativePath = targetRelativePath;
        this.video = video;
        this.countryName = countryName == null ? "" : countryName;
        this.adminArea = adminArea == null ? "" : adminArea;
        this.addressLine = addressLine == null ? "" : addressLine;
    }
}
