package com.example.gallerysorter;

import android.content.Context;
import android.content.SharedPreferences;

/** Small independent acknowledgement preferences; never invalidates cached media/source results. */
final class MemorySeenStore {
    private final SharedPreferences preferences;
    MemorySeenStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("memory_seen_versions", Context.MODE_PRIVATE);
    }
    long version(MemoryRecord record) {
        String key = MemorySeenPolicy.key(record);
        return key.isEmpty() ? -1 : preferences.getLong(key, -1);
    }
    void acknowledge(MemoryBrowserDetail detail) {
        if (!MemorySeenPolicy.canAcknowledge(detail)) return;
        long version = detail.record.discoveryGroup.snapshotVersion;
        if (version > version(detail.record)) preferences.edit().putLong(MemorySeenPolicy.key(detail.record), version).apply();
    }
}
