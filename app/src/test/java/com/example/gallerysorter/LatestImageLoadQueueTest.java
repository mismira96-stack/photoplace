package com.example.gallerysorter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;
import static org.junit.Assert.*;

public class LatestImageLoadQueueTest {
    private static final class Tasks implements Executor {
        final Queue<Runnable> tasks = new ArrayDeque<>();
        @Override public void execute(Runnable command) { tasks.add(command); }
        void next() { tasks.remove().run(); }
    }

    @Test public void rapidSwipesSkipQueuedOldDecodesAndCancelTheirSignals() {
        Tasks worker = new Tasks(), ui = new Tasks();
        LatestImageLoadQueue<String> queue = new LatestImageLoadQueue<>(worker, ui);
        List<String> events = new ArrayList<>();
        queue.load(() -> { fail("Old queued decode ran"); return "old"; },
                () -> events.add("cancel-old"), events::add, error -> fail());
        queue.load(() -> "current", () -> { }, events::add, error -> fail());
        assertEquals(Arrays.asList("cancel-old"), events);
        worker.next();
        assertTrue(ui.tasks.isEmpty());
        worker.next();
        ui.next();
        assertEquals(Arrays.asList("cancel-old", "current"), events);
    }

    @Test public void completedOldDecodeCannotReplaceNewlySelectedImage() {
        Tasks worker = new Tasks(), ui = new Tasks();
        LatestImageLoadQueue<String> queue = new LatestImageLoadQueue<>(worker, ui);
        List<String> shown = new ArrayList<>();
        queue.load(() -> "old", () -> { }, shown::add, error -> fail());
        worker.next(); // Old result is waiting to bind on the UI thread.
        queue.load(() -> "new", () -> { }, shown::add, error -> fail());
        ui.next();
        assertTrue(shown.isEmpty());
        worker.next();
        ui.next();
        assertEquals(Arrays.asList("new"), shown);
    }

    @Test public void leavingViewerOrSwitchingToVideoDiscardsReadyPhoto() {
        Tasks worker = new Tasks(), ui = new Tasks();
        LatestImageLoadQueue<String> queue = new LatestImageLoadQueue<>(worker, ui);
        int[] cancels = {0};
        queue.load(() -> "photo", () -> cancels[0]++, result -> fail("Photo replaced video"), error -> fail());
        worker.next();
        queue.cancel();
        queue.cancel();
        ui.next();
        assertEquals(1, cancels[0]);
    }

    @Test public void failedPhotoDoesNotBlockNextLoadAndStaleErrorsAreIgnored() {
        Tasks worker = new Tasks(), ui = new Tasks();
        LatestImageLoadQueue<String> queue = new LatestImageLoadQueue<>(worker, ui);
        List<String> events = new ArrayList<>();
        queue.load(() -> { throw new Exception("unreadable"); }, () -> { }, result -> fail(),
                error -> events.add(error.getMessage()));
        worker.next();
        ui.next();
        assertEquals(Arrays.asList("unreadable"), events);
        queue.load(() -> { throw new Exception("stale-error"); }, () -> { }, result -> fail(), error -> fail());
        worker.next();
        queue.load(() -> "recovered", () -> { }, events::add, error -> fail());
        ui.next();
        worker.next();
        ui.next();
        assertEquals(Arrays.asList("unreadable", "recovered"), events);
    }
}
