package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class PreviewRefreshGateTest {
    private static final class Fake implements PreviewRefreshGate.Environment {
        PreviewRefreshGate.State state = new PreviewRefreshGate.State("volume-v1-generation10-pathCamera", 12);
        String revision = "snapshot1";
        String checkpoint = "";
        boolean fail;
        @Override public PreviewRefreshGate.State readState() throws Exception {
            if (fail) throw new Exception("provider unavailable");
            return state;
        }
        @Override public String snapshotRevision() { return revision; }
        @Override public String readCheckpoint() { return checkpoint; }
        @Override public void writeCheckpoint(String value) { checkpoint = value; }
    }

    @Test public void firstRunScansAndSuccessfulStableRepeatSkipsAcrossInstances() {
        Fake env = new Fake();
        PreviewRefreshGate gate = new PreviewRefreshGate(env);
        assertFalse(gate.begin());
        env.revision = "snapshot2";
        gate.complete(true, 12);
        assertTrue(new PreviewRefreshGate(env).begin());
    }

    @Test public void addedModifiedDeletedMediaOrChangedFoldersForceScan() {
        for (String key : new String[]{"generation11-added", "generation11-modified",
                "generation11-deleted", "version-reset", "different-folder", "policy2"}) {
            Fake env = baseline();
            env.state = new PreviewRefreshGate.State(key, 12);
            assertFalse(new PreviewRefreshGate(env).begin());
        }
    }

    @Test public void missingReplacedOrChangedSnapshotForcesScan() {
        for (String revision : new String[]{"", "snapshot-changed"}) {
            Fake env = baseline();
            env.revision = revision;
            assertFalse(new PreviewRefreshGate(env).begin());
        }
    }

    @Test public void failedSaveAndPartialScanCannotEstablishCheckpoint() {
        Fake env = new Fake();
        PreviewRefreshGate gate = new PreviewRefreshGate(env);
        assertFalse(gate.begin());
        gate.complete(false, 12);
        assertFalse(new PreviewRefreshGate(env).begin());
        assertFalse(gate.begin());
        gate.complete(true, 11);
        assertFalse(new PreviewRefreshGate(env).begin());
    }

    @Test public void cancelledScanWithoutCompletionCannotEstablishCheckpoint() {
        Fake env = new Fake();
        assertFalse(new PreviewRefreshGate(env).begin());
        assertFalse(new PreviewRefreshGate(env).begin());
    }

    @Test public void changesDuringScanDoNotBecomeBaseline() {
        Fake env = new Fake();
        PreviewRefreshGate gate = new PreviewRefreshGate(env);
        assertFalse(gate.begin());
        env.state = new PreviewRefreshGate.State("generation11", 12);
        gate.complete(true, 12);
        assertFalse(new PreviewRefreshGate(env).begin());
    }

    @Test public void unknownProviderAndLimitedPermissionAlwaysScan() {
        Fake env = baseline();
        env.fail = true;
        assertFalse(new PreviewRefreshGate(env).begin());
        env.fail = false;
        env.state = null;
        PreviewRefreshGate gate = new PreviewRefreshGate(env);
        assertFalse(gate.begin());
        gate.complete(true, 12);
        assertEquals("", env.checkpoint);
    }

    @Test public void providerFailureAtCompletionDoesNotSave() {
        Fake env = new Fake();
        PreviewRefreshGate gate = new PreviewRefreshGate(env);
        assertFalse(gate.begin());
        env.fail = true;
        gate.complete(true, 12);
        assertEquals("", env.checkpoint);
    }

    private Fake baseline() {
        Fake env = new Fake();
        PreviewRefreshGate gate = new PreviewRefreshGate(env);
        assertFalse(gate.begin());
        gate.complete(true, 12);
        return env;
    }
}
