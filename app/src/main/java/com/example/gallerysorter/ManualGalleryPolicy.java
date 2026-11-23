package com.example.gallerysorter;

import org.json.JSONObject;
import java.util.*;

/** Manual placement wins over physical album membership for the same verified media identity. */
final class ManualGalleryPolicy {
    private static final class Owner {
        final UnresolvedMediaRecord row;final String memoryId;final long takenAt;
        Owner(UnresolvedMediaRecord row,String memoryId,long takenAt){this.row=row;this.memoryId=memoryId;this.takenAt=takenAt;}
    }
    private final Map<String,List<Owner>> owners=new HashMap<>();
    private final Map<String,String> aliases;
    ManualGalleryPolicy(ManualPlaceSnapshot snapshot,Map<String,UnresolvedMediaRecord> live,Map<String,String> aliases)throws Exception {
        this.aliases=new HashMap<>(aliases);JSONObject places=snapshot.writableCopy().getJSONObject("places");
        for(ManualMediaAssignment a:snapshot.assignments()) {
            UnresolvedMediaRecord row=live.get(a.media.assetKey());if(row==null)continue;
            JSONObject place=places.getJSONObject(a.placeId);
            owners.computeIfAbsent(row.kind+":"+row.mediaId,k->new ArrayList<>()).add(new Owner(row,place.getString("stableMemoryId"),a.media.takenAt));
        }
    }
    MemoryMediaStoreReader.Result filter(MemoryMediaStoreReader.Result gallery,MemoryRecord memory) {
        if(gallery.lookup!=MemoryMediaSourcePolicy.GalleryLookup.FOUND)return gallery;
        String memoryId=aliases.get(memory.memoryKey);
        if(memoryId==null&&memory.discoveryGroup!=null)memoryId=aliases.get(memory.discoveryGroup.memoryKey);
        List<DiscoveryPhotoRef> keep=new ArrayList<>();
        for(DiscoveryPhotoRef ref:gallery.refs) {
            List<Owner> possible=owners.get(ref.mediaKind.name()+":"+ref.mediaStoreId);Owner chosen=null;
            if(possible!=null)for(Owner owner:possible) {
                if(ref.sourceRelativePath.isEmpty())throw new IllegalStateException("앨범 사진의 실제 경로를 확인하지 못했어요.");
                if(!owner.row.relativePath.equals(ManualPlacePaths.normalize(ref.sourceRelativePath)))continue;
                if(!ref.sourceUri.contains("/external/")&&!ref.sourceUri.contains("/"+owner.row.volume+"/"))continue;
                if(chosen!=null&&!chosen.row.assetKey().equals(owner.row.assetKey()))throw new IllegalStateException("저장소 사진 식별자가 겹쳐 있어요.");
                chosen=owner;
            }
            if(chosen==null)keep.add(ref);
            else if(chosen.memoryId.equals(memoryId))keep.add(new DiscoveryPhotoRef(ref.sourceUri,ref.mediaStoreId,
                    ref.mediaKind,ref.mimeType,ref.displayName,chosen.takenAt,ref.locationKey,ref.placeName,
                    ref.countryCode,ref.countryName,ref.adminArea,ref.addressLine,ref.sourceRelativePath,
                    ref.firstSeenSnapshotVersion,ref.lastSeenSnapshotVersion,ref.stale,true));
        }
        return MemoryMediaStoreReader.Result.found(keep);
    }
}
