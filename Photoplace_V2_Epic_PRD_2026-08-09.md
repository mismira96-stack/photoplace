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

## Product North Star

PhotoPlace is not a gallery editor.

PhotoPlace helps users discover, understand, and personalize memories created from places.

When deciding whether to add a feature, use these two questions:

1. Does this make the user's memory easier to recall?
2. Does this strengthen a PhotoPlace-specific memory experience instead of rebuilding Gallery features?

If neither is true, lower the priority.

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
| No-location folder move option | 미구현 | No confirm/default-off UX or separate folder move flow yet. |
| Back/navigation reliability | 부분 구현 | Several fixes landed, but still a regression-test area for sorting/detail/history/Fold flows. |

### P0 - Trust Next

1. Validate 1.2.7 on large real libraries.
   - Friend device with 10,000+ photos.
   - Check sort history speed, overseas history, and abnormal folder/history counts.
2. No-location UX decision flow.
   - Default OFF.
   - Ask only during/after preview when count is meaningful.
   - Explain that files move but photos are not deleted.
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
| Sort history / recent place search | 완료 | `StoredAlbumSummarySearch` filters loaded summaries in memory. UI uses search icon toggle and opens existing detail screen. |
| Search by place/country/address/date | 완료 | Implemented in `StoredAlbumSummarySearch`; unit tests cover fields and date separators. |
| Search by user display name | 미구현 | `displayName` storage field exists in `MemoryPersonalizationStore`, but edit UI and search overlay are not implemented. |
| Search by memo | 미구현 | Existing memo is not included in `StoredAlbumSummarySearch`. |
| Country/city exploration | 부분 구현 | Overseas country cards exist, but no full country/city browsing model. |
| POI | 부분 구현 | Conservative POI/name policy exists, but broad automatic POI expansion is intentionally avoided. |
| International address normalization | 부분 구현 | Stabilization mappings exist, but structured `AddressNormalizer` is not implemented. |
| Travel Session | 미구현 | No trip/session grouping yet. |

### P0 - Discovery Current

1. Monitor sort history search in real use.
   - Large record count.
   - Fold layout.
   - Detail navigation.
2. International address normalization design.
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
| Personal Place recommendation | 미구현 | PRD exists, but no `PersonalPlace`, `PlaceCandidate`, or `PersonalPlaceStore`. |

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

### P2 - Personalization Expansion

1. Home `기억해 둔 장소`.
   - Show memories with memo and/or custom name.
   - Tap card to open detail.
2. Personal Place MVP.
   - Recommend repeated GPS clusters.
   - User confirms/edits place name.
   - Saving a place does not move files.

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
   - Default OFF.
   - Explicit confirmation.
   - Strong warning that file location changes.

## Updated Priority

## P0 - Current Sprint

| Item | Status | Notes |
| --- | --- | --- |
| WorkManager/background sorting | 부분 구현 | Core path exists; recovery/progress reliability remains. |
| Preview / trust UX | 부분 구현 | Existing preview/result flow, needs polish. |
| Progress reliability | 부분 구현 | App UI mostly works; OS notification phase issue remains. |
| No-location repeated analysis prevention | 부분 구현 | Cache exists; UX and safety need work. |
| Sort history search | 완료 | Released as 1.2.7 draft. |
| Large-library validation | 미구현 | Needs friend device follow-up. |

## P1 - Next

| Item | Status | First MVP Step |
| --- | --- | --- |
| Memory name change | 미구현 | Add `displayName` override and search integration. |
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
| Personal Place MVP | 미구현 | Requires design-first, user-confirmed layer. |
| POI expansion | 부분 구현 | Keep conservative; prefer Personal Place for reliability. |
| Revisit / old memory rediscovery | 미구현 | Future dashboard feature. |
| Moving / in-flight badges | 미구현 | Classifier exists; UI/storage integration missing. |

## Recommended Next Implementation Order

Do not start with cover change first. It requires photo grid selection and override propagation.

Safer next sequence:

1. Memory personalization storage design.
   - Add a small store/model first.
   - Preserve existing `albumMemory()` notes.
2. Display name MVP.
   - Low file risk.
   - Directly improves search and detail UX.
3. Search displayName integration.
   - Extend `StoredAlbumSummarySearch` or wrap summaries with personalization.
4. Memo model cleanup.
   - Keep current one-line memo compatible.
   - Decide home exposure later.
5. Cover override.
   - Needs photo picker/grid from existing album/detail.
6. Home `기억해 둔 장소`.
7. Personal Place and Travel Session after the personalization base is stable.

## Non-Goals For Next Patch

- Do not rename physical Gallery folders when changing memory name.
- Do not edit/crop/filter photos for cover selection.
- Do not broaden automatic POI classification.
- Do not add Travel Session while displayName/memo/cover storage is unsettled.
- Do not put the full new feature directly into `MainActivity`; keep it as thin UI wiring only.
