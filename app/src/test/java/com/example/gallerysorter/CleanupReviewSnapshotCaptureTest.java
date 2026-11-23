package com.example.gallerysorter;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class CleanupReviewSnapshotCaptureTest {
    static class Snapshot implements CleanupReviewCoordinator.SnapshotProvider {
        int projections;
        boolean current=true;
        public MemoryDependencyGuard.Snapshot project(java.util.List<CleanupEvidenceRecord> selected,boolean exclude) {
            projections++;return null;
        }
        public boolean isCurrent() {return current;}
    }
    @Test public void longContentComparisonDoesNotCaptureDependencyStateEarly() throws Exception {
        AtomicInteger captures=new AtomicInteger(); Snapshot snapshot=new Snapshot();
        CleanupReviewSnapshotProvider provider=new CleanupReviewSnapshotProvider(()->{captures.incrementAndGet();return snapshot;});
        assertEquals(0,captures.get()); assertFalse(provider.isCurrent()); assertEquals(0,captures.get());
        provider.project(Collections.emptyList(),false);
        assertEquals(1,captures.get()); assertTrue(provider.isCurrent());
    }
    @Test public void beforeAfterAndRemainderProjectionUseTheSameCapturedState() throws Exception {
        AtomicInteger captures=new AtomicInteger();Snapshot snapshot=new Snapshot();
        CleanupReviewSnapshotProvider provider=new CleanupReviewSnapshotProvider(()->{captures.incrementAndGet();return snapshot;});
        provider.project(Collections.emptyList(),false);provider.project(Collections.emptyList(),true);
        snapshot.current=false;
        assertFalse(provider.isCurrent());assertEquals(1,captures.get());assertEquals(2,snapshot.projections);
    }
    @Test public void captureFailureIsNotTreatedAsAnEmptySafeSnapshot() throws Exception {
        CleanupReviewSnapshotProvider provider=new CleanupReviewSnapshotProvider(()->{throw new IllegalStateException("unavailable");});
        try{provider.project(Collections.emptyList(),false);fail();}catch(IllegalStateException expected){}
        assertFalse(provider.isCurrent());
    }
}
