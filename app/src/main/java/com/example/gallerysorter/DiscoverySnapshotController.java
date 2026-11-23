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
    private Context manualContext;

    DiscoverySnapshotController(Context context) {
        this(new DiscoverySnapshotStore(context), new SystemClock(),
                new DiscoverySnapshotLiveFilter(context.getContentResolver()),
                new MemoryIdentityRegistryStore(context), new MemoryOrganizationLinkStore(context));
        this.galleryReader = new MemoryMediaStoreReader(context.getContentResolver());
        this.manualContext=context.getApplicationContext();
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
        boolean saved = store.save(merged);
        if (saved && manualContext != null) {
            PersonalPlaceStore.syncSnapshotOnRefresh(manualContext, merged, identityRegistryStore);
        }
        return updateFor(saved, existing, snapshot);
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
        boolean saved = store.save(merged);
        if (saved && manualContext != null) {
            PersonalPlaceStore.syncSnapshotOnRefresh(manualContext, merged, identityRegistryStore);
        }
        return updateFor(saved, existing, snapshot);
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
        MemoryRepository repo = repository(organizedAlbums);
        return MemoryBrowserState.fromRecords(MemoryBrowserSearch.filter(
                repo.discoveryMemories(),
                query,
                manualContext,
                repo));
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
        MemoryBrowserState state=MemoryBrowserState.fromRecords(records);
        String resolvedKey=includeOrganizedSources?repository.presentationKey(memoryKey):memoryKey;
        if(state.item(resolvedKey)==null) {
            String id=repository.stableId(memoryKey);
            if(!id.isEmpty())for(MemoryRecord record:records)if(id.equals(repository.stableId(record.memoryKey))){resolvedKey=record.memoryKey;break;}
        }
        return withCurrentNotes(state.detail(resolvedKey, repository, galleryReader));
    }

    MemoryBrowserDetail loadBrowserCardDetail(MemoryBrowserEntry entry, String memoryKey, String revision) {
        return withCurrentNotes(entry.detailForRevision(memoryKey, revision, galleryReader));
    }

    MemoryBrowserDetail loadPhysicalAlbumDetail(StoredAlbumSummary album, MemoryBrowserEntry entry) {
        return withCurrentNotes(PhysicalAlbumDetail.buildFromRecords(album,
                entry == null ? Collections.emptyList() : entry.records, galleryReader));
    }

    private MemoryBrowserDetail withCurrentNotes(MemoryBrowserDetail detail) {
        if(detail!=null&&manualContext!=null) {
            String id=new MemoryIdentityRegistryStore(manualContext).findStableId(detail.record.memoryKey);
            detail=detail.withNotes(MemoryNoteProjection.forId(new MemoryDateNoteStore(manualContext).readAll(),id));
        }
        return detail;
    }

    MemoryRepository repository(List<StoredAlbumSummary> organizedAlbums) {
        DiscoverySnapshot snapshot = store.read();
        DiscoverySnapshot rawSnapshot=snapshot;
        if(manualContext!=null)organizedAlbums=MemoryManagementAlbums.withLinks(organizedAlbums,new MemoryOrganizationLinkStore(manualContext).readAll());
        if (liveFilter != null) {
            snapshot = liveFilter.filter(snapshot, organizedAlbums);
        }
        Map<String, String> aliases = identityRegistryStore == null
                ? Collections.<String, String>emptyMap()
                : identityRegistryStore.readAliasesSnapshot();
        List<OrganizationLink> links = organizationLinkStore == null
                ? Collections.<OrganizationLink>emptyList()
                : organizationLinkStore.readAll();
        ManualGalleryPolicy galleryPolicy=null;
        if(manualContext!=null) {
            try {
                ManualPlaceSnapshot manual=new ManualPlaceStore(manualContext).readAssignments();
                organizedAlbums=MemoryManagementAlbums.withManual(organizedAlbums,manual);
                List<UnresolvedMediaRecord> expected=new java.util.ArrayList<>();
                for(ManualMediaAssignment a:manual.assignments())expected.add(a.media);
                Map<String,UnresolvedMediaRecord> live=new MemoryMediaAccess(manualContext).available(expected,false);
                snapshot=EffectiveMediaPlaceResolver.project(snapshot,manual,live,false);
                aliases=new java.util.LinkedHashMap<>(manual.mergedAliases(aliases));
                snapshot=MemoryNoteProjection.retain(rawSnapshot,snapshot,aliases,new MemoryDateNoteStore(manualContext).readAll());
                galleryPolicy=new ManualGalleryPolicy(manual,live,aliases);
                if(new ManualPlaceStore(manualContext).revision()!=manual.revision)
                    throw new IllegalStateException("위치기록이 바뀌었어요. 다시 불러와 주세요.");
            } catch(Exception error) { throw new IllegalStateException("수동 위치기록을 불러오지 못했어요.",error); }
        }
        return new MemoryRepository(snapshot, organizedAlbums, aliases, links,galleryPolicy,
                manualContext==null?null:new RecoveredAlbumGeoReader(manualContext));
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
