package com.example.gallerysorter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Read-only facade for memory browsing. It adapts discovery-only groups and organized albums into
 * MemoryRecord objects without writing discovery data into album history.
 */
final class MemoryRepository {
    private static final String ORGANIZED_PREFIX = "path:";
    private static final String ALBUM_PREFIX = "album:";

    private final DiscoverySnapshot discoverySnapshot;
    private final List<StoredAlbumSummary> organizedAlbums;

    MemoryRepository(DiscoverySnapshot discoverySnapshot, List<StoredAlbumSummary> organizedAlbums) {
        this.discoverySnapshot = discoverySnapshot;
        this.organizedAlbums = immutableCopy(organizedAlbums);
    }

    List<MemoryRecord> memories() {
        LinkedHashMap<String, MemoryRecord> records = new LinkedHashMap<>();
        LinkedHashMap<String, List<MemoryRecord>> organizedByPlaceKey = new LinkedHashMap<>();
        for (StoredAlbumSummary summary : organizedAlbums) {
            MemoryRecord record = fromOrganizedAlbum(summary);
            if (record != null) {
                records.put(record.memoryKey, record);
                addByPlaceIdentity(organizedByPlaceKey, record);
            }
        }
        if (discoverySnapshot != null) {
            for (DiscoveryMemoryGroup group : discoverySnapshot.groups) {
                MemoryRecord discoveryRecord = fromDiscoveryGroup(group);
                if (discoveryRecord == null) {
                    continue;
                }
                MemoryRecord existing = records.get(discoveryRecord.memoryKey);
                if (existing == null) {
                    existing = findCompatibleOrganizedRecord(organizedByPlaceKey, discoveryRecord);
                }
                if (existing == null) {
                    records.put(discoveryRecord.memoryKey, discoveryRecord);
                } else {
                    records.put(existing.memoryKey, merge(existing, discoveryRecord));
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(records.values()));
    }

    List<MemoryRecord> discoveryMemories() {
        LinkedHashMap<String, MemoryRecord> organizedByKey = new LinkedHashMap<>();
        LinkedHashMap<String, List<MemoryRecord>> organizedByPlaceKey = new LinkedHashMap<>();
        for (StoredAlbumSummary summary : organizedAlbums) {
            MemoryRecord record = fromOrganizedAlbum(summary);
            if (record != null) {
                organizedByKey.put(record.memoryKey, record);
                addByPlaceIdentity(organizedByPlaceKey, record);
            }
        }
        if (discoverySnapshot == null || discoverySnapshot.groups.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<MemoryRecord> records = new ArrayList<>();
        for (DiscoveryMemoryGroup group : discoverySnapshot.groups) {
            MemoryRecord discoveryRecord = fromDiscoveryGroup(group);
            if (discoveryRecord == null) {
                continue;
            }
            MemoryRecord existing = organizedByKey.get(discoveryRecord.memoryKey);
            if (existing == null) {
                existing = findCompatibleOrganizedRecord(organizedByPlaceKey, discoveryRecord);
            }
            records.add(existing == null ? discoveryRecord : merge(existing, discoveryRecord));
        }
        return Collections.unmodifiableList(records);
    }

    MemoryRecord memory(String memoryKey) {
        String key = clean(memoryKey);
        if (key.isEmpty()) {
            return null;
        }
        for (MemoryRecord record : memories()) {
            if (key.equals(record.memoryKey)) {
                return record;
            }
        }
        return null;
    }

    List<DiscoveryPhotoRef> discoveryPhotoRefs(String memoryKey) {
        MemoryRecord record = memory(memoryKey);
        if (record == null || record.discoveryGroup == null) {
            return Collections.emptyList();
        }
        return record.discoveryGroup.photoRefs;
    }

    static MemoryRecord fromDiscoveryGroup(DiscoveryMemoryGroup group) {
        if (group == null || clean(group.memoryKey).isEmpty()) {
            return null;
        }
        int availableCount = Math.max(0, group.itemCount - group.staleCount);
        return new MemoryRecord(
                group.memoryKey,
                group.placeKey,
                firstNonEmpty(group.placeName, group.placeKey),
                firstNonEmpty(group.placeName, group.placeKey),
                "",
                group.countryCode,
                group.countryName,
                group.adminArea,
                group.addressLine,
                group.itemCount,
                group.photoCount,
                group.videoCount,
                group.startDateMillis,
                group.endDateMillis,
                group.coverUri,
                MemorySourceType.DISCOVERED_ONLY,
                group,
                null,
                group.staleCount,
                availableCount,
                availableCount > 0,
                false,
                availableCount > 0,
                false);
    }

    static MemoryRecord fromOrganizedAlbum(StoredAlbumSummary summary) {
        if (summary == null || clean(summary.albumName).isEmpty()) {
            return null;
        }
        OrganizedAlbumRef organizedAlbum = new OrganizedAlbumRef(
                summary.relativePath,
                summary.albumName,
                summary.itemCount,
                summary.thumbnailUri,
                summary.countryCode,
                summary.countryName,
                parseDateMillis(summary.startDate),
                parseDateMillis(summary.endDate));
        boolean hasGalleryAlbum = !organizedAlbum.relativePath.isEmpty();
        return new MemoryRecord(
                organizedMemoryKey(summary),
                canonicalPlaceKey(summary),
                summary.albumName,
                canonicalPlaceName(summary.albumName),
                "",
                summary.countryCode,
                summary.countryName,
                summary.adminArea,
                summary.addressLine,
                summary.itemCount,
                summary.itemCount,
                0,
                organizedAlbum.startDateMillis,
                organizedAlbum.endDateMillis,
                summary.thumbnailUri,
                MemorySourceType.ORGANIZED_ALBUM,
                null,
                organizedAlbum,
                0,
                summary.itemCount,
                false,
                hasGalleryAlbum,
                false,
                false);
    }

    private static MemoryRecord merge(MemoryRecord existing, MemoryRecord incoming) {
        DiscoveryMemoryGroup discoveryGroup = existing.discoveryGroup != null
                ? existing.discoveryGroup
                : incoming.discoveryGroup;
        OrganizedAlbumRef organizedAlbum = existing.organizedAlbum != null
                ? existing.organizedAlbum
                : incoming.organizedAlbum;
        int staleCount = discoveryGroup == null ? 0 : discoveryGroup.staleCount;
        int availableCount = discoveryGroup == null
                ? Math.max(existing.availableCount, incoming.availableCount)
                : Math.max(0, discoveryGroup.itemCount - discoveryGroup.staleCount);
        int itemCount = Math.max(existing.itemCount, incoming.itemCount);
        int photoCount = Math.max(existing.photoCount, incoming.photoCount);
        int videoCount = Math.max(existing.videoCount, incoming.videoCount);
        long startDateMillis = minPositive(existing.startDateMillis, incoming.startDateMillis);
        long endDateMillis = Math.max(existing.endDateMillis, incoming.endDateMillis);
        return new MemoryRecord(
                existing.memoryKey,
                firstNonEmpty(existing.placeKey, incoming.placeKey),
                firstNonEmpty(existing.title, incoming.title),
                firstNonEmpty(existing.canonicalPlaceName, incoming.canonicalPlaceName),
                firstNonEmpty(existing.displayName, incoming.displayName),
                firstNonEmpty(existing.countryCode, incoming.countryCode),
                firstNonEmpty(existing.countryName, incoming.countryName),
                firstNonEmpty(existing.adminArea, incoming.adminArea),
                firstNonEmpty(existing.addressLine, incoming.addressLine),
                itemCount,
                photoCount,
                videoCount,
                startDateMillis,
                endDateMillis,
                firstNonEmpty(existing.coverUri, incoming.coverUri),
                MemorySourceType.MIXED,
                discoveryGroup,
                organizedAlbum,
                staleCount,
                availableCount,
                discoveryGroup != null && availableCount > 0,
                organizedAlbum != null && !organizedAlbum.relativePath.isEmpty(),
                false,
                discoveryGroup != null && organizedAlbum != null && availableCount > 0);
    }

    private static List<StoredAlbumSummary> immutableCopy(List<StoredAlbumSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(summaries));
    }

    private static String organizedMemoryKey(StoredAlbumSummary summary) {
        String relativePath = clean(summary.relativePath);
        if (!relativePath.isEmpty()) {
            return ORGANIZED_PREFIX + relativePath;
        }
        return ALBUM_PREFIX + clean(summary.albumName);
    }

    private static String canonicalPlaceKey(StoredAlbumSummary summary) {
        String countryCode = clean(summary.countryCode);
        String canonicalName = canonicalPlaceName(summary.albumName);
        if (!countryCode.isEmpty() && !canonicalName.isEmpty()) {
            return countryCode + "|" + canonicalName;
        }
        return canonicalName;
    }

    private static String placeIdentityKey(MemoryRecord record) {
        if (record == null) {
            return "";
        }
        String countryCode = clean(record.countryCode);
        String placeName = firstNonEmpty(record.canonicalPlaceName, canonicalPlaceName(record.title));
        if (countryCode.isEmpty() || placeName.isEmpty()) {
            return "";
        }
        return countryCode + "|" + placeName;
    }

    private static void addByPlaceIdentity(LinkedHashMap<String, List<MemoryRecord>> recordsByPlace,
                                           MemoryRecord record) {
        String key = placeIdentityKey(record);
        if (key.isEmpty()) {
            return;
        }
        List<MemoryRecord> records = recordsByPlace.get(key);
        if (records == null) {
            records = new ArrayList<>();
            recordsByPlace.put(key, records);
        }
        records.add(record);
    }

    private static MemoryRecord findCompatibleOrganizedRecord(LinkedHashMap<String, List<MemoryRecord>> organizedByPlaceKey,
                                                              MemoryRecord discoveryRecord) {
        List<MemoryRecord> candidates = organizedByPlaceKey.get(placeIdentityKey(discoveryRecord));
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (MemoryRecord candidate : candidates) {
            if (isCompatibleFallbackMerge(candidate, discoveryRecord)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isCompatibleFallbackMerge(MemoryRecord organizedRecord, MemoryRecord discoveryRecord) {
        return hasSameAdminArea(organizedRecord, discoveryRecord)
                || dateRangesOverlap(organizedRecord, discoveryRecord);
    }

    private static boolean hasSameAdminArea(MemoryRecord first, MemoryRecord second) {
        String firstAdmin = clean(first == null ? "" : first.adminArea);
        String secondAdmin = clean(second == null ? "" : second.adminArea);
        return !firstAdmin.isEmpty() && firstAdmin.equals(secondAdmin);
    }

    private static boolean dateRangesOverlap(MemoryRecord first, MemoryRecord second) {
        long firstStart = normalizedStart(first == null ? 0L : first.startDateMillis, first == null ? 0L : first.endDateMillis);
        long firstEnd = normalizedEnd(first == null ? 0L : first.startDateMillis, first == null ? 0L : first.endDateMillis);
        long secondStart = normalizedStart(second == null ? 0L : second.startDateMillis, second == null ? 0L : second.endDateMillis);
        long secondEnd = normalizedEnd(second == null ? 0L : second.startDateMillis, second == null ? 0L : second.endDateMillis);
        if (firstStart <= 0L || firstEnd <= 0L || secondStart <= 0L || secondEnd <= 0L) {
            return false;
        }
        return firstStart <= secondEnd && secondStart <= firstEnd;
    }

    private static long normalizedStart(long startMillis, long endMillis) {
        if (startMillis > 0L) {
            return startMillis;
        }
        return Math.max(0L, endMillis);
    }

    private static long normalizedEnd(long startMillis, long endMillis) {
        if (endMillis > 0L) {
            return endMillis;
        }
        return Math.max(0L, startMillis);
    }

    private static String canonicalPlaceName(String albumName) {
        String value = clean(albumName);
        return value.endsWith("에서") ? value.substring(0, value.length() - 2) : value;
    }

    private static long parseDateMillis(String date) {
        String value = clean(date);
        if (value.isEmpty()) {
            return 0L;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(value).getTime();
        } catch (ParseException unused) {
            return 0L;
        }
    }

    private static long minPositive(long first, long second) {
        if (first <= 0L) {
            return Math.max(0L, second);
        }
        if (second <= 0L) {
            return first;
        }
        return Math.min(first, second);
    }

    private static String firstNonEmpty(String first, String second) {
        String cleanFirst = clean(first);
        return cleanFirst.isEmpty() ? clean(second) : cleanFirst;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
