package com.example.gallerysorter;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** UI-thread requests; stale queued work and completions never replace the current image. */
final class LatestImageLoadQueue<T> {
    private final Executor worker;
    private final Executor ui;
    private final AtomicLong generation = new AtomicLong();
    private Runnable cancelRunning;

    LatestImageLoadQueue(Executor worker, Executor ui) {
        this.worker = worker;
        this.ui = ui;
    }

    void load(Callable<T> decode, Runnable cancel, Consumer<T> ready, Consumer<Exception> failed) {
        cancel();
        long request = generation.get();
        cancelRunning = cancel;
        worker.execute(() -> {
            if (generation.get() != request) return;
            try {
                T result = decode.call();
                ui.execute(() -> { if (generation.get() == request) ready.accept(result); });
            } catch (Exception error) {
                ui.execute(() -> { if (generation.get() == request) failed.accept(error); });
            }
        });
    }

    void cancel() {
        generation.incrementAndGet();
        Runnable previous = cancelRunning;
        cancelRunning = null;
        if (previous != null) previous.run();
    }
}
