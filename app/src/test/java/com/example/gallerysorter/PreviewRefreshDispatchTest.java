package com.example.gallerysorter;

import org.junit.Test;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import static org.junit.Assert.*;

public class PreviewRefreshDispatchTest {
    @Test public void leavingTheSourceScreenDiscardsPendingUiAndDoesNotStartScan() {
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor ui = new QueueExecutor();
        PreviewRefreshDispatch dispatch = new PreviewRefreshDispatch();
        dispatch.execute(worker, ui, () -> false, () -> fail("Stale progress"),
                () -> fail("Stale scan"), () -> fail("Stale notice"), () -> false);
        worker.next();
        ui.next();
        assertTrue(worker.tasks.isEmpty());
        dispatch.execute(worker, ui, () -> true, () -> { }, () -> { }, () -> { });
        assertEquals(1, worker.tasks.size());
    }
    private static final class QueueExecutor implements Executor {
        final Queue<Runnable> tasks = new ArrayDeque<>();
        @Override public void execute(Runnable command) { tasks.add(command); }
        void next() { tasks.remove().run(); }
    }

    @Test public void unchangedPreflightNeverClearsContentOrStartsProgress() {
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor ui = new QueueExecutor();
        List<String> events = new ArrayList<>();
        new PreviewRefreshDispatch().execute(worker, ui, () -> true,
                () -> fail("Must preserve current views"), () -> fail("Must skip scan"),
                () -> events.add("notice"));
        assertTrue(events.isEmpty());
        worker.next();
        assertTrue(events.isEmpty());
        ui.next();
        assertEquals(Arrays.asList("notice"), events);
        assertTrue(worker.tasks.isEmpty());
    }

    @Test public void changedMediaPreparesOnUiBeforeWorkerScan() {
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor ui = new QueueExecutor();
        List<String> events = new ArrayList<>();
        new PreviewRefreshDispatch().execute(worker, ui, () -> false,
                () -> events.add("progress"), () -> events.add("scan"),
                () -> fail("Must scan changed media"));
        worker.next();
        assertTrue(events.isEmpty());
        ui.next();
        assertEquals(Arrays.asList("progress"), events);
        worker.next();
        assertEquals(Arrays.asList("progress", "scan"), events);
    }

    @Test public void rapidTapsCoalesceUntilUiCompletionThenAllowRetry() {
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor ui = new QueueExecutor();
        PreviewRefreshDispatch dispatch = new PreviewRefreshDispatch();
        Runnable tap = () -> dispatch.execute(worker, ui, () -> true, () -> { }, () -> { }, () -> { });
        tap.run();
        tap.run();
        assertEquals(1, worker.tasks.size());
        worker.next();
        tap.run();
        assertTrue(worker.tasks.isEmpty());
        ui.next();
        tap.run();
        assertEquals(1, worker.tasks.size());
    }
}
