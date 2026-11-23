package com.example.gallerysorter;

import android.content.Context;
import android.net.Uri;
import org.json.JSONObject;
import java.util.*;

/** Durable handoff only for manually assigned media; ordinary copy/evidence behavior is retained. */
final class ManualPlaceCopyBridge {
    private final Context context;
    private Map<String,List<ManualMediaAssignment>> candidates;
    private final OrganizationRequest request;
    ManualPlaceCopyBridge(Context c,OrganizationRequest request){context=c.getApplicationContext();this.request=request;}
    String begin(PhotoItem item)throws Exception {
        if(!item.manualAssigned||request!=null&&request.subjectType==OrganizationLink.SubjectType.COLLECTION)return "";
        if(candidates==null) {
            candidates=new HashMap<>();
            for(ManualMediaAssignment a:new ManualPlaceStore(context).readAssignments().assignments())
                candidates.computeIfAbsent(a.media.kind+":"+a.media.mediaId,k->new ArrayList<>()).add(a);
        }
        String id=item.uri.getLastPathSegment(),kind=item.video?"VIDEO":"PHOTO";
        List<ManualMediaAssignment> possible=candidates.get(kind+":"+id);if(possible==null)return "";
        MemoryMediaAccess.State current=new MemoryMediaAccess(context).read(item.uri.toString(),false);
        if(current==null)throw new IllegalStateException("정리할 사진을 찾지 못했어요.");
        for(ManualMediaAssignment a:possible)if(a.media.assetKey().equals(current.media.assetKey())) {
            if(request!=null) {
                JSONObject place=new ManualPlaceStore(context).readIdentities().writableCopy().getJSONObject("places").getJSONObject(a.placeId);
                if(!place.getString("stableMemoryId").equals(request.subjectId)
                        ||!ManualPlacePaths.normalize(request.relativePath).equals(ManualPlacePaths.normalize(item.targetRelativePath)))
                    throw new IllegalStateException("앨범 대상과 사진의 위치기록 연결이 달라요.");
            }
            return new ManualPlaceStore(context).beginCopy(a,item.targetRelativePath,item.video);
        }
        return "";
    }
    void target(String id,Uri target)throws Exception {if(!id.isEmpty())new ManualPlaceStore(context).copyTarget(id,target.toString());}
    void failed(String id) {if(!id.isEmpty())try{new ManualPlaceStore(context).failedCopy(id);}catch(Exception e){android.util.Log.w("ManualCopy","Failure receipt retained",e);}}
    void complete(String id,Uri target) {
        if(id.isEmpty())return;
        try {
            MemoryMediaAccess.State actual=new MemoryMediaAccess(context).read(target.toString(),false);
            if(actual==null)throw new IllegalStateException("복사본을 확인하지 못했어요.");
            JSONObject receipt=new ManualPlaceStore(context).completeCopy(id,actual.media);
            link(context,receipt);
        }catch(Exception error){android.util.Log.w("ManualCopy","Copy succeeded; pending handoff retained",error);ManualReceiptWorker.enqueue(context);}
    }
    static void link(Context c,JSONObject receipt)throws Exception {
        String path=receipt.getString("targetPath"),id=receipt.getString("id");
        String[] parts=path.split("/");String album=parts[parts.length-1];
        OrganizationLink link=new OrganizationLink("link_manual_"+id,OrganizationLink.SubjectType.MEMORY,
                receipt.getString("stableMemoryId"),"manual_copy_"+id,album,path,System.currentTimeMillis(),
                OrganizationLink.Status.PARTIAL,1,0,0);
        MemoryOrganizationLinkStore.CommitResult result=new MemoryOrganizationLinkStore(c).commit(link);
        if(result==MemoryOrganizationLinkStore.CommitResult.FAILED||result==MemoryOrganizationLinkStore.CommitResult.INVALID||result==MemoryOrganizationLinkStore.CommitResult.CONFLICT)
            throw new IllegalStateException("앨범 연결 저장을 마치지 못했어요.");
        new ManualPlaceStore(c).finishCopyLink(id);
    }
}
