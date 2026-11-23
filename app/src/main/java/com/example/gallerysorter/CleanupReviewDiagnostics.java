package com.example.gallerysorter;

import java.util.EnumMap;
import java.util.Map;

/** Candidate pairs are not the device's total duplicate-original count. */
final class CleanupReviewDiagnostics {
    static String text(CleanupReviewAssessment assessment, int discovered) {
        Map<CleanupPairVerifier.Status, Integer> content = new EnumMap<>(CleanupPairVerifier.Status.class);
        Map<MemoryDependencyGuard.Status, Integer> dependencies = new EnumMap<>(MemoryDependencyGuard.Status.class);
        for (CleanupCandidate candidate : assessment.candidates) {
            content.put(candidate.contentStatus, content.getOrDefault(candidate.contentStatus, 0) + 1);
            MemoryDependencyGuard.Status local = assessment.blockedSources.get(candidate.evidence.sourceUri);
            if (candidate.contentStatus == CleanupPairVerifier.Status.MATCH && local != null)
                dependencies.put(local, dependencies.getOrDefault(local, 0) + 1);
        }
        StringBuilder result = new StringBuilder("확인한 원본 사진 ").append(number(assessment.totalCount))
                .append("장\n같은 내용의 복사본 있음 ").append(number(assessment.matchedCount))
                .append("장\n기록 연결까지 확인 ").append(number(assessment.eligibleCount)).append("장");
        for (Map.Entry<CleanupPairVerifier.Status, Integer> entry : content.entrySet()) {
            if (entry.getKey() == CleanupPairVerifier.Status.MATCH) continue;
            String label;
            switch (entry.getKey()) {
                case SIZE_MISMATCH: label = "크기 다름"; break;
                case CONTENT_MISMATCH: label = "내용 다름"; break;
                case CHANGED: label = "검사 중 변경"; break;
                case UNREADABLE: label = "읽기 확인 불가"; break;
                default: label = "검사 취소";
            }
            result.append("\n").append(label).append(' ').append(number(entry.getValue())).append("장");
        }
        for (Map.Entry<MemoryDependencyGuard.Status, Integer> entry : dependencies.entrySet()) {
            if (entry.getKey() == MemoryDependencyGuard.Status.PRESERVED) continue;
            result.append("\n").append(dependencyLabel(entry.getKey())).append(' ')
                    .append(number(entry.getValue())).append("장");
        }
        int identified = 0;
        for (Integer count : dependencies.values()) identified += count;
        int pending = Math.max(0, assessment.matchedCount - assessment.eligibleCount - identified);
        if (pending > 0) result.append("\n전체 연결 검토가 끝나지 않은 사진 ").append(number(pending)).append("장");
        if (assessment.selectionStatus == MemoryDependencyGuard.Status.SNAPSHOT_CHANGED)
            result.append("\n검사 중 사진이나 기록이 바뀌었어요. 다시 확인해 주세요.");
        return result.append("\n\n같은 원본은 한 번만 셌어요.\n사진은 선택하고 시스템에서 확인하기 전까지 이동하지 않아요.").toString();
    }

    static String number(int value) { return String.format(java.util.Locale.KOREA, "%,d", value); }

    static String dependencyLabel(MemoryDependencyGuard.Status status) {
        switch (status) {
            case DATE_SECTION_CHANGED: return "날짜 구성 확인 필요";
            case COLLECTION_DEPENDENCY: return "모음·대표사진 연결 확인 필요";
            case CYCLIC_SELECTION: return "선택 항목 간 참조 확인 필요";
            case SOURCE_ONLY: return "복사본의 사진 보기 연결 확인 필요";
            case SNAPSHOT_CHANGED: return "검사 중 기록 변경";
            default: return "기록 연결 확인 불가";
        }
    }
}
