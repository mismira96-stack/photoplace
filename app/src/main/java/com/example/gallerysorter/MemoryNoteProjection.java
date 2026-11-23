package com.example.gallerysorter;

import java.util.*;

/** Retains authored notes without migrating their stable id or date to another place. */
final class MemoryNoteProjection {
    static DiscoverySnapshot retain(DiscoverySnapshot raw,DiscoverySnapshot effective,
                                    Map<String,String> aliases,List<MemoryDateNote> notes) {
        Set<String> noted=new HashSet<>();for(MemoryDateNote n:notes)noted.add(n.stableMemoryId);
        LinkedHashMap<String,DiscoveryMemoryGroup> groups=new LinkedHashMap<>();
        if(effective!=null)for(DiscoveryMemoryGroup g:effective.groups)groups.put(g.memoryKey,g);
        if(raw!=null)for(DiscoveryMemoryGroup g:raw.groups)
            if(noted.contains(aliases.get(g.memoryKey))&&!groups.containsKey(g.memoryKey))
                groups.put(g.memoryKey,EffectiveMediaPlaceResolver.group(g,Collections.emptyList(),g.snapshotVersion));
        Set<String> represented=new HashSet<>();
        for(DiscoveryMemoryGroup g:new ArrayList<>(groups.values())) {
            String id=aliases.get(g.memoryKey);if(!noted.contains(id))continue;
            represented.add(id);
            groups.put(g.memoryKey,new DiscoveryMemoryGroup(g.memoryKey,g.placeKey,g.placeName,g.countryCode,g.countryName,
                    g.adminArea,g.addressLine,g.itemCount,g.photoCount,g.videoCount,g.startDateMillis,g.endDateMillis,
                    g.coverUri,g.photoRefs,g.staleCount,g.snapshotVersion,true));
        }
        for(String id:noted)if(!represented.contains(id)) {
            String key="note-only:"+id;aliases.put(key,id);
            groups.put(key,new DiscoveryMemoryGroup(key,key,"사진 없는 기록","","","","",0,0,0,0,0,"",
                    Collections.emptyList(),0,effective.snapshotVersion,true));
        }
        return new DiscoverySnapshot(effective.schemaVersion,effective.snapshotVersion,effective.createdAtMillis,
                effective.sourceSignature,effective.sourceItemCount,new ArrayList<>(groups.values()),
                effective.analysisPolicyVersion,effective.countryIdentityPolicyVersion);
    }
    static List<MemoryDateNote> forId(List<MemoryDateNote> notes,String id) {
        List<MemoryDateNote> result=new ArrayList<>();for(MemoryDateNote n:notes)if(n.stableMemoryId.equals(id))result.add(n);
        return result;
    }
}
