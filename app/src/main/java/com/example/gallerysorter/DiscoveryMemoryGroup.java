package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DiscoveryMemoryGroup {
    final String memoryKey;
    final String placeKey;
    final String placeName;
    final String countryCode;
    final String countryName;
    final String adminArea;
    final String addressLine;
    final int itemCount;
    final int photoCount;
    final int videoCount;
    final long startDateMillis;
    final long endDateMillis;
    final String coverUri;
    final List<DiscoveryPhotoRef> photoRefs;
    final int staleCount;
    final long snapshotVersion;
    final boolean hasUserNotes;

    DiscoveryMemoryGroup(String memoryKey,
                         String placeKey,
                         String placeName,
                         String countryCode,
                         String countryName,
                         String adminArea,
                         String addressLine,
                         int itemCount,
                         int photoCount,
                         int videoCount,
                         long startDateMillis,
                         long endDateMillis,
                         String coverUri,
                         List<DiscoveryPhotoRef> photoRefs,
                         int staleCount,
                         long snapshotVersion) {
        this(memoryKey,placeKey,placeName,countryCode,countryName,adminArea,addressLine,itemCount,photoCount,
                videoCount,startDateMillis,endDateMillis,coverUri,photoRefs,staleCount,snapshotVersion,false);
    }
    DiscoveryMemoryGroup(String memoryKey,String placeKey,String placeName,String countryCode,String countryName,
                         String adminArea,String addressLine,int itemCount,int photoCount,int videoCount,
                         long startDateMillis,long endDateMillis,String coverUri,List<DiscoveryPhotoRef> photoRefs,
                         int staleCount,long snapshotVersion,boolean hasUserNotes) {
        this.memoryKey = clean(memoryKey);
        this.placeKey = clean(placeKey);
        this.placeName = clean(placeName);
        this.countryCode = CountryIdentityNormalizer.countryCode(countryCode, countryName);
        this.countryName = CountryIdentityNormalizer.displayName(this.countryCode, countryName);
        this.adminArea = clean(adminArea);
        this.addressLine = clean(addressLine);
        this.itemCount = Math.max(0, itemCount);
        this.photoCount = Math.max(0, photoCount);
        this.videoCount = Math.max(0, videoCount);
        this.startDateMillis = startDateMillis;
        this.endDateMillis = endDateMillis;
        this.coverUri = clean(coverUri);
        this.photoRefs = immutableCopy(photoRefs);
        this.staleCount = Math.max(0, staleCount);
        this.snapshotVersion = snapshotVersion;
        this.hasUserNotes=hasUserNotes;
    }

    private static List<DiscoveryPhotoRef> immutableCopy(List<DiscoveryPhotoRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(refs));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
