package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;

public class DiscoverySnapshotJsonTest {
    @Test
    public void roundTripPreservesDiscoveryOnlyPhotoRefs() throws Exception {
        DiscoveryPhotoRef ref = new DiscoveryPhotoRef(
                "content://media/external/images/media/123",
                123L,
                MediaKind.PHOTO,
                "image/jpeg",
                "IMG_0001.jpg",
                1785600000000L,
                "삿포로",
                "삿포로",
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan",
                "DCIM/Camera/",
                7L,
                7L,
                false);
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup(
                "memory:JP|Hokkaido|Sapporo|2026-08-02|2026-08-06",
                "JP|Hokkaido|Sapporo",
                "삿포로",
                "JP",
                "Japan",
                "Hokkaido",
                "Sapporo, Hokkaido, Japan",
                1,
                1,
                0,
                1785600000000L,
                1785945600000L,
                ref.sourceUri,
                Collections.singletonList(ref),
                0,
                7L);
        DiscoverySnapshot snapshot = new DiscoverySnapshot(
                DiscoverySnapshot.CURRENT_SCHEMA_VERSION,
                7L,
                1786000000000L,
                "folder:DCIM/Camera|count:1",
                1,
                Collections.singletonList(group),
                "analysis-v1",
                "country-v1");

        JSONObject json = DiscoverySnapshotJson.toJson(snapshot);
        DiscoverySnapshot restored = DiscoverySnapshotJson.fromJson(json);

        assertEquals(1, restored.groupCount());
        assertEquals("JP", restored.groups.get(0).countryCode);
        assertEquals("일본", restored.groups.get(0).countryName);
        assertEquals(1, restored.groups.get(0).photoRefs.size());
        assertEquals("content://media/external/images/media/123", restored.groups.get(0).photoRefs.get(0).sourceUri);
        assertEquals(MediaKind.PHOTO, restored.groups.get(0).photoRefs.get(0).mediaKind);
        assertEquals("DCIM/Camera/", restored.groups.get(0).photoRefs.get(0).sourceRelativePath);
        assertFalse(restored.groups.get(0).photoRefs.get(0).stale);
    }

    @Test
    public void missingSnapshotReadsAsEmptySnapshot() {
        DiscoverySnapshot restored = DiscoverySnapshotJson.fromJson(null);

        assertEquals(DiscoverySnapshot.CURRENT_SCHEMA_VERSION, restored.schemaVersion);
        assertEquals(0, restored.groupCount());
    }

    @Test
    public void unsupportedSchemaReadsAsEmptySnapshot() throws Exception {
        JSONObject root = new JSONObject()
                .put("schemaVersion", DiscoverySnapshot.CURRENT_SCHEMA_VERSION + 1)
                .put("groups", new JSONArray().put(new JSONObject()
                        .put("memoryKey", "memory:future")
                        .put("placeKey", "future")
                        .put("itemCount", 1)));

        DiscoverySnapshot restored = DiscoverySnapshotJson.fromJson(root);

        assertEquals(DiscoverySnapshot.CURRENT_SCHEMA_VERSION, restored.schemaVersion);
        assertEquals(0, restored.groupCount());
    }

    @Test
    public void invalidGroupsTypeReadsAsEmptySnapshot() throws Exception {
        JSONObject root = new JSONObject()
                .put("schemaVersion", DiscoverySnapshot.CURRENT_SCHEMA_VERSION)
                .put("groups", "not-an-array");

        DiscoverySnapshot restored = DiscoverySnapshotJson.fromJson(root);

        assertEquals(0, restored.groupCount());
    }

    @Test
    public void fromJsonRecalculatesCountsWhenPhotoRefsExist() throws Exception {
        JSONObject photo = new JSONObject()
                .put("sourceUri", "content://media/external/images/media/1")
                .put("mediaKind", "PHOTO")
                .put("stale", true);
        JSONObject video = new JSONObject()
                .put("sourceUri", "content://media/external/video/media/2")
                .put("mediaKind", "VIDEO")
                .put("stale", false);
        JSONObject group = new JSONObject()
                .put("memoryKey", "memory:JP|Hokkaido|Sapporo")
                .put("placeKey", "JP|Hokkaido|Sapporo")
                .put("itemCount", 99)
                .put("photoCount", 0)
                .put("videoCount", 0)
                .put("staleCount", 0)
                .put("photoRefs", new JSONArray().put(photo).put(video));
        JSONObject root = new JSONObject()
                .put("groups", new JSONArray().put(group));

        DiscoverySnapshot restored = DiscoverySnapshotJson.fromJson(root);

        assertEquals(1, restored.groupCount());
        assertEquals(2, restored.groups.get(0).itemCount);
        assertEquals(1, restored.groups.get(0).photoCount);
        assertEquals(1, restored.groups.get(0).videoCount);
        assertEquals(1, restored.groups.get(0).staleCount);
    }

    @Test
    public void fromJsonSkipsInvalidRefsAndGroups() throws Exception {
        JSONObject invalidRef = new JSONObject()
                .put("sourceUri", "")
                .put("mediaKind", "PHOTO");
        JSONObject invalidGroup = new JSONObject()
                .put("memoryKey", "")
                .put("placeKey", "JP|Hokkaido|Sapporo")
                .put("itemCount", 1)
                .put("photoRefs", new JSONArray().put(invalidRef));
        JSONObject emptyGroup = new JSONObject()
                .put("memoryKey", "memory:empty")
                .put("placeKey", "JP|empty")
                .put("itemCount", 0)
                .put("photoRefs", new JSONArray());
        JSONObject validSummaryOnlyGroup = new JSONObject()
                .put("memoryKey", "memory:summary")
                .put("placeKey", "JP|summary")
                .put("itemCount", 3);
        JSONObject root = new JSONObject()
                .put("groups", new JSONArray()
                        .put(invalidGroup)
                        .put(emptyGroup)
                        .put(validSummaryOnlyGroup));

        DiscoverySnapshot restored = DiscoverySnapshotJson.fromJson(root);

        assertEquals(1, restored.groupCount());
        assertEquals("memory:summary", restored.groups.get(0).memoryKey);
        assertEquals(3, restored.groups.get(0).itemCount);
    }
}
