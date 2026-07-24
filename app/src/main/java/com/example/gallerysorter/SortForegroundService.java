package com.example.gallerysorter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public class SortForegroundService extends Service {
    static final String ACTION_START = "com.photoplace.app.sort.START";
    static final String ACTION_UPDATE = "com.photoplace.app.sort.UPDATE";
    static final String ACTION_STOP = "com.photoplace.app.sort.STOP";
    static final String ACTION_COMPLETE = "com.photoplace.app.sort.COMPLETE";
    static final String EXTRA_LABEL = "label";
    static final String EXTRA_CURRENT = "current";
    static final String EXTRA_TOTAL = "total";
    static final String EXTRA_CONTEXT = "context";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TEXT = "text";

    static final int NOTIFICATION_PROGRESS_ID = 4101;
    static final int NOTIFICATION_COMPLETE_ID = 4102;

    @Override
    public void onCreate() {
        super.onCreate();
        SortNotificationHelper.ensureChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_COMPLETE.equals(action)) {
            postCompletion(intent);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        Notification notification = buildProgressNotification(intent);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_PROGRESS_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_PROGRESS_ID, notification);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static void start(Context context, String label) {
        Intent intent = new Intent(context, SortForegroundService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_LABEL, label);
        startForeground(context, intent);
    }

    static void update(Context context, String label, int current, int total, String progressContext) {
        Intent intent = new Intent(context, SortForegroundService.class);
        intent.setAction(ACTION_UPDATE);
        intent.putExtra(EXTRA_LABEL, label);
        intent.putExtra(EXTRA_CURRENT, current);
        intent.putExtra(EXTRA_TOTAL, total);
        intent.putExtra(EXTRA_CONTEXT, progressContext);
        startForeground(context, intent);
    }

    static void stop(Context context) {
        Intent intent = new Intent(context, SortForegroundService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    static void complete(Context context, String title, String text) {
        Intent intent = new Intent(context, SortForegroundService.class);
        intent.setAction(ACTION_COMPLETE);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_TEXT, text);
        context.startService(intent);
    }

    private static void startForeground(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private Notification buildProgressNotification(Intent intent) {
        String label = intent == null ? null : intent.getStringExtra(EXTRA_LABEL);
        String progressContext = intent == null ? null : intent.getStringExtra(EXTRA_CONTEXT);
        int current = intent == null ? 0 : intent.getIntExtra(EXTRA_CURRENT, 0);
        int total = intent == null ? 0 : intent.getIntExtra(EXTRA_TOTAL, 0);
        return SortNotificationHelper.progressNotification(this, label, current, total, progressContext);
    }

    private void postCompletion(Intent intent) {
        String title = intent == null ? null : intent.getStringExtra(EXTRA_TITLE);
        String text = intent == null ? null : intent.getStringExtra(EXTRA_TEXT);
        Notification notification = SortNotificationHelper.completeNotification(this, title, text);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_COMPLETE_ID, notification);
        }
    }
}
