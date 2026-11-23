package com.example.gallerysorter;

/** Fast completion stays quiet; cancelled/older requests cannot show a delayed notice. */
final class DelayedLoadingNotice {
    interface Scheduler {
        void after(long delayMillis, Runnable action);
        void remove(Runnable action);
    }
    private final Scheduler scheduler;
    private final long delayMillis;
    private long generation;
    private Runnable pending;

    DelayedLoadingNotice(Scheduler scheduler, long delayMillis) {
        this.scheduler = scheduler;
        this.delayMillis = delayMillis;
    }

    void start(Runnable show) {
        cancel();
        long request = generation;
        pending = () -> { if (request == generation) { pending = null; show.run(); } };
        scheduler.after(delayMillis, pending);
    }

    void cancel() {
        generation++;
        if (pending != null) scheduler.remove(pending);
        pending = null;
    }
}
