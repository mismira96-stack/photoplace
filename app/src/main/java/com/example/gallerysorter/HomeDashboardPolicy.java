package com.example.gallerysorter;

final class HomeDashboardPolicy {
    static boolean canSkip(HomeDashboardSnapshot snapshot, String revision) {
        return snapshot != null && snapshot.complete() && revision != null && !revision.isEmpty() && snapshot.revision.equals(revision);
    }
    static String stableRevision(String before, String after) {
        return before != null && before.equals(after) ? before : "";
    }
    static boolean shouldRender(String shown, HomeDashboardSnapshot snapshot) {
        return snapshot != null && !snapshot.displayKey().equals(shown);
    }
}
