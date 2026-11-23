package com.example.gallerysorter;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.work.*;
import org.json.JSONObject;
import java.io.InputStream;
import java.security.MessageDigest;

/** Replays app metadata only; never copies, moves, deletes or trashes a media file. */
public final class ManualReceiptWorker extends Worker {
    public ManualReceiptWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
    static void enqueue(Context c) {
        WorkManager.getInstance(c).enqueueUniqueWork("manual_copy_receipt_recovery",ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(ManualReceiptWorker.class).build());
    }
    @NonNull @Override public Result doWork() {
        Context c=getApplicationContext();
        try { if(new ManualPlaceStore(c).pendingCopies().isEmpty())return Result.success(); }
        catch(Exception unavailable){return Result.failure();}
        try(MediaMutationCoordinator.Reservation reservation=MediaMutationCoordinator.reserve(c,"repair")) {
            ManualPlaceStore store=new ManualPlaceStore(c);
            for(JSONObject receipt:store.pendingCopies()) {
                if(isStopped())return Result.retry();
                if("ASSIGNED".equals(receipt.getString("state"))){ManualPlaceCopyBridge.link(c,receipt);continue;}
                String uri=receipt.getString("targetUri");
                ManualMediaAssignment source=ManualMediaAssignment.fromJson(receipt.getJSONObject("source"));
                MemoryMediaAccess access=new MemoryMediaAccess(c);
                MemoryMediaAccess.State original=access.read(source.media.uri,false);
                if(uri.isEmpty()) {
                    if(original!=null&&original.media.assetKey().equals(source.media.assetKey()))store.failedCopy(receipt.getString("id"));
                    continue;
                }
                MemoryMediaAccess.State target;
                try {target=access.read(uri,false);}catch(MemoryMediaAccess.Pending pending) {
                    if(original!=null&&original.media.assetKey().equals(source.media.assetKey()))store.failedCopy(receipt.getString("id"));
                    continue;
                }
                if(target==null||!target.media.relativePath.equals(receipt.getString("targetPath"))) {
                    if(original!=null&&original.media.assetKey().equals(source.media.assetKey()))store.failedCopy(receipt.getString("id"));
                    continue;
                }
                if(receipt.getBoolean("video")) {
                    if(!target.media.assetKey().equals(source.media.assetKey()))continue;
                } else {
                    if(original==null||!original.media.assetKey().equals(source.media.assetKey())
                            ||original.media.size!=target.media.size||!hash(c,source.media.uri).equals(hash(c,uri)))continue;
                }
                ManualPlaceCopyBridge.link(c,store.completeCopy(receipt.getString("id"),target.media));
            }
            return Result.success();
        }catch(Exception error){return getRunAttemptCount()<3?Result.retry():Result.failure();}
    }
    private String hash(Context c,String uri)throws Exception {
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        try(InputStream in=c.getContentResolver().openInputStream(MediaStore.setRequireOriginal(Uri.parse(uri)))) {
            if(in==null)throw new IllegalStateException("Cannot open original bytes");
            byte[] buffer=new byte[65536];int n;
            while((n=in.read(buffer))!=-1){if(isStopped())throw new InterruptedException();hash.update(buffer,0,n);}
        }
        return CleanupPairVerifier.hex(hash.digest());
    }
}
