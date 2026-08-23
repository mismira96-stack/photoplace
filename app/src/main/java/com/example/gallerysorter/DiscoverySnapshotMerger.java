package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DiscoverySnapshotMerger {
    private DiscoverySnapshotMerger() {
    }

    static DiscoverySnapshot replaceAnalyzedItems(DiscoverySnapshot existing,
                                                   DiscoverySnapshot incoming,
                                                   Set<String> analyzedSourceUris) {
        if (incoming == null) {
            return existing;
        }
        LinkedHashMap<String, DiscoveryPhotoRef> refsByUri = new LinkedHashMap<>();
        addExistingRefs(refsByUri, existing, cleanSet(analyzedSourceUris));
        addIncomingRefs(refsByUri, incoming);
        return rebuild(existing, incoming, refsByUri.values());
    }

    private static void addExistingRefs(Map<String, DiscoveryPhotoRef> refsByUri,
                                        DiscoverySnapshot existing,
                                        Set<String> analyzedSourceUris) {
        if (existing == null) {
            return;
        }
        for (DiscoveryMemoryGroup group : existing.groups) {
            if (group == null) {
                continue;
            }
            for (DiscoveryPhotoRef ref : group.photoRefs) {
                String uri = clean(ref == null ? "" : ref.sourceUri);
                if (!uri.isEmpty() && !analyzedSourceUris.contains(uri)) {
                    refsByUri.put(uri, ref);
                }
            }
        }
    }

    private static void addIncomingRefs(Map<String, DiscoveryPhotoRef> refsByUri,
                                        DiscoverySnapshot incoming) {
        for (DiscoveryMemoryGroup group : incoming.groups) {
            if (group == null) {
                continue;
            }
            for (DiscoveryPhotoRef ref : group.photoRefs) {
                String uri = clean(ref == null ? "" : ref.sourceUri);
                if (!uri.isEmpty()) {
                    refsByUri.put(uri, ref);
                }
            }
        }
    }

    private static DiscoverySnapshot rebuild(DiscoverySnapshot existing,
                                             DiscoverySnapshot incoming,
                                             Iterable<DiscoveryPhotoRef> refs) {
        LinkedHashMap<String, GroupBuilder> groups = new LinkedHashMap<>();
        for (DiscoveryPhotoRef ref : refs) {
            if (ref == null) {
                continue;
            }
            String placeKey = clean(ref.locationKey);
            if (placeKey.isEmpty()) {
                continue;
            }
            GroupBuilder builder = groups.get(placeKey);
            if (builder == null) {
                builder = new GroupBuilder(placeKey);
                groups.put(placeKey, builder);
            }
            builder.add(ref);
        }
        ArrayList<DiscoveryMemoryGroup> mergedGroups = new ArrayList<>();
        for (GroupBuilder builder : groups.values()) {
            mergedGroups.add(builder.build(incoming.snapshotVersion));
        }
        long createdAt = existing == null || existing.createdAtMillis <= 0L
                ? incoming.createdAtMillis
                : Math.min(existing.createdAtMillis, incoming.createdAtMillis);
        return new DiscoverySnapshot(
                DiscoverySnapshot.CURRENT_SCHEMA_VERSION,
                incoming.snapshotVersion,
                createdAt,
                incoming.sourceSignature,
                refsCount(mergedGroups),
                mergedGroups,
                incoming.analysisPolicyVersion,
                incoming.countryIdentityPolicyVersion);
    }

    private static int refsCount(List<DiscoveryMemoryGroup> groups) {
        int count = 0;
        for (DiscoveryMemoryGroup group : groups) {
            count += group.photoRefs.size();
        }
        return count;
    }

    private static Set<String> cleanSet(Set<String> values) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String uri = clean(value);
                if (!uri.isEmpty()) {
                    cleaned.add(uri);
                }
            }
        }
        return cleaned;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class GroupBuilder {
        final String placeKey;
        final ArrayList<DiscoveryPhotoRef> refs = new ArrayList<>();
        String countryCode = "";
        String countryName = "";
        String adminArea = "";
        String addressLine = "";
        long startDateMillis;
        long endDateMillis;
        long firstSeenVersion;
        long lastSeenVersion;

        GroupBuilder(String placeKey) {
            this.placeKey = placeKey;
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
            if (ref.firstSeenSnapshotVersion > 0L
                    && (firstSeenVersion <= 0L || ref.firstSeenSnapshotVersion < firstSeenVersion)) {
                firstSeenVersion = ref.firstSeenSnapshotVersion;
            }
            lastSeenVersion = Math.max(lastSeenVersion, ref.lastSeenSnapshotVersion);
        }

        DiscoveryMemoryGroup build(long currentSnapshotVersion) {
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
                    Math.max(currentSnapshotVersion, lastSeenVersion));
        }
    }
}
