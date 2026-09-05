# PhotoPlace Memory Grouping Spike

**Date:** 2026-09-05  
**Status:** Research complete; implementation deferred  
**Scope:** Code investigation and architecture only. No production code, Gallery media, or user data was changed.

## Purpose

PhotoPlace currently discovers location-based places and lets people browse photos by place and date before optionally creating Gallery location albums.

The next product direction is to let a person group several discovered places into one app-only memory without moving or copying Gallery media.

```text
Sapporo
Otaru
New Chitose
    ↓
2026 Sapporo Trip
```

This is not a Gallery folder merge. Original discovered places, photos, and date notes remain independent. The group is a user-authored layer that can later become the input for creating one optional Gallery album.

## Working Tree Check

- Branch: `codex/photoplace-v2-bg-wip`
- The immediately preceding code change was the unshipped date-note pencil icon polish (`557f69f`).
- Existing modified/untracked worklog, review, dump, and design files belong to prior user work and were not modified by this spike.
- This spike created no application code changes.

## Single-place Gallery Album Spike Handoff

### Current Full Organization Flow

```text
Discovery tab global CTA
  -> MainActivity.showDiscoveryOrganizeConfirmation()
  -> DiscoveryOrganizeConfirmDialog
  -> MainActivity.prepareDiscoveryAlbums(records)
  -> DiscoveryAlbumOrganizer.prepare(records, MediaStoreAlbumLookup)
  -> OrganizePlaceService.planFor(record)
  -> DiscoveryAlbumItemAdapter.toPhotoItems()
  -> MainActivity.runCopy()
  -> SortInputStore
  -> WorkManager / SortWorker
  -> SortJob
  -> MediaCopyEngine
  -> SortResultStore
  -> MainActivity.handleBackgroundSortResultIfAvailable()
  -> MainActivity.saveAlbumSummaryHistory()
  -> AlbumSummaryHistoryStore
```

There is no ViewModel layer. `MainActivity` owns the UI orchestration, while planning, MediaStore lookup, worker input adaptation, worker execution, and history persistence have already been extracted to focused helpers.

### Reusable Single-place Path

The Gallery creation core is already place-oriented:

- `OrganizePlaceService.planFor(MemoryRecord)` builds a side-effect-free plan for one place.
- `DiscoveryAlbumOrganizer.prepare(List<MemoryRecord>, AlbumLookup)` can accept `Collections.singletonList(record)`.
- `MediaStoreAlbumLookup` resolves an existing target path and checks target duplicate names.
- `DiscoveryAlbumItemAdapter`, `SortWorker`, `SortJob`, and `MediaCopyEngine` can remain unchanged for one-place creation.

### Current Lifecycle Gap

Creating a Gallery album does not currently persist an explicit `DISCOVERED -> ORGANIZED` link for the source discovery Memory.

- `AlbumSummaryHistoryStore` records successful Gallery output.
- `DiscoverySnapshot` itself is not turned into an organized-memory record.
- Date notes are keyed through the `discovery:<placeKey>` alias and a stable `mem_...` identity.
- The location-album viewer does not yet register a `path:` alias or open the shared date-based Memory viewer.

Therefore, single-place Gallery creation is a **small Gallery execution extension**, but a **medium lifecycle feature** if it promises that the same Memory and date notes remain visible after organization.

### Single-place Spike Conclusion

| Topic | Conclusion |
| --- | --- |
| Implementation difficulty | Medium when lifecycle is included |
| Existing logic reuse | High; no new copy/move engine is needed |
| Core risk | Gallery completion can become disconnected from the source Memory/date notes |
| Expected later scope | Organization link, shared Memory detail, and lifecycle filtering |
| Relation to Grouping | A future group can reuse the same worker pipeline with one group target path |

Single-place Gallery creation is intentionally deferred until the Memory Grouping and lifecycle boundary are defined.

## Current Memory Data Flow

```text
MediaStore images/videos
  -> MainActivity.loadSourceImages() / loadSourceVideos()
  -> LocationResult and PhotoItem
  -> DiscoverySnapshotMapper
  -> DiscoveryMemoryGroup grouped by locationKey
  -> MemoryRepository
  -> MemoryRecord
  -> MemoryPhotoSection.fromDiscoveryRefs()
  -> Date-grouped discovery detail
  -> MemoryIdentityRegistryStore
  -> MemoryDateNoteStore
```

### Source and Analysis

- `MainActivity.loadSourceImages()` queries MediaStore and uses `ImageAnalysisCacheSession` plus `MediaAnalysisStore` to reuse location analysis for unchanged images.
- A cache hit skips EXIF/Geocoder work only. It still creates a normal `PhotoItem`, so preview, discovery snapshot, and no-location counts continue to include the media.
- `loadSourceVideos()` still uses the existing non-cached video scan path.

### Place Identity and Media Membership

- `DiscoverySnapshotMapper` groups each analyzed `PhotoItem` by `locationKey`.
- `DiscoveryMemoryGroup.memoryKey` is generated as `discovery:<placeKey>`.
- Each `DiscoveryPhotoRef` belongs to one discovery group in a snapshot.
- `DiscoverySnapshotMerger` deduplicates observations by `sourceUri` while preserving first-seen version information.
- A Memory Group aggregation must deduplicate by `sourceUri` again as a safety boundary.

### Date Grouping

- `MemoryPhotoSection.fromDiscoveryRefs()` sorts refs by taken time and groups them by `yyyyMMdd`.
- `MemoryPhotoPage` pages those sections in 48-item slices.
- A plain reuse for a multi-place group is not sufficient: `MemoryPhotoSection` has only one `placeText`, derived from the first ref in the date. A group needs to preserve which member Place produced each date's photos.

### Date Notes

- `MemoryIdentityRegistryStore` maps aliases such as `discovery:<placeKey>` to immutable `mem_<UUID>` IDs.
- `MemoryDateNoteStore` stores one-line notes under `memory-id:<stable-id>#yyyyMMdd`.
- Notes do not use the visible place name or Gallery relative path, so ordinary title changes do not lose notes.
- A selected group member should receive a stable ID at group-creation time, even if the user has not yet written a date note for that Place.

## Recommended Memory Group Model

The project uses app-private JSON files rather than Room. The recommended model follows the existing JSON/tmp/bak storage pattern.

```java
MemoryCollection {
  String collectionId;       // group_<UUID>
  String title;              // e.g. "2026 Sapporo Trip"
  List<MemberRef> members;
  long createdAtMillis;
  long updatedAtMillis;
}

MemberRef {
  String stableMemoryId;     // mem_<UUID>
  String lastKnownAlias;     // discovery:<placeKey>
}
```

Suggested persistence:

```text
memory_collections.json
  schemaVersion
  collections[]
    collectionId
    title
    members[]
    createdAtMillis
    updatedAtMillis
```

`MemoryCollectionStore` should use the same conservative `tmp -> bak -> replace` write and corrupt-file refusal policy used by `MemoryDateNoteStore` and `MemoryIdentityRegistryStore`.

### Membership Policy for the First MVP

- Original Places are never moved, renamed, or deleted by grouping.
- A Place belongs to at most one active collection in the MVP to avoid duplicate discovery cards and ambiguous aggregate views.
- The storage model may support multiple collections later, but the UI should enforce the simple one-group rule first.
- Deleting a collection dissolves it only; it never deletes member Places, media, or notes.

## Candidate Model Comparison

| Candidate | Strength | Limitation | Decision |
| --- | --- | --- | --- |
| Add `groupId` to Place | Very small initial implementation | Couples source Place to one group; awkward removal/history | Do not use |
| Move Place data into Group | Simple viewer | Loses original Place and risks memo loss | Do not use |
| Group entity plus relation | Preserves originals and supports removal/addition | Requires a resolver and aggregate viewer | Recommended |
| Room entities/tables | Strong relational tooling | Project does not currently use Room; migration cost is unnecessary | Do not introduce for MVP |

## Group Domain API

The UI may later use long press, selection, or drag, but the domain API should be selection-agnostic:

```text
createCollection(title, memberAliases)
renameCollection(collectionId, title)
dissolveCollection(collectionId)
addMembers(collectionId, memberAliases)       // later
removeMember(collectionId, stableMemoryId)    // later
```

`createCollection` resolves each selected `discovery:<placeKey>` alias through `MemoryIdentityRegistryStore.resolveOrCreate()` and persists stable member IDs. The caller should reject empty titles, duplicate member IDs, and an attempt to group fewer than two Places.

## Group Viewer Design Boundary

The Group viewer must not flatten same-date member notes into one date-wide note.

Recommended logical shape:

```text
2026 Sapporo Trip

August 5
  Sapporo
    "Arrived and ate jingisukan"
    [photos]
  Otaru
    "Walked by the canal"
    [photos]
```

This lets two Places on the same date retain their original note keys:

```text
mem_sapporo#20260805 -> note A
mem_otaru#20260805   -> note B
```

The Group has no new replacement note in the first MVP. It renders original Place/date notes as-is.

## Search Behavior

Grouping must not remove original Place searchability.

- Search collection title: `2026 삿포로 여행`
- Search a member title: `오타루`
- Search member country/admin/POI metadata as currently supported

The group search index should combine its title with member Place search tokens. Whether results show the group, the original Place, or both is a product policy; the MVP should prefer the group result once a member is grouped, with a clear member-place label in the result.

## Future Gallery Album Connection

```text
MemoryCollection
  -> member stable IDs
  -> resolve current member Place records
  -> gather live DiscoveryPhotoRefs
  -> dedupe by sourceUri
  -> one target relative path
  -> MediaStoreAlbumLookup
  -> SortWorker / SortJob / MediaCopyEngine
```

`DiscoveryAlbumOrganizer` currently calculates a target path per Place. A later Group-album feature should add a focused plan variant such as `prepareForTarget(records, targetName)` rather than duplicating copy/move logic. It can continue to reuse MediaStore target lookup, item adaptation, WorkManager execution, and result/history handling.

## Organized Lifecycle

For the grouping MVP, a collection should not persist one blanket `ORGANIZED` state.

- A member Place may be discovered or organized independently.
- The Group may display a computed state such as `all discovered`, `partially organized`, or `organized as one album` later.
- When a Group is eventually made into one Gallery album, create an explicit Group-to-album link. Do not mutate or delete member Place identities.
- The future organization link must be written only after Worker results confirm the intended media was handled.

## Deletion, Reanalysis, and App Reinstall

- MediaStore reconciliation should remove unavailable refs only from live views; it must not delete user-authored Group metadata or date notes automatically.
- If a grouped Place has no remaining media but has a note, retain it as unavailable rather than dropping the note.
- App data deletion/uninstall removes `DiscoverySnapshot`, date-note files, identity registry, and the future collection file. Photo scanning can rebuild discovered places, but cannot reconstruct user Group titles or memberships.
- Backup/export is not part of the grouping MVP. The limitation should be treated as a future user-data backup requirement, not hidden by an implied restore promise.

## MVP Scope Decision

### MVP

1. Create a collection from two or more ungrouped discovered Places.
2. Name and rename a collection.
3. Show an app-only Group detail with date -> Place -> photos and original date notes.
4. Dissolve a collection safely.
5. Preserve original Places, media refs, search tokens, and notes.

### Next Version

1. Add another Place to an existing Group.
2. Remove one Place from a Group.
3. Group-level Gallery album creation.
4. Group title/member search polish.
5. Group lifecycle states and Group-to-album links.

### Deferred

1. Group A plus Group B merge.
2. Drag-and-drop, auto-scroll, and animation.
3. Group creation Undo distinct from safe dissolve.
4. App-data backup/export.

## Minimum Implementation Units

1. Add `MemoryCollection`, `MemberRef`, and `MemoryCollectionStore`, including JSON recovery tests.
2. Resolve selected discovery aliases to immutable IDs through `MemoryIdentityRegistryStore`.
3. Add a collection repository/resolver that maps current Places to member IDs and deduplicates media URI values.
4. Add Group-specific date sections that preserve member Place boundaries and their note IDs.
5. Add a `내 기억 모음` projection and Group detail entry point in Discovery.
6. Add selection UI only after the model and aggregate viewer are verified.

## Reusable Existing Code

| File | Class / function | Current role | Grouping reuse |
| --- | --- | --- | --- |
| `MemoryRepository.java` | `MemoryRepository` | Discovery and organized records | Resolve member Places |
| `MemoryPhotoSection.java` | `fromDiscoveryRefs` | One Place date grouping | Base for Group date builders |
| `MemoryPhotoPage.java` | `from` | 48-item paging | Group viewer paging |
| `MemoryIdentityRegistryStore.java` | `resolveOrCreate`, `registerAlias` | Alias to stable ID | Stable Group membership |
| `MemoryDateNoteStore.java` | `get`, `save` | Place/date notes | Render original member notes |
| `DiscoveryAlbumOrganizer.java` | `prepare` | Existing album planning | Later Group target planning |
| `MediaStoreAlbumLookup.java` | target lookup / duplicate check | Existing Gallery target safety | Later Group album safety |
| `SortWorker.java` | worker pipeline | Copy/move execution | Later Group album execution |

## Risks

1. **Date note loss in a flattened viewer:** never replace Place/date note keys with a group/date key for the MVP.
2. **Place reclassification:** current `discovery:<placeKey>` aliases can change when analysis relocates a media item. A future resolver must register the new alias against the existing stable ID when the same source URI changes group.
3. **Duplicate media:** aggregate refs must be deduplicated by source URI.
4. **Original Place hiding:** hiding originals without a clear collection projection can make search and group dissolution confusing.
5. **User metadata after uninstall:** Group titles/membership are app-private metadata and cannot be recreated from photos alone.
6. **Organized lifecycle collision:** do not use grouping as a shortcut for Gallery organization state.
7. **Existing discovery regression:** keep `DiscoverySnapshotMapper.duplicateInTarget` policy unchanged during Grouping MVP.

## Expected Files When Implementation Begins

```text
New: MemoryCollection.java
New: MemoryCollectionStore.java
New: MemoryCollectionRepository.java
New: MemoryCollectionStoreTest.java
New: MemoryCollectionRepositoryTest.java
Modify: MemoryIdentityRegistryStore.java
Modify: MemoryRepository.java
Modify: MemoryPhotoSection.java or add GroupMemoryPhotoSection.java
Modify: MemoryBrowserState.java
Modify: MainActivity.java
```

## Final Answers

### Q1. Is a new Entity plus relation appropriate?

Yes. In this project, a file-backed `MemoryCollection` plus member relation is appropriate. It preserves original Places and avoids an unnecessary Room migration.

### Q2. Can existing date notes remain unchanged?

Yes. Groups should render notes owned by the original stable Place ID and date key. No note migration is needed.

### Q3. Can original Place data remain after grouping?

Yes, and it is a hard requirement. Grouping adds metadata only; it does not mutate snapshot groups, media refs, or original titles.

### Q4. Does this connect naturally to one Group Gallery album later?

Yes. Resolve members, aggregate/deduplicate refs, apply one target path, and reuse the existing worker pipeline.

### Q5. Is this a small extension or a structural change?

Medium. It is not a file-system feature, but it introduces user-authored collection data and a multi-place viewer that must preserve note ownership.

### Q6. What is the safest first implementation step?

Implement and test the collection store plus stable-member identity contract first. Do not start with drag UI or Gallery generation.

## Recommended Delivery Order

1. Review the stable membership, note ownership, and collection persistence design.
2. Implement the isolated collection store and unit tests.
3. Implement member resolution and deduplicated aggregation tests.
4. Implement the Group detail data model and viewer.
5. Add simple selection-based creation UI.
6. Validate on real data before adding Group-level Gallery organization.
