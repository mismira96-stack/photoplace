# PhotoPlace P1 Design: Memory Collections + Selective Gallery Organization

**Date:** 2026-09-12
**Status:** Design proposal; no application code changed
**Scope:** User-created virtual Memory collections and creating a Gallery location album for one selected Memory

## 1. Product Goal

PhotoPlace should let people:

1. Group selected discovered places into a named Memory collection, without changing Gallery files.
2. Optionally create a Gallery location album for one selected place, without losing that place's Memory identity, dates, or notes.

These are related but distinct actions:

```text
MemoryCollection = an app-only view over existing memories
Gallery album    = an optional physical output for one memory
```

The collection is not a place merge and does not rename its members. The single-place action does not organize every place or convert the collection into a Gallery folder.

## 2. Repository Findings

### Existing collection foundation

- `MemoryCollection` already models `group_<UUID>`, a title, and stable `mem_<UUID>` member references with a last-known alias.
- `MemoryCollectionStore` already supports create, rename, and dissolve with atomic JSON/backup safety. It enforces at least two distinct members and one active collection per stable Memory.
- `MemoryCollectionResolver` already preserves `date -> place -> original date note -> photos`, deduplicates source URIs, and does not mutate Gallery or discovery data.
- Collection creation is not wired to `MemoryIdentityRegistryStore`; there is no selection UI, repository facade, card, or viewer UI yet.
- The current resolver only resolves `MemoryRecord.discoveryGroup` photo refs. It cannot render a place whose live discovery refs have disappeared after organization.

### Existing single-place Gallery pipeline

```text
Discovery detail/global CTA
 -> DiscoveryAlbumOrganizer.prepare(records, MediaStoreAlbumLookup)
 -> OrganizePlaceService.planFor(record)
 -> DiscoveryAlbumItemAdapter
 -> SortInputStore -> WorkManager/SortWorker -> SortResultStore
 -> MainActivity result handling -> AlbumSummaryHistoryStore
```

`OrganizePlaceService.planFor(record)` and `DiscoveryAlbumOrganizer.prepare(...)` are already place-oriented. Passing one selected `MemoryRecord` is sufficient to plan one destination and reuse the existing copy/skip/failure machinery.

### Current lifecycle gap

- `MemoryIdentityRegistryStore` maps aliases to stable IDs; date notes use the stable ID plus date.
- Gallery success is recorded in album history, but no durable link records which stable Memory produced that Gallery album.
- `MemoryRepository` currently merges discovery and album records using compatible place-name/admin/date heuristics. This is useful legacy fallback, but it is not a durable identity contract.
- Discovery live filtering can hide refs once they are no longer present in their original location. The current group resolver then has no Gallery-media source to fall back to.
- Therefore, adding the CTA alone is not enough to promise “same Memory, notes, and collection membership remain available.”

## 3. Recommended Identity and Lifecycle Contract

### Memory identity

`mem_<UUID>` is the durable identity for a place Memory. Visible place names, `placeKey`, album names, and paths are aliases or metadata, never the identity.

At first use by a user-authored feature, resolve the current `discovery:<placeKey>` alias to a stable Memory ID. When a successful Gallery output is recorded, register its exact `path:<relativePath>` alias to that same ID. Alias registration must refuse to reassign an existing alias to a different ID.

If discovery reclassification changes the place alias for media known to be the same Memory, alias reassignment must be an explicit reconciliation rule; do not infer identity from display-name equality alone.

### Organization output link

Add a separate durable organization-link store. Do not put a second mutable `ORGANIZED` flag on `MemoryRecord` or `MemoryCollection`.

Suggested record:

```text
OrganizationLink {
  linkId
  subjectType: MEMORY | COLLECTION
  subjectId: mem_<UUID> | group_<UUID>
  requestId
  albumName
  relativePath             // last-known Gallery output metadata, not identity
  organizedAtMillis
  status: SUCCESS | PARTIAL | MISSING
  copiedCount
  skippedCount
  failedCount
}
```

For the first delivery only `subjectType = MEMORY` is executed. Keeping the subject contract generic avoids a second schema redesign when collection-to-one-album is considered later. `isOrganized` is derived from a usable active success/partial output link, not duplicated elsewhere. A `MISSING` link remains as history but does not assert that the Gallery album is currently available.

One Memory may have a last-known output and later a replacement output. Preserve prior link history or mark it superseded; never overwrite the stable Memory identity with the new path.

### Successful output commit boundary

The organization link and `path:` alias are written only after worker results are available. A prepared plan, enqueue, cancellation, or zero successful copies is not an organization success.

The request identity must survive app/process restart. Carry a `requestId` and subject metadata through the persisted sort input/result contract, or use a durable pending-organization record referenced by both. Completion handling must be idempotent so re-reading a worker result cannot append duplicate album history or duplicate links.

For partial completion:

- Keep the Memory and all date notes active.
- Record the actual Gallery output as `PARTIAL` if at least one item was successfully copied/moved.
- Keep failed and not-yet-processed refs available for retry; do not report full completion.
- A duplicate already present in the target is satisfied/skipped, not a copy failure.
- If the task is cancelled, persist only outputs that the worker confirms succeeded.

An external Gallery deletion or rename must not delete the Memory or collection. A later reconciliation can mark an output `MISSING` or refresh its last-known path after reliable identification; path/name guesswork must not relink a different album.

## 4. User Flows

### A. Virtual collection

```text
발견 기록
 -> 장소 카드 길게 누르기 또는 선택 모드 진입
 -> 여러 장소 선택
 -> 기억으로 묶기
 -> 이름 입력
 -> memory_collections.json 저장
 -> 내 기억 모음 카드 및 읽기 전용 Group Viewer
```

MVP policy:

- Select at least two distinct place Memories.
- One stable Memory may belong to at most one active collection.
- Collection create/rename/dissolve changes only collection metadata.
- Dissolve releases membership; it never deletes a place, photo, note, or Gallery album.
- Keep original places searchable and independently openable.
- Group detail remains `date -> place -> that place/date note -> photos`; never flatten notes by date.
- Missing members remain in the collection and are represented as unavailable rather than silently removed.

### B. One-place Gallery organization

```text
발견 장소 상세
 -> 이 장소를 위치 앨범으로 만들기
 -> refresh live refs and prepare only this Memory
 -> confirmation: destination, photos/videos, duplicates, skipped items, source-retention policy
 -> existing SortWorker pipeline
 -> success result -> AlbumSummaryHistory + path alias + OrganizationLink
 -> same stable Memory and notes remain reachable
```

The place detail is the primary entry point. Keep the existing “all discovered places” action as an explicitly secondary bulk action until the one-place flow has been exercised. This supports users who want folders while avoiding surprise folders for every foreign/unknown place.

The confirmation must state whether photos are copied and whether videos are moved under current settings; do not claim “originals remain” universally if the worker's video path moves them.

## 5. How the Two Features Interact

The common link makes a collection member resolvable after its Gallery output is created:

```text
MemoryCollection.Member(stableMemoryId)
 -> identity registry / organization link
 -> current discovery projection and/or exact linked Gallery album
 -> GroupMemoryDetail date -> place -> note -> photos
```

The collection stores member identity only. It must not copy photo lists, notes, or album paths into the collection JSON. The resolver obtains current media and note data at read time.

If a member is organized after it was grouped:

- The membership and stable ID remain unchanged.
- Date notes remain keyed to that stable ID and date.
- The Group viewer resolves media from the linked Gallery album when discovery refs are no longer live.
- If neither source is currently available, retain the member and notes, show an unavailable state, and offer no destructive cleanup.

If a Memory is organized before collection creation, it can be selected only when the repository can resolve its stable Memory identity and its source can be displayed. The initial UI may restrict selection to discovered Memories; if so, explain this as an MVP boundary and do not make the store schema discovery-specific.

## 6. Delivery Plan

### Phase 0 — Close identity/lifecycle contracts

1. Specify the OrganizationLink schema, active/missing semantics, request idempotency, and partial-result behavior.
2. Add focused tests for stable `discovery:` and `path:` alias ownership and exact linked Memory/album matching.
3. Extend the repository projection to prefer explicit stable-ID links; keep current heuristic matching only as legacy fallback.
4. Make a shared Memory media resolver capable of obtaining the place's live discovery refs or the exact linked Gallery album. Keep date-note ownership at `stableMemoryId + yyyyMMdd`.

### Phase 1 — Single-place organization

1. Add the detail CTA and confirmation for exactly one selected Memory.
2. Refresh the live discovery refs immediately before plan creation; do not trust stale detail state.
3. Persist request context through worker completion/restart and process completion idempotently.
4. Write history/link/alias only from confirmed worker output; preserve Memory and notes on complete, partial, cancelled, and failed outcomes.
5. Verify re-entry from Discovery, Location Albums, Overseas detail, and an existing MemoryCollection.

### Phase 2 — Collection UI on the established identity layer

1. Wire selected discovery aliases to `MemoryIdentityRegistryStore.resolveOrCreate()` when saving a collection.
2. Add selection mode, title entry, and one-active-collection validation.
3. Add the `내 기억 모음` list/card and read-only group detail using `MemoryCollectionResolver` or its shared-media successor.
4. Add rename/dissolve controls and unavailable-member rendering.
5. Keep Gallery output out of collection creation; add Collection-to-album only as a separately reviewed future phase.

This order deliberately establishes the common lifecycle contract first, but keeps each user-facing feature independently testable. If implementation time requires a smaller vertical slice, ship Phase 1 first and leave Collection UI hidden until its resolver can read linked albums.

## 7. Main Risks and Guards

| Risk | Guard |
| --- | --- |
| Gallery completion disconnects the source Memory | Stable subject ID in persisted request; link only from confirmed worker result |
| Similar place names merge the wrong Memories | Exact stable-ID/path link takes precedence; heuristics are fallback only |
| Partial copy is reported as complete | Persist per-request counts/status; preserve retry path and Memory |
| Worker result is processed twice after restart | Idempotent `requestId` completion and link/history writes |
| Album deletion removes notes or collection membership | Never cascade-delete canonical Memory/notes/collection on Gallery disappearance |
| Collection viewer loses photos after organization | Resolve exact linked Gallery album as a media source |
| Same-day notes from different places get combined | Keep date -> place sections and original stable ID/date note keys |
| Global bulk action surprises users | Make one-place CTA primary; retain bulk as explicit secondary action |
| Video semantics are misrepresented | Confirmation reflects actual copy/move policy and permissions |

## 8. Verification Plan

### Unit tests

- Stable ID remains the same after registering a `path:` alias; alias collision cannot steal an ID.
- Repository links an organized album to its exact Memory through the persisted link even if album display name changes.
- Similar names without an explicit link do not become a new identity match beyond existing conservative fallback rules.
- Collection creation resolves stable IDs and rejects fewer than two members, duplicates, and already-grouped members.
- A member can be resolved from discovery refs before organization and the exact linked Gallery source after organization.
- Date notes remain distinct for two members with photos on the same date.
- Partial/cancelled/zero-success outcomes persist only confirmed outputs; completion replay with the same request ID is idempotent.
- Missing Gallery output preserves Memory, collection membership, and notes.
- Collection create/rename/dissolve never changes MediaStore rows or album history.

### Device scenarios

1. Create a collection from two discovered places; verify the originals remain visible/searchable and Gallery is unchanged.
2. Add a date note to each place on the same date; verify both notes remain separate in the group detail.
3. Create a Gallery album for one selected place; verify no other place is included and the Memory/notes remain reachable.
4. Organize a collection member, then reopen the collection; verify its Gallery media is shown and other members remain untouched.
5. Exercise duplicate-only target, one-item failure, cancellation/restart, and revoked permission paths.
6. Rename/delete the Gallery album externally; verify no Memory, note, or collection metadata is silently deleted.

## 9. Decision Summary

- Treat the two P1s as one lifecycle design but separate user actions.
- Add stable identity-to-Gallery output linkage before exposing either action as a complete Memory lifecycle.
- Prefer the one-place Gallery action; keep “organize all” as an opt-in bulk path.
- Finish Collection UI with a resolver that can use both live discovery media and an exact linked Gallery output.
- Do not add physical album merge, group-to-single-album generation, or automatic place grouping to these MVPs.
- No application code was changed while preparing this design.
