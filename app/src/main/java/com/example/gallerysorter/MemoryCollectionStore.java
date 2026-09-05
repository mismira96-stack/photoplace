package com.example.gallerysorter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Persists app-only Memory collections without changing media, places, or notes. */
final class MemoryCollectionStore {
    private static final String FILE_NAME = "memory_collections.json";
    private static final int SCHEMA_VERSION = 1;

    private final File filesDir;

    MemoryCollectionStore(Context context) {
        this(context.getApplicationContext().getFilesDir());
    }

    MemoryCollectionStore(File filesDir) {
        this.filesDir = filesDir;
    }

    synchronized List<MemoryCollection> readAll() {
        return collectionsFrom(readRoot());
    }

    synchronized MemoryCollection create(String title,
                                         List<MemoryCollection.Member> members,
                                         long nowMillis) {
        MemoryCollection collection = new MemoryCollection(
                "group_" + UUID.randomUUID().toString().replace("-", ""),
                title,
                members,
                nowMillis,
                nowMillis);
        if (!collection.isValid()) {
            return null;
        }
        JSONObject root = readWritableRoot();
        if (root == null) {
            return null;
        }
        List<MemoryCollection> collections = collectionsFrom(root);
        if (hasMemberConflict(collection, collections)) {
            return null;
        }
        collections.add(collection);
        try {
            writeRoot(rootWithCollections(collections));
            return collection;
        } catch (Exception ignored) {
            return null;
        }
    }

    synchronized boolean rename(String collectionId, String title, long nowMillis) {
        String id = clean(collectionId);
        JSONObject root = readWritableRoot();
        if (id.isEmpty() || root == null) {
            return false;
        }
        List<MemoryCollection> collections = collectionsFrom(root);
        boolean updated = false;
        for (int index = 0; index < collections.size(); index++) {
            MemoryCollection current = collections.get(index);
            if (!id.equals(current.collectionId)) {
                continue;
            }
            MemoryCollection renamed = current.withTitle(title, nowMillis);
            if (!renamed.isValid()) {
                return false;
            }
            collections.set(index, renamed);
            updated = true;
            break;
        }
        if (!updated) {
            return false;
        }
        try {
            writeRoot(rootWithCollections(collections));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    synchronized boolean dissolve(String collectionId) {
        String id = clean(collectionId);
        JSONObject root = readWritableRoot();
        if (id.isEmpty() || root == null) {
            return false;
        }
        List<MemoryCollection> collections = collectionsFrom(root);
        boolean removed = false;
        for (int index = collections.size() - 1; index >= 0; index--) {
            if (id.equals(collections.get(index).collectionId)) {
                collections.remove(index);
                removed = true;
            }
        }
        if (!removed) {
            return false;
        }
        try {
            writeRoot(rootWithCollections(collections));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasMemberConflict(MemoryCollection candidate,
                                      List<MemoryCollection> existingCollections) {
        HashSet<String> existingIds = new HashSet<>();
        if (existingCollections != null) {
            for (MemoryCollection existing : existingCollections) {
                if (existing == null) {
                    continue;
                }
                for (MemoryCollection.Member member : existing.members) {
                    existingIds.add(member.stableMemoryId);
                }
            }
        }
        for (MemoryCollection.Member member : candidate.members) {
            if (existingIds.contains(member.stableMemoryId)) {
                return true;
            }
        }
        return false;
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

    private List<MemoryCollection> collectionsFrom(JSONObject root) {
        JSONArray values = root == null ? null : root.optJSONArray("collections");
        if (values == null || values.length() == 0) {
            return new ArrayList<>();
        }
        ArrayList<MemoryCollection> collections = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        for (int index = 0; index < values.length(); index++) {
            MemoryCollection collection = MemoryCollection.fromJson(values.optJSONObject(index));
            if (collection != null && ids.add(collection.collectionId)) {
                collections.add(collection);
            }
        }
        return collections;
    }

    private JSONObject rootWithCollections(List<MemoryCollection> collections) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", SCHEMA_VERSION);
        JSONArray values = new JSONArray();
        if (collections != null) {
            for (MemoryCollection collection : collections) {
                if (collection != null && collection.isValid()) {
                    values.put(collection.toJson());
                }
            }
        }
        root.put("collections", values);
        return root;
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
                    && root.opt("collections") instanceof JSONArray ? root : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeRoot(JSONObject root) throws Exception {
        if (!canSafelyWrite()) {
            throw new IllegalStateException("Could not safely write " + FILE_NAME);
        }
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IllegalStateException("Could not create memory collection directory");
        }
        File target = file();
        File temp = tempFile();
        File backup = backupFile();
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        if (backup.exists() && !backup.delete()) {
            throw new IllegalStateException("Could not clear collection backup");
        }
        boolean hadTarget = target.exists();
        if (hadTarget && !target.renameTo(backup)) {
            throw new IllegalStateException("Could not back up collections");
        }
        if (!temp.renameTo(target)) {
            if (hadTarget) {
                backup.renameTo(target);
            }
            throw new IllegalStateException("Could not write collections");
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
