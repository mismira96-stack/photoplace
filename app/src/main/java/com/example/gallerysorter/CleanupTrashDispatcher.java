package com.example.gallerysorter;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Isolated dispatcher that creates MediaStore.createTrashRequest for user-confirmed original items.
 * Strictly separated from MainActivity to satisfy LegacyCleanupIsolationTest.
 */
final class CleanupTrashDispatcher {
    static final int MAX_BATCH_SIZE = 100;
    static final int REQUEST_CODE_TRASH = 21; // Matches REQUEST_DELETE_ORIGINALS

    interface DispatchCallback {
        void onSuccess(String requestId, int count);
        void onError(String message);
    }

    private final CleanupEvidenceStore store;

    CleanupTrashDispatcher(CleanupEvidenceStore store) {
        this.store = store;
    }

    void dispatch(Activity activity,
                  List<CleanupCandidate> candidates,
                  DispatchCallback callback) {
        // Legacy candidate-list requests lack durable, freshly verified receipts.
        if (callback != null) callback.onError("이전 검토 전용 요청은 실행할 수 없어요. 원본을 다시 검토해 주세요.");
    }
}
