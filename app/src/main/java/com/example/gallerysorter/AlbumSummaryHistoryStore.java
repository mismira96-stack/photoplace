package com.example.gallerysorter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

final class AlbumSummaryHistoryStore {
    private static final String FILE_NAME = "album_summary_history.json";
    private static final int MAX_SESSIONS = 20;

    private final Context context;

    AlbumSummaryHistoryStore(Context context) {
        this.context = context.getApplicationContext();
    }

    JSONObject readRoot() {
        StringBuilder builder = new StringBuilder();
        try (FileInputStream input = context.openFileInput(FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            if (builder.length() > 0) {
                return new JSONObject(builder.toString());
            }
        } catch (Exception unused) {
        }
        return new JSONObject();
    }

    void writeRebuilt(Map<String, AlbumSummary> summaries) throws Exception {
        long now = System.currentTimeMillis();
        JSONObject root = new JSONObject();
        JSONArray sessions = new JSONArray();
        sessions.put(buildSessionJson(now, countItems(summaries), 0, 0, summaries));
        root.put("schemaVersion", 1);
        root.put("rebuiltFromExistingAlbums", true);
        root.put("updatedAt", formatTimestamp(now));
        root.put("updatedAtMillis", now);
        root.put("sessions", sessions);
        writeRoot(root);
    }

    void appendSession(int sortedItemCount, int skippedItemCount, int failedItemCount,
                       Map<String, AlbumSummary> summaries) throws Exception {
        long now = System.currentTimeMillis();
        JSONObject root = readRoot();
        JSONArray previousSessions = root.optJSONArray("sessions");
        JSONArray sessions = new JSONArray();
        sessions.put(buildSessionJson(now, sortedItemCount, skippedItemCount, failedItemCount, summaries));
        if (previousSessions != null) {
            int keepCount = Math.min(previousSessions.length(), MAX_SESSIONS - 1);
            for (int i = 0; i < keepCount; i++) {
                sessions.put(previousSessions.getJSONObject(i));
            }
        }
        root.put("schemaVersion", 1);
        root.put("updatedAt", formatTimestamp(now));
        root.put("updatedAtMillis", now);
        root.put("sessions", sessions);
        writeRoot(root);
    }

    private void writeRoot(JSONObject root) throws Exception {
        try (FileOutputStream output = context.openFileOutput(FILE_NAME, 0)) {
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    private JSONObject buildSessionJson(long createdAtMillis, int sortedItemCount,
                                        int skippedItemCount, int failedItemCount,
                                        Map<String, AlbumSummary> summaries) throws JSONException {
        JSONArray albums = new JSONArray();
        for (AlbumSummary summary : summaries.values()) {
            JSONObject album = new JSONObject();
            album.put("albumName", summary.albumName);
            album.put("relativePath", summary.relativePath);
            album.put("itemCount", summary.itemCount);
            album.put("startDate", formatDate(summary.dateRange.start));
            album.put("endDate", formatDate(summary.dateRange.end));
            album.put("startDateMillis", summary.dateRange.start == null ? JSONObject.NULL : Long.valueOf(summary.dateRange.start.getTime()));
            album.put("endDateMillis", summary.dateRange.end == null ? JSONObject.NULL : Long.valueOf(summary.dateRange.end.getTime()));
            album.put("thumbnailUri", summary.thumbnailUri);
            album.put("countryCode", emptyToJsonNull(summary.countryCode));
            album.put("countryName", emptyToJsonNull(summary.countryName));
            album.put("adminArea", emptyToJsonNull(summary.adminArea));
            album.put("addressLine", emptyToJsonNull(summary.addressLine));
            album.put("createdAt", formatTimestamp(createdAtMillis));
            album.put("createdAtMillis", createdAtMillis);
            albums.put(album);
        }
        JSONObject session = new JSONObject();
        session.put("createdAt", formatTimestamp(createdAtMillis));
        session.put("createdAtMillis", createdAtMillis);
        session.put("sortedItemCount", sortedItemCount);
        session.put("skippedItemCount", skippedItemCount);
        session.put("failedItemCount", failedItemCount);
        session.put("albumCount", summaries.size());
        session.put("albums", albums);
        return session;
    }

    private int countItems(Map<String, AlbumSummary> summaries) {
        Iterator<AlbumSummary> iterator = summaries.values().iterator();
        int count = 0;
        while (iterator.hasNext()) {
            count += iterator.next().itemCount;
        }
        return count;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(date);
    }

    static String formatTimestamp(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date(millis));
    }

    private Object emptyToJsonNull(String value) {
        return value == null || value.trim().isEmpty() ? JSONObject.NULL : value;
    }
}
