package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MemoryMediaSourcePolicyTest {
    @Test
    public void exactGalleryOutputWinsWithoutCombiningDiscoveryRefs() {
        assertEquals(MemoryMediaSourcePolicy.Source.GALLERY_OUTPUT,
                MemoryMediaSourcePolicy.choose(memoryLink(OrganizationLink.Status.SUCCESS),
                        MemoryMediaSourcePolicy.GalleryLookup.FOUND, true));
    }

    @Test
    public void missingExactOutputFallsBackToLiveDiscovery() {
        assertEquals(MemoryMediaSourcePolicy.Source.DISCOVERY,
                MemoryMediaSourcePolicy.choose(memoryLink(OrganizationLink.Status.PARTIAL),
                        MemoryMediaSourcePolicy.GalleryLookup.MISSING, true));
        assertEquals(MemoryMediaSourcePolicy.Source.EMPTY,
                MemoryMediaSourcePolicy.choose(memoryLink(OrganizationLink.Status.SUCCESS),
                        MemoryMediaSourcePolicy.GalleryLookup.MISSING, false));
    }

    @Test
    public void failedGalleryLookupDoesNotPretendTheOutputIsMissing() {
        assertEquals(MemoryMediaSourcePolicy.Source.UNAVAILABLE,
                MemoryMediaSourcePolicy.choose(memoryLink(OrganizationLink.Status.SUCCESS),
                        MemoryMediaSourcePolicy.GalleryLookup.FAILED, true));
        assertEquals(MemoryMediaSourcePolicy.Source.UNAVAILABLE,
                MemoryMediaSourcePolicy.choose(memoryLink(OrganizationLink.Status.SUCCESS),
                        null, true));
        assertEquals(MemoryMediaSourcePolicy.Source.UNAVAILABLE,
                MemoryMediaSourcePolicy.choose(memoryLink(OrganizationLink.Status.SUCCESS),
                        MemoryMediaSourcePolicy.GalleryLookup.UNKNOWN, true));
    }

    @Test
    public void absentOrUnusableMemoryLinkUsesDiscoveryOnly() {
        assertEquals(MemoryMediaSourcePolicy.Source.DISCOVERY,
                MemoryMediaSourcePolicy.choose(null,
                        MemoryMediaSourcePolicy.GalleryLookup.FOUND, true));
        assertEquals(MemoryMediaSourcePolicy.Source.DISCOVERY,
                MemoryMediaSourcePolicy.choose(memoryLink(OrganizationLink.Status.MISSING),
                        MemoryMediaSourcePolicy.GalleryLookup.MISSING, true));
        assertEquals(MemoryMediaSourcePolicy.Source.DISCOVERY,
                MemoryMediaSourcePolicy.choose(collectionLink(),
                        MemoryMediaSourcePolicy.GalleryLookup.FOUND, true));
        assertEquals(MemoryMediaSourcePolicy.Source.DISCOVERY,
                MemoryMediaSourcePolicy.choose(invalidMemoryLink(),
                        MemoryMediaSourcePolicy.GalleryLookup.FOUND, true));
        assertEquals(MemoryMediaSourcePolicy.Source.EMPTY,
                MemoryMediaSourcePolicy.choose(null,
                        MemoryMediaSourcePolicy.GalleryLookup.MISSING, false));
    }

    private static OrganizationLink memoryLink(OrganizationLink.Status status) {
        return new OrganizationLink("link-1", OrganizationLink.SubjectType.MEMORY,
                "mem_place", "request-1", "Place", "Pictures/Place/",
                100L, status, 2, 0, 0);
    }

    private static OrganizationLink collectionLink() {
        return new OrganizationLink("link-collection", OrganizationLink.SubjectType.COLLECTION,
                "group_trip", "request-collection", "Trip", "Pictures/Trip/",
                100L, OrganizationLink.Status.SUCCESS, 2, 0, 0);
    }

    private static OrganizationLink invalidMemoryLink() {
        return new OrganizationLink("link-invalid", OrganizationLink.SubjectType.MEMORY,
                "", "request-invalid", "Place", "Pictures/Place/",
                100L, OrganizationLink.Status.SUCCESS, 2, 0, 0);
    }
}
