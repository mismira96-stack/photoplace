package com.example.gallerysorter;

import android.content.Context;

import java.util.List;

final class DiscoverySnapshotController {
    private final DiscoverySnapshotStore store;
    private final Clock clock;
    private final DiscoverySnapshotLiveFilter liveFilter;

    DiscoverySnapshotController(Context context) {
        this(new DiscoverySnapshotStore(context), new SystemClock(), new DiscoverySnapshotLiveFilter(context.getContentResolver()));
    }

    DiscoverySnapshotController(DiscoverySnapshotStore store, Clock clock) {
        this(store, clock, null);
    }

    DiscoverySnapshotController(DiscoverySnapshotStore store,
                                Clock clock,
                                DiscoverySnapshotLiveFilter liveFilter) {
        this.store = store;
        this.clock = clock == null ? new SystemClock() : clock;
        this.liveFilter = liveFilter;
    }

    boolean savePreviewItems(List<PhotoItem> items, String sourceSignature) {
        long now = clock.nowMillis();
        DiscoverySnapshot snapshot = DiscoverySnapshotMapper.fromPhotoItems(
                items,
                now,
                now,
                sourceSignature == null ? "" : sourceSignature);
        return store.save(snapshot);
    }

    boolean saveSourceItems(List<DiscoverySnapshotMapper.SourceItem> items,
                            int sourceItemCount,
                            String sourceSignature) {
        long now = clock.nowMillis();
        DiscoverySnapshot snapshot = DiscoverySnapshotMapper.fromSourceItems(
                items,
                sourceItemCount,
                now,
                now,
                sourceSignature == null ? "" : sourceSignature,
                DiscoverySnapshotMapper.DEFAULT_ANALYSIS_POLICY_VERSION,
                DiscoverySnapshotMapper.DEFAULT_COUNTRY_IDENTITY_POLICY_VERSION);
        return store.save(snapshot);
    }

    MemoryBrowserState loadBrowserState(List<StoredAlbumSummary> organizedAlbums) {
        return loadBrowserState(organizedAlbums, "");
    }

    MemoryBrowserState loadBrowserState(List<StoredAlbumSummary> organizedAlbums, String query) {
        return MemoryBrowserState.fromRecords(MemoryBrowserSearch.filter(
                repository(organizedAlbums).discoveryMemories(),
                query));
    }

    MemoryBrowserDetail loadBrowserDetail(String memoryKey,
                                          List<StoredAlbumSummary> organizedAlbums) {
        MemoryRepository repository = repository(organizedAlbums);
        return MemoryBrowserState.fromRecords(repository.discoveryMemories())
                .detail(memoryKey, repository);
    }

    MemoryRepository repository(List<StoredAlbumSummary> organizedAlbums) {
        DiscoverySnapshot snapshot = store.read();
        if (liveFilter != null) {
            snapshot = liveFilter.filter(snapshot);
        }
        return new MemoryRepository(snapshot, organizedAlbums);
    }

    interface Clock {
        long nowMillis();
    }

    private static final class SystemClock implements Clock {
        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }
    }
}
