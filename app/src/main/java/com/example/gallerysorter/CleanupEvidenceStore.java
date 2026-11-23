package com.example.gallerysorter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/** Private, additive ledger. No resolver/media access and no eligibility flag. Worker use only. */
final class CleanupEvidenceStore extends SQLiteOpenHelper implements CleanupEvidenceSession.Sink, CleanupReviewCoordinator.EvidenceLedger, CleanupPostTrashAuditor.Journal {
    static final String CREATE_TABLE = "CREATE TABLE copy_evidence ("
            + "sequence INTEGER PRIMARY KEY AUTOINCREMENT, record_id TEXT NOT NULL UNIQUE, "
            + "job_id TEXT NOT NULL, source_uri TEXT NOT NULL, target_uri TEXT NOT NULL, "
            + "target_path TEXT NOT NULL, taken_at INTEGER NOT NULL, copied_bytes INTEGER NOT NULL, "
            + "observed_at INTEGER NOT NULL, state TEXT NOT NULL CHECK(state='COPY_OBSERVED_UNVERIFIED'), content_sha256 TEXT, "
            + "CHECK(copied_bytes>=0), CHECK(source_uri<>target_uri), "
            + "UNIQUE(job_id, source_uri, target_uri))";

    CleanupEvidenceStore(Context context) {
        super(context.getApplicationContext(), "cleanup_evidence.db", null, 3);
    }

    @Override public void onConfigure(SQLiteDatabase database) {
        database.execSQL("PRAGMA synchronous=FULL");
    }

    @Override public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE);
        database.execSQL("CREATE INDEX evidence_job ON copy_evidence(job_id, sequence)");
        createChecks(database);
        createJournal(database);
    }

    @Override public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion >= 2) {
            createChecks(database);
        }
        if (oldVersion <= 2 && newVersion == 3) {
            createJournal(database);
            return;
        }
        throw new IllegalStateException("Unsupported evidence schema upgrade");
    }

    private void createChecks(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE cleanup_content_checks (record_id TEXT PRIMARY KEY, "
                + "status TEXT NOT NULL, sha256 TEXT, checked_at INTEGER NOT NULL)");
    }

    private void createJournal(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE cleanup_requests (request_id TEXT PRIMARY KEY, "
                + "batch_id TEXT NOT NULL, source_uris TEXT NOT NULL, target_uris TEXT NOT NULL, "
                + "status TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
    }

    void recordJournal(String requestId, String batchId, List<String> sourceUris, List<String> targetUris) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("request_id", requestId);
        values.put("batch_id", batchId);
        values.put("source_uris", String.join("\n", sourceUris));
        values.put("target_uris", String.join("\n", targetUris));
        values.put("status", "REQUESTED");
        long now = System.currentTimeMillis();
        values.put("created_at", now);
        values.put("updated_at", now);
        if (database.insertWithOnConflict("cleanup_requests", null, values, SQLiteDatabase.CONFLICT_REPLACE) < 0) {
            throw new IllegalStateException("Failed to record cleanup journal");
        }
    }

    @Override public void updateJournalStatus(String requestId, String status) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("updated_at", System.currentTimeMillis());
        database.update("cleanup_requests", values, "request_id=?", new String[]{requestId});
    }

    JournalEntry readLatestJournal() {
        try (Cursor cursor = getReadableDatabase().query("cleanup_requests",
                new String[]{"request_id", "batch_id", "source_uris", "target_uris", "status", "created_at", "updated_at"},
                null, null, null, null, "created_at DESC", "1")) {
            if (cursor.moveToFirst()) {
                return parseJournalEntry(cursor);
            }
        }
        return null;
    }

    @Override public JournalEntry readJournal(String requestId) {
        try (Cursor cursor = getReadableDatabase().query("cleanup_requests",
                new String[]{"request_id", "batch_id", "source_uris", "target_uris", "status", "created_at", "updated_at"},
                "request_id=?", new String[]{requestId}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return parseJournalEntry(cursor);
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static JournalEntry parseJournalEntry(Cursor cursor) {
        String reqId = cursor.getString(0);
        String batchId = cursor.getString(1);
        String[] sources = cursor.getString(2).split("\n");
        String[] targets = cursor.getString(3).split("\n");
        String status = cursor.getString(4);
        long createdAt = cursor.getLong(5);
        long updatedAt = cursor.getLong(6);
        List<String> srcList = new ArrayList<>();
        for (String s : sources) { if (!s.trim().isEmpty()) srcList.add(s); }
        List<String> tgtList = new ArrayList<>();
        for (String t : targets) { if (!t.trim().isEmpty()) tgtList.add(t); }
        return new JournalEntry(reqId, batchId, srcList, tgtList, status, createdAt, updatedAt);
    }

    void clearChecks(List<CleanupEvidenceRecord> records) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            for (CleanupEvidenceRecord record : records) {
                ContentValues values = new ContentValues();
                values.putNull("content_sha256");
                if (database.update("copy_evidence", values, "record_id=?", new String[]{record.recordId}) != 1)
                    throw new IllegalStateException("Evidence row changed");
                database.delete("cleanup_content_checks", "record_id=?", new String[]{record.recordId});
            }
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
    }

    @Override public void saveCheck(CleanupEvidenceRecord record, CleanupPairVerifier.Result result) {
        if (result.status == CleanupPairVerifier.Status.MATCH && !result.sha256.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid full content digest");
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            ContentValues content = new ContentValues();
            if (result.status == CleanupPairVerifier.Status.MATCH && result.sha256.matches("[0-9a-f]{64}"))
                content.put("content_sha256", result.sha256);
            else content.putNull("content_sha256");
            if (database.update("copy_evidence", content, "record_id=? AND source_uri=? AND target_uri=? AND copied_bytes=?",
                    new String[]{record.recordId, record.sourceUri, record.targetUri, Long.toString(record.copiedBytes)}) != 1)
                throw new IllegalStateException("Evidence row changed");
            ContentValues values = new ContentValues();
            values.put("record_id", record.recordId); values.put("status", result.status.name());
            values.put("sha256", content.getAsString("content_sha256"));
            values.put("checked_at", result.checkedAtMillis);
            if (database.insertWithOnConflict("cleanup_content_checks", null, values, SQLiteDatabase.CONFLICT_REPLACE) < 0)
                throw new IllegalStateException("Content check was not persisted");
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
    }

    @Override public CleanupPairVerifier.Result readCheck(String recordId) {
        try (Cursor cursor = getReadableDatabase().query("cleanup_content_checks",
                new String[]{"status", "sha256"}, "record_id=?", new String[]{recordId}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return new CleanupPairVerifier.Result(
                        CleanupPairVerifier.Status.valueOf(cursor.getString(0)),
                        cursor.getString(1));
            }
        } catch (Exception ignored) { }
        return null;
    }

    @Override public List<CleanupEvidenceRecord> readCandidates(String jobId) {
        ArrayList<CleanupEvidenceRecord> all = new ArrayList<>();
        long seq = 0;
        while (true) {
            Page page = readPage(jobId, seq, 100);
            if (page.records.isEmpty()) break;
            all.addAll(page.records);
            if (page.nextSequence <= seq) break;
            seq = page.nextSequence;
        }
        return Collections.unmodifiableList(all);
    }

    @Override public void commit(List<CleanupEvidenceRecord> records) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            for (CleanupEvidenceRecord record : records) {
                // Exact pair replay is idempotent; preserve the first observation.
                try (Cursor existing = database.query("copy_evidence", new String[]{"record_id"},
                        "record_id=?", new String[]{record.recordId}, null, null, null)) {
                    if (existing.moveToFirst()) continue;
                }
                ContentValues values = new ContentValues();
                values.put("record_id", record.recordId);
                values.put("job_id", record.jobId);
                values.put("source_uri", record.sourceUri);
                values.put("target_uri", record.targetUri);
                values.put("target_path", record.targetRelativePath);
                values.put("taken_at", record.takenAtMillis);
                values.put("copied_bytes", record.copiedBytes);
                values.put("observed_at", record.observedAtMillis);
                values.put("state", CleanupEvidenceRecord.STATE);
                // Stream bytes are an observation, not a separately verified target hash.
                values.putNull("content_sha256");
                database.insertOrThrow("copy_evidence", null, values);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void clearHistoricalRecords() {
        try {
            getWritableDatabase().delete("copy_evidence", "job_id=?", new String[]{"historical"});
        } catch (Exception ignored) { }
    }

    /** Bounded keyset page for a future background review worker. Errors propagate as unknown. */
    Page readPage(String jobId, long afterSequence, int limit) {
        ArrayList<CleanupEvidenceRecord> records = new ArrayList<>();
        long nextSequence = afterSequence;
        try (Cursor cursor = pageCursor(jobId, afterSequence, limit)) {
            while (cursor.moveToNext()) {
                records.add(new CleanupEvidenceRecord(cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getLong(5),
                        cursor.getLong(6), cursor.getLong(7)));
                nextSequence = cursor.getLong(0);
            }
        }
        return new Page(records, nextSequence);
    }

    // Cursor includes sequence so callers can advance without OFFSET or reading the whole ledger.
    private Cursor pageCursor(String jobId, long afterSequence, int limit) {
        String selection = "sequence>?" + (jobId == null ? "" : " AND job_id=?");
        String[] args = jobId == null ? new String[]{Long.toString(Math.max(0, afterSequence))}
                : new String[]{Long.toString(Math.max(0, afterSequence)), jobId};
        return getReadableDatabase().query("copy_evidence",
                new String[]{"sequence", "job_id", "source_uri", "target_uri", "target_path",
                        "taken_at", "copied_bytes", "observed_at"},
                selection, args, null, null, "sequence ASC", Integer.toString(Math.max(1, Math.min(100, limit))));
    }

    static final class Page {
        final List<CleanupEvidenceRecord> records;
        final long nextSequence;
        Page(List<CleanupEvidenceRecord> records, long nextSequence) {
            this.records = Collections.unmodifiableList(new ArrayList<>(records));
            this.nextSequence = nextSequence;
        }
    }

    static final class JournalEntry {
        final String requestId;
        final String batchId;
        final List<String> sourceUris;
        final List<String> targetUris;
        final String status;
        final long createdAt;
        final long updatedAt;

        JournalEntry(String requestId, String batchId, List<String> sourceUris,
                     List<String> targetUris, String status, long createdAt, long updatedAt) {
            this.requestId = requestId;
            this.batchId = batchId;
            this.sourceUris = Collections.unmodifiableList(sourceUris);
            this.targetUris = Collections.unmodifiableList(targetUris);
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
}
