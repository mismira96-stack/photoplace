package com.example.gallerysorter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.MediaStore;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/** A lightweight invalidation token: no media rows, EXIF or Gallery folder queries. */
final class HomeRevisionReader {
    private final Context context;
    HomeRevisionReader(Context context) { this.context = context; }
    boolean fullAccess() {
        if (!granted("android.permission.ACCESS_MEDIA_LOCATION")) return false;
        return Build.VERSION.SDK_INT >= 33
                ? granted("android.permission.READ_MEDIA_IMAGES") && granted("android.permission.READ_MEDIA_VIDEO")
                : granted("android.permission.READ_EXTERNAL_STORAGE");
    }
    private boolean granted(String permission) { return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED; }
    String read() {
        if (!fullAccess() || Build.VERSION.SDK_INT < 30) return null;
        try {
            StringBuilder key = new StringBuilder("home-v1|").append(DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION)
                    .append('|').append(DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);
            ArrayList<String> volumes = new ArrayList<>(MediaStore.getExternalVolumeNames(context));
            Collections.sort(volumes);
            if (volumes.isEmpty()) return null;
            for (String volume : volumes) key.append('|').append(volume).append(':')
                    .append(MediaStore.getVersion(context, volume)).append(':').append(MediaStore.getGeneration(context, volume));
            for (String name : new String[]{"discovery_snapshot.json", "album_summary_history.json", "memory_identity_registry.json",
                    "memory_organization_links.json", "memory_personalization.json", "memory_collections.json", "memory_date_notes.json"}) {
                File file = new File(context.getFilesDir(), name);
                key.append('|').append(name).append(':').append(file.lastModified()).append(':').append(file.length());
            }
            key.append("|manual:").append(new ManualPlaceStore(context).revision());
            return key.toString();
        } catch (Exception ignored) { return null; }
    }
}
