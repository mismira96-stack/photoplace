package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class MemoryCollectionStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createsAndReadsStableMembersWithoutChangingAliases() throws Exception {
        MemoryCollectionStore store = new MemoryCollectionStore(temporaryFolder.newFolder("collections"));

        MemoryCollection collection = store.create("2026 삿포로 여행", members("mem_sapporo", "mem_otaru"), 100L);

        assertNotNull(collection);
        assertTrue(collection.collectionId.startsWith("group_"));
        assertEquals("2026 삿포로 여행", collection.title);
        assertEquals(2, collection.members.size());
        assertEquals("discovery:sapporo", collection.members.get(0).lastKnownAlias);
        assertEquals(collection.collectionId, store.readAll().get(0).collectionId);
    }

    @Test
    public void onePlaceCanBelongToOnlyOneActiveCollection() throws Exception {
        MemoryCollectionStore store = new MemoryCollectionStore(temporaryFolder.newFolder("one-group"));
        assertNotNull(store.create("삿포로 여행", members("mem_sapporo", "mem_otaru"), 10L));

        assertNull(store.create("다른 여행", members("mem_sapporo", "mem_chitose"), 20L));
        assertEquals(1, store.readAll().size());
    }

    @Test
    public void renameAndDissolveOnlyChangeCollectionMetadata() throws Exception {
        MemoryCollectionStore store = new MemoryCollectionStore(temporaryFolder.newFolder("lifecycle"));
        MemoryCollection collection = store.create("삿포로", members("mem_sapporo", "mem_otaru"), 10L);

        assertTrue(store.rename(collection.collectionId, "2026 삿포로 여행", 20L));
        assertEquals("2026 삿포로 여행", store.readAll().get(0).title);
        assertTrue(store.dissolve(collection.collectionId));
        assertTrue(store.readAll().isEmpty());
    }

    @Test
    public void invalidTitleOrFewerThanTwoDistinctMembersAreRejected() throws Exception {
        MemoryCollectionStore store = new MemoryCollectionStore(temporaryFolder.newFolder("invalid"));

        assertNull(store.create("", members("mem_sapporo", "mem_otaru"), 1L));
        assertNull(store.create("여행", Arrays.asList(
                new MemoryCollection.Member("mem_sapporo", "discovery:삿포로"),
                new MemoryCollection.Member("mem_sapporo", "discovery:삿포로")), 1L));
        assertTrue(store.readAll().isEmpty());
    }

    @Test
    public void corruptMainFileRestoresBackupAndUnsafeFileIsNotOverwritten() throws Exception {
        File folder = temporaryFolder.newFolder("recovery");
        MemoryCollectionStore store = new MemoryCollectionStore(folder);
        MemoryCollection collection = store.create("삿포로 여행", members("mem_sapporo", "mem_otaru"), 10L);

        File main = new File(folder, "memory_collections.json");
        File backup = new File(folder, "memory_collections.json.bak");
        Files.copy(main.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.write(main.toPath(), "{not-json".getBytes(StandardCharsets.UTF_8));

        assertEquals(collection.collectionId, store.readAll().get(0).collectionId);
        Files.write(main.toPath(), "{not-json".getBytes(StandardCharsets.UTF_8));
        Files.deleteIfExists(backup.toPath());
        assertFalse(store.rename(collection.collectionId, "새 제목", 20L));
        assertEquals("{not-json", new String(Files.readAllBytes(main.toPath()), StandardCharsets.UTF_8));
    }

    private static java.util.List<MemoryCollection.Member> members(String first, String second) {
        return Arrays.asList(
                new MemoryCollection.Member(first, "discovery:" + first.substring("mem_".length())),
                new MemoryCollection.Member(second, "discovery:" + second.substring("mem_".length())));
    }
}
