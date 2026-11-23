package com.example.gallerysorter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import java.util.*;

/** Ordinary durable worker: verifies selected files only, with small resumable checkpoints. */
public final class MemoryTrashAuditWorker extends Worker {
    public MemoryTrashAuditWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
    static UUID enqueue(Context c,String sessionId)throws Exception {
        synchronized(MemoryTrashJournal.LOCK) {
            MemoryTrashJournal journal=new MemoryTrashJournal(c);MemoryTrashJournal.Session s=journal.read();
            if(s==null||!s.id.equals(sessionId)||!s.state.equals("AUDITING"))throw new IllegalStateException("작업 상태가 바뀌었어요.");
            if(!s.workId.isEmpty()) {
                WorkInfo old=WorkManager.getInstance(c).getWorkInfoById(UUID.fromString(s.workId)).get();
                if(old!=null&&!old.getState().isFinished())return old.getId();
            }
            OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(MemoryTrashAuditWorker.class)
                    .setInputData(new Data.Builder().putString("session",s.id).build()).build();
            s.workId=request.getId().toString();journal.save(s);
            WorkManager.getInstance(c).enqueueUniqueWork("memory_trash_"+s.id+"_"+s.offset,
                    ExistingWorkPolicy.KEEP,request).getResult().get();
            return request.getId();
        }
    }
    @NonNull @Override public Result doWork() {
        Context c=getApplicationContext();String id=getInputData().getString("session");
        MemoryTrashJournal journal=new MemoryTrashJournal(c);MemoryMediaAccess access=new MemoryMediaAccess(c);
        try {
            while(!isStopped()) {
                MemoryTrashJournal.Session s;
                synchronized(MemoryTrashJournal.LOCK) {
                    s=journal.read();if(s==null||!s.id.equals(id)||!s.state.equals("AUDITING"))return Result.success();
                }
                List<UnresolvedMediaRecord> batch=s.batch();
                int end=Math.min(s.audited+50,batch.size()),moved=0,skipped=0,unknown=0;
                for(int i=s.audited;i<end;i++) {
                    if(isStopped())return Result.retry();
                    UnresolvedMediaRecord expected=batch.get(i);
                    try {
                        MemoryMediaAccess.State actual=access.read(expected.uri,true);
                        if(actual==null||!actual.media.assetKey().equals(expected.assetKey()))unknown++;
                        else if(actual.trashed)moved++;else skipped++;
                    }catch(Exception unavailable){unknown++;}
                }
                synchronized(MemoryTrashJournal.LOCK) {
                    MemoryTrashJournal.Session current=journal.read();
                    if(current==null||!current.id.equals(id)||!current.state.equals("AUDITING")||current.audited!=s.audited)return Result.success();
                    MemoryTrashProgress.record(current,end,moved,skipped,unknown);
                    journal.save(current);MemoryTrashNotification.show(c,current);
                    setProgressAsync(new Data.Builder().putInt("checked",current.audited).build());
                    if(!current.state.equals("AUDITING"))return Result.success();
                }
            }
            return Result.retry();
        } catch(Exception error) {return getRunAttemptCount()<3?Result.retry():Result.failure();}
    }
}
