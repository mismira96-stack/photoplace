package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
}
