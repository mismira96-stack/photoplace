package com.example.gallerysorter;

/** One browser bookmark: keep the same card at the same viewport offset after NEW reorders it. */
final class MemoryScrollAnchor {
    final String key, scope;
    final int offset, fallbackY;

    MemoryScrollAnchor(String key, String scope, int offset, int fallbackY) {
        this.key = key; this.scope = scope; this.offset = offset; this.fallbackY = fallbackY;
    }

    static String scope(boolean includeAlbums, String query) {
        return (includeAlbums ? "albums|" : "discovery|") + (query == null ? "" : query.trim());
    }

    boolean matches(boolean includeAlbums, String query) { return scope.equals(scope(includeAlbums, query)); }

    int restoreY(Integer currentCardTop, int maxScrollY) {
        long target = currentCardTop == null ? fallbackY : (long) currentCardTop - offset;
        return (int) Math.max(0L, Math.min(Math.max(0, maxScrollY), target));
    }
}
