package com.example.gallerysorter;

import android.content.Context;
import android.util.AtomicFile;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import org.json.JSONObject;
import java.io.*;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Serializes preparation, then hands exclusion to durable cleanup/trash/sort input state. */
final class MediaMutationCoordinator {
    private static final Object LOCK=new Object();
    private static final String PROCESS=UUID.randomUUID().toString();
    private interface Locked<T>{T run()throws Exception;}
    private static <T>T locked(Context c,Locked<T> body)throws Exception {
        synchronized(LOCK) {
            File dir=c.getNoBackupFilesDir();
            try(RandomAccessFile handle=new RandomAccessFile(new File(dir,"media_mutation.lock"),"rw");
                FileLock lock=handle.getChannel().lock()) { return body.run(); }
        }
    }
    private static AtomicFile file(Context c){return new AtomicFile(new File(c.getNoBackupFilesDir(),"media_mutation_reservation.json"));}
    private static JSONObject read(Context c)throws Exception {
        AtomicFile f=file(c);if(!f.getBaseFile().exists()&&!new File(f.getBaseFile()+".bak").exists())return new JSONObject();
        if(f.getBaseFile().length()>65536)throw new IllegalStateException("작업 예약 상태를 확인하지 못했어요.");
        return new JSONObject(new String(f.readFully(),StandardCharsets.UTF_8));
    }
    private static void write(Context c,JSONObject value)throws Exception {
        AtomicFile f=file(c);FileOutputStream out=null;
        try{out=f.startWrite();out.write(value.toString().getBytes(StandardCharsets.UTF_8));out.getFD().sync();f.finishWrite(out);}
        catch(Exception e){if(out!=null)f.failWrite(out);throw e;}
    }
    static Reservation reserve(Context context,String kind)throws Exception {
        Context c=context.getApplicationContext();
        return locked(c,()->{
            CleanupExecutionJournal.Session cleanup=new CleanupExecutionJournal(c).read();
            if(!kind.equals("cleanup")&&cleanup!=null&&cleanup.active())
                throw new IllegalStateException("진행 중인 원본 정리를 먼저 마쳐 주세요.");
            MemoryTrashJournal.Session trash=new MemoryTrashJournal(c).read();
            if(!kind.equals("trash")&&trash!=null&&trash.active())
                throw new IllegalStateException("진행 중인 사진 휴지통 이동을 먼저 마치거나 중단해 주세요.");
            {
                boolean sorting=SortProgressStore.read(c).active||!new SortInputStore(c).read().items.isEmpty();
                for(WorkInfo work:WorkManager.getInstance(c).getWorkInfosForUniqueWork(SortWorker.WORK_NAME).get())
                    sorting|=!work.getState().isFinished();
                if(sorting)throw new IllegalStateException("앨범 만들기가 끝난 뒤 정리해 주세요.");
            }
            JSONObject old=read(c);
            if(PROCESS.equals(old.optString("process","")))
                throw new IllegalStateException("다른 정리 작업을 준비하고 있어요. 잠시 후 다시 시도해 주세요.");
            // An old process's preparation lease is reclaimable only after the durable states above were checked.
            String token=UUID.randomUUID().toString();
            write(c,new JSONObject().put("process",PROCESS).put("kind",kind).put("token",token));
            return new Reservation(c,token);
        });
    }
    static final class Reservation implements AutoCloseable {
        private final Context c;private final String token;
        Reservation(Context c,String token){this.c=c;this.token=token;}
        @Override public void close()throws Exception {
            locked(c,()->{if(token.equals(read(c).optString("token","")))write(c,new JSONObject());return null;});
        }
    }
}
