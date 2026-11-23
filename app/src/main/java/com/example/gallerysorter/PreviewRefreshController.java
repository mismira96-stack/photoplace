package com.example.gallerysorter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** Read-only preflight; no service, media writes or persistent background job. */
final class PreviewRefreshController {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final PreviewRefreshDispatch dispatch = new PreviewRefreshDispatch();
    private PreviewRefreshGate gate;
    private long startedAt;

    PreviewRefreshController(Context context) { this.context = context.getApplicationContext(); }

    boolean isChecking() { return dispatch.isChecking(); }

    void execute(Executor worker, List<String> sourcePaths, View sourceView, Runnable prepareScan,
                 Runnable fullScan, Runnable unchanged) {
        final List<String> paths = new ArrayList<>(sourcePaths);
        final long requestedAt = SystemClock.elapsedRealtime();
        dispatch.execute(worker, command -> main.postDelayed(command,
                Math.max(0L, 180L - (SystemClock.elapsedRealtime() - requestedAt))), () -> {
            startedAt = SystemClock.elapsedRealtime();
            gate = new PreviewRefreshGate(new AndroidEnvironment(context, paths));
            boolean skip = gate.begin();
            Log.i("PhotoPlaceRefresh", "preflightMs=" + (SystemClock.elapsedRealtime() - startedAt)
                    + " unchanged=" + skip);
            return skip;
        }, prepareScan, fullScan, unchanged, sourceView::isAttachedToWindow);
    }

    void complete(Executor worker, boolean saved, List<?> analyzedItems) {
        final PreviewRefreshGate completedGate = gate;
        final long scanStartedAt = startedAt;
        final int analyzedCount = analyzedItems == null ? -1 : analyzedItems.size();
        worker.execute(() -> {
            if (completedGate != null) completedGate.complete(saved, analyzedCount);
            Log.i("PhotoPlaceRefresh", "completedMs=" + (SystemClock.elapsedRealtime() - scanStartedAt)
                    + " items=" + analyzedCount + " snapshotSaved=" + saved);
        });
    }

    static boolean checkpointMatches(Context context, List<String> sourcePaths) {
        try {
            return new PreviewRefreshGate(new AndroidEnvironment(context, sourcePaths)).matchesCheckpoint();
        } catch (Exception ignored) {
            return false;
        }
    }

    static String snapshotRevision(Context context) {
        File snapshot = new File(context.getFilesDir(), "discovery_snapshot.json");
        return snapshot.isFile() && snapshot.length() > 0
                ? snapshot.lastModified() + ":" + snapshot.length() : "";
    }

    private static final class AndroidEnvironment implements PreviewRefreshGate.Environment {
        private final Context context;
        private final List<String> paths;
        private final SharedPreferences preferences;

        AndroidEnvironment(Context context, List<String> paths) {
            this.context = context;
            this.paths = new ArrayList<>(paths);
            Collections.sort(this.paths);
            preferences = context.getSharedPreferences("preview_refresh", Context.MODE_PRIVATE);
        }

        @Override public PreviewRefreshGate.State readState() throws Exception {
            // API 29 retains the normal path. Full photo/video access is required to cache a scan.
            if (Build.VERSION.SDK_INT < 30 || paths.isEmpty() || !hasFullAccess()) return null;
            List<String> volumes = new ArrayList<>(MediaStore.getExternalVolumeNames(context));
            Collections.sort(volumes);
            if (volumes.isEmpty()) return null;
            StringBuilder key = new StringBuilder("incremental=2|analysis=").append(ImageAnalysisCacheSession.POLICY_VERSION);
            key.append("|discovery=").append(DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION)
                    .append("|country=").append(DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);
            key.append("|paths=").append(paths.toString());
            for (String volume : volumes) {
                key.append('|').append(volume).append(':').append(MediaStore.getVersion(context, volume))
                        .append(':').append(MediaStore.getGeneration(context, volume));
            }
            int count = count(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    + count(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            return new PreviewRefreshGate.State(key.toString(), count);
        }

        private boolean hasFullAccess() {
            if (context.checkSelfPermission("android.permission.ACCESS_MEDIA_LOCATION") != PackageManager.PERMISSION_GRANTED) return false;
            if (Build.VERSION.SDK_INT >= 33) {
                return context.checkSelfPermission("android.permission.READ_MEDIA_IMAGES") == PackageManager.PERMISSION_GRANTED
                        && context.checkSelfPermission("android.permission.READ_MEDIA_VIDEO") == PackageManager.PERMISSION_GRANTED;
            }
            return context.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
        }

        private int count(Uri collection) {
            StringBuilder selection = new StringBuilder("relative_path IN (");
            for (int i = 0; i < paths.size(); i++) selection.append(i == 0 ? "?" : ",?");
            selection.append(") AND is_pending=0 AND is_trashed=0");
            try (Cursor cursor = context.getContentResolver().query(collection, new String[]{"_id"},
                    selection.toString(), paths.toArray(new String[0]), null)) {
                if (cursor == null) throw new IllegalStateException("Unknown source inventory");
                return cursor.getCount();
            }
        }

        @Override public String snapshotRevision() {
            return PreviewRefreshController.snapshotRevision(context);
        }

        @Override public String readCheckpoint() { return preferences.getString("checkpoint", ""); }
        @Override public void writeCheckpoint(String value) {
            if (!preferences.edit().putString("checkpoint", value).commit()) {
                throw new IllegalStateException("Checkpoint could not be saved");
            }
        }
    }
}
