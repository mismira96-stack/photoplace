package com.example.gallerysorter;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupReviewSafetyTest {
    private CleanupCandidate candidate(int id, CleanupPairVerifier.Status content, MemoryDependencyGuard.Status dependency) {
        CleanupEvidenceRecord record = new CleanupEvidenceRecord("job", "content://media/external/images/media/" + id,
                "content://media/external/images/media/" + (id + 100), "Pictures/Test/", 1, 3, 1);
        return new CleanupCandidate(record, content, "", dependency);
    }

    @Test public void finalDispatcherRejectsBeforeAccessingAndroidOrDatabase() {
        assertTrue(CleanupExecutionPolicy.isEnabled());
        AtomicReference<String> message = new AtomicReference<>();
        new CleanupTrashDispatcher(null).dispatch(null, Collections.emptyList(), new CleanupTrashDispatcher.DispatchCallback() {
            public void onSuccess(String id, int count) { fail("Mutation must stay locked"); }
            public void onError(String text) { message.set(text); }
        });
        assertTrue(message.get().contains("검토 전용"));
    }

    @Test public void noCandidatesAndRejectedCandidatesHaveDifferentSummaries() {
        String empty = CleanupReviewDiagnostics.text(new CleanupReviewAssessment(null, Collections.emptyList()), 0);
        assertTrue(empty.contains("확인한 원본 사진 0장"));
        CleanupReviewAssessment assessment = new CleanupReviewAssessment(null, Arrays.asList(
                candidate(1, CleanupPairVerifier.Status.MATCH, MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY),
                candidate(2, CleanupPairVerifier.Status.UNREADABLE, MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY)),
                Collections.singletonMap("content://media/external/images/media/1", MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY),
                MemoryDependencyGuard.Status.UNVERIFIED_IDENTITY);
        String text = CleanupReviewDiagnostics.text(assessment, 2);
        assertTrue(text.contains("확인한 원본 사진 2장"));
        assertTrue(text.contains("같은 내용의 복사본 있음 1장"));
        assertTrue(text.contains("기록 연결까지 확인 0장"));
        assertTrue(text.contains("읽기 확인 불가 1장"));
        assertTrue(text.contains("기록 연결 확인 불가 1장"));
    }

    @Test public void missingTakenDateIsOptionalButSizeAndVolumeRemainRequired() {
        assertEquals("<unknown>", CleanupMediaAccess.identityValue("datetaken", null));
        assertEquals("123", CleanupMediaAccess.identityValue("_size", "123"));
        for (String column : Arrays.asList("_size", "_id", "volume_name", "relative_path", "generation_modified")) {
            try { CleanupMediaAccess.identityValue(column, null); fail(column); }
            catch (IllegalStateException expected) { }
        }
    }
}
