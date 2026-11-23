package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryViewerVideoActionTest {
    @Test public void viewerVideoUsesSameItemAndUriForExplicitPlayback() {
        MemoryPhotoItem video = new MemoryPhotoItem("content://media/external/video/media/8",
                MediaKind.VIDEO, "video/mp4", 12);
        MemoryPhotoItem[] opened = {null};
        assertTrue(MemoryViewerVideoAction.open(video, item -> opened[0] = item));
        assertSame(video, opened[0]);
    }

    @Test public void photosMissingItemsAndEmptyVideoUrisDoNotLaunchPlayer() {
        for (MemoryPhotoItem item : new MemoryPhotoItem[]{null,
                new MemoryPhotoItem("content://image/1", MediaKind.PHOTO, "image/jpeg", 0),
                new MemoryPhotoItem("", MediaKind.VIDEO, "video/mp4", 0)}) {
            assertFalse(MemoryViewerVideoAction.open(item, ignored -> fail("Unexpected external playback")));
        }
    }

    @Test public void smallFingerMovementRemainsTapButSwipeAndVerticalDragAreNot() {
        assertTrue(MemoryViewerVideoAction.isTap(0, 0, 8));
        assertTrue(MemoryViewerVideoAction.isTap(-7, 8, 8));
        assertFalse(MemoryViewerVideoAction.isTap(9, 0, 8));
        assertFalse(MemoryViewerVideoAction.isTap(-80, 0, 8));
        assertFalse(MemoryViewerVideoAction.isTap(0, -30, 8));
    }
}
