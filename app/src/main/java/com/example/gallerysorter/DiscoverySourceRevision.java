package com.example.gallerysorter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Identifies visible rows in the selected folders, including rows just published by the camera. */
final class DiscoverySourceRevision {
    private final Context context;

    DiscoverySourceRevision(Context context) {
        this.context = context;
    }

    String read(List<String> paths) {
        StringBuilder key = new StringBuilder("discovery-source-v1|")
                .append(ImageAnalysisCacheSession.POLICY_VERSION).append('|')
                .append(DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION).append('|')
                .append(DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);
        if (Build.VERSION.SDK_INT >= 30) {
            List<String> volumes = new ArrayList<>(MediaStore.getExternalVolumeNames(context));
            Collections.sort(volumes);
            if (volumes.isEmpty()) throw new IllegalStateException("No external media volumes");
            for (String volume : volumes) {
                key.append('|').append(volume).append(':').append(MediaStore.getVersion(context, volume));
            }
        } else {
            key.append('|').append(MediaStore.getVersion(context));
        }
        for (String path : paths) key.append('|').append(path.length()).append(':').append(path);
        key.append('|').append(rows(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, paths));
        key.append('|').append(rows(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, paths));
        return key.toString();
    }

    private String rows(Uri collection, List<String> paths) {
        boolean generations = Build.VERSION.SDK_INT >= 30;
        String[] projection = generations
                ? new String[]{"_id", "generation_modified"}
                : new String[]{"_id", "date_added", "date_modified", "_size"};
        String selection = "relative_path COLLATE BINARY IN ("
                + String.join(",", Collections.nCopies(paths.size(), "?")) + ") AND is_pending=0"
                + (generations ? " AND is_trashed=0" : "");
        try (Cursor cursor = context.getContentResolver().query(collection, projection, selection,
                paths.toArray(new String[0]), "_id ASC")) {
            if (cursor == null) throw new IllegalStateException("Unknown source inventory");
            // Ordered rows distinguish a same-count replacement and late publication of an older row.
            long fingerprint = 0xcbf29ce484222325L;
            int count = 0;
            while (cursor.moveToNext()) {
                for (int column = 0; column < projection.length; column++) {
                    fingerprint = (fingerprint ^ cursor.getLong(column)) * 0x100000001b3L;
                }
                count++;
            }
            return count + ":" + Long.toHexString(fingerprint);
        }
    }
}
