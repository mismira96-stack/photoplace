package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryViewerGesturePolicyTest {
    @Test
    public void zoomedImageLocksToPanAndNeverTriggersVerticalDismissOrPageSwipe() {
        MemoryViewerGesturePolicy.Mode mode = MemoryViewerGesturePolicy.evaluate(
                MemoryViewerGesturePolicy.Mode.NONE,
                1,
                2.2f,
                0.0f,
                180.0f,
                8);
        assertEquals(MemoryViewerGesturePolicy.Mode.PAN_ZOOMED, mode);
        assertFalse(MemoryViewerGesturePolicy.shouldDismissVertical(mode, 180.0f, 90.0f));
        assertEquals(0, MemoryViewerGesturePolicy.resolvePageDelta(mode, -140.0f, 40.0f, 1, 5));
    }

    @Test
    public void multiTouchImmediatelyLocksPinchZoomAndBlocksSwipeAndDismiss() {
        MemoryViewerGesturePolicy.Mode mode = MemoryViewerGesturePolicy.evaluate(
                MemoryViewerGesturePolicy.Mode.NONE,
                2,
                1.0f,
                -120.0f,
                150.0f,
                8);
        assertEquals(MemoryViewerGesturePolicy.Mode.PINCH_ZOOM, mode);
        // Even after lifting one finger, the rest of the gesture stays locked in PINCH_ZOOM.
        MemoryViewerGesturePolicy.Mode afterOneFingerUp = MemoryViewerGesturePolicy.evaluate(
                mode,
                1,
                1.0f,
                -120.0f,
                150.0f,
                8);
        assertEquals(MemoryViewerGesturePolicy.Mode.PINCH_ZOOM, afterOneFingerUp);
        assertFalse(MemoryViewerGesturePolicy.shouldDismissVertical(afterOneFingerUp, 200.0f, 80.0f));
    }

    @Test
    public void unzoomedHorizontalDominanceLocksHorizontalSwipeAndRespectsBounds() {
        MemoryViewerGesturePolicy.Mode mode = MemoryViewerGesturePolicy.evaluate(
                MemoryViewerGesturePolicy.Mode.NONE,
                1,
                1.0f,
                -45.0f,
                10.0f,
                8);
        assertEquals(MemoryViewerGesturePolicy.Mode.SWIPE_HORIZONTAL, mode);
        // Once locked horizontally, downward drift mid-swipe does not switch to vertical dismiss.
        MemoryViewerGesturePolicy.Mode drifted = MemoryViewerGesturePolicy.evaluate(
                mode,
                1,
                1.0f,
                -50.0f,
                120.0f,
                8);
        assertEquals(MemoryViewerGesturePolicy.Mode.SWIPE_HORIZONTAL, drifted);
        assertEquals(1, MemoryViewerGesturePolicy.resolvePageDelta(drifted, -60.0f, 40.0f, 0, 3));
        assertEquals(0, MemoryViewerGesturePolicy.resolvePageDelta(drifted, 60.0f, 40.0f, 0, 3));
        assertEquals(-1, MemoryViewerGesturePolicy.resolvePageDelta(drifted, 60.0f, 40.0f, 2, 3));
        assertEquals(0, MemoryViewerGesturePolicy.resolvePageDelta(drifted, -60.0f, 40.0f, 2, 3));
    }

    @Test
    public void unzoomedDownwardDominanceLocksVerticalDismissAndComputesFadeAlpha() {
        MemoryViewerGesturePolicy.Mode mode = MemoryViewerGesturePolicy.evaluate(
                MemoryViewerGesturePolicy.Mode.NONE,
                1,
                1.0f,
                6.0f,
                32.0f,
                8);
        assertEquals(MemoryViewerGesturePolicy.Mode.DISMISS_VERTICAL, mode);
        assertFalse(MemoryViewerGesturePolicy.shouldDismissVertical(mode, 70.0f, 100.0f));
        assertTrue(MemoryViewerGesturePolicy.shouldDismissVertical(mode, 110.0f, 100.0f));
        assertEquals(1.0f, MemoryViewerGesturePolicy.computeDismissAlpha(0.0f, 300.0f), 0.001f);
        assertTrue(MemoryViewerGesturePolicy.computeDismissAlpha(150.0f, 300.0f) < 1.0f);
        assertEquals(0.35f, MemoryViewerGesturePolicy.computeDismissAlpha(600.0f, 300.0f), 0.001f);
    }

    @Test
    public void doubleTapScaleTogglesBetweenDefaultAndZoomed() {
        assertEquals(2.5f, MemoryViewerGesturePolicy.nextDoubleTapScale(1.0f), 0.001f);
        assertEquals(1.0f, MemoryViewerGesturePolicy.nextDoubleTapScale(2.5f), 0.001f);
        assertEquals(4.0f, MemoryViewerGesturePolicy.clampScale(6.0f), 0.001f);
        assertEquals(1.0f, MemoryViewerGesturePolicy.clampScale(0.4f), 0.001f);
    }

    @Test
    public void videoDurationFormattingHandlesMinutesAndHours() {
        assertEquals("00:00", MemoryViewerVideoPlayer.formatDuration(0L));
        assertEquals("00:09", MemoryViewerVideoPlayer.formatDuration(9400L));
        assertEquals("02:15", MemoryViewerVideoPlayer.formatDuration(135_000L));
        assertEquals("1:05:03", MemoryViewerVideoPlayer.formatDuration(3_903_000L));
    }
}
