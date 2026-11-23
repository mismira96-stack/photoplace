package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Browser sources and collection counts from one repository read, ready for UI binding. */
final class MemoryBrowserEntry {
    final List<MemoryRecord> records;
    final MemoryBrowserState state;
    final MemoryBrowserSummary summary;
    final AutomaticCountryGroups countries;
    private final MemoryBrowserMediaProjection media;
    final List<MemoryCollection> collections;
    final Map<String, MemoryCollectionCardSummary> collectionSummaries;
    final int unresolvedCount;
    private final boolean mediaComplete;
    private final MemoryRepository repository;
    private final boolean includesAlbums;
    private final String revision;

    private MemoryBrowserEntry(MemoryBrowserEntry source, MemoryBrowserState state, String revision,
                               int unresolvedCount) {
        this.records = source.records;
        this.state = state;
        this.summary = source.summary;
        this.countries = source.countries;
        this.media = source.media;
        this.collections = source.collections;
        this.collectionSummaries = source.collectionSummaries;
        this.unresolvedCount = unresolvedCount;
        this.mediaComplete = source.mediaComplete;
        this.repository = source.repository;
        this.includesAlbums = source.includesAlbums;
        this.revision = revision;
    }

    MemoryBrowserEntry withSeenVersions(java.util.function.ToLongFunction<MemoryRecord> seen) {
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (MemoryRecord record : records) counts.put(record.memoryKey, MemorySeenPolicy.recentCount(record, seen.applyAsLong(record)));
        return new MemoryBrowserEntry(this, state.withRecentCounts(counts), revision, unresolvedCount);
    }

    MemoryBrowserEntry withUnresolvedCount(int unresolvedCount) {
        return new MemoryBrowserEntry(this, state, revision, Math.max(0, unresolvedCount));
    }

    MemoryBrowserEntry withRevision(String revision) {
        return new MemoryBrowserEntry(this, state, revision, unresolvedCount);
    }

    MemoryBrowserDetail detailForCard(String key, MemoryMediaResolver.GalleryReader reader) {
        MemoryBrowserItem item = state.item(key);
        if (item == null) return null;
        for (MemoryRecord card : records) {
            if (item.memoryKey.equals(card.memoryKey)) {
                return MemoryBrowserState.detail(item, repository.detailRecordForBrowserCard(card), repository, reader);
            }
        }
        return null;
    }

    MemoryBrowserDetail detailForRevision(String key, String currentRevision,
                                          MemoryMediaResolver.GalleryReader reader) {
        if (!includesAlbums || revision == null || revision.isEmpty() || !revision.equals(currentRevision)) {
            return null;
        }
        return detailForCard(key, reader);
    }

    private MemoryBrowserEntry(MemoryRepository repository, boolean includesAlbums,
                               List<MemoryRecord> records, List<MemoryCollection> collections,
                               Map<String, MemoryCollectionCardSummary> summaries, MemoryBrowserMediaProjection media) {
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
        this.state = MemoryBrowserState.fromRecords(records).withCounts(media.counts);
        this.summary = media.summary(records);
        this.countries = AutomaticCountryGroups.build(records, media);
        this.media = media;
        this.mediaComplete = media.complete;
        this.collections = Collections.unmodifiableList(new ArrayList<>(collections));
        this.collectionSummaries = summaries;
        this.unresolvedCount = 0;
        this.repository = repository;
        this.includesAlbums = includesAlbums;
        this.revision = "";
    }

    static MemoryBrowserEntry build(MemoryRepository repository, boolean includeAlbums,
                                    List<MemoryCollection> collections, Map<String, String> aliases,
                                    MemoryMediaResolver.GalleryReader reader) {
        List<MemoryRecord> records = includeAlbums ? repository.memories() : repository.discoveryMemories();
        List<MemoryCollection> groups = collections == null ? Collections.emptyList() : collections;
        MemoryMediaResolver.GalleryReader sharedReader = MemoryBrowserMediaProjection.memoize(reader, records, repository);
        MemoryBrowserMediaProjection media = MemoryBrowserMediaProjection.build(records, repository, sharedReader);
        return new MemoryBrowserEntry(repository, includeAlbums, records, groups,
                MemoryCollectionCardSummary.buildAll(groups, records, aliases, repository, sharedReader), media);
    }

    boolean matches(MemoryBrowserEntry other) {
        if (other == null) return false;
        if (!countries.signature.equals(other.countries.signature)) return false;
        if (summary.photoCount != other.summary.photoCount || summary.placeCount != other.summary.placeCount
                || !summary.yearRange.equals(other.summary.yearRange)) return false;
        if (this.unresolvedCount != other.unresolvedCount) return false;
        if (this.records.size() != other.records.size()) return false;
        if (this.complete() != other.complete()) return false;
        if (this.state == null || other.state == null) return this.state == other.state;
        if (this.state.items.size() != other.state.items.size()) return false;
        for (int i = 0; i < this.state.items.size(); i++) {
            MemoryBrowserItem a = this.state.items.get(i);
            MemoryBrowserItem b = other.state.items.get(i);
            if (!a.memoryKey.equals(b.memoryKey)) return false;
            if (a.recentAddedCount != b.recentAddedCount) return false;
            if (!a.countText.equals(b.countText)) return false;
            if (!a.coverUri.equals(b.coverUri)) return false;
            if (!a.title.equals(b.title)) return false;
            if (!a.dateText.equals(b.dateText)) return false;
            if (!a.cardDateText.equals(b.cardDateText)) return false;
            if (!a.subtitle.equals(b.subtitle)) return false;
        }
        if (this.collections.size() != other.collections.size()) return false;
        for (int i = 0; i < this.collections.size(); i++) {
            MemoryCollection a = this.collections.get(i);
            MemoryCollection b = other.collections.get(i);
            if (!a.collectionId.equals(b.collectionId)) return false;
            if (!a.title.equals(b.title)) return false;
            if (a.updatedAtMillis != b.updatedAtMillis) return false;
            if (a.members.size() != b.members.size()) return false;
            for (int m = 0; m < a.members.size(); m++) {
                if (!a.members.get(m).stableMemoryId.equals(b.members.get(m).stableMemoryId)) return false;
                if (!a.members.get(m).lastKnownAlias.equals(b.members.get(m).lastKnownAlias)) return false;
            }
        }
        if (this.collectionSummaries.size() != other.collectionSummaries.size()) return false;
        for (Map.Entry<String, MemoryCollectionCardSummary> e : this.collectionSummaries.entrySet()) {
            MemoryCollectionCardSummary aSum = e.getValue();
            MemoryCollectionCardSummary bSum = other.collectionSummaries.get(e.getKey());
            if (bSum == null) return false;
            if (aSum.photoCount != bSum.photoCount) return false;
            if (aSum.videoCount != bSum.videoCount) return false;
            if (!aSum.coverUri.equals(bSum.coverUri)) return false;
            if (!aSum.placeNames.equals(bSum.placeNames)) return false;
            if (!aSum.dateRange.equals(bSum.dateRange)) return false;
            if (aSum.complete != bSum.complete) return false;
        }
        return true;
    }

    MemoryBrowserEntry countryForRevision(String code, String currentRevision) {
        if (!includesAlbums || revision.isEmpty() || !revision.equals(currentRevision)) return null;
        return new MemoryBrowserEntry(repository, true, AutomaticCountryGroups.recordsForCountry(records, code),
                Collections.emptyList(), Collections.emptyMap(), media).withRevision(currentRevision);
    }

    static MemoryBrowserEntry buildCountry(MemoryRepository repository, String code,
                                           MemoryMediaResolver.GalleryReader reader) {
        List<MemoryRecord> records = AutomaticCountryGroups.recordsForCountry(repository.memories(), code);
        MemoryMediaResolver.GalleryReader shared = MemoryBrowserMediaProjection.memoize(reader, records, repository);
        return new MemoryBrowserEntry(repository, true, records, Collections.emptyList(), Collections.emptyMap(),
                MemoryBrowserMediaProjection.build(records, repository, shared));
    }

    boolean complete() {
        if (!mediaComplete) return false;
        for (MemoryCollectionCardSummary summary : collectionSummaries.values()) {
            if (!summary.complete) return false;
        }
        return true;
    }
}
