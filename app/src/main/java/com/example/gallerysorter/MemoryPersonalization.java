package com.example.gallerysorter;

import org.json.JSONObject;

final class MemoryPersonalization {
    final String memoryKey;
    final String displayName;
    final String memo;
    final String userCoverUri;
    final long updatedAtMillis;

    MemoryPersonalization(String memoryKey, String displayName, String memo,
                          String userCoverUri, long updatedAtMillis) {
        this.memoryKey = clean(memoryKey);
        this.displayName = clean(displayName);
        this.memo = clean(memo);
        this.userCoverUri = clean(userCoverUri);
        this.updatedAtMillis = updatedAtMillis;
    }

    static MemoryPersonalization empty(String memoryKey) {
        return new MemoryPersonalization(memoryKey, "", "", "", 0L);
    }

    static MemoryPersonalization fromJson(String memoryKey, JSONObject json) {
        if (json == null) {
            return empty(memoryKey);
        }
        return new MemoryPersonalization(
                memoryKey,
                json.optString("displayName", ""),
                json.optString("memo", ""),
                json.optString("userCoverUri", ""),
                json.optLong("updatedAtMillis", 0L));
    }

    JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        if (!displayName.isEmpty()) {
            json.put("displayName", displayName);
        }
        if (!memo.isEmpty()) {
            json.put("memo", memo);
        }
        if (!userCoverUri.isEmpty()) {
            json.put("userCoverUri", userCoverUri);
        }
        json.put("updatedAtMillis", updatedAtMillis);
        return json;
    }

    MemoryPersonalization withDisplayName(String value, long nowMillis) {
        return new MemoryPersonalization(memoryKey, value, memo, userCoverUri, nowMillis);
    }

    MemoryPersonalization withMemo(String value, long nowMillis) {
        return new MemoryPersonalization(memoryKey, displayName, value, userCoverUri, nowMillis);
    }

    MemoryPersonalization withUserCoverUri(String value, long nowMillis) {
        return new MemoryPersonalization(memoryKey, displayName, memo, value, nowMillis);
    }

    boolean isEmpty() {
        return displayName.isEmpty() && memo.isEmpty() && userCoverUri.isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
