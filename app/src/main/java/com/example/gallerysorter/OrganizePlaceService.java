package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a side-effect-free organization plan from an in-app memory.
 * The actual SortWorker input conversion stays in an Android-aware adapter later.
 */
final class OrganizePlaceService {
    private static final String PICTURES_PREFIX = "Pictures/";

    private OrganizePlaceService() {
    }

    static Plan planFor(MemoryRecord record) {
        if (record == null || record.discoveryGroup == null) {
            return Plan.empty();
        }
        String placeName = firstNonEmpty(record.displayName, record.canonicalPlaceName, record.title);
        if (placeName.isEmpty()) {
            return Plan.empty();
        }
        List<DiscoveryPhotoRef> refs = availableRefs(record.discoveryGroup.photoRefs);
        return new Plan(
                record.memoryKey,
                placeName,
                targetRelativePath(placeName),
                refs,
                record.discoveryGroup.photoRefs.size() - refs.size());
    }

    private static List<DiscoveryPhotoRef> availableRefs(List<DiscoveryPhotoRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<DiscoveryPhotoRef> available = new ArrayList<>();
        for (DiscoveryPhotoRef ref : refs) {
            if (ref != null && !ref.stale && !clean(ref.sourceUri).isEmpty()) {
                available.add(ref);
            }
        }
        return Collections.unmodifiableList(available);
    }

    private static String targetRelativePath(String placeName) {
        String value = clean(placeName);
        if (value.isEmpty()) {
            return "";
        }
        return PICTURES_PREFIX + (value.endsWith("에서") ? value : value + "에서") + "/";
    }

    private static String firstNonEmpty(String first, String second, String third) {
        String value = clean(first);
        if (!value.isEmpty()) {
            return value;
        }
        value = clean(second);
        return value.isEmpty() ? clean(third) : value;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static final class Plan {
        final String memoryKey;
        final String placeName;
        final String targetRelativePath;
        final List<DiscoveryPhotoRef> refs;
        final int skippedRefCount;

        Plan(String memoryKey,
             String placeName,
             String targetRelativePath,
             List<DiscoveryPhotoRef> refs,
             int skippedRefCount) {
            this.memoryKey = clean(memoryKey);
            this.placeName = clean(placeName);
            this.targetRelativePath = clean(targetRelativePath);
            this.refs = refs == null ? Collections.emptyList() : refs;
            this.skippedRefCount = Math.max(0, skippedRefCount);
        }

        boolean canOrganize() {
            return !placeName.isEmpty() && !targetRelativePath.isEmpty() && !refs.isEmpty();
        }

        static Plan empty() {
            return new Plan("", "", "", Collections.<DiscoveryPhotoRef>emptyList(), 0);
        }
    }
}
