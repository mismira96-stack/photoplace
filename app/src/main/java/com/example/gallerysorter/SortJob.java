package com.example.gallerysorter;

import android.net.Uri;

import java.util.List;

final class SortJob {
    interface CopyEngine {
        MediaCopyReceipt copy(PhotoItem item) throws Exception;
        CleanupEvidenceSession newEvidenceSession();
    }

    interface CancelSignal {
        boolean isCanceled();
    }

    private final CopyEngine copyEngine;
    private final CancelSignal cancelSignal;
    private final SortJobProgressListener progressListener;

    SortJob(CopyEngine copyEngine, CancelSignal cancelSignal, SortJobProgressListener progressListener) {
        this.copyEngine = copyEngine;
        this.cancelSignal = cancelSignal;
        this.progressListener = progressListener;
    }

    SortJobResult run(List<PhotoItem> items, boolean shouldMoveVideos) {
        SortJobResult result = new SortJobResult();
        if (items == null) {
            return result;
        }
        CleanupEvidenceSession evidence = copyEngine.newEvidenceSession();
        try {
            for (int i = 0; i < items.size(); i++) {
                if (cancelSignal != null && cancelSignal.isCanceled()) {
                    result.canceled = true;
                    result.log.append("사용자 멈춤\n");
                    break;
                }
                PhotoItem item = items.get(i);
                processItem(item, shouldMoveVideos, result, evidence);
                evidence.tick();
                if (progressListener != null) {
                    progressListener.onItem(i + 1, items.size(), item);
                }
            }
        } finally {
            evidence.close();
            result.evidenceJobId = evidence.jobId;
            result.evidenceRecordedCount = evidence.recordedCount();
            result.evidenceUnrecordedCount = evidence.unrecordedCount();
            result.evidenceFailureCount = evidence.failureCount();
            result.evidenceCheckpointMillis = evidence.checkpointMillis();
        }
        return result;
    }

    private void processItem(PhotoItem item, boolean shouldMoveVideos, SortJobResult result,
                             CleanupEvidenceSession evidence) {
        if (item == null) {
            result.skippedCount++;
            return;
        }
        if (item.noLocation) {
            result.skippedCount++;
            result.log.append("위치없음, 건너뜀: ").append(item.name).append("\n");
            return;
        }
        if (item.video && !shouldMoveVideos) {
            result.skippedCount++;
            result.log.append("동영상 이동 안 함, 건너뜀: ").append(item.name).append("\n");
            return;
        }
        if (item.duplicateInTarget) {
            result.skippedCount++;
            result.log.append("복사본 있음, 건너뜀: ").append(item.name).append(" -> ").append(item.targetRelativePath).append("\n");
            return;
        }
        MediaCopyReceipt receipt;
        try {
            receipt = copyEngine.copy(item);
            result.copiedCount++;
            result.sortedUris.add(item.uri);
            result.sortedItems.add(item);
            if (!item.video) {
                addUniqueUri(result.copiedOriginalUris, item.uri);
            }
            result.log.append(item.video ? "이동: " : "복사: ").append(item.name).append(" -> ").append(item.targetRelativePath).append("\n");
        } catch (Exception e) {
            result.failedCount++;
            result.log.append("실패: ").append(item.name).append(" / ").append(e.getMessage()).append("\n");
            return;
        }
        // Outside the copy try/catch: evidence failure cannot relabel or retry a successful copy.
        if (!item.video) {
            evidence.observe(item.uri == null ? "" : item.uri.toString(),
                    receipt.targetUri == null ? "" : receipt.targetUri.toString(),
                    item.targetRelativePath, item.takenAt == null ? 0L : item.takenAt.getTime(),
                    receipt.copiedBytes);
        }
    }

    private void addUniqueUri(List<Uri> uris, Uri uri) {
        if (uri != null && !uris.contains(uri)) {
            uris.add(uri);
        }
    }
}
