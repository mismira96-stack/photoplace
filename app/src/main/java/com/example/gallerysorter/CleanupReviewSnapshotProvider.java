package com.example.gallerysorter;

import android.content.Context;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;

/** Capture the dependency snapshot AFTER the lengthy byte comparison, then keep it immutable. */
final class CleanupReviewSnapshotProvider implements CleanupReviewCoordinator.SnapshotProvider {
    private final java.util.concurrent.Callable<CleanupReviewCoordinator.SnapshotProvider> factory;
    private CleanupReviewCoordinator.SnapshotProvider reader;
    CleanupReviewSnapshotProvider(Context context, BooleanSupplier canceled, BiConsumer<Integer, Integer> progress) {
        this(() -> {
            CleanupMemorySnapshotReader captured = new CleanupMemorySnapshotReader(context.getApplicationContext());
            captured.observe(canceled, progress); return captured;
        });
    }
    CleanupReviewSnapshotProvider(java.util.concurrent.Callable<CleanupReviewCoordinator.SnapshotProvider> factory) {
        this.factory = factory;
    }
    @Override public MemoryDependencyGuard.Snapshot project(List<CleanupEvidenceRecord> selected, boolean exclude) throws Exception {
        if (reader == null) {
            reader = factory.call();
        }
        return reader.project(selected, exclude);
    }
    @Override public boolean isCurrent() throws Exception { return reader != null && reader.isCurrent(); }
}
