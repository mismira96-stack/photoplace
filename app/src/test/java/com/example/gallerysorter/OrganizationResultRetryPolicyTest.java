package com.example.gallerysorter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OrganizationResultRetryPolicyTest {
    @Test
    public void allowsRetriesUntilConfiguredAttemptLimit() {
        assertTrue(OrganizationResultRetryPolicy.shouldRetry(1, 3));
        assertTrue(OrganizationResultRetryPolicy.shouldRetry(2, 3));
        assertFalse(OrganizationResultRetryPolicy.shouldRetry(3, 3));
        assertFalse(OrganizationResultRetryPolicy.shouldRetry(1, 0));
    }
}
