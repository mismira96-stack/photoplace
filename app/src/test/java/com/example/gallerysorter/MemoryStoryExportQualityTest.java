package com.example.gallerysorter;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryStoryExportQualityTest {
    @Test public void portraitDecodeCoversExportAndIncreasesForCrop() {
        MemoryStoryComposer.Box slot = new MemoryStoryComposer.Box(0, 0, 900, 1200);
        assertEquals(3600, MemoryStoryExportQuality.decodeEdge(300, 400, slot, 1, 1));
        assertEquals(4000, MemoryStoryExportQuality.decodeEdge(300, 400, slot, 4, 1));
    }

    @Test public void smallCollageSlotsKeepPreviousInputResolutionFloor() {
        MemoryStoryComposer.Box slot = new MemoryStoryComposer.Box(0, 0, 461, 346);
        for (int count = 1; count <= 4; count++) {
            assertEquals(1440, MemoryStoryExportQuality.decodeEdge(512, 384, slot, 1, count));
        }
    }

    @Test public void largerSlotsKeepHeadroomForFinalRotation() {
        MemoryStoryComposer.Box slot = new MemoryStoryComposer.Box(0, 0, 700, 525);
        assertEquals(2100, MemoryStoryExportQuality.decodeEdge(512, 384, slot, 1, 2));
    }

    @Test public void allPhotoCountsAndAspectRatiosStayWithinDecodeBudget() {
        int[][] dimensions = {{400,300}, {300,400}, {400,400}, {800,100}};
        MemoryStoryComposer.Box slot = new MemoryStoryComposer.Box(0, 0, 1000, 1600);
        for (int count = 1; count <= 4; count++) {
            for (int[] size : dimensions) {
                int edge = MemoryStoryExportQuality.decodeEdge(size[0], size[1], slot, 4, count);
                double ratio = Math.min(size[0], size[1]) / (double) Math.max(size[0], size[1]);
                assertTrue(edge <= 4096);
                assertTrue(edge * (double) edge * ratio * count <= 12_000_000);
            }
        }
    }

    @Test public void fourPhotosStayWithinSharedDecodeBudget() {
        MemoryStoryComposer.Box slot = new MemoryStoryComposer.Box(0, 0, 900, 1200);
        int edge = MemoryStoryExportQuality.decodeEdge(300, 400, slot, 4, 4);
        assertTrue(edge * (long) edge * .75 * 4 <= 12_000_000);
        assertTrue(edge >= 1900);
    }
}
