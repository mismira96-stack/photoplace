package com.example.gallerysorter;

/** Enabled after isolated device cancellation, corruption/recovery and 301-item batch verification. */
final class CleanupExecutionPolicy {
    private CleanupExecutionPolicy() { }
    static boolean isEnabled() { return true; }
}
