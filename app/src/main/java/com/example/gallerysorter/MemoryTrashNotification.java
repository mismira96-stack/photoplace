package com.example.gallerysorter;

import android.app.*;
import android.content.*;
import android.os.Build;

final class MemoryTrashNotification {
    private static boolean created;
    static void show(Context c,MemoryTrashJournal.Session s) {
        try {
            NotificationManager manager=c.getSystemService(NotificationManager.class);
            synchronized(MemoryTrashNotification.class) {
                if(!created){manager.createNotificationChannel(new NotificationChannel("memory_trash","사진·동영상 정리",NotificationManager.IMPORTANCE_LOW));created=true;}
            }
            Intent intent=new Intent(c,MemoryManagementActivity.class).putExtra("trash_resume",true);
            PendingIntent pending=PendingIntent.getActivity(c,7411,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            boolean working=s.state.equals("AUDITING");
            String text=working?"휴지통 상태 확인 "+s.audited+" / "+s.batch().size():s.message;
            Notification.Builder builder=new Notification.Builder(c,"memory_trash").setSmallIcon(android.R.drawable.ic_menu_gallery)
                    .setContentTitle(s.state.equals("DONE")?"사진 정리가 끝났어요":"사진 휴지통 이동")
                    .setContentText(text).setContentIntent(pending).setOnlyAlertOnce(true).setOngoing(working).setAutoCancel(!working);
            if(working)builder.setProgress(s.batch().size(),s.audited,false);
            manager.notify(7411,builder.build());
        } catch(SecurityException denied) { /* Processing does not depend on notification permission. */ }
    }
}
