package com.example.gallerysorter;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Immutable selected-file plan plus a small fsynced progress journal, outside cloud backup. */
final class MemoryTrashJournal {
    static final Object LOCK=new Object();
    static final int BATCH_SIZE=1000;
    static final class Session {
        final String id;
        final List<UnresolvedMediaRecord> rows;
        final int batchSize;
        String planHash="",state="READY",message="",workId="";
        int offset,moved,skipped,audited,batchMoved,batchSkipped,batchUnknown;
        Session(List<UnresolvedMediaRecord> rows) {
            this(UUID.randomUUID().toString(),rows,BATCH_SIZE);
        }
        private Session(String id,List<UnresolvedMediaRecord> rows,int batchSize) {
            if(!id.matches("[0-9a-f-]{36}")||rows.isEmpty()||batchSize<1||batchSize>1000)throw new IllegalArgumentException("Invalid trash plan");
            Set<String> keys=new HashSet<>();
            for(UnresolvedMediaRecord r:rows)if(!keys.add(r.assetKey()))throw new IllegalArgumentException("Duplicate selection");
            this.id=id;this.rows=Collections.unmodifiableList(new ArrayList<>(rows));
            this.batchSize=batchSize;
        }
        boolean active(){return !state.equals("DONE")&&!state.equals("CANCELED");}
        List<UnresolvedMediaRecord> batch(){return rows.subList(offset,Math.min(offset+batchSize,rows.size()));}
    }
    private static final class Cached {
        final String id,hash;final List<UnresolvedMediaRecord> rows;
        final int batchSize;
        Cached(String id,String hash,List<UnresolvedMediaRecord> rows,int batchSize){this.id=id;this.hash=hash;this.rows=rows;this.batchSize=batchSize;}
    }
    private static Cached cache;
    private final File dir;
    private final AtomicFile progress;
    MemoryTrashJournal(Context c){dir=c.getNoBackupFilesDir();progress=new AtomicFile(new File(dir,"memory_trash_progress.json"));}
    private AtomicFile plan(String id) {
        if(!id.matches("[0-9a-f-]{36}"))throw new IllegalArgumentException("Invalid session id");
        return new AtomicFile(new File(dir,"memory_trash_plan_"+id+".json"));
    }
    void create(Session session)throws Exception {
        synchronized(LOCK) {
            Session prior=read();if(prior!=null&&prior.active())throw new IllegalStateException("이전 사진 정리를 먼저 마쳐 주세요.");
            JSONArray rows=new JSONArray();for(UnresolvedMediaRecord row:session.rows)rows.put(row.toJson());
            byte[] bytes=new JSONObject().put("version",1).put("sessionId",session.id).put("batchSize",session.batchSize).put("rows",rows).toString().getBytes(StandardCharsets.UTF_8);
            if(bytes.length>32*1024*1024)throw new IllegalArgumentException("한 번에 선택한 사진이 너무 많아요. 나눠서 선택해 주세요.");
            session.planHash=hash(bytes);write(plan(session.id),bytes);save(session);
        }
    }
    Session read()throws Exception {
        synchronized(LOCK) {
            if(!progress.getBaseFile().exists()&&!new File(progress.getBaseFile()+".bak").exists())return null;
            if(progress.getBaseFile().length()>65536)throw new IllegalStateException("휴지통 작업 기록을 확인해 주세요.");
            JSONObject state=new JSONObject(new String(progress.readFully(),StandardCharsets.UTF_8));
            if(state.getInt("version")!=1)throw new IllegalStateException("Unknown trash journal");
            String id=state.getString("sessionId"),expectedHash=state.getString("planHash");
            AtomicFile file=plan(id);
            if(file.getBaseFile().length()>32L*1024*1024)throw new IllegalStateException("Trash plan too large");
            byte[] bytes=file.readFully();String actualHash=hash(bytes);
            if(!actualHash.equals(expectedHash))throw new IllegalStateException("선택한 사진 기록이 손상되었어요.");
            if(cache==null||!cache.id.equals(id)||!cache.hash.equals(actualHash)) {
                JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
                if(root.getInt("version")!=1||!id.equals(root.getString("sessionId")))throw new IllegalStateException("Invalid trash plan");
                JSONArray array=root.getJSONArray("rows");List<UnresolvedMediaRecord> rows=new ArrayList<>();
                for(int i=0;i<array.length();i++)rows.add(UnresolvedMediaRecord.fromJson(array.getJSONObject(i)));
                int batchSize=root.has("batchSize")?root.getInt("batchSize"):1000;
                cache=new Cached(id,actualHash,new Session(id,rows,batchSize).rows,batchSize);
            }
            Session s=new Session(id,cache.rows,cache.batchSize);s.planHash=actualHash;s.state=state.getString("state");
            s.offset=state.getInt("offset");s.moved=state.getInt("moved");s.skipped=state.getInt("skipped");
            s.audited=state.getInt("audited");s.batchMoved=state.getInt("batchMoved");s.batchSkipped=state.getInt("batchSkipped");
            s.batchUnknown=state.getInt("batchUnknown");s.message=state.optString("message","");s.workId=state.optString("workId","");
            validate(s);return s;
        }
    }
    void save(Session s)throws Exception {
        synchronized(LOCK) {
            validate(s);
            write(progress,new JSONObject().put("version",1).put("sessionId",s.id).put("planHash",s.planHash)
                    .put("state",s.state).put("offset",s.offset).put("moved",s.moved).put("skipped",s.skipped)
                    .put("audited",s.audited).put("batchMoved",s.batchMoved).put("batchSkipped",s.batchSkipped)
                    .put("batchUnknown",s.batchUnknown).put("message",s.message).put("workId",s.workId)
                    .toString().getBytes(StandardCharsets.UTF_8));
        }
    }
    private static void validate(Session s) {
        if(!Arrays.asList("READY","REQUESTED","AUDITING","PAUSED","NEEDS_ATTENTION","DONE","CANCELED").contains(s.state)
                ||s.offset<0||s.offset>s.rows.size()||s.moved<0||s.skipped<0||s.moved+s.skipped!=s.offset
                ||s.audited<0||s.audited>s.batch().size()||s.batchMoved<0||s.batchSkipped<0||s.batchUnknown<0
                ||s.batchMoved+s.batchSkipped+s.batchUnknown!=s.audited||!s.planHash.matches("[0-9a-f]{64}")
                ||s.offset==s.rows.size()&&!(s.state.equals("DONE")||s.state.equals("CANCELED")))
            throw new IllegalStateException("Invalid trash progress");
    }
    private static String hash(byte[] bytes)throws Exception{return CleanupPairVerifier.hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static void write(AtomicFile file,byte[] bytes)throws Exception {
        FileOutputStream out=null;try{out=file.startWrite();out.write(bytes);out.getFD().sync();file.finishWrite(out);}
        catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
}
