package com.example.gallerysorter;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Maps observed memory aliases to immutable ids used by user-authored memory data. */
final class MemoryIdentityRegistryStore {
    private static final String FILE_NAME = "memory_identity_registry.json";
    private static final int SCHEMA_VERSION = 1;

    private final File filesDir;

    MemoryIdentityRegistryStore(Context context) {
        this(context.getApplicationContext().getFilesDir());
    }

    MemoryIdentityRegistryStore(File filesDir) {
        this.filesDir = filesDir;
    }

    synchronized String findStableId(String alias) {
        String key = clean(alias);
        if (key.isEmpty()) {
            return "";
        }
        return clean(readAliases().optString(key, ""));
    }

    synchronized String resolveOrCreate(String alias) {
        String key = clean(alias);
        if (key.isEmpty()) {
            return "";
        }
        JSONObject root = readWritableRoot();
        if (root == null) {
            return "";
        }
        JSONObject aliases = aliasesFrom(root);
        String existing = clean(aliases.optString(key, ""));
        if (!existing.isEmpty()) {
            return existing;
        }
        String stableId = "mem_" + UUID.randomUUID().toString().replace("-", "");
        try {
            aliases.put(key, stableId);
            writeRoot(rootWithAliases(aliases));
            return stableId;
        } catch (Exception ignored) {
            return "";
        }
    }

    synchronized boolean registerAlias(String stableId, String alias) {
        String id = clean(stableId);
        String key = clean(alias);
        if (id.isEmpty() || key.isEmpty()) {
            return false;
        }
        JSONObject root = readWritableRoot();
        if (root == null) {
            return false;
        }
        JSONObject aliases = aliasesFrom(root);
        String existing = clean(aliases.optString(key, ""));
        if (!existing.isEmpty()) {
            return existing.equals(id);
        }
        try {
            aliases.put(key, id);
            writeRoot(rootWithAliases(aliases));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private JSONObject readAliases() {
        return aliasesFrom(readRoot());
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
                    && root.opt("aliases") instanceof JSONObject ? root : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private JSONObject aliasesFrom(JSONObject root) {
        JSONObject aliases = root == null ? null : root.optJSONObject("aliases");
        return aliases == null ? new JSONObject() : aliases;
    }

    private JSONObject rootWithAliases(JSONObject aliases) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("aliases", aliases == null ? new JSONObject() : aliases);
        return root;
    }

    private void writeRoot(JSONObject root) throws Exception {
        if (!canSafelyWrite()) {
            throw new IllegalStateException("Could not safely write " + FILE_NAME);
        }
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IllegalStateException("Could not create memory identity directory");
        }
        File target = file();
        File temp = tempFile();
        File backup = backupFile();
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        if (backup.exists() && !backup.delete()) {
            throw new IllegalStateException("Could not clear identity backup");
        }
        boolean hadTarget = target.exists();
        if (hadTarget && !target.renameTo(backup)) {
            throw new IllegalStateException("Could not back up identity registry");
        }
        if (!temp.renameTo(target)) {
            if (hadTarget) {
                backup.renameTo(target);
            }
            throw new IllegalStateException("Could not write identity registry");
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
