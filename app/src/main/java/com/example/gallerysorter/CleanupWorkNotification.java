package com.example.gallerysorter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Separate channel/IDs from album copying and emergency recovery. */
final class CleanupWorkNotification {
    private static volatile boolean channelCreated;
    private static void ensureChannel(Context c) {
        if(channelCreated)return;
        synchronized(CleanupWorkNotification.class) {
            if(channelCreated)return;
            NotificationManager manager=c.getSystemService(NotificationManager.class);
            if(manager==null)throw new IllegalStateException("Notification service unavailable");
            manager.createNotificationChannel(new NotificationChannel("cleanup_progress","원본 정리 진행",NotificationManager.IMPORTANCE_LOW));
            channelCreated=true;
        }
    }
    static Notification progress(Context c, String message, int done, int total) {
        ensureChannel(c);
        Notification.Builder builder = new Notification.Builder(c, "cleanup_progress")
                .setSmallIcon(android.R.drawable.ic_menu_gallery).setContentTitle("원본 정리")
                .setContentText(message).setOnlyAlertOnce(true).setOngoing(true)
                .setProgress(total, done, total <= 0);
        Intent launch = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (launch != null) builder.setContentIntent(PendingIntent.getActivity(c,7202,launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return builder.build();
    }
    static void ready(Context c, int done, int total, boolean complete) {
        try {
            NotificationManager manager = c.getSystemService(NotificationManager.class);
            if (manager == null || !manager.areNotificationsEnabled()) return;
            Notification notification = progress(c, done + " / " + total + "장 확인 · "
                    + (complete ? "정리 완료" : "앱에서 남은 작업을 확인해 주세요"), done, total);
            // Keep the state notification separate from WorkManager's foreground ID.
            Notification.Builder builder = Notification.Builder.recoverBuilder(c, notification)
                    .setOngoing(false).setAutoCancel(true);
            manager.notify(7203, builder.build());
        } catch (RuntimeException unavailable) { }
    }
    static void update(Context c,String message,int done,int total) {
        try {
            NotificationManager manager=c.getSystemService(NotificationManager.class);
            if(manager!=null && manager.areNotificationsEnabled())manager.notify(7202,progress(c,message,done,total));
        } catch(RuntimeException unavailable) { }
    }
    static void clearProgress(Context c) {
        try {NotificationManager manager=c.getSystemService(NotificationManager.class);if(manager!=null)manager.cancel(7202);}
        catch(RuntimeException unavailable) { }
    }
}
