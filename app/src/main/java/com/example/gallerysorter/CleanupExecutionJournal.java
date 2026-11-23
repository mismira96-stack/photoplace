package com.example.gallerysorter;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Single active session, fsynced before opening the system dialog. Excluded from backups. */
final class CleanupExecutionJournal {
    static final Object LOCK = new Object();
    static final class Session {
        String id = java.util.UUID.randomUUID().toString();
        final CleanupExecutionPlan plan;
        int offset;
        int restoreOffset;
        String state = "READY";
        String reason = "";
        boolean restoring;
        Session(CleanupExecutionPlan plan) { this.plan = plan; }
        boolean active() { return !"DONE".equals(state) && !"CANCELED".equals(state); }
        Session forContinuation() {
            if(restoring || !("READY".equals(state)||"PAUSED".equals(state)))
                throw new IllegalStateException("작업 상태를 다시 확인해 주세요.");
            if(plan.batchSize==CleanupExecutionPlan.BATCH_SIZE)return this;
            // Explicit continuation only: rotate identity so delayed old workers cannot audit a larger batch.
            Session updated=new Session(new CleanupExecutionPlan(plan.proofs,CleanupExecutionPlan.BATCH_SIZE));
            updated.offset=offset;updated.restoreOffset=restoreOffset;updated.state=state;updated.reason=reason;
            return updated;
        }
    }
    private final AtomicFile file;
    CleanupExecutionJournal(Context context) {
        file = new AtomicFile(new File(context.getNoBackupFilesDir(), "cleanup_execution_v1.json"));
    }
    Session read() throws Exception {
        synchronized (LOCK) { return readLocked(); }
    }
    private Session readLocked() throws Exception {
        if (!file.getBaseFile().exists() && !new File(file.getBaseFile() + ".bak").exists()) return null;
        if (file.getBaseFile().length() > 64L * 1024 * 1024 || new File(file.getBaseFile() + ".bak").length() > 64L * 1024 * 1024)
            throw new IllegalStateException("Cleanup journal exceeds memory budget");
        JSONObject root = new JSONObject(new String(file.readFully(), StandardCharsets.UTF_8));
        if (root.getInt("version") != 1) throw new IllegalStateException("Unknown cleanup session");
        List<CleanupExecutionPlan.Proof> proofs = new ArrayList<>();
        JSONArray rows = root.getJSONArray("proofs");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject p = rows.getJSONObject(i);
            CleanupEvidenceRecord r = new CleanupEvidenceRecord(p.getString("job"), p.getString("source"),
                    p.getString("target"), p.getString("path"), p.getLong("taken"), p.getLong("size"), p.getLong("observed"));
            proofs.add(new CleanupExecutionPlan.Proof(r, p.getString("hash"), p.getString("sourceKey"),
                    p.getString("targetKey"), p.getString("sourceIdentity"), p.getString("targetIdentity")));
        }
        Session s = new Session(new CleanupExecutionPlan(proofs,storedBatchSize(root)));
        // Legacy v1 journals acquire a stable token on their next atomic save.
        s.id = root.optString("sessionId", "legacy-v1");
        s.offset = root.getInt("offset"); s.state = root.getString("state"); s.restoring = root.getBoolean("restoring");
        s.restoreOffset = root.optInt("restoreOffset", 0);
        s.reason = root.optString("reason", "");
        if (s.offset < 0 || s.offset > s.plan.sources.size() || s.restoreOffset < 0 || s.restoreOffset > s.plan.sources.size() || !java.util.Arrays.asList(
                "READY", "REQUESTED", "AUDIT_PENDING", "PAUSED", "NEEDS_RECOVERY", "DONE", "CANCELED").contains(s.state))
            throw new IllegalStateException("Invalid cleanup session state");
        return s;
    }
    static int storedBatchSize(JSONObject root) throws Exception {
        if(!root.has("batchSize"))return CleanupExecutionPlan.LEGACY_BATCH_SIZE;
        Object value=root.get("batchSize");
        if(!(value instanceof Integer || value instanceof Long))throw new IllegalStateException("Invalid batch size metadata");
        long count=((Number)value).longValue();
        if(count<1||count>2000)throw new IllegalStateException("Invalid batch size metadata");
        return (int)count;
    }
    void save(Session s) throws Exception {
        synchronized (LOCK) { saveLocked(s); }
    }
    private void saveLocked(Session s) throws Exception {
        JSONArray rows = new JSONArray();
        for (CleanupExecutionPlan.Proof p : s.plan.proofs) {
            CleanupEvidenceRecord r = p.record;
            rows.put(new JSONObject().put("job", r.jobId).put("source", r.sourceUri).put("target", r.targetUri)
                    .put("path", r.targetRelativePath).put("taken", r.takenAtMillis).put("size", r.copiedBytes)
                    .put("observed", r.observedAtMillis).put("hash", p.hash).put("sourceKey", p.sourceKey)
                    .put("targetKey", p.targetKey).put("sourceIdentity", p.sourceIdentity).put("targetIdentity", p.targetIdentity));
        }
        byte[] bytes = new JSONObject().put("version", 1).put("batchSize",s.plan.batchSize).put("sessionId", s.id).put("offset", s.offset).put("state", s.state)
                .put("restoreOffset", s.restoreOffset)
                .put("reason", s.reason)
                .put("restoring", s.restoring).put("proofs", rows).toString().getBytes(StandardCharsets.UTF_8);
        FileOutputStream out = null;
        try { out = file.startWrite(); out.write(bytes); out.getFD().sync(); file.finishWrite(out); }
        catch (Exception failed) { if (out != null) file.failWrite(out); throw failed; }
    }
}
