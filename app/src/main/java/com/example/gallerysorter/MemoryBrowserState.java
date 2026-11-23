package com.example.gallerysorter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class MemoryBrowserState {
    final List<MemoryBrowserItem> items;

    private MemoryBrowserState(List<MemoryBrowserItem> items) {
        this.items = immutableCopy(items);
    }

    static MemoryBrowserState from(MemoryRepository repository) {
        if (repository == null) {
            return empty();
        }
        return fromRecords(repository.memories());
    }

    static MemoryBrowserState fromRecords(List<MemoryRecord> records) {
        if (records == null || records.isEmpty()) {
            return empty();
        }
        ArrayList<MemoryBrowserItem> items = new ArrayList<>();
        for (MemoryRecord record : records) {
            MemoryBrowserItem item = MemoryBrowserItem.from(record);
            if (item != null) {
                items.add(item);
            }
        }
        Collections.sort(items, MemoryBrowserItem.BY_RECENT_THEN_TITLE);
        return new MemoryBrowserState(items);
    }

    static MemoryBrowserState empty() {
        return new MemoryBrowserState(Collections.<MemoryBrowserItem>emptyList());
    }

    MemoryBrowserState withCounts(java.util.Map<String, String> counts) {
        ArrayList<MemoryBrowserItem> projected = new ArrayList<>();
        for (MemoryBrowserItem item : items) {
            String count = counts.get(item.memoryKey);
            projected.add(count == null ? item : item.withCountText(count));
        }
        return new MemoryBrowserState(projected);
    }

    MemoryBrowserState retainingRecords(List<MemoryRecord> records) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (MemoryRecord record : records) if (record != null) keys.add(record.memoryKey);
        ArrayList<MemoryBrowserItem> selected = new ArrayList<>();
        for (MemoryBrowserItem item : items) if (keys.contains(item.memoryKey)) selected.add(item);
        return new MemoryBrowserState(selected);
    }

    MemoryBrowserState withRecentCounts(java.util.Map<String, Integer> counts) {
        ArrayList<MemoryBrowserItem> projected = new ArrayList<>();
        for (MemoryBrowserItem item : items) {
            Integer count = counts.get(item.memoryKey);
            projected.add(count == null ? item : item.withRecentAddedCount(count));
        }
        Collections.sort(projected, MemoryBrowserItem.BY_RECENT_THEN_TITLE);
        return new MemoryBrowserState(projected);
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    MemoryBrowserItem item(String memoryKey) {
        String key = clean(memoryKey);
        if (key.isEmpty()) {
            return null;
        }
        for (MemoryBrowserItem item : items) {
            if (key.equals(item.memoryKey)) {
                return item;
            }
        }
        return null;
    }

    MemoryBrowserDetail detail(String memoryKey, MemoryRepository repository) {
        return detail(memoryKey, repository, null);
    }

    MemoryBrowserDetail detail(String memoryKey,
                               MemoryRepository repository,
                               MemoryMediaResolver.GalleryReader galleryReader) {
        MemoryBrowserItem item = item(memoryKey);
        if (item == null || repository == null) {
            return null;
        }
        return detail(item, repository.memory(memoryKey), repository, galleryReader);
    }

    static MemoryBrowserDetail detail(MemoryBrowserItem item, MemoryRecord record,
                                     MemoryRepository repository, MemoryMediaResolver.GalleryReader galleryReader) {
        if (item == null || record == null) return null;
        OrganizationLink exactLink = repository.usableMemoryLink(record);
        record=record.withKnownOutput(exactLink);
        MemoryMediaResolution resolution = MemoryMediaResolver.resolve(record, exactLink, galleryReader);
        MemoryBrowserDetail detail = MemoryBrowserDetail.from(item, record, resolution.refs);
        return resolution.source == MemoryMediaSourcePolicy.Source.UNAVAILABLE
                ? detail.withUnavailableCount() : detail;
    }

    private static List<MemoryBrowserItem> immutableCopy(List<MemoryBrowserItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

final class MemoryBrowserItem {
    static final Comparator<MemoryBrowserItem> BY_RECENT_THEN_TITLE = new Comparator<MemoryBrowserItem>() {
        @Override
        public int compare(MemoryBrowserItem left, MemoryBrowserItem right) {
            int newItems = Integer.compare(right.recentAddedCount, left.recentAddedCount);
            if (newItems != 0) {
                return newItems;
            }
            int date = Long.compare(right.sortDateMillis, left.sortDateMillis);
            if (date != 0) {
                return date;
            }
            return left.title.compareTo(right.title);
        }
    };

    final String memoryKey;
    final String title;
    final String subtitle;
    final String countText;
    final String dateText;
    final String cardDateText;
    final String coverUri;
    final MemorySourceType sourceType;
    final boolean discoveryOnly;
    final boolean organizedAlbum;
    final boolean canOpenPhotos;
    final boolean canOpenGalleryAlbum;
    final boolean canOrganize;
    final boolean canAddNewItems;
    final int recentAddedCount;
    final long sortDateMillis;

    private MemoryBrowserItem(String memoryKey,
                              String title,
                              String subtitle,
                              String countText,
                              String dateText,
                              String cardDateText,
                              String coverUri,
                              MemorySourceType sourceType,
                              boolean discoveryOnly,
                              boolean organizedAlbum,
                              boolean canOpenPhotos,
                              boolean canOpenGalleryAlbum,
                              boolean canOrganize,
                              boolean canAddNewItems,
                              int recentAddedCount,
                              long sortDateMillis) {
        this.memoryKey = clean(memoryKey);
        this.title = clean(title);
        this.subtitle = clean(subtitle);
        this.countText = clean(countText);
        this.dateText = clean(dateText);
        this.cardDateText = clean(cardDateText);
        this.coverUri = clean(coverUri);
        this.sourceType = sourceType == null ? MemorySourceType.DISCOVERED_ONLY : sourceType;
        this.discoveryOnly = discoveryOnly;
        this.organizedAlbum = organizedAlbum;
        this.canOpenPhotos = canOpenPhotos;
        this.canOpenGalleryAlbum = canOpenGalleryAlbum;
        this.canOrganize = canOrganize;
        this.canAddNewItems = canAddNewItems;
        this.recentAddedCount = Math.max(0, recentAddedCount);
        this.sortDateMillis = Math.max(0L, sortDateMillis);
    }

    static MemoryBrowserItem from(MemoryRecord record) {
        if (record == null || clean(record.memoryKey).isEmpty()) {
            return null;
        }
        String title = firstNonEmpty(record.displayName,
                firstNonEmpty(record.discoveryGroup == null ? "" : record.discoveryGroup.placeName,
                        record.title, "", ""), record.canonicalPlaceName, record.placeKey);
        if (title.isEmpty()) {
            return null;
        }
        return new MemoryBrowserItem(
                record.memoryKey,
                title,
                subtitle(record),
                MemoryMediaCounts.forRecord(record),
                dateText(record.startDateMillis, record.endDateMillis),
                monthRangeText(record.startDateMillis, record.endDateMillis),
                record.coverUri,
                record.sourceType,
                record.sourceType == MemorySourceType.DISCOVERED_ONLY,
                record.sourceType == MemorySourceType.ORGANIZED_ALBUM,
                record.canOpenPhotos,
                record.canOpenGalleryAlbum,
                record.canOrganize,
                record.canAddNewItems,
                recentAddedCount(record),
                record.endDateMillis > 0L ? record.endDateMillis : record.startDateMillis);
    }

    private static String subtitle(MemoryRecord record) {
        if (record.sourceType == MemorySourceType.DISCOVERED_ONLY) {
            return "앨범 만들기 전";
        }
        if (record.sourceType == MemorySourceType.MIXED) {
            return "앱 안 보기 + 갤러리 앨범";
        }
        return "정리된 앨범";
    }

    MemoryBrowserItem withCountText(String value) {
        return new MemoryBrowserItem(memoryKey, title, subtitle, value, dateText, cardDateText,
                coverUri, sourceType, discoveryOnly, organizedAlbum, canOpenPhotos,
                canOpenGalleryAlbum, canOrganize, canAddNewItems, recentAddedCount, sortDateMillis);
    }

    MemoryBrowserItem withRecentAddedCount(int value) {
        return new MemoryBrowserItem(memoryKey, title, subtitle, countText, dateText, cardDateText,
                coverUri, sourceType, discoveryOnly, organizedAlbum, canOpenPhotos,
                canOpenGalleryAlbum, canOrganize, canAddNewItems, value, sortDateMillis);
    }

    private static int recentAddedCount(MemoryRecord record) {
        if (record == null || record.discoveryGroup == null) {
            return 0;
        }
        int count = 0;
        long currentVersion = record.discoveryGroup.snapshotVersion;
        for (DiscoveryPhotoRef ref : record.discoveryGroup.photoRefs) {
            if (ref != null && ref.firstSeenSnapshotVersion == currentVersion) {
                count++;
            }
        }
        return count;
    }

    private static String dateText(long startMillis, long endMillis) {
        if (startMillis <= 0L && endMillis <= 0L) {
            return "";
        }
        long start = startMillis > 0L ? startMillis : endMillis;
        long end = endMillis > 0L ? endMillis : startMillis;
        String startText = formatDate(start);
        String endText = formatDate(end);
        if (startText.equals(endText)) {
            return startText;
        }
        return startText + " ~ " + endText;
    }

    private static String formatDate(long millis) {
        return new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(new Date(millis));
    }

    private static String monthRangeText(long startMillis, long endMillis) {
        if (startMillis <= 0L && endMillis <= 0L) {
            return "";
        }
        long start = startMillis > 0L ? startMillis : endMillis;
        long end = endMillis > 0L ? endMillis : startMillis;
        String startText = new SimpleDateFormat("yyyy.M", Locale.KOREA).format(new Date(start));
        String endText = new SimpleDateFormat("yyyy.M", Locale.KOREA).format(new Date(end));
        return startText.equals(endText) ? startText : startText + "~" + endText;
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

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

final class MemoryBrowserDetail {
    final List<MemoryDateNote> notes;
    final MemoryBrowserItem item;
    final MemoryRecord record;
    final List<String> sourceUris;
    final List<MemoryPhotoSection> photoSections;
    final boolean canOpenPhotos;
    final boolean canOpenGalleryAlbum;
    final boolean canOrganize;
    final boolean mediaResolved;

    private MemoryBrowserDetail(MemoryBrowserItem item,
                                MemoryRecord record,
                                List<String> sourceUris,
                                List<MemoryPhotoSection> photoSections,
                                boolean canOpenPhotos,
                                boolean canOpenGalleryAlbum,
                                boolean canOrganize,
                                boolean mediaResolved) {
        this(item,record,sourceUris,photoSections,canOpenPhotos,canOpenGalleryAlbum,canOrganize,mediaResolved,Collections.emptyList());
    }
    private MemoryBrowserDetail(MemoryBrowserItem item,MemoryRecord record,List<String> sourceUris,
                                List<MemoryPhotoSection> photoSections,boolean canOpenPhotos,
                                boolean canOpenGalleryAlbum,boolean canOrganize,boolean mediaResolved,List<MemoryDateNote> notes) {
        this.item = item;
        this.record = record;
        this.sourceUris = immutableCopy(sourceUris);
        this.photoSections = immutableSections(photoSections);
        this.canOpenPhotos = canOpenPhotos;
        this.canOpenGalleryAlbum = canOpenGalleryAlbum;
        this.canOrganize = canOrganize;
        this.mediaResolved = mediaResolved;
        this.notes=Collections.unmodifiableList(new ArrayList<>(notes));
    }

    MemoryBrowserDetail withNotes(List<MemoryDateNote> notes) {
        return new MemoryBrowserDetail(item,record,sourceUris,MemoryPhotoSection.withNotes(photoSections,notes),
                canOpenPhotos||!notes.isEmpty(),canOpenGalleryAlbum,canOrganize,mediaResolved||!notes.isEmpty(),notes);
    }

    static MemoryBrowserDetail from(MemoryBrowserItem item,
                                    MemoryRecord record,
                                    List<DiscoveryPhotoRef> refs) {
        if (item == null || record == null) {
            return null;
        }
        List<MemoryPhotoSection> sections = MemoryPhotoSection.fromDiscoveryRefs(refs);
        List<String> sourceUris = MemoryPhotoSection.sourceUris(sections);
        return new MemoryBrowserDetail(
                item.withCountText(MemoryMediaCounts.fromSections(sections).text()),
                record,
                sourceUris,
                sections,
                !sourceUris.isEmpty(),
                record.canOpenGalleryAlbum,
                MemoryAlbumAction.canPrepare(record, refs) && !sourceUris.isEmpty(), true);
    }

    MemoryBrowserDetail withUnavailableCount() {
        return new MemoryBrowserDetail(item.withCountText("불러오기 필요"), record, sourceUris,
                photoSections, canOpenPhotos, canOpenGalleryAlbum, false, false,notes);
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<MemoryPhotoSection> immutableSections(List<MemoryPhotoSection> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
