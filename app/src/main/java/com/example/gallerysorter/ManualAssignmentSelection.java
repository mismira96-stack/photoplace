package com.example.gallerysorter;

import java.util.*;

/** A confirmed source/copy pair is one logical placement; intentional file trash stays per-file. */
final class ManualAssignmentSelection {
    static List<UnresolvedMediaRecord> expand(List<UnresolvedMediaRecord> selected,List<ManualMediaAssignment> assignments) {
        Map<String,ManualMediaAssignment> all=new LinkedHashMap<>();
        Map<String,Set<String>> edges=new HashMap<>();
        for(ManualMediaAssignment a:assignments)all.put(a.media.assetKey(),a);
        for(ManualMediaAssignment a:assignments) {
            ManualMediaAssignment target=all.get(a.copyTargetKey);
            if(target==null||!target.placeId.equals(a.placeId))continue;
            edges.computeIfAbsent(a.media.assetKey(),k->new LinkedHashSet<>()).add(target.media.assetKey());
            edges.computeIfAbsent(target.media.assetKey(),k->new LinkedHashSet<>()).add(a.media.assetKey());
        }
        Map<String,UnresolvedMediaRecord> result=new LinkedHashMap<>();
        for(UnresolvedMediaRecord row:selected)result.put(row.assetKey(),row);
        ArrayDeque<String> pending=new ArrayDeque<>(result.keySet());
        while(!pending.isEmpty())for(String other:edges.getOrDefault(pending.remove(),Collections.emptySet())) {
            if(!result.containsKey(other)){result.put(other,all.get(other).media);pending.add(other);}
        }
        return new ArrayList<>(result.values());
    }
}
