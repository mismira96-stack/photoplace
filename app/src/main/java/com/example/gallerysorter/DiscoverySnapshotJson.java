package com.example.gallerysorter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class DiscoverySnapshotJson {
    private DiscoverySnapshotJson() {
    }

    static JSONObject toJson(DiscoverySnapshot snapshot) throws JSONException {
        JSONObject root = new JSONObject();
        if (snapshot == null) {
            root.put("schemaVersion", DiscoverySnapshot.CURRENT_SCHEMA_VERSION);
            root.put("snapshotVersion", 0L);
            root.put("createdAtMillis", 0L);
            root.put("sourceSignature", "");
            root.put("sourceItemCount", 0);
            root.put("groupCount", 0);
            root.put("groups", new JSONArray());
            return root;
        }
        root.put("schemaVersion", snapshot.schemaVersion);
        root.put("snapshotVersion", snapshot.snapshotVersion);
        root.put("createdAtMillis", snapshot.createdAtMillis);
        root.put("sourceSignature", snapshot.sourceSignature);
        root.put("sourceItemCount", snapshot.sourceItemCount);
        root.put("groupCount", snapshot.groupCount());
        root.put("analysisPolicyVersion", snapshot.analysisPolicyVersion);
        root.put("countryIdentityPolicyVersion", snapshot.countryIdentityPolicyVersion);
        JSONArray groups = new JSONArray();
        for (DiscoveryMemoryGroup group : snapshot.groups) {
            groups.put(groupToJson(group));
        }
        root.put("groups", groups);
        return root;
    }

    static DiscoverySnapshot fromJson(JSONObject root) {
        if (root == null) {
            return empty();
        }
        JSONArray groupArray = root.optJSONArray("groups");
        List<DiscoveryMemoryGroup> groups = new ArrayList<>();
        if (groupArray != null) {
            for (int i = 0; i < groupArray.length(); i++) {
                JSONObject groupJson = groupArray.optJSONObject(i);
                if (groupJson != null) {
                    groups.add(groupFromJson(groupJson));
                }
            }
        }
        return new DiscoverySnapshot(
                root.optInt("schemaVersion", DiscoverySnapshot.CURRENT_SCHEMA_VERSION),
                root.optLong("snapshotVersion", 0L),
                root.optLong("createdAtMillis", 0L),
                root.optString("sourceSignature", ""),
                root.optInt("sourceItemCount", 0),
                groups,
                root.optString("analysisPolicyVersion", ""),
                root.optString("countryIdentityPolicyVersion", ""));
    }

    private static JSONObject groupToJson(DiscoveryMemoryGroup group) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("memoryKey", group.memoryKey);
        json.put("placeKey", group.placeKey);
        json.put("placeName", group.placeName);
        json.put("countryCode", emptyToNull(group.countryCode));
        json.put("countryName", emptyToNull(group.countryName));
        json.put("adminArea", emptyToNull(group.adminArea));
        json.put("addressLine", emptyToNull(group.addressLine));
        json.put("itemCount", group.itemCount);
        json.put("photoCount", group.photoCount);
        json.put("videoCount", group.videoCount);
        json.put("startDateMillis", group.startDateMillis);
        json.put("endDateMillis", group.endDateMillis);
        json.put("coverUri", emptyToNull(group.coverUri));
        json.put("staleCount", group.staleCount);
        json.put("snapshotVersion", group.snapshotVersion);
        JSONArray refs = new JSONArray();
        for (DiscoveryPhotoRef ref : group.photoRefs) {
            refs.put(photoRefToJson(ref));
        }
        json.put("photoRefs", refs);
        return json;
    }

    private static DiscoveryMemoryGroup groupFromJson(JSONObject json) {
        JSONArray refArray = json.optJSONArray("photoRefs");
        List<DiscoveryPhotoRef> refs = new ArrayList<>();
        if (refArray != null) {
            for (int i = 0; i < refArray.length(); i++) {
                JSONObject refJson = refArray.optJSONObject(i);
                if (refJson != null) {
                    refs.add(photoRefFromJson(refJson));
                }
            }
        }
        return new DiscoveryMemoryGroup(
                json.optString("memoryKey", ""),
                json.optString("placeKey", ""),
                json.optString("placeName", ""),
                json.optString("countryCode", ""),
                json.optString("countryName", ""),
                json.optString("adminArea", ""),
                json.optString("addressLine", ""),
                json.optInt("itemCount", refs.size()),
                json.optInt("photoCount", 0),
                json.optInt("videoCount", 0),
                json.optLong("startDateMillis", 0L),
                json.optLong("endDateMillis", 0L),
                json.optString("coverUri", ""),
                refs,
                json.optInt("staleCount", 0),
                json.optLong("snapshotVersion", 0L));
    }

    private static JSONObject photoRefToJson(DiscoveryPhotoRef ref) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("sourceUri", ref.sourceUri);
        json.put("mediaStoreId", ref.mediaStoreId);
        json.put("mediaKind", ref.mediaKind.name());
        json.put("mimeType", emptyToNull(ref.mimeType));
        json.put("displayName", emptyToNull(ref.displayName));
        json.put("takenAtMillis", ref.takenAtMillis);
        json.put("locationKey", emptyToNull(ref.locationKey));
        json.put("placeName", emptyToNull(ref.placeName));
        json.put("countryCode", emptyToNull(ref.countryCode));
        json.put("countryName", emptyToNull(ref.countryName));
        json.put("adminArea", emptyToNull(ref.adminArea));
        json.put("addressLine", emptyToNull(ref.addressLine));
        json.put("sourceRelativePath", emptyToNull(ref.sourceRelativePath));
        json.put("firstSeenSnapshotVersion", ref.firstSeenSnapshotVersion);
        json.put("lastSeenSnapshotVersion", ref.lastSeenSnapshotVersion);
        json.put("stale", ref.stale);
        return json;
    }

    private static DiscoveryPhotoRef photoRefFromJson(JSONObject json) {
        return new DiscoveryPhotoRef(
                json.optString("sourceUri", ""),
                json.optLong("mediaStoreId", DiscoveryPhotoRef.UNKNOWN_ID),
                mediaKindFrom(json.optString("mediaKind", "")),
                json.optString("mimeType", ""),
                json.optString("displayName", ""),
                json.optLong("takenAtMillis", DiscoveryPhotoRef.UNKNOWN_TIME),
                json.optString("locationKey", ""),
                json.optString("placeName", ""),
                json.optString("countryCode", ""),
                json.optString("countryName", ""),
                json.optString("adminArea", ""),
                json.optString("addressLine", ""),
                json.optString("sourceRelativePath", ""),
                json.optLong("firstSeenSnapshotVersion", 0L),
                json.optLong("lastSeenSnapshotVersion", 0L),
                json.optBoolean("stale", false));
    }

    private static MediaKind mediaKindFrom(String value) {
        if ("VIDEO".equals(value)) {
            return MediaKind.VIDEO;
        }
        return MediaKind.PHOTO;
    }

    private static DiscoverySnapshot empty() {
        return new DiscoverySnapshot(
                DiscoverySnapshot.CURRENT_SCHEMA_VERSION,
                0L,
                0L,
                "",
                0,
                new ArrayList<>(),
                "",
                "");
    }

    private static Object emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? JSONObject.NULL : value;
    }
}
