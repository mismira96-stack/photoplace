package com.example.gallerysorter;

import java.util.*;
import java.text.*;

/** Prepared off-main; presentation identity/order never changes the target memory or its dates. */
final class MemoryPlaceChoice {
    final MemoryRecord record;
    final String id,title,subtitle,coverUri,search;
    final long date;
    MemoryPlaceChoice(MemoryRecord record,String stableId) {
        this.record=record;id=stableId==null?"":stableId;
        title=record.canonicalPlaceName.isEmpty()?record.title:record.canonicalPlaceName;
        coverUri=record.coverUri;date=Math.max(record.startDateMillis,record.endDateMillis);
        subtitle=MemoryMediaCounts.forRecord(record)+(date>0?" · "+new SimpleDateFormat("yyyy.MM",Locale.KOREA).format(new Date(date)):"");
        search=(title+" "+record.title+" "+record.countryName+" "+record.adminArea+" "+record.addressLine).toLowerCase(Locale.ROOT);
    }
    static List<MemoryPlaceChoice> filter(List<MemoryPlaceChoice> all,String query,boolean byName) {
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);List<MemoryPlaceChoice> result=new ArrayList<>();
        for(MemoryPlaceChoice c:all)if(q.isEmpty()||c.search.contains(q))result.add(c);
        result.sort(byName?Comparator.comparing(c->c.title,String.CASE_INSENSITIVE_ORDER):Comparator.comparingLong((MemoryPlaceChoice c)->c.date).reversed().thenComparing(c->c.title));
        return result;
    }
    static List<MemoryPlaceChoice> recent(List<MemoryPlaceChoice> all,List<String> history) {
        List<MemoryPlaceChoice> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(String id:history)for(MemoryPlaceChoice c:all)if(!c.id.isEmpty()&&c.id.equals(id)&&seen.add(id)){result.add(c);break;}
        return result.subList(0,Math.min(3,result.size()));
    }
}
