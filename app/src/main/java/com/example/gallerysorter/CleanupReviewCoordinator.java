package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Off-UI coordinator that evaluates copied original photos for safe cleanup review.
 * Performs Tier 1 (content SHA-256 match) and Tier 2 (whole-selection memory projection dependency).
 * Never deletes or trashes any media.
 */
final class CleanupReviewCoordinator {
    interface EvidenceLedger {
        List<CleanupEvidenceRecord> readCandidates(String jobId) throws Exception;
        CleanupPairVerifier.Result readCheck(String recordId) throws Exception;
        void saveCheck(CleanupEvidenceRecord record, CleanupPairVerifier.Result result) throws Exception;
    }

    interface SnapshotProvider {
        MemoryDependencyGuard.Snapshot project(List<CleanupEvidenceRecord> selected, boolean excludeSources) throws Exception;
        default boolean isCurrent() throws Exception { return true; }
    }

    interface ProgressListener {
        void onProgress(int checked, int total, CleanupCandidate current);
        default void onStage(String stage) { }
    }

    private final EvidenceLedger ledger;
    private final CleanupPairVerifier verifier;
    private final SnapshotProvider snapshotProvider;

    CleanupReviewCoordinator(EvidenceLedger ledger,
                             CleanupPairVerifier verifier,
                             SnapshotProvider snapshotProvider) {
        if (ledger == null || verifier == null || snapshotProvider == null) {
            throw new IllegalArgumentException("Ledger, verifier and snapshotProvider required");
        }
        this.ledger = ledger;
        this.verifier = verifier;
        this.snapshotProvider = snapshotProvider;
    }

    CleanupReviewAssessment evaluate(String jobId,
                                     ProgressListener listener,
                                     BooleanSupplier canceled) throws Exception {
        BooleanSupplier isCanceled = canceled != null ? canceled : () -> false;
        List<CleanupEvidenceRecord> records = distinctPairs(ledger.readCandidates(jobId));
        if (records == null || records.isEmpty() || isCanceled.getAsBoolean()) {
            return new CleanupReviewAssessment(jobId, Collections.emptyList());
        }

        // Tier 1: Content verification for each pair
        List<PairCheck> checks = new ArrayList<>();
        List<CleanupEvidenceRecord> contentMatches = new ArrayList<>();
        if (listener != null) listener.onStage("CONTENT");

        for (int i = 0; i < records.size(); i++) {
            if (isCanceled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                break;
            }
            CleanupEvidenceRecord record = records.get(i);
            CleanupPairVerifier.Result result = verifier.verify(record, isCanceled);
            try {
                ledger.saveCheck(record, result);
            } catch (Exception ignored) { }

            checks.add(new PairCheck(record, result));
            if (result.status == CleanupPairVerifier.Status.MATCH) {
                contentMatches.add(record);
            }
            if (listener != null) listener.onProgress(i + 1, records.size(), new CleanupCandidate(
                    record, result.status, result.sha256, MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY));
        }

        // Tier 2: Whole-selection memory dependency verification on all matching pairs
        MemoryDependencyGuard.Status wholeSelectionStatus = MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY;
        Map<String, MemoryDependencyGuard.Status> blocked = new LinkedHashMap<>();
        Map<String, String> descriptions = new LinkedHashMap<>();
        if (!contentMatches.isEmpty() && !isCanceled.getAsBoolean()) {
            if (listener != null) listener.onStage("CONNECTION");
            try {
                List<CleanupEvidenceRecord> remaining = new ArrayList<>(contentMatches);
                // Remove only explicitly identified blockers, then re-project the ENTIRE remainder.
                // Bounded passes never turn an incomplete projection into permission.
                for (int pass = 0; pass < 8 && !remaining.isEmpty() && !isCanceled.getAsBoolean(); pass++) {
                    MemoryDependencyGuard.Snapshot before = snapshotProvider.project(remaining, false);
                    MemoryDependencyGuard.Snapshot after = snapshotProvider.project(remaining, true);
                    MemoryDependencyGuard.Report report = MemoryDependencyGuard.inspect(remaining, before, after);
                    if (report.blockedSources.isEmpty()) {
                        wholeSelectionStatus = report.status; break;
                    }
                    blocked.putAll(report.blockedSources);
                    for (String source : report.blockedSources.keySet()) {
                        String description = before.referenceDescription(source);
                        if (!description.isEmpty()) descriptions.put(source, description);
                    }
                    List<CleanupEvidenceRecord> next = new ArrayList<>();
                    for (CleanupEvidenceRecord record : remaining)
                        if (!blocked.containsKey(record.sourceUri)) next.add(record);
                    remaining = next;
                }
                if (!snapshotProvider.isCurrent()) {
                    wholeSelectionStatus = MemoryDependencyGuard.Status.SNAPSHOT_CHANGED;
                    blocked.clear();
                    descriptions.clear();
                }
            } catch (Exception failure) {
                wholeSelectionStatus = MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY;
                blocked.clear();
                descriptions.clear();
            }
        }
        if (isCanceled.getAsBoolean()) {
            wholeSelectionStatus = MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY;
            blocked.clear();
            descriptions.clear();
        }

        // Assemble candidates with individual and group statuses
        List<CleanupCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < checks.size(); i++) {
            PairCheck check = checks.get(i);
            MemoryDependencyGuard.Status depStatus;
            if (check.result.status != CleanupPairVerifier.Status.MATCH) {
                depStatus = MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY;
            } else {
                depStatus = blocked.getOrDefault(check.record.sourceUri, wholeSelectionStatus);
            }
            CleanupCandidate candidate = new CleanupCandidate(
                    check.record,
                    check.result.status,
                    check.result.sha256,
                    depStatus);
            candidates.add(candidate);
        }

        return new CleanupReviewAssessment(jobId, candidates, blocked, wholeSelectionStatus, descriptions);
    }

    private static List<CleanupEvidenceRecord> distinctPairs(List<CleanupEvidenceRecord> records) {
        Map<String, CleanupEvidenceRecord> unique = new LinkedHashMap<>();
        if (records != null) for (CleanupEvidenceRecord record : records) {
            String key = record.sourceUri + "\u0000" + record.targetUri;
            CleanupEvidenceRecord old = unique.get(key);
            if (old == null || record.observedAtMillis > old.observedAtMillis) unique.put(key, record);
        }
        return new ArrayList<>(unique.values());
    }

    private static final class PairCheck {
        final CleanupEvidenceRecord record;
        final CleanupPairVerifier.Result result;
        PairCheck(CleanupEvidenceRecord record, CleanupPairVerifier.Result result) {
            this.record = record;
            this.result = result;
        }
    }
}
