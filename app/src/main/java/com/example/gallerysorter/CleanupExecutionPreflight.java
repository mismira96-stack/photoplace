package com.example.gallerysorter;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Worker-only final verification. Any change rejects the whole selected set. */
final class CleanupExecutionPreflight {
    interface Progress { void checked(int done, int total); }
    static CleanupExecutionPlan prepare(Context context, List<CleanupCandidate> selected,
                                        List<CleanupCandidate> all, java.util.function.BooleanSupplier canceled, Progress progress) throws Exception {
        Set<String> sources = new HashSet<>();
        for (CleanupCandidate c : selected) {
            if (!c.isEligible()) throw new IllegalStateException("확인이 필요한 사진이 선택되어 있어요.");
            sources.add(c.evidence.sourceUri);
        }
        List<CleanupExecutionPlan.Proof> proofs = new ArrayList<>();
        CleanupMediaAccess access = new CleanupMediaAccess(context);
        CleanupPairVerifier verifier = new CleanupPairVerifier(access);
        Set<String> seen = new HashSet<>();
        int total = 0;
        for (CleanupCandidate c : all) if (sources.contains(c.evidence.sourceUri) && c.isEligible()) total++;
        for (CleanupCandidate c : all) if (sources.contains(c.evidence.sourceUri) && c.isEligible()) {
            CleanupEvidenceRecord r = c.evidence;
            if (!seen.add(r.sourceUri + "\u0000" + r.targetUri)) continue;
            CleanupPairVerifier.Result check = verifier.verify(r, canceled);
            if (check.status != CleanupPairVerifier.Status.MATCH || !check.sha256.equals(c.contentSha256))
                throw new IllegalStateException("검토 후 사진이 바뀌었어요. 원본 검토를 다시 해 주세요.");
            proofs.add(new CleanupExecutionPlan.Proof(r, check.sha256, access.key(r.sourceUri), access.key(r.targetUri),
                    access.metadata(r.sourceUri).identity, access.metadata(r.targetUri).identity));
            if (progress != null) progress.checked(proofs.size(), total);
        }
        CleanupExecutionPlan plan = new CleanupExecutionPlan(proofs);
        if (!sources.equals(new HashSet<>(plan.sources))) throw new IllegalStateException("선택 증거가 부족해요.");
        verifyDependencies(context, proofs);
        return plan;
    }

    static String verifyBatch(Context context, CleanupExecutionPlan plan, int offset) throws Exception {
        return verifyBatch(context,plan,offset,null);
    }
    static String verifyBatch(Context context,CleanupExecutionPlan plan,int offset,Progress progress) throws Exception {
        String revision = revision(context);
        if (revision == null) throw new IllegalStateException("사진 접근 권한을 확인해 주세요.");
        CleanupMediaAccess access = new CleanupMediaAccess(context);
        List<CleanupExecutionPlan.Proof> batch = plan.batch(offset);
        int checked=0;
        for (CleanupExecutionPlan.Proof p : batch) {
            if (!p.sourceKey.equals(access.key(p.record.sourceUri)) || !p.targetKey.equals(access.key(p.record.targetUri))
                    || !CleanupExecutionAudit.matches(p.record.sourceUri, p.record.copiedBytes, p.hash, p.sourceIdentity, access)
                    || !CleanupExecutionAudit.matches(p.record.targetUri, p.record.copiedBytes, p.hash, p.targetIdentity, access))
                throw new IllegalStateException("사진이 변경되어 중단했어요. 다시 검토해 주세요.");
            if(progress!=null)progress.checked(++checked,batch.size());
        }
        verifyDependencies(context, batch);
        if (!revision.equals(revision(context))) throw new IllegalStateException("검증 중 사진이나 기록이 바뀌었어요.");
        return revision;
    }

    static String revision(Context c) {
        String key = new HomeRevisionReader(c).read();
        java.io.File notes = new java.io.File(c.getFilesDir(), "memory_date_notes.json");
        return key == null ? null : key + "|" + notes.length() + ":" + notes.lastModified();
    }

    static String verifyDependencies(Context context, List<CleanupExecutionPlan.Proof> proofs) throws Exception {
        List<CleanupEvidenceRecord> records = new ArrayList<>();
        for (CleanupExecutionPlan.Proof p : proofs) records.add(p.record);
        CleanupMemorySnapshotReader reader = new CleanupMemorySnapshotReader(context, records);
        for (CleanupEvidenceRecord r : records)
            if ("historical".equals(r.jobId) && !reader.knownOutputPath(r.targetRelativePath))
                throw new IllegalStateException("앱에서 만든 앨범인지 확인하지 못했어요. 해당 사진은 정리에서 제외해 주세요.");
        MemoryDependencyGuard.Status status=MemoryDependencyGuard.check(records,reader.project(records,false),reader.project(records,true));
        if(!reader.isCurrent())throw new CleanupDependencyRetry.RevisionChanged();
        if(status != MemoryDependencyGuard.Status.PRESERVED)
            throw new IllegalStateException("발견 기록이나 메모 연결이 바뀌었어요. 다시 검토해 주세요.");
        return reader.token;
    }
}
