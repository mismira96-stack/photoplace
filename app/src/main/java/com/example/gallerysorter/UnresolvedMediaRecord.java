package com.example.gallerysorter;

import org.json.JSONArray;
import org.json.JSONObject;

/** Raw unresolved item retained from the existing scan; no guessed GPS/location diagnosis. */
final class UnresolvedMediaRecord {
    final String uri, volume, mediaVersion, kind, name, mimeType, relativePath;
    final long mediaId, size, modified, takenAt;
    private final String assetKey;

    UnresolvedMediaRecord(String uri, String volume, String mediaVersion, long mediaId,
                          String kind, String name, String mimeType, String relativePath,
                          long size, long modified, long takenAt) {
        if (uri == null || !uri.matches("content://media/[^/]+/(images|video)/media/[0-9]+")
                || volume == null || volume.trim().isEmpty() || "external".equals(volume)
                || mediaVersion == null || mediaVersion.trim().isEmpty() || mediaId < 0
                || !("PHOTO".equals(kind) || "VIDEO".equals(kind))
                || !(uri.endsWith("/" + mediaId))
                || !(uri.contains("/images/") == "PHOTO".equals(kind)))
            throw new IllegalArgumentException("Incomplete unresolved media identity");
        this.uri = uri; this.volume = volume; this.mediaVersion = mediaVersion;
        this.mediaId = mediaId; this.kind = kind; this.name = name == null ? "" : name;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.relativePath = ManualPlacePaths.normalize(relativePath);
        // Unknown/legacy metadata is not a broken physical identity. Follow existing date policy.
        this.size = Math.max(0L, size); this.modified = Math.max(0L, modified);
        this.takenAt = Math.max(0L, takenAt);
        assetKey = new JSONArray().put(volume).put(mediaVersion).put(kind).put(mediaId).toString();
    }

    String assetKey() {
        // JSON tuple avoids delimiter collisions and distinguishes physical volumes and epochs.
        return assetKey;
    }

    JSONObject toJson() throws Exception {
        return new JSONObject().put("uri", uri).put("volume", volume).put("version", mediaVersion)
                .put("id", mediaId).put("kind", kind).put("name", name).put("mimeType", mimeType)
                .put("path", relativePath).put("size", size).put("modified", modified)
                .put("takenAt", takenAt).put("status", "UNRESOLVED");
    }

    static UnresolvedMediaRecord fromJson(JSONObject json) throws Exception {
        if (!"UNRESOLVED".equals(json.getString("status")))
            throw new IllegalStateException("Unknown unresolved status");
        return new UnresolvedMediaRecord(json.getString("uri"), json.getString("volume"),
                json.getString("version"), json.getLong("id"), json.getString("kind"),
                json.getString("name"), json.getString("mimeType"), json.getString("path"),
                json.getLong("size"), json.getLong("modified"), json.getLong("takenAt"));
    }
}
