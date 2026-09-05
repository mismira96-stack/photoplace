package com.example.gallerysorter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Builds a read-only collection viewer without mutating discovery records or date notes. */
final class MemoryCollectionResolver {
    private final MemoryIdentityRegistryStore identityRegistry;
    private final MemoryDateNoteStore noteStore;

    MemoryCollectionResolver(MemoryIdentityRegistryStore identityRegistry,
                             MemoryDateNoteStore noteStore) {
        this.identityRegistry = identityRegistry;
        this.noteStore = noteStore;
    }

    GroupMemoryDetail resolve(MemoryCollection collection, List<MemoryRecord> discoveryRecords) {
        if (collection == null || !collection.isValid()) {
            return null;
        }
        LinkedHashMap<String, MemoryRecord> recordsByStableId = recordsByStableId(discoveryRecords);
        LinkedHashMap<String, DateBuilder> dates = new LinkedHashMap<>();
        Set<String> seenUris = new HashSet<>();

        for (MemoryCollection.Member member : collection.members) {
            MemoryRecord record = member == null ? null : recordsByStableId.get(member.stableMemoryId);
            if (record == null || record.discoveryGroup == null) {
                continue;
            }
            addRecord(member.stableMemoryId, record, dates, seenUris);
        }

        ArrayList<DateBuilder> orderedDates = new ArrayList<>(dates.values());
        Collections.sort(orderedDates, new Comparator<DateBuilder>() {
            @Override
            public int compare(DateBuilder left, DateBuilder right) {
                return right.dateKey.compareTo(left.dateKey);
            }
        });
        ArrayList<GroupMemoryDateSection> sections = new ArrayList<>();
        for (DateBuilder builder : orderedDates) {
            sections.add(builder.build());
        }
        return new GroupMemoryDetail(collection.collectionId, collection.title, sections);
    }

    private LinkedHashMap<String, MemoryRecord> recordsByStableId(List<MemoryRecord> records) {
        LinkedHashMap<String, MemoryRecord> result = new LinkedHashMap<>();
        if (records == null || identityRegistry == null) {
            return result;
        }
        for (MemoryRecord record : records) {
            if (record == null || record.discoveryGroup == null || clean(record.memoryKey).isEmpty()) {
                continue;
            }
            String stableId = identityRegistry.findStableId(record.memoryKey);
            if (!stableId.isEmpty() && !result.containsKey(stableId)) {
                result.put(stableId, record);
            }
        }
        return result;
    }

    private void addRecord(String stableMemoryId,
                           MemoryRecord record,
                           LinkedHashMap<String, DateBuilder> dates,
                           Set<String> seenUris) {
        ArrayList<DiscoveryPhotoRef> refs = new ArrayList<>(record.discoveryGroup.photoRefs);
        Collections.sort(refs, new Comparator<DiscoveryPhotoRef>() {
            @Override
            public int compare(DiscoveryPhotoRef left, DiscoveryPhotoRef right) {
                return Long.compare(sortTime(right), sortTime(left));
            }
        });
        for (DiscoveryPhotoRef ref : refs) {
            if (ref == null || ref.stale || clean(ref.sourceUri).isEmpty() || !seenUris.add(ref.sourceUri)) {
                continue;
            }
            String dateKey = dateKey(ref.takenAtMillis);
            DateBuilder date = dates.get(dateKey);
            if (date == null) {
                date = new DateBuilder(dateKey, dateText(ref.takenAtMillis));
                dates.put(dateKey, date);
            }
            date.add(stableMemoryId, record, ref, noteFor(stableMemoryId, dateKey));
        }
    }

    private String noteFor(String stableMemoryId, String dateKey) {
        if (noteStore == null || "unknown".equals(dateKey)) {
            return "";
        }
        MemoryDateNote note = noteStore.get(stableMemoryId, dateKey);
        return note == null ? "" : note.text;
    }

    private static long sortTime(DiscoveryPhotoRef ref) {
        return ref == null || ref.takenAtMillis <= 0L ? 0L : ref.takenAtMillis;
    }

    private static String dateKey(long millis) {
        return millis <= 0L ? "unknown" : new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(new Date(millis));
    }

    private static String dateText(long millis) {
        if (millis <= 0L) {
            return "날짜 없음";
        }
        Calendar calendar = Calendar.getInstance(Locale.KOREA);
        calendar.setTimeInMillis(millis);
        String pattern = calendar.get(Calendar.YEAR) == Calendar.getInstance(Locale.KOREA).get(Calendar.YEAR)
                ? "M월 d일" : "yyyy년 M월 d일";
        return new SimpleDateFormat(pattern, Locale.KOREA).format(new Date(millis));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class DateBuilder {
        final String dateKey;
        final String dateText;
        final LinkedHashMap<String, PlaceBuilder> places = new LinkedHashMap<>();

        DateBuilder(String dateKey, String dateText) {
            this.dateKey = dateKey;
            this.dateText = dateText;
        }

        void add(String stableMemoryId, MemoryRecord record, DiscoveryPhotoRef ref, String noteText) {
            PlaceBuilder place = places.get(stableMemoryId);
            if (place == null) {
                place = new PlaceBuilder(stableMemoryId, record, noteText);
                places.put(stableMemoryId, place);
            }
            place.photos.add(new MemoryPhotoItem(ref.sourceUri, ref.mediaKind, ref.mimeType, ref.takenAtMillis));
        }

        GroupMemoryDateSection build() {
            ArrayList<GroupMemoryPlaceSection> result = new ArrayList<>();
            for (PlaceBuilder place : places.values()) {
                result.add(place.build());
            }
            return new GroupMemoryDateSection(dateKey, dateText, result);
        }
    }

    private static final class PlaceBuilder {
        final String stableMemoryId;
        final MemoryRecord record;
        final String noteText;
        final ArrayList<MemoryPhotoItem> photos = new ArrayList<>();

        PlaceBuilder(String stableMemoryId, MemoryRecord record, String noteText) {
            this.stableMemoryId = stableMemoryId;
            this.record = record;
            this.noteText = noteText;
        }

        GroupMemoryPlaceSection build() {
            return new GroupMemoryPlaceSection(
                    stableMemoryId,
                    record.memoryKey,
                    record.title,
                    record.countryCode,
                    record.adminArea,
                    noteText,
                    photos);
        }
    }
}
