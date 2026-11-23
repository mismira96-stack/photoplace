package com.example.gallerysorter;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class MemoryStoryPhotoEditsTest {
    @Test public void sourceIdentitySurvivesReorderAndFontChanges(){
        MemoryStoryConfig c=new MemoryStoryConfig(null,true,"성남","날짜",null,null,1)
                .withPhotoOrder(Arrays.asList("a","b")).withPhotoEdit(0,2.5f,.5f,-.3f)
                .withFontKind(MemoryStoryConfig.FontKind.HAND).withTextScale(1.4f).withPhotoOrder(Arrays.asList("b","a"));
        assertEquals(1,c.photoEdits.get(0).zoom,.001);assertEquals(2.5,c.photoEdits.get(1).zoom,.001);
        assertEquals(.5,c.photoEdits.get(1).x,.001);assertEquals(1.4,c.textScale,.001);
    }
    @Test public void fitInitiallyPreservesWholePortraitAndLandscapeAndZoomCanBeReduced(){
        MemoryStoryComposer.Box slot=new MemoryStoryComposer.Box(100,200,800,600);
        for(int[] size:new int[][]{{900,1600},{1600,900}}){
            MemoryStoryComposer.Box box=MemoryStoryPhotoEdits.imageBox(size[0],size[1],slot,new MemoryStoryPhotoEdits.Transform(1,0,0));
            assertTrue(box.left>=slot.left-.01&&box.right<=slot.right+.01&&box.top>=slot.top-.01&&box.bottom<=slot.bottom+.01);
            assertEquals(size[0]/(float)size[1],box.width()/box.height(),.001);
        }
        assertEquals(.5,new MemoryStoryPhotoEdits.Transform(.1f,10,-10).zoom,.001);
    }
}
