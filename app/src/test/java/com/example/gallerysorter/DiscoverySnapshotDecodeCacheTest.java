package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class DiscoverySnapshotDecodeCacheTest {
    private static final String VALID = "{\"schemaVersion\":1,\"snapshotVersion\":7,\"groups\":[]}";

    @Test public void identicalContentsReuseImmutableSnapshotAndClearForcesDecode() {
        DiscoverySnapshotDecodeCache cache = new DiscoverySnapshotDecodeCache();
        DiscoverySnapshot first = cache.decode(VALID);
        assertNotNull(first);
        assertSame(first, cache.decode(new String(VALID)));
        cache.clear();
        assertNotSame(first, cache.decode(VALID));
    }

    @Test public void missingCorruptOrUnsupportedContentCannotReuseEarlierSnapshot() {
        for (String text : new String[]{null, "", "{broken", "{\"schemaVersion\":999,\"groups\":[]}"}) {
            DiscoverySnapshotDecodeCache cache = new DiscoverySnapshotDecodeCache();
            DiscoverySnapshot first = cache.decode(VALID);
            assertNull(cache.decode(text));
            assertNotSame(first, cache.decode(VALID));
        }
    }

    @Test public void oversizedContentIsDecodedButNotRetained() {
        DiscoverySnapshotDecodeCache cache = new DiscoverySnapshotDecodeCache(VALID.length() - 1);
        DiscoverySnapshot first = cache.decode(VALID);
        assertNotNull(first);
        assertNotSame(first, cache.decode(VALID));
    }
}
