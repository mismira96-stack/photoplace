# PhotoPlace V2.1 - Media Lifecycle and Memory Sync

## Decision

PhotoPlace has one logical **Memory** layer and two different projections:

- `발견 기록`: newly analyzed or not-yet-organized memory filter (inbox).
- `위치 앨범`: actual Gallery albums and their organization history (output/archive).

When the product wording moves beyond an inbox-only experience, the root tab should be named `기억`, not `발견 기록`. `발견 기록` remains a useful latest-analysis section inside that tab.

Creating, renaming, merging, moving, or deleting a Gallery album must never delete a Memory or a user memo. Gallery is an optional physical output; Memory is the logical view.

## Current boundary

Current code deliberately excludes `PhotoItem.duplicateInTarget` in `DiscoverySnapshotMapper`; this protects the existing discovery-only view, but it means the snapshot is not yet a complete Memory source. `StoredAlbumSummary` separately represents Gallery-backed location albums.

Do not solve the new lifecycle by re-including duplicates in the existing discovery UI. First introduce a durable analyzed-media/index model, then derive each UI from it.

## Target layers

1. **MediaStore**: current device-media source of truth.
2. **MediaAnalysisStore**: per-media analysis cache/index. One record holds stable identity/signature, analysis status, and normalized location result.
3. **Memory index**: groups live analyzed media by stable place/date identity. It is not a Gallery folder.
4. **Memory personalization**: display name, cover override, and memo. It survives a missing/deleted photo reference.
5. **Gallery organization**: actual album summaries and user-created album collections. It references Memory; it does not own it.

## Lifecycle

- New media: analyze once, cache normalized result, then add it to Memory.
- Unchanged media: reuse cached normalized result; still rebuild visible counts/cards.
- No-location media: cache `NO_LOCATION`; keep it in analysis counts but not in location Memory groups.
- Deleted media: remove only its live photo reference during reconciliation.
- A date/place node with no remaining photos: remove it only when it has no user memo/personalization. Otherwise show the memo with an unavailable-photo state.
- Gallery album creation: mark/link the related Memory as organized. Do not remove its underlying Memory record.
- Gallery rename/merge/move: update Gallery organization references only. Memory place/date/memo identity remains unchanged.

## Album merge policy

There are two separate user-facing merge features. Do not use one as a shortcut for the other.

### Discovery History: virtual MemoryCollection

`발견 기록` can group multiple Memory records under a user-provided name without changing a Gallery folder or a media item. For example, `삿포로에서`, `오타루에서`, and `비에이에서` can become `2026 여름 일본 여행` in the app.

Persist stable collection id, user display name, member Memory stable keys, and timestamps. This preserves the user's memory name even when Gallery organization changes. The MVP interaction is `long press -> multi-select -> 기억으로 묶기 -> 이름 입력 -> 저장`, and saved collections appear in a `내 기억 모음` section in Discovery History.

### Location Albums: physical Gallery move

`위치 앨범` is a list of real Gallery folders. Its merge feature is always a real move operation, never a virtual collection.

The user selects PhotoPlace-created location albums, enters a target name, and the generated-album media moves to `Pictures/{user name}/`. Memory stays independent: its location/date/memo data is not renamed, moved, or deleted by the Gallery operation.

### Interaction MVP

Use `long press -> multi-select -> 통합 -> 이름 입력 -> 이동 확인`. It is more predictable than drag-and-drop on phone and Fold layouts. The confirmation must state photo/video counts, that source folders may become empty, and that this does not delete the separate originals.

### Implementation boundary

Do not use `MediaCopyEngine` directly: its normal sort contract copies photos but moves videos. Album merge needs a dedicated engine that moves both generated photos and generated videos by updating each MediaStore entry's `relative_path` to the target folder. This is a real Gallery move without creating another byte-for-byte copy, but it still runs per media item. Persist an action plan before moving and record per-item outcomes for recovery/rollback.

## Incremental analysis and reconciliation

`LocationAnalysisCache` is replaced by `MediaAnalysisStore` / incremental analysis:

- cache key: MediaStore id + URI plus a content signature (name, modified/added/taken time, size, duration, media type, policy versions);
- cache result: `ANALYZED`, `NO_LOCATION`, or retryable `FAILED`, plus normalized location metadata;
- new/copied/moved/changed/policy-changed media is a miss;
- if MediaStore currently provides GPS, do not reuse an old `NO_LOCATION` result;
- reconciliation checks whether indexed media still exists before doing any expensive EXIF/Geocoder work;
- app/data deletion cannot magically restore app-private Memory. Provide `기억 다시 구성하기`, which re-analyzes selected folders without deleting photos or Gallery albums.

## Delivery order

1. Define media identity/signature and a file-backed `MediaAnalysisStore`; add tests before enabling reuse.
2. Reconcile indexed refs against MediaStore and make Discovery/Memory views use the resulting live index.
3. Preserve Memory after Gallery organization and expose `발견 기록` / `정리 전` / `전체 기억` as filters.
4. Implement `MemoryCollection` storage and virtual merge UI in Discovery History with user-name preservation.
5. Separately design and implement the Location Albums physical merge/move action, including recovery and rollback.
