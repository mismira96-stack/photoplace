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
        MemoryBrowserItem item = item(memoryKey);
        if (item == null || repository == null) {
            return null;
        }
        return MemoryBrowserDetail.from(item, repository.memory(memoryKey), repository.discoveryPhotoRefs(memoryKey));
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
    final String coverUri;
    final MemorySourceType sourceType;
    final boolean discoveryOnly;
    final boolean organizedAlbum;
    final boolean canOpenPhotos;
    final boolean canOpenGalleryAlbum;
    final boolean canOrganize;
    final boolean canAddNewItems;
    final long sortDateMillis;

    private MemoryBrowserItem(String memoryKey,
                              String title,
                              String subtitle,
                              String countText,
                              String dateText,
                              String coverUri,
                              MemorySourceType sourceType,
                              boolean discoveryOnly,
                              boolean organizedAlbum,
                              boolean canOpenPhotos,
                              boolean canOpenGalleryAlbum,
                              boolean canOrganize,
                              boolean canAddNewItems,
                              long sortDateMillis) {
        this.memoryKey = clean(memoryKey);
        this.title = clean(title);
        this.subtitle = clean(subtitle);
        this.countText = clean(countText);
        this.dateText = clean(dateText);
        this.coverUri = clean(coverUri);
        this.sourceType = sourceType == null ? MemorySourceType.DISCOVERED_ONLY : sourceType;
        this.discoveryOnly = discoveryOnly;
        this.organizedAlbum = organizedAlbum;
        this.canOpenPhotos = canOpenPhotos;
        this.canOpenGalleryAlbum = canOpenGalleryAlbum;
        this.canOrganize = canOrganize;
        this.canAddNewItems = canAddNewItems;
        this.sortDateMillis = Math.max(0L, sortDateMillis);
    }

    static MemoryBrowserItem from(MemoryRecord record) {
        if (record == null || clean(record.memoryKey).isEmpty()) {
            return null;
        }
        String title = firstNonEmpty(record.displayName, record.title, record.canonicalPlaceName, record.placeKey);
        if (title.isEmpty()) {
            return null;
        }
        return new MemoryBrowserItem(
                record.memoryKey,
                title,
                subtitle(record),
                countText(record.itemCount),
                dateText(record.startDateMillis, record.endDateMillis),
                record.coverUri,
                record.sourceType,
                record.sourceType == MemorySourceType.DISCOVERED_ONLY,
                record.sourceType == MemorySourceType.ORGANIZED_ALBUM,
                record.canOpenPhotos,
                record.canOpenGalleryAlbum,
                record.canOrganize,
                record.canAddNewItems,
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

    private static String countText(int itemCount) {
        return Math.max(0, itemCount) + "개";
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
    final MemoryBrowserItem item;
    final MemoryRecord record;
    final List<String> sourceUris;
    final List<MemoryPhotoSection> photoSections;
    final boolean canOpenPhotos;
    final boolean canOpenGalleryAlbum;
    final boolean canOrganize;

    private MemoryBrowserDetail(MemoryBrowserItem item,
                                MemoryRecord record,
                                List<String> sourceUris,
                                List<MemoryPhotoSection> photoSections,
                                boolean canOpenPhotos,
                                boolean canOpenGalleryAlbum,
                                boolean canOrganize) {
        this.item = item;
        this.record = record;
        this.sourceUris = immutableCopy(sourceUris);
        this.photoSections = immutableSections(photoSections);
        this.canOpenPhotos = canOpenPhotos;
        this.canOpenGalleryAlbum = canOpenGalleryAlbum;
        this.canOrganize = canOrganize;
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
                item,
                record,
                sourceUris,
                sections,
                record.canOpenPhotos && !sourceUris.isEmpty(),
                record.canOpenGalleryAlbum,
                record.canOrganize && !sourceUris.isEmpty());
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
