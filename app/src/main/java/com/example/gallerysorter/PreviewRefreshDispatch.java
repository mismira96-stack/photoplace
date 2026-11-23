package com.example.gallerysorter;

import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/** Keep current content visible until preflight confirms that analysis is needed. */
final class PreviewRefreshDispatch {
    private boolean checking;

    boolean isChecking() { return checking; }

    // Called on the UI executor. Coalesce taps while the read-only preflight is pending.
    void execute(Executor worker, Executor ui, BooleanSupplier isUnchanged,
                 Runnable prepareScan, Runnable scan, Runnable unchanged) {
        execute(worker, ui, isUnchanged, prepareScan, scan, unchanged, () -> true);
    }

    void execute(Executor worker, Executor ui, BooleanSupplier isUnchanged,
                 Runnable prepareScan, Runnable scan, Runnable unchanged, BooleanSupplier stillVisible) {
        if (checking) return;
        checking = true;
        worker.execute(() -> {
            boolean skip = isUnchanged.getAsBoolean();
            ui.execute(() -> {
                try {
                    if (!stillVisible.getAsBoolean()) return;
                    if (skip) unchanged.run();
                    else {
                        prepareScan.run();
                        worker.execute(scan);
                    }
                } finally {
                    checking = false;
                }
            });
        });
    }
}
