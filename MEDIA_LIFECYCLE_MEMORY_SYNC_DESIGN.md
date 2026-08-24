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

The first safe merge feature is a user-named **virtual AlbumCollection**:

- fields: stable collection id, user display name, member album relative paths, created/updated time;
- it is shown in `위치 앨범` while member albums and Memory details remain intact;
- user-created name is preserved even when member albums are refreshed.

Physical folder merge (moving/copying media into one Gallery folder) is a later explicit operation. It needs a confirmation screen, a result/action record, partial-failure reporting, and an undo/cleanup policy. Do not combine it with the initial collection UI.

### Interaction MVP

Use `long press -> multi-select -> 통합 -> 이름 입력` for the first UI. It is more predictable than drag-and-drop on phone and Fold layouts, and it keeps the first release focused on user meaning rather than file movement. Show saved collections in a small `내 통합 앨범` section above the ordinary location-album list.

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
4. Implement `AlbumCollection` storage and virtual merge UI with user-name preservation.
5. Only then evaluate physical merge/move, undo, and background reconciliation.
