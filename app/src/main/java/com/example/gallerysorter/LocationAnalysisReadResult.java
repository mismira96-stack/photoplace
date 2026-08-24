package com.example.gallerysorter;

final class LocationAnalysisReadResult {
    final LocationResult locationResult;
    final boolean coordinatesObserved;

    LocationAnalysisReadResult(LocationResult locationResult, boolean coordinatesObserved) {
        this.locationResult = locationResult;
        this.coordinatesObserved = coordinatesObserved;
    }

    boolean canPersist() {
        return locationResult != null
                && (!PlaceNamePolicy.LOCATION_NONE.equals(locationResult.folderKey) || !coordinatesObserved);
    }
}
