package com.example.gallerysorter;

import android.net.Uri;

import org.json.JSONObject;

import java.util.Date;

final class PhotoItemJson {
    private PhotoItemJson() {
    }

    static JSONObject toJson(PhotoItem item) throws Exception {
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
        json.put("countryCode", item.countryCode);
        json.put("countryName", item.countryName);
        json.put("adminArea", item.adminArea);
        json.put("addressLine", item.addressLine);
        return json;
    }

    static PhotoItem fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        String uriValue = clean(json.optString("uri", ""));
        if (uriValue.isEmpty()) {
            return null;
        }
        long takenAtMillis = json.optLong("takenAtMillis", 0L);
        Date takenAt = takenAtMillis > 0L ? new Date(takenAtMillis) : null;
        return new PhotoItem(
                Uri.parse(uriValue),
                clean(json.optString("name", "")),
                clean(json.optString("mimeType", "")),
                takenAt,
                clean(json.optString("locationKey", PlaceNamePolicy.LOCATION_NONE)),
                json.optBoolean("noLocation", false),
                json.optBoolean("targetExists", false),
                json.optBoolean("duplicateInTarget", false),
                clean(json.optString("targetRelativePath", "")),
                json.optBoolean("video", false),
                clean(json.optString("countryCode", "")),
                clean(json.optString("countryName", "")),
                clean(json.optString("adminArea", "")),
                clean(json.optString("addressLine", "")));
    }

    static String clean(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return "null".equalsIgnoreCase(trimmed) ? "" : trimmed;
    }
}
