package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MemoryDateNoteStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void registryKeepsOneImmutableIdForAnAlias() throws Exception {
        MemoryIdentityRegistryStore registry = new MemoryIdentityRegistryStore(temporaryFolder.newFolder("registry"));

        String stableId = registry.resolveOrCreate("discovery:삿포로");

        assertTrue(stableId.startsWith("mem_"));
        assertEquals(stableId, registry.findStableId("discovery:삿포로"));
        assertEquals(stableId, registry.resolveOrCreate("discovery:삿포로"));
        assertTrue(registry.registerAlias(stableId, "path:Pictures/삿포로에서"));
        assertEquals(stableId, registry.findStableId("path:Pictures/삿포로에서"));
        assertFalse(registry.registerAlias("mem_other", "path:Pictures/삿포로에서"));
    }

    @Test
    public void notesAreIsolatedByStableIdAndDateAndBlankTextDeletes() throws Exception {
        MemoryDateNoteStore store = new MemoryDateNoteStore(temporaryFolder.newFolder("notes"));

        assertTrue(store.save("mem_a", "20260802", "청의 호수", 10L));
        assertTrue(store.save("mem_a", "20260803", "오타루 산책", 20L));
        assertTrue(store.save("mem_b", "20260802", "다른 장소", 30L));

        assertEquals("청의 호수", store.get("mem_a", "20260802").text);
        assertEquals(10L, store.get("mem_a", "20260802").createdAtMillis);
        assertTrue(store.save("mem_a", "20260802", "청의 호수 다시", 40L));
        assertEquals(10L, store.get("mem_a", "20260802").createdAtMillis);
        assertEquals(40L, store.get("mem_a", "20260802").updatedAtMillis);
        assertTrue(store.save("mem_a", "20260802", "", 50L));
        assertNull(store.get("mem_a", "20260802"));
        assertEquals("오타루 산책", store.get("mem_a", "20260803").text);
        assertEquals("다른 장소", store.get("mem_b", "20260802").text);
    }

    @Test
    public void invalidDateKeyDoesNotWriteANote() throws Exception {
        MemoryDateNoteStore store = new MemoryDateNoteStore(temporaryFolder.newFolder("invalid"));

        assertFalse(store.save("mem_a", "2026-08-02", "메모", 1L));
        assertNull(store.get("mem_a", "2026-08-02"));
    }

    @Test
    public void corruptNoteFileRestoresBackupBeforeReading() throws Exception {
        File folder = temporaryFolder.newFolder("backup");
        MemoryDateNoteStore store = new MemoryDateNoteStore(folder);
        assertTrue(store.save("mem_a", "20260802", "보존할 메모", 1L));

        File main = new File(folder, "memory_date_notes.json");
        File backup = new File(folder, "memory_date_notes.json.bak");
        Files.copy(main.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.write(main.toPath(), "{not-json".getBytes(StandardCharsets.UTF_8));

        assertEquals("보존할 메모", store.get("mem_a", "20260802").text);
        assertTrue(main.isFile());
    }
}
