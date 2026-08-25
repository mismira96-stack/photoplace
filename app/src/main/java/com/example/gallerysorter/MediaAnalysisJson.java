package com.example.gallerysorter;

import org.json.JSONObject;

final class MediaAnalysisJson {
    private MediaAnalysisJson() {
    }

    static JSONObject toJson(MediaAnalysisEntry entry) {
        JSONObject json = new JSONObject();
        if (entry == null) {
            return json;
        }
        try {
            json.put("status", entry.status);
            json.put("takenAtMillis", entry.takenAtMillis);
            json.put("folderKey", entry.folderKey);
            json.put("countryCode", entry.countryCode);
            json.put("countryName", entry.countryName);
            json.put("adminArea", entry.adminArea);
            json.put("addressLine", entry.addressLine);
            json.put("policyVersion", entry.policyVersion);
        } catch (Exception ignored) {
        }
        return json;
    }

    static MediaAnalysisEntry fromJson(String signature, JSONObject json) {
        if (json == null || signature == null || signature.trim().isEmpty()) {
            return null;
        }
        String status = json.optString("status", "");
        if (!MediaAnalysisEntry.STATUS_ANALYZED.equals(status)
                && !MediaAnalysisEntry.STATUS_NO_LOCATION.equals(status)) {
            return null;
        }
        return new MediaAnalysisEntry(
                signature,
                status,
                json.optLong("takenAtMillis", 0L),
                json.optString("folderKey", ""),
                json.optString("countryCode", ""),
                json.optString("countryName", ""),
                json.optString("adminArea", ""),
                json.optString("addressLine", ""),
                json.optInt("policyVersion", 1));
    }
}
