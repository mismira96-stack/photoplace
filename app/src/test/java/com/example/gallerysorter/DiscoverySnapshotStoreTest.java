package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

public class DiscoverySnapshotStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void missingSnapshotReadsAsEmpty() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("missing"));

        DiscoverySnapshot snapshot = store.read();

        assertEquals(DiscoverySnapshot.CURRENT_SCHEMA_VERSION, snapshot.schemaVersion);
        assertEquals(0, snapshot.groupCount());
    }

    @Test
    public void saveAndReadRoundTrip() throws Exception {
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(temporaryFolder.newFolder("roundtrip"));

        assertTrue(store.save(sampleSnapshot(7L)));
        DiscoverySnapshot restored = store.read();

        assertEquals(7L, restored.snapshotVersion);
        assertEquals("folder:Download|count:1", restored.sourceSignature);
        assertEquals(1, restored.groupCount());
        assertEquals("삿포로", restored.groups.get(0).placeName);
        assertEquals("content://media/external/images/media/123", restored.groups.get(0).photoRefs.get(0).sourceUri);
    }

    @Test
    public void readRestoresBackupWhenMainFileIsCorrupt() throws Exception {
        File dir = temporaryFolder.newFolder("backup");
        DiscoverySnapshot snapshot = sampleSnapshot(8L);
        writeText(new File(dir, "discovery_snapshot.json"), "{broken");
        writeText(new File(dir, "discovery_snapshot.json.bak"), DiscoverySnapshotJson.toJson(snapshot).toString(2));
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(dir);

        DiscoverySnapshot restored = store.read();

        assertEquals(8L, restored.snapshotVersion);
        assertEquals(1, restored.groupCount());
        assertFalse(new File(dir, "discovery_snapshot.json.bak").exists());
        assertTrue(new File(dir, "discovery_snapshot.json.corrupt").exists());
    }

    @Test
    public void readRestoresBackupWhenMainFileHasUnsupportedSchema() throws Exception {
        File dir = temporaryFolder.newFolder("semantic-backup");
        DiscoverySnapshot snapshot = sampleSnapshot(8L);
        writeText(new File(dir, "discovery_snapshot.json"),
                new JSONObject()
                        .put("schemaVersion", DiscoverySnapshot.CURRENT_SCHEMA_VERSION + 1)
                        .put("groups", Collections.emptyList())
                        .toString(2));
        writeText(new File(dir, "discovery_snapshot.json.bak"), DiscoverySnapshotJson.toJson(snapshot).toString(2));
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(dir);

        DiscoverySnapshot restored = store.read();

        assertEquals(8L, restored.snapshotVersion);
        assertEquals(1, restored.groupCount());
        assertFalse(new File(dir, "discovery_snapshot.json.bak").exists());
        assertTrue(new File(dir, "discovery_snapshot.json.corrupt").exists());
    }

    @Test
    public void saveDoesNotOverwriteCorruptFileWithoutBackup() throws Exception {
        File dir = temporaryFolder.newFolder("corrupt");
        File target = new File(dir, "discovery_snapshot.json");
        writeText(target, "{broken");
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(dir);

        assertFalse(store.save(sampleSnapshot(9L)));

        assertEquals("{broken", readText(target));
    }

    @Test
    public void saveDoesNotOverwriteUnsupportedSchemaWithoutBackup() throws Exception {
        File dir = temporaryFolder.newFolder("semantic-corrupt");
        File target = new File(dir, "discovery_snapshot.json");
        String unsupported = new JSONObject()
                .put("schemaVersion", DiscoverySnapshot.CURRENT_SCHEMA_VERSION + 1)
                .put("groups", Collections.emptyList())
                .toString(2);
        writeText(target, unsupported);
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(dir);

        assertFalse(store.save(sampleSnapshot(9L)));

        assertEquals(unsupported, readText(target));
    }

    @Test
    public void clearDeletesSnapshotFiles() throws Exception {
        File dir = temporaryFolder.newFolder("clear");
        writeText(new File(dir, "discovery_snapshot.json"), new JSONObject().put("groups", Collections.emptyList()).toString());
        writeText(new File(dir, "discovery_snapshot.json.tmp"), "tmp");
        writeText(new File(dir, "discovery_snapshot.json.bak"), "bak");
        DiscoverySnapshotStore store = new DiscoverySnapshotStore(dir);

        assertTrue(store.clear());

        assertFalse(new File(dir, "discovery_snapshot.json").exists());
        assertFalse(new File(dir, "discovery_snapshot.json.tmp").exists());
        assertFalse(new File(dir, "discovery_snapshot.json.bak").exists());
    }

    private static DiscoverySnapshot sampleSnapshot(long snapshotVersion) {
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
                "Download/",
                snapshotVersion,
                snapshotVersion,
                false);
        DiscoveryMemoryGroup group = new DiscoveryMemoryGroup(
                "memory:JP|Hokkaido|Sapporo",
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
                snapshotVersion);
        return new DiscoverySnapshot(
                DiscoverySnapshot.CURRENT_SCHEMA_VERSION,
                snapshotVersion,
                1786000000000L,
                "folder:Download|count:1",
                1,
                Collections.singletonList(group),
                "analysis-v1",
                "country-v1");
    }

    private static void writeText(File file, String value) throws Exception {
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }

    private static String readText(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
