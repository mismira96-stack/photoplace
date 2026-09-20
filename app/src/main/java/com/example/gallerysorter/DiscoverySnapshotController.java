package com.example.gallerysorter;

import android.content.Context;

import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DiscoverySnapshotController {
    private final DiscoverySnapshotStore store;
    private final Clock clock;
    private final DiscoverySnapshotLiveFilter liveFilter;
    private final MemoryIdentityRegistryStore identityRegistryStore;
    private final MemoryOrganizationLinkStore organizationLinkStore;
    private MemoryMediaResolver.GalleryReader galleryReader;

    DiscoverySnapshotController(Context context) {
        this(new DiscoverySnapshotStore(context), new SystemClock(),
                new DiscoverySnapshotLiveFilter(context.getContentResolver()),
                new MemoryIdentityRegistryStore(context), new MemoryOrganizationLinkStore(context));
        this.galleryReader = new MemoryMediaStoreReader(context.getContentResolver());
    }

    DiscoverySnapshotController(DiscoverySnapshotStore store, Clock clock) {
        this(store, clock, null, null, null);
    }

    DiscoverySnapshotController(DiscoverySnapshotStore store,
                                Clock clock,
                                DiscoverySnapshotLiveFilter liveFilter) {
        this(store, clock, liveFilter, null, null);
    }

    DiscoverySnapshotController(DiscoverySnapshotStore store,
                                Clock clock,
                                DiscoverySnapshotLiveFilter liveFilter,
                                MemoryIdentityRegistryStore identityRegistryStore,
                                MemoryOrganizationLinkStore organizationLinkStore) {
        this.store = store;
        this.clock = clock == null ? new SystemClock() : clock;
        this.liveFilter = liveFilter;
        this.identityRegistryStore = identityRegistryStore;
        this.organizationLinkStore = organizationLinkStore;
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
                photoUris(snapshot));
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
                photoUris(snapshot));
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
        return loadBrowserDetail(memoryKey, organizedAlbums, false);
    }

    MemoryBrowserDetail loadBrowserDetail(String memoryKey,
                                          List<StoredAlbumSummary> organizedAlbums,
                                          boolean includeOrganizedSources) {
        MemoryRepository repository = repository(organizedAlbums);
        List<MemoryRecord> records = includeOrganizedSources
                ? repository.memories()
                : repository.discoveryMemories();
        return MemoryBrowserState.fromRecords(records)
                .detail(memoryKey, repository, galleryReader);
    }

    MemoryRepository repository(List<StoredAlbumSummary> organizedAlbums) {
        DiscoverySnapshot snapshot = store.read();
        if (liveFilter != null) {
            snapshot = liveFilter.filter(snapshot, organizedAlbums);
        }
        Map<String, String> aliases = identityRegistryStore == null
                ? Collections.<String, String>emptyMap()
                : identityRegistryStore.readAliasesSnapshot();
        List<OrganizationLink> links = organizationLinkStore == null
                ? Collections.<OrganizationLink>emptyList()
                : organizationLinkStore.readAll();
        return new MemoryRepository(snapshot, organizedAlbums, aliases, links);
    }

    MemoryMediaResolver.GalleryReader galleryReader() {
        return galleryReader;
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
