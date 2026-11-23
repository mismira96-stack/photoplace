package com.example.gallerysorter;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Full byte audit plus actual MediaStore state, never RESULT_OK alone. */
final class CleanupExecutionAudit {
    interface Access extends CleanupPairVerifier.Access {
        String key(String uri) throws Exception;
        boolean trashed(String uri) throws Exception;
    }
    enum Outcome { TRASHED, NOT_APPLIED, NEEDS_RECOVERY }
    static final class Report {
        final Outcome outcome;
        final String reason;
        Report(Outcome outcome, String reason) { this.outcome = outcome; this.reason = reason; }
    }
    static Outcome audit(List<CleanupExecutionPlan.Proof> proofs, Access access) {
        return inspect(proofs, access).outcome;
    }
    static Report inspect(List<CleanupExecutionPlan.Proof> proofs, Access access) {
        return inspect(proofs, access, null);
    }
    interface Progress { void checked(int done, int total); }
    static Report inspect(List<CleanupExecutionPlan.Proof> proofs, Access access, Progress progress) {
        if (proofs == null || proofs.isEmpty()) return new Report(Outcome.NEEDS_RECOVERY, "MISSING_RECEIPT");
        try {
            int trashed = 0;
            Set<String> sources = new HashSet<>();
            int checked = 0;
            for (CleanupExecutionPlan.Proof p : proofs) {
                if (!p.targetKey.equals(access.key(p.record.targetUri)))
                    return new Report(Outcome.NEEDS_RECOVERY, "TARGET_IDENTITY_CHANGED");
                if (!matches(p.record.targetUri, p.record.copiedBytes, p.hash, p.targetIdentity, access))
                    return new Report(Outcome.NEEDS_RECOVERY, "TARGET_CONTENT_UNVERIFIED");
                if (sources.add(p.sourceKey)) {
                    if (!p.sourceKey.equals(access.key(p.record.sourceUri))) return new Report(Outcome.NEEDS_RECOVERY, "SOURCE_IDENTITY_CHANGED");
                    if (access.trashed(p.record.sourceUri)) trashed++;
                    else if (!matches(p.record.sourceUri, p.record.copiedBytes, p.hash, null, access))
                        return new Report(Outcome.NEEDS_RECOVERY, "SOURCE_CONTENT_UNVERIFIED");
                }
                if (progress != null) progress.checked(++checked, proofs.size());
            }
            if (trashed == sources.size()) return new Report(Outcome.TRASHED, "");
            return trashed == 0 ? new Report(Outcome.NOT_APPLIED, "") : new Report(Outcome.NEEDS_RECOVERY, "PARTIAL_TRASH_STATE");
        } catch (Exception unknown) { return new Report(Outcome.NEEDS_RECOVERY, "MEDIA_STATE_UNAVAILABLE"); }
    }
    static String explanation(String reason) {
        if ("MEMORY_PROJECTION_CHANGED".equals(reason)) return "사진 내용은 확인했지만 발견 기록이나 메모 연결을 다시 확인하지 못했어요.";
        if ("TARGET_CONTENT_UNVERIFIED".equals(reason)) return "위치 앨범의 복사본 내용이나 파일 상태가 검토할 때와 달라졌어요.";
        if ("TARGET_IDENTITY_CHANGED".equals(reason) || "SOURCE_IDENTITY_CHANGED".equals(reason)) return "검토할 때와 사진 식별자가 달라졌어요.";
        if ("PARTIAL_TRASH_STATE".equals(reason)) return "이번 묶음의 일부 원본만 휴지통으로 이동한 상태예요.";
        return "복사본이나 원본의 휴지통 상태를 확인하지 못했어요.";
    }
    static boolean matches(String uri, long expectedSize, String expectedHash, String identity,
                           CleanupPairVerifier.Access access) throws Exception {
        if (expectedSize <= 0 || expectedHash == null || !expectedHash.matches("[0-9a-f]{64}")) return false;
        CleanupPairVerifier.Metadata before = access.metadata(uri);
        if (before == null || !before.readablePhoto || before.size != expectedSize
                || (identity != null && !identity.equals(before.identity))) return false;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long count = 0;
        try (InputStream input = access.open(uri)) {
            if (input == null) return false;
            byte[] buffer = new byte[65536]; int n;
            while ((n = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) return false;
                count += n;
                if (count > expectedSize) return false;
                digest.update(buffer, 0, n);
            }
        }
        return count == expectedSize && expectedHash.equals(CleanupPairVerifier.hex(digest.digest()))
                && before.same(access.metadata(uri));
    }
}
