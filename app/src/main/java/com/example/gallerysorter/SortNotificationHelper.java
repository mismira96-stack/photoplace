package com.example.gallerysorter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

final class SortNotificationHelper {
    static final String CHANNEL_PROGRESS = "photoplace_sort_progress";
    static final String CHANNEL_COMPLETE = "photoplace_sort_complete";

    private SortNotificationHelper() {
    }

    static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel progress = new NotificationChannel(CHANNEL_PROGRESS, "PhotoPlace 정리 진행", NotificationManager.IMPORTANCE_LOW);
        progress.setDescription("사진 정리 진행률을 표시합니다.");
        manager.createNotificationChannel(progress);
        NotificationChannel complete = new NotificationChannel(CHANNEL_COMPLETE, "PhotoPlace 정리 완료", NotificationManager.IMPORTANCE_DEFAULT);
        complete.setDescription("정리 완료 알림을 표시합니다.");
        manager.createNotificationChannel(complete);
    }

    static Notification progressNotification(Context context, String label, int current, int total, String progressContext) {
        ensureChannels(context);
        Notification.Builder builder = builder(context, CHANNEL_PROGRESS)
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setContentTitle(label == null || label.trim().isEmpty() ? "PhotoPlace 정리 중" : label)
                .setContentText(progressText(current, total, progressContext))
                .setOngoing(true)
                .setOnlyAlertOnce(true);
        if (total > 0) {
            builder.setProgress(total, Math.min(Math.max(0, current), total), false);
        } else {
            builder.setProgress(0, 0, true);
        }
        return builder.build();
    }

    static Notification completeNotification(Context context, String title, String text) {
        ensureChannels(context);
        return builder(context, CHANNEL_COMPLETE)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle(title == null || title.trim().isEmpty() ? "PhotoPlace 정리가 완료되었습니다" : title)
                .setContentText(text == null ? "" : text)
                .setAutoCancel(true)
                .build();
    }

    private static Notification.Builder builder(Context context, String channelId) {
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(context, channelId);
        }
        return new Notification.Builder(context);
    }

    private static String progressText(int current, int total, String progressContext) {
        String count = total > 0 ? Math.max(0, current) + " / " + total : "진행 준비 중";
        if (progressContext == null || progressContext.trim().isEmpty()) {
            return count;
        }
        return count + " · " + progressContext;
    }
}
