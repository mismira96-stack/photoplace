package com.example.gallerysorter;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MemoryPersonalizationStore {
    private static final String FILE_NAME = "memory_personalization.json";
    private static final String PREFS_NAME = "album_sorter";
    private static final String PREF_ALBUM_ALIAS_PREFIX = "album_alias_";
    private static final String PREF_ALBUM_MEMORY_PREFIX = "album_memory_";
    private static final int SCHEMA_VERSION = 1;

    private final Context context;
    private final SharedPreferences legacyPreferences;

    MemoryPersonalizationStore(Context context) {
        this.context = context.getApplicationContext();
        this.legacyPreferences = this.context.getSharedPreferences(PREFS_NAME, 0);
    }

    synchronized MemoryPersonalization get(StoredAlbumSummary summary) {
        String memoryKey = MemoryPersonalizationKey.forSummary(summary);
        JSONObject memories = readMemories();
        JSONObject json = memories.optJSONObject(memoryKey);
        if (json != null) {
            return MemoryPersonalization.fromJson(memoryKey, json);
        }
        return migrateLegacyMemoIfNeeded(summary, memoryKey, memories);
    }

    synchronized Map<String, MemoryPersonalization> getAllByMemoryKey(List<StoredAlbumSummary> summaries) {
        LinkedHashMap<String, MemoryPersonalization> values = new LinkedHashMap<>();
        if (summaries == null) {
            return values;
        }
        for (StoredAlbumSummary summary : summaries) {
            MemoryPersonalization personalization = get(summary);
            values.put(personalization.memoryKey, personalization);
        }
        return values;
    }

    synchronized String displayNameFor(StoredAlbumSummary summary) {
        MemoryPersonalization personalization = get(summary);
        return personalization.displayName.isEmpty()
                ? (summary == null ? "" : summary.albumName)
                : personalization.displayName;
    }

    synchronized String memoFor(StoredAlbumSummary summary) {
        return get(summary).memo;
    }

    synchronized String coverUriFor(StoredAlbumSummary summary) {
        MemoryPersonalization personalization = get(summary);
        return personalization.userCoverUri.isEmpty()
                ? (summary == null ? "" : clean(summary.thumbnailUri))
                : personalization.userCoverUri;
    }

    synchronized void saveDisplayName(StoredAlbumSummary summary, String displayName) {
        update(summary, personalization ->
                personalization.withDisplayName(displayName, System.currentTimeMillis()), false);
    }

    synchronized void saveMemo(StoredAlbumSummary summary, String memo) {
        update(summary, personalization ->
                personalization.withMemo(memo, System.currentTimeMillis()), true);
    }

    synchronized void saveUserCoverUri(StoredAlbumSummary summary, String uri) {
        update(summary, personalization ->
                personalization.withUserCoverUri(uri, System.currentTimeMillis()), false);
    }

    synchronized void clear(StoredAlbumSummary summary) {
        String memoryKey = MemoryPersonalizationKey.forSummary(summary);
        JSONObject root = readWritableRoot();
        if (root == null) {
            return;
        }
        JSONObject memories = root.optJSONObject("memories");
        if (memories == null) {
            memories = new JSONObject();
        }
        memories.remove(memoryKey);
        try {
            writeRoot(rootWithMemories(memories));
            removeLegacyMemoKeys(summary);
        } catch (Exception unused) {
        }
    }

    private void update(StoredAlbumSummary summary, Updater updater, boolean clearLegacyAfterSuccess) {
        String memoryKey = MemoryPersonalizationKey.forSummary(summary);
        JSONObject root = readWritableRoot();
        if (root == null) {
            return;
        }
        JSONObject memories = root.optJSONObject("memories");
        if (memories == null) {
            memories = new JSONObject();
        }
        MemoryPersonalization current = personalizationForUpdate(
                memoryKey,
                MemoryPersonalization.fromJson(memoryKey, memories.optJSONObject(memoryKey)),
                memories.optJSONObject(memoryKey) != null,
                legacyMemoFor(summary));
        MemoryPersonalization updated = updater.update(current);
        try {
            if (updated.isEmpty()) {
                memories.remove(memoryKey);
            } else {
                memories.put(memoryKey, updated.toJson());
            }
            writeRoot(rootWithMemories(memories));
            if (clearLegacyAfterSuccess) {
                removeLegacyMemoKeys(summary);
            }
        } catch (Exception unused) {
        }
    }

    private MemoryPersonalization migrateLegacyMemoIfNeeded(StoredAlbumSummary summary, String memoryKey,
                                                           JSONObject memories) {
        String legacy = legacyMemoFor(summary);
        if (legacy.isEmpty()) {
            return MemoryPersonalization.empty(memoryKey);
        }
        MemoryPersonalization migrated = MemoryPersonalization.empty(memoryKey)
                .withMemo(legacy, System.currentTimeMillis());
        try {
            if (readWritableRoot() == null) {
                return migrated;
            }
            memories.put(memoryKey, migrated.toJson());
            writeRoot(rootWithMemories(memories));
            removeLegacyMemoKeys(summary);
        } catch (Exception unused) {
        }
        return migrated;
    }

    private void removeLegacyMemoKeys(StoredAlbumSummary summary) {
        legacyPreferences.edit()
                .remove(MemoryPersonalizationKey.legacyPreferenceKey(PREF_ALBUM_MEMORY_PREFIX, summary))
                .remove(MemoryPersonalizationKey.legacyPreferenceKey(PREF_ALBUM_ALIAS_PREFIX, summary))
                .apply();
    }

    private JSONObject readMemories() {
        JSONObject memories = readRoot().optJSONObject("memories");
        return memories == null ? new JSONObject() : memories;
    }

    private JSONObject readWritableRoot() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            return new JSONObject();
        }
        return readRootFile(file, false);
    }

    private JSONObject readRoot() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        JSONObject root = readRootFile(file, true);
        return root == null ? new JSONObject() : root;
    }

    private JSONObject readRootFile(File file, boolean emptyOnFailure) {
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
        return emptyOnFailure ? new JSONObject() : null;
    }

    private JSONObject rootWithMemories(JSONObject memories) throws Exception {
        JSONObject root = new JSONObject();
        long now = System.currentTimeMillis();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("updatedAtMillis", now);
        root.put("memories", memories == null ? new JSONObject() : memories);
        return root;
    }

    private void writeRoot(JSONObject root) throws Exception {
        File target = new File(context.getFilesDir(), FILE_NAME);
        File temp = new File(context.getFilesDir(), FILE_NAME + ".tmp");
        File backup = new File(context.getFilesDir(), FILE_NAME + ".bak");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        if (backup.exists() && !backup.delete()) {
            throw new IllegalStateException("Could not clear backup " + FILE_NAME);
        }
        boolean hadTarget = target.exists();
        if (hadTarget && !target.renameTo(backup)) {
            throw new IllegalStateException("Could not back up " + FILE_NAME);
        }
        if (!temp.renameTo(target)) {
            if (hadTarget) {
                backup.renameTo(target);
            }
            throw new IllegalStateException("Could not write " + FILE_NAME);
        }
        if (backup.exists()) {
            backup.delete();
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static MemoryPersonalization personalizationForUpdate(String memoryKey,
                                                          MemoryPersonalization existing,
                                                          boolean hasExistingRecord,
                                                          String legacyMemo) {
        if (hasExistingRecord) {
            return existing == null ? MemoryPersonalization.empty(memoryKey) : existing;
        }
        String memo = clean(legacyMemo);
        return memo.isEmpty()
                ? MemoryPersonalization.empty(memoryKey)
                : MemoryPersonalization.empty(memoryKey).withMemo(memo, System.currentTimeMillis());
    }

    private String legacyMemoFor(StoredAlbumSummary summary) {
        String legacy = clean(legacyPreferences.getString(
                MemoryPersonalizationKey.legacyPreferenceKey(PREF_ALBUM_MEMORY_PREFIX, summary), ""));
        if (legacy.isEmpty()) {
            legacy = clean(legacyPreferences.getString(
                    MemoryPersonalizationKey.legacyPreferenceKey(PREF_ALBUM_ALIAS_PREFIX, summary), ""));
        }
        return legacy;
    }

    private interface Updater {
        MemoryPersonalization update(MemoryPersonalization personalization);
    }
}
