package com.example.gallerysorter;

import android.content.Context;

import java.util.List;

final class DiscoverySnapshotController {
    private final DiscoverySnapshotStore store;
    private final Clock clock;

    DiscoverySnapshotController(Context context) {
        this(new DiscoverySnapshotStore(context), new SystemClock());
    }

    DiscoverySnapshotController(DiscoverySnapshotStore store, Clock clock) {
        this.store = store;
        this.clock = clock == null ? new SystemClock() : clock;
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
        return MemoryBrowserState.fromRecords(repository(organizedAlbums).discoveryMemories());
    }

    MemoryBrowserDetail loadBrowserDetail(String memoryKey,
                                          List<StoredAlbumSummary> organizedAlbums) {
        MemoryRepository repository = repository(organizedAlbums);
        return MemoryBrowserState.fromRecords(repository.discoveryMemories())
                .detail(memoryKey, repository);
    }

    MemoryRepository repository(List<StoredAlbumSummary> organizedAlbums) {
        return new MemoryRepository(store.read(), organizedAlbums);
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
