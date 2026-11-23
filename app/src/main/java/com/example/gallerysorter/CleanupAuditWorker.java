package com.example.gallerysorter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Durable, read-only verification after OS approval. Never launches a trash request. */
public final class CleanupAuditWorker extends Worker {
    private long lastProgress;
    static String enqueue(Context c, CleanupExecutionJournal.Session s) {
        String name = "cleanup-audit-" + s.id + "-" + s.offset + "-" + s.restoreOffset + "-" + s.restoring;
        Data input = new Data.Builder().putString("session", s.id).putInt("offset", s.offset)
                .putInt("restoreOffset", s.restoreOffset).putBoolean("restoring", s.restoring).build();
        WorkManager.getInstance(c).enqueueUniqueWork(name, ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(CleanupAuditWorker.class).setInputData(input).build());
        return name;
    }
    public CleanupAuditWorker(@NonNull Context c, @NonNull WorkerParameters p) { super(c,p); }
    @NonNull @Override public Result doWork() {
        Context c = getApplicationContext();
        try {
            // Audit is a regular scheduled Worker. It does not require foreground-service promotion.
            CleanupWorkNotification.update(c,"복사본 보존을 확인하고 있어요",0,0);
            synchronized (CleanupExecutionJournal.LOCK) {
                CleanupExecutionJournal journal = new CleanupExecutionJournal(c);
                CleanupExecutionJournal.Session s = journal.read();
                if (!matches(s)) return Result.success();
                if (isStopped()) return Result.retry();
                CleanupSessionAudit.inspect(s,new CleanupMediaAccess(c),proofs ->
                        CleanupDependencyRetry.verify(()->CleanupExecutionPreflight.verifyDependencies(c,proofs),this::isStopped,Thread::sleep), (done,total) -> {
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (done != total && now - lastProgress < 750) return;
                    lastProgress = now;
                    setProgressAsync(new Data.Builder().putInt("checked",done).putInt("total",total).build());
                    CleanupWorkNotification.update(c,"현재 묶음 복사본 확인 " + done + " / " + total,done,total);
                });
                // A stopped worker must not commit partially inspected state.
                if (isStopped()) return Result.retry();
                journal.save(s);
                if ("NEEDS_RECOVERY".equals(s.state)) CleanupRecoveryNotifier.show(c);
                else {
                    CleanupRecoveryNotifier.clear(c);
                    CleanupWorkNotification.ready(c, s.offset, s.plan.sources.size(), "DONE".equals(s.state));
                }
            }
            return Result.success();
        } catch (Exception failed) {
            android.util.Log.w("CleanupExecution", "Durable audit deferred; journal retained", failed);
            return CleanupDependencyRetry.retryWorker(getRunAttemptCount()) ? Result.retry() : Result.failure();
        } finally {
            CleanupWorkNotification.clearProgress(c);
        }
    }
    private boolean matches(CleanupExecutionJournal.Session s) {
        return s != null && s.id.equals(getInputData().getString("session"))
                && s.offset == getInputData().getInt("offset", -1)
                && s.restoreOffset == getInputData().getInt("restoreOffset", -1)
                && s.restoring == getInputData().getBoolean("restoring", false);
    }
}
