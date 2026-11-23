package com.example.gallerysorter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

public class SettingsBackupTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File filesDir;

    @Before
    public void setUp() throws Exception {
        filesDir = temp.newFolder("files");
    }

    @Test
    public void generateBackupData_emptyDir_returnsZeroItems() {
        SettingsBackupController.BackupSummary summary = SettingsBackupController.generateBackupData(filesDir);
        assertEquals(0, summary.totalItems());
        assertEquals(0, summary.notesCount);
        assertEquals(0, summary.placesCount);
        assertEquals(0, summary.collectionsCount);
    }

    @Test
    public void generateBackupData_withExistingRecords_gathersAllData() throws Exception {
        // Create mock notes
        JSONObject notesRoot = new JSONObject();
        notesRoot.put("schemaVersion", 1);
        JSONObject notes = new JSONObject();
        notes.put("memory-id:1#2026-11-15", new JSONObject().put("text", "가족 모임"));
        notes.put("memory-id:2#2026-11-16", new JSONObject().put("text", "출근길"));
        notesRoot.put("notes", notes);
        SettingsBackupController.atomicWrite(new File(filesDir, "memory_date_notes.json"), notesRoot.toString(2));

        // Create mock personal places
        JSONObject placesRoot = new JSONObject();
        placesRoot.put("schemaVersion", 1);
        JSONArray places = new JSONArray();
        places.put(new JSONObject().put("placeId", "p1").put("placeName", "우리집"));
        placesRoot.put("places", places);
        SettingsBackupController.atomicWrite(new File(filesDir, "personal_places.json"), placesRoot.toString(2));

        // Create mock collections
        JSONObject collectionsRoot = new JSONObject();
        collectionsRoot.put("schemaVersion", 1);
        JSONArray collections = new JSONArray();
        collections.put(new JSONObject().put("collectionId", "c1").put("title", "여행"));
        collectionsRoot.put("collections", collections);
        SettingsBackupController.atomicWrite(new File(filesDir, "memory_collections.json"), collectionsRoot.toString(2));

        SettingsBackupController.BackupSummary summary = SettingsBackupController.generateBackupData(filesDir);
        assertEquals(2, summary.notesCount);
        assertEquals(1, summary.placesCount);
        assertEquals(1, summary.collectionsCount);
        assertEquals(4, summary.totalItems());

        JSONObject parsed = new JSONObject(summary.jsonString);
        assertEquals(1, parsed.getInt("backupVersion"));
        assertEquals("PhotoPlace", parsed.getString("app"));
        assertTrue(parsed.getJSONObject("data").has("memory_date_notes"));
        assertTrue(parsed.getJSONObject("data").has("personal_places"));
        assertTrue(parsed.getJSONObject("data").has("memory_collections"));
    }

    @Test
    public void restoreFromJsonString_emptyDir_restoresAllRecords() throws Exception {
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        backupRoot.put("app", "PhotoPlace");
        JSONObject data = new JSONObject();

        JSONObject notesRoot = new JSONObject();
        JSONObject notes = new JSONObject();
        notes.put("note1", new JSONObject().put("text", "메모1").put("updatedAtMillis", 1000L));
        notesRoot.put("notes", notes);
        data.put("memory_date_notes", notesRoot);

        JSONObject placesRoot = new JSONObject();
        JSONArray places = new JSONArray();
        places.put(new JSONObject().put("placeId", "p1").put("placeName", "카페"));
        placesRoot.put("places", places);
        data.put("personal_places", placesRoot);

        backupRoot.put("data", data);

        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());

        assertTrue(result.success);
        assertEquals(1, result.notesCount);
        assertEquals(1, result.placesCount);

        File notesFile = new File(filesDir, "memory_date_notes.json");
        assertTrue(notesFile.exists());
        File placesFile = new File(filesDir, "personal_places.json");
        assertTrue(placesFile.exists());
    }

    @Test
    public void restoreFromJsonString_mergesWithExistingData() throws Exception {
        // Initial state: existing note
        JSONObject currentNotesRoot = new JSONObject();
        currentNotesRoot.put("schemaVersion", 1);
        JSONObject currentNotes = new JSONObject();
        currentNotes.put("note_existing", new JSONObject().put("text", "기존 메모").put("updatedAtMillis", 500L));
        currentNotesRoot.put("notes", currentNotes);
        SettingsBackupController.atomicWrite(new File(filesDir, "memory_date_notes.json"), currentNotesRoot.toString(2));

        // Backup has: note_existing (newer) + note_new
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        JSONObject data = new JSONObject();
        JSONObject backupNotesRoot = new JSONObject();
        JSONObject backupNotes = new JSONObject();
        backupNotes.put("note_existing", new JSONObject().put("text", "수정된 메모").put("updatedAtMillis", 800L));
        backupNotes.put("note_new", new JSONObject().put("text", "새 메모").put("updatedAtMillis", 700L));
        backupNotesRoot.put("notes", backupNotes);
        data.put("memory_date_notes", backupNotesRoot);
        backupRoot.put("data", data);

        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());

        assertTrue(result.success);

        // Read restored file
        File notesFile = new File(filesDir, "memory_date_notes.json");
        JSONObject readRoot = new JSONObject(new String(java.nio.file.Files.readAllBytes(notesFile.toPath()), "UTF-8"));
        JSONObject readNotes = readRoot.getJSONObject("notes");
        assertEquals(2, readNotes.length());
        assertEquals("수정된 메모", readNotes.getJSONObject("note_existing").getString("text"));
        assertEquals("새 메모", readNotes.getJSONObject("note_new").getString("text"));
    }

    @Test
    public void restoreFromJsonString_invalidJson_returnsFailure() {
        SettingsBackupController.RestoreResult r1 =
                SettingsBackupController.restoreFromJsonString(filesDir, "not a json");
        assertFalse(r1.success);

        SettingsBackupController.RestoreResult r2 =
                SettingsBackupController.restoreFromJsonString(filesDir, "{}");
        assertFalse(r2.success);

        SettingsBackupController.RestoreResult r3 =
                SettingsBackupController.restoreFromJsonString(filesDir, "");
        assertFalse(r3.success);
    }

    @Test
    public void generateBackupData_capturesIdentityRegistry() throws Exception {
        JSONObject registryRoot = new JSONObject();
        registryRoot.put("schemaVersion", 1);
        JSONObject aliases = new JSONObject();
        aliases.put("discovery:서초구", "mem_12345");
        registryRoot.put("aliases", aliases);
        SettingsBackupController.atomicWrite(new File(filesDir, "memory_identity_registry.json"), registryRoot.toString(2));

        SettingsBackupController.BackupSummary summary = SettingsBackupController.generateBackupData(filesDir);
        JSONObject parsed = new JSONObject(summary.jsonString);
        assertTrue(parsed.getJSONObject("data").has("memory_identity_registry"));
        assertEquals("mem_12345", parsed.getJSONObject("data")
                .getJSONObject("memory_identity_registry").getJSONObject("aliases").getString("discovery:서초구"));
    }

    @Test
    public void restore_freshInstall_restoresIdentityRegistryAndLinksNotes() throws Exception {
        // Prepare backup payload with identity registry & notes
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        backupRoot.put("app", "PhotoPlace");
        JSONObject data = new JSONObject();

        JSONObject registryRoot = new JSONObject();
        registryRoot.put("schemaVersion", 1);
        JSONObject aliases = new JSONObject();
        aliases.put("discovery:서초구", "mem_orig123");
        registryRoot.put("aliases", aliases);
        data.put("memory_identity_registry", registryRoot);

        JSONObject notesRoot = new JSONObject();
        notesRoot.put("schemaVersion", 1);
        JSONObject notes = new JSONObject();
        JSONObject noteObj = new JSONObject();
        noteObj.put("text", "서초구 첫 방문");
        noteObj.put("createdAtMillis", 100L);
        noteObj.put("updatedAtMillis", 100L);
        notes.put("memory-id:mem_orig123#20261116", noteObj);
        notesRoot.put("notes", notes);
        data.put("memory_date_notes", notesRoot);

        backupRoot.put("data", data);

        // Restore into clean filesDir
        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        assertTrue(result.success);
        assertEquals(1, result.notesCount);

        // Verify stores
        MemoryIdentityRegistryStore registryStore = new MemoryIdentityRegistryStore(filesDir);
        String stableId = registryStore.findStableId("discovery:서초구");
        assertEquals("mem_orig123", stableId);

        MemoryDateNoteStore noteStore = new MemoryDateNoteStore(filesDir);
        MemoryDateNote note = noteStore.get(stableId, "20261116");
        assertNotNull(note);
        assertEquals("서초구 첫 방문", note.text);
    }

    @Test
    public void restore_existingInstall_remapsNotesToExistingStableId() throws Exception {
        // Current device already has a different stable ID for "discovery:서초구"
        MemoryIdentityRegistryStore currentRegistry = new MemoryIdentityRegistryStore(filesDir);
        String existingStableId = currentRegistry.resolveOrCreate("discovery:서초구");
        assertFalse(existingStableId.isEmpty());
        assertNotEquals("mem_backup111", existingStableId);

        // Backup has different stable ID "mem_backup111" for the same alias
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        backupRoot.put("app", "PhotoPlace");
        JSONObject data = new JSONObject();

        JSONObject registryRoot = new JSONObject();
        registryRoot.put("schemaVersion", 1);
        JSONObject aliases = new JSONObject();
        aliases.put("discovery:서초구", "mem_backup111");
        registryRoot.put("aliases", aliases);
        data.put("memory_identity_registry", registryRoot);

        JSONObject notesRoot = new JSONObject();
        notesRoot.put("schemaVersion", 1);
        JSONObject notes = new JSONObject();
        JSONObject noteObj = new JSONObject();
        noteObj.put("text", "서초구 재방문 메모");
        noteObj.put("createdAtMillis", 200L);
        noteObj.put("updatedAtMillis", 200L);
        notes.put("memory-id:mem_backup111#20261116", noteObj);
        notesRoot.put("notes", notes);
        data.put("memory_date_notes", notesRoot);

        backupRoot.put("data", data);

        // Restore
        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        assertTrue(result.success);

        // Verify that lookup using existing stable ID succeeds!
        MemoryDateNoteStore noteStore = new MemoryDateNoteStore(filesDir);
        MemoryDateNote note = noteStore.get(existingStableId, "20261116");
        assertNotNull(note);
        assertEquals("서초구 재방문 메모", note.text);
    }

    @Test
    public void restore_olderBackupDoesNotOverwriteNewerCollection() throws Exception {
        // Current collection has newer title and timestamp
        JSONObject currentCollectionsRoot = new JSONObject();
        currentCollectionsRoot.put("schemaVersion", 1);
        JSONArray currentArray = new JSONArray();
        JSONObject currentCol = new JSONObject();
        currentCol.put("collectionId", "c1");
        currentCol.put("title", "최신 여행 모음");
        currentCol.put("updatedAtMillis", 2000L);
        currentArray.put(currentCol);
        currentCollectionsRoot.put("collections", currentArray);
        SettingsBackupController.atomicWrite(new File(filesDir, "memory_collections.json"), currentCollectionsRoot.toString(2));

        // Backup contains older version of the same collection
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        backupRoot.put("app", "PhotoPlace");
        JSONObject data = new JSONObject();
        JSONObject backupCollectionsRoot = new JSONObject();
        backupCollectionsRoot.put("schemaVersion", 1);
        JSONArray backupArray = new JSONArray();
        JSONObject backupCol = new JSONObject();
        backupCol.put("collectionId", "c1");
        backupCol.put("title", "예전 여행");
        backupCol.put("updatedAtMillis", 1000L);
        backupArray.put(backupCol);
        backupCollectionsRoot.put("collections", backupArray);
        data.put("memory_collections", backupCollectionsRoot);
        backupRoot.put("data", data);

        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        assertTrue(result.success);

        // Verify current collection title was NOT overwritten
        File collectionsFile = new File(filesDir, "memory_collections.json");
        JSONObject readRoot = new JSONObject(new String(java.nio.file.Files.readAllBytes(collectionsFile.toPath()), "UTF-8"));
        JSONArray collections = readRoot.getJSONArray("collections");
        assertEquals(1, collections.length());
        assertEquals("최신 여행 모음", collections.getJSONObject(0).getString("title"));
    }

    @Test
    public void restore_corruptSectionFailsWithoutModifyingDisk() throws Exception {
        // Current state: valid note exists
        JSONObject currentNotesRoot = new JSONObject();
        currentNotesRoot.put("schemaVersion", 1);
        JSONObject notes = new JSONObject();
        notes.put("memory-id:mem_keep#20261116", new JSONObject().put("text", "보존되어야 하는 메모"));
        currentNotesRoot.put("notes", notes);
        File notesFile = new File(filesDir, "memory_date_notes.json");
        SettingsBackupController.atomicWrite(notesFile, currentNotesRoot.toString(2));
        long originalModified = notesFile.lastModified();

        // Backup has valid notes but corrupted personal_places
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        JSONObject data = new JSONObject();

        JSONObject backupNotesRoot = new JSONObject();
        JSONObject backupNotes = new JSONObject();
        backupNotes.put("memory-id:mem_corrupt#20261116", new JSONObject().put("text", "오염 데이터"));
        backupNotesRoot.put("notes", backupNotes);
        data.put("memory_date_notes", backupNotesRoot);

        // Corrupted personal_places: string instead of JSONObject
        data.put("personal_places", "invalid_section_not_an_object");
        backupRoot.put("data", data);

        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        assertFalse(result.success);

        // Verify existing note was untouched
        JSONObject readRoot = new JSONObject(new String(java.nio.file.Files.readAllBytes(notesFile.toPath()), "UTF-8"));
        assertEquals("보존되어야 하는 메모", readRoot.getJSONObject("notes")
                .getJSONObject("memory-id:mem_keep#20261116").getString("text"));
        assertFalse(readRoot.getJSONObject("notes").has("memory-id:mem_corrupt#20261116"));
    }

    @Test
    public void restore_backupIdWithMultipleAliases_unifiesToTargetExistingId() throws Exception {
        // Target device already has discovery:서초구 mapped to mem_T1
        JSONObject currentRegistryRoot = new JSONObject();
        currentRegistryRoot.put("schemaVersion", 1);
        JSONObject curAliases = new JSONObject();
        curAliases.put("discovery:서초구", "mem_T1");
        currentRegistryRoot.put("aliases", curAliases);
        SettingsBackupController.atomicWrite(new File(filesDir, "memory_identity_registry.json"), currentRegistryRoot.toString(2));

        // Backup has both discovery:서초구 AND path:Pictures/서초구/ mapped to mem_B1
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        backupRoot.put("app", "PhotoPlace");
        JSONObject data = new JSONObject();

        JSONObject backupRegistryRoot = new JSONObject();
        backupRegistryRoot.put("schemaVersion", 1);
        JSONObject bAliases = new JSONObject();
        bAliases.put("discovery:서초구", "mem_B1");
        bAliases.put("path:Pictures/서초구/", "mem_B1");
        backupRegistryRoot.put("aliases", bAliases);
        data.put("memory_identity_registry", backupRegistryRoot);

        // Backup has note under mem_B1
        JSONObject backupNotesRoot = new JSONObject();
        backupNotesRoot.put("schemaVersion", 1);
        JSONObject notes = new JSONObject();
        JSONObject noteObj = new JSONObject();
        noteObj.put("text", "서초구 양쪽 별칭 메모");
        noteObj.put("createdAtMillis", 100L);
        noteObj.put("updatedAtMillis", 100L);
        notes.put("memory-id:mem_B1#20261116", noteObj);
        backupNotesRoot.put("notes", notes);
        data.put("memory_date_notes", backupNotesRoot);

        backupRoot.put("data", data);

        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        assertTrue(result.success);

        // Verify stores: both aliases must map to mem_T1, never split!
        MemoryIdentityRegistryStore registryStore = new MemoryIdentityRegistryStore(filesDir);
        assertEquals("mem_T1", registryStore.findStableId("discovery:서초구"));
        assertEquals("mem_T1", registryStore.findStableId("path:Pictures/서초구/"));

        // Verify note is queryable by mem_T1
        MemoryDateNoteStore noteStore = new MemoryDateNoteStore(filesDir);
        MemoryDateNote note = noteStore.get("mem_T1", "20261116");
        assertNotNull(note);
        assertEquals("서초구 양쪽 별칭 메모", note.text);
    }

    @Test
    public void restore_preWriteFailureDoesNotDeleteExistingCollections() throws Exception {
        // Target device has an existing memory_collections.json
        File collectionsFile = new File(filesDir, "memory_collections.json");
        JSONObject currentCollectionsRoot = new JSONObject();
        currentCollectionsRoot.put("schemaVersion", 1);
        JSONArray arr = new JSONArray();
        arr.put(new JSONObject().put("collectionId", "c_keep").put("title", "소중한 여행"));
        currentCollectionsRoot.put("collections", arr);
        SettingsBackupController.atomicWrite(collectionsFile, currentCollectionsRoot.toString(2));

        // Backup has invalid JSON section that fails parsing in Phase 1
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        JSONObject data = new JSONObject();
        data.put("personal_places", "not_a_json_object_corrupt");
        backupRoot.put("data", data);

        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        assertFalse(result.success);

        // Verify collectionsFile still exists and was NOT deleted!
        assertTrue(collectionsFile.exists());
        JSONObject readCollections = new JSONObject(new String(java.nio.file.Files.readAllBytes(collectionsFile.toPath()), "UTF-8"));
        assertEquals("소중한 여행", readCollections.getJSONArray("collections").getJSONObject(0).getString("title"));
    }

    @Test
    public void restore_conflictingAliasesAcrossDistinctIds_failsAndProtectsExistingNotes() throws Exception {
        // Target device has discovery:서초구 -> mem_T1 and path:Pictures/서초구/ -> mem_T2 (two separate places!)
        JSONObject currentRegistryRoot = new JSONObject();
        currentRegistryRoot.put("schemaVersion", 1);
        JSONObject curAliases = new JSONObject();
        curAliases.put("discovery:서초구", "mem_T1");
        curAliases.put("path:Pictures/서초구/", "mem_T2");
        currentRegistryRoot.put("aliases", curAliases);
        SettingsBackupController.atomicWrite(new File(filesDir, "memory_identity_registry.json"), currentRegistryRoot.toString(2));

        // Existing notes for both places
        MemoryDateNoteStore noteStore = new MemoryDateNoteStore(filesDir);
        noteStore.save("mem_T1", "20261116", "발견 메모", 100L);
        noteStore.save("mem_T2", "20261116", "앨범 메모", 100L);

        // Backup attempts to merge discovery:서초구 and path:Pictures/서초구/ into single mem_Backup1
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        backupRoot.put("app", "PhotoPlace");
        JSONObject data = new JSONObject();

        JSONObject backupRegistryRoot = new JSONObject();
        backupRegistryRoot.put("schemaVersion", 1);
        JSONObject bAliases = new JSONObject();
        bAliases.put("discovery:서초구", "mem_Backup1");
        bAliases.put("path:Pictures/서초구/", "mem_Backup1");
        backupRegistryRoot.put("aliases", bAliases);
        data.put("memory_identity_registry", backupRegistryRoot);

        JSONObject backupNotesRoot = new JSONObject();
        backupNotesRoot.put("schemaVersion", 1);
        JSONObject notes = new JSONObject();
        JSONObject noteObj = new JSONObject();
        noteObj.put("text", "백업된 통합 메모");
        noteObj.put("createdAtMillis", 500L);
        noteObj.put("updatedAtMillis", 500L);
        notes.put("memory-id:mem_Backup1#20261116", noteObj);
        backupNotesRoot.put("notes", notes);
        data.put("memory_date_notes", backupNotesRoot);

        backupRoot.put("data", data);

        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        // Must fail due to conflict to prevent destructive merge!
        assertFalse(result.success);
        assertTrue(result.message.contains("충돌"));

        // Verify existing mappings are intact
        MemoryIdentityRegistryStore registryStore = new MemoryIdentityRegistryStore(filesDir);
        assertEquals("mem_T1", registryStore.findStableId("discovery:서초구"));
        assertEquals("mem_T2", registryStore.findStableId("path:Pictures/서초구/"));

        // Verify existing separate notes are 100% intact
        assertEquals("발견 메모", noteStore.get("mem_T1", "20261116").text);
        assertEquals("앨범 메모", noteStore.get("mem_T2", "20261116").text);
    }

    @Test
    public void restore_writeFailureRollsBackBakFileWithoutLeavingFailedDataInBak() throws Exception {
        // Initial state: memory_identity_registry.json has discovery:A -> mem_A
        JSONObject registryRoot = new JSONObject();
        registryRoot.put("schemaVersion", 1);
        JSONObject aliases = new JSONObject();
        aliases.put("discovery:A", "mem_A");
        registryRoot.put("aliases", aliases);
        File registryFile = new File(filesDir, "memory_identity_registry.json");
        SettingsBackupController.atomicWrite(registryFile, registryRoot.toString(2));

        // Create a directory at .tmp where memory_date_notes.json.tmp would be written,
        // so snapshot capture succeeds, but atomicWrite to notesFile fails during write!
        File notesTemp = new File(filesDir, "memory_date_notes.json.tmp");
        assertTrue(notesTemp.mkdir());

        // Backup has new alias discovery:B -> mem_B and notes
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        backupRoot.put("app", "PhotoPlace");
        JSONObject data = new JSONObject();

        JSONObject backupRegistryRoot = new JSONObject();
        backupRegistryRoot.put("schemaVersion", 1);
        JSONObject bAliases = new JSONObject();
        bAliases.put("discovery:A", "mem_A");
        bAliases.put("discovery:B", "mem_B");
        backupRegistryRoot.put("aliases", bAliases);
        data.put("memory_identity_registry", backupRegistryRoot);

        JSONObject backupNotesRoot = new JSONObject();
        backupNotesRoot.put("schemaVersion", 1);
        JSONObject notes = new JSONObject();
        notes.put("memory-id:mem_B#20261116", new JSONObject().put("text", "메모B").put("updatedAtMillis", 100L));
        backupNotesRoot.put("notes", notes);
        data.put("memory_date_notes", backupNotesRoot);

        backupRoot.put("data", data);

        // Restore should fail because notesFile cannot be written
        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        assertFalse(result.success);

        // Clean up the mock directory
        assertTrue(notesTemp.delete());

        // Verify that .bak of registry does NOT contain the failed restore data (mem_B)
        File registryBak = new File(registryFile.getPath() + ".bak");
        if (registryBak.exists()) {
            JSONObject bakJson = new JSONObject(new String(java.nio.file.Files.readAllBytes(registryBak.toPath()), "UTF-8"));
            assertFalse(bakJson.getJSONObject("aliases").has("discovery:B"));
        }

        // Corrupt registryFile to force fallback to .bak
        assertTrue(registryFile.delete());
        MemoryIdentityRegistryStore registryStore = new MemoryIdentityRegistryStore(filesDir);
        // discovery:B should NOT exist in restored/fallback state!
        assertEquals("", registryStore.findStableId("discovery:B"));
    }

    @Test
    public void restore_corruptedPrimaryFileWithValidBak_mergesExistingDataFromBak() throws Exception {
        // Corrupt primary files on purpose, but provide valid .bak files
        File registryFile = new File(filesDir, "memory_identity_registry.json");
        File registryBak = new File(filesDir, "memory_identity_registry.json.bak");
        java.nio.file.Files.write(registryFile.toPath(), "{corrupted json".getBytes("UTF-8"));

        JSONObject bakRegistry = new JSONObject();
        bakRegistry.put("schemaVersion", 1);
        JSONObject aliases = new JSONObject();
        aliases.put("discovery:기존장소", "mem_orig");
        bakRegistry.put("aliases", aliases);
        java.nio.file.Files.write(registryBak.toPath(), bakRegistry.toString(2).getBytes("UTF-8"));

        File notesFile = new File(filesDir, "memory_date_notes.json");
        File notesBak = new File(filesDir, "memory_date_notes.json.bak");
        java.nio.file.Files.write(notesFile.toPath(), "{corrupted json".getBytes("UTF-8"));

        JSONObject bakNotes = new JSONObject();
        bakNotes.put("schemaVersion", 1);
        JSONObject notes = new JSONObject();
        notes.put("memory-id:mem_orig#20261116", new JSONObject().put("text", "기존메모").put("updatedAtMillis", 100L));
        bakNotes.put("notes", notes);
        java.nio.file.Files.write(notesBak.toPath(), bakNotes.toString(2).getBytes("UTF-8"));

        // Backup has new place and note
        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        backupRoot.put("app", "PhotoPlace");
        JSONObject data = new JSONObject();

        JSONObject backupRegistryRoot = new JSONObject();
        backupRegistryRoot.put("schemaVersion", 1);
        JSONObject bAliases = new JSONObject();
        bAliases.put("discovery:신규장소", "mem_new");
        backupRegistryRoot.put("aliases", bAliases);
        data.put("memory_identity_registry", backupRegistryRoot);

        JSONObject backupNotesRoot = new JSONObject();
        backupNotesRoot.put("schemaVersion", 1);
        JSONObject bNotes = new JSONObject();
        bNotes.put("memory-id:mem_new#20261116", new JSONObject().put("text", "신규메모").put("updatedAtMillis", 200L));
        backupNotesRoot.put("notes", bNotes);
        data.put("memory_date_notes", backupNotesRoot);

        backupRoot.put("data", data);

        // Restore should succeed because valid .bak is used for existing data
        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        assertTrue(result.success);

        // Verify both original data (from .bak) and new data (from backup) are present
        MemoryIdentityRegistryStore regStore = new MemoryIdentityRegistryStore(filesDir);
        assertEquals("mem_orig", regStore.findStableId("discovery:기존장소"));
        assertEquals("mem_new", regStore.findStableId("discovery:신규장소"));

        MemoryDateNoteStore noteStore = new MemoryDateNoteStore(filesDir);
        assertEquals("기존메모", noteStore.get("mem_orig", "20261116").text);
        assertEquals("신규메모", noteStore.get("mem_new", "20261116").text);
    }

    @Test
    public void restore_corruptedPrimaryFileWithoutValidBak_failsSafelyBeforeWriting() throws Exception {
        // Corrupt primary file, and NO .bak file
        File registryFile = new File(filesDir, "memory_identity_registry.json");
        java.nio.file.Files.write(registryFile.toPath(), "{corrupted json".getBytes("UTF-8"));

        JSONObject backupRoot = new JSONObject();
        backupRoot.put("backupVersion", 1);
        backupRoot.put("app", "PhotoPlace");
        JSONObject data = new JSONObject();
        JSONObject backupRegistryRoot = new JSONObject();
        backupRegistryRoot.put("schemaVersion", 1);
        backupRegistryRoot.put("aliases", new JSONObject().put("discovery:신규장소", "mem_new"));
        data.put("memory_identity_registry", backupRegistryRoot);
        backupRoot.put("data", data);

        // Restore should fail safely because existing data is corrupted and cannot be validated
        SettingsBackupController.RestoreResult result =
                SettingsBackupController.restoreFromJsonString(filesDir, backupRoot.toString());
        assertFalse(result.success);

        // File should still exist on disk untouched
        assertTrue(registryFile.exists());
        assertEquals("{corrupted json", new String(java.nio.file.Files.readAllBytes(registryFile.toPath()), "UTF-8"));
    }
}
