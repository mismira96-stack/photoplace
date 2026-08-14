package com.example.gallerysorter;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class DiscoverySnapshotStore {
    private static final String FILE_NAME = "discovery_snapshot.json";

    private final File filesDir;

    DiscoverySnapshotStore(Context context) {
        this(context.getApplicationContext().getFilesDir());
    }

    DiscoverySnapshotStore(File filesDir) {
        this.filesDir = filesDir;
    }

    synchronized DiscoverySnapshot read() {
        return DiscoverySnapshotJson.fromJson(readRoot());
    }

    synchronized boolean save(DiscoverySnapshot snapshot) {
        try {
            if (!canSafelyWrite()) {
                return false;
            }
            writeRoot(DiscoverySnapshotJson.toJson(snapshot));
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    synchronized boolean clear() {
        boolean ok = true;
        ok &= deleteIfExists(file());
        ok &= deleteIfExists(tempFile());
        ok &= deleteIfExists(backupFile());
        return ok;
    }

    private JSONObject readRoot() {
        File target = file();
        JSONObject root = readRootFile(target);
        if (root != null) {
            return root;
        }
        JSONObject backupRoot = readRootFile(backupFile());
        if (backupRoot != null) {
            restoreBackup();
            return backupRoot;
        }
        return new JSONObject();
    }

    private boolean canSafelyWrite() {
        File target = file();
        if (!target.exists()) {
            return true;
        }
        if (readRootFile(target) != null) {
            return true;
        }
        JSONObject backupRoot = readRootFile(backupFile());
        if (backupRoot == null) {
            return false;
        }
        restoreBackup();
        return readRootFile(target) != null;
    }

    private JSONObject readRootFile(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        try (FileInputStream input = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            if (builder.length() > 0) {
                return new JSONObject(builder.toString());
            }
        } catch (Exception unused) {
        }
        return null;
    }

    private void writeRoot(JSONObject root) throws Exception {
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IllegalStateException("Could not create snapshot directory");
        }
        File target = file();
        File temp = tempFile();
        File backup = backupFile();
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        if (backup.exists() && !backup.delete()) {
            throw new IllegalStateException("Could not clear snapshot backup");
        }
        boolean hadTarget = target.exists();
        if (hadTarget && !target.renameTo(backup)) {
            throw new IllegalStateException("Could not back up snapshot");
        }
        if (!temp.renameTo(target)) {
            if (hadTarget) {
                backup.renameTo(target);
            }
            throw new IllegalStateException("Could not write snapshot");
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
        deleteIfExists(corrupt);
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

    private static boolean deleteIfExists(File file) {
        return file == null || !file.exists() || file.delete();
    }
}
