package com.example.gallerysorter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupReviewCoordinatorTest {
    private static final String ID = "mem_test", DATE = "20261024";

    private void seed(FakeAccess access, FakeLedger ledger, int source, int target) {
        CleanupEvidenceRecord pair = record("job", source, target, 3);
        ledger.records.add(pair);
        access.put(pair.sourceUri, new byte[]{1, 2, 3}, "DCIM/Camera/");
        access.put(pair.targetUri, new byte[]{1, 2, 3}, "Pictures/Test/");
    }

    private MemoryDependencyGuard.Snapshot externalSnapshot(List<CleanupEvidenceRecord> selected,
                                                            String blocked, boolean unknown) {
        boolean present = false;
        for (CleanupEvidenceRecord record : selected) if (record.sourceUri.equals(blocked)) present = true;
        Set<String> refs = present ? Collections.singleton(blocked) : Collections.emptySet();
        return new MemoryDependencyGuard.Snapshot(!unknown || !present, Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), unknown ? Collections.emptySet() : refs,
                unknown ? refs : Collections.emptySet(), unknown ? Collections.emptyMap()
                : Collections.singletonMap(blocked, Collections.singleton("앨범 정리 이력")));
    }

    @Test public void samePhysicalPairAcrossJobsIsOnlyHashedOnce() throws Exception {
        FakeAccess access = new FakeAccess(); FakeLedger ledger = new FakeLedger();
        seed(access, ledger, 1, 11);
        ledger.records.add(record("historical", 1, 11, 3));
        CleanupReviewAssessment result = new CleanupReviewCoordinator(ledger, new CleanupPairVerifier(access),
                (selected, exclude) -> externalSnapshot(selected, "none", false)).evaluate(null, null, () -> false);
        assertEquals(1, result.totalCount); assertEquals(1, result.eligibleCount);
        assertEquals(2, access.openCalls); assertEquals(1, ledger.savedChecks.size());
        assertEquals(2, ledger.records.size()); // Original evidence is not deleted to fix display counts.
    }

    @Test public void multipleProtectedCopiesCountOriginalAndBytesOnlyOnce() throws Exception {
        FakeAccess access = new FakeAccess(); FakeLedger ledger = new FakeLedger();
        seed(access, ledger, 1, 11); seed(access, ledger, 1, 12);
        CleanupReviewAssessment result = new CleanupReviewCoordinator(ledger, new CleanupPairVerifier(access),
                (selected, exclude) -> externalSnapshot(selected, "none", false)).evaluate(null, null, () -> false);
        assertEquals(1, result.totalCount); assertEquals(1, result.matchedCount);
        assertEquals(1, result.eligibleCount); assertEquals(3L, result.eligibleBytes);
        assertEquals(2, result.evidenceCandidates.size()); assertEquals(4, access.openCalls);
    }

    @Test public void oneReferencedOriginalDoesNotLabelAllHundredAsCoverDependencies() throws Exception {
        FakeAccess access = new FakeAccess(); FakeLedger ledger = new FakeLedger();
        for (int i = 1; i <= 100; i++) seed(access, ledger, i, i + 1000);
        String cover = ledger.records.get(0).sourceUri;
        List<Integer> projectionSizes = new ArrayList<>();
        CleanupReviewAssessment result = new CleanupReviewCoordinator(ledger, new CleanupPairVerifier(access),
                (selected, exclude) -> { projectionSizes.add(selected.size()); return externalSnapshot(selected, cover, false); })
                .evaluate(null, null, () -> false);
        assertEquals(100, result.matchedCount); assertEquals(99, result.eligibleCount);
        assertEquals(1, result.blockedSources.size());
        assertEquals(Arrays.asList(100, 100, 99, 99), projectionSizes); // Entire remainder is re-projected.
        assertEquals(MemoryDependencyGuard.Status.COLLECTION_DEPENDENCY, result.blockedSources.get(cover));
        assertTrue(result.explanationFor(result.attentionCandidates().get(0)).contains("앨범 정리 이력"));
        String diagnostic = CleanupReviewDiagnostics.text(result, 100);
        assertTrue(diagnostic.contains("모음·대표사진 연결 확인 필요 1장"));
        assertFalse(diagnostic.contains("연결 확인 필요 100장"));
    }

    @Test public void knownUnverifiedSourceIsHeldBeforeRecheckingTheRemainder() throws Exception {
        FakeAccess access = new FakeAccess(); FakeLedger ledger = new FakeLedger();
        for (int i = 1; i <= 10; i++) seed(access, ledger, i, i + 100);
        String unknown = ledger.records.get(0).sourceUri;
        CleanupReviewAssessment result = new CleanupReviewCoordinator(ledger, new CleanupPairVerifier(access),
                (selected, exclude) -> externalSnapshot(selected, unknown, true)).evaluate(null, null, () -> false);
        assertEquals(9, result.eligibleCount); assertEquals(1, result.blockedSources.size());
        assertEquals(MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY, result.blockedSources.get(unknown));
    }

    @Test public void unassignedIncompleteSnapshotDoesNotInventPerPhotoReasonsOrPass() throws Exception {
        FakeAccess access = new FakeAccess(); FakeLedger ledger = new FakeLedger(); seed(access, ledger, 1, 11);
        CleanupReviewAssessment result = new CleanupReviewCoordinator(ledger, new CleanupPairVerifier(access),
                (selected, exclude) -> new MemoryDependencyGuard.Snapshot(false, Collections.emptyMap(), Collections.emptyMap(),
                        Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet())).evaluate(null, null, () -> false);
        assertEquals(0, result.eligibleCount); assertEquals(0, result.blockedSources.size());
        assertTrue(CleanupReviewDiagnostics.text(result, 1).contains("전체 연결 검토가 끝나지 않은 사진 1장"));
    }

    @Test public void changedRevisionInvalidatesAllMatchingResults() throws Exception {
        FakeAccess access = new FakeAccess(); FakeLedger ledger = new FakeLedger(); seed(access, ledger, 1, 11);
        CleanupReviewCoordinator.SnapshotProvider snapshots = new CleanupReviewCoordinator.SnapshotProvider() {
            public MemoryDependencyGuard.Snapshot project(List<CleanupEvidenceRecord> selected, boolean exclude) {
                return externalSnapshot(selected, "none", false);
            }
            public boolean isCurrent() { return false; }
        };
        CleanupReviewAssessment result = new CleanupReviewCoordinator(ledger, new CleanupPairVerifier(access), snapshots)
                .evaluate(null, null, () -> false);
        assertEquals(0, result.eligibleCount); assertEquals(1, result.matchedCount);
        assertEquals(MemoryDependencyGuard.Status.SNAPSHOT_CHANGED, result.selectionStatus);
    }

    @Test public void removingLocalBlockerStillCannotPassAnInvalidRemainingProjection() throws Exception {
        FakeAccess access = new FakeAccess(); FakeLedger ledger = new FakeLedger();
        seed(access, ledger, 1, 11); seed(access, ledger, 2, 12);
        String cover = ledger.records.get(0).sourceUri;
        AtomicInteger calls = new AtomicInteger();
        CleanupReviewAssessment result = new CleanupReviewCoordinator(ledger, new CleanupPairVerifier(access),
                (selected, exclude) -> {
                    calls.incrementAndGet();
                    if (selected.size() == 2) return externalSnapshot(selected, cover, false);
                    return new MemoryDependencyGuard.Snapshot(false, Collections.emptyMap(), Collections.emptyMap(),
                            Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());
                }).evaluate(null, null, () -> false);
        assertEquals(4, calls.get()); assertEquals(0, result.eligibleCount); assertEquals(1, result.blockedSources.size());
    }

    @Test public void noteChangesAreGlobalAndNotMisreportedAsOneReferencePerPhoto() throws Exception {
        FakeAccess access = new FakeAccess(); FakeLedger ledger = new FakeLedger(); seed(access, ledger, 1, 11);
        CleanupReviewAssessment result = new CleanupReviewCoordinator(ledger, new CleanupPairVerifier(access),
                (selected, exclude) -> new MemoryDependencyGuard.Snapshot(true, Collections.emptyMap(), Collections.emptyMap(),
                        Collections.emptyMap(), Collections.singletonMap("note", exclude ? "changed" : "old"), Collections.emptySet()))
                .evaluate(null, null, () -> false);
        assertEquals(0, result.eligibleCount); assertTrue(result.blockedSources.isEmpty());
        assertEquals(MemoryDependencyGuard.Status.COLLECTION_DEPENDENCY, result.selectionStatus);
    }

    private CleanupEvidenceRecord record(String job, int sourceId, int targetId, long bytes) {
        return new CleanupEvidenceRecord(job,
                "content://media/external/images/media/" + sourceId,
                "content://media/external_primary/images/media/" + targetId,
                "Pictures/Test/", 1000L, bytes, 2000L);
    }

    private static class FakeAccess implements CleanupPairVerifier.Access {
        final Map<String, byte[]> data = new HashMap<>();
        final Map<String, String> paths = new HashMap<>();
        int openCalls = 0;

        void put(String uri, byte[] bytes, String path) {
            data.put(uri, bytes);
            paths.put(uri, path);
        }

        @Override public CleanupPairVerifier.Metadata metadata(String uri) {
            byte[] bytes = data.get(uri);
            if (bytes == null) return null;
            return new CleanupPairVerifier.Metadata(bytes.length, uri + "|gen1", paths.get(uri), true);
        }

        @Override public InputStream open(String uri) {
            byte[] bytes = data.get(uri);
            if (bytes == null) return null;
            openCalls++;
            return new ByteArrayInputStream(bytes);
        }
    }

    private static class FakeLedger implements CleanupReviewCoordinator.EvidenceLedger {
        final List<CleanupEvidenceRecord> records = new ArrayList<>();
        final Map<String, CleanupPairVerifier.Result> checks = new HashMap<>();
        final List<CleanupEvidenceRecord> savedChecks = new ArrayList<>();

        @Override public List<CleanupEvidenceRecord> readCandidates(String jobId) {
            if (jobId == null || jobId.isEmpty()) return records;
            List<CleanupEvidenceRecord> filtered = new ArrayList<>();
            for (CleanupEvidenceRecord r : records) {
                if (jobId.equals(r.jobId)) filtered.add(r);
            }
            return filtered;
        }

        @Override public CleanupPairVerifier.Result readCheck(String recordId) {
            return checks.get(recordId);
        }

        @Override public void saveCheck(CleanupEvidenceRecord record, CleanupPairVerifier.Result result) {
            checks.put(record.recordId, result);
            savedChecks.add(record);
        }
    }

    private MemoryDependencyGuard.Snapshot makeSnapshot(Set<String> visible, Set<String> sourceUris) {
        Map<String, Set<String>> dates = Collections.singletonMap(DATE, visible);
        Map<String, Set<String>> originals = new HashMap<>();
        Map<String, Set<String>> sourceMemories = new HashMap<>();
        for (String source : sourceUris) {
            originals.put(source, Collections.singleton(DATE));
            sourceMemories.put(source, Collections.singleton(ID));
        }
        return new MemoryDependencyGuard.Snapshot(true,
                Collections.singletonMap(ID, new MemoryDependencyGuard.Memory(dates, originals)),
                sourceMemories,
                Collections.singletonMap("group_trip", Collections.singleton(ID)),
                Collections.singletonMap("memory-id:" + ID + "#" + DATE, "memo"),
                Collections.emptySet());
    }

    @Test
    public void fullyMatchingAndPreservedCandidatesAreAllEligible() throws Exception {
        FakeAccess access = new FakeAccess();
        byte[] content1 = new byte[]{1, 2, 3, 4, 5};
        byte[] content2 = new byte[]{6, 7, 8};
        access.put("content://media/external/images/media/1", content1, "DCIM/Camera/");
        access.put("content://media/external_primary/images/media/11", content1, "Pictures/Test/");
        access.put("content://media/external/images/media/2", content2, "DCIM/Camera/");
        access.put("content://media/external_primary/images/media/12", content2, "Pictures/Test/");

        FakeLedger ledger = new FakeLedger();
        CleanupEvidenceRecord r1 = record("job1", 1, 11, 5);
        CleanupEvidenceRecord r2 = record("job1", 2, 12, 3);
        ledger.records.add(r1);
        ledger.records.add(r2);

        CleanupPairVerifier verifier = new CleanupPairVerifier(access);
        CleanupReviewCoordinator.SnapshotProvider snapshotProvider = (selected, excludeSources) -> {
            Set<String> visible = new HashSet<>(Arrays.asList(
                    "content://media/external_primary/images/media/11",
                    "content://media/external_primary/images/media/12"));
            Set<String> sources = new HashSet<>(Arrays.asList(
                    "content://media/external/images/media/1",
                    "content://media/external/images/media/2"));
            return makeSnapshot(visible, sources);
        };

        CleanupReviewCoordinator coordinator = new CleanupReviewCoordinator(ledger, verifier, snapshotProvider);
        AtomicInteger progressCount = new AtomicInteger();
        CleanupReviewAssessment assessment = coordinator.evaluate("job1",
                (checked, total, current) -> progressCount.incrementAndGet(),
                () -> false);

        assertEquals(2, assessment.totalCount);
        assertEquals(2, assessment.eligibleCount);
        assertEquals(0, assessment.ineligibleCount);
        assertEquals(8L, assessment.eligibleBytes);
        assertTrue(assessment.isActionable());
        assertEquals(2, progressCount.get());
        assertEquals(2, ledger.savedChecks.size());
        assertEquals("정리 가능한 원본 2장 (8 B 절약 가능)", assessment.summaryText());

        for (CleanupCandidate c : assessment.candidates) {
            assertTrue(c.isEligible());
            assertEquals(CleanupPairVerifier.Status.MATCH, c.contentStatus);
            assertEquals(MemoryDependencyGuard.Status.PRESERVED, c.dependencyStatus);
            assertTrue(c.diagnosticMessage().contains("정리 가능"));
        }
    }

    @Test
    public void contentMismatchExcludesCandidateAndMarksIneligible() throws Exception {
        FakeAccess access = new FakeAccess();
        byte[] matchContent = new byte[]{1, 2, 3};
        byte[] diffContentA = new byte[]{4, 5, 6};
        byte[] diffContentB = new byte[]{4, 5, 9};
        access.put("content://media/external/images/media/1", matchContent, "DCIM/Camera/");
        access.put("content://media/external_primary/images/media/11", matchContent, "Pictures/Test/");
        access.put("content://media/external/images/media/2", diffContentA, "DCIM/Camera/");
        access.put("content://media/external_primary/images/media/12", diffContentB, "Pictures/Test/");

        FakeLedger ledger = new FakeLedger();
        ledger.records.add(record("job1", 1, 11, 3));
        ledger.records.add(record("job1", 2, 12, 3));

        CleanupPairVerifier verifier = new CleanupPairVerifier(access);
        CleanupReviewCoordinator.SnapshotProvider snapshotProvider = (selected, excludeSources) ->
                makeSnapshot(Collections.singleton("content://media/external_primary/images/media/11"),
                        Collections.singleton("content://media/external/images/media/1"));

        CleanupReviewCoordinator coordinator = new CleanupReviewCoordinator(ledger, verifier, snapshotProvider);
        CleanupReviewAssessment assessment = coordinator.evaluate("job1", null, () -> false);

        assertEquals(2, assessment.totalCount);
        assertEquals(1, assessment.eligibleCount);
        assertEquals(1, assessment.ineligibleCount);
        assertEquals(3L, assessment.eligibleBytes);
        assertTrue(assessment.summaryText().contains("정리 가능 1장"));
        assertTrue(assessment.summaryText().contains("제외 1장"));

        CleanupCandidate second = assessment.candidates.get(1);
        assertFalse(second.isEligible());
        assertEquals(CleanupPairVerifier.Status.CONTENT_MISMATCH, second.contentStatus);
        assertTrue(second.diagnosticMessage().contains("내용 불일치"));
    }

    @Test
    public void dependencyViolationMarksMatchingCandidatesAsIneligible() throws Exception {
        FakeAccess access = new FakeAccess();
        byte[] content = new byte[]{1, 2, 3};
        access.put("content://media/external/images/media/1", content, "DCIM/Camera/");
        access.put("content://media/external_primary/images/media/11", content, "Pictures/Test/");

        FakeLedger ledger = new FakeLedger();
        ledger.records.add(record("job1", 1, 11, 3));

        CleanupPairVerifier verifier = new CleanupPairVerifier(access);
        // After snapshot is missing target in date section
        CleanupReviewCoordinator.SnapshotProvider snapshotProvider = (selected, excludeSources) -> {
            Set<String> visible = excludeSources ? Collections.emptySet()
                    : Collections.singleton("content://media/external_primary/images/media/11");
            return makeSnapshot(visible, Collections.singleton("content://media/external/images/media/1"));
        };

        CleanupReviewCoordinator coordinator = new CleanupReviewCoordinator(ledger, verifier, snapshotProvider);
        CleanupReviewAssessment assessment = coordinator.evaluate("job1", null, () -> false);

        assertEquals(1, assessment.totalCount);
        assertEquals(0, assessment.eligibleCount);
        assertEquals(1, assessment.ineligibleCount);
        assertFalse(assessment.isActionable());

        CleanupCandidate candidate = assessment.candidates.get(0);
        assertFalse(candidate.isEligible());
        assertEquals(CleanupPairVerifier.Status.MATCH, candidate.contentStatus);
        assertEquals(MemoryDependencyGuard.Status.DATE_SECTION_CHANGED, candidate.dependencyStatus);
        assertTrue(candidate.diagnosticMessage().contains("날짜 불일치"));
    }

    @Test
    public void cachedMatchCannotBypassCurrentUnreadableFiles() throws Exception {
        FakeAccess access = new FakeAccess();
        FakeLedger ledger = new FakeLedger();
        CleanupEvidenceRecord r = record("job1", 1, 11, 100);
        ledger.records.add(r);

        String cachedSha = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        ledger.checks.put(r.recordId, new CleanupPairVerifier.Result(CleanupPairVerifier.Status.MATCH, cachedSha));

        CleanupPairVerifier verifier = new CleanupPairVerifier(access);
        CleanupReviewCoordinator.SnapshotProvider snapshotProvider = (selected, excludeSources) ->
                makeSnapshot(Collections.singleton("content://media/external_primary/images/media/11"),
                        Collections.singleton("content://media/external/images/media/1"));

        CleanupReviewCoordinator coordinator = new CleanupReviewCoordinator(ledger, verifier, snapshotProvider);
        CleanupReviewAssessment assessment = coordinator.evaluate("job1", null, () -> false);

        assertEquals(0, assessment.eligibleCount);
        assertEquals(CleanupPairVerifier.Status.UNREADABLE, assessment.candidates.get(0).contentStatus);
        assertEquals("", assessment.candidates.get(0).contentSha256);
    }

    @Test
    public void cancellationStopsReviewImmediately() throws Exception {
        FakeAccess access = new FakeAccess();
        FakeLedger ledger = new FakeLedger();
        ledger.records.add(record("job1", 1, 11, 10));
        ledger.records.add(record("job1", 2, 12, 10));

        CleanupPairVerifier verifier = new CleanupPairVerifier(access);
        CleanupReviewCoordinator coordinator = new CleanupReviewCoordinator(ledger, verifier, (s, e) -> null);
        CleanupReviewAssessment assessment = coordinator.evaluate("job1", null, () -> true);

        assertEquals(0, assessment.totalCount);
        assertEquals(0, assessment.eligibleCount);
    }
}
