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
import java.util.List;

public class MemoryOrganizationLinkStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void commitsAndRestoresLinksForMemoryAndCollectionSubjects() throws Exception {
        File folder = temporaryFolder.newFolder("links");
        MemoryOrganizationLinkStore store = new MemoryOrganizationLinkStore(folder);
        OrganizationLink memory = link("link-1", "request-1", "mem_place", "Pictures/Place/",
                OrganizationLink.Status.SUCCESS, 4, 0, 0);
        OrganizationLink collection = new OrganizationLink(
                "link-2", OrganizationLink.SubjectType.COLLECTION, "group_trip", "request-2",
                "2026 Trip", "Pictures/2026 Trip/", 200L,
                OrganizationLink.Status.PARTIAL, 3, 1, 1);

        assertEquals(MemoryOrganizationLinkStore.CommitResult.ADDED, store.commit(memory));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.ADDED, store.commit(collection));

        MemoryOrganizationLinkStore restored = new MemoryOrganizationLinkStore(folder);
        assertEquals(2, restored.readAll().size());
        assertEquals("mem_place", restored.findByRequestId("request-1").subjectId);
        assertTrue(restored.hasUsableOutput(OrganizationLink.SubjectType.MEMORY, "mem_place"));
        assertTrue(restored.hasUsableOutput(OrganizationLink.SubjectType.COLLECTION, "group_trip"));
    }

    @Test
    public void requestReplayIsIdempotentAndConflictingPayloadIsRejected() throws Exception {
        MemoryOrganizationLinkStore store = new MemoryOrganizationLinkStore(
                temporaryFolder.newFolder("idempotency"));
        OrganizationLink original = link("link-1", "request-1", "mem_place", "Pictures/Place/",
                OrganizationLink.Status.SUCCESS, 4, 0, 0);
        OrganizationLink replay = link("new-link-id", "request-1", "mem_place", "Pictures/Place/",
                OrganizationLink.Status.SUCCESS, 4, 0, 0);
        OrganizationLink conflict = link("link-3", "request-1", "mem_place", "Pictures/Other/",
                OrganizationLink.Status.SUCCESS, 4, 0, 0);

        assertEquals(MemoryOrganizationLinkStore.CommitResult.ADDED, store.commit(original));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.ALREADY_COMMITTED, store.commit(replay));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.CONFLICT, store.commit(conflict));
        assertEquals(1, store.readAll().size());
    }

    @Test
    public void replayIgnoresCreatedAtAndNormalizesRelativePath() throws Exception {
        MemoryOrganizationLinkStore store = new MemoryOrganizationLinkStore(
                temporaryFolder.newFolder("replay-metadata"));
        OrganizationLink original = link("link-1", "request-1", "mem_place", "Pictures/Place/",
                OrganizationLink.Status.SUCCESS, 4, 0, 0);
        OrganizationLink replay = new OrganizationLink(
                "new-link-id", OrganizationLink.SubjectType.MEMORY, "mem_place", "request-1",
                "Place", "Pictures\\Place", 999L, OrganizationLink.Status.SUCCESS, 4, 0, 0);

        assertEquals(MemoryOrganizationLinkStore.CommitResult.ADDED, store.commit(original));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.ALREADY_COMMITTED, store.commit(replay));
        assertEquals("Pictures/Place/", replay.relativePath);
    }

    @Test
    public void findsLatestUsableOutputAndIgnoresMissingHistory() throws Exception {
        MemoryOrganizationLinkStore store = new MemoryOrganizationLinkStore(
                temporaryFolder.newFolder("latest-output"));
        OrganizationLink oldOutput = link("link-1", "request-1", "mem_place", "Pictures/Old/",
                OrganizationLink.Status.SUCCESS, 4, 0, 0);
        OrganizationLink newerMissing = new OrganizationLink(
                "link-2", OrganizationLink.SubjectType.MEMORY, "mem_place", "request-2",
                "Missing", "Pictures/Missing/", 300L, OrganizationLink.Status.MISSING, 2, 0, 0);
        OrganizationLink latestOutput = new OrganizationLink(
                "link-3", OrganizationLink.SubjectType.MEMORY, "mem_place", "request-3",
                "Latest", "Pictures/Latest/", 200L, OrganizationLink.Status.PARTIAL, 2, 0, 1);
        store.commit(oldOutput);
        store.commit(newerMissing);
        store.commit(latestOutput);

        assertEquals("link-3", store.findLatestUsableLink(
                OrganizationLink.SubjectType.MEMORY, "mem_place").linkId);
        assertEquals("link-3", store.findByRequestId("request-3").linkId);
        assertNull(store.findByRequestId("unknown"));
        assertNull(store.findLatestUsableLink(
                OrganizationLink.SubjectType.MEMORY, "mem_unknown"));
    }

    @Test
    public void rejectsInvalidCandidatesAndLinkIdConflicts() throws Exception {
        MemoryOrganizationLinkStore store = new MemoryOrganizationLinkStore(
                temporaryFolder.newFolder("validation"));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.INVALID, store.commit(null));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.ADDED,
                store.commit(link("link-1", "request-1", "mem_place", "Pictures/Place/",
                        OrganizationLink.Status.SUCCESS, 4, 0, 0)));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.CONFLICT,
                store.commit(link("link-1", "request-2", "mem_place", "Pictures/Other/",
                        OrganizationLink.Status.SUCCESS, 1, 0, 0)));
    }

    @Test
    public void missingOutputStaysMissingWhenOriginalResultIsReplayed() throws Exception {
        MemoryOrganizationLinkStore store = new MemoryOrganizationLinkStore(
                temporaryFolder.newFolder("missing"));
        OrganizationLink success = link("link-1", "request-1", "mem_place", "Pictures/Place/",
                OrganizationLink.Status.SUCCESS, 4, 0, 0);
        assertEquals(MemoryOrganizationLinkStore.CommitResult.ADDED, store.commit(success));

        assertTrue(store.markMissing("link-1"));
        assertTrue(store.markMissing("link-1"));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.ALREADY_COMMITTED, store.commit(success));
        assertFalse(store.hasUsableOutput(OrganizationLink.SubjectType.MEMORY, "mem_place"));
        assertEquals(OrganizationLink.Status.MISSING, store.readAll().get(0).status);
    }

    @Test
    public void preservesLinkHistoryForRepeatedOrganizationOfOneMemory() throws Exception {
        MemoryOrganizationLinkStore store = new MemoryOrganizationLinkStore(
                temporaryFolder.newFolder("history"));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.ADDED,
                store.commit(link("link-1", "request-1", "mem_place", "Pictures/Place/",
                        OrganizationLink.Status.MISSING, 4, 0, 0)));
        assertEquals(MemoryOrganizationLinkStore.CommitResult.ADDED,
                store.commit(link("link-2", "request-2", "mem_place", "Pictures/New Place/",
                        OrganizationLink.Status.SUCCESS, 2, 0, 0)));

        List<OrganizationLink> links = store.linksForSubject(
                OrganizationLink.SubjectType.MEMORY, "mem_place");
        assertEquals(2, links.size());
        assertTrue(store.hasUsableOutput(OrganizationLink.SubjectType.MEMORY, "mem_place"));
    }

    @Test
    public void restoresBackupAndRefusesToOverwriteUnrecoverableData() throws Exception {
        File folder = temporaryFolder.newFolder("recovery");
        MemoryOrganizationLinkStore store = new MemoryOrganizationLinkStore(folder);
        OrganizationLink original = link("link-1", "request-1", "mem_place", "Pictures/Place/",
                OrganizationLink.Status.SUCCESS, 4, 0, 0);
        assertEquals(MemoryOrganizationLinkStore.CommitResult.ADDED, store.commit(original));

        File main = new File(folder, "memory_organization_links.json");
        File backup = new File(folder, "memory_organization_links.json.bak");
        Files.copy(main.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.write(main.toPath(), "{broken".getBytes(StandardCharsets.UTF_8));
        assertEquals(1, store.readAll().size());

        Files.write(main.toPath(), "{broken".getBytes(StandardCharsets.UTF_8));
        Files.deleteIfExists(backup.toPath());
        String corruptContent = new String(Files.readAllBytes(main.toPath()), StandardCharsets.UTF_8);
        assertEquals(MemoryOrganizationLinkStore.CommitResult.FAILED,
                store.commit(link("link-2", "request-2", "mem_other", "Pictures/Other/",
                        OrganizationLink.Status.SUCCESS, 1, 0, 0)));
        assertEquals(corruptContent,
                new String(Files.readAllBytes(main.toPath()), StandardCharsets.UTF_8));
    }

    private static OrganizationLink link(String linkId,
                                         String requestId,
                                         String memoryId,
                                         String path,
                                         OrganizationLink.Status status,
                                         int copied,
                                         int skipped,
                                         int failed) {
        String albumName = path.substring("Pictures/".length(), path.length() - 1);
        return new OrganizationLink(
                linkId,
                OrganizationLink.SubjectType.MEMORY,
                memoryId,
                requestId,
                albumName,
                path,
                100L,
                status,
                copied,
                skipped,
                failed);
    }
}
