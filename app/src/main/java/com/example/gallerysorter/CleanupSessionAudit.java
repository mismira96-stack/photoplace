package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Idempotent state transition shared by durable workers and recovery tests. No UI or writes. */
final class CleanupSessionAudit {
    interface Dependencies { void verify(List<CleanupExecutionPlan.Proof> proofs) throws Exception; }
    static void inspect(CleanupExecutionJournal.Session s, CleanupExecutionAudit.Access access,
                        Dependencies dependencies) throws Exception {
        inspect(s, access, dependencies, null);
    }
    static void inspect(CleanupExecutionJournal.Session s, CleanupExecutionAudit.Access access,
                        Dependencies dependencies, CleanupExecutionAudit.Progress progress) throws Exception {
        if ("DONE".equals(s.state) || "CANCELED".equals(s.state)
                || "READY".equals(s.state) || "PAUSED".equals(s.state)) return;
        if ("NEEDS_RECOVERY".equals(s.state)) {
            if (originalsRestored(s, access)) s.state = "CANCELED";
            return;
        }
        if (s.restoring) {
            if (s.restoreOffset >= affectedCount(s)) s.restoreOffset = 0;
            boolean restored = true;
            for (CleanupExecutionPlan.Proof p : restorationBatch(s)) {
                if (!p.sourceKey.equals(access.key(p.record.sourceUri)) || access.trashed(p.record.sourceUri)
                        || !CleanupExecutionAudit.matches(p.record.sourceUri, p.record.copiedBytes, p.hash, null, access)) restored = false;
            }
            if (restored) s.restoreOffset += Math.min(s.plan.batchSize, affectedCount(s) - s.restoreOffset);
            s.state = restored && s.restoreOffset == affectedCount(s) && originalsRestored(s, access)
                    ? "CANCELED" : "NEEDS_RECOVERY";
            return;
        }
        CleanupExecutionAudit.Report report = CleanupExecutionAudit.inspect(s.plan.batch(s.offset), access, progress);
        s.reason = report.reason;
        boolean previousGood = s.offset == 0
                || CleanupExecutionAudit.audit(completed(s), access) == CleanupExecutionAudit.Outcome.TRASHED;
        if (report.outcome == CleanupExecutionAudit.Outcome.TRASHED && previousGood) {
            try { dependencies.verify(affected(s)); }
            catch (Exception changed) { s.reason = "MEMORY_PROJECTION_CHANGED"; s.state = "NEEDS_RECOVERY"; return; }
            s.offset += s.plan.batchCount(s.offset);
            s.state = s.offset == s.plan.sources.size() ? "DONE" : "PAUSED";
        } else if (report.outcome == CleanupExecutionAudit.Outcome.NOT_APPLIED && previousGood)
            s.state = s.offset > 0 ? "PAUSED" : "CANCELED";
        else s.state = "NEEDS_RECOVERY";
    }
    static List<CleanupExecutionPlan.Proof> completed(CleanupExecutionJournal.Session s) {
        return selected(s, 0, s.offset);
    }
    static List<CleanupExecutionPlan.Proof> affected(CleanupExecutionJournal.Session s) {
        return selected(s, 0, affectedCount(s));
    }
    static int affectedCount(CleanupExecutionJournal.Session s) {
        return Math.min(s.plan.sources.size(), s.offset + s.plan.batchCount(s.offset));
    }
    static List<CleanupExecutionPlan.Proof> restorationBatch(CleanupExecutionJournal.Session s) {
        int offset = s.restoreOffset >= affectedCount(s) ? 0 : s.restoreOffset;
        return selected(s, offset, Math.min(affectedCount(s), offset + s.plan.batchSize));
    }
    private static List<CleanupExecutionPlan.Proof> selected(CleanupExecutionJournal.Session s, int from, int to) {
        Set<String> selected = new HashSet<>(s.plan.sources.subList(from, to));
        List<CleanupExecutionPlan.Proof> result = new ArrayList<>();
        for (CleanupExecutionPlan.Proof p : s.plan.proofs) if (selected.contains(p.record.sourceUri)) result.add(p);
        return result;
    }
    static boolean originalsRestored(CleanupExecutionJournal.Session s, CleanupExecutionAudit.Access access) {
        try {
            for (CleanupExecutionPlan.Proof p : affected(s))
                if (!p.sourceKey.equals(access.key(p.record.sourceUri)) || access.trashed(p.record.sourceUri)
                        || !CleanupExecutionAudit.matches(p.record.sourceUri, p.record.copiedBytes, p.hash, null, access)) return false;
            return true;
        } catch (Exception unknown) { return false; }
    }
}
