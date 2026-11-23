package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Durable receipts for a user-selected set. No cached review result authorizes a mutation. */
final class CleanupExecutionPlan {
    static final int BATCH_SIZE = 1000;
    static final int LEGACY_BATCH_SIZE = 300;
    final int batchSize;
    static final class Proof {
        final CleanupEvidenceRecord record;
        final String hash, sourceKey, targetKey, sourceIdentity, targetIdentity;
        Proof(CleanupEvidenceRecord record, String hash, String sourceKey, String targetKey,
              String sourceIdentity, String targetIdentity) {
            this.record = record; this.hash = hash; this.sourceKey = sourceKey; this.targetKey = targetKey;
            this.sourceIdentity = sourceIdentity; this.targetIdentity = targetIdentity;
        }
    }
    final List<Proof> proofs;
    final List<String> sources;
    CleanupExecutionPlan(List<Proof> input) {
        this(input,BATCH_SIZE);
    }
    CleanupExecutionPlan(List<Proof> input,int batchSize) {
        if(batchSize<1 || batchSize>2000)throw new IllegalArgumentException("Invalid batch size");
        this.batchSize=batchSize;
        if (input == null || input.isEmpty()) throw new IllegalArgumentException("Empty cleanup plan");
        Set<String> keys = new HashSet<>(), uris = new LinkedHashSet<>();
        java.util.Map<String, String> identities = new java.util.HashMap<>();
        for (Proof p : input) {
            if (p == null || p.record == null || p.record.copiedBytes <= 0 || p.hash == null
                    || !p.hash.matches("[0-9a-f]{64}") || blank(p.sourceKey) || blank(p.targetKey)
                    || blank(p.sourceIdentity) || blank(p.targetIdentity))
                throw new IllegalArgumentException("Incomplete cleanup receipt");
            String previous = identities.put(p.sourceKey, p.record.sourceUri);
            if (previous != null && !previous.equals(p.record.sourceUri))
                throw new IllegalArgumentException("Aliased source selection");
            keys.add(p.sourceKey); uris.add(p.record.sourceUri);
        }
        for (Proof p : input) if (keys.contains(p.targetKey))
            throw new IllegalArgumentException("A protected copy is also selected as an original");
        proofs = Collections.unmodifiableList(new ArrayList<>(input));
        sources = Collections.unmodifiableList(new ArrayList<>(uris));
    }
    List<Proof> batch(int offset) {
        if (offset < 0 || offset >= sources.size()) throw new IllegalArgumentException("Invalid batch cursor");
        Set<String> selected = new HashSet<>(sources.subList(offset, Math.min(sources.size(), offset + batchSize)));
        List<Proof> result = new ArrayList<>();
        for (Proof p : proofs) if (selected.contains(p.record.sourceUri)) result.add(p);
        return result;
    }
    int batchCount(int offset) { return Math.min(batchSize, sources.size() - offset); }
    private static boolean blank(String value) { return value == null || value.isEmpty(); }
}
