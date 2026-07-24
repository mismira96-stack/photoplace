package com.example.gallerysorter;

final class MovementClassifier {
    private static final double AIRCRAFT_ALTITUDE_METERS = 2500.0d;
    private static final double AIRCRAFT_SPEED_KMH = 260.0d;
    private static final double FAST_GROUND_SPEED_KMH = 90.0d;

    private MovementClassifier() {
    }

    static MovementType classify(Double altitudeMeters, Double distanceMeters, Long elapsedMillis) {
        double speedKmh = speedKmh(distanceMeters, elapsedMillis);
        if (altitudeMeters != null && altitudeMeters.doubleValue() >= AIRCRAFT_ALTITUDE_METERS) {
            return MovementType.IN_FLIGHT;
        }
        if (speedKmh >= AIRCRAFT_SPEED_KMH) {
            return MovementType.IN_FLIGHT;
        }
        if (speedKmh >= FAST_GROUND_SPEED_KMH) {
            return MovementType.MOVING;
        }
        return MovementType.STILL;
    }

    static double speedKmh(Double distanceMeters, Long elapsedMillis) {
        if (distanceMeters == null || elapsedMillis == null || distanceMeters.doubleValue() <= 0.0d || elapsedMillis.longValue() <= 0L) {
            return 0.0d;
        }
        double hours = elapsedMillis.doubleValue() / 3600000.0d;
        if (hours <= 0.0d) {
            return 0.0d;
        }
        return (distanceMeters.doubleValue() / 1000.0d) / hours;
    }
}
