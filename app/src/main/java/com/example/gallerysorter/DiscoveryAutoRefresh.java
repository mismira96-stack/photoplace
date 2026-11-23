package com.example.gallerysorter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** A short foreground check on return, using the same worker as manual preview. */
final class DiscoveryAutoRefresh {
    private static final long BUSY_RETRY_DELAY_MS = 500L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Executor worker;
    private final Supplier<List<String>> selectedPaths;
    private final BooleanSupplier busy;
    private final Supplier<List<AlbumFolder>> albums;
    private final IncrementalMediaScanner.MetadataReader metadata;
    private final Supplier<DiscoverySnapshotController> snapshots;
    private final Runnable refreshed;
    private final Context context;
    private final HomeRevisionReader access;
    private final DiscoverySourceRevision revisions;
    private final SharedPreferences state;

    private final Runnable check = this::check;
    private final BusyRetry busyRetry = new BusyRetry();
    private volatile int request;
    private boolean resumed;
    private boolean checking;
    private boolean cameraSettleCheck;

    DiscoveryAutoRefresh(Context context, Executor worker, Supplier<List<String>> selectedPaths,
                         BooleanSupplier busy, Supplier<List<AlbumFolder>> albums,
                         IncrementalMediaScanner.MetadataReader metadata,
                         Supplier<DiscoverySnapshotController> snapshots, Runnable refreshed) {
        this.context = context.getApplicationContext();
        this.worker = worker;
        this.selectedPaths = selectedPaths;
        this.busy = busy;
        this.albums = albums;
        this.metadata = metadata;
        this.snapshots = snapshots;
        this.refreshed = refreshed;
        access = new HomeRevisionReader(this.context);
        revisions = new DiscoverySourceRevision(this.context);
        state = this.context.getSharedPreferences("discovery_auto_refresh", Context.MODE_PRIVATE);
    }

    void resume() {
        resumed = true;
        cameraSettleCheck = true;
        busyRetry.reset();
        schedule(250L);
    }

    void pause() {
        resumed = false;
        cancel();
    }

    void cancel() {
        request++;
        cameraSettleCheck = false;
        busyRetry.reset();
        main.removeCallbacks(check);
    }

    private void schedule(long delay) {
        main.removeCallbacks(check);
        main.postDelayed(check, delay);
    }

    private void check() {
        if (!resumed || checking || !access.fullAccess()) return;
        if (busyRetry.defer(busy.getAsBoolean(), () -> schedule(BUSY_RETRY_DELAY_MS))) return;
        int current = request;
        List<String> paths = selectedPaths.get();
        List<String> scope = scope(paths);
        if (scope.isEmpty()) return;
        checking = true;
        worker.execute(() -> {
            boolean saved = false;
            BooleanSupplier active = () -> request == current && !Thread.currentThread().isInterrupted();
            try {
                saved = DiscoveryAutoRefreshSession.run(new DiscoveryAutoRefreshSession.Source() {
                    @Override public String revision() {
                        if (!access.fullAccess() || !scope(selectedPaths.get()).equals(scope)) return null;
                        return revisions.read(scope);
                    }

                    @Override public String checkpoint() {
                        String snapshot = PreviewRefreshController.snapshotRevision(context);
                        if (snapshot.isEmpty() || !snapshot.equals(state.getString("snapshot", ""))) return "";
                        return state.getString("source", "");
                    }

                    @Override public boolean manualSnapshotIsCurrent() {
                        return PreviewRefreshController.checkpointMatches(context, paths);
                    }

                    @Override public void saveCheckpoint(String revision) {
                        String snapshot = PreviewRefreshController.snapshotRevision(context);
                        if (!snapshot.isEmpty()) {
                            state.edit().putString("source", revision).putString("snapshot", snapshot).apply();
                        }
                    }
                }, () -> {
                    List<PhotoItem> items = new IncrementalMediaScanner(context, new MediaAnalysisStore(context),
                            metadata, null, () -> !active.getAsBoolean()).scan(albums.get(), scope);
                    return () -> snapshots.get().savePreviewItems(items, "auto:" + items.size());
                }, active);
            } catch (Exception failure) {
                Log.w("PhotoPlaceRefresh", "Automatic discovery check failed", failure);
            } finally {
                boolean updated = saved;
                main.post(() -> {
                    checking = false;
                    if (!resumed) return;
                    if (current != request) {
                        if (cameraSettleCheck) schedule(250L);
                        return;
                    }
                    if (updated && !busy.getAsBoolean()) refreshed.run();
                    if (cameraSettleCheck) {
                        cameraSettleCheck = false;
                        schedule(1500L);
                    }
                });
            }
        });
    }

    private static List<String> scope(List<String> paths) {
        List<String> normalized = ManualPlacePaths.normalizeAll(paths);
        Collections.sort(normalized);
        return normalized;
    }

    static final class BusyRetry {
        private static final int MAX_RETRIES = 20;
        private int scheduledRetries;

        boolean defer(boolean busy, Runnable retry) {
            if (!busy) {
                reset();
                return false;
            }
            if (scheduledRetries < MAX_RETRIES) {
                scheduledRetries++;
                retry.run();
            }
            return true;
        }

        void reset() {
            scheduledRetries = 0;
        }
    }
}
