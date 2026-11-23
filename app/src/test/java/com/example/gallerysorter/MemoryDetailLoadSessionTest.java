package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryDetailLoadSessionTest {
    @Test public void alternatingLargePlacesReuseBothResolvedLists() throws Exception {
        MemoryDetailLoadSession<Integer> session = new MemoryDetailLoadSession<>(v -> true, 3, 10000, v -> v);
        int[] reads = {0};
        for (int pass = 0; pass < 3; pass++) {
            for (int size : new int[]{2107, 865}) {
                MemoryDetailLoadSession.Result<Integer> result = session.read(session.begin(), "place" + size,
                        () -> "rev", () -> { reads[0]++; return size; });
                assertEquals(pass > 0, result.reused);
                assertEquals(size, result.value.intValue());
            }
        }
        assertEquals(2, reads[0]);
    }

    @Test public void leastRecentlyUsedPlaceIsEvictedAtEntryLimit() throws Exception {
        MemoryDetailLoadSession<String> session = new MemoryDetailLoadSession<>(v -> true, 2, 100, v -> 1);
        for (String key : new String[]{"a", "b", "a", "c"}) {
            session.read(session.begin(), key, () -> "rev", () -> key);
        }
        assertTrue(session.read(session.begin(), "a", () -> "rev", () -> "unexpected").reused);
        assertFalse(session.read(session.begin(), "b", () -> "rev", () -> "reloaded").reused);
    }

    @Test public void referenceBudgetEvictsEvenBeforeEntryLimit() throws Exception {
        MemoryDetailLoadSession<Integer> session = new MemoryDetailLoadSession<>(v -> true, 3, 10000, v -> v);
        session.read(session.begin(), "a", () -> "rev", () -> 6000);
        session.read(session.begin(), "b", () -> "rev", () -> 5000);
        assertTrue(session.read(session.begin(), "b", () -> "rev", () -> 5000).reused);
        assertFalse(session.read(session.begin(), "a", () -> "rev", () -> 6000).reused);
    }

    @Test public void oversizedResultIsDeliveredWithoutCachingOrEvictingSmallerPlace() throws Exception {
        MemoryDetailLoadSession<Integer> session = new MemoryDetailLoadSession<>(v -> true, 3, 10000, v -> v);
        session.read(session.begin(), "small", () -> "rev", () -> 2107);
        for (int attempt = 0; attempt < 2; attempt++) {
            MemoryDetailLoadSession.Result<Integer> result = session.read(session.begin(), "huge",
                    () -> "rev", () -> 10001);
            assertFalse(result.reused);
            assertEquals(10001, result.value.intValue());
        }
        assertTrue(session.read(session.begin(), "small", () -> "rev", () -> 2107).reused);
    }

    @Test public void changedOrUnknownRevisionDropsEveryOldPlaceEvenIfOldTokenReturns() throws Exception {
        for (String revision : new String[]{"new", "", null}) {
            MemoryDetailLoadSession<String> session = new MemoryDetailLoadSession<>(v -> true, 3, 3, v -> 1);
            session.read(session.begin(), "a", () -> "old", () -> "a");
            session.read(session.begin(), "b", () -> "old", () -> "b");
            session.read(session.begin(), "c", () -> revision, () -> "c");
            assertFalse(session.read(session.begin(), "a", () -> "old", () -> "fresh-a").reused);
            assertFalse(session.read(session.begin(), "b", () -> "old", () -> "fresh-b").reused);
        }
    }

    @Test public void clearDuringResolutionPreventsLateCachePublicationForEveryPlace() throws Exception {
        MemoryDetailLoadSession<String> session = new MemoryDetailLoadSession<>(v -> true, 3, 3, v -> 1);
        session.read(session.begin(), "a", () -> "rev", () -> "a");
        assertNull(session.read(session.begin(), "b", () -> "rev", () -> { session.clear(); return "b"; }));
        for (String key : new String[]{"a", "b"}) {
            assertFalse(session.read(session.begin(), key, () -> "rev", () -> "fresh").reused);
        }
    }

    @Test public void stableSamePlaceReentryReusesResolvedDetailButOtherSourceScopeDoesNot() throws Exception {
        MemoryDetailLoadSession<Object> session = new MemoryDetailLoadSession<>();
        Object detail = new Object();
        session.read(session.begin(), "albums|성남", () -> "rev", () -> detail);
        MemoryDetailLoadSession.Result<Object> cached = session.read(session.begin(), "albums|성남",
                () -> "rev", () -> { fail("Repeated full resolution"); return null; });
        assertSame(detail, cached.value);
        assertTrue(cached.reused);
        Object discoveryOnly = new Object();
        assertSame(discoveryOnly, session.read(session.begin(), "discovery|성남", () -> "rev",
                () -> discoveryOnly).value);
    }

    @Test public void changedOrUnknownSourceCannotReuseCachedCountsAndUris() throws Exception {
        for (String revision : new String[]{"changed", "", null}) {
            MemoryDetailLoadSession<String> session = new MemoryDetailLoadSession<>();
            session.read(session.begin(), "성남", () -> "old", () -> "old-detail");
            assertEquals("fresh", session.read(session.begin(), "성남", () -> revision, () -> "fresh").value);
        }
    }

    @Test public void sourceChangeDuringResolutionDoesNotEstablishReusableOrReturnViewRevision() throws Exception {
        MemoryDetailLoadSession<String> session = new MemoryDetailLoadSession<>();
        String[] revision = {"before"};
        MemoryDetailLoadSession.Result<String> unstable = session.read(session.begin(), "성남", () -> revision[0],
                () -> { revision[0] = "after"; return "unstable"; });
        assertEquals("", unstable.revision);
        assertTrue(unstable.sourceChanged);
        assertEquals("fresh", session.read(session.begin(), "성남", () -> "after", () -> "fresh").value);
    }

    @Test public void newTapOrNavigationSkipsQueuedOldWorkAndRejectsInFlightResult() throws Exception {
        MemoryDetailLoadSession<String> session = new MemoryDetailLoadSession<>();
        long old = session.begin();
        session.begin();
        assertNull(session.read(old, "old", () -> "rev", () -> { fail("Old queued query"); return null; }));
        long active = session.begin();
        assertNull(session.read(active, "성남", () -> "rev", () -> { session.cancel(); return "stale"; }));
        assertFalse(session.current(active));
        assertEquals("new", session.read(session.begin(), "성남", () -> "rev", () -> "new").value);
    }

    @Test public void missingAndFailedDetailAreNotCachedAndDoNotBlockRetry() throws Exception {
        MemoryDetailLoadSession<String> session = new MemoryDetailLoadSession<>();
        assertNull(session.read(session.begin(), "성남", () -> "rev", () -> null).value);
        try {
            session.read(session.begin(), "성남", () -> "rev", () -> { throw new Exception("provider failed"); });
            fail();
        } catch (Exception expected) { assertEquals("provider failed", expected.getMessage()); }
        assertEquals("recovered", session.read(session.begin(), "성남", () -> "rev", () -> "recovered").value);
        session.clear();
        assertEquals("after-close", session.read(session.begin(), "성남", () -> "rev", () -> "after-close").value);
    }

    @Test public void unavailableMediaResultCannotBecomeAnUnchangedDetailOrReturnViewBaseline() throws Exception {
        MemoryDetailLoadSession<String> session = new MemoryDetailLoadSession<>(value -> !"unavailable".equals(value));
        MemoryDetailLoadSession.Result<String> unavailable = session.read(session.begin(), "성남", () -> "rev", () -> "unavailable");
        assertEquals("", unavailable.revision);
        assertFalse(unavailable.sourceChanged);
        assertEquals("fresh", session.read(session.begin(), "성남", () -> "rev", () -> "fresh").value);
    }
}
