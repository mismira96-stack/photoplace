package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DiscoverySnapshot {
    static final int CURRENT_SCHEMA_VERSION = 1;

    final int schemaVersion;
    final long snapshotVersion;
    final long createdAtMillis;
    final String sourceSignature;
    final int sourceItemCount;
    final List<DiscoveryMemoryGroup> groups;
    final String analysisPolicyVersion;
    final String countryIdentityPolicyVersion;

    DiscoverySnapshot(int schemaVersion,
                      long snapshotVersion,
                      long createdAtMillis,
                      String sourceSignature,
                      int sourceItemCount,
                      List<DiscoveryMemoryGroup> groups,
                      String analysisPolicyVersion,
                      String countryIdentityPolicyVersion) {
        this.schemaVersion = schemaVersion <= 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
        this.snapshotVersion = snapshotVersion;
        this.createdAtMillis = createdAtMillis;
        this.sourceSignature = clean(sourceSignature);
        this.sourceItemCount = Math.max(0, sourceItemCount);
        this.groups = immutableCopy(groups);
        this.analysisPolicyVersion = clean(analysisPolicyVersion);
        this.countryIdentityPolicyVersion = clean(countryIdentityPolicyVersion);
    }

    int groupCount() {
        return groups.size();
    }

    private static List<DiscoveryMemoryGroup> immutableCopy(List<DiscoveryMemoryGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(groups));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
