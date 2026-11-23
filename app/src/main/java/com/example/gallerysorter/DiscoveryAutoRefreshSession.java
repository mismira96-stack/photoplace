package com.example.gallerysorter;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Persist only scans that completed for the same selected media and permissions. */
final class DiscoveryAutoRefreshSession {
    interface Source {
        String revision();
        String checkpoint();
        boolean manualSnapshotIsCurrent();
        void saveCheckpoint(String revision);
    }

    interface Scan {
        Save scan() throws Exception;
    }

    interface Save {
        boolean snapshot() throws Exception;
    }

    static boolean run(Source source, Scan scan, BooleanSupplier active) throws Exception {
        if (!active.getAsBoolean()) return false;
        String initial = source.revision();
        if (initial == null || !active.getAsBoolean() || initial.equals(source.checkpoint())) return false;

        if (source.manualSnapshotIsCurrent()) {
            if (stable(source, initial, active)) source.saveCheckpoint(initial);
            return false;
        }

        Save result = scan.scan();
        if (!stable(source, initial, active) || !result.snapshot()) return false;
        source.saveCheckpoint(initial);
        return true;
    }

    private static boolean stable(Source source, String initial, BooleanSupplier active) {
        return active.getAsBoolean() && Objects.equals(initial, source.revision()) && active.getAsBoolean();
    }
}
