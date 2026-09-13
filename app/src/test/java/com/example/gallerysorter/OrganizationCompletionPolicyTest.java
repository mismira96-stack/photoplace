package com.example.gallerysorter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OrganizationCompletionPolicyTest {
    @Test
    public void homeSingleAlbumCanSucceedWithoutClaimingMemoryLink() {
        assertTrue(OrganizationCompletionPolicy.isComplete(3, 0, false, false, false));
        assertFalse(OrganizationCompletionPolicy.shouldWarnAboutMemoryLink(3, false, false));
    }

    @Test
    public void memoryCompletionRequiresPersistedLink() {
        assertTrue(OrganizationCompletionPolicy.isComplete(3, 0, false, true, true));
        assertFalse(OrganizationCompletionPolicy.isComplete(3, 0, false, true, false));
        assertTrue(OrganizationCompletionPolicy.shouldWarnAboutMemoryLink(3, true, false));
    }
}
