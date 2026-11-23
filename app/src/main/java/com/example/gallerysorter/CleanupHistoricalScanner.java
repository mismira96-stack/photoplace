package com.example.gallerysorter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Discovers historical pairs between original photos and known PhotoPlace location albums.
 * Commits unverified candidate records into CleanupEvidenceStore for full SHA-256 and dependency checks.
 * Completely read-only; never deletes or modifies media files.
 */
final class CleanupHistoricalScanner {
    static final int DEFAULT_MAX_CANDIDATES = 100;

    private final Context context;
    private final CleanupEvidenceStore store;

    CleanupHistoricalScanner(Context context, CleanupEvidenceStore store) {
        this.context = context.getApplicationContext();
        this.store = store;
    }

    int scanAndRegister() {
        return scanAndRegister(DEFAULT_MAX_CANDIDATES);
    }

    int scanAndRegister(int maxCandidates) {
        try {
            List<String> knownAlbumPaths = loadKnownAlbumRelativePaths();
            if (knownAlbumPaths.isEmpty()) {
                return 0;
            }

            // 1. Index target copies in known PhotoPlace albums by (filename + size)
            Map<String, List<TargetItem>> targetsByKey = new HashMap<>();
            queryTargets(knownAlbumPaths, targetsByKey);
            if (targetsByKey.isEmpty()) {
                return 0;
            }

            // 2. Clear old historical records so we don't replay stale or already-trashed items
            store.clearHistoricalRecords();

            // 3. Query source photos from all non-album locations (Download, Camera, etc.)
            List<CleanupEvidenceRecord> discovered = querySourceMatches(targetsByKey, knownAlbumPaths, maxCandidates);
            if (!discovered.isEmpty()) {
                store.commit(discovered);
            }
            return discovered.size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    List<String> loadKnownAlbumRelativePaths() {
        Set<String> paths = new LinkedHashSet<>();

        // 1. Read from AlbumSummaryHistoryStore
        try {
            AlbumSummaryHistoryStore summaryStore = new AlbumSummaryHistoryStore(context);
            JSONObject root = summaryStore.readRoot();
            JSONArray sessions = root.optJSONArray("sessions");
            if (sessions != null) {
                for (int s = 0; s < sessions.length(); s++) {
                    JSONObject session = sessions.optJSONObject(s);
                    if (session == null) continue;
                    JSONArray albums = session.optJSONArray("albums");
                    if (albums == null) continue;
                    for (int a = 0; a < albums.length(); a++) {
                        JSONObject album = albums.optJSONObject(a);
                        if (album == null) continue;
                        String relPath = album.optString("relativePath", "");
                        if (!relPath.isEmpty()) {
                            paths.add(normalizePath(relPath));
                        }
                    }
                }
            }
        } catch (Exception ignored) { }

        // 2. Read from SortResultStore
        try {
            SortResultStore sortResultStore = new SortResultStore(context);
            SortResultStore.Snapshot snapshot = sortResultStore.read();
            if (snapshot.sortedItems != null) {
                for (PhotoItem item : snapshot.sortedItems) {
                    if (item != null && item.targetRelativePath != null && !item.targetRelativePath.isEmpty()) {
                        paths.add(normalizePath(item.targetRelativePath));
                    }
                }
            }
        } catch (Exception ignored) { }

        // 3. Read from MemoryOrganizationLinkStore
        try {
            MemoryOrganizationLinkStore linkStore = new MemoryOrganizationLinkStore(context.getFilesDir());
            List<OrganizationLink> links = linkStore.readAll();
            if (links != null) {
                for (OrganizationLink link : links) {
                    if (link != null && link.relativePath != null && !link.relativePath.isEmpty()) {
                        paths.add(normalizePath(link.relativePath));
                    }
                }
            }
        } catch (Exception ignored) { }

        // 4. Read from legacy files if they exist
        for (String filename : new String[]{"album_history.json", "device_album_history.json"}) {
            try {
                File historyFile = new File(context.getFilesDir(), filename);
                if (historyFile.exists()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(historyFile.toPath());
                    JSONObject json = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                    JSONArray sessions = json.optJSONArray("sessions");
                    if (sessions != null) {
                        for (int s = 0; s < sessions.length(); s++) {
                            JSONObject session = sessions.optJSONObject(s);
                            if (session == null) continue;
                            JSONArray albums = session.optJSONArray("albums");
                            if (albums == null) continue;
                            for (int a = 0; a < albums.length(); a++) {
                                JSONObject album = albums.optJSONObject(a);
                                if (album == null) continue;
                                String relPath = album.optString("relativePath", "");
                                if (!relPath.isEmpty()) {
                                    paths.add(normalizePath(relPath));
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) { }
        }

        // 5. Query MediaStore directly for any album under Pictures/ ending with "에서/" or "에서"
        // Note: NEVER use "DISTINCT relative_path" in projection because Android MediaProvider
        // throws IllegalArgumentException: Invalid column distinct relative_path.
        // ALWAYS use parameterized selectionArgs for reliable UTF-8 handling across ContentResolver IPC.
        String albumSelection = "relative_path LIKE ? OR relative_path LIKE ? OR relative_path LIKE ?";
        String[] albumArgs = new String[]{"Pictures/%에서/", "Pictures/%에서", "Pictures/%에서/%"};
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{"relative_path"},
                albumSelection,
                albumArgs,
                null)) {
            if (cursor != null) {
                int col = cursor.getColumnIndexOrThrow("relative_path");
                while (cursor.moveToNext()) {
                    String relPath = cursor.getString(col);
                    if (relPath != null && !relPath.isEmpty()) {
                        paths.add(normalizePath(relPath));
                    }
                }
            }
        } catch (Exception ignored) { }

        return new ArrayList<>(paths);
    }

    private void queryTargets(List<String> knownAlbumPaths, Map<String, List<TargetItem>> out) {
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();
        selection.append("mime_type LIKE 'image/%'");
        if (Build.VERSION.SDK_INT >= 30) {
            selection.append(" AND is_trashed = 0");
        }
        // Match standard location albums under Pictures/ ending in 에서
        selection.append(" AND (relative_path LIKE ? OR relative_path LIKE ? OR relative_path LIKE ?");
        selectionArgs.add("Pictures/%에서/");
        selectionArgs.add("Pictures/%에서");
        selectionArgs.add("Pictures/%에서/%");
        for (String path : knownAlbumPaths) {
            String norm = normalizePath(path);
            if (!norm.startsWith("Pictures/") || !norm.contains("에서")) {
                selection.append(" OR relative_path = ?");
                selectionArgs.add(norm);
                if (norm.endsWith("/")) {
                    selection.append(" OR relative_path = ?");
                    selectionArgs.add(norm.substring(0, norm.length() - 1));
                }
            }
        }
        selection.append(")");

        String[] projection = {"_id", "_display_name", "_size", "relative_path", "datetaken"};
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection.toString(),
                selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]),
                null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String name = cursor.getString(1);
                    long size = cursor.getLong(2);
                    String relPath = cursor.getString(3);
                    long datetaken = cursor.getLong(4);
                    if (name != null && size > 0 && relPath != null) {
                        String key = name + "|" + size;
                        String uri = "content://media/external/images/media/" + id;
                        TargetItem item = new TargetItem(uri, normalizePath(relPath), size, datetaken);
                        List<TargetItem> list = out.computeIfAbsent(key, k -> new ArrayList<>());
                        list.add(item);
                    }
                }
            }
        } catch (Exception ignored) { }
    }

    private List<CleanupEvidenceRecord> querySourceMatches(Map<String, List<TargetItem>> targets,
                                                          List<String> knownAlbumPaths,
                                                          int maxCandidates) {
        List<CleanupEvidenceRecord> records = new ArrayList<>();
        String selection = "mime_type LIKE 'image/%'";
        if (Build.VERSION.SDK_INT >= 30) {
            selection += " AND is_trashed = 0";
        }
        String[] projection = {"_id", "_display_name", "_size", "relative_path", "datetaken"};

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "datetaken DESC")) {
            if (cursor != null) {
                long now = System.currentTimeMillis();
                Set<String> seenSourceUris = new HashSet<>();
                while (cursor.moveToNext()) {
                    if (maxCandidates > 0 && records.size() >= maxCandidates) {
                        break;
                    }
                    String relPath = cursor.getString(3);
                    if (isTargetAlbumPath(relPath, knownAlbumPaths)) {
                        // Skip images that are inside the destination location albums
                        continue;
                    }
                    long id = cursor.getLong(0);
                    String name = cursor.getString(1);
                    long size = cursor.getLong(2);
                    long datetaken = cursor.getLong(4);
                    if (name != null && size > 0) {
                        String key = name + "|" + size;
                        List<TargetItem> targetList = targets.get(key);
                        if (targetList != null && !targetList.isEmpty()) {
                            String sourceUri = "content://media/external/images/media/" + id;
                            if (seenSourceUris.contains(sourceUri)) {
                                continue;
                            }
                            for (TargetItem target : targetList) {
                                if (!sourceUri.equals(target.uri)) {
                                    records.add(new CleanupEvidenceRecord(
                                            "historical",
                                            sourceUri,
                                            target.uri,
                                            target.relativePath,
                                            datetaken > 0 ? datetaken : target.datetaken,
                                            size,
                                            now));
                                    seenSourceUris.add(sourceUri);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return records;
    }

    static String normalizePath(String path) {
        if (path == null) return "";
        String p = path.trim().replace('\\', '/');
        while (p.startsWith("/")) p = p.substring(1);
        if (!p.isEmpty() && !p.endsWith("/")) {
            p = p + "/";
        }
        return p;
    }

    static boolean isTargetAlbumPath(String relPath, List<String> knownAlbumPaths) {
        if (relPath == null) return false;
        String normalized = normalizePath(relPath);
        if (knownAlbumPaths != null && knownAlbumPaths.contains(normalized)) {
            return true;
        }
        if (normalized.startsWith("Pictures/") && normalized.contains("에서/")) {
            return true;
        }
        return false;
    }

    private static final class TargetItem {
        final String uri;
        final String relativePath;
        final long size;
        final long datetaken;

        TargetItem(String uri, String relativePath, long size, long datetaken) {
            this.uri = uri;
            this.relativePath = relativePath;
            this.size = size;
            this.datetaken = datetaken;
        }
    }
}
