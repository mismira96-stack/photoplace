package com.example.gallerysorter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CleanupFeatureGateTest {
    @Before public void setUp() {
        CleanupFeatureGate.setEnabledForTesting(false);
    }

    @After public void tearDown() {
        CleanupFeatureGate.setEnabledForTesting(true);
    }

    @Test public void disabledStateBlocksPendingAndRequests() {
        assertFalse(CleanupFeatureGate.isEnabled());
        for (boolean completed : new boolean[]{false, true}) {
            for (int pending : new int[]{-1, 0, 1, 2000, Integer.MAX_VALUE}) {
                assertFalse(CleanupFeatureGate.hasActionablePending(completed, pending));
            }
        }

        List<String> messages = new ArrayList<>();
        CleanupFeatureGate.onLegacyRequest(messages::add);
        CleanupFeatureGate.onLegacyRequest(messages::add);
        assertEquals(2, messages.size());
        assertTrue(messages.get(0).contains("현재 사용할 수 없어요"));
    }

    @Test public void enabledReviewDoesNotReviveUnverifiedLegacyHomeCounts() {
        CleanupFeatureGate.setEnabledForTesting(true);
        assertTrue(CleanupFeatureGate.isEnabled());
        assertFalse(CleanupFeatureGate.hasActionablePending(false, 10));
        assertFalse(CleanupFeatureGate.hasActionablePending(true, 10));
    }

    @Test public void nullActivityReportsCleanNoticeWithoutCrash() {
        CleanupFeatureGate.setEnabledForTesting(true);
        List<String> messages = new ArrayList<>();
        CleanupFeatureGate.onLegacyRequest(messages::add);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("화면을 확인할 수 없어"));

        messages.clear();
        CleanupFeatureGate.onLegacyResult(0, messages::add);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("화면을 확인할 수 없어"));
    }
}
