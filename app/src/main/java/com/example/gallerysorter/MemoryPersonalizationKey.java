package com.example.gallerysorter;

import android.net.Uri;

final class MemoryPersonalizationKey {
    private MemoryPersonalizationKey() {
    }

    static String forSummary(StoredAlbumSummary summary) {
        if (summary == null) {
            return "album:";
        }
        if (hasText(summary.relativePath)) {
            return "path:" + summary.relativePath;
        }
        return "album:" + clean(summary.albumName);
    }

    static String legacyPreferenceKey(String prefix, StoredAlbumSummary summary) {
        return prefix + Uri.encode(legacyIdentity(summary));
    }

    private static String legacyIdentity(StoredAlbumSummary summary) {
        if (summary == null) {
            return "";
        }
        if (hasText(summary.relativePath)) {
            return clean(summary.relativePath);
        }
        return clean(summary.albumName);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
