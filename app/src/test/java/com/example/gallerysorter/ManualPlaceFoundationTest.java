package com.example.gallerysorter;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class ManualPlaceFoundationTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private UnresolvedMediaRecord row(String volume, String version, long id, String path, String kind) {
        return new UnresolvedMediaRecord("content://media/external/" + (kind.equals("PHOTO") ? "images" : "video")
                + "/media/" + id, volume, version, id, kind, "IMG_0001.jpg", "image/jpeg", path, 10, 50, 100);
    }

    private ManualPlaceSnapshot manual(String id) throws Exception {
        JSONObject root = ManualPlaceSnapshot.empty().writableCopy();
        root.getJSONObject("places").put("abc", new JSONObject()
                .put("memoryKey", "manual:abc").put("stableMemoryId", id).put("name", "우리집"));
        return new ManualPlaceSnapshot(8, root.toString());
    }

    @Test public void separateVolumesVersionsAndKindsDoNotCollapseSameId() throws Exception {
        ManualPlaceSnapshot snapshot = ManualPlaceInventory.reconcile(ManualPlaceSnapshot.empty(),
                Collections.singletonList("Download/"), Arrays.asList(
                        row("external_primary", "v1", 1, "Download/", "PHOTO"),
                        row("1234-5678", "v1", 1, "Download/", "PHOTO"),
                        row("external_primary", "v2", 1, "Download/", "PHOTO"),
                        row("external_primary", "v1", 1, "Download/", "VIDEO")));
        assertEquals(4, snapshot.unresolved().size());
        assertEquals(1, snapshot.revision);
    }

    @Test public void partialFolderRefreshRetainsOtherFolders() throws Exception {
        ManualPlaceSnapshot before = ManualPlaceInventory.reconcile(ManualPlaceSnapshot.empty(),
                Arrays.asList("Download/", "DCIM/Camera/"), Arrays.asList(
                        row("external_primary", "v1", 1, "Download/", "PHOTO"),
                        row("external_primary", "v1", 2, "DCIM/Camera/", "PHOTO")));
        ManualPlaceSnapshot after = ManualPlaceInventory.reconcile(before,
                Collections.singletonList("Download/"), Collections.emptyList());
        assertEquals(1, after.unresolved().size());
        assertEquals(2, after.unresolved().get(0).mediaId);
        assertEquals(2, after.revision);
        assertEquals(2, before.unresolved().size());
    }

    @Test public void badIncomingScopeCannotPartlyMutateOriginal() throws Exception {
        ManualPlaceSnapshot before = ManualPlaceSnapshot.empty();
        try {
            ManualPlaceInventory.reconcile(before, Collections.singletonList("Download/"),
                    Collections.singletonList(row("external_primary", "v1", 1, "Other/", "PHOTO")));
            fail();
        } catch (IllegalArgumentException expected) { }
        assertEquals(0, before.revision);
        assertTrue(before.unresolved().isEmpty());
    }

    @Test public void unresolvedRemovalPreservesAssignmentsAndUserPlaces() throws Exception {
        JSONObject root = manual("mem_abc").writableCopy();
        root.getJSONObject("assignments").put("asset", new JSONObject().put("target", "mem_abc"));
        ManualPlaceSnapshot before = new ManualPlaceSnapshot(8, root.toString());
        ManualPlaceSnapshot after = ManualPlaceInventory.reconcile(before,
                Collections.singletonList("Download/"), Collections.emptyList());
        assertTrue(after.hasAssignments());
        assertEquals("mem_abc", after.mergedAliases(Collections.emptyMap()).get("manual:abc"));
    }

    @Test public void mergeKeepsLegacyIdsWithoutSecondWrite() throws Exception {
        Map<String, String> legacy = new LinkedHashMap<>();
        legacy.put("path:Pictures/성남에서/", "mem_old");
        Map<String, String> merged = manual("mem_abc").mergedAliases(legacy);
        assertEquals("mem_old", merged.get("path:Pictures/성남에서/"));
        assertEquals("mem_abc", merged.get("manual:abc"));
        assertEquals(1, legacy.size());
        try { merged.put("x", "y"); fail(); } catch (UnsupportedOperationException expected) { }
    }

    @Test public void conflictingAliasFailsClosed() throws Exception {
        try { manual("mem_abc").mergedAliases(Collections.singletonMap("manual:abc", "mem_other")); fail(); }
        catch (IllegalStateException expected) { }
    }

    @Test public void registryResolvesCanonicalManualIdWithoutCreatingJsonAlias() throws Exception {
        File dir = temporary.newFolder();
        MemoryIdentityRegistryStore registry = new MemoryIdentityRegistryStore(dir, () -> {
            try { return manual("mem_abc"); } catch (Exception error) { throw new IllegalStateException(error); }
        });
        assertEquals("mem_abc", registry.resolveOrCreate("manual:abc"));
        assertTrue(registry.registerAlias("mem_abc", "manual:abc"));
        assertFalse(registry.registerAlias("mem_other", "manual:abc"));
        assertEquals("", registry.resolveOrCreate("manual:missing"));
        assertFalse(new File(dir, "memory_identity_registry.json").exists());
    }

    @Test public void registryDetectsPersistedLegacyConflict() throws Exception {
        File dir = temporary.newFolder();
        Files.write(new File(dir, "memory_identity_registry.json").toPath(),
                "{\"schemaVersion\":1,\"aliases\":{\"manual:abc\":\"mem_other\"}}".getBytes(StandardCharsets.UTF_8));
        MemoryIdentityRegistryStore registry = new MemoryIdentityRegistryStore(dir, () -> {
            try { return manual("mem_abc"); } catch (Exception error) { throw new IllegalStateException(error); }
        });
        try { registry.findStableId("manual:abc"); fail(); } catch (IllegalStateException expected) { }
    }

    @Test public void snapshotCopyDoesNotLeakMutableJson() throws Exception {
        ManualPlaceSnapshot snapshot = manual("mem_abc");
        snapshot.writableCopy().getJSONObject("places").remove("abc");
        assertEquals(1, snapshot.mergedAliases(Collections.emptyMap()).size());
    }

    @Test public void identityProjectionKeepsRevisionAndAliasesWithoutLoadingInventory() throws Exception {
        ManualPlaceSnapshot full = ManualPlaceInventory.reconcile(manual("mem_abc"),
                Collections.singletonList("Download/"),
                Collections.singletonList(row("external_primary", "v1", 1, "Download/", "PHOTO")));
        ManualPlaceSnapshot identities = new ManualPlaceSnapshot(full.revision, full.identitiesPayload());
        assertEquals(full.revision, identities.revision);
        assertTrue(identities.unresolved().isEmpty());
        assertEquals("mem_abc", identities.mergedAliases(Collections.emptyMap()).get("manual:abc"));
        assertEquals(1, full.unresolved().size());
    }

    @Test public void malformedPayloadIsNotAnEmptySuccessfulState() throws Exception {
        for (String value : Arrays.asList("broken", "{}", "{\"schemaVersion\":2}",
                "{\"schemaVersion\":1,\"places\":{},\"assignments\":{},\"unresolved\":{\"bad\":{}}}")) {
            try { new ManualPlaceSnapshot(1, value); fail(value); } catch (Exception expected) { }
        }
    }

    @Test public void ambiguousExternalVolumeCannotBecomePhysicalIdentity() {
        try { row("external", "v1", 1, "Download/", "PHOTO"); fail(); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void negativeOrOverflowRevisionCannotCommit() throws Exception {
        try { new ManualPlaceSnapshot(-1, ManualPlaceSnapshot.empty().identitiesPayload()); fail(); }
        catch (IllegalArgumentException expected) { }
        try {
            ManualPlaceInventory.reconcile(new ManualPlaceSnapshot(Long.MAX_VALUE, ManualPlaceSnapshot.empty().identitiesPayload()),
                    Collections.singletonList("Download/"), Collections.emptyList());
            fail();
        } catch (ArithmeticException expected) { }
    }

    @Test public void negativeOptionalMetadataDoesNotInvalidateIdentityOrDiscardPeers() throws Exception {
        UnresolvedMediaRecord legacy = new UnresolvedMediaRecord("content://media/external/images/media/2",
                "external_primary", "v1", 2, "PHOTO", "old.jpg", "image/jpeg", "Download/", -1, -2, -3);
        assertEquals(0, legacy.size); assertEquals(0, legacy.modified); assertEquals(0, legacy.takenAt);
        assertEquals(row("external_primary", "v1", 2, "Download/", "PHOTO").assetKey(), legacy.assetKey());
        ManualPlaceSnapshot snapshot = ManualPlaceInventory.reconcile(ManualPlaceSnapshot.empty(),
                Collections.singletonList("Download/"), Arrays.asList(
                        row("external_primary", "v1", 1, "Download/", "PHOTO"), legacy,
                        row("external_primary", "v1", 3, "Download/", "PHOTO")));
        assertEquals(3, snapshot.unresolved().size());
        assertEquals(0, UnresolvedMediaRecord.fromJson(legacy.toJson()).takenAt);
    }

    @Test public void largeInventoryUsesImmutableObjectsAndNeverInflatesIdentityPayload() throws Exception {
        java.util.List<UnresolvedMediaRecord> rows = new java.util.ArrayList<>();
        for (int i = 1; i <= 3500; i++) rows.add(row("external_primary", "v1", i, "Download/", "PHOTO"));
        ManualPlaceSnapshot identities = manual("mem_abc");
        ManualPlaceSnapshot full = identities.withInventory(identities.revision, rows);
        assertEquals(identities.identitiesPayload(), full.identitiesPayload());
        assertSame(full.unresolved(), full.unresolved());
        assertSame(rows.get(0), full.unresolved().get(0));
        rows.clear();
        assertEquals(3500, full.unresolved().size());
        assertEquals(0, full.writableCopy().getJSONObject("unresolved").length());
        try { full.unresolved().clear(); fail(); } catch (UnsupportedOperationException expected) { }
    }

    @Test public void outsideScopeSameKeyDifferentUriIsRejected() throws Exception {
        ManualPlaceSnapshot before = ManualPlaceInventory.reconcile(ManualPlaceSnapshot.empty(),
                Collections.singletonList("Old/"), Collections.singletonList(row("external_primary", "v1", 1, "Old/", "PHOTO")));
        UnresolvedMediaRecord conflicting = new UnresolvedMediaRecord("content://media/external_primary/images/media/1",
                "external_primary", "v1", 1, "PHOTO", "same.jpg", "image/jpeg", "New/", 1, 1, 1);
        try {
            ManualPlaceInventory.reconcile(before, Collections.singletonList("New/"), Collections.singletonList(conflicting));
            fail();
        } catch (IllegalStateException expected) { }
        assertEquals("Old/", before.unresolved().get(0).relativePath);
        assertEquals(1, before.revision);
    }

    @Test public void sameUriMayReconcileAnObservedMoveAcrossScopes() throws Exception {
        ManualPlaceSnapshot before = ManualPlaceInventory.reconcile(ManualPlaceSnapshot.empty(),
                Collections.singletonList("Old/"), Collections.singletonList(row("external_primary", "v1", 1, "Old/", "PHOTO")));
        ManualPlaceSnapshot after = ManualPlaceInventory.reconcile(before, Collections.singletonList("New/"),
                Collections.singletonList(row("external_primary", "v1", 1, "New/", "PHOTO")));
        assertEquals(1, after.unresolved().size());
        assertEquals("New/", after.unresolved().get(0).relativePath);
    }

    @Test public void duplicateIncomingKeyFailsBeforeRevisionCommit() {
        UnresolvedMediaRecord row = row("external_primary", "v1", 1, "Download/", "PHOTO");
        try {
            new ManualPlaceInventory.Update(0, Collections.singletonList("Download/"), Arrays.asList(row, row));
            fail();
        } catch (IllegalStateException expected) { }
    }

    @Test public void normalizedSlashScopeMatchesButDifferentCaseRemainsSeparate() throws Exception {
        ManualPlaceSnapshot before = ManualPlaceInventory.reconcile(ManualPlaceSnapshot.empty(),
                Arrays.asList("Download/", "download/"), Arrays.asList(
                        row("external_primary", "v1", 1, "Download/", "PHOTO"),
                        row("external_primary", "v1", 2, "download/", "PHOTO")));
        ManualPlaceSnapshot after = ManualPlaceInventory.reconcile(before,
                Collections.singletonList("Download\\\\"), Collections.emptyList());
        assertEquals(1, after.unresolved().size());
        assertEquals("download/", after.unresolved().get(0).relativePath);
        assertEquals("Download/Sub/", ManualPlacePaths.normalize("Download//Sub"));
    }

    @Test public void invalidPathsCannotExpandScanScope() {
        for (String path : Arrays.asList("", "/Download/", "Download/../", "./Download/")) {
            try { ManualPlacePaths.normalize(path); fail(path); } catch (IllegalArgumentException expected) { }
        }
    }
}
