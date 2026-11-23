package com.example.gallerysorter;

import org.junit.Test;

import static org.junit.Assert.*;

public class MemoryListDensityTest {
    @Test public void oneContinuousPinchCanMoveThroughEachStepAndReverse() {
        MemoryListDensity density = new MemoryListDensity();
        assertEquals(2, density.columns(false));
        density.beginPinch();
        assertFalse(density.scaleBy(0.95f, false));
        assertTrue(density.scaleBy(0.85f, false));
        assertEquals(3, density.columns(false));
        assertTrue(density.scaleBy(0.80f, false));
        assertEquals(4, density.columns(false));
        assertFalse(density.scaleBy(0.5f, false));
        assertTrue(density.scaleBy(1.25f, false));
        assertEquals(3, density.columns(false));
        assertTrue(density.scaleBy(1.25f, false));
        assertEquals(2, density.columns(false));
    }

    @Test public void wideWindowStartsAtThreeAndRemembersItsChoiceSeparately() {
        MemoryListDensity density = new MemoryListDensity();
        assertEquals(3, density.columns(true));
        density.beginPinch();
        assertFalse(density.scaleBy(1.8f, true));
        assertTrue(density.scaleBy(0.80f, true));
        assertEquals(4, density.columns(true));
        assertEquals(2, density.columns(false));
        density.beginPinch();
        assertTrue(density.scaleBy(1.4f, true));
        assertEquals(3, density.columns(true));
        assertFalse(density.scaleBy(1.4f, true));
    }

    @Test public void invalidSamplesAndUnfinishedPinchDoNotChangeNextGesture() {
        MemoryListDensity density = new MemoryListDensity();
        density.beginPinch();
        assertFalse(density.scaleBy(Float.NaN, false));
        assertFalse(density.scaleBy(0f, false));
        assertFalse(density.scaleBy(0.83f, false));
        density.beginPinch();
        assertFalse(density.scaleBy(0.95f, false));
        assertEquals(2, density.columns(false));
    }

    @Test public void detailDensityCanScaleToFourOnNarrowAndFiveOnWide() {
        MemoryListDensity density = MemoryListDensity.forDetail();
        assertEquals(3, density.columns(false));
        assertEquals(3, density.columns(true));

        // Narrow pinch out to 4 columns
        density.beginPinch();
        assertTrue(density.scaleBy(0.80f, false));
        assertEquals(4, density.columns(false));
        // Max 4 on narrow
        assertFalse(density.scaleBy(0.80f, false));
        assertEquals(4, density.columns(false));

        // Wide pinch out to 4 then 5 columns
        density.beginPinch();
        assertTrue(density.scaleBy(0.80f, true));
        assertEquals(4, density.columns(true));
        assertTrue(density.scaleBy(0.80f, true));
        assertEquals(5, density.columns(true));
        // Max 5 on wide
        assertFalse(density.scaleBy(0.80f, true));
        assertEquals(5, density.columns(true));

        // Wide pinch in to 3 columns
        density.beginPinch();
        assertTrue(density.scaleBy(1.25f, true));
        assertEquals(4, density.columns(true));
        assertTrue(density.scaleBy(1.25f, true));
        assertEquals(3, density.columns(true));
        assertFalse(density.scaleBy(1.25f, true));
        assertEquals(3, density.columns(true));
    }
}
