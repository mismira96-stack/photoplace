package com.example.gallerysorter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupEvidenceSessionTest {
    static class Store implements CleanupEvidenceSession.Sink {
        final List<CleanupEvidenceRecord> durable = new ArrayList<>();
        int commits, closes;
        int failAt = -1;
        @Override public void commit(List<CleanupEvidenceRecord> records) throws Exception {
            commits++;
            if (commits == failAt) throw new IOException("Disk full");
            durable.addAll(records);
        }
        @Override public void close() { closes++; }
    }

    private void observe(CleanupEvidenceSession session, int i) {
        session.observe("content://media/external/images/media/" + i,
                "content://media/external_primary/images/media/" + (i + 10000),
                "Pictures/Test/", 100, 99);
    }

    @Test public void boundedBatchesAndFinalTailAreCheckpointedWithoutPerPhotoWrites() {
        Store store = new Store();
        CleanupEvidenceSession session = new CleanupEvidenceSession("job", store, () -> 0L);
        for (int i = 0; i < 123; i++) {
            observe(session, i);
            assertTrue(session.bufferedCount() < 50);
        }
        assertEquals(2, store.commits);
        assertEquals(100, session.recordedCount());
        assertEquals(23, session.bufferedCount());
        session.close();
        session.close();
        assertEquals(3, store.commits);
        assertEquals(123, session.recordedCount());
        assertEquals(1, store.closes);
        assertEquals(0, session.unrecordedCount());
    }

    @Test public void timeCheckpointAlsoRunsDuringSkippedOrFailedItems() {
        long[] now = {0};
        Store store = new Store();
        CleanupEvidenceSession session = new CleanupEvidenceSession("job", store, () -> now[0]);
        observe(session, 1);
        now[0] = 4999;
        session.tick();
        assertEquals(0, store.commits);
        now[0] = 5000;
        session.tick();
        assertEquals(1, store.commits);
        session.close();
        assertEquals(1, store.commits);
    }

    @Test public void diskFailurePreservesEarlierCheckpointAndNeverRetriesOrGrowsBuffer() {
        Store store = new Store();
        store.failAt = 2;
        CleanupEvidenceSession session = new CleanupEvidenceSession("job", store, () -> 0L);
        for (int i = 0; i < 10000; i++) observe(session, i);
        session.close();
        assertEquals(2, store.commits);
        assertEquals(50, store.durable.size());
        assertEquals(50, session.recordedCount());
        assertEquals(9950, session.unrecordedCount());
        assertEquals(1, session.failureCount());
        assertEquals(0, session.bufferedCount());
    }

    @Test public void canceledOrExceptionalRunCanFlushAlreadyCopiedTailInFinally() {
        Store store = new Store();
        CleanupEvidenceSession session = new CleanupEvidenceSession("job", store, () -> 0L);
        try {
            observe(session, 1);
            observe(session, 2);
            throw new IllegalStateException("Stopped run");
        } catch (IllegalStateException expected) {
            // SortJob uses this same finally boundary for cancellation/exception exit.
        } finally { session.close(); }
        assertEquals(2, store.durable.size());
    }

    @Test public void abruptProcessLossLeavesUncheckpointedTailWithoutEvidence() {
        Store store = new Store();
        CleanupEvidenceSession lost = new CleanupEvidenceSession("old", store, () -> 0L);
        for (int i = 0; i < 67; i++) observe(lost, i);
        assertEquals(50, store.durable.size());
        // Simulate restart without closing the lost instance. Its tail is never inferred.
        CleanupEvidenceSession next = new CleanupEvidenceSession("new", store, () -> 0L);
        observe(next, 80);
        next.close();
        assertEquals(51, store.durable.size());
        assertEquals("new", store.durable.get(50).jobId);
    }

    @Test public void invalidCopyObservationDoesNotDisableLaterValidObservations() {
        Store store = new Store();
        CleanupEvidenceSession session = new CleanupEvidenceSession("job", store, () -> 0L);
        session.observe("content://cloud/a", "content://cloud/b", "Pictures/Test/", 0, 1);
        observe(session, 2);
        session.close();
        assertEquals(1, session.unrecordedCount());
        assertEquals(1, session.recordedCount());
        assertEquals(0, session.failureCount());
    }

    @Test public void failedFinalFlushAndFailedCloseAreContained() {
        Store store = new Store() {
            @Override public void close() { throw new IllegalStateException("Close failed"); }
        };
        store.failAt = 1;
        CleanupEvidenceSession session = new CleanupEvidenceSession("job", store, () -> 0L);
        observe(session, 1);
        session.close();
        assertEquals(0, session.recordedCount());
        assertEquals(1, session.unrecordedCount());
        assertEquals(2, session.failureCount());
    }
}
