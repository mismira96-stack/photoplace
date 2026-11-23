package com.example.gallerysorter;

import android.app.Activity;
import java.util.function.Consumer;

/**
 * Feature gate for Original Cleanup.
 * Routes review requests and system trash results to the isolated CleanupController.
 */
final class CleanupFeatureGate {
    private static volatile boolean enabled = true;

    private CleanupFeatureGate() { }

    static boolean isEnabled() { return enabled; }

    static void setEnabledForTesting(boolean value) { enabled = value; }

    static boolean hasActionablePending(boolean trashCompleted, int pendingCount) {
        // Old SortResult counts are not verified cleanup candidates. Keep that home banner retired.
        return false;
    }

    static void onLegacyRequest(Activity activity, Consumer<String> notice) {
        if (!isEnabled()) {
            if (notice != null) notice.accept("원본 정리는 안전성 검증을 준비 중이라 현재 사용할 수 없어요.");
            return;
        }
        if (activity == null) {
            if (notice != null) notice.accept("화면을 확인할 수 없어 원본 정리를 시작할 수 없어요.");
            return;
        }
        if (android.os.Build.VERSION.SDK_INT < 30) {
            if (notice != null) notice.accept("원본 휴지통 정리는 Android 11 이상에서 지원해요.");
            return;
        }
        CleanupController.getInstance().startReview(activity, notice);
    }

    static void onLegacyRequest(Consumer<String> notice) {
        onLegacyRequest(null, notice);
    }

    static void onLegacyResult(Activity activity, int resultCode, Consumer<String> notice) {
        // A feature kill switch must never suppress auditing an already launched request.
        if (activity == null) {
            if (notice != null) notice.accept("화면을 확인할 수 없어 원본 정리를 시작할 수 없어요.");
            return;
        }
        CleanupController.getInstance().onTrashResult(activity, resultCode, notice);
    }

    static void onLegacyResult(int resultCode, Consumer<String> notice) {
        onLegacyResult(null, resultCode, notice);
    }
}
