package com.example.gallerysorter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Run-local staged analysis for both media kinds. Never commits an incomplete run. */
final class IncrementalAnalysisSession {
    interface Reader { LocationResult read() throws Exception; }
    interface Writer { boolean save(java.util.Collection<MediaAnalysisEntry> entries); }
    private final ImageAnalysisCacheSession cache;
    private final Set<String> fingerprinted = new HashSet<>();
    int hits;
    int analyzed;
    private boolean dirty;

    IncrementalAnalysisSession(Map<String, MediaAnalysisEntry> entries) {
        cache = new ImageAnalysisCacheSession(entries);
        for (MediaAnalysisEntry entry : cache.entriesForSave()) {
            int marker = entry.signature.indexOf("|row-v2=");
            if (marker >= 0) fingerprinted.add(entry.signature.substring(0, marker));
        }
    }

    LocationResult resolve(String base, String fingerprint, boolean hasGps, Reader reader) throws Exception {
        String key = base + "|row-v2=" + fingerprint;
        LocationResult result = cache.cachedResult(key, hasGps);
        // Migrate the existing image cache once, under its existing row-signature policy.
        // Once fingerprinted, size/generation/GPS changes may never fall back to that legacy entry.
        if (result == null && base.startsWith("i|") && !fingerprinted.contains(base)) {
            result = cache.cachedResult(base, hasGps);
        }
        if (result == null) {
            result = reader.read();
            analyzed++;
        } else {
            hits++;
        }
        if (cache.cachedResult(key, hasGps) == null) {
            cache.remember(key, result);
            cache.remove(base);
            fingerprinted.add(base);
            dirty = true;
        }
        return result;
    }

    boolean finish(boolean completed, Writer writer) {
        return completed && (!dirty || writer.save(cache.entriesForSave()));
    }
}
