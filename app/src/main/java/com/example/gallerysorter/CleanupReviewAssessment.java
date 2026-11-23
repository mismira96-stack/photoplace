package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

/** Aggregated review result of evaluated original photos duplicated into location albums. */
final class CleanupReviewAssessment {
    final String jobId;
    final int totalCount;
    final int eligibleCount;
    final long eligibleBytes;
    final int ineligibleCount;
    final List<CleanupCandidate> candidates;
    final List<CleanupCandidate> evidenceCandidates;
    final Map<String, MemoryDependencyGuard.Status> blockedSources;
    final MemoryDependencyGuard.Status selectionStatus;
    final int matchedCount;
    final Map<String, String> referenceDescriptions;

    CleanupReviewAssessment(String jobId, List<CleanupCandidate> candidates) {
        this(jobId, candidates, Collections.emptyMap(), MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY);
    }

    CleanupReviewAssessment(String jobId, List<CleanupCandidate> candidates,
                            Map<String, MemoryDependencyGuard.Status> blockedSources,
                            MemoryDependencyGuard.Status selectionStatus) {
        this(jobId, candidates, blockedSources, selectionStatus, Collections.emptyMap());
    }

    CleanupReviewAssessment(String jobId, List<CleanupCandidate> candidates,
                            Map<String, MemoryDependencyGuard.Status> blockedSources,
                            MemoryDependencyGuard.Status selectionStatus, Map<String, String> referenceDescriptions) {
        this.jobId = jobId == null ? "" : jobId;
        this.evidenceCandidates = candidates == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(candidates));
        this.blockedSources = Collections.unmodifiableMap(new LinkedHashMap<>(blockedSources));
        this.selectionStatus = selectionStatus;
        this.referenceDescriptions = Collections.unmodifiableMap(new LinkedHashMap<>(referenceDescriptions));
        Map<String, CleanupCandidate> originals = new LinkedHashMap<>();
        for (CleanupCandidate candidate : this.evidenceCandidates) {
            CleanupCandidate existing = originals.get(candidate.evidence.sourceUri);
            if (existing == null || rank(candidate) > rank(existing)) originals.put(candidate.evidence.sourceUri, candidate);
        }
        this.candidates = Collections.unmodifiableList(new ArrayList<>(originals.values()));
        int total = this.candidates.size();
        int eligible = 0;
        int matched = 0;
        long bytes = 0L;
        for (CleanupCandidate c : this.candidates) {
            if (c.contentStatus == CleanupPairVerifier.Status.MATCH) matched++;
            if (c.isEligible()) {
                eligible++;
                bytes += c.evidence.copiedBytes;
            }
        }
        this.totalCount = total;
        this.eligibleCount = eligible;
        this.eligibleBytes = bytes;
        this.ineligibleCount = total - eligible;
        this.matchedCount = matched;
    }

    private static int rank(CleanupCandidate candidate) {
        return candidate.isEligible() ? 2 : candidate.contentStatus == CleanupPairVerifier.Status.MATCH ? 1 : 0;
    }

    String explanationFor(CleanupCandidate candidate) {
        if (candidate.isEligible()) return "같은 내용의 복사본과 기록 연결 확인";
        String description = referenceDescriptions.get(candidate.evidence.sourceUri);
        return description == null ? candidate.diagnosticMessage() : "확인할 연결: " + description;
    }

    boolean isActionable() {
        return eligibleCount > 0;
    }

    List<CleanupCandidate> eligibleCandidates() {
        List<CleanupCandidate> list = new ArrayList<>();
        for (CleanupCandidate c : candidates) {
            if (c.isEligible()) list.add(c);
        }
        return Collections.unmodifiableList(list);
    }

    List<CleanupCandidate> attentionCandidates() {
        List<CleanupCandidate> result = new ArrayList<>();
        for (CleanupCandidate candidate : candidates) if (!candidate.isEligible()) result.add(candidate);
        return Collections.unmodifiableList(result);
    }

    List<CleanupCandidate> reviewOrder() {
        List<CleanupCandidate> result = new ArrayList<>(attentionCandidates());
        result.addAll(eligibleCandidates());
        return Collections.unmodifiableList(result);
    }

    String formattedEligibleBytes() {
        if (eligibleBytes <= 0) return "0 B";
        if (eligibleBytes < 1024L) return eligibleBytes + " B";
        if (eligibleBytes < 1024L * 1024L) {
            return String.format(Locale.KOREA, "%.1f KB", eligibleBytes / 1024.0);
        }
        if (eligibleBytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.KOREA, "%.1f MB", eligibleBytes / (1024.0 * 1024.0));
        }
        return String.format(Locale.KOREA, "%.2f GB", eligibleBytes / (1024.0 * 1024.0 * 1024.0));
    }

    String summaryText() {
        if (totalCount == 0) return "정리할 수 있는 복사본 원본 사진이 없어요.";
        if (eligibleCount == totalCount) {
            return String.format(Locale.KOREA, "정리 가능한 원본 %d장 (%s 절약 가능)",
                    eligibleCount, formattedEligibleBytes());
        }
        return String.format(Locale.KOREA, "정리 가능 %d장 (%s) · 제외 %d장",
                eligibleCount, formattedEligibleBytes(), ineligibleCount);
    }
}
