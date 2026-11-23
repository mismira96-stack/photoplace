package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure spatial calculation and membership precedence engine for Personal Places.
 * Free of Android UI/Context dependencies for deterministic unit testing.
 */
final class PersonalPlaceMatcher {
    static final double EARTH_RADIUS_METERS = 6371008.8d;
    static final double MAX_ANCHOR_SPREAD_METERS = 1000.0d;
    static final double OVERLAP_CLEAR_MARGIN_METERS = 15.0d;
    static final int INDOOR_DRIFT_MARGIN_METERS = 15;

    private PersonalPlaceMatcher() {
    }

    static final class AnchorAnalysis {
        final boolean hasValidCoordinate;
        final boolean spreadTooWide;
        final double centerLat;
        final double centerLng;
        final double maxSpreadMeters;
        final int validAnchorCount;

        AnchorAnalysis(boolean hasValidCoordinate,
                       boolean spreadTooWide,
                       double centerLat,
                       double centerLng,
                       double maxSpreadMeters,
                       int validAnchorCount) {
            this.hasValidCoordinate = hasValidCoordinate;
            this.spreadTooWide = spreadTooWide;
            this.centerLat = centerLat;
            this.centerLng = centerLng;
            this.maxSpreadMeters = maxSpreadMeters;
            this.validAnchorCount = validAnchorCount;
        }
    }

    /** Haversine distance in meters between two WGS84 coordinates. */
    static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        if (!PersonalPlace.isUsableCoordinate(lat1, lng1) || !PersonalPlace.isUsableCoordinate(lat2, lng2)) {
            return Double.POSITIVE_INFINITY;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2.0d) * Math.sin(dLat / 2.0d)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2.0d) * Math.sin(dLng / 2.0d);
        double c = 2.0d * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0d, 1.0d - a)));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Picks the representative medoid coordinate among valid anchor photos (the point with minimum sum of distances
     * to all other anchor points) and checks if the anchors are scattered wider than MAX_ANCHOR_SPREAD_METERS.
     */
    static AnchorAnalysis analyzeAnchors(Collection<PersonalPlaceGpsPoint> anchorPoints) {
        ArrayList<PersonalPlaceGpsPoint> valid = new ArrayList<>();
        if (anchorPoints != null) {
            for (PersonalPlaceGpsPoint point : anchorPoints) {
                if (point != null && point.hasCoordinates()) {
                    valid.add(point);
                }
            }
        }
        if (valid.isEmpty()) {
            return new AnchorAnalysis(false, false, 0.0d, 0.0d, 0.0d, 0);
        }
        if (valid.size() == 1) {
            PersonalPlaceGpsPoint single = valid.get(0);
            return new AnchorAnalysis(true, false, single.latitude, single.longitude, 0.0d, 1);
        }
        PersonalPlaceGpsPoint bestMedoid = valid.get(0);
        double bestSum = Double.POSITIVE_INFINITY;
        for (int i = 0; i < valid.size(); i++) {
            PersonalPlaceGpsPoint candidate = valid.get(i);
            double sum = 0.0d;
            for (int j = 0; j < valid.size(); j++) {
                if (i == j) {
                    continue;
                }
                PersonalPlaceGpsPoint other = valid.get(j);
                sum += distanceMeters(candidate.latitude, candidate.longitude, other.latitude, other.longitude);
            }
            if (sum < bestSum) {
                bestSum = sum;
                bestMedoid = candidate;
            }
        }
        double maxSpread = 0.0d;
        for (PersonalPlaceGpsPoint point : valid) {
            double d = distanceMeters(bestMedoid.latitude, bestMedoid.longitude, point.latitude, point.longitude);
            if (d > maxSpread) {
                maxSpread = d;
            }
        }
        boolean tooWide = maxSpread > MAX_ANCHOR_SPREAD_METERS;
        return new AnchorAnalysis(true, tooWide, bestMedoid.latitude, bestMedoid.longitude, maxSpread, valid.size());
    }

    /**
     * Computes the effective auto-matching radius: at least selectedRadiusMeters, expanded by confirmed
     * indoor GPS drift points (+15m margin) up to MAX_EFFECTIVE_RADIUS (150m).
     */
    static int computeEffectiveRadius(int selectedRadiusMeters,
                                      double centerLat,
                                      double centerLng,
                                      Collection<PersonalPlaceGpsPoint> confirmedPoints) {
        int clampedSelected = PersonalPlace.clampSelectedRadius(selectedRadiusMeters);
        if (!PersonalPlace.isUsableCoordinate(centerLat, centerLng) || confirmedPoints == null || confirmedPoints.isEmpty()) {
            return clampedSelected;
        }
        double maxConfirmedWithinBound = 0.0d;
        double maxAllowedConfirmedDist = PersonalPlace.MAX_EFFECTIVE_RADIUS - INDOOR_DRIFT_MARGIN_METERS;
        for (PersonalPlaceGpsPoint point : confirmedPoints) {
            if (point == null || !point.hasCoordinates()) {
                continue;
            }
            double dist = distanceMeters(centerLat, centerLng, point.latitude, point.longitude);
            if (dist <= maxAllowedConfirmedDist && dist > maxConfirmedWithinBound) {
                maxConfirmedWithinBound = dist;
            }
        }
        int driftAdjusted = (int) Math.ceil(maxConfirmedWithinBound + INDOOR_DRIFT_MARGIN_METERS);
        return Math.min(PersonalPlace.MAX_EFFECTIVE_RADIUS, Math.max(clampedSelected, driftAdjusted));
    }

    /**
     * Finds candidate photo URIs within radiusMeters for preview, preserving anchor photos first/always
     * and excluding photos explicitly locked into another personal place of the same parent record.
     */
    static List<String> findPreviewCandidateUris(double centerLat,
                                                 double centerLng,
                                                 boolean hasCenter,
                                                 int radiusMeters,
                                                 List<DiscoveryPhotoRef> groupRefs,
                                                 Map<String, PersonalPlaceGpsPoint> gpsByUri,
                                                 Set<String> anchorUris,
                                                 Set<String> excludedFromThisPlaceUris,
                                                 Set<String> lockedInOtherPlacesUris) {
        LinkedHashSet<String> matched = new LinkedHashSet<>();
        Set<String> anchors = anchorUris == null ? Collections.emptySet() : anchorUris;
        Set<String> excluded = excludedFromThisPlaceUris == null ? Collections.emptySet() : excludedFromThisPlaceUris;
        Set<String> lockedOther = lockedInOtherPlacesUris == null ? Collections.emptySet() : lockedInOtherPlacesUris;
        if (groupRefs == null) {
            return Collections.emptyList();
        }
        for (DiscoveryPhotoRef ref : groupRefs) {
            if (ref == null || ref.stale || ref.sourceUri == null || ref.sourceUri.trim().isEmpty()) {
                continue;
            }
            String uri = ref.sourceUri.trim();
            if (anchors.contains(uri)) {
                matched.add(uri);
                continue;
            }
            if (excluded.contains(uri) || lockedOther.contains(uri)) {
                continue;
            }
            if (!hasCenter || !PersonalPlace.isUsableCoordinate(centerLat, centerLng)) {
                continue;
            }
            PersonalPlaceGpsPoint pt = gpsByUri == null ? null : gpsByUri.get(uri);
            if (pt != null && pt.hasCoordinates()) {
                double dist = distanceMeters(centerLat, centerLng, pt.latitude, pt.longitude);
                if (dist <= radiusMeters) {
                    matched.add(uri);
                }
            }
        }
        return new ArrayList<>(matched);
    }

    /**
     * Evaluates automatic personal place membership for all photos in a parent record.
     * Rules:
     * 1. Explicit INCLUDED photos stay in their place.
     * 2. Explicit EXCLUDED photos are never auto-matched to that place.
     * 3. Existing AUTO_MATCHED photos in a place stay in that place (adding a second overlapping place never steals them).
     * 4. Unassigned photos with valid GPS:
     *    - If within effectiveRadiusMeters of exactly 1 non-excluded place -> AUTO_MATCHED to that place (no boundary holdback!).
     *    - If within effectiveRadiusMeters of 2+ non-excluded places -> AUTO_MATCHED to the closest place ONLY if
     *      (secondClosestDist - closestDist >= OVERLAP_CLEAR_MARGIN_METERS); otherwise held in parent record.
     */
    static Map<String, Set<String>> evaluateAutoMatches(List<PersonalPlace> placesInParent,
                                                        List<DiscoveryPhotoRef> groupRefs,
                                                        Map<String, PersonalPlaceGpsPoint> gpsByUri) {
        LinkedHashMap<String, Set<String>> autoByPlaceId = new LinkedHashMap<>();
        if (placesInParent == null || placesInParent.isEmpty()) {
            return autoByPlaceId;
        }
        for (PersonalPlace place : placesInParent) {
            if (place != null) {
                autoByPlaceId.put(place.placeId, new LinkedHashSet<>());
            }
        }
        if (groupRefs == null || groupRefs.isEmpty()) {
            return autoByPlaceId;
        }

        // Track photos already claimed by explicit inclusion or existing auto-match
        LinkedHashSet<String> claimedUris = new LinkedHashSet<>();
        for (DiscoveryPhotoRef ref : groupRefs) {
            if (ref == null || ref.stale || ref.sourceUri == null || ref.sourceUri.trim().isEmpty()) {
                continue;
            }
            String uri = ref.sourceUri.trim();
            for (PersonalPlace place : placesInParent) {
                if (place != null && place.isExplicitlyIncluded(ref)) {
                    claimedUris.add(uri);
                    break;
                }
            }
        }
        for (DiscoveryPhotoRef ref : groupRefs) {
            if (ref == null || ref.stale || ref.sourceUri == null || ref.sourceUri.trim().isEmpty()) {
                continue;
            }
            String uri = ref.sourceUri.trim();
            if (claimedUris.contains(uri)) {
                continue;
            }
            for (PersonalPlace place : placesInParent) {
                if (place != null && place.isAutoMatched(ref)) {
                    Set<String> bucket = autoByPlaceId.get(place.placeId);
                    if (bucket != null) {
                        bucket.add(uri);
                        claimedUris.add(uri);
                        break;
                    }
                }
            }
        }

        // Evaluate remaining unassigned photos
        for (DiscoveryPhotoRef ref : groupRefs) {
            if (ref == null || ref.stale || ref.sourceUri == null || ref.sourceUri.trim().isEmpty()) {
                continue;
            }
            String uri = ref.sourceUri.trim();
            if (claimedUris.contains(uri)) {
                continue;
            }
            PersonalPlaceGpsPoint pt = gpsByUri == null ? null : gpsByUri.get(uri);
            if (pt == null || !pt.hasCoordinates()) {
                continue;
            }
            PersonalPlace bestPlace = null;
            double bestDist = Double.POSITIVE_INFINITY;
            double secondBestDist = Double.POSITIVE_INFINITY;
            int matchingCount = 0;

            for (PersonalPlace place : placesInParent) {
                if (place == null || !place.hasCenterCoordinates || place.isExcluded(ref)) {
                    continue;
                }
                double dist = distanceMeters(place.centerLat, place.centerLng, pt.latitude, pt.longitude);
                if (dist <= place.effectiveRadiusMeters) {
                    matchingCount++;
                    if (dist < bestDist) {
                        secondBestDist = bestDist;
                        bestDist = dist;
                        bestPlace = place;
                    } else if (dist < secondBestDist) {
                        secondBestDist = dist;
                    }
                }
            }

            if (bestPlace != null) {
                if (matchingCount == 1 || (secondBestDist - bestDist) >= OVERLAP_CLEAR_MARGIN_METERS) {
                    Set<String> bucket = autoByPlaceId.get(bestPlace.placeId);
                    if (bucket != null) {
                        bucket.add(uri);
                        claimedUris.add(uri);
                    }
                }
            }
        }
        return autoByPlaceId;
    }

    /** Filters photo sections to only photos belonging to the given PersonalPlace. */
    static List<MemoryPhotoSection> filterSectionsForPlace(List<MemoryPhotoSection> sections, PersonalPlace place) {
        if (sections == null || sections.isEmpty() || place == null) {
            return Collections.emptyList();
        }
        ArrayList<MemoryPhotoSection> result = new ArrayList<>();
        for (MemoryPhotoSection section : sections) {
            if (section == null || section.photos == null || section.photos.isEmpty()) {
                continue;
            }
            ArrayList<MemoryPhotoItem> matchedPhotos = new ArrayList<>();
            for (MemoryPhotoItem photo : section.photos) {
                if (photo != null && place.contains(photo)) {
                    matchedPhotos.add(photo);
                }
            }
            if (!matchedPhotos.isEmpty()) {
                result.add(MemoryPhotoSection.fromCollectionPlace(
                        section.dateKey, section.dateText, place.name, matchedPhotos));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Filters photo sections to photos that do NOT belong to any PersonalPlace in the parent record ('나머지'). */
    static List<MemoryPhotoSection> filterSectionsForRemaining(List<MemoryPhotoSection> sections,
                                                               List<PersonalPlace> places) {
        if (sections == null || sections.isEmpty()) {
            return Collections.emptyList();
        }
        if (places == null || places.isEmpty()) {
            return sections;
        }
        ArrayList<MemoryPhotoSection> result = new ArrayList<>();
        for (MemoryPhotoSection section : sections) {
            if (section == null || section.photos == null || section.photos.isEmpty()) {
                continue;
            }
            ArrayList<MemoryPhotoItem> remaining = new ArrayList<>();
            for (MemoryPhotoItem photo : section.photos) {
                if (photo == null) {
                    continue;
                }
                boolean inAnyPlace = false;
                for (PersonalPlace place : places) {
                    if (place != null && place.contains(photo)) {
                        inAnyPlace = true;
                        break;
                    }
                }
                if (!inAnyPlace) {
                    remaining.add(photo);
                }
            }
            if (!remaining.isEmpty()) {
                result.add(MemoryPhotoSection.fromCollectionPlace(
                        section.dateKey, section.dateText, section.placeText, remaining));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Filters parent date notes to only dates present in the filtered sections. */
    static List<MemoryDateNote> filterNotesForSections(List<MemoryDateNote> notes,
                                                       List<MemoryPhotoSection> filteredSections) {
        if (notes == null || notes.isEmpty() || filteredSections == null || filteredSections.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> activeDates = new LinkedHashSet<>();
        for (MemoryPhotoSection section : filteredSections) {
            if (section != null && section.dateKey != null && !section.dateKey.isEmpty()) {
                activeDates.add(section.dateKey);
            }
        }
        ArrayList<MemoryDateNote> matched = new ArrayList<>();
        for (MemoryDateNote note : notes) {
            if (note != null && activeDates.contains(note.dateKey)) {
                matched.add(note);
            }
        }
        return Collections.unmodifiableList(matched);
    }
}
