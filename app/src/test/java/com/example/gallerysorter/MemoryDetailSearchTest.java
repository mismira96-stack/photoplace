package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class MemoryDetailSearchTest {
    @Test public void filenameSearchIsCaseInsensitiveAndRetainsExactOriginalUri() {
        DiscoveryPhotoRef ref=new DiscoveryPhotoRef("content://p/99",99,MediaKind.PHOTO,"image/jpeg","SNOW_trip.JPG",100,"p","성남","","","","","Download/",0,1,false);
        MemoryPhotoItem item=new MemoryPhotoItem(ref);
        MemoryPhotoSection source=MemoryPhotoSection.fromCollectionPlace("20261108","11월 8일","성남",Arrays.asList(item));
        List<MemoryPhotoSection> found=MemoryDetailSearch.filter(Arrays.asList(source),Collections.emptyList(),"snow_TRIP");
        assertSame(item,found.get(0).photos.get(0));assertEquals("content://p/99",found.get(0).photos.get(0).sourceUri);
    }
    private final MemoryPhotoItem photo=new MemoryPhotoItem("content://p/1",MediaKind.PHOTO,"image/jpeg",100);
    private final MemoryPhotoItem video=new MemoryPhotoItem("content://p/2",MediaKind.VIDEO,"video/mp4",100);
    private final MemoryPhotoSection section=MemoryPhotoSection.fromCollectionPlace("20261108","11월 8일","성남",Arrays.asList(photo,video));
    @Test public void dateFormatsAndTypeFilterKeepExactFileAndDateIdentity() {
        List<MemoryPhotoSection> result=MemoryDetailSearch.filter(Arrays.asList(section),Collections.emptyList(),"2026.11.08 동영상");
        assertEquals(1,result.size());assertEquals("20261108",result.get(0).dateKey);
        assertEquals(Arrays.asList(video),result.get(0).photos);assertEquals(100,video.takenAtMillis);
    }
    @Test public void noteTextSearchIncludesItsDateAndNoteOnlySections() {
        MemoryPhotoSection empty=MemoryPhotoSection.fromCollectionPlace("20261107","11월 7일","",Collections.emptyList());
        List<MemoryDateNote> notes=Arrays.asList(new MemoryDateNote("mem_1","20261107","친구 생일",1,1));
        List<MemoryPhotoSection> result=MemoryDetailSearch.filter(Arrays.asList(section,empty),notes,"생일");
        assertEquals(1,result.size());assertEquals(empty.dateKey,result.get(0).dateKey);assertTrue(result.get(0).photos.isEmpty());
    }
    @Test public void clearRestoresOriginalSectionsAndMissingQueryDoesNotInventMatches() {
        List<MemoryPhotoSection> source=Arrays.asList(section);
        assertSame(source,MemoryDetailSearch.filter(source,Collections.emptyList(),"  "));
        assertTrue(MemoryDetailSearch.filter(source,Collections.emptyList(),"없는내용").isEmpty());
        assertEquals(2,section.photos.size());
    }
}
