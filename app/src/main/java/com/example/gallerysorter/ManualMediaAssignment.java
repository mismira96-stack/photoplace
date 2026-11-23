package com.example.gallerysorter;

import org.json.JSONObject;

/** A durable assignment of one physical media identity, including a confirmed copy preference. */
final class ManualMediaAssignment {
    final UnresolvedMediaRecord media;
    final String placeId;
    final String copyTargetKey;
    final boolean galleryCopy;

    ManualMediaAssignment(UnresolvedMediaRecord media, String placeId, String copyTargetKey, boolean galleryCopy) {
        if (media == null || placeId == null || placeId.isEmpty()) throw new IllegalArgumentException("Invalid media assignment");
        this.media = media; this.placeId = placeId;
        this.copyTargetKey = copyTargetKey == null ? "" : copyTargetKey;
        this.galleryCopy = galleryCopy;
        if (media.assetKey().equals(this.copyTargetKey)) throw new IllegalArgumentException("Self copy preference");
    }

    JSONObject toJson() throws Exception {
        return new JSONObject().put("media", media.toJson()).put("placeId", placeId)
                .put("copyTargetKey", copyTargetKey).put("galleryCopy", galleryCopy);
    }
    static ManualMediaAssignment fromJson(JSONObject json) throws Exception {
        return new ManualMediaAssignment(UnresolvedMediaRecord.fromJson(json.getJSONObject("media")),
                json.getString("placeId"), json.optString("copyTargetKey", ""), json.optBoolean("galleryCopy", false));
    }
}
