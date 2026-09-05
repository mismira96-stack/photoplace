package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only, place-preserving projection of an app-only memory collection. */
final class GroupMemoryDetail {
    final String collectionId;
    final String title;
    final List<GroupMemoryDateSection> dates;

    GroupMemoryDetail(String collectionId, String title, List<GroupMemoryDateSection> dates) {
        this.collectionId = clean(collectionId);
        this.title = clean(title);
        this.dates = immutableCopy(dates);
    }

    private static List<GroupMemoryDateSection> immutableCopy(List<GroupMemoryDateSection> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

final class GroupMemoryDateSection {
    final String dateKey;
    final String dateText;
    final List<GroupMemoryPlaceSection> places;

    GroupMemoryDateSection(String dateKey, String dateText, List<GroupMemoryPlaceSection> places) {
        this.dateKey = clean(dateKey);
        this.dateText = clean(dateText);
        this.places = immutableCopy(places);
    }

    private static List<GroupMemoryPlaceSection> immutableCopy(List<GroupMemoryPlaceSection> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

final class GroupMemoryPlaceSection {
    final String stableMemoryId;
    final String memoryKey;
    final String placeTitle;
    final String countryCode;
    final String adminArea;
    final String noteText;
    final List<MemoryPhotoItem> photos;

    GroupMemoryPlaceSection(String stableMemoryId,
                            String memoryKey,
                            String placeTitle,
                            String countryCode,
                            String adminArea,
                            String noteText,
                            List<MemoryPhotoItem> photos) {
        this.stableMemoryId = clean(stableMemoryId);
        this.memoryKey = clean(memoryKey);
        this.placeTitle = clean(placeTitle);
        this.countryCode = clean(countryCode);
        this.adminArea = clean(adminArea);
        this.noteText = clean(noteText);
        this.photos = immutableCopy(photos);
    }

    private static List<MemoryPhotoItem> immutableCopy(List<MemoryPhotoItem> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
