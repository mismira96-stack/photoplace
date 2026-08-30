package com.example.gallerysorter;

import org.json.JSONObject;

final class MemoryDateNote {
    final String stableMemoryId;
    final String dateKey;
    final String text;
    final long createdAtMillis;
    final long updatedAtMillis;

    MemoryDateNote(String stableMemoryId, String dateKey, String text,
                   long createdAtMillis, long updatedAtMillis) {
        this.stableMemoryId = clean(stableMemoryId);
        this.dateKey = cleanDateKey(dateKey);
        this.text = clean(text);
        this.createdAtMillis = Math.max(0L, createdAtMillis);
        this.updatedAtMillis = Math.max(0L, updatedAtMillis);
    }

    static String key(String stableMemoryId, String dateKey) {
        String id = clean(stableMemoryId);
        String date = cleanDateKey(dateKey);
        return id.isEmpty() || date.isEmpty() ? "" : "memory-id:" + id + "#" + date;
    }

    static MemoryDateNote fromJson(String stableMemoryId, String dateKey, JSONObject json) {
        if (json == null) {
            return null;
        }
        MemoryDateNote note = new MemoryDateNote(
                stableMemoryId,
                dateKey,
                json.optString("text", ""),
                json.optLong("createdAtMillis", 0L),
                json.optLong("updatedAtMillis", 0L));
        return note.text.isEmpty() ? null : note;
    }

    JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("text", text);
        json.put("createdAtMillis", createdAtMillis);
        json.put("updatedAtMillis", updatedAtMillis);
        return json;
    }

    static String cleanDateKey(String value) {
        String key = clean(value);
        return key.matches("\\d{8}") ? key : "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
