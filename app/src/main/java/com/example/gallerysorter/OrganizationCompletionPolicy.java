package com.example.gallerysorter;

/** Keeps Gallery-operation success distinct from Memory-link persistence success. */
final class OrganizationCompletionPolicy {
    private OrganizationCompletionPolicy() {
    }

    static boolean isComplete(int copiedCount, int failedCount, boolean canceled,
                              boolean memoryLinkRequired, boolean memoryLinkSaved) {
        return copiedCount > 0 && failedCount == 0 && !canceled
                && (!memoryLinkRequired || memoryLinkSaved);
    }

    static boolean shouldWarnAboutMemoryLink(int copiedCount,
                                             boolean memoryLinkRequired,
                                             boolean memoryLinkSaved) {
        return copiedCount > 0 && memoryLinkRequired && !memoryLinkSaved;
    }
}
