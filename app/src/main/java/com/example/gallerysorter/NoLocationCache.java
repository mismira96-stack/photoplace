package com.example.gallerysorter;

import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

final class NoLocationCache {
    private static final String PREF_KEY = "no_location_cache_v1";
    private final SharedPreferences preferences;
    private JSONObject entries;

    NoLocationCache(SharedPreferences preferences) {
        this.preferences = preferences;
        this.entries = readEntries(preferences.getString(PREF_KEY, ""));
    }

    long cachedTakenAtMillis(Uri uri, String name, long modifiedSeconds, long addedSeconds, long mediaTakenMillis, boolean video) {
        String key = signature(uri, name, modifiedSeconds, addedSeconds, mediaTakenMillis, video);
        return entries.optLong(key, -1L);
    }

    void remember(Uri uri, String name, long modifiedSeconds, long addedSeconds, long mediaTakenMillis, boolean video, long takenAtMillis) {
        try {
            entries.put(signature(uri, name, modifiedSeconds, addedSeconds, mediaTakenMillis, video), takenAtMillis);
            trimIfNeeded();
            preferences.edit().putString(PREF_KEY, entries.toString()).apply();
        } catch (JSONException unused) {
        }
    }

    private void trimIfNeeded() {
        if (entries.length() <= 6000) {
            return;
        }
        Iterator<String> keys = entries.keys();
        int removeCount = entries.length() - 5000;
        while (keys.hasNext() && removeCount > 0) {
            keys.next();
            keys.remove();
            removeCount--;
        }
    }

    private static JSONObject readEntries(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(raw);
        } catch (JSONException unused) {
            return new JSONObject();
        }
    }

    private static String signature(Uri uri, String name, long modifiedSeconds, long addedSeconds, long mediaTakenMillis, boolean video) {
        return MediaAnalysisSignature.buildForNoLocation(uri, name, modifiedSeconds, addedSeconds, mediaTakenMillis, video);
    }
}
