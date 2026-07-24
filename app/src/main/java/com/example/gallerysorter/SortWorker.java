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

    public SortWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SortProgressStore.start(context, "PhotoPlace 정리 준비 중");
        setForegroundAsync(foregroundInfo("PhotoPlace 정리 준비 중", 0, 0, ""));

        // The real sort engine still lives in MainActivity. This Worker is the
        // stable shell that will own the job after SortJob is extracted.
        SortProgressStore.finish(context);
        return Result.success();
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
