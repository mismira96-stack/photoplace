package com.example.gallerysorter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Recovery remains discoverable if a post-trash check finishes while the app is backgrounded. */
final class CleanupRecoveryNotifier {
    private static final int ID = 7201;
    static void show(Context context) {
        try {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null || !manager.areNotificationsEnabled()) return;
            manager.createNotificationChannel(new NotificationChannel("cleanup_recovery", "원본 복구 확인", NotificationManager.IMPORTANCE_HIGH));
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            Notification.Builder builder = new Notification.Builder(context, "cleanup_recovery")
                    .setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle("원본 정리를 멈췄어요")
                    .setContentText("복사본 보존 확인이 필요해요. 앱을 열어 원본 복구를 확인해 주세요.")
                    .setStyle(new Notification.BigTextStyle().bigText("남은 원본 정리를 중단했어요. 앱에서 원본 복구를 요청하거나 갤러리 휴지통을 확인해 주세요."))
                    .setAutoCancel(true);
            if (launch != null) builder.setContentIntent(PendingIntent.getActivity(context, ID, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            manager.notify(ID, builder.build());
        } catch (RuntimeException unavailable) { /* Durable journal and in-app recovery remain available. */ }
    }
    static void clear(Context context) {
        try {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.cancel(ID);
        } catch (RuntimeException ignored) { }
    }
}
