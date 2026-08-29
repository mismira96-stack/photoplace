package com.example.gallerysorter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Run-local cache state for image scanning. Disk writes remain owned by MediaAnalysisStore. */
final class ImageAnalysisCacheSession {
    static final int POLICY_VERSION = 1;

    private final LinkedHashMap<String, MediaAnalysisEntry> entries = new LinkedHashMap<>();

    ImageAnalysisCacheSession(Map<String, MediaAnalysisEntry> existingEntries) {
        if (existingEntries != null) {
            for (MediaAnalysisEntry entry : existingEntries.values()) {
                if (entry != null && !entry.signature.isEmpty()) {
                    entries.put(entry.signature, entry);
                }
            }
        }
    }

    LocationResult cachedResult(String signature, boolean hasCurrentMediaStoreGps) {
        MediaAnalysisEntry entry = entries.get(clean(signature));
        if (entry == null || entry.policyVersion != POLICY_VERSION) {
            return null;
        }
        if (MediaAnalysisEntry.STATUS_NO_LOCATION.equals(entry.status) && hasCurrentMediaStoreGps) {
            return null;
        }
        return entry.toLocationResult();
    }

    void remember(String signature, LocationResult result) {
        MediaAnalysisEntry entry = MediaAnalysisEntry.fromLocationResult(clean(signature), result, POLICY_VERSION);
        if (!entry.signature.isEmpty()) {
            entries.put(entry.signature, entry);
        }
    }

    Collection<MediaAnalysisEntry> entriesForSave() {
        return entries.values();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
