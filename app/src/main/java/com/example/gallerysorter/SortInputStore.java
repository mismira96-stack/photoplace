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
import java.util.Date;
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
                    array.put(toJson(item));
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
                    PhotoItem item = fromJson(array.optJSONObject(i));
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

    private JSONObject toJson(PhotoItem item) throws Exception {
        JSONObject json = new JSONObject();
        json.put("uri", item.uri == null ? "" : item.uri.toString());
        json.put("name", item.name);
        json.put("mimeType", item.mimeType);
        json.put("takenAtMillis", item.takenAt == null ? JSONObject.NULL : Long.valueOf(item.takenAt.getTime()));
        json.put("locationKey", item.locationKey);
        json.put("noLocation", item.noLocation);
        json.put("targetExists", item.targetExists);
        json.put("duplicateInTarget", item.duplicateInTarget);
        json.put("targetRelativePath", item.targetRelativePath);
        json.put("video", item.video);
        json.put("countryName", item.countryName);
        json.put("adminArea", item.adminArea);
        json.put("addressLine", item.addressLine);
        return json;
    }

    private PhotoItem fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        String uriValue = json.optString("uri", "");
        if (uriValue.trim().isEmpty()) {
            return null;
        }
        long takenAtMillis = json.optLong("takenAtMillis", 0L);
        Date takenAt = takenAtMillis > 0L ? new Date(takenAtMillis) : null;
        return new PhotoItem(
                Uri.parse(uriValue),
                json.optString("name", ""),
                json.optString("mimeType", ""),
                takenAt,
                json.optString("locationKey", PlaceNamePolicy.LOCATION_NONE),
                json.optBoolean("noLocation", false),
                json.optBoolean("targetExists", false),
                json.optBoolean("duplicateInTarget", false),
                json.optString("targetRelativePath", ""),
                json.optBoolean("video", false),
                json.optString("countryName", ""),
                json.optString("adminArea", ""),
                json.optString("addressLine", ""));
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
