package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/** Read-only country container for discovery and legacy organized sources. */
final class OverseasCountryProjection {
    final String countryCode;
    final String countryName;
    final List<MemoryRecord> discoveryRecords;
    final List<StoredAlbumSummary> organizedAlbums;

    private OverseasCountryProjection(String countryCode,
                                      List<MemoryRecord> discoveryRecords,
                                      List<StoredAlbumSummary> organizedAlbums) {
        this.countryCode = countryCode;
        this.countryName = CountryIdentityNormalizer.displayNameForCode(countryCode);
        this.discoveryRecords = immutableCopy(discoveryRecords);
        this.organizedAlbums = immutableCopy(organizedAlbums);
    }

    static List<OverseasCountryProjection> build(List<MemoryRecord> discoveryRecords,
                                                  List<StoredAlbumSummary> organizedAlbums) {
        LinkedHashMap<String, Bucket> buckets = new LinkedHashMap<>();
        addDiscovery(buckets, discoveryRecords);
        addOrganized(buckets, organizedAlbums);
        ArrayList<OverseasCountryProjection> result = new ArrayList<>();
        for (Bucket bucket : buckets.values()) {
            result.add(new OverseasCountryProjection(bucket.countryCode, bucket.discovery, bucket.organized));
        }
        return Collections.unmodifiableList(result);
    }

    boolean hasDiscovery() {
        return !discoveryRecords.isEmpty();
    }

    boolean hasOrganizedAlbums() {
        return !organizedAlbums.isEmpty();
    }

    int sourceCount() {
        return discoveryRecords.size() + organizedAlbums.size();
    }

    private static void addDiscovery(LinkedHashMap<String, Bucket> buckets, List<MemoryRecord> records) {
        if (records == null) {
            return;
        }
        for (MemoryRecord record : records) {
            if (record == null) {
                continue;
            }
            String countryCode = CountryIdentityNormalizer.countryCode(record.countryCode, record.countryName);
            if (countryCode.isEmpty() || CountryIdentityNormalizer.isKorea(countryCode, record.countryName)) {
                continue;
            }
            Bucket bucket = bucket(buckets, countryCode);
            bucket.discovery.add(record);
        }
    }

    private static void addOrganized(LinkedHashMap<String, Bucket> buckets, List<StoredAlbumSummary> summaries) {
        if (summaries == null) {
            return;
        }
        for (StoredAlbumSummary summary : summaries) {
            String countryCode = OverseasMemoryGrouper.countryCodeFor(summary);
            if (countryCode.isEmpty()) {
                continue;
            }
            Bucket bucket = bucket(buckets, countryCode);
            bucket.organized.add(summary);
        }
    }

    private static Bucket bucket(LinkedHashMap<String, Bucket> buckets, String countryCode) {
        Bucket bucket = buckets.get(countryCode);
        if (bucket == null) {
            bucket = new Bucket(countryCode);
            buckets.put(countryCode, bucket);
        }
        return bucket;
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static final class Bucket {
        final String countryCode;
        final ArrayList<MemoryRecord> discovery = new ArrayList<>();
        final ArrayList<StoredAlbumSummary> organized = new ArrayList<>();

        Bucket(String countryCode) {
            this.countryCode = countryCode;
        }
    }
}
