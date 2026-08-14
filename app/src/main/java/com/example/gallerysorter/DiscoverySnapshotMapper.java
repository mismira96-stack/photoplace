package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DiscoverySnapshotMapper {
    static final String DEFAULT_ANALYSIS_POLICY_VERSION = "discovery-mapper-v1";
    static final String DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION = "country-identity-v1";

    private DiscoverySnapshotMapper() {
    }

    static DiscoverySnapshot fromPhotoItems(List<PhotoItem> items,
                                            long snapshotVersion,
                                            long createdAtMillis,
                                            String sourceSignature) {
        return fromPhotoItems(
                items,
                snapshotVersion,
                createdAtMillis,
                sourceSignature,
                DEFAULT_ANALYSIS_POLICY_VERSION,
                DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);
    }

    static DiscoverySnapshot fromPhotoItems(List<PhotoItem> items,
                                            long snapshotVersion,
                                            long createdAtMillis,
                                            String sourceSignature,
                                            String analysisPolicyVersion,
                                            String countryIdentityPolicyVersion) {
        ArrayList<SourceItem> sourceItems = new ArrayList<>();
        if (items != null) {
            for (PhotoItem item : items) {
                if (item != null) {
                    sourceItems.add(SourceItem.fromPhotoItem(item));
                }
            }
        }
        return fromSourceItems(
                sourceItems,
                items == null ? 0 : items.size(),
                snapshotVersion,
                createdAtMillis,
                sourceSignature,
                analysisPolicyVersion,
                countryIdentityPolicyVersion);
    }

    static DiscoverySnapshot fromSourceItems(List<SourceItem> items,
                                             int sourceItemCount,
                                             long snapshotVersion,
                                             long createdAtMillis,
                                             String sourceSignature,
                                             String analysisPolicyVersion,
                                             String countryIdentityPolicyVersion) {
        LinkedHashMap<String, GroupBuilder> builders = new LinkedHashMap<>();
        if (items != null) {
            for (SourceItem item : items) {
                DiscoveryPhotoRef ref = photoRefFrom(item, snapshotVersion);
                if (ref == null) {
                    continue;
                }
                String placeKey = clean(item.locationKey);
                GroupBuilder builder = builders.get(placeKey);
                if (builder == null) {
                    builder = new GroupBuilder(placeKey, snapshotVersion);
                    builders.put(placeKey, builder);
                }
                builder.add(ref);
            }
        }
        ArrayList<DiscoveryMemoryGroup> groups = new ArrayList<>();
        for (Map.Entry<String, GroupBuilder> entry : builders.entrySet()) {
            groups.add(entry.getValue().build());
        }
        return new DiscoverySnapshot(
                DiscoverySnapshot.CURRENT_SCHEMA_VERSION,
                Math.max(0L, snapshotVersion),
                Math.max(0L, createdAtMillis),
                clean(sourceSignature),
                Math.max(0, sourceItemCount),
                groups,
                clean(analysisPolicyVersion),
                clean(countryIdentityPolicyVersion));
    }

    private static DiscoveryPhotoRef photoRefFrom(SourceItem item, long snapshotVersion) {
        if (item == null || item.noLocation) {
            return null;
        }
        String sourceUri = clean(item.sourceUri);
        String placeKey = clean(item.locationKey);
        if (sourceUri.isEmpty() || placeKey.isEmpty() || PlaceNamePolicy.LOCATION_NONE.equals(placeKey)) {
            return null;
        }
        return new DiscoveryPhotoRef(
                sourceUri,
                mediaStoreIdFrom(sourceUri),
                item.video ? MediaKind.VIDEO : MediaKind.PHOTO,
                item.mimeType,
                item.name,
                item.takenAtMillis,
                placeKey,
                placeKey,
                item.countryCode,
                item.countryName,
                item.adminArea,
                item.addressLine,
                "",
                Math.max(0L, snapshotVersion),
                Math.max(0L, snapshotVersion),
                false);
    }

    private static long mediaStoreIdFrom(String sourceUri) {
        String uri = clean(sourceUri);
        if (uri.isEmpty()) {
            return DiscoveryPhotoRef.UNKNOWN_ID;
        }
        try {
            int slash = uri.lastIndexOf('/');
            String segment = slash >= 0 ? uri.substring(slash + 1) : uri;
            return Long.parseLong(segment);
        } catch (Exception unused) {
            return DiscoveryPhotoRef.UNKNOWN_ID;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static final class SourceItem {
        final String sourceUri;
        final String name;
        final String mimeType;
        final long takenAtMillis;
        final String locationKey;
        final boolean noLocation;
        final boolean video;
        final String countryCode;
        final String countryName;
        final String adminArea;
        final String addressLine;

        SourceItem(String sourceUri,
                   String name,
                   String mimeType,
                   long takenAtMillis,
                   String locationKey,
                   boolean noLocation,
                   boolean video,
                   String countryCode,
                   String countryName,
                   String adminArea,
                   String addressLine) {
            this.sourceUri = clean(sourceUri);
            this.name = clean(name);
            this.mimeType = clean(mimeType);
            this.takenAtMillis = Math.max(0L, takenAtMillis);
            this.locationKey = clean(locationKey);
            this.noLocation = noLocation;
            this.video = video;
            this.countryCode = clean(countryCode);
            this.countryName = clean(countryName);
            this.adminArea = clean(adminArea);
            this.addressLine = clean(addressLine);
        }

        static SourceItem fromPhotoItem(PhotoItem item) {
            return new SourceItem(
                    item.uri == null ? "" : item.uri.toString(),
                    item.name,
                    item.mimeType,
                    item.takenAt == null ? DiscoveryPhotoRef.UNKNOWN_TIME : item.takenAt.getTime(),
                    item.locationKey,
                    item.noLocation,
                    item.video,
                    item.countryCode,
                    item.countryName,
                    item.adminArea,
                    item.addressLine);
        }
    }

    private static final class GroupBuilder {
        final String placeKey;
        final long snapshotVersion;
        final ArrayList<DiscoveryPhotoRef> refs = new ArrayList<>();
        String countryCode = "";
        String countryName = "";
        String adminArea = "";
        String addressLine = "";
        long startDateMillis = 0L;
        long endDateMillis = 0L;

        GroupBuilder(String placeKey, long snapshotVersion) {
            this.placeKey = placeKey;
            this.snapshotVersion = Math.max(0L, snapshotVersion);
        }

        void add(DiscoveryPhotoRef ref) {
            refs.add(ref);
            if (countryCode.isEmpty() && !ref.countryCode.isEmpty()) {
                countryCode = ref.countryCode;
            }
            if (countryName.isEmpty() && !ref.countryName.isEmpty()) {
                countryName = ref.countryName;
            }
            if (adminArea.isEmpty() && !ref.adminArea.isEmpty()) {
                adminArea = ref.adminArea;
            }
            if (addressLine.isEmpty() && !ref.addressLine.isEmpty()) {
                addressLine = ref.addressLine;
            }
            if (ref.takenAtMillis > 0L) {
                if (startDateMillis <= 0L || ref.takenAtMillis < startDateMillis) {
                    startDateMillis = ref.takenAtMillis;
                }
                if (endDateMillis <= 0L || ref.takenAtMillis > endDateMillis) {
                    endDateMillis = ref.takenAtMillis;
                }
            }
        }

        DiscoveryMemoryGroup build() {
            int photoCount = 0;
            int videoCount = 0;
            for (DiscoveryPhotoRef ref : refs) {
                if (ref.mediaKind == MediaKind.VIDEO) {
                    videoCount++;
                } else {
                    photoCount++;
                }
            }
            String coverUri = refs.isEmpty() ? "" : refs.get(0).sourceUri;
            return new DiscoveryMemoryGroup(
                    "discovery:" + placeKey,
                    placeKey,
                    placeKey,
                    countryCode,
                    countryName,
                    adminArea,
                    addressLine,
                    refs.size(),
                    photoCount,
                    videoCount,
                    startDateMillis,
                    endDateMillis,
                    coverUri,
                    refs,
                    0,
                    snapshotVersion);
        }
    }
}
