package com.example.gallerysorter;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Main coordinator for the Original Cleanup feature.
 * Isolates all verification, dialog presentation, trash dispatch, and post-trash audit logic away from MainActivity.
 */
final class CleanupController {
    private static volatile CleanupController instance;

    static CleanupController getInstance() {
        if (instance == null) {
            synchronized (CleanupController.class) {
                if (instance == null) {
                    instance = new CleanupController();
                }
            }
        }
        return instance;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean reviewRunning = new AtomicBoolean(false);

    private CleanupController() { }

    void startReview(Activity activity, Consumer<String> toast) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (!reviewRunning.compareAndSet(false, true)) {
            if (toast != null) toast.accept("진행 중인 원본 검토를 먼저 마쳐 주세요.");
            return;
        }
        Context context = activity.getApplicationContext();

        AtomicBoolean canceled = new AtomicBoolean(false);
        java.util.function.BooleanSupplier isCanceled = () -> canceled.get() || activity.isFinishing() || activity.isDestroyed();
        CleanupProgressScreen progress = new CleanupProgressScreen(activity, () -> canceled.set(true));
        try { progress.show(); }
        catch (RuntimeException unavailable) {
            reviewRunning.set(false);
            if (toast != null) toast.accept("화면을 다시 열고 원본 검토를 시작해 주세요.");
            return;
        }

        executor.execute(() -> {
            try (CleanupEvidenceStore store = new CleanupEvidenceStore(context)) {
                if (CleanupExecutionController.active(context)) {
                    mainHandler.post(() -> {
                        try { if (progress.isShowing()) progress.dismiss(); }
                        catch (RuntimeException ignored) { }
                        if (isCanceled.getAsBoolean()) return;
                        if (toast != null) toast.accept("이전 원본 정리의 진행 상태를 확인하고 있어요.");
                        CleanupExecutionController.get().resume(activity, toast);
                    });
                    return;
                }
                // Review all discovered pairs; selection is reverified before execution.
                CleanupHistoricalScanner scanner = new CleanupHistoricalScanner(context, store);
                int discovered = scanner.scanAndRegister(0);

                // 2. Run two-tier verification coordinator
                CleanupMediaAccess mediaAccess = new CleanupMediaAccess(context);
                CleanupPairVerifier verifier = new CleanupPairVerifier(mediaAccess);
                CleanupReviewSnapshotProvider snapshotReader = new CleanupReviewSnapshotProvider(context, isCanceled, (done,total) -> {
                    mainHandler.post(() -> { if (!isCanceled.getAsBoolean()) progress.connectionUpdate(done,total); });
                });
                CleanupReviewCoordinator coordinator = new CleanupReviewCoordinator(store, verifier, snapshotReader);

                CleanupReviewAssessment assessment = coordinator.evaluate(null, new CleanupReviewCoordinator.ProgressListener() {
                    @Override public void onProgress(int checked, int total, CleanupCandidate candidate) {
                        if (checked % 5 != 0 && checked != total) return;
                        mainHandler.post(() -> {
                            if (!isCanceled.getAsBoolean()) progress.update(checked, total);
                        });
                    }
                    @Override public void onStage(String stage) {
                        mainHandler.post(() -> { if (!isCanceled.getAsBoolean()) progress.stage(stage); });
                    }
                }, isCanceled);

                java.util.Map<String, CleanupReviewModel.Source> sources = CleanupReviewSourceLoader.read(context, assessment.candidates, isCanceled);

                mainHandler.post(() -> {
                    try {
                        if (progress.isShowing()) progress.dismiss();
                    } catch (Exception ignored) { }

                    if (isCanceled.getAsBoolean()) {
                        return;
                    }

                    // Show review dialog
                    new CleanupReviewDialog(activity, assessment, new CleanupReviewDialog.Listener() {
                        @Override
                        public void onConfirm(List<CleanupCandidate> selected) {
                            CleanupExecutionController.get().start(activity, selected, assessment.evidenceCandidates, toast);
                        }

                        @Override
                        public void onCancel() {
                            if (toast != null) toast.accept("원본 정리를 취소했습니다.");
                        }
                    }, CleanupReviewDiagnostics.text(assessment, discovered), sources).show();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    try {
                        if (progress.isShowing()) progress.dismiss();
                    } catch (Exception ignored) { }
                    if (toast != null) {
                        if (!isCanceled.getAsBoolean()) toast.accept("원본 검토 중 오류가 발생했습니다: " + e.getMessage());
                    }
                });
            } finally {
                reviewRunning.set(false);
            }
        });
    }

    void onTrashResult(Activity activity, int resultCode, Consumer<String> toast) {
        CleanupExecutionController.get().result(activity, resultCode, toast);
    }
}
