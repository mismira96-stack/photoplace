package com.example.gallerysorter;

/** An evaluated candidate original photo with verified copy match and dependency status. */
final class CleanupCandidate {
    final CleanupEvidenceRecord evidence;
    final CleanupPairVerifier.Status contentStatus;
    final String contentSha256;
    final MemoryDependencyGuard.Status dependencyStatus;

    CleanupCandidate(CleanupEvidenceRecord evidence,
                     CleanupPairVerifier.Status contentStatus,
                     String contentSha256,
                     MemoryDependencyGuard.Status dependencyStatus) {
        if (evidence == null || contentStatus == null || dependencyStatus == null) {
            throw new IllegalArgumentException("Evidence and statuses required");
        }
        this.evidence = evidence;
        this.contentStatus = contentStatus;
        this.contentSha256 = contentSha256 == null ? "" : contentSha256;
        this.dependencyStatus = dependencyStatus;
    }

    boolean isEligible() {
        return contentStatus == CleanupPairVerifier.Status.MATCH
                && dependencyStatus == MemoryDependencyGuard.Status.PRESERVED;
    }

    String formatBytes() {
        long bytes = evidence.copiedBytes;
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(java.util.Locale.KOREA, "%.1f KB", bytes / 1024.0);
        return String.format(java.util.Locale.KOREA, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    String diagnosticMessage() {
        if (isEligible()) {
            return "정리 가능 (내용 일치 및 기록 보존 확인)";
        }
        switch (contentStatus) {
            case SIZE_MISMATCH:
                return "크기 불일치 (복사본이 수정되었거나 손상됨)";
            case CONTENT_MISMATCH:
                return "내용 불일치 (복사본 내용이 원본과 다름)";
            case CHANGED:
                return "파일 변경 감지 (검사 중 파일이 변경됨)";
            case UNREADABLE:
                return "읽기 불가 (원본 또는 복사본을 읽을 수 없음)";
            case CANCELED:
                return "검사 취소됨";
            case MATCH:
                break;
        }
        switch (dependencyStatus) {
            case DATE_SECTION_CHANGED:
                return "날짜 불일치 (정리 시 날짜별 메모/목록 위치가 변경됨)";
            case COLLECTION_DEPENDENCY:
                return "모음 의존성 (모음 또는 대표사진으로 사용 중)";
            case CYCLIC_SELECTION:
                return "순환 대상 (선택 항목 간 순환 참조)";
            case SOURCE_ONLY:
                return "복사본 미투영 (정리 시 사진이 목록에서 누락됨)";
            case UNVERIFIED_IDENTITY:
                return "기록 연결 검토가 아직 완료되지 않았어요";
            case SNAPSHOT_CHANGED:
                return "검사 중 사진·기록이 바뀌어 다시 확인이 필요해요";
            case PRESERVED:
                break;
        }
        return "정리 보류 (안전 검증 미통과)";
    }
}
