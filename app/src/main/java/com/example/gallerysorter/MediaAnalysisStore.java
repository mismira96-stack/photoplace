package com.example.gallerysorter;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent, file-backed results for unchanged media. This is not a Memory store. */
final class MediaAnalysisStore {
    static final int POLICY_VERSION = 1;
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_ENTRIES = 50000;
    private static final String FILE_NAME = "media_analysis_store.json";

    private final File filesDir;
    private final LinkedHashMap<String, MediaAnalysisRecord> entries = new LinkedHashMap<>();
    private boolean loaded;
    private boolean dirty;

    MediaAnalysisStore(Context context) {
        this(context.getApplicationContext().getFilesDir());
    }

    MediaAnalysisStore(File filesDir) {
        this.filesDir = filesDir;
    }

    synchronized LocationResult cachedResult(String signature, boolean mediaStoreHasCoordinates) {
        ensureLoaded();
        if (signature == null || signature.isEmpty()) {
            return null;
        }
        MediaAnalysisRecord record = entries.get(signature);
        if (record == null || (mediaStoreHasCoordinates && record.isNoLocation())) {
            return null;
        }
        return record.toLocationResult();
    }

    synchronized void remember(String signature, LocationResult result) {
        ensureLoaded();
        MediaAnalysisRecord record = MediaAnalysisRecord.from(result);
        if (signature == null || signature.isEmpty() || record == null) {
            return;
        }
        entries.remove(signature);
        entries.put(signature, record);
        trim();
        dirty = true;
    }

    synchronized boolean flush() {
        ensureLoaded();
        if (!dirty) {
            return true;
        }
        try {
            if (!canSafelyWrite()) {
                return false;
            }
            writeRoot(toJson());
            dirty = false;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        JSONObject root = readRoot();
        JSONObject jsonEntries = root.optJSONObject("entries");
        if (root.optInt("schemaVersion", SCHEMA_VERSION) != SCHEMA_VERSION || jsonEntries == null) {
            return;
        }
        Iterator<String> keys = jsonEntries.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject value = jsonEntries.optJSONObject(key);
            if (value == null) {
                continue;
            }
            MediaAnalysisRecord record = new MediaAnalysisRecord(
                    value.optString("status", ""),
                    value.optLong("takenAtMillis", 0L),
                    value.optString("folderKey", ""),
                    value.optString("countryCode", ""),
                    value.optString("countryName", ""),
                    value.optString("adminArea", ""),
                    value.optString("addressLine", ""));
            if (MediaAnalysisRecord.STATUS_ANALYZED.equals(record.status)
                    || MediaAnalysisRecord.STATUS_NO_LOCATION.equals(record.status)) {
                entries.put(key, record);
            }
        }
        trim();
    }

    private void trim() {
        while (entries.size() > MAX_ENTRIES) {
            Iterator<Map.Entry<String, MediaAnalysisRecord>> iterator = entries.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
            dirty = true;
        }
    }

    private JSONObject toJson() throws Exception {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("policyVersion", POLICY_VERSION);
        JSONObject jsonEntries = new JSONObject();
        for (Map.Entry<String, MediaAnalysisRecord> entry : entries.entrySet()) {
            MediaAnalysisRecord record = entry.getValue();
            JSONObject value = new JSONObject();
            value.put("status", record.status);
            value.put("takenAtMillis", record.takenAtMillis);
            value.put("folderKey", record.folderKey);
            value.put("countryCode", record.countryCode);
            value.put("countryName", record.countryName);
            value.put("adminArea", record.adminArea);
            value.put("addressLine", record.addressLine);
            jsonEntries.put(entry.getKey(), value);
        }
        root.put("entries", jsonEntries);
        return root;
    }

    private JSONObject readRoot() {
        JSONObject main = readRootFile(file());
        if (main != null) {
            return main;
        }
        JSONObject backup = readRootFile(backupFile());
        if (backup != null) {
            restoreBackup();
            return backup;
        }
        return new JSONObject();
    }

    private boolean canSafelyWrite() {
        if (!file().exists() || readRootFile(file()) != null) {
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
        StringBuilder raw = new StringBuilder();
        try (FileInputStream input = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                raw.append(line);
            }
            JSONObject root = new JSONObject(raw.toString());
            return root.optInt("schemaVersion", -1) == SCHEMA_VERSION && root.optJSONObject("entries") != null
                    ? root : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeRoot(JSONObject root) throws Exception {
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IllegalStateException("Could not create media analysis directory");
        }
        try (FileOutputStream output = new FileOutputStream(tempFile())) {
            output.write(root.toString().getBytes(StandardCharsets.UTF_8));
        }
        File target = file();
        File backup = backupFile();
        if (backup.exists() && !backup.delete()) {
            throw new IllegalStateException("Could not clear media analysis backup");
        }
        boolean hadTarget = target.exists();
        if (hadTarget && !target.renameTo(backup)) {
            throw new IllegalStateException("Could not back up media analysis store");
        }
        if (!tempFile().renameTo(target)) {
            if (hadTarget) {
                backup.renameTo(target);
            }
            throw new IllegalStateException("Could not save media analysis store");
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

    private File file() { return new File(filesDir, FILE_NAME); }
    private File tempFile() { return new File(filesDir, FILE_NAME + ".tmp"); }
    private File backupFile() { return new File(filesDir, FILE_NAME + ".bak"); }
}
