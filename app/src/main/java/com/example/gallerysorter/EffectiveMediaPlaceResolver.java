package com.example.gallerysorter;

import org.json.JSONObject;
import java.util.*;

/** Pure effective-place projection, independent of raw discovery JSON integrity checks. */
final class EffectiveMediaPlaceResolver {
    static final String UNRESOLVED_KEY="manual:unresolved";
    private EffectiveMediaPlaceResolver(){ }

    static List<UnresolvedMediaRecord> unresolved(ManualPlaceSnapshot manual,
            Map<String,UnresolvedMediaRecord> live,Set<String> organizedPaths) {
        Set<String> assigned=new HashSet<>();
        for(ManualMediaAssignment a:manual.assignments())assigned.add(a.media.assetKey());
        Set<String> paths=new HashSet<>();for(String p:organizedPaths)if(p!=null&&!p.isEmpty())paths.add(ManualPlacePaths.normalize(p));
        List<UnresolvedMediaRecord> result=new ArrayList<>();
        for(UnresolvedMediaRecord r:manual.unresolved()) {
            UnresolvedMediaRecord current=live.get(r.assetKey());
            if(current!=null&&!assigned.contains(r.assetKey())&&!paths.contains(current.relativePath))result.add(current);
        }
        result.sort((a,b)->Long.compare(b.takenAt,a.takenAt));
        return result;
    }

    static DiscoverySnapshot project(DiscoverySnapshot base,ManualPlaceSnapshot manual,
            Map<String,UnresolvedMediaRecord> live,boolean includeCopySources) throws Exception {
        if(base==null)base=new DiscoverySnapshot(1,0,0,"",0,Collections.emptyList(),"","");
        JSONObject places=manual.writableCopy().getJSONObject("places");
        if(places.length()==0&&manual.assignments().isEmpty())return base;
        Map<String,ManualMediaAssignment> assigned=new LinkedHashMap<>();
        for(ManualMediaAssignment a:manual.assignments()) {
            if(!places.has(a.placeId))throw new IllegalStateException("수동 위치기록 연결이 불완전해요.");
            assigned.put(a.media.assetKey(),a);
        }
        Set<String> assignedUris=new HashSet<>();
        for(ManualMediaAssignment a:assigned.values())assignedUris.add(a.media.uri);
        Map<String,DiscoveryMemoryGroup> templates=new LinkedHashMap<>();
        Map<String,List<DiscoveryPhotoRef>> refs=new LinkedHashMap<>();
        for(DiscoveryMemoryGroup g:base.groups) {
            templates.put(g.memoryKey,g);List<DiscoveryPhotoRef> keep=new ArrayList<>();
            for(DiscoveryPhotoRef ref:g.photoRefs)if(!assignedUris.contains(ref.sourceUri))keep.add(ref);
            refs.put(g.memoryKey,keep);
        }
        for(Iterator<String> keys=places.keys();keys.hasNext();) {
            String id=keys.next();JSONObject p=places.getJSONObject(id);
            String key=p.optString("attachMemoryKey","");if(key.isEmpty())key=p.getString("memoryKey");
            if(!templates.containsKey(key))templates.put(key,new DiscoveryMemoryGroup(key,"manual-place:"+id,p.getString("name"),
                    p.optString("countryCode",""),p.optString("countryName",""),p.optString("adminArea",""),p.optString("addressLine",""),
                    0,0,0,0,0,"",Collections.emptyList(),0,manual.revision));
            refs.computeIfAbsent(key,k->new ArrayList<>());
        }
        for(ManualMediaAssignment a:assigned.values()) {
            UnresolvedMediaRecord row=live.get(a.media.assetKey());if(row==null)continue;
            ManualMediaAssignment copy=assigned.get(a.copyTargetKey);
            if(!includeCopySources&&copy!=null&&copy.placeId.equals(a.placeId)&&live.containsKey(a.copyTargetKey))continue;
            JSONObject p=places.getJSONObject(a.placeId);
            String key=p.optString("attachMemoryKey","");if(key.isEmpty())key=p.getString("memoryKey");
            DiscoveryMemoryGroup template=templates.get(key);
            refs.get(key).add(new DiscoveryPhotoRef(row.uri,row.mediaId,row.kind.equals("VIDEO")?MediaKind.VIDEO:MediaKind.PHOTO,
                    row.mimeType,row.name,a.media.takenAt,template.placeKey,template.placeName,template.countryCode,
                    template.countryName,template.adminArea,template.addressLine,row.relativePath,0,manual.revision,false,true));
        }
        List<DiscoveryMemoryGroup> groups=new ArrayList<>();
        for(Map.Entry<String,DiscoveryMemoryGroup> entry:templates.entrySet()) {
            DiscoveryMemoryGroup g=entry.getValue();List<DiscoveryPhotoRef> media=refs.get(entry.getKey());
            // Empty manual groups remain valid records; notes and identity must survive removal of their photos.
            if(media.isEmpty()&&!g.placeKey.startsWith("manual-place:")) {
                if(g.photoRefs.isEmpty()&&g.itemCount>0)groups.add(g);
                continue;
            }
            groups.add(group(g,media,Math.max(base.snapshotVersion,manual.revision)));
        }
        return new DiscoverySnapshot(base.schemaVersion,base.snapshotVersion,base.createdAtMillis,base.sourceSignature,
                base.sourceItemCount,groups,base.analysisPolicyVersion,base.countryIdentityPolicyVersion);
    }
    static DiscoveryMemoryGroup group(DiscoveryMemoryGroup g,List<DiscoveryPhotoRef> refs,long revision) {
        int photos=0,videos=0;long start=0,end=0;Set<String> seen=new HashSet<>();List<DiscoveryPhotoRef> unique=new ArrayList<>();
        for(DiscoveryPhotoRef r:refs)if(seen.add(r.sourceUri)) {
            unique.add(r);if(r.mediaKind==MediaKind.VIDEO)videos++;else photos++;
            if(r.takenAtMillis>0){start=start==0?r.takenAtMillis:Math.min(start,r.takenAtMillis);end=Math.max(end,r.takenAtMillis);}
        }
        unique.sort((a,b)->Long.compare(b.takenAtMillis,a.takenAtMillis));
        return new DiscoveryMemoryGroup(g.memoryKey,g.placeKey,g.placeName,g.countryCode,g.countryName,g.adminArea,g.addressLine,
                unique.size(),photos,videos,start,end,unique.isEmpty()?"":unique.get(0).sourceUri,unique,0,revision,g.hasUserNotes);
    }
}
