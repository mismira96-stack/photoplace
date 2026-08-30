package com.example.gallerysorter;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class MemoryDateNoteStore {
    private static final String FILE_NAME = "memory_date_notes.json";
    private static final int SCHEMA_VERSION = 1;

    private final File filesDir;

    MemoryDateNoteStore(Context context) {
        this(context.getApplicationContext().getFilesDir());
    }

    MemoryDateNoteStore(File filesDir) {
        this.filesDir = filesDir;
    }

    synchronized MemoryDateNote get(String stableMemoryId, String dateKey) {
        String key = MemoryDateNote.key(stableMemoryId, dateKey);
        if (key.isEmpty()) {
            return null;
        }
        return MemoryDateNote.fromJson(clean(stableMemoryId), MemoryDateNote.cleanDateKey(dateKey),
                notesFrom(readRoot()).optJSONObject(key));
    }

    synchronized boolean save(String stableMemoryId, String dateKey, String text, long nowMillis) {
        String id = clean(stableMemoryId);
        String date = MemoryDateNote.cleanDateKey(dateKey);
        String key = MemoryDateNote.key(id, date);
        if (key.isEmpty()) {
            return false;
        }
        JSONObject root = readWritableRoot();
        if (root == null) {
            return false;
        }
        JSONObject notes = notesFrom(root);
        String value = clean(text);
        try {
            if (value.isEmpty()) {
                notes.remove(key);
            } else {
                MemoryDateNote existing = MemoryDateNote.fromJson(id, date, notes.optJSONObject(key));
                long createdAt = existing == null ? Math.max(0L, nowMillis) : existing.createdAtMillis;
                notes.put(key, new MemoryDateNote(id, date, value, createdAt, nowMillis).toJson());
            }
            writeRoot(rootWithNotes(notes));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private JSONObject readWritableRoot() {
        if (!file().exists()) {
            return new JSONObject();
        }
        JSONObject root = readRootFile(file());
        if (root != null) {
            return root;
        }
        JSONObject backup = readRootFile(backupFile());
        if (backup != null) {
            restoreBackup();
            return backup;
        }
        return null;
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

    private JSONObject readRootFile(File candidate) {
        if (candidate == null || !candidate.exists()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        try (FileInputStream input = new FileInputStream(candidate);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
            JSONObject root = new JSONObject(text.toString());
            return root.optInt("schemaVersion", -1) == SCHEMA_VERSION
                    && root.opt("notes") instanceof JSONObject ? root : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private JSONObject notesFrom(JSONObject root) {
        JSONObject notes = root == null ? null : root.optJSONObject("notes");
        return notes == null ? new JSONObject() : notes;
    }

    private JSONObject rootWithNotes(JSONObject notes) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("notes", notes == null ? new JSONObject() : notes);
        return root;
    }

    private void writeRoot(JSONObject root) throws Exception {
        if (!canSafelyWrite()) {
            throw new IllegalStateException("Could not safely write " + FILE_NAME);
        }
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IllegalStateException("Could not create memory note directory");
        }
        File target = file();
        File temp = tempFile();
        File backup = backupFile();
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        if (backup.exists() && !backup.delete()) {
            throw new IllegalStateException("Could not clear note backup");
        }
        boolean hadTarget = target.exists();
        if (hadTarget && !target.renameTo(backup)) {
            throw new IllegalStateException("Could not back up note store");
        }
        if (!temp.renameTo(target)) {
            if (hadTarget) {
                backup.renameTo(target);
            }
            throw new IllegalStateException("Could not write note store");
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

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
