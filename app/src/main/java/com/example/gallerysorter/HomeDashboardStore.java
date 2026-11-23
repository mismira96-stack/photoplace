package com.example.gallerysorter;

import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Small disposable summary, never serialized photo lists or personal notes. */
final class HomeDashboardStore {
    private static final int MAX_BYTES = 65536;
    private final File file;
    HomeDashboardStore(File filesDir) { file = new File(filesDir, "home_dashboard_cache.json"); }
    synchronized HomeDashboardSnapshot read() {
        try {
            if (!file.isFile() || file.length() > MAX_BYTES) return null;
            return HomeDashboardSnapshot.fromJson(new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)));
        } catch (Exception ignored) { return null; }
    }
    synchronized boolean save(HomeDashboardSnapshot snapshot) {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            byte[] data = snapshot.toJson().toString().getBytes(StandardCharsets.UTF_8);
            if (data.length > MAX_BYTES) return false;
            Files.write(temp.toPath(), data);
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception ignored) { return false; }
    }
}
