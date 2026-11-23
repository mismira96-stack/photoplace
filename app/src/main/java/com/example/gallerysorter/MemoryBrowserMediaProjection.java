package com.example.gallerysorter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Worker-only card counts use exactly the same resolved media as the detail view. */
final class MemoryBrowserMediaProjection {
    final Map<String, String> counts;
    final boolean complete;
    private final Map<String, MemoryMediaCounts> resolvedCounts;

    private MemoryBrowserMediaProjection(Map<String, String> counts, boolean complete,
                                         Map<String, MemoryMediaCounts> resolvedCounts) {
        this.counts = Collections.unmodifiableMap(counts);
        this.complete = complete;
        this.resolvedCounts = Collections.unmodifiableMap(resolvedCounts);
    }

    MemoryBrowserSummary summary(List<MemoryRecord> records) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (Map.Entry<String, MemoryMediaCounts> entry : resolvedCounts.entrySet()) {
            totals.put(entry.getKey(), entry.getValue().photos + entry.getValue().videos);
        }
        return MemoryBrowserSummary.from(records, totals);
    }

    static MemoryBrowserMediaProjection build(List<MemoryRecord> records, MemoryRepository repository,
                                               MemoryMediaResolver.GalleryReader reader) {
        Map<String, String> counts = new LinkedHashMap<>();
        Map<String, MemoryMediaCounts> resolvedCounts = new LinkedHashMap<>();
        boolean complete = true;
        for (MemoryRecord record : records) {
            // Album-only legacy rows keep their stored estimate. Do not scan every unrelated
            // foreign album merely to correct discovered-place cards such as Seongnam/Suwon.
            if (record.discoveryGroup == null) {
                counts.put(record.memoryKey, MemoryMediaCounts.forRecord(record));
                continue;
            }
            MemoryMediaResolution media = MemoryMediaResolver.resolve(record, repository.usableMemoryLink(record), reader);
            boolean available = media.source != MemoryMediaSourcePolicy.Source.UNAVAILABLE;
            if (available) {
                MemoryMediaCounts actual = MemoryMediaCounts.fromRefs(media.refs);
                counts.put(record.memoryKey, actual.text());
                resolvedCounts.put(record.memoryKey, actual);
            } else counts.put(record.memoryKey, "불러오기 필요");
            complete &= available;
        }
        return new MemoryBrowserMediaProjection(counts, complete, resolvedCounts);
    }

    /** One entry build shares Gallery reads between place cards and collection summaries. */
    static MemoryMediaResolver.GalleryReader memoize(MemoryMediaResolver.GalleryReader reader) {
        return memoize(reader, Collections.<MemoryRecord>emptyList(), null);
    }

    static MemoryMediaResolver.GalleryReader memoize(MemoryMediaResolver.GalleryReader reader,
                                                     List<MemoryRecord> records,
                                                     MemoryRepository repository) {
        if (reader == null) return null;
        final MemoryMediaResolver.GalleryReader backing;
        if (reader instanceof MemoryMediaStoreReader && records != null && repository != null) {
            java.util.Set<String> paths = new java.util.LinkedHashSet<>();
            for (MemoryRecord record : records) {
                if (record == null) continue;
                OrganizationLink link = repository.usableMemoryLink(record);
                if (link != null && link.subjectType == OrganizationLink.SubjectType.MEMORY && !link.relativePath.isEmpty()) {
                    paths.add(link.relativePath);
                } else if (record.organizedAlbum != null && !record.organizedAlbum.relativePath.isEmpty()) {
                    paths.add(record.organizedAlbum.relativePath);
                }
            }
            backing = ((MemoryMediaStoreReader) reader).withBatchPreload(paths);
        } else {
            backing = reader;
        }
        Map<String, Map<String, MemoryMediaStoreReader.Result>> byPath = new LinkedHashMap<>();
        return (path, record) -> {
            Map<String, MemoryMediaStoreReader.Result> byMemory = byPath.get(path);
            if (byMemory == null) {
                byMemory = new LinkedHashMap<>();
                byPath.put(path, byMemory);
            }
            if (!byMemory.containsKey(record.memoryKey)) byMemory.put(record.memoryKey, backing.read(path, record));
            return byMemory.get(record.memoryKey);
        };
    }
}
