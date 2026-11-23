package com.example.gallerysorter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONArray;

/** Single SQLite commit boundary; manual aliases never require a second JSON write. */
final class ManualPlaceStore {
    static final String FILE_NAME = "manual_places.db";
    private static final Object LOCK = new Object();
    private final File file;

    ManualPlaceStore(Context context) { file = context.getDatabasePath(FILE_NAME); }

    long revision() {
        synchronized (LOCK) {
            if (!file.exists()) return 0;
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                if (!initialized(db)) return 0;
                try (Cursor cursor = db.rawQuery("SELECT revision FROM manual_state WHERE id=1", null)) {
                    if (!cursor.moveToFirst() || cursor.getType(0) != Cursor.FIELD_TYPE_INTEGER || cursor.getLong(0) < 0)
                        throw new IllegalStateException("Manual revision unavailable");
                    return cursor.getLong(0);
                }
            }
        }
    }

    ManualPlaceSnapshot read() throws Exception {
        return read(false,true);
    }

    ManualPlaceSnapshot readIdentities() throws Exception {
        return read(true,false);
    }
    ManualPlaceSnapshot readAssignments() throws Exception { return read(false,false); }

    int countUnresolved(java.util.Set<String> excludedNormalizedPaths) {
        synchronized (LOCK) {
            if (!file.exists()) return 0;
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                if (!initialized(db)) return 0;
                String sql = db.getVersion() >= 2
                        ? "SELECT i.path FROM manual_inventory i LEFT JOIN manual_assignments a ON i.asset_key=a.asset_key WHERE a.asset_key IS NULL"
                        : "SELECT path FROM manual_inventory";
                int count = 0;
                try (Cursor cursor = db.rawQuery(sql, null)) {
                    while (cursor.moveToNext()) {
                        String path = ManualPlacePaths.normalize(cursor.getString(0));
                        if (excludedNormalizedPaths == null || !excludedNormalizedPaths.contains(path)) {
                            count++;
                        }
                    }
                }
                return count;
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    private ManualPlaceSnapshot read(boolean identitiesOnly,boolean loadInventory) throws Exception {
        synchronized (LOCK) {
            if (!file.exists()) return ManualPlaceSnapshot.empty();
            // Read-only: no schema creation, backup repair or alias migration during projection.
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                if (!initialized(db)) return ManualPlaceSnapshot.empty();
                return read(db, identitiesOnly,loadInventory);
            }
        }
    }

    void reconcileInventory(List<String> paths, List<UnresolvedMediaRecord> rows) throws Exception {
        if (paths.isEmpty()) {
            if (!rows.isEmpty()) throw new IllegalArgumentException("Inventory without scan scope");
            return;
        }
        synchronized (LOCK) {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Manual store directory unavailable");
            try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
                // FULL synchronization and a single transaction cover revision, aliases and payload.
                db.execSQL("PRAGMA synchronous=FULL");
                db.beginTransaction();
                try {
                    boolean existed = initialized(db);
                    db.execSQL("CREATE TABLE IF NOT EXISTS manual_state (id INTEGER PRIMARY KEY CHECK(id=1), revision INTEGER NOT NULL, payload TEXT NOT NULL)");
                    db.execSQL("CREATE TABLE IF NOT EXISTS manual_inventory (asset_key TEXT PRIMARY KEY, path TEXT NOT NULL, payload TEXT NOT NULL)");
                    ensureAssignmentTables(db);
                    ManualPlaceSnapshot before;
                    try (Cursor cursor = db.rawQuery("SELECT revision,payload FROM manual_state WHERE id=1", null)) {
                        if (cursor.moveToFirst()) {
                            if (cursor.getType(0) != Cursor.FIELD_TYPE_INTEGER)
                                throw new IllegalStateException("Invalid manual revision");
                            before = snapshot(cursor);
                        } else {
                            if (existed) throw new IllegalStateException("Existing manual state is incomplete");
                            before = ManualPlaceSnapshot.empty();
                        }
                    }
                    // Validate the complete incoming scope before touching any persisted inventory.
                    ManualPlaceInventory.Update update = new ManualPlaceInventory.Update(before.revision, paths, rows);
                    ArrayList<String> removeKeys = new ArrayList<>();
                    // Inspect light key/path columns. Parse an existing row only for an outside-scope collision.
                    try (Cursor existing = db.rawQuery("SELECT asset_key,path FROM manual_inventory", null)) {
                        while (existing.moveToNext()) {
                            String key = existing.getString(0);
                            if (update.containsPath(existing.getString(1))) {
                                removeKeys.add(key);
                            } else if (update.containsIncoming(key)) {
                                try (Cursor collision = db.rawQuery("SELECT payload FROM manual_inventory WHERE asset_key=?", new String[]{key})) {
                                    if (!collision.moveToFirst()) throw new IllegalStateException("Missing inventory collision");
                                    UnresolvedMediaRecord row = UnresolvedMediaRecord.fromJson(new JSONObject(collision.getString(0)));
                                    if (!row.assetKey().equals(key)
                                            || !row.relativePath.equals(ManualPlacePaths.normalize(existing.getString(1))))
                                        throw new IllegalStateException("Stored inventory identity/path mismatch");
                                    update.verifyOutsideScope(row);
                                }
                            }
                        }
                    }
                    for (int offset = 0; offset < removeKeys.size(); offset += 400) {
                        List<String> batch = removeKeys.subList(offset, Math.min(removeKeys.size(), offset + 400));
                        db.delete("manual_inventory", "asset_key IN (" + String.join(",", java.util.Collections.nCopies(batch.size(), "?")) + ")",
                                batch.toArray(new String[0]));
                    }
                    for (UnresolvedMediaRecord row : update.incoming)
                        db.execSQL("INSERT OR REPLACE INTO manual_inventory(asset_key,path,payload) VALUES(?,?,?)",
                                new Object[]{row.assetKey(), row.relativePath, row.toJson().toString()});
                    JSONObject state = before.writableCopy();
                    java.util.LinkedHashSet<String> scopes = new java.util.LinkedHashSet<>();
                    JSONObject oldInventory = state.optJSONObject("inventory");
                    JSONArray oldScopes = oldInventory == null ? null : oldInventory.optJSONArray("scopes");
                    if (oldScopes != null) for (int i=0;i<oldScopes.length();i++) scopes.add(oldScopes.getString(i));
                    scopes.addAll(ManualPlacePaths.normalizeAll(paths));
                    state.put("inventory", new JSONObject().put("capturedAt", System.currentTimeMillis())
                            .put("scopes", new JSONArray(scopes)));
                    db.execSQL("INSERT OR REPLACE INTO manual_state(id,revision,payload) VALUES(1,?,?)",
                            new Object[]{update.nextRevision, state.toString()});
                    db.setVersion(2);
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
            }
        }
    }

    private ManualPlaceSnapshot read(SQLiteDatabase db, boolean identitiesOnly,boolean loadInventory) throws Exception {
        try (Cursor cursor = db.rawQuery("SELECT revision,payload FROM manual_state WHERE id=1", null)) {
            // An existing but incomplete DB is unknown, never a fresh empty user store.
            if (!cursor.moveToFirst() || cursor.getType(0) != Cursor.FIELD_TYPE_INTEGER)
                throw new IllegalStateException("Manual state unavailable");
            ManualPlaceSnapshot snapshot = snapshot(cursor);
            if (cursor.moveToNext()) throw new IllegalStateException("Multiple manual states");
            if (db.getVersion() >= 2) {
                int count;
                try (Cursor number = db.rawQuery("SELECT COUNT(*) FROM manual_assignments", null)) {
                    if (!number.moveToFirst()) throw new IllegalStateException("Assignments unavailable");
                    count = number.getInt(0);
                }
                List<ManualMediaAssignment> assignments = new ArrayList<>();
                if (!identitiesOnly) try (Cursor assigned = db.rawQuery("SELECT asset_key,payload FROM manual_assignments", null)) {
                    while (assigned.moveToNext()) {
                        ManualMediaAssignment a = ManualMediaAssignment.fromJson(new JSONObject(assigned.getString(1)));
                        if (!a.media.assetKey().equals(assigned.getString(0))) throw new IllegalStateException("Assignment identity mismatch");
                        assignments.add(a);
                    }
                    if (assignments.size() != count) throw new IllegalStateException("Assignments changed while reading");
                }
                snapshot = snapshot.withAssignments(assignments, count);
            }
            if (loadInventory) {
                List<UnresolvedMediaRecord> rows = new ArrayList<>();
                try (Cursor inventory = db.rawQuery("SELECT asset_key,payload FROM manual_inventory", null)) {
                    while (inventory.moveToNext()) {
                        UnresolvedMediaRecord row = UnresolvedMediaRecord.fromJson(new JSONObject(inventory.getString(1)));
                        if (!row.assetKey().equals(inventory.getString(0))) throw new IllegalStateException("Inventory identity mismatch");
                        rows.add(row);
                    }
                }
                snapshot = snapshot.withInventory(snapshot.revision, rows);
            }
            try (Cursor latest = db.rawQuery("SELECT revision FROM manual_state WHERE id=1", null)) {
                if (!latest.moveToFirst() || latest.getType(0) != Cursor.FIELD_TYPE_INTEGER
                        || latest.getLong(0) != snapshot.revision)
                    throw new IllegalStateException("Manual state changed during read");
            }
            return snapshot;
        }
    }

    private ManualPlaceSnapshot snapshot(Cursor cursor) throws Exception {
        return new ManualPlaceSnapshot(cursor.getLong(0), cursor.getString(1));
    }

    private boolean initialized(SQLiteDatabase db) {
        int version = db.getVersion();
        if (version == 1 || version == 2) return true;
        if (version != 0) throw new IllegalStateException("Unknown manual database version");
        try (Cursor tables = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name IN (?,?)",
                new String[]{"manual_state", "manual_inventory"})) {
            if (tables.moveToFirst()) throw new IllegalStateException("Unversioned manual database");
        }
        // A first-ever transaction rolled back before commit; it contained no user state.
        return false;
    }

    private void ensureAssignmentTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS manual_assignments (asset_key TEXT PRIMARY KEY, payload TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS manual_copy_receipts (receipt_id TEXT PRIMARY KEY, payload TEXT NOT NULL)");
    }

    private interface Mutation {
        String apply(SQLiteDatabase db, ManualPlaceSnapshot before, JSONObject state) throws Exception;
    }

    private String mutate(long expectedRevision, Mutation mutation) throws Exception {
        synchronized (LOCK) {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) throw new IllegalStateException("기록 저장소를 열지 못했어요.");
            try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
                db.execSQL("PRAGMA synchronous=FULL");
                db.beginTransaction();
                try {
                    boolean existed = initialized(db);
                    if (!existed) {
                        db.execSQL("CREATE TABLE manual_state (id INTEGER PRIMARY KEY CHECK(id=1), revision INTEGER NOT NULL, payload TEXT NOT NULL)");
                        db.execSQL("CREATE TABLE manual_inventory (asset_key TEXT PRIMARY KEY, path TEXT NOT NULL, payload TEXT NOT NULL)");
                        db.execSQL("INSERT INTO manual_state VALUES(1,0,?)", new Object[]{ManualPlaceSnapshot.empty().identitiesPayload()});
                    }
                    ensureAssignmentTables(db);
                    // Revision is read within the same write transaction as every assignment mutation.
                    ManualPlaceSnapshot before;
                    try (Cursor cursor = db.rawQuery("SELECT revision,payload FROM manual_state WHERE id=1", null)) {
                        if (!cursor.moveToFirst() || cursor.getType(0) != Cursor.FIELD_TYPE_INTEGER)
                            throw new IllegalStateException("기록 상태를 확인하지 못했어요.");
                        before = snapshot(cursor);
                    }
                    if (expectedRevision >= 0 && expectedRevision != before.revision)
                        throw new IllegalStateException("사진이나 기록이 바뀌었어요. 다시 불러와 주세요.");
                    JSONObject state = before.writableCopy();
                    if (state.getJSONObject("assignments").length() != 0)
                        throw new IllegalStateException("이전 수동 기록 형식의 확인이 필요해요.");
                    String result = mutation.apply(db, before, state);
                    ManualPlaceSnapshot next = new ManualPlaceSnapshot(Math.addExact(before.revision, 1), state.toString());
                    db.execSQL("UPDATE manual_state SET revision=?,payload=? WHERE id=1", new Object[]{next.revision, next.identitiesPayload()});
                    db.setVersion(2);
                    db.setTransactionSuccessful();
                    return result;
                } finally { db.endTransaction(); }
            }
        }
    }

    String assign(List<UnresolvedMediaRecord> rows, MemoryRecord target, String stableId,
                  String title, long expectedRevision) throws Exception {
        if (rows.isEmpty()) throw new IllegalArgumentException("사진을 선택해 주세요.");
        String name = title == null ? "" : title.trim();
        if (name.isEmpty() || name.length() > 80 || name.matches("(?s).*\\p{Cntrl}.*"))
            throw new IllegalArgumentException("장소 이름은 1~80자로 입력해 주세요.");
        return mutate(expectedRevision, (db,before,state) -> {
            JSONObject places = state.getJSONObject("places");
            String placeId = "";
            if (target != null) {
                if (stableId == null || !stableId.startsWith("mem_") || stableId.length() <= 4)
                    throw new IllegalStateException("위치기록의 식별자를 확인하지 못했어요.");
                for (java.util.Iterator<String> keys=places.keys();keys.hasNext();) {
                    String key=keys.next();
                    if (stableId.equals(places.getJSONObject(key).getString("stableMemoryId"))) { placeId=key; break; }
                }
            }
            if (placeId.isEmpty()) {
                placeId=java.util.UUID.randomUUID().toString();
                String id=target==null ? "mem_"+java.util.UUID.randomUUID().toString().replace("-", "") : stableId;
                places.put(placeId,new JSONObject().put("memoryKey","manual:"+placeId).put("stableMemoryId",id)
                        .put("name",name).put("attachMemoryKey",target==null?"":target.memoryKey)
                        .put("countryCode",target==null?"":target.countryCode).put("countryName",target==null?"":target.countryName)
                        .put("adminArea",target==null?"":target.adminArea).put("addressLine",target==null?"":target.addressLine));
            }
            List<ManualMediaAssignment> linked=assignments(db);
            for (UnresolvedMediaRecord row : ManualAssignmentSelection.expand(rows,linked)) {
                // Live metadata is used for identity/freshness, but the existing scan may have
                // resolved an EXIF/fallback date that DATE_TAKEN alone does not contain.
                // Reassignment must also retain the original logical date and its notes.
                ManualMediaAssignment previous=assignment(db,row.assetKey());
                long date=row.takenAt;
                if(previous!=null)date=previous.media.takenAt;
                else try(Cursor raw=db.rawQuery("SELECT payload FROM manual_inventory WHERE asset_key=?",new String[]{row.assetKey()})) {
                    if(raw.moveToFirst()) {
                        UnresolvedMediaRecord captured=UnresolvedMediaRecord.fromJson(new JSONObject(raw.getString(0)));
                        if(captured.size==row.size&&captured.modified==row.modified)date=captured.takenAt;
                    }
                }
                UnresolvedMediaRecord dated=new UnresolvedMediaRecord(row.uri,row.volume,row.mediaVersion,row.mediaId,
                        row.kind,row.name,row.mimeType,row.relativePath,row.size,row.modified,date);
                putAssignment(db,new ManualMediaAssignment(dated,placeId,previous==null?"":previous.copyTargetKey,
                        previous!=null&&previous.galleryCopy));
            }
            JSONObject place=places.getJSONObject(placeId);
            String attach=place.optString("attachMemoryKey","");
            return attach.isEmpty()?place.getString("memoryKey"):attach;
        });
    }

    void unassign(List<UnresolvedMediaRecord> rows, long expectedRevision) throws Exception {
        mutate(expectedRevision, (db,before,state) -> {
            for (UnresolvedMediaRecord row:ManualAssignmentSelection.expand(rows,assignments(db)))
                db.delete("manual_assignments","asset_key=?",new String[]{row.assetKey()});
            return "";
        });
    }

    private void putAssignment(SQLiteDatabase db, ManualMediaAssignment assignment) throws Exception {
        db.execSQL("INSERT OR REPLACE INTO manual_assignments(asset_key,payload) VALUES(?,?)",
                new Object[]{assignment.media.assetKey(),assignment.toJson().toString()});
    }

    private List<ManualMediaAssignment> assignments(SQLiteDatabase db)throws Exception {
        List<ManualMediaAssignment> result=new ArrayList<>();
        try(Cursor cursor=db.rawQuery("SELECT asset_key,payload FROM manual_assignments",null)) {
            while(cursor.moveToNext()) {
                ManualMediaAssignment a=ManualMediaAssignment.fromJson(new JSONObject(cursor.getString(1)));
                if(!a.media.assetKey().equals(cursor.getString(0)))throw new IllegalStateException("Assignment identity mismatch");
                result.add(a);
            }
        }
        return result;
    }

    ManualMediaAssignment assignment(String assetKey) throws Exception {
        synchronized (LOCK) {
            if (!file.exists()) return null;
            try (SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY)) {
                if (!initialized(db) || db.getVersion()<2) return null;
                try (Cursor row=db.rawQuery("SELECT payload FROM manual_assignments WHERE asset_key=?",new String[]{assetKey})) {
                    return row.moveToFirst()?ManualMediaAssignment.fromJson(new JSONObject(row.getString(0))):null;
                }
            }
        }
    }

    String beginCopy(ManualMediaAssignment source,String path,boolean video) throws Exception {
        return mutate(-1,(db,before,state)->{
            ManualMediaAssignment current=assignment(db,source.media.assetKey());
            if(current==null||!current.placeId.equals(source.placeId))throw new IllegalStateException("사진의 장소 연결이 바뀌었어요.");
            if(!current.copyTargetKey.isEmpty())throw new IllegalStateException("이미 복사된 사진이에요. 목록을 다시 불러와 주세요.");
            try(Cursor rows=db.rawQuery("SELECT payload FROM manual_copy_receipts",null)) {
                while(rows.moveToNext()) {
                    JSONObject existing=new JSONObject(rows.getString(0));
                    if(source.media.assetKey().equals(existing.getString("sourceKey"))&&!"FAILED".equals(existing.getString("state")))
                        throw new IllegalStateException("이전 복사 결과를 확인한 뒤 다시 시도해 주세요.");
                }
            }
            String id=java.util.UUID.randomUUID().toString();
            JSONObject receipt=new JSONObject().put("id",id).put("sourceKey",source.media.assetKey()).put("source",source.toJson())
                    .put("targetPath",ManualPlacePaths.normalize(path)).put("targetUri",video?source.media.uri:"")
                    .put("video",video).put("state","PENDING");
            db.execSQL("INSERT INTO manual_copy_receipts VALUES(?,?)",new Object[]{id,receipt.toString()});return id;
        });
    }
    void copyTarget(String id,String uri)throws Exception {
        mutate(-1,(db,before,state)->{JSONObject receipt=receipt(db,id);receipt.put("targetUri",uri);saveReceipt(db,receipt);return "";});
    }
    JSONObject completeCopy(String id,UnresolvedMediaRecord target)throws Exception {
        final JSONObject[] completed=new JSONObject[1];
        mutate(-1,(db,before,state)->{
            JSONObject receipt=receipt(db,id);ManualMediaAssignment source=ManualMediaAssignment.fromJson(receipt.getJSONObject("source"));
            if(!target.relativePath.equals(receipt.getString("targetPath")))throw new IllegalStateException("복사 대상 경로가 바뀌었어요.");
            if(!target.uri.equals(receipt.getString("targetUri")))throw new IllegalStateException("복사 대상 파일이 바뀌었어요.");
            ManualMediaAssignment current=assignment(db,source.media.assetKey());
            if(current==null||!current.placeId.equals(source.placeId))throw new IllegalStateException("복사 중 장소 연결이 바뀌었어요.");
            if(!receipt.getBoolean("video")&&target.assetKey().equals(source.media.assetKey()))throw new IllegalStateException("원본과 복사본이 같은 파일이에요.");
            // Preserve the memory's logical date, including UNKNOWN (0); MediaProvider may
            // manufacture today's date for a copy that has no embedded capture timestamp.
            UnresolvedMediaRecord datedTarget=new UnresolvedMediaRecord(target.uri,target.volume,target.mediaVersion,
                    target.mediaId,target.kind,target.name,target.mimeType,target.relativePath,target.size,target.modified,source.media.takenAt);
            putAssignment(db,new ManualMediaAssignment(datedTarget,source.placeId,"",true));
            if(!receipt.getBoolean("video"))putAssignment(db,new ManualMediaAssignment(source.media,source.placeId,target.assetKey(),false));
            JSONObject place=state.getJSONObject("places").getJSONObject(source.placeId);
            place.put("outputPath",target.relativePath);
            receipt.put("state","ASSIGNED").put("stableMemoryId",place.getString("stableMemoryId"));
            saveReceipt(db,receipt);completed[0]=receipt;return "";
        });return completed[0];
    }
    void finishCopyLink(String id)throws Exception {
        mutate(-1,(db,before,state)->{db.delete("manual_copy_receipts","receipt_id=?",new String[]{id});return "";});
    }
    void failedCopy(String id)throws Exception {
        mutate(-1,(db,before,state)->{JSONObject r=receipt(db,id);r.put("state","FAILED");saveReceipt(db,r);return "";});
    }
    List<JSONObject> pendingCopies()throws Exception {
        synchronized(LOCK) {
            List<JSONObject> result=new ArrayList<>();if(!file.exists())return result;
            try(SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY)) {
                if(!initialized(db)||db.getVersion()<2)return result;
                try(Cursor cursor=db.rawQuery("SELECT payload FROM manual_copy_receipts",null)) {
                    while(cursor.moveToNext()){JSONObject r=new JSONObject(cursor.getString(0));if(!"FAILED".equals(r.getString("state")))result.add(r);}
                }
            }return result;
        }
    }
    private ManualMediaAssignment assignment(SQLiteDatabase db,String key)throws Exception {
        try(Cursor cursor=db.rawQuery("SELECT payload FROM manual_assignments WHERE asset_key=?",new String[]{key})) {
            return cursor.moveToFirst()?ManualMediaAssignment.fromJson(new JSONObject(cursor.getString(0))):null;
        }
    }
    private JSONObject receipt(SQLiteDatabase db,String id)throws Exception {
        try(Cursor row=db.rawQuery("SELECT payload FROM manual_copy_receipts WHERE receipt_id=?",new String[]{id})) {
            if(!row.moveToFirst())throw new IllegalStateException("복사 증거를 확인하지 못했어요.");return new JSONObject(row.getString(0));
        }
    }
    private void saveReceipt(SQLiteDatabase db,JSONObject receipt)throws Exception {
        db.execSQL("UPDATE manual_copy_receipts SET payload=? WHERE receipt_id=?",new Object[]{receipt.toString(),receipt.getString("id")});
    }
}
