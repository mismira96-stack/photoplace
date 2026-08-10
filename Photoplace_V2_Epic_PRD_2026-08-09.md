# PhotoPlace V2 Epic PRD - 2026-08-09

## Purpose

This document reorganizes current V2 requirements by Epic and P0/P1/P2 priority, based on the current codebase after release `1.2.7 (28)`.

It removes overlap between previous PRD notes and marks each requirement as:

- `완료`: implemented and device-tested enough to treat as current product behavior.
- `부분 구현`: some code or UX exists, but the PRD goal is not fully satisfied.
- `미구현`: not yet implemented in code.

## Current Code Baseline

- Branch: `codex/photoplace-v2-bg-wip`
- Latest release prep: `fa9f5f6 Prepare 1.2.7 search release`
- Latest Play draft: `1.2.7 (28)`
- Package: `com.photoplace.app`

Relevant current structures:

- `StoredAlbumSummary`
  - Current memory/history summary model.
  - Fields include `albumName`, `relativePath`, `itemCount`, `startDate`, `endDate`, `thumbnailUri`, `countryName`, `adminArea`, `addressLine`.
- `StoredAlbumSummarySearch`
  - Pure in-memory search helper for sort history/recent places.
- `MemoryItem`, `MemoryGroup`, `OverseasMemoryGrouper`
  - Current overseas memory grouping layer.
- `SortWorker`, `SortInputStore`, `SortResultStore`, `SortProgressStore`
  - Current WorkManager/background sort pipeline.
- `NoLocationCache`
  - Signature-based no-location cache exists, but final no-location UX is not complete.
- `MemoryPersonalization`, `MemoryPersonalizationKey`, `MemoryPersonalizationStore`
  - First personalization storage layer for memo/displayName/userCover overlay. UI currently still only uses memo.
- `MainActivity`
  - Still owns too much UI/state/rendering logic. New features should be split out where practical.
- `DiscoverySnapshot`, `DiscoveryMemoryGroup`, `DiscoveryPhotoRef`, `MemoryRecord`
  - First Display First model skeleton exists after 2026-08-10.
  - Not yet wired to Preview/Home/Search/Detail.

## Product North Star

PhotoPlace is not a gallery editor.

PhotoPlace helps users discover, understand, and personalize memories created from places.

When deciding whether to add a feature, use these two questions:

1. Does this make the user's memory easier to recall?
2. Does this strengthen a PhotoPlace-specific memory experience instead of rebuilding Gallery features?

If neither is true, lower the priority.

## V2 UX Principle - Display First, Organize Optional

PhotoPlace V2 should not become a discovery-only app, and it should not weaken the existing Gallery album creation value.

Instead, V2 adopts this product principle:

```text
Display First, Organize Optional

먼저 장소별 사진을 보여준다.
사용자가 기억을 확인한다.
필요한 경우 실제 Gallery에 정리한다.
```

This is not a mode split.

Do not introduce:

```text
○ 발견 모드
○ 정리 모드
```

Use one natural flow:

```text
Analyze
  -> Display discovered places
  -> Explore photos inside PhotoPlace
  -> Organize into Gallery albums if wanted
```

### Why This Matters

User feedback shows two user intents:

- `Organizer`: wants actual Gallery albums created by place.
- `Viewer / Explorer`: wants to see place-based memories inside PhotoPlace but does not want Gallery structure changed.

Both users first need to trust what PhotoPlace found.

### User Observation - Search Before Organize

During a real user test, a user immediately searched for `일본` after seeing PhotoPlace instead of first asking how to create albums.

This is an important product signal:

```text
User mental model:
  "Where did I go?"
  before
  "How do I organize these files?"
```

It supports the idea that PhotoPlace can be perceived as a place-based memory search tool, not only a Gallery album creation tool.

Implications:

- Search should remain highly visible and fast.
- Country/city/place search quality matters.
- Search guidance must match actual behavior. If UI says country search works, `일본` should work reliably for current and rebuilt records.
- Date search should support user language, not only stored formats. Users may try `8월`, `2026년 8월`, or `8월 2일`, not only `2026-08` / `2026.08`.
- Place search should support common localized/romanized aliases where practical. For example, both `삿포로` and `sapporo` should find the same memory.
- `displayName` and user-personalized naming become more important.
- Display First should expose discovered places before asking users to commit Gallery changes.
- Gallery album creation should remain available as a clear next action after the user finds a memory worth organizing.

Therefore V2 should shift from:

```text
Forced Organization
```

to:

```text
Intentional Organization
```

Gallery album creation remains a core capability. Optional means user-confirmed, not unimportant.

### Architecture Boundary

Do not put discovery-only results directly into `AlbumSummaryHistoryStore`.

Current `AlbumSummaryHistoryStore` and many detail/gallery flows assume:

- a real Gallery album exists.
- `relativePath` identifies that album.
- MediaStore can load thumbnails/photos from that album folder.

For Display First experiments, use a separate model/store later, such as:

```text
DiscoverySnapshotStore
or
MemorySnapshotStore
```

Potential fields:

```text
placeKey
placeName
country
adminArea
photoCount
dateRange
coverUri
sourcePhotoReferences
```

The eventual long-term direction can be a common `MemoryRecord` abstraction:

```text
sourceType:
  ORGANIZED_ALBUM
  DISCOVERED_ONLY
```

But do not introduce that abstraction before the Display First experiment proves useful.

### Minimum Experiment

The lowest-risk V2 experiment is:

```text
Preview complete
  -> 발견한 장소 둘러보기
  -> Gallery 앨범으로 정리
```

Rules:

- Keep the existing Organizer flow.
- Do not remove the Gallery album creation CTA.
- Discovery-only detail should use original photo URIs, not `relativePath`.
- If no Gallery album exists, hide album-open actions and offer photo viewing instead.
- Measure whether users open discovered places, view photos, return later, and then optionally create albums.

## Product Boundary

### Allowed

User edits the memory unit:

- memory name
- representative scene / cover
- memo
- personal meaning of a place or trip
- search/discovery of memories

### Avoid

User edits photos or uses PhotoPlace as a file manager:

- photo correction
- crop/filter
- photo ordering
- generic file management
- broad gallery replacement behavior

## Epic 1 - Trust

Users must feel safe letting PhotoPlace analyze and organize large photo libraries.

### Status Summary

| Requirement | Status | Code Basis / Notes |
| --- | --- | --- |
| WorkManager background sorting | 부분 구현 | `SortWorker`, `SortInputStore`, `SortResultStore`, `SortProgressStore` exist. Real sort work can run in Worker, but process-death recovery and notification phase clarity still need hardening. |
| Progress UI | 부분 구현 | In-app progress and foreground notification exist. User previously observed notification progress jump/stall in some travel-folder cases. |
| Preview before sorting | 부분 구현 | Preview/result flow exists. PRD-level representative preview and trust copy still need UX polish. |
| Completion notification / badge cleanup | 완료 | `SortNotificationHelper.clearCompleteNotification()` is called on app open/resume. |
| Original photo cleanup | 부분 구현 | Pending original cleanup card exists and moved higher in result flow. Full Step UI and rollback/delete-copy management are not complete. |
| No-location repeated analysis prevention | 부분 구현 | `NoLocationCache` exists, but final user-facing no-location decision flow is not complete. |
| No-location snapshot/cache skip | 미구현 | Display First snapshot should remember unchanged no-location items so they do not get re-analyzed every time. |
| No-location folder move option | 미구현 | Later optional cleanup only. Do not use file movement as the primary repeated-analysis fix. |
| Back/navigation reliability | 부분 구현 | Several fixes landed, but still a regression-test area for sorting/detail/history/Fold flows. |

### P0 - Trust Next

1. Validate 1.2.7 on large real libraries.
   - Friend device with 10,000+ photos.
   - Check sort history speed, overseas history, and abnormal folder/history counts.
2. No-location repeated-analysis prevention.
   - Prefer snapshot/cache identity skip over moving files.
   - Store unchanged no-location item signatures and skip them on later analysis.
   - Explain skipped counts clearly and provide `다시 확인하기`.
   - Keep folder move as a later explicit cleanup option, default OFF.
3. Notification/progress phase cleanup.
   - Separate scan/preview/rebuild/copy progress concepts.
   - Decide which phases deserve OS notifications.

## Epic 2 - Discovery

Users should quickly find memories that PhotoPlace already discovered.

### Status Summary

| Requirement | Status | Code Basis / Notes |
| --- | --- | --- |
| Home recent places | 완료 | Home shows recent discovered places from stored summaries. |
| Home overseas records | 부분 구현 | Overseas groups display on Home and open filtered memory view. International normalization is still fragile. |
| Display First model skeleton | 부분 구현 | `DiscoverySnapshot`, `DiscoveryMemoryGroup`, `DiscoveryPhotoRef`, and `MemoryRecord` exist, but no Store/UI wiring yet. |
| Display First experiment | 미구현 | Preview can already analyze without creating albums, but there is no separate `발견한 장소 둘러보기` path or URI-based discovery detail yet. |
| Sort history / recent place search | 완료 | `StoredAlbumSummarySearch` filters loaded summaries in memory. UI uses search icon toggle and opens existing detail screen. |
| Search by place/country/address/date | 완료 | Implemented in `StoredAlbumSummarySearch`; unit tests cover fields and date separators. |
| Search by user display name | 미구현 | `displayName` storage field exists in `MemoryPersonalizationStore`, but edit UI and search overlay are not implemented. |
| Search by memo | 미구현 | Existing memo is not included in `StoredAlbumSummarySearch`. |
| Country/city exploration | 부분 구현 | Overseas country cards exist, but no full country/city browsing model. |
| POI | 부분 구현 | Conservative POI/name policy exists, but broad automatic POI expansion is intentionally avoided. |
| International address normalization | 부분 구현 | Stabilization mappings exist, but structured `AddressNormalizer` is not implemented. |
| Travel Session | 미구현 | No trip/session grouping yet. |
| Repeated-place recommendations | 미구현 | Personal Place PRD exists. This is now a core V2 memory feature for splitting broad admin groups into user-confirmed places. |
| Candidate Quality Validation | 미구현 | Before recommendation UI, generate top candidates from real photos and manually judge whether clusters are meaningful. |

### P0 - Discovery Current

1. Monitor sort history search in real use.
   - Large record count.
   - Fold layout.
   - Detail navigation.
   - Real user observed searching `일본` immediately; treat country/place search as a core behavior, not a secondary utility.
   - Verify country search actually works for current, rebuilt, and older records. Some records may lack `countryName` metadata even though the UI suggests country search.
   - Add Korean date query support later, for example `8월`, `2026년 8월`, `8월 2일`.
   - Add localized/romanized alias search for known/common places, for example `삿포로` <-> `sapporo`.
   - Do not keep fixing overseas history by adding city-by-city country hints such as `KarlovyVary -> 체코` or `Fatih -> 튀르키예`.
   - Proper fix: store and normalize country identity from structured metadata where possible, preferably `countryCode` / normalized country name, then derive display text (`튀르키예`, not whichever localized spelling happened to be returned).
2. Display First experiment design.
   - Add `발견한 장소 둘러보기` after Preview.
   - Keep `Gallery 앨범으로 정리` visible.
   - Do not store discovery-only records in `AlbumSummaryHistoryStore`.
   - Discovery detail must use original photo URIs, not Gallery album `relativePath`.
3. Repeated-place / Personal Place candidate design.
   - Detect dense GPS clusters inside broad groups such as `수원`, `성남`, `송파구`.
   - Recommend user-confirmed personal memories such as `집`, `회사`, `발레학원`.
   - Do not auto-promote unknown POIs or rename/move files without confirmation.
   - Saving a Personal Place changes PhotoPlace's internal display/grouping first; Gallery album creation remains optional.
   - Add Candidate Quality Validation as an implementation gate before UI:
     - output top 20 candidates
     - inspect photos/date spread/coordinate spread
     - label meaningful / ambiguous / false positive
     - tune 100m / 20 photos / 3 days / 2 weeks before treating them as product specs
4. International address normalization design.
   - Do not keep growing one-off aliases.
   - Separate stable key from display name.
   - Overseas ward/subLocality must not become final album name alone.

### P1 - Discovery Next

1. Search expansion after personalization.
   - Include user display name first.
   - Later consider memo search.
2. Sort history filters.
   - Recent order.
   - Country.
   - 가나다.
   - Date.

## Epic 3 - Personalization

Users should be able to complete automatically discovered memories in their own language.

### Product Principle

Memory Personalization is not photo editing.

It edits the memory unit:

- what this memory is called
- which scene represents it
- what the user wants to remember

### Current Memory Model

Current code effectively has:

```text
StoredAlbumSummary
  albumName
  relativePath
  itemCount
  startDate / endDate
  thumbnailUri
  countryName / adminArea / addressLine

SharedPreferences memory text
  key = PREF_ALBUM_MEMORY_PREFIX + encoded(relativePath or albumName)
```

Missing model fields:

```text
displayName
userCoverUri
memo as first-class data
originalPlaceName / stableKey separation
```

### Status Summary

| Requirement | Status | Code Basis / Notes |
| --- | --- | --- |
| Memo / memory one-line edit | 부분 구현 | `showAlbumMemoryEditor()`, `albumMemory()`, `saveAlbumMemory()` exist. Reads/writes now delegate to `MemoryPersonalizationStore`. Shown on cards/detail if present. |
| Memo as first-class model | 부분 구현 | `MemoryPersonalizationStore` stores `memo` in `memory_personalization.json` with lazy legacy migration. Memo UX is still the old one-line editor. |
| Home remembered places | 미구현 | No `기억해 둔 장소` / memo-only home section yet. |
| Memory display name change | 미구현 | `displayName` field exists in storage only. Detail title and cards still use `storedAlbumSummary.albumName`. |
| Search by custom display name | 미구현 | `displayName` is not included in `StoredAlbumSummarySearch` yet. |
| Representative cover change | 미구현 | `userCoverUri` field exists in storage only. No picker/edit UI. |
| User cover applied consistently | 미구현 | Store can return cover override, but UI still uses auto `thumbnailUri`. |
| Personal Place recommendation | 미구현 | PRD exists, but no `PersonalPlace`, `PlaceCandidate`, or `PersonalPlaceStore`. Now treated as V2 core memory personalization, not distant POI expansion. |

### P1 - Personalization Next

1. Add memory personalization storage.
   - First layer is implemented in `MemoryPersonalizationStore`.
   - Keep user values separate from `StoredAlbumSummary`.
   - Key by stable identity: first `relativePath`, later stable memory key.
2. Name change MVP.
   - Add `displayName`.
   - Display priority: `displayName -> albumName`.
   - Do not rename physical Gallery folders.
   - Add displayName to search.
3. Memo UX improvement.
   - Migrate or wrap existing `albumMemory()` behavior.
   - Keep current saved notes compatible.
   - Decide whether memo joins search now or later.
4. Cover change MVP.
   - Add `userCoverUri`.
   - Display priority: `userCoverUri -> thumbnailUri`.
   - Pick from existing memory photos.
   - Do not crop/edit photos.
5. Repeated-place / Personal Place MVP.
   - Start from DiscoverySnapshot photo refs, not Gallery album folders.
   - Generate candidate clusters inside broad admin groups.
   - User reviews photos before naming the place.
   - Apply confirmed names inside PhotoPlace before offering Gallery organization.

### P2 - Personalization Expansion

1. Home `기억해 둔 장소`.
   - Show memories with memo and/or custom name.
   - Tap card to open detail.
2. Personal Place management.
   - Edit/delete/hide confirmed places and dismissed recommendations.
   - Optional album creation from a saved personal place.

## Epic 4 - Rediscovery

PhotoPlace should bring back memories users forgot they had.

### Status Summary

| Requirement | Status | Code Basis / Notes |
| --- | --- | --- |
| Memory Dashboard direction | 부분 구현 | Home has overseas records and recent places, but not a full dashboard. |
| Old memories / revisit prompts | 미구현 | No time-based rediscovery surfaces yet. |
| Travel Session | 미구현 | No trip-level grouping. |
| Remembered places section | 미구현 | Depends on personalization model. |
| Meaning-based exploration | 미구현 | Out of scope for current local model. |
| Moving / in-flight badges | 미구현 | `MovementClassifier` tests exist, but UI integration is not implemented. |

### P2 - Rediscovery Next

1. Home `기억해 둔 장소`.
2. Travel Session.
   - Example: `2026 삿포로 여행`.
   - Groups Sapporo/Otaru/Biei/Chitose.
3. Moving/in-flight badges.
   - Show, do not auto-exclude.
4. Memory Dashboard.
   - Country, place, revisit, season, memo-based resurfacing.

## Epic 5 - Cleanup

Cleanup should be safe, explicit, and separate from memory personalization.

### Status Summary

| Requirement | Status | Code Basis / Notes |
| --- | --- | --- |
| Photo copy / video move | 완료 | Existing core behavior. |
| Original photo trash flow | 부분 구현 | Confirmation exists; pending cleanup state exists. Needs clearer Step UI and management surface. |
| Generated album delete / rollback | 미구현 | No full rollback/delete generated copies management. |
| No-location folder move | 미구현 | Not implemented. |
| Physical Gallery album rename | 미구현 | Should remain separate from displayName. Requires explicit future decision. |

### P1/P2 - Cleanup Next

1. Original cleanup Step UI.
   - Step 1: album creation complete.
   - Step 2: original photo cleanup.
2. Generated album cleanup / rollback.
   - Requires action log such as `PhotoAction`.
3. No-location folder move.
   - Later optional cleanup only.
   - Default OFF.
   - Explicit confirmation.
   - Strong warning that file location changes.
   - Primary repeated-analysis prevention should come from snapshot/cache skip.

## Updated Priority

## P0 - Current Sprint

| Item | Status | Notes |
| --- | --- | --- |
| WorkManager/background sorting | 부분 구현 | Core path exists; recovery/progress reliability remains. |
| Preview / trust UX | 부분 구현 | Existing preview/result flow, needs polish. |
| Progress reliability | 부분 구현 | App UI mostly works; OS notification phase issue remains. |
| No-location repeated analysis prevention | 부분 구현 | Cache exists; next direction is snapshot/cache skip, not folder move first. |
| Sort history search | 완료 | Released as 1.2.7 draft. |
| Large-library validation | 미구현 | Needs friend device follow-up. |
| Display First model skeleton | 부분 구현 | Model/JSON foundation exists; no Store/UI wiring yet. |
| Display First experiment | 미구현 | Add Preview -> discovered places browsing without forcing Gallery album creation. |

## P1 - Next

| Item | Status | First MVP Step |
| --- | --- | --- |
| Memory name change | 미구현 | Add `displayName` override and search integration. |
| Display snapshot persistence | 미구현 | Add `DiscoverySnapshotStore`; this is now the next foundation step. |
| Repeated-place recommendations | 미구현 | Detect dense GPS clusters and ask the user to name/confirm them. |
| Candidate Quality Validation | 미구현 | Must run before recommendation UI or Gallery Personal Place actions. |
| Representative cover change | 미구현 | Add `userCoverUri` override. |
| Memo UX improvement | 부분 구현 | Formalize existing SharedPreferences memo into personalization model. |
| CTA/Icon/Detail consistency | 부분 구현 | Continue hidden-screen audit. |
| Search personalization integration | 미구현 | Search `displayName`; later memo. |
| International address normalization | 부분 구현 | Design structured layer before more patches. |

## P2 - Later

| Item | Status | Notes |
| --- | --- | --- |
| Home remembered places | 미구현 | Depends on memo/displayName model. |
| Travel Session | 미구현 | Trip-level grouping. |
| Memory Dashboard | 부분 구현 | Home has early memory sections only. |
| Personal Place management | 미구현 | Edit/delete/hide saved places and optional album creation. |
| POI expansion | 부분 구현 | Keep conservative; prefer Personal Place for reliability. |
| Revisit / old memory rediscovery | 미구현 | Future dashboard feature. |
| Moving / in-flight badges | 미구현 | Classifier exists; UI/storage integration missing. |

## Recommended Next Implementation Order

Do not start with cover change first. It requires photo grid selection and override propagation.

Safer next sequence:

1. `DiscoverySnapshotStore`.
   - Save analysis results separately from Gallery album history.
   - Use atomic JSON write/read and corruption protection.
2. `DiscoverySnapshotMapper`.
   - Convert `PhotoItem` into discovery memory groups and source photo refs.
   - Add no-location analyzed-result skip metadata.
3. Repeated-place candidate logic.
   - Pure clustering first, with tests.
   - Detect smaller repeated memories inside broad admin groups.
4. Candidate Quality Validation.
   - Output top 20 candidates from real photos.
   - Manually judge candidate quality before UI.
   - Tune threshold/radius as experiment parameters.
5. Preview/Memory entry.
   - `발견한 장소 둘러보기`.
   - Original URI-based detail.
6. Personal Place confirmation.
   - Review photos, name place, save internal display/grouping.
7. Country/date/place search expansion.
   - `일본`, `Japan`, `JP`.
   - `8월`, `2026년 8월`.
   - `삿포로` / `sapporo`.
8. Memory personalization display name/search integration.
   - Still important, but should ride on stable `memoryKey`.
9. Cover override.
   - Needs photo picker/grid from memory detail.
10. Home `기억해 둔 장소`.
11. Travel Session after memory/grouping base is stable.

## Deferred / Possibly Unneeded

### No-location folder move

The earlier idea was to move no-location photos into a separate folder so they would not be analyzed every time.

After the Display First / snapshot direction, this is no longer the preferred solution.

Preferred approach:

```text
Analyze once
  -> remember unchanged no-location item identity
  -> skip in later scans
  -> let user explicitly re-check if needed
```

Why:

- It avoids changing the user's Gallery/file structure.
- It fits the trust principle better.
- It solves the repeated-analysis cost without creating another album/folder.
- Users who only want to view memories should not be pushed into file movement.

Keep folder move as a future optional cleanup idea only if snapshot/cache skip is insufficient in real large-library tests.

## Non-Goals For Next Patch

- Do not rename physical Gallery folders when changing memory name.
- Do not edit/crop/filter photos for cover selection.
- Do not broaden automatic POI classification.
- Do not add Travel Session while displayName/memo/cover storage is unsettled.
- Do not put the full new feature directly into `MainActivity`; keep it as thin UI wiring only.
