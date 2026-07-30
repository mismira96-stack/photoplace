package com.example.gallerysorter;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class SortResultStore {
    private static final String FILE_NAME = "sort_result_snapshot.json";
    private final Context context;

    SortResultStore(Context context) {
        this.context = context.getApplicationContext();
    }

    void write(SortJobResult result) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", 1);
        root.put("completedAtMillis", System.currentTimeMillis());
        root.put("copiedCount", result == null ? 0 : result.copiedCount);
        root.put("skippedCount", result == null ? 0 : result.skippedCount);
        root.put("failedCount", result == null ? 0 : result.failedCount);
        root.put("canceled", result != null && result.canceled);
        root.put("sortedUris", uriArray(result == null ? null : result.sortedUris));
        root.put("sortedItems", itemArray(result == null ? null : result.sortedItems));
        root.put("copiedOriginalUris", uriArray(result == null ? null : result.copiedOriginalUris));
        root.put("log", result == null || result.log == null ? "" : result.log.toString());
        try (FileOutputStream output = context.openFileOutput(FILE_NAME, 0)) {
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    Snapshot read() {
        StringBuilder builder = new StringBuilder();
        try (FileInputStream input = context.openFileInput(FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            if (builder.length() == 0) {
                return Snapshot.empty();
            }
            JSONObject root = new JSONObject(builder.toString());
            return new Snapshot(
                    root.optLong("completedAtMillis", 0L),
                    root.optInt("copiedCount", 0),
                    root.optInt("skippedCount", 0),
                    root.optInt("failedCount", 0),
                    root.optBoolean("canceled", false),
                    uriList(root.optJSONArray("sortedUris")),
                    itemList(root.optJSONArray("sortedItems")),
                    uriList(root.optJSONArray("copiedOriginalUris")),
                    root.optString("log", ""));
        } catch (Exception unused) {
            return Snapshot.empty();
        }
    }

    void clear() {
        context.deleteFile(FILE_NAME);
    }

    private JSONArray uriArray(List<Uri> uris) {
        JSONArray array = new JSONArray();
        if (uris != null) {
            for (Uri uri : uris) {
                if (uri != null) {
                    array.put(uri.toString());
                }
            }
        }
        return array;
    }

    private JSONArray itemArray(List<PhotoItem> items) throws Exception {
        JSONArray array = new JSONArray();
        if (items != null) {
            for (PhotoItem item : items) {
                if (item != null) {
                    array.put(PhotoItemJson.toJson(item));
                }
            }
        }
        return array;
    }

    private List<Uri> uriList(JSONArray array) {
        ArrayList<Uri> uris = new ArrayList<>();
        if (array == null) {
            return uris;
        }
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "");
            if (!value.trim().isEmpty()) {
                try {
                    uris.add(Uri.parse(value));
                } catch (Exception unused) {
                }
            }
        }
        return uris;
    }

    private List<PhotoItem> itemList(JSONArray array) {
        ArrayList<PhotoItem> items = new ArrayList<>();
        if (array == null) {
            return items;
        }
        for (int i = 0; i < array.length(); i++) {
            PhotoItem item = PhotoItemJson.fromJson(array.optJSONObject(i));
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    static final class Snapshot {
        final long completedAtMillis;
        final int copiedCount;
        final int skippedCount;
        final int failedCount;
        final boolean canceled;
        final List<Uri> sortedUris;
        final List<PhotoItem> sortedItems;
        final List<Uri> copiedOriginalUris;
        final String log;

        Snapshot(long completedAtMillis, int copiedCount, int skippedCount, int failedCount, boolean canceled, List<Uri> sortedUris, List<PhotoItem> sortedItems, List<Uri> copiedOriginalUris, String log) {
            this.completedAtMillis = completedAtMillis;
            this.copiedCount = copiedCount;
            this.skippedCount = skippedCount;
            this.failedCount = failedCount;
            this.canceled = canceled;
            this.sortedUris = sortedUris == null ? new ArrayList<Uri>() : sortedUris;
            this.sortedItems = sortedItems == null ? new ArrayList<PhotoItem>() : sortedItems;
            this.copiedOriginalUris = copiedOriginalUris == null ? new ArrayList<Uri>() : copiedOriginalUris;
            this.log = log == null ? "" : log;
        }

        boolean isEmpty() {
            return completedAtMillis <= 0L;
        }

        static Snapshot empty() {
            return new Snapshot(0L, 0, 0, 0, false, new ArrayList<Uri>(), new ArrayList<PhotoItem>(), new ArrayList<Uri>(), "");
        }
    }
}
