package com.example.gallerysorter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OrganizationMediaPolicyTest {
    @Test
    public void memoryOrganizationNeverMovesSourceVideosYetBulkHonorsPreference() {
        OrganizationRequest memoryRequest = new OrganizationRequest(
                "request-1", OrganizationLink.SubjectType.MEMORY,
                "mem_place", "Place", "Pictures/Place/");

        assertFalse(OrganizationMediaPolicy.shouldMoveVideos(memoryRequest, true));
        assertFalse(OrganizationMediaPolicy.shouldMoveVideos(memoryRequest, false));
        assertTrue(OrganizationMediaPolicy.shouldMoveVideos(null, true));
        assertFalse(OrganizationMediaPolicy.shouldMoveVideos(null, false));
    }
}
