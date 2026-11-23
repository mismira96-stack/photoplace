package com.example.gallerysorter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MemoryStoryPhotoOrderTest {
 private UnresolvedMediaRecord photo(long id,long taken,long modified){return new UnresolvedMediaRecord("content://media/external_primary/images/media/"+id,"external_primary","epoch",id,"PHOTO","photo.jpg","image/jpeg","DCIM/Camera/",100,modified,taken);}
 @Test public void globalNewestOrderIgnoresProviderIdAndChunkOrderWithoutChangingInput(){
  UnresolvedMediaRecord may=photo(1,1746057600000L,1760000000),jan=photo(1000,1735689600000L,1770000000),nov=photo(401,1762560000000L,1762560000);
  List<UnresolvedMediaRecord> original=Arrays.asList(may,jan,nov);List<UnresolvedMediaRecord> sorted=MemoryStoryPhotoOrder.newestFirst(original);
  assertEquals(Arrays.asList(nov,may,jan),sorted);assertEquals(Arrays.asList(may,jan,nov),original);assertSame(nov,sorted.get(0));assertEquals(1746057600000L,may.takenAt);
 }
 @Test public void missingCaptureTimeUsesModificationSecondsAsMilliseconds(){
  UnresolvedMediaRecord old=photo(3,1700000000000L,2000000000L),fallback=photo(2,0,1800000000L),latest=photo(1,1900000000000L,1);
  assertEquals(Arrays.asList(latest,fallback,old),MemoryStoryPhotoOrder.newestFirst(Arrays.asList(old,fallback,latest)));
 }
 @Test public void identicalDatesHaveStableOrderAcrossRefreshes(){
  UnresolvedMediaRecord a=photo(1,1700000000000L,1),b=photo(2,1700000000000L,1);
  assertEquals(Arrays.asList(b,a),MemoryStoryPhotoOrder.newestFirst(Arrays.asList(a,b)));assertEquals(Arrays.asList(b,a),MemoryStoryPhotoOrder.newestFirst(Arrays.asList(b,a)));
 }
}
