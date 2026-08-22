package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Converts discovery-only memories into the existing SortWorker input contract. */
final class DiscoveryAlbumOrganizer {
    interface AlbumLookup {
        String resolveTargetRelativePath(String placeName, String proposedRelativePath) throws Exception;

        Match find(String targetRelativePath, String displayName, boolean video) throws Exception;
    }

    static final class Match {
        final boolean albumExists;
        final boolean duplicateExists;

        Match(boolean albumExists, boolean duplicateExists) {
            this.albumExists = albumExists;
            this.duplicateExists = duplicateExists;
        }
    }

    static final class Preparation {
        final List<PreparedItem> items;
        final int placeCount;
        final int copyableCount;
        final int duplicateCount;
        final int skippedRefCount;

        Preparation(List<PreparedItem> items,
                    int placeCount,
                    int copyableCount,
                    int duplicateCount,
                    int skippedRefCount) {
            this.items = items == null
                    ? Collections.<PreparedItem>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(items));
            this.placeCount = Math.max(0, placeCount);
            this.copyableCount = Math.max(0, copyableCount);
            this.duplicateCount = Math.max(0, duplicateCount);
            this.skippedRefCount = Math.max(0, skippedRefCount);
        }

        static Preparation empty() {
            return new Preparation(Collections.<PreparedItem>emptyList(), 0, 0, 0, 0);
        }
    }

    static final class PreparedItem {
        final String sourceUri;
        final String displayName;
        final String mimeType;
        final long takenAtMillis;
        final String locationKey;
        final boolean targetExists;
        final boolean duplicateInTarget;
        final String targetRelativePath;
        final boolean video;
        final String countryCode;
        final String countryName;
        final String adminArea;
        final String addressLine;

        PreparedItem(DiscoveryPhotoRef ref,
                     String locationKey,
                     String targetRelativePath,
                     Match match) {
            this.sourceUri = ref.sourceUri;
            this.displayName = ref.displayName;
            this.mimeType = ref.mimeType;
            this.takenAtMillis = ref.takenAtMillis;
            this.locationKey = locationKey;
            this.targetExists = match.albumExists;
            this.duplicateInTarget = match.duplicateExists;
            this.targetRelativePath = targetRelativePath;
            this.video = ref.mediaKind == MediaKind.VIDEO;
            this.countryCode = ref.countryCode;
            this.countryName = ref.countryName;
            this.adminArea = ref.adminArea;
            this.addressLine = ref.addressLine;
        }
    }

    private DiscoveryAlbumOrganizer() {
    }

    static Preparation prepare(List<MemoryRecord> records, AlbumLookup lookup) throws Exception {
        if (records == null || records.isEmpty() || lookup == null) {
            return Preparation.empty();
        }
        ArrayList<PreparedItem> items = new ArrayList<>();
        Set<String> addedUris = new HashSet<>();
        int placeCount = 0;
        int copyableCount = 0;
        int duplicateCount = 0;
        int skippedRefCount = 0;
        for (MemoryRecord record : records) {
            OrganizePlaceService.Plan plan = OrganizePlaceService.planFor(record);
            skippedRefCount += plan.skippedRefCount;
            if (!plan.canOrganize()) {
                continue;
            }
            String targetRelativePath = lookup.resolveTargetRelativePath(
                    plan.placeName,
                    plan.targetRelativePath);
            if (targetRelativePath == null || targetRelativePath.trim().isEmpty()) {
                skippedRefCount += plan.refs.size();
                continue;
            }
            int addedForPlace = 0;
            for (DiscoveryPhotoRef ref : plan.refs) {
                if (ref == null || ref.sourceUri.isEmpty() || !addedUris.add(ref.sourceUri)) {
                    skippedRefCount++;
                    continue;
                }
                boolean video = ref.mediaKind == MediaKind.VIDEO;
                Match match = lookup.find(targetRelativePath, ref.displayName, video);
                PreparedItem item = new PreparedItem(
                        ref,
                        firstNonEmpty(ref.locationKey, plan.placeName),
                        targetRelativePath,
                        match);
                items.add(item);
                addedForPlace++;
                if (match.duplicateExists) {
                    duplicateCount++;
                } else {
                    copyableCount++;
                }
            }
            if (addedForPlace > 0) {
                placeCount++;
            }
        }
        return new Preparation(items, placeCount, copyableCount, duplicateCount, skippedRefCount);
    }

    private static String firstNonEmpty(String first, String second) {
        String value = first == null ? "" : first.trim();
        return value.isEmpty() ? (second == null ? "" : second.trim()) : value;
    }
}
