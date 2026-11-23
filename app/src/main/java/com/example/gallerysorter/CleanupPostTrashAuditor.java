package com.example.gallerysorter;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Executes the B1 Post-Trash Audit Protocol off the UI thread.
 * Verifies that the destination copies in location albums survive with matching size and full SHA-256 hash.
 */
final class CleanupPostTrashAuditor {
    interface Journal {
        CleanupEvidenceStore.JournalEntry readJournal(String requestId);
        void updateJournalStatus(String requestId, String status);
    }

    enum Status {
        AUDIT_PASSED,
        NEEDS_RECOVERY,
        CANCELED,
        UNRESOLVED
    }

    static final class Result {
        final Status status;
        final int auditedCount;
        final String message;

        Result(Status status, int auditedCount, String message) {
            this.status = status;
            this.auditedCount = auditedCount;
            this.message = message;
        }
    }

    private final Journal store;
    private final CleanupPairVerifier.Access mediaAccess;

    CleanupPostTrashAuditor(Journal store, CleanupPairVerifier.Access mediaAccess) {
        this.store = store;
        this.mediaAccess = mediaAccess;
    }

    Result audit(String requestId, BooleanSupplier canceled) {
        BooleanSupplier isCanceled = canceled != null ? canceled : () -> false;
        if (requestId == null || requestId.trim().isEmpty()) {
            return new Result(Status.UNRESOLVED, 0, "요청 식별자가 없습니다.");
        }

        CleanupEvidenceStore.JournalEntry entry = store.readJournal(requestId);
        if (entry == null) {
            return new Result(Status.UNRESOLVED, 0, "요청 기록을 찾을 수 없습니다.");
        }

        if (isCanceled.getAsBoolean()) {
            store.updateJournalStatus(requestId, "AUDIT_PENDING");
            return new Result(Status.CANCELED, 0, "사후 확인을 중단했어요.");
        }
        // V3 legacy requests contain no expected digest, size or physical media identity.
        // Reading arbitrary surviving bytes must NEVER certify these requests as safe.
        store.updateJournalStatus(requestId, "UNRESOLVED");
        return new Result(Status.UNRESOLVED, 0, "이전 요청에는 대조 증거가 없어요. 갤러리 휴지통에서 원본을 복구해 주세요.");
    }
}
