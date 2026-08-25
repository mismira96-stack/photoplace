package com.example.gallerysorter;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

final class MediaAnalysisStore {
    private static final String FILE_NAME = "media_analysis_cache.json";
    private static final int SCHEMA_VERSION = 1;

    private final File filesDir;

    MediaAnalysisStore(Context context) {
        this(context.getApplicationContext().getFilesDir());
    }

    MediaAnalysisStore(File filesDir) {
        this.filesDir = filesDir;
    }

    synchronized MediaAnalysisEntry get(String signature) {
        return readAll().get(clean(signature));
    }

    synchronized Map<String, MediaAnalysisEntry> readAll() {
        JSONObject root = readRoot();
        LinkedHashMap<String, MediaAnalysisEntry> entries = new LinkedHashMap<>();
        JSONObject values = root.optJSONObject("entries");
        if (values == null) {
            return entries;
        }
        java.util.Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String signature = keys.next();
            MediaAnalysisEntry entry = MediaAnalysisJson.fromJson(signature, values.optJSONObject(signature));
            if (entry != null) {
                entries.put(signature, entry);
            }
        }
        return entries;
    }

    synchronized boolean saveAll(Collection<MediaAnalysisEntry> entries) {
        try {
            if (!canSafelyWrite()) {
                return false;
            }
            JSONObject values = new JSONObject();
            if (entries != null) {
                for (MediaAnalysisEntry entry : entries) {
                    if (entry != null && !entry.signature.isEmpty()) {
                        values.put(entry.signature, MediaAnalysisJson.toJson(entry));
                    }
                }
            }
            JSONObject root = new JSONObject();
            root.put("schemaVersion", SCHEMA_VERSION);
            root.put("entries", values);
            writeRoot(root);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private JSONObject readRoot() {
        JSONObject root = readRootFile(file());
        if (root != null) {
            return root;
        }
        JSONObject backup = readRootFile(backupFile());
        if (backup != null) {
            restoreBackup();
            return backup;
        }
        return new JSONObject();
    }

    private boolean canSafelyWrite() {
        if (!file().exists()) {
            return true;
        }
        if (readRootFile(file()) != null) {
            return true;
        }
        if (readRootFile(backupFile()) == null) {
            return false;
        }
        restoreBackup();
        return readRootFile(file()) != null;
    }

    private JSONObject readRootFile(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        try (FileInputStream input = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
            if (text.length() == 0) {
                return null;
            }
            JSONObject root = new JSONObject(text.toString());
            return root.optInt("schemaVersion", -1) == SCHEMA_VERSION
                    && root.opt("entries") instanceof JSONObject ? root : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeRoot(JSONObject root) throws Exception {
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IllegalStateException("Could not create media analysis directory");
        }
        File target = file();
        File temp = tempFile();
        File backup = backupFile();
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        if (backup.exists() && !backup.delete()) {
            throw new IllegalStateException("Could not clear cache backup");
        }
        boolean hadTarget = target.exists();
        if (hadTarget && !target.renameTo(backup)) {
            throw new IllegalStateException("Could not back up cache");
        }
        if (!temp.renameTo(target)) {
            if (hadTarget) {
                backup.renameTo(target);
            }
            throw new IllegalStateException("Could not write cache");
        }
        if (backup.exists()) {
            backup.delete();
        }
    }

    private void restoreBackup() {
        File backup = backupFile();
        if (!backup.exists()) {
            return;
        }
        File target = file();
        File corrupt = new File(filesDir, FILE_NAME + ".corrupt");
        if (corrupt.exists()) {
            corrupt.delete();
        }
        if (target.exists()) {
            target.renameTo(corrupt);
        }
        if (!backup.renameTo(target) && corrupt.exists()) {
            corrupt.renameTo(target);
        }
    }

    private File file() {
        return new File(filesDir, FILE_NAME);
    }

    private File tempFile() {
        return new File(filesDir, FILE_NAME + ".tmp");
    }

    private File backupFile() {
        return new File(filesDir, FILE_NAME + ".bak");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
