package com.example.gallerysorter;

import java.util.*;

/** Display order only. MediaStore capture dates and the existing selection are never changed. */
final class MemoryStoryPhotoOrder {
    private MemoryStoryPhotoOrder() { }
    static List<UnresolvedMediaRecord> newestFirst(List<UnresolvedMediaRecord> media) {
        List<UnresolvedMediaRecord> result=new ArrayList<>(media);
        result.sort((a,b)->{
            int date=Long.compare(displayDate(b),displayDate(a));
            if(date!=0)return date;
            int id=Long.compare(b.mediaId,a.mediaId);
            return id!=0?id:a.assetKey().compareTo(b.assetKey());
        });
        return Collections.unmodifiableList(result);
    }
    private static long displayDate(UnresolvedMediaRecord media) {
        // DATE_TAKEN is milliseconds; DATE_MODIFIED is seconds. Capture time has precedence.
        return media.takenAt>0?media.takenAt:Math.min(media.modified,Long.MAX_VALUE/1000L)*1000L;
    }
}
