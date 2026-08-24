package com.example.gallerysorter;

import android.content.Context;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        return savePreviewItemsWithResult(items, sourceSignature).saved;
    }

    DiscoverySnapshotUpdate savePreviewItemsWithResult(List<PhotoItem> items, String sourceSignature) {
        long now = clock.nowMillis();
        DiscoverySnapshot snapshot = DiscoverySnapshotMapper.fromPhotoItems(
                items,
                now,
                now,
                sourceSignature == null ? "" : sourceSignature);
        DiscoverySnapshot existing = store.read();
        DiscoverySnapshot merged = DiscoverySnapshotMerger.replaceAnalyzedItems(
                existing,
                snapshot,
                photoItemUris(items));
        return updateFor(store.save(merged), existing, snapshot);
    }

    boolean saveSourceItems(List<DiscoverySnapshotMapper.SourceItem> items,
                            int sourceItemCount,
                            String sourceSignature) {
        return saveSourceItemsWithResult(items, sourceItemCount, sourceSignature).saved;
    }

    DiscoverySnapshotUpdate saveSourceItemsWithResult(List<DiscoverySnapshotMapper.SourceItem> items,
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
        DiscoverySnapshot existing = store.read();
        DiscoverySnapshot merged = DiscoverySnapshotMerger.replaceAnalyzedItems(
                existing,
                snapshot,
                sourceItemUris(items));
        return updateFor(store.save(merged), existing, snapshot);
    }

    private DiscoverySnapshotUpdate updateFor(boolean saved,
                                              DiscoverySnapshot existing,
                                              DiscoverySnapshot incoming) {
        int itemCount = 0;
        int placeCount = 0;
        int newPlaceCount = 0;
        Set<String> existingUris = photoUris(existing);
        if (incoming != null) {
            placeCount = incoming.groupCount();
            for (DiscoveryMemoryGroup group : incoming.groups) {
                if (group == null) {
                    continue;
                }
                itemCount += group.itemCount;
                if (hasNewPhotoRef(group, existingUris)) {
                    newPlaceCount++;
                }
            }
        }
        return new DiscoverySnapshotUpdate(saved, itemCount, placeCount, newPlaceCount);
    }

    private boolean hasNewPhotoRef(DiscoveryMemoryGroup group, Set<String> existingUris) {
        if (group == null) {
            return false;
        }
        for (DiscoveryPhotoRef ref : group.photoRefs) {
            if (ref != null && !ref.sourceUri.isEmpty() && !existingUris.contains(ref.sourceUri)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> photoUris(DiscoverySnapshot snapshot) {
        HashSet<String> uris = new HashSet<>();
        if (snapshot != null) {
            for (DiscoveryMemoryGroup group : snapshot.groups) {
                if (group == null) {
                    continue;
                }
                for (DiscoveryPhotoRef ref : group.photoRefs) {
                    if (ref != null && !ref.sourceUri.isEmpty()) {
                        uris.add(ref.sourceUri);
                    }
                }
            }
        }
        return uris;
    }

    private Set<String> photoItemUris(List<PhotoItem> items) {
        LinkedHashSet<String> uris = new LinkedHashSet<>();
        if (items != null) {
            for (PhotoItem item : items) {
                if (item != null && item.uri != null) {
                    uris.add(item.uri.toString());
                }
            }
        }
        return uris;
    }

    private Set<String> sourceItemUris(List<DiscoverySnapshotMapper.SourceItem> items) {
        LinkedHashSet<String> uris = new LinkedHashSet<>();
        if (items != null) {
            for (DiscoverySnapshotMapper.SourceItem item : items) {
                if (item != null && !item.sourceUri.isEmpty()) {
                    uris.add(item.sourceUri);
                }
            }
        }
        return uris;
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
            snapshot = liveFilter.filter(snapshot, organizedAlbums);
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
