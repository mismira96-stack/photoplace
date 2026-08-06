package com.example.gallerysorter;

import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SortWorker extends Worker {
    static final String WORK_NAME = "photoplace_sort_work";
    private static final long NOTIFICATION_UPDATE_INTERVAL_MS = 1000L;
    private long lastNotificationUpdateMillis = 0L;
    private int lastNotificationPercent = -1;

    public SortWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        final SortInputStore inputStore = new SortInputStore(context);
        SortInputStore.Snapshot input = inputStore.read();
        if (input.items.isEmpty()) {
            SortProgressStore.finish(context);
            return Result.success();
        }
        final String label = "앨범으로 정리 중";
        SortProgressStore.start(context, label);
        setForegroundAsync(foregroundInfo(label, 0, input.items.size(), ""));
        SortNotificationHelper.notifyProgress(context, label, 0, input.items.size(), "");
        SortJob sortJob = new SortJob(new MediaCopyEngine(context), new SortJob.CancelSignal() {
            @Override
            public boolean isCanceled() {
                return SortWorker.this.isStopped();
            }
        }, new SortJobProgressListener() {
            @Override
            public void onItem(int current, int total, PhotoItem item) {
                String progressContext = item == null || item.noLocation ? "정리 제외: 위치 정보 없음" : item.locationKey;
                SortProgressStore.update(context, label, current, total, progressContext);
                if (shouldUpdateNotification(current, total)) {
                    setForegroundAsync(foregroundInfo(label, current, total, progressContext));
                    SortNotificationHelper.notifyProgress(context, label, current, total, progressContext);
                }
            }
        });
        SortJobResult result = sortJob.run(input.items, input.shouldMoveVideos);
        try {
            new SortResultStore(context).write(result);
            inputStore.clear();
        } catch (Exception unused) {
            SortProgressStore.finish(context);
            return Result.retry();
        }
        SortProgressStore.finish(context);
        SortForegroundService.complete(
                context,
                result.canceled ? "PhotoPlace 정리가 멈췄습니다" : "PhotoPlace 정리가 완료되었습니다",
                result.canceled ? "남은 사진을 이어서 정리할 수 있어요." : "정리 " + result.copiedCount + "개 · 건너뜀 " + result.skippedCount + "개 · 실패 " + result.failedCount + "개");
        return Result.success();
    }

    private boolean shouldUpdateNotification(int current, int total) {
        long now = System.currentTimeMillis();
        int percent = total > 0 ? Math.min(100, Math.max(0, (int) ((((long) current) * 100L) / ((long) total)))) : -1;
        if (current <= 0 || (total > 0 && current >= total)) {
            lastNotificationUpdateMillis = now;
            lastNotificationPercent = percent;
            return true;
        }
        if (percent != lastNotificationPercent) {
            lastNotificationUpdateMillis = now;
            lastNotificationPercent = percent;
            return true;
        }
        if (now - lastNotificationUpdateMillis >= NOTIFICATION_UPDATE_INTERVAL_MS) {
            lastNotificationUpdateMillis = now;
            return true;
        }
        return false;
    }

    private ForegroundInfo foregroundInfo(String label, int current, int total, String progressContext) {
        if (Build.VERSION.SDK_INT >= 29) {
            return new ForegroundInfo(
                    SortForegroundService.NOTIFICATION_PROGRESS_ID,
                    SortNotificationHelper.progressNotification(getApplicationContext(), label, current, total, progressContext),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }
        return new ForegroundInfo(
                SortForegroundService.NOTIFICATION_PROGRESS_ID,
                SortNotificationHelper.progressNotification(getApplicationContext(), label, current, total, progressContext));
    }
}
