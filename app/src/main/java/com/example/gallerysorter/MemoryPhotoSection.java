package com.example.gallerysorter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

final class MemoryPhotoSection {
    final String dateKey;
    final String dateText;
    final String placeText;
    final List<MemoryPhotoItem> photos;

    private MemoryPhotoSection(String dateKey,
                               String dateText,
                               String placeText,
                               List<MemoryPhotoItem> photos) {
        this.dateKey = clean(dateKey);
        this.dateText = clean(dateText);
        this.placeText = clean(placeText);
        this.photos = immutableCopy(photos);
    }

    static List<MemoryPhotoSection> fromDiscoveryRefs(List<DiscoveryPhotoRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<DiscoveryPhotoRef> validRefs = new ArrayList<>();
        for (DiscoveryPhotoRef ref : refs) {
            if (ref != null && !ref.stale && !clean(ref.sourceUri).isEmpty()) {
                validRefs.add(ref);
            }
        }
        if (validRefs.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.sort(validRefs, new Comparator<DiscoveryPhotoRef>() {
            @Override
            public int compare(DiscoveryPhotoRef left, DiscoveryPhotoRef right) {
                return Long.compare(sortTime(right), sortTime(left));
            }
        });

        LinkedHashMap<String, SectionBuilder> builders = new LinkedHashMap<>();
        for (DiscoveryPhotoRef ref : validRefs) {
            String key = dateKey(ref.takenAtMillis);
            SectionBuilder builder = builders.get(key);
            if (builder == null) {
                builder = new SectionBuilder(key, dateText(ref.takenAtMillis), placeText(ref));
                builders.put(key, builder);
            }
            builder.add(ref);
        }

        ArrayList<MemoryPhotoSection> sections = new ArrayList<>();
        for (SectionBuilder builder : builders.values()) {
            sections.add(builder.build());
        }
        return Collections.unmodifiableList(sections);
    }

    static List<String> sourceUris(List<MemoryPhotoSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> uris = new ArrayList<>();
        for (MemoryPhotoSection section : sections) {
            if (section == null || section.photos == null) {
                continue;
            }
            for (MemoryPhotoItem photo : section.photos) {
                if (photo != null && !photo.sourceUri.isEmpty()) {
                    uris.add(photo.sourceUri);
                }
            }
        }
        return Collections.unmodifiableList(uris);
    }

    private static long sortTime(DiscoveryPhotoRef ref) {
        return ref == null || ref.takenAtMillis <= 0L ? 0L : ref.takenAtMillis;
    }

    private static String dateKey(long millis) {
        if (millis <= 0L) {
            return "unknown";
        }
        return new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(new Date(millis));
    }

    private static String dateText(long millis) {
        if (millis <= 0L) {
            return "날짜 없음";
        }
        return new SimpleDateFormat("M월 d일", Locale.KOREA).format(new Date(millis));
    }

    private static String placeText(DiscoveryPhotoRef ref) {
        if (ref == null) {
            return "";
        }
        return firstNonEmpty(ref.placeName, ref.locationKey, ref.adminArea, ref.countryName);
    }

    private static String firstNonEmpty(String first, String second, String third, String fourth) {
        String value = clean(first);
        if (!value.isEmpty()) {
            return value;
        }
        value = clean(second);
        if (!value.isEmpty()) {
            return value;
        }
        value = clean(third);
        if (!value.isEmpty()) {
            return value;
        }
        return clean(fourth);
    }

    private static List<MemoryPhotoItem> immutableCopy(List<MemoryPhotoItem> photos) {
        if (photos == null || photos.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(photos));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class SectionBuilder {
        final String dateKey;
        final String dateText;
        final String placeText;
        final ArrayList<MemoryPhotoItem> photos = new ArrayList<>();

        SectionBuilder(String dateKey, String dateText, String placeText) {
            this.dateKey = dateKey;
            this.dateText = dateText;
            this.placeText = placeText;
        }

        void add(DiscoveryPhotoRef ref) {
            photos.add(new MemoryPhotoItem(
                    ref.sourceUri,
                    ref.mediaKind,
                    ref.mimeType,
                    ref.takenAtMillis));
        }

        MemoryPhotoSection build() {
            return new MemoryPhotoSection(dateKey, dateText, placeText, photos);
        }
    }
}

final class MemoryPhotoItem {
    final String sourceUri;
    final MediaKind mediaKind;
    final String mimeType;
    final long takenAtMillis;

    MemoryPhotoItem(String sourceUri, MediaKind mediaKind, String mimeType, long takenAtMillis) {
        this.sourceUri = sourceUri == null ? "" : sourceUri.trim();
        this.mediaKind = mediaKind == null ? MediaKind.PHOTO : mediaKind;
        this.mimeType = mimeType == null ? "" : mimeType.trim();
        this.takenAtMillis = Math.max(0L, takenAtMillis);
    }
}
