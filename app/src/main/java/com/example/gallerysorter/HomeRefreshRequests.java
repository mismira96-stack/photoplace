package com.example.gallerysorter;

/** Coalesce work while retaining a refresh requested during an in-flight revision check. */
final class HomeRefreshRequests {
    private boolean running, pending;
    boolean begin() {
        if (running) { pending = true; return false; }
        running = true;
        return true;
    }
    boolean complete() {
        running = false;
        boolean retry = pending;
        pending = false;
        return retry;
    }
}
