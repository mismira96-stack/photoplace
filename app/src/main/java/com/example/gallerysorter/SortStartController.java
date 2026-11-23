package com.example.gallerysorter;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.work.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Reservation, durable input and enqueue are off-main; a rejection never falls back to direct copy. */
final class SortStartController {
    static void start(Activity activity,List<PhotoItem> items,boolean videos,OrganizationRequest request,
                      Executor executor,Runnable started,Consumer<String> failed) {
        Context context=activity.getApplicationContext();Handler main=new Handler(Looper.getMainLooper());
        List<PhotoItem> snapshot=new ArrayList<>(items);
        Runnable task=()->{
            try {
                new SortInputStore(context).write(snapshot,videos,request);
                new SortResultStore(context).clear();
                WorkManager.getInstance(context).enqueueUniqueWork(SortWorker.WORK_NAME,ExistingWorkPolicy.KEEP,
                        new OneTimeWorkRequest.Builder(SortWorker.class).build()).getResult().get();
                main.post(()->{if(!activity.isFinishing()&&!activity.isDestroyed())started.run();});
            } catch(Exception error) {
                String message=error.getMessage();
                if(message==null||!message.matches("(?s).*[가-힣].*"))message="백그라운드 정리를 시작하지 못했어요. 진행 중인 작업을 확인해 주세요.";
                final String text=message;
                main.post(()->{if(!activity.isFinishing()&&!activity.isDestroyed())failed.accept(text);});
            }
        };
        try{executor.execute(task);}catch(RuntimeException rejected){failed.accept("화면을 다시 열고 정리를 시작해 주세요.");}
    }
}
