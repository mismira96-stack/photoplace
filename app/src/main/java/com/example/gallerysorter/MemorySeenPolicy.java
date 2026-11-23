package com.example.gallerysorter;

import java.util.HashSet;
import java.util.Set;

/** Acknowledgement belongs to a discovery group/version, independent of album path and source cache. */
final class MemorySeenPolicy {
    private MemorySeenPolicy() { }

    static String key(MemoryRecord record) {
        return record == null || record.discoveryGroup == null ? "" : record.discoveryGroup.memoryKey;
    }

    static int recentCount(MemoryRecord record, long seenVersion) {
        if (record == null || record.discoveryGroup == null) return 0;
        DiscoveryMemoryGroup group = record.discoveryGroup;
        if (group.snapshotVersion <= seenVersion) return 0;
        Set<String> seen = new HashSet<>();
        int count = 0;
        for (DiscoveryPhotoRef ref : group.photoRefs) {
            if (ref != null && !ref.stale && ref.sourceUri != null && !ref.sourceUri.trim().isEmpty()
                    && ref.firstSeenSnapshotVersion == group.snapshotVersion && seen.add(ref.sourceUri)) count++;
        }
        return count;
    }

    static boolean canAcknowledge(MemoryBrowserDetail detail) {
        return detail != null && detail.mediaResolved && detail.canOpenPhotos && !key(detail.record).isEmpty();
    }
}
