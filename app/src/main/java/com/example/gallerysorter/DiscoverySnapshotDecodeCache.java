package com.example.gallerysorter;

import org.json.JSONObject;

/** Reuses immutable decoded data only for identical file contents; never skips file validation. */
final class DiscoverySnapshotDecodeCache {
    private final int maxChars;
    private String previousText;
    private DiscoverySnapshot previous;

    DiscoverySnapshotDecodeCache() { this(4 * 1024 * 1024); }
    DiscoverySnapshotDecodeCache(int maxChars) { this.maxChars = Math.max(0, maxChars); }

    DiscoverySnapshot decode(String text) {
        if (text != null && text.equals(previousText)) return previous;
        clear();
        if (text == null || text.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(text);
            if (!DiscoverySnapshotJson.isReadableRoot(root)) return null;
            DiscoverySnapshot result = DiscoverySnapshotJson.fromJson(root);
            if (text.length() <= maxChars) {
                previousText = text;
                previous = result;
            }
            return result;
        } catch (Exception unreadable) { return null; }
    }

    void clear() { previousText = null; previous = null; }
}
