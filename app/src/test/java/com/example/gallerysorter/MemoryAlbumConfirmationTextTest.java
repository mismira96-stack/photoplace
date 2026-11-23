package com.example.gallerysorter;
import org.junit.Test;
import static org.junit.Assert.*;
public class MemoryAlbumConfirmationTextTest {
    @Test public void videoOnlyAndExcludedVideoDoNotShowZeroPhotos() {
        assertEquals("새로 넣을 동영상 1개",MemoryAlbumConfirmationText.added(0,1,true));
        assertEquals("새로 넣을 사진 2장",MemoryAlbumConfirmationText.added(2,1,false));
        assertEquals("새로 넣을 항목 없음",MemoryAlbumConfirmationText.added(0,1,false));
        assertEquals("새로 넣을 사진 2장 · 동영상 1개",MemoryAlbumConfirmationText.added(2,1,true));
    }
}
