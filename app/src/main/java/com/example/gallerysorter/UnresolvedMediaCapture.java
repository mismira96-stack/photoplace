package com.example.gallerysorter;

import android.content.Context;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Observes existing inventory rows; adds no MediaStore scan and no per-photo database writes. */
final class UnresolvedMediaCapture {
    private final Context context;
    private final List<UnresolvedMediaRecord> rows = new ArrayList<>();
    private final Map<String, String> versions = new HashMap<>();
    private boolean incomplete;
    private boolean finished;

    UnresolvedMediaCapture(Context context) { this.context = context.getApplicationContext(); }
    void begin() { ManualInventoryStatus.write(context,"RUNNING"); }
    void close() { if (!finished) ManualInventoryStatus.write(context,"INTERRUPTED"); }

    void observe(PhotoItem item, String volume, String path, long id, long size, long modified) {
        if (!item.noLocation || incomplete) return;
        try {
            String version = versions.get(volume);
            if (version == null) {
                version = Build.VERSION.SDK_INT >= 30 ? MediaStore.getVersion(context, volume)
                        : MediaStore.getVersion(context);
                versions.put(volume, version);
            }
            rows.add(new UnresolvedMediaRecord(item.uri.toString(), volume, version, id,
                    item.video ? "VIDEO" : "PHOTO", item.name, item.mimeType, path,
                    size, modified, item.takenAt == null ? 0 : item.takenAt.getTime()));
        } catch (Exception error) {
            incomplete = true;
            rows.clear();
            Log.w("ManualPlaceInventory", "Incomplete inventory identity; prior snapshot retained", error);
        }
    }

    void finish(List<String> scannedPaths) {
        finished=true;
        if (incomplete) { ManualInventoryStatus.write(context,"FAILED"); return; }
        try {
            new ManualPlaceStore(context).reconcileInventory(scannedPaths, rows);
            if (!scannedPaths.isEmpty()) ManualInventoryStatus.write(context,"SUCCEEDED");
            else ManualInventoryStatus.write(context,"UNCAPTURED");
        } catch (Exception error) {
            ManualInventoryStatus.write(context,"FAILED");
            // Supplemental inventory failure must not retry/reset a successful normal analysis.
            Log.w("ManualPlaceInventory", "Inventory save failed; normal scan preserved", error);
        }
    }
}
