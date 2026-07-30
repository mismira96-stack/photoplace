package com.example.gallerysorter;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class SortInputStore {
    private static final String FILE_NAME = "sort_input_snapshot.json";
    private final Context context;

    SortInputStore(Context context) {
        this.context = context.getApplicationContext();
    }

    void write(List<PhotoItem> items, boolean shouldMoveVideos) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", 1);
        root.put("createdAtMillis", System.currentTimeMillis());
        root.put("shouldMoveVideos", shouldMoveVideos);
        JSONArray array = new JSONArray();
        if (items != null) {
            for (PhotoItem item : items) {
                if (item != null) {
                    array.put(PhotoItemJson.toJson(item));
                }
            }
        }
        root.put("items", array);
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
            JSONArray array = root.optJSONArray("items");
            ArrayList<PhotoItem> items = new ArrayList<>();
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    PhotoItem item = PhotoItemJson.fromJson(array.optJSONObject(i));
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
            return new Snapshot(root.optBoolean("shouldMoveVideos", true), root.optLong("createdAtMillis", 0L), items);
        } catch (Exception unused) {
            return Snapshot.empty();
        }
    }

    void clear() {
        context.deleteFile(FILE_NAME);
    }

    static final class Snapshot {
        final boolean shouldMoveVideos;
        final long createdAtMillis;
        final List<PhotoItem> items;

        Snapshot(boolean shouldMoveVideos, long createdAtMillis, List<PhotoItem> items) {
            this.shouldMoveVideos = shouldMoveVideos;
            this.createdAtMillis = createdAtMillis;
            this.items = items == null ? new ArrayList<PhotoItem>() : items;
        }

        static Snapshot empty() {
            return new Snapshot(true, 0L, new ArrayList<PhotoItem>());
        }
    }
}
