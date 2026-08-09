package com.example.gallerysorter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class SortNotificationHelper {
    static final String CHANNEL_PROGRESS = "photoplace_sort_progress_v2";
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
        NotificationChannel progress = new NotificationChannel(CHANNEL_PROGRESS, "PhotoPlace 정리 진행", NotificationManager.IMPORTANCE_DEFAULT);
        progress.setDescription("사진 정리 진행률을 표시합니다.");
        progress.setSound(null, null);
        progress.enableVibration(false);
        progress.setShowBadge(true);
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
                .setContentIntent(openAppIntent(context))
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true);
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }
        if (total > 0) {
            builder.setProgress(total, Math.min(Math.max(0, current), total), false);
        } else {
            builder.setProgress(0, 0, true);
        }
        return builder.build();
    }

    static void notifyProgress(Context context, String label, int current, int total, String progressContext) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(SortForegroundService.NOTIFICATION_PROGRESS_ID, progressNotification(context, label, current, total, progressContext));
        }
    }

    static void clearCompleteNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(SortForegroundService.NOTIFICATION_COMPLETE_ID);
        }
    }

    static Notification completeNotification(Context context, String title, String text) {
        ensureChannels(context);
        return builder(context, CHANNEL_COMPLETE)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle(title == null || title.trim().isEmpty() ? "PhotoPlace 정리가 완료되었습니다" : title)
                .setContentText(text == null ? "" : text)
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true)
                .build();
    }

    private static Notification.Builder builder(Context context, String channelId) {
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(context, channelId);
        }
        return new Notification.Builder(context);
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            intent = new Intent(context, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 4103, intent, flags);
    }

    private static String progressText(int current, int total, String progressContext) {
        String count = total > 0 ? Math.max(0, current) + " / " + total : "진행 준비 중";
        if (progressContext == null || progressContext.trim().isEmpty()) {
            return count;
        }
        return count + " · " + progressContext;
    }
}
