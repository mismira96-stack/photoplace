package com.example.gallerysorter;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Controller for PhotoPlace data backup and restore.
 * Manages user-created metadata (date notes, personal places, collections, personalization).
 * Keeps MainActivity 100% free of business logic (0-line rule).
 */
public final class SettingsBackupController {

    public static class BackupSummary {
        public final int notesCount;
        public final int placesCount;
        public final int collectionsCount;
        public final int personalizationCount;
        public final String jsonString;

        public BackupSummary(int notes, int places, int collections, int personalization, String json) {
            this.notesCount = notes;
            this.placesCount = places;
            this.collectionsCount = collections;
            this.personalizationCount = personalization;
            this.jsonString = json;
        }

        public int totalItems() {
            return notesCount + placesCount + collectionsCount + personalizationCount;
        }
    }

    public static class RestoreResult {
        public final boolean success;
        public final int notesCount;
        public final int placesCount;
        public final int collectionsCount;
        public final String message;

        public RestoreResult(boolean success, int notes, int places, int collections, String message) {
            this.success = success;
            this.notesCount = notes;
            this.placesCount = places;
            this.collectionsCount = collections;
            this.message = message;
        }

        public static RestoreResult fail(String msg) {
            return new RestoreResult(false, 0, 0, 0, msg);
        }

        public static RestoreResult ok(int notes, int places, int collections, String msg) {
            return new RestoreResult(true, notes, places, collections, msg);
        }
    }

    public static class BackupFileInfo {
        public final String displayName;
        public final Uri uri;
        public final File file;
        public final long lastModified;

        public BackupFileInfo(String name, Uri uri, File file, long lastModified) {
            this.displayName = name;
            this.uri = uri;
            this.file = file;
            this.lastModified = lastModified;
        }
    }

    public static List<BackupFileInfo> findAvailableBackups(Context context) {
        List<BackupFileInfo> list = new ArrayList<>();
        Map<String, BackupFileInfo> map = new LinkedHashMap<>();

        // 1. Check internal filesDir/backups
        File internalBackups = new File(context.getFilesDir(), "backups");
        if (internalBackups.exists() && internalBackups.isDirectory()) {
            File[] files = internalBackups.listFiles((dir, name) -> name != null && name.startsWith("photoplace_backup_") && name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    map.put(f.getName(), new BackupFileInfo(f.getName(), Uri.fromFile(f), f, f.lastModified()));
                }
            }
        }

        // 2. Check MediaStore.Downloads
        try {
            Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            String[] projection = new String[]{MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.DATE_MODIFIED};
            String selection = MediaStore.Downloads.DISPLAY_NAME + " LIKE 'photoplace_backup_%.json'";
            try (Cursor cursor = context.getContentResolver().query(collection, projection, selection, null, MediaStore.Downloads.DATE_MODIFIED + " DESC")) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        String name = cursor.getString(1);
                        long dateSec = cursor.getLong(2);
                        Uri contentUri = ContentUris.withAppendedId(collection, id);
                        if (name != null && !map.containsKey(name)) {
                            map.put(name, new BackupFileInfo(name, contentUri, null, dateSec * 1000L));
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Check public Download/PhotoPlace directory
        try {
            File publicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PhotoPlace");
            if (publicDir.exists() && publicDir.isDirectory()) {
                File[] files = publicDir.listFiles((dir, name) -> name != null && name.startsWith("photoplace_backup_") && name.endsWith(".json"));
                if (files != null) {
                    for (File f : files) {
                        if (!map.containsKey(f.getName())) {
                            map.put(f.getName(), new BackupFileInfo(f.getName(), Uri.fromFile(f), f, f.lastModified()));
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        list.addAll(map.values());
        list.sort((a, b) -> Long.compare(b.lastModified, a.lastModified));
        return list;
    }

    public static BackupSummary generateBackupData(File filesDir) {
        int notesCount = 0;
        int placesCount = 0;
        int collectionsCount = 0;
        int personalizationCount = 0;

        JSONObject data = new JSONObject();

        // 1. memory_date_notes.json
        File notesFile = new File(filesDir, "memory_date_notes.json");
        if (notesFile.exists() && notesFile.length() > 0) {
            try {
                JSONObject root = new JSONObject(readString(notesFile));
                JSONObject notes = root.optJSONObject("notes");
                if (notes != null) {
                    notesCount = notes.length();
                    data.put("memory_date_notes", root);
                }
            } catch (Exception ignored) {}
        }

        // 2. personal_places.json
        File placesFile = new File(filesDir, "personal_places.json");
        if (placesFile.exists() && placesFile.length() > 0) {
            try {
                JSONObject root = new JSONObject(readString(placesFile));
                JSONArray places = root.optJSONArray("places");
                if (places != null) {
                    placesCount = places.length();
                    data.put("personal_places", root);
                }
            } catch (Exception ignored) {}
        }

        // 3. memory_collections.json
        File collectionsFile = new File(filesDir, "memory_collections.json");
        if (collectionsFile.exists() && collectionsFile.length() > 0) {
            try {
                JSONObject root = new JSONObject(readString(collectionsFile));
                JSONArray collections = root.optJSONArray("collections");
                if (collections != null) {
                    collectionsCount = collections.length();
                    data.put("memory_collections", root);
                }
            } catch (Exception ignored) {}
        }

        // 4. memory_personalization.json
        File personalizationFile = new File(filesDir, "memory_personalization.json");
        if (personalizationFile.exists() && personalizationFile.length() > 0) {
            try {
                JSONObject root = new JSONObject(readString(personalizationFile));
                JSONObject memories = root.optJSONObject("memories");
                if (memories != null) {
                    personalizationCount = memories.length();
                    data.put("memory_personalization", root);
                }
            } catch (Exception ignored) {}
        }

        // 5. memory_identity_registry.json
        File registryFile = new File(filesDir, "memory_identity_registry.json");
        if (registryFile.exists() && registryFile.length() > 0) {
            try {
                JSONObject root = new JSONObject(readString(registryFile));
                JSONObject aliases = root.optJSONObject("aliases");
                if (aliases != null) {
                    data.put("memory_identity_registry", root);
                }
            } catch (Exception ignored) {}
        }

        JSONObject backupRoot = new JSONObject();
        try {
            backupRoot.put("backupVersion", 1);
            backupRoot.put("app", "PhotoPlace");
            backupRoot.put("createdAtMillis", System.currentTimeMillis());
            backupRoot.put("data", data);
        } catch (Exception ignored) {}

        return new BackupSummary(notesCount, placesCount, collectionsCount, personalizationCount, backupRoot.toString());
    }

    public static RestoreResult restoreFromJsonString(File filesDir, String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return RestoreResult.fail("백업 파일이 비어 있습니다.");
        }
        JSONObject backupRoot;
        try {
            backupRoot = new JSONObject(jsonStr);
        } catch (Exception e) {
            return RestoreResult.fail("올바른 JSON 형식이 아닙니다.");
        }

        int backupVersion = backupRoot.optInt("backupVersion", -1);
        if (backupVersion < 1) {
            return RestoreResult.fail("지원되지 않거나 손상된 백업 버전입니다.");
        }

        JSONObject data = backupRoot.optJSONObject("data");
        if (data == null) {
            return RestoreResult.fail("백업 데이터 항목(data)이 없습니다.");
        }

        int restoredNotes = 0;
        int restoredPlaces = 0;
        int restoredCollections = 0;

        Map<File, String> filesToWrite = new LinkedHashMap<>();
        Map<String, String> backupIdToCurrentId = new HashMap<>();

        // Phase 1: In-memory parsing, ID mapping, and timestamp-based conflict resolution

        // 1. Process memory_identity_registry
        File registryFile = new File(filesDir, "memory_identity_registry.json");
        JSONObject currentAliases = new JSONObject();
        try {
            JSONObject currentRoot = readSafeExistingRoot(registryFile, "aliases", false);
            JSONObject aliases = currentRoot.optJSONObject("aliases");
            if (aliases != null) {
                currentAliases = aliases;
            }
        } catch (Exception e) {
            return RestoreResult.fail("식별자 레지스트리 검증 오류: " + e.getMessage());
        }

        if (data.has("memory_identity_registry")) {
            try {
                JSONObject backupRegistryRoot = data.getJSONObject("memory_identity_registry");
                JSONObject backupAliases = backupRegistryRoot.optJSONObject("aliases");
                if (backupAliases != null) {
                    // Group backup aliases by their backupStableId
                    Map<String, List<String>> backupIdToAliases = new LinkedHashMap<>();
                    for (Iterator<String> it = backupAliases.keys(); it.hasNext(); ) {
                        String alias = it.next();
                        String backupStableId = backupAliases.optString(alias, "");
                        if (backupStableId.isEmpty()) continue;
                        List<String> list = backupIdToAliases.get(backupStableId);
                        if (list == null) {
                            list = new ArrayList<>();
                            backupIdToAliases.put(backupStableId, list);
                        }
                        list.add(alias);
                    }

                    // For each backupStableId, check for conflict and determine targetId
                    for (Map.Entry<String, List<String>> entry : backupIdToAliases.entrySet()) {
                        String backupStableId = entry.getKey();
                        List<String> aliasesForId = entry.getValue();

                        Set<String> existingTargetIds = new LinkedHashSet<>();
                        for (String alias : aliasesForId) {
                            String existingId = currentAliases.optString(alias, "");
                            if (!existingId.isEmpty()) {
                                existingTargetIds.add(existingId);
                            }
                        }

                        // Conflict check: if the aliases in this backup ID currently point to
                        // multiple DIFFERENT existing IDs on this device, merging them would overwrite
                        // one place's notes with the other's! Stop immediately before modifying any file.
                        if (existingTargetIds.size() > 1) {
                            return RestoreResult.fail("복원 충돌: 기존 데이터의 서로 다른 장소("
                                    + String.join(", ", existingTargetIds) + ")가 백업에서 하나로 연결되어 있어 데이터 보호를 위해 복원을 중단했습니다.");
                        }

                        String targetId = existingTargetIds.isEmpty() ? backupStableId : existingTargetIds.iterator().next();
                        backupIdToCurrentId.put(backupStableId, targetId);

                        // Unify all aliases of this backup place to targetId
                        for (String alias : aliasesForId) {
                            currentAliases.put(alias, targetId);
                        }
                    }

                    JSONObject mergedRegistry = new JSONObject();
                    mergedRegistry.put("schemaVersion", 1);
                    mergedRegistry.put("aliases", currentAliases);
                    filesToWrite.put(registryFile, mergedRegistry.toString(2));
                }
            } catch (Exception e) {
                return RestoreResult.fail("식별자 레지스트리 복원 중 오류: " + e.getMessage());
            }
        }

        // 2. Process memory_date_notes
        if (data.has("memory_date_notes")) {
            try {
                JSONObject backupNotesRoot = data.getJSONObject("memory_date_notes");
                JSONObject backupNotes = backupNotesRoot.optJSONObject("notes");
                if (backupNotes != null) {
                    File notesFile = new File(filesDir, "memory_date_notes.json");
                    JSONObject currentNotes = new JSONObject();
                    JSONObject currentRoot = readSafeExistingRoot(notesFile, "notes", false);
                    JSONObject cur = currentRoot.optJSONObject("notes");
                    if (cur != null) {
                        currentNotes = cur;
                    }
                    for (Iterator<String> it = backupNotes.keys(); it.hasNext(); ) {
                        String key = it.next();
                        JSONObject backupNote = backupNotes.optJSONObject(key);
                        if (backupNote == null) continue;

                        // Remap stableMemoryId if key has format "memory-id:<stableMemoryId>#<dateKey>"
                        String targetKey = key;
                        if (key.startsWith("memory-id:") && key.contains("#")) {
                            int split = key.lastIndexOf('#');
                            String oldId = key.substring("memory-id:".length(), split);
                            String dateKey = key.substring(split + 1);
                            String mappedId = backupIdToCurrentId.getOrDefault(oldId, oldId);
                            targetKey = "memory-id:" + mappedId + "#" + dateKey;
                        }

                        JSONObject existingNote = currentNotes.optJSONObject(targetKey);
                        if (existingNote == null || backupNote.optLong("updatedAtMillis") >= existingNote.optLong("updatedAtMillis")) {
                            currentNotes.put(targetKey, backupNote);
                        }
                        restoredNotes++;
                    }
                    JSONObject mergedRoot = new JSONObject();
                    mergedRoot.put("schemaVersion", 1);
                    mergedRoot.put("notes", currentNotes);
                    filesToWrite.put(notesFile, mergedRoot.toString(2));
                }
            } catch (Exception e) {
                return RestoreResult.fail("메모 복원 중 오류: " + e.getMessage());
            }
        }

        // 3. Process personal_places
        if (data.has("personal_places")) {
            try {
                JSONObject backupPlacesRoot = data.getJSONObject("personal_places");
                JSONArray backupPlaces = backupPlacesRoot.optJSONArray("places");
                if (backupPlaces != null) {
                    File placesFile = new File(filesDir, "personal_places.json");
                    Map<String, JSONObject> placesMap = new LinkedHashMap<>();
                    JSONObject currentRoot = readSafeExistingRoot(placesFile, "places", true);
                    JSONArray currentPlaces = currentRoot.optJSONArray("places");
                    if (currentPlaces != null) {
                        for (int i = 0; i < currentPlaces.length(); i++) {
                            JSONObject p = currentPlaces.optJSONObject(i);
                            if (p != null) {
                                String id = p.optString("placeId", String.valueOf(i));
                                placesMap.put(id, p);
                            }
                        }
                    }
                    for (int i = 0; i < backupPlaces.length(); i++) {
                        JSONObject p = backupPlaces.optJSONObject(i);
                        if (p != null) {
                            String id = p.optString("placeId", "backup_" + i);
                            // Remap parentStableId if present
                            String parentStableId = p.optString("parentStableId", "");
                            if (!parentStableId.isEmpty() && backupIdToCurrentId.containsKey(parentStableId)) {
                                p.put("parentStableId", backupIdToCurrentId.get(parentStableId));
                            }
                            JSONObject existing = placesMap.get(id);
                            // Preserve newer records: only overwrite if backup is newer or equal
                            if (existing == null || p.optLong("updatedAtMillis") >= existing.optLong("updatedAtMillis")) {
                                placesMap.put(id, p);
                            }
                            restoredPlaces++;
                        }
                    }
                    JSONArray mergedPlaces = new JSONArray();
                    for (JSONObject p : placesMap.values()) {
                        mergedPlaces.put(p);
                    }
                    JSONObject mergedRoot = new JSONObject();
                    mergedRoot.put("schemaVersion", 1);
                    mergedRoot.put("places", mergedPlaces);
                    filesToWrite.put(placesFile, mergedRoot.toString(2));
                }
            } catch (Exception e) {
                return RestoreResult.fail("개인 장소 복원 중 오류: " + e.getMessage());
            }
        }

        // 4. Process memory_collections
        if (data.has("memory_collections")) {
            try {
                JSONObject backupCollectionsRoot = data.getJSONObject("memory_collections");
                JSONArray backupCollections = backupCollectionsRoot.optJSONArray("collections");
                if (backupCollections != null) {
                    File collectionsFile = new File(filesDir, "memory_collections.json");
                    Map<String, JSONObject> collectionsMap = new LinkedHashMap<>();
                    JSONObject currentRoot = readSafeExistingRoot(collectionsFile, "collections", true);
                    JSONArray currentCollections = currentRoot.optJSONArray("collections");
                    if (currentCollections != null) {
                        for (int i = 0; i < currentCollections.length(); i++) {
                            JSONObject c = currentCollections.optJSONObject(i);
                            if (c != null) {
                                String id = c.optString("collectionId", String.valueOf(i));
                                collectionsMap.put(id, c);
                            }
                        }
                    }
                    for (int i = 0; i < backupCollections.length(); i++) {
                        JSONObject c = backupCollections.optJSONObject(i);
                        if (c != null) {
                            String id = c.optString("collectionId", "backup_" + i);
                            // Remap member stableMemoryIds if present
                            JSONArray members = c.optJSONArray("members");
                            if (members != null) {
                                for (int m = 0; m < members.length(); m++) {
                                    JSONObject mem = members.optJSONObject(m);
                                    if (mem != null) {
                                        String sId = mem.optString("stableMemoryId", "");
                                        if (!sId.isEmpty() && backupIdToCurrentId.containsKey(sId)) {
                                            mem.put("stableMemoryId", backupIdToCurrentId.get(sId));
                                        }
                                    }
                                }
                            }
                            JSONObject existing = collectionsMap.get(id);
                            // Preserve newer records: only overwrite if backup is newer or equal
                            if (existing == null || c.optLong("updatedAtMillis") >= existing.optLong("updatedAtMillis")) {
                                collectionsMap.put(id, c);
                            }
                            restoredCollections++;
                        }
                    }
                    JSONArray mergedCollections = new JSONArray();
                    for (JSONObject c : collectionsMap.values()) {
                        mergedCollections.put(c);
                    }
                    JSONObject mergedRoot = new JSONObject();
                    mergedRoot.put("schemaVersion", 1);
                    mergedRoot.put("collections", mergedCollections);
                    filesToWrite.put(collectionsFile, mergedRoot.toString(2));
                }
            } catch (Exception e) {
                return RestoreResult.fail("컬렉션 복원 중 오류: " + e.getMessage());
            }
        }

        // 5. Process memory_personalization
        if (data.has("memory_personalization")) {
            try {
                JSONObject backupPersonalizationRoot = data.getJSONObject("memory_personalization");
                JSONObject backupMemories = backupPersonalizationRoot.optJSONObject("memories");
                if (backupMemories != null) {
                    File personalizationFile = new File(filesDir, "memory_personalization.json");
                    JSONObject currentMemories = new JSONObject();
                    JSONObject currentRoot = readSafeExistingRoot(personalizationFile, "memories", false);
                    JSONObject cur = currentRoot.optJSONObject("memories");
                    if (cur != null) {
                        currentMemories = cur;
                    }
                    for (Iterator<String> it = backupMemories.keys(); it.hasNext(); ) {
                        String key = it.next();
                        JSONObject m = backupMemories.optJSONObject(key);
                        if (m != null) {
                            JSONObject existing = currentMemories.optJSONObject(key);
                            // Preserve newer records: only overwrite if backup is newer or equal
                            if (existing == null || m.optLong("updatedAtMillis") >= existing.optLong("updatedAtMillis")) {
                                currentMemories.put(key, m);
                            }
                        }
                    }
                    JSONObject mergedRoot = new JSONObject();
                    mergedRoot.put("schemaVersion", 1);
                    mergedRoot.put("memories", currentMemories);
                    filesToWrite.put(personalizationFile, mergedRoot.toString(2));
                }
            } catch (Exception e) {
                return RestoreResult.fail("대표 설정 복원 중 오류: " + e.getMessage());
            }
        }

        // Phase 2: Atomic all-or-nothing write with snapshot rollback
        Map<File, FileSnapshot> snapshots = new HashMap<>();
        try {
            for (File target : filesToWrite.keySet()) {
                snapshots.put(target, FileSnapshot.capture(target));
            }
        } catch (Exception snapshotError) {
            // Snapshotting existing files failed before any write was made.
            // Do NOT touch, modify, or delete any file on disk!
            return RestoreResult.fail("기존 데이터 백업 중 오류: " + snapshotError.getMessage());
        }

        Set<File> writtenFiles = new HashSet<>();
        try {
            for (Map.Entry<File, String> entry : filesToWrite.entrySet()) {
                atomicWrite(entry.getKey(), entry.getValue());
                writtenFiles.add(entry.getKey());
            }
        } catch (Exception writeError) {
            // Roll back ONLY the files that were actually modified or created during this write attempt!
            for (File target : writtenFiles) {
                FileSnapshot snap = snapshots.get(target);
                if (snap != null) {
                    snap.rollback();
                }
            }
            return RestoreResult.fail("복원 파일 저장 중 오류: " + writeError.getMessage());
        }

        String msg = "데이터가 안전하게 복원되었습니다.\n\n"
                + "• 날짜별 메모: " + restoredNotes + "건\n"
                + "• 개인 장소: " + restoredPlaces + "건\n"
                + "• 컬렉션: " + restoredCollections + "건";
        return RestoreResult.ok(restoredNotes, restoredPlaces, restoredCollections, msg);
    }

    public static RestoreResult restoreFromInputStream(File filesDir, InputStream in) {
        if (in == null) {
            return RestoreResult.fail("파일을 열 수 없습니다.");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            return restoreFromJsonString(filesDir, out.toString("UTF-8"));
        } catch (Exception e) {
            return RestoreResult.fail("파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public static RestoreResult restoreFromUri(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            return restoreFromInputStream(context.getFilesDir(), in);
        } catch (Exception e) {
            return RestoreResult.fail("파일을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public static void performBackup(Activity activity) {
        BackupSummary summary = generateBackupData(activity.getFilesDir());
        if (summary.totalItems() == 0) {
            MemoryActionDialog.show(activity, "백업 안내",
                    "아직 백업할 메모나 개인 장소 데이터가 없습니다.", "확인", () -> {}, null, null);
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "photoplace_backup_" + timeStamp + ".json";
        byte[] bytes = summary.jsonString.getBytes(StandardCharsets.UTF_8);

        // 1. Save to Downloads via MediaStore
        boolean savedToDownloads = false;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PhotoPlace");
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            }
            Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = activity.getContentResolver().openOutputStream(uri)) {
                    out.write(bytes);
                    out.flush();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    activity.getContentResolver().update(uri, values, null, null);
                }
                savedToDownloads = true;
            }
        } catch (Exception ignored) {}

        // 2. Prepare internal app copy for instant restore discovery
        try {
            File internalBackups = new File(activity.getFilesDir(), "backups");
            if (!internalBackups.exists()) {
                internalBackups.mkdirs();
            }
            File internalFile = new File(internalBackups, fileName);
            try (FileOutputStream out = new FileOutputStream(internalFile)) {
                out.write(bytes);
                out.flush();
            }
        } catch (Exception ignored) {}

        // 3. Prepare local cache copy for Intent.ACTION_SEND
        File cacheDir = new File(activity.getCacheDir(), "stories");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        File cacheFile = new File(cacheDir, fileName);
        try (FileOutputStream out = new FileOutputStream(cacheFile)) {
            out.write(bytes);
            out.flush();
        } catch (Exception ignored) {}

        Uri shareUri = null;
        try {
            shareUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".storyfiles", cacheFile);
        } catch (Exception ignored) {}

        final Uri finalShareUri = shareUri;
        String desc = (savedToDownloads ? "내 기록이 다운로드(Download/PhotoPlace) 폴더에 안전하게 저장되었습니다.\n\n" : "백업 파일이 생성되었습니다.\n\n")
                + "• 날짜별 메모: " + summary.notesCount + "건\n"
                + "• 개인 장소: " + summary.placesCount + "건\n"
                + "• 컬렉션: " + summary.collectionsCount + "건\n\n"
                + "파일명: " + fileName + "\n\n"
                + "다른 기기나 클라우드(드라이브, 카카오톡 등)로 백업 파일을 전송하시겠습니까?";

        if (finalShareUri != null) {
            MemoryActionDialog.show(activity, "기록 백업 완료", desc, "파일 공유하기", () -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("application/json");
                share.putExtra(Intent.EXTRA_STREAM, finalShareUri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(Intent.createChooser(share, "백업 파일 저장/공유"));
            }, "닫기", () -> {});
        } else {
            MemoryActionDialog.show(activity, "기록 백업 완료", desc, "확인", () -> {}, null, null);
        }
    }

    private static String readString(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            return out.toString("UTF-8");
        }
    }

    static void atomicWrite(File target, String content) throws Exception {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File temp = new File(target.getPath() + ".tmp");
        File bak = new File(target.getPath() + ".bak");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
        if (bak.exists()) {
            bak.delete();
        }
        boolean hadTarget = target.exists();
        if (hadTarget && !target.renameTo(bak)) {
            if (temp.exists()) temp.delete();
            throw new IllegalStateException("Could not move " + target.getName() + " to backup");
        }
        if (!temp.renameTo(target)) {
            if (hadTarget && bak.exists()) {
                bak.renameTo(target);
            }
            throw new IllegalStateException("Failed to replace " + target.getName());
        }
        if (bak.exists()) {
            bak.delete();
        }
    }

    static final class FileSnapshot {
        final File target;
        final boolean targetExists;
        final String targetContent;
        final boolean bakExists;
        final String bakContent;

        FileSnapshot(File target, boolean targetExists, String targetContent, boolean bakExists, String bakContent) {
            this.target = target;
            this.targetExists = targetExists;
            this.targetContent = targetContent;
            this.bakExists = bakExists;
            this.bakContent = bakContent;
        }

        static FileSnapshot capture(File target) throws Exception {
            boolean targetExists = target.exists();
            String targetContent = targetExists ? readString(target) : null;
            File bak = new File(target.getPath() + ".bak");
            boolean bakExists = bak.exists();
            String bakContent = bakExists ? readString(bak) : null;
            return new FileSnapshot(target, targetExists, targetContent, bakExists, bakContent);
        }

        void rollback() {
            if (targetExists) {
                try (FileOutputStream out = new FileOutputStream(target)) {
                    out.write(targetContent.getBytes(StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            } else {
                if (target.exists()) target.delete();
            }

            File bak = new File(target.getPath() + ".bak");
            if (bakExists) {
                try (FileOutputStream out = new FileOutputStream(bak)) {
                    out.write(bakContent.getBytes(StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            } else {
                if (bak.exists()) bak.delete();
            }

            File temp = new File(target.getPath() + ".tmp");
            if (temp.exists()) temp.delete();
        }
    }

    static JSONObject readSafeExistingRoot(File target, String expectedKey, boolean isArray) throws Exception {
        File bak = new File(target.getPath() + ".bak");
        boolean targetExists = target.exists();
        boolean bakExists = bak.exists();

        if (!targetExists && !bakExists) {
            return new JSONObject();
        }

        JSONObject targetRoot = readValidatedRoot(target, expectedKey, isArray);
        if (targetRoot != null) {
            return targetRoot;
        }

        JSONObject bakRoot = readValidatedRoot(bak, expectedKey, isArray);
        if (bakRoot != null) {
            return bakRoot;
        }

        throw new IllegalStateException("기존 " + target.getName() + " 파일이 손상되어 안전하게 복구할 수 없습니다.");
    }

    private static JSONObject readValidatedRoot(File file, String expectedKey, boolean isArray) {
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            String content = readString(file);
            if (content == null || content.trim().isEmpty()) {
                return null;
            }
            JSONObject root = new JSONObject(content);
            if (root.optInt("schemaVersion", -1) != 1) {
                return null;
            }
            if (expectedKey != null) {
                Object val = root.opt(expectedKey);
                if (isArray) {
                    if (!(val instanceof JSONArray)) return null;
                } else {
                    if (!(val instanceof JSONObject)) return null;
                }
            }
            return root;
        } catch (Exception ignored) {
            return null;
        }
    }
}
