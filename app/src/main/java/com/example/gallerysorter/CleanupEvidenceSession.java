package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;

/** One sort run, bounded checkpoints. A storage failure disables only evidence for this run. */
final class CleanupEvidenceSession implements AutoCloseable {
    interface Sink extends AutoCloseable {
        void commit(List<CleanupEvidenceRecord> records) throws Exception;
        @Override default void close() throws Exception { }
    }

    static final int MAX_BUFFERED = 50;
    static final long CHECKPOINT_INTERVAL_MS = 5000;
    final String jobId;
    private final Sink sink;
    private final LongSupplier clock;
    private final List<CleanupEvidenceRecord> pending = new ArrayList<>();
    private long lastCheckpoint;
    private boolean disabled, closed;
    private int recorded, unrecorded, failures;
    private long checkpointNanos;

    CleanupEvidenceSession(Sink sink) {
        this("copy_" + UUID.randomUUID(), sink, () -> System.nanoTime() / 1000000L);
    }

    CleanupEvidenceSession(String jobId, Sink sink, LongSupplier clock) {
        this.jobId = jobId;
        this.sink = sink;
        this.clock = clock;
        this.lastCheckpoint = clock.getAsLong();
    }

    void observe(String source, String target, String path, long taken, long bytes) {
        if (closed) throw new IllegalStateException("Evidence session already closed");
        if (disabled) { unrecorded++; return; }
        try {
            pending.add(new CleanupEvidenceRecord(jobId, source, target, path,
                    taken, bytes, System.currentTimeMillis()));
        } catch (IllegalArgumentException unsupported) {
            unrecorded++;
            return;
        }
        if (pending.size() >= MAX_BUFFERED) checkpoint();
        else tick();
    }

    // Called after every processed item, including failures/skips. No timer or UI thread I/O.
    void tick() {
        if (!closed && !disabled && !pending.isEmpty()
                && clock.getAsLong() - lastCheckpoint >= CHECKPOINT_INTERVAL_MS) checkpoint();
    }

    private void checkpoint() {
        if (pending.isEmpty()) return;
        long start = System.nanoTime();
        try {
            sink.commit(Collections.unmodifiableList(new ArrayList<>(pending)));
            recorded += pending.size();
        } catch (Exception failure) {
            // Never bubble into SortJob's copy failure path or request a worker retry.
            unrecorded += pending.size();
            failures++;
            disabled = true;
        } finally {
            checkpointNanos += System.nanoTime() - start;
            pending.clear();
            lastCheckpoint = clock.getAsLong();
        }
    }

    @Override public void close() {
        if (closed) return;
        checkpoint();
        closed = true;
        try { sink.close(); } catch (Exception failure) { failures++; }
    }

    int recordedCount() { return recorded; }
    int unrecordedCount() { return unrecorded; }
    int failureCount() { return failures; }
    int bufferedCount() { return pending.size(); }
    long checkpointMillis() { return checkpointNanos / 1000000L; }
}
