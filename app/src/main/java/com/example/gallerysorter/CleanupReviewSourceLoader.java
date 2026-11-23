package com.example.gallerysorter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Worker-only batched, read-only source location labels. No guessed Camera/Download fallback. */
final class CleanupReviewSourceLoader {
    static Map<String, CleanupReviewModel.Source> read(Context context, List<CleanupCandidate> candidates, BooleanSupplier canceled) {
        Map<String, List<String>> volumes = new LinkedHashMap<>();
        Map<String, CleanupReviewModel.Source> result = new HashMap<>();
        for (CleanupCandidate c : candidates) {
            Uri uri = Uri.parse(c.evidence.sourceUri);
            List<String> segments = uri.getPathSegments();
            if (segments.size() == 4) volumes.computeIfAbsent(segments.get(0), key -> new ArrayList<>()).add(c.evidence.sourceUri);
        }
        for (Map.Entry<String, List<String>> volume : volumes.entrySet()) {
            List<String> uris = volume.getValue();
            for (int offset = 0; offset < uris.size() && !canceled.getAsBoolean(); offset += 300) {
                List<String> batch = uris.subList(offset, Math.min(uris.size(), offset + 300));
                Map<String, String> ids = new HashMap<>();
                for (String uri : batch) ids.put(Uri.parse(uri).getLastPathSegment(), uri);
                String[] args = ids.keySet().toArray(new String[0]);
                String selection = "_id IN (" + String.join(",", java.util.Collections.nCopies(args.length, "?")) + ")";
                try (Cursor cursor = context.getContentResolver().query(Uri.parse("content://media/" + volume.getKey() + "/images/media"),
                        new String[]{"_id", "relative_path", "_display_name", "volume_name"}, selection, args, null)) {
                    while (cursor != null && cursor.moveToNext()) {
                        String uri = ids.get(cursor.getString(0));
                        if (uri != null) result.put(uri, new CleanupReviewModel.Source(cursor.getString(1), cursor.getString(2), cursor.getString(3)));
                    }
                } catch (Exception unavailable) { /* Unknown source locations stay unselectable. */ }
            }
        }
        return result;
    }
}
