package com.example.gallerysorter;

/** Bounds retries for persisting metadata after a background sort has completed. */
final class OrganizationResultRetryPolicy {
    private OrganizationResultRetryPolicy() {
    }

    static boolean shouldRetry(int attemptsMade, int maxAttempts) {
        return maxAttempts > 0 && attemptsMade < maxAttempts;
    }
}
