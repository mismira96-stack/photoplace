package com.example.gallerysorter;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DiscoveryAutoRefreshSessionTest {
    @Test public void busyCheckSchedulesRetryAndAllowsCheckWhenWorkFinishes() {
        DiscoveryAutoRefresh.BusyRetry retry = new DiscoveryAutoRefresh.BusyRetry();
        AtomicInteger scheduled = new AtomicInteger();

        assertTrue(retry.defer(true, scheduled::incrementAndGet));
        assertEquals(1, scheduled.get());
        assertFalse(retry.defer(false, scheduled::incrementAndGet));
        assertEquals(1, scheduled.get());
    }

    @Test public void busyRetryStopsSchedulingAndResetsForNextResume() {
        DiscoveryAutoRefresh.BusyRetry retry = new DiscoveryAutoRefresh.BusyRetry();
        AtomicInteger scheduled = new AtomicInteger();

        for (int check = 0; check < 25; check++) {
            assertTrue(retry.defer(true, scheduled::incrementAndGet));
        }
        assertEquals(20, scheduled.get());

        retry.reset();
        assertTrue(retry.defer(true, scheduled::incrementAndGet));
        assertEquals(21, scheduled.get());
    }

    @Test public void unchangedOrUnavailableSourceSkipsScan() throws Exception {
        Source source = new Source();
        source.checkpoint = "before";
        assertFalse(DiscoveryAutoRefreshSession.run(source, this::unexpectedScan, () -> true));
        source.revision = null;
        assertFalse(DiscoveryAutoRefreshSession.run(source, this::unexpectedScan, () -> true));
    }

    @Test public void currentManualSnapshotSuppliesInitialCheckpointWithoutScan() throws Exception {
        Source source = new Source();
        source.manualCurrent = true;
        assertFalse(DiscoveryAutoRefreshSession.run(source, this::unexpectedScan, () -> true));
        assertEquals("before", source.checkpoint);
    }

    @Test public void newInventoryPublishesOnceAndThenSkips() throws Exception {
        Source source = new Source();
        assertTrue(DiscoveryAutoRefreshSession.run(source, () -> () -> true, () -> true));
        assertEquals("before", source.checkpoint);
        assertFalse(DiscoveryAutoRefreshSession.run(source, this::unexpectedScan, () -> true));
    }

    @Test public void changesWhileScanningPreventPublishingAndCanRetry() throws Exception {
        Source source = new Source();
        assertFalse(DiscoveryAutoRefreshSession.run(source, () -> {
            source.revision = "later";
            return () -> { throw new AssertionError("An unstable scan must not publish"); };
        }, () -> true));
        assertEquals("", source.checkpoint);
        assertTrue(DiscoveryAutoRefreshSession.run(source, () -> () -> true, () -> true));
        assertEquals("later", source.checkpoint);
    }

    @Test public void leavingForegroundOrFailedSaveDoesNotAdvanceCheckpoint() throws Exception {
        Source source = new Source();
        boolean[] active = {true};
        assertFalse(DiscoveryAutoRefreshSession.run(source, () -> {
            active[0] = false;
            return () -> { throw new AssertionError("A cancelled scan must not publish"); };
        }, () -> active[0]));
        assertFalse(DiscoveryAutoRefreshSession.run(source, () -> () -> false, () -> true));
        assertEquals("", source.checkpoint);
    }

    private DiscoveryAutoRefreshSession.Save unexpectedScan() {
        throw new AssertionError("The scan should have been skipped");
    }

    private static final class Source implements DiscoveryAutoRefreshSession.Source {
        String revision = "before";
        String checkpoint = "";
        boolean manualCurrent;

        @Override public String revision() { return revision; }
        @Override public String checkpoint() { return checkpoint; }
        @Override public boolean manualSnapshotIsCurrent() { return manualCurrent; }
        @Override public void saveCheckpoint(String value) { checkpoint = value; }
    }
}
