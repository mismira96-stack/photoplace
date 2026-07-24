package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MovementClassifierTest {
    @Test
    public void highAltitudeIsInFlight() {
        assertEquals(MovementType.IN_FLIGHT, MovementClassifier.classify(9000.0d, null, null));
    }

    @Test
    public void aircraftLikeSpeedIsInFlightEvenWithoutAltitude() {
        assertEquals(MovementType.IN_FLIGHT, MovementClassifier.classify(null, 500000.0d, 60L * 60L * 1000L));
    }

    @Test
    public void highwaySpeedIsMovingNotExcluded() {
        assertEquals(MovementType.MOVING, MovementClassifier.classify(null, 100000.0d, 60L * 60L * 1000L));
    }

    @Test
    public void walkingOrStationaryIsStill() {
        assertEquals(MovementType.STILL, MovementClassifier.classify(null, 1000.0d, 30L * 60L * 1000L));
    }

    @Test
    public void missingSignalsDefaultToStill() {
        assertEquals(MovementType.STILL, MovementClassifier.classify(null, null, null));
    }
}
