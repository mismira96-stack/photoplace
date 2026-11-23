package com.example.gallerysorter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MemoryStorySelectionTest {
 static UnresolvedMediaRecord photo(int id){return new UnresolvedMediaRecord("content://media/external_primary/images/media/"+id,"external_primary","epoch",id,"PHOTO","photo.jpg","image/jpeg","DCIM/Camera/",100,1,1762560000000L);}
 @Test public void oneUsesExistingSingleAndFourUseStripWithoutChangingSelection(){
  List<UnresolvedMediaRecord> selected=new ArrayList<>();selected.add(photo(1));
  MemoryStorySelection one=MemoryStorySelection.from("성남",selected);assertEquals(MemoryStoryConfig.LayoutMode.SINGLE_FRAME,one.config.layoutMode);
  for(int n=2;n<=4;n++){selected.add(photo(n));MemoryStorySelection plan=MemoryStorySelection.from("성남",selected);assertEquals(MemoryStoryConfig.LayoutMode.PHOTO_STRIP,plan.config.layoutMode);assertEquals(n,plan.uris.size());assertEquals(n,selected.size());assertEquals("성남에서",plan.config.captionText);assertEquals(MemoryStoryConfig.ThemeTone.CREAM,plan.config.themeTone);}
  selected.clear();assertEquals(1,one.uris.size());
 }
 @Test public void overFourGivesExactMessage(){try{MemoryStorySelection.from("성남",Arrays.asList(photo(1),photo(2),photo(3),photo(4),photo(5)));fail();}catch(IllegalArgumentException e){assertEquals("Instagram Story에는 최대 4장까지 선택할 수 있어요",e.getMessage());}}
 @Test public void videoIsNotSilentlyDropped(){UnresolvedMediaRecord video=new UnresolvedMediaRecord("content://media/external_primary/video/media/8","external_primary","epoch",8,"VIDEO","a.mp4","video/mp4","DCIM/",100,1,0);try{MemoryStorySelection.from("성남",Arrays.asList(photo(1),video));fail();}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("사진"));}}
}
