package com.example.gallerysorter;
import org.junit.Test;
import static org.junit.Assert.*;
public class DiscoveryResultTextTest {
    @Test public void existingPlaceWithNewPhotoIsDescribedAsUpdatedNotNewLocation() {
        assertTrue(DiscoveryResultText.summary(1, 257, 0).contains("추가된 장소 1곳"));
        assertFalse(DiscoveryResultText.summary(1, 257, 0).contains("새 장소"));
        assertEquals("사진 확인을 마쳤어요", DiscoveryResultText.title(0));
    }
    @Test public void homeSummaryOmitsInternalThroughputAndDoesNotClaimUnknownMediaArePhotos() {
        assertEquals("새로 추가된 항목 없음 · 위치 없는 항목 2,880개",DiscoveryResultText.summary(0,896,2880));
        assertFalse(DiscoveryResultText.summary(1,896,0).contains("896"));
    }
}
