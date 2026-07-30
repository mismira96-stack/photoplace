package com.example.gallerysorter;

import android.net.Uri;

import java.util.List;

final class SortJob {
    interface CancelSignal {
        boolean isCanceled();
    }

    private final MediaCopyEngine copyEngine;
    private final CancelSignal cancelSignal;
    private final SortJobProgressListener progressListener;

    SortJob(MediaCopyEngine copyEngine, CancelSignal cancelSignal, SortJobProgressListener progressListener) {
        this.copyEngine = copyEngine;
        this.cancelSignal = cancelSignal;
        this.progressListener = progressListener;
    }

    SortJobResult run(List<PhotoItem> items, boolean shouldMoveVideos) {
        SortJobResult result = new SortJobResult();
        if (items == null) {
            return result;
        }
        for (int i = 0; i < items.size(); i++) {
            if (cancelSignal != null && cancelSignal.isCanceled()) {
                result.canceled = true;
                result.log.append("사용자 멈춤\n");
                break;
            }
            PhotoItem item = items.get(i);
            if (progressListener != null) {
                progressListener.onItem(i + 1, items.size(), item);
            }
            processItem(item, shouldMoveVideos, result);
        }
        return result;
    }

    private void processItem(PhotoItem item, boolean shouldMoveVideos, SortJobResult result) {
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
            if (!item.video) {
                addUniqueUri(result.copiedOriginalUris, item.uri);
            }
            result.log.append("복사본 있음, 건너뜀: ").append(item.name).append(" -> ").append(item.targetRelativePath).append("\n");
            return;
        }
        try {
            copyEngine.copy(item);
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
        }
    }

    private void addUniqueUri(List<Uri> uris, Uri uri) {
        if (uri != null && !uris.contains(uri)) {
            uris.add(uri);
        }
    }
}
