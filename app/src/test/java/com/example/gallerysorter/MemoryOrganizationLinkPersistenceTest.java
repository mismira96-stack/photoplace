package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class MemoryOrganizationLinkPersistenceTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void sameMemoryAndPathIsIdempotentAcrossRequests() throws Exception {
        File root = temporaryFolder.newFolder("same-memory");
        MemoryIdentityRegistryStore identities = new MemoryIdentityRegistryStore(root);
        MemoryOrganizationLinkStore links = new MemoryOrganizationLinkStore(root);
        String memoryId = identities.resolveOrCreate("discovery:place-a");

        assertEquals(MemoryOrganizationLinkPersistence.Result.PERSISTED,
                MemoryOrganizationLinkPersistence.persist(identities, links,
                        link("link-1", "request-1", memoryId, "Pictures/Place/")));
        assertEquals(MemoryOrganizationLinkPersistence.Result.ALREADY_PERSISTED,
                MemoryOrganizationLinkPersistence.persist(identities, links,
                        link("link-2", "request-2", memoryId, "Pictures/Place/")));
        assertEquals(1, links.readAll().size());
    }

    @Test
    public void differentMemoryCannotClaimAnAlreadyLinkedPath() throws Exception {
        File root = temporaryFolder.newFolder("different-memory");
        MemoryIdentityRegistryStore identities = new MemoryIdentityRegistryStore(root);
        MemoryOrganizationLinkStore links = new MemoryOrganizationLinkStore(root);
        String firstMemory = identities.resolveOrCreate("discovery:place-a");
        String secondMemory = identities.resolveOrCreate("discovery:place-b");

        assertEquals(MemoryOrganizationLinkPersistence.Result.PERSISTED,
                MemoryOrganizationLinkPersistence.persist(identities, links,
                        link("link-1", "request-1", firstMemory, "Pictures/Place/")));
        assertEquals(MemoryOrganizationLinkPersistence.Result.CONFLICT,
                MemoryOrganizationLinkPersistence.persist(identities, links,
                        link("link-2", "request-2", secondMemory, "Pictures/Place/")));
        assertEquals(1, links.readAll().size());
        assertEquals(firstMemory, identities.findStableId("path:Pictures/Place/"));
    }

    private static OrganizationLink link(String linkId, String requestId,
                                         String memoryId, String path) {
        return new OrganizationLink(linkId, OrganizationLink.SubjectType.MEMORY,
                memoryId, requestId, "Place", path, 100L,
                OrganizationLink.Status.SUCCESS, 1, 0, 0);
    }
}
