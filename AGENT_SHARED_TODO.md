# PhotoPlace Agent Shared TODO

Updated: 2026-08-15

This file is the shared handoff note for Codex and Gemini CLI. Keep it short, current, and safe to act on.

## Workspace And Shared Docs

Project root:

- `C:\Users\mismi\Documents\Codex\GallerySorter`

Read these first:

- `AGENT_SHARED_TODO.md`
  - Current shared status, stable baseline, open TODOs, and guardrails.
- `WORKLOG_2026-07-30.md`
  - Latest release/work record for 1.2.5, overseas-history metadata fix, Fold UI checks, and Play draft automation.
- `WORKLOG_2026-08-06.md`
  - Current WIP stabilization record after Japan-trip overseas-history fixes caused home/history performance regressions.
- `WORKLOG_2026-08-09.md`
  - Post-1.2.6 polish record: completion badge clearing, result-screen action cleanup, and next V2 priorities.
- `WORKLOG_2026-08-10.md`
  - 1.2.8 overseas country identity release record and Display First / Organize Optional architecture checkpoint.
- `WORKLOG_2026-08-15.md`
  - MemoryRepository checkpoint for Memory browsing first. Includes Gemini review prompt in Korean.
- `WORKLOG_2026-07-26.md`
  - Date-based work record for the WorkManager/background-sort stabilization day.
- `Photoplace_V2_Personal_Place_PRD.md`
  - Personal Place PRD draft. Encoding may look broken in some terminals, but the final Gemini handoff section was already understood by Gemini.
- `Photoplace_V2_Epic_PRD_2026-08-09.md`
  - V2 PRD reorganized by Epic/P0/P1/P2 with code-based implementation status after 1.2.7. Now includes the `Display First, Organize Optional` V2 UX principle.
- `DISCOVERY_FIRST_HYPOTHESIS_REVIEW_2026-08-09.md`
  - Discovery-first product hypothesis and architecture review. Treat as supporting rationale; the final PRD principle is `Display First, Organize Optional`.
- `MEMORY_PERSONALIZATION_P1_DESIGN.md`
  - P1 Memory Personalization design for display name, memo, and user cover storage. Use this before coding personalization UI.
- `DISPLAY_FIRST_ORGANIZE_OPTIONAL_DESIGN_2026-08-10.md`
  - Final V2 direction for showing location-based memories inside PhotoPlace before optional Gallery album creation.
- `MEMORY_BROWSING_FIRST_DESIGN.md`
  - Gemini design for the Memory browsing first implementation path. Includes sequence diagram and example JSON.
- `PERSONAL_PLACE_PRD_REVIEW_2026-08-10.md`
  - Gemini review of the Personal Place PRD. Use it as the data-contract checklist before implementation.
- `COMMIT_9E51A2C_REVIEW.md`
  - Gemini review for the DiscoverySnapshot model hardening commit.

Reference docs:

- `BACKGROUND_PROCESSING_PLAN.md`
  - Background sort follow-up plan.
- `REGRESSION_CHECKLIST.md`
  - Release/regression test checklist.
- `README.md`
  - General project notes.
- `TODO.md`
  - Older large TODO file. Treat as historical/reference only unless a current item is copied into this shared TODO.
- `HANDOFF_*.md`, `WORKLOG_*.md`
  - Historical handoff/worklog notes.

## Current Stable Baseline

- Branch: `codex/photoplace-v2-bg-wip`
- Latest Play release/draft baseline: `versionCode 29` / `versionName 1.2.8` / `targetSdk 36`
- Latest pushed release checkpoint: `7c6275e Bump version for overseas country identity fix`
- Latest pushed V2 architecture checkpoint: MemoryRepository checkpoint. Check the latest git log and `WORKLOG_2026-08-15.md`.
- Release AAB: `photoplace-1.2.8-code29-overseas-country-identity-api36.aab`
- Release APK: `photoplace-1.2.8-code29-overseas-country-identity-api36.apk`
- Play Console status: 1.2.8 production draft uploaded by API; user manually sent for review.
- Latest confirmed device result:
  - Japan album naming no longer shows `中央区` in the user's test.
  - Existing test folder notification looked normal; the odd notification sequence seems tied to the Japan/travel folder case.
  - Already-sorted duplicate items no longer create a misleading pending original-cleanup count.
  - `위치 없음 0개` no longer shows an empty no-location preview/focus screen.
- Current WIP after 1.2.8 adds the first Display First / Organize Optional model skeleton. Check `git status` before editing and do not discard local changes.
- Keep avoiding the earlier broad Geocoder candidate scoring experiment. It caused POI overclassification.
- Weekend WIP starts from Memory browsing first. Do not jump to Personal Place UI before discovery-only memory browsing works.
- Overseas country grouping/search is stabilized after the country identity patch. The friend device case was rechecked and resolved.
- `일본` / `Japan` / `JP` search is supported through `CountryIdentityNormalizer` and `StoredAlbumSummarySearch`.
- Keep overseas work as observation backlog only. Reopen only if a new real sample/dump fails country grouping/search. Do not add city-by-city country patches.

## Current WIP Note - Display First Model Boundary

Current local WIP intentionally does not change visible UI behavior.

Added as the first architecture checkpoint for V2:

- `MemorySourceType`
- `MediaKind`
- `DiscoveryPhotoRef`
- `DiscoveryMemoryGroup`
- `DiscoverySnapshot`
- `DiscoverySnapshotJson`
- `OrganizedAlbumRef`
- `MemoryRecord`
- `DiscoverySnapshotJsonTest`

Important boundary:

- Do not store discovery-only memories in `AlbumSummaryHistoryStore`.
- Do not represent discovery-only memories as `StoredAlbumSummary` with an empty `relativePath`.
- Do not add new Display First logic to `MainActivity` directly.
- Next implementation should add `DiscoverySnapshotStore`, `DiscoverySnapshotMapper`, and later `MemoryRepository` as separate classes.
- The current skeleton is not wired to Preview, Home, Search, Detail, or SortWorker yet.

Validation:

- `testDebugUnitTest` passes after adding JVM-only `testImplementation "org.json:json:20240303"` for JSON unit tests.

2026-08-14 update:

- Added `DiscoverySnapshotStore` as the first Memory browsing persistence step.
- It stores the existing `DiscoverySnapshotJson` schema in `discovery_snapshot.json`.
- It uses temp/write + backup recovery and refuses to overwrite a corrupt snapshot when no valid backup exists.
- `MainActivity` remains untouched.
- Added `DiscoverySnapshotMapper` as a pure conversion layer from analyzed `PhotoItem` data to `DiscoverySnapshot` groups.
- Mapper MVP uses existing `locationKey` grouping only; it does not perform GPS clustering because raw lat/lng is not preserved in `PhotoItem`.
- `MemoryRepository` and discovery-only detail are still not implemented.
- Added mapper edge-case tests for null input, unknown time, and trimmed location keys.
- Test/validation checkpoint:
  - `.\gradlew.bat testDebugUnitTest`
  - `.\gradlew.bat assembleDebug`
  - `MainActivity` unchanged.

2026-08-15 update:

- Added `MemoryRepository` as the first read-only facade for Memory browsing first.
- It converts discovery-only groups and organized albums into `MemoryRecord`.
- It does not write discovery-only records into `AlbumSummaryHistoryStore`.
- It merges records only when the exact `memoryKey` matches; it does not guess by title/country/city.
- `MainActivity` remains untouched.
- Gemini review prompt is in `WORKLOG_2026-08-15.md`.
- `MemoryRepository` UI wiring and discovery-only detail screen are still not implemented.

## Immediate Next Session Checklist

Start from:

- Branch: `codex/photoplace-v2-bg-wip`
- Commit: latest `MemoryRepository` checkpoint in git log.

Before coding:

1. Check `git status`.
2. Read this file, `WORKLOG_2026-08-14.md`, `MEMORY_BROWSING_FIRST_DESIGN.md`, and `Photoplace_V2_Epic_PRD_2026-08-09.md`.
3. Keep replies/reviews in Korean when prompting Gemini.

Next code patch after reviewing `MemoryRepository`:

1. Address Gemini review findings if any.
2. Add a small UI-facing controller/adapter for memory list state.
3. Wire the least possible UI entry point.
4. Keep `MainActivity` changes thin and avoid moving repository logic into it.
5. Do not write discovery-only rows to `AlbumSummaryHistoryStore`.
6. Do not use empty `relativePath` as a fake organized album.

Focused tests for the next UI-facing patch:

- memory list state from discovery-only records.
- empty state when no snapshot/history exists.
- detail handoff by `memoryKey` returns source refs for discovery-only records.
- organized album actions remain Gallery-based.
- `MainActivity` contains only wiring, not repository logic.

Older release follow-ups are separate backlog, not the next coding default:

- Overseas country identity follow-up only for new real repros. The known friend case is resolved.
- 5,300-folder anomaly if a dump can confirm the source data shape.
- Notification phase reliability for folders that switch from analysis to album creation.
- Home latency monitoring on large libraries.

## Current Release/WIP Note - 2026-08-09

Post-1.2.6 polish and sort history search MVP were committed, pushed, and prepared as Play draft `1.2.7 (28)`.

Kept changes small:

- completion notification is cleared when the app opens/resumes, so Samsung launcher badge does not stay stuck after the user enters the app.
- preview-only result screen no longer shows the large `확인 완료` header card.
- low-value result-screen recheck action was removed.
- original-photo cleanup card appears higher in the result screen when pending originals exist.
- action button icons were made smaller/lighter, closer to a One UI-like line tone.

Decision:

- Search MVP made the update worth releasing.
- Production draft is uploaded; user still needs to manually review Play policy warnings and send for review.

Next work should be design-led and split out of `MainActivity` where practical.

Recommended next feature order:

1. Display First snapshot foundation:
  - `DiscoverySnapshotStore`
  - `DiscoverySnapshotMapper`
  - Preview result saved as discovery-only memory data.
2. MemoryRepository / MemoryRecord UI path:
  - Preview complete -> `발견한 장소 둘러보기`
  - discovery-only Memory detail using original photo URIs
  - merge organized albums and discovery-only memories without storing discovery-only rows in `AlbumSummaryHistoryStore`
  - keep `Gallery 앨범으로 정리`
3. Repeated-place / Personal Place recommendation MVP:
  - Detect dense GPS clusters inside broad admin groups such as `수원`, `성남`, `송파구`.
  - Suggest user-confirmed names like `집`, `회사`, `발레학원`.
  - Keep saving a place separate from moving files.
  - Must pass `Candidate Quality Validation` before any recommendation UI or Gallery action is built.
4. Country/date/place search expansion:
  - `일본`, `Japan`, `JP` is implemented for country identity search; keep regression tests when touching search.
  - `8월`, `2026년 8월`
  - `삿포로` / `sapporo`
5. Memory Personalization displayName MVP and search integration.
6. No-location repeated-analysis prevention via snapshot/cache, not file movement first.
7. International address normalization design.
8. Result/detail UI consistency pass.

Latest product observation:

- In a real user test, the user immediately searched `일본` instead of first focusing on album creation.
- Treat this as evidence that PhotoPlace can be perceived as a place-based memory search tool.
- This strengthens the priority of visible/fast search, country/place search quality, displayName, and the Display First experiment.
- Repeated-place clustering is now a core V2 memory feature, not a distant POI feature.
  - Example: if broad groups show `수원 400장` or `성남 800장`, the app should help discover smaller meaningful places such as home, company, family home, or ballet academy.
  - The app should recommend a candidate and ask the user to name/confirm it; it must not automatically create or move Gallery albums.
  - The first risk to validate is whether repeated GPS clusters are actually meaningful to users. Treat radius/count/date thresholds as experiment parameters until validated.
- Search TODO from user feedback:
  - Country search now works for normalized country identity records (`일본`, `Japan`, `JP`). Recheck only for old/corrupt history or friend sample failures.
  - Add Korean date query support later: `8월`, `2026년 8월`, `8월 2일`.
  - Add localized/romanized alias search where practical: `삿포로` and `sapporo` should find the same memory.
  - Prefer a small alias/normalization layer over one-off string checks inside `MainActivity`.
  - Overseas-history country bug should not be fixed by city-by-city patches (`KarlovyVary`, `Fatih`, etc.).
  - Proper direction is structured country normalization: persist/derive `countryCode` or canonical country identity, then map to Korean display name consistently.
  - Keep this as TODO for now; wait for more friend feedback before coding.

## Current WIP Note - Memory Personalization Base

Implemented as a low-risk first step:

- Added `MemoryPersonalization`, `MemoryPersonalizationKey`, and `MemoryPersonalizationStore`.
- Existing memo UI still behaves the same from the user's point of view.
- `MainActivity.albumMemory()` and `saveAlbumMemory()` now delegate to the store instead of owning SharedPreferences key logic.
- Legacy `album_memory_` and `album_alias_` values are treated as memo fallback only, not as display names.
- Lazy migration writes legacy memo into `memory_personalization.json` and removes legacy keys only after the JSON write succeeds.
- Gemini review edge case was addressed: if displayName/userCover is saved before memo migration, the update path now seeds from legacy memo first so old notes are not hidden.
- Corrupt existing `memory_personalization.json` is not treated as a writable empty store; write paths stop instead of overwriting it.
- `displayName` and `userCoverUri` fields exist in the model/store, but no UI uses them yet.
- Unit tests cover key policy and model update behavior.

Do not assume display-name editing, cover editing, or displayName search is complete yet.

## Current Release/WIP Note - 2026-08-06

The current uncommitted WIP was built and uploaded as Play production draft `1.2.6 (27)`.

Today started as a Japan/overseas-history fix, but changes spread into:

- overseas country/place normalization
- Japanese/English/Kanji place-name canonicalization
- activity back-stack `singleTask`
- original-cleanup count safety
- no-location empty-state safety
- notification progress experiments

Some broad performance experiments were backed out during the session. Keep the final scope focused and do not reintroduce heavy home/history validation without profiling.

Next work should avoid widening this stabilization batch:

1. Keep pure overseas-history normalization candidates:
  - `OverseasMemoryGrouper`
  - focused `PlaceNamePolicy` mappings
  - unit tests
2. Keep the original-cleanup and no-location empty-state fixes.
3. Treat notification progress as a follow-up architecture issue, not a quick patch.
4. Commit a stable checkpoint after Play submission/review status is confirmed.

## Completed Today

- WorkManager background sort stabilization from 1.2.2 remains the baseline.
- Fold/wide layout was adjusted:
  - home uses compact status summary instead of large status cards.
  - sort history/recent places use width-based grid behavior, with Fold opened layout kept at 3 columns after testing 4 columns.
  - 4 columns showed more items but 3 columns was visually better.
- Overseas history metadata persistence was fixed for background-completed sorts:
  - Worker result now persists sorted `PhotoItem` metadata, not only sorted URIs.
  - country/address/admin metadata is preserved for summary/history regeneration.
  - empty literal `"null"` metadata is cleaned when restoring summaries.
- Result/home performance patches were verified on device:
  - home entry improved.
  - sort result detail opens much faster after lazy/heavier work was deferred.
- Play release automation skill was added:
  - `C:\Users\mismi\.codex\skills\photoplace-release`
  - It builds APK/AAB and can upload a Play Console production draft.
  - Final Play Console review submission remains manual.
- Release `1.2.5` / code `26` AAB was built and uploaded as a Play production draft.
- Release `1.2.6` / code `27` APK+AAB was built.
- Release `1.2.6` / code `27` AAB was uploaded as a Play production draft.
- Release notes file:
  - `release-notes-1.2.6-ko.txt`

## Known Open TODOs

1. Sort history search (V2 priority):
  - User need: 정리기록/최근 발견한 장소가 많아지면 드래그로 찾기 어렵다.
  - Add a search bar to the sort history/recent places screen.
  - Search target: album/place display name, country, admin/address metadata, date text if practical.
  - Selecting a result should open the matching album/place detail screen directly.
  - Keep filtering local and fast; do not scan MediaStore on every keystroke.
  - Suggested first implementation: filter already loaded `StoredAlbumSummary` list in memory, then reuse `showRecentPlaceDetailScreen(summary)`.
2. International address normalization (design before coding):
  - Current fix for `中央区` is a stabilization patch, not the final model.
  - Do not keep growing `knownTravelPlaceName()` into a giant city alias table.
  - Add a structured layer before final album naming:
    - parse address components
    - normalize country/admin/locality/subLocality
    - decide canonical city/place
    - then format display name
  - Overseas rule: subLocality/ward alone must not become a final album name.
  - Stable key/display name should eventually be separated, for example `JP|Hokkaido|Sapporo` vs `삿포로`.
3. No-location repeated-analysis prevention (snapshot/cache first):
  - Problem: users with thousands of no-GPS photos repeatedly re-scan the same files.
  - With Display First snapshots, solve this primarily by remembering analyzed no-location file identities.
  - Store enough signature data to skip unchanged no-location items:
    - mediaStoreId or sourceUri
    - displayName
    - taken/modified time where available
    - size/path signal if available
    - no-location analysis result timestamp/policy version
  - Preview copy should be transparent, for example:
    - `위치 정보 없는 항목 2,034개는 이전에 확인되어 이번 분석에서 제외했어요.`
    - Action: `다시 확인하기`
  - Folder move is deferred and may never be needed if snapshot/cache skip works well.
  - Do not default-enable moving files just to avoid re-analysis.
  - Prefer not implementing no-location folder move until real tests prove skip/cache is insufficient.
4. Friend device follow-up:
  - Friend has 10,000+ photos.
  - Albums are reportedly created correctly.
  - Overseas history reportedly showed only Japan.
  - Ask for at least 1-2 sample photos from missing overseas countries if possible.
  - Run "rebuild discovered places" on the updated build and confirm country count.
  - If possible, collect logs/dump and compare stored history JSON vs actual generated album folders.
5. Investigate reported 5,300-folder anomaly:
  - User observed record/history count around 5,300 folders while actual generated albums seemed around 40.
  - Need dump/sample before assuming root cause.
  - Suspect duplicate grouping/state regeneration, not necessarily file creation.
6. Home latency:
  - Current home is acceptable but slightly less instant than earlier.
  - Watch for delays where overseas/recent places appear after first draw.
  - Avoid blocking first draw on heavy MediaStore/history reconciliation.
7. Back/navigation:
  - Most back issues were patched, but keep this high in regression testing.
  - Especially test sorting, result detail, history tab, Home tab, and Fold open/close.
8. Notification reliability:
  - Still open as of 2026-08-06 late test.
  - Device symptom: progress can move to a value such as `141`, then later jump/restart as `1 / 178`, then finish.
  - Existing test folder notification looked normal. Japan/travel folder showed the odd sequence.
  - This appears to be more than a simple notification refresh bug; a preview/rebuild/result-refresh phase may be reusing or reseeding the same progress path.
  - Do not keep patching blindly tonight. Next pass should separate sort-copy progress from preview/rebuild/result-refresh progress, and decide which phases deserve OS notifications.
9. Personal Place PRD should be analyzed before implementation.
10. Airplane / drive-through location handling is not implemented. Prefer visible badges such as "in flight" / "moving", not automatic exclusion.
11. POI policy:
  - Do not broaden automatic POI classification.
  - Everland / Seoul Arts Center may still fall back to administrative names because Google Photos place labels are not the same data returned by Android Geocoder.
  - Prefer Personal Place recommendation for reliable user-confirmed names.
12. Result album detail should eventually support viewing more/all photos, not only representative thumbnails.
13. Process-death recovery can be hardened further for completed Worker results.
14. Split `MainActivity` further once the current release settles:
  - background sort coordinator
  - result screen renderer/model
  - original cleanup state/store
  - memory/home sections

## Latest Late-Night Device Findings - 2026-08-06

Keep from the current WIP if final device testing still agrees:

- Japan album naming: `中央区` no longer appears after the Sapporo/Hokkaido context fix.
- Original cleanup count: already-sorted/duplicate items are no longer added to pending original-trash candidates.
- Empty no-location UI: when location-missing count is `0`, the no-location preview section is hidden and focus screen entry is blocked with a toast.

Still unresolved:

- OS notification progress is not reliable enough. It may stall, then restart with a different total (`1 / 178`) before completion.
- Treat notification as follow-up architecture work, not a quick release blocker patch, unless a minimal phase-separation fix is identified.

## Personal Place Guardrails

- Repeated GPS cluster recommendation is a V2 core memory feature.
- It solves the broad-admin-group problem:
  - `수원에서 400장` may contain home, company, school, cafe, or family place memories.
  - `성남에서 800장` may contain home/work/frequent routines that should not stay buried inside one city card.
- Add a `Candidate Quality Validation` gate before UI:
  - generate candidates from existing photos
  - output top 20 candidates
  - review photos/date distribution/coordinate spread
  - manually label each as meaningful / ambiguous / false positive
  - tune thresholds before building recommendation UI
- Do not implement Gallery creation, History, or Undo for Personal Place until candidate quality is validated.
- Do not strengthen automatic POI classification broadly.
- Personal Place is a user-confirmed layer, not automatic folder renaming.
- Saving a personal place must not move/copy files by itself.
- Actual gallery album creation/move must be a separate explicit confirmation.
- Initial MVP uses contextual recommendation first:
  - show candidates inside the parent PhotoPlace memory/place detail such as `송파구`, `수원`, or `성남`.
  - do not start with a global Home popup like "여러 번 방문한 장소를 발견했어요".
  - here "folder" means a PhotoPlace parent memory/place group, not a physical Gallery folder.
  - example: `송파구` detail -> "이 안에서 여러 번 방문한 장소가 있어요" -> photo review -> user names it `라비에벨 발레`.
- Home/global resurfacing is a later step only after contextual recommendations prove useful.
- Keep model contracts explicit:
  - `PersonalPlace` is a user-confirmed overlay on a parent `MemoryRecord`, not a replacement for the original `placeKey`.
  - `PhotoPlaceMembership` stores confirmed existing photo membership.
  - Future GPS matches start as candidate/provisional, not automatically confirmed membership.
  - `PlaceCandidate` needs `sourceSnapshotVersion`, `candidatePolicyVersion`, `candidateSignature`, score/count/date/radius metadata, and review state.
  - dismissed candidates need stable identity; MVP can use `candidatePolicyVersion + clusterSignature + sourceSnapshotVersion`, then improve approximate matching later.
  - same displayName can exist inside PhotoPlace; check Gallery album name collision only when explicitly creating a Gallery album.
  - candidate coordinates, membership, and signatures are sensitive local data; do not send them to analytics by default.
- Priority order should be:
  1. user-confirmed Personal Place
  2. highly reliable known POI
  3. administrative location
- Before coding, analyze existing structures:
  - `PlaceNamePolicy`
  - `MainActivity` location naming flow
  - `PhotoItem`
  - `StoredAlbumSummary`
  - `SortInputStore`
  - `AlbumSummaryHistoryStore`
  - `SortWorker`
  - sort result / undo feasibility

## Suggested Gemini Task

Analyze only first. Do not edit code.

Goal: propose the minimum-change design for Personal Place MVP:

- repeated GPS cluster candidate generation
- separating broad admin groups into user-confirmed personal memories
- Candidate Quality Validation before UI
- contextual recommendation inside a parent Memory/place detail first
- candidate photo review screen
- user-entered place name storage
- personal place priority in app views
- file movement kept separate from place saving

Output expected:

- affected files/classes
- proposed storage model
- data flow
- UI entry points
- risks
- implementation steps split into small patches

## Next Collaboration Plan

### Product Direction

Personal Place should solve reliable place naming without broad automatic POI promotion.

Preferred UX:

- The app finds repeated GPS clusters inside broad places like `수원`, `성남`, `송파구`.
- The app recommends a memory place, for example:
  - "이 근처에서 여러 번 찍은 사진이 있어요."
  - "이 장소를 `회사` 또는 `발레학원`처럼 기억할까요?"
- The user confirms or edits the name.
- Confirmed Personal Place names are used first inside PhotoPlace memory/result views.
- Saving a Personal Place does not move/copy files.
- Creating or reorganizing gallery albums from a Personal Place requires a separate explicit action.

### Gemini Responsibilities

Gemini should work in analysis/design mode first:

1. Read:
   - `AGENT_SHARED_TODO.md`
   - `WORKLOG_2026-07-26.md`
   - `Photoplace_V2_Personal_Place_PRD.md`
   - `BACKGROUND_PROCESSING_PLAN.md`
2. Review existing code structures without editing:
   - `PlaceNamePolicy`
   - `MainActivity` location naming and result rendering flow
   - `PhotoItem`
   - `StoredAlbumSummary`
   - `AlbumSummaryHistoryStore`
   - `SortInputStore`
   - `SortWorker`
3. Produce a minimal implementation design:
   - data model: `PersonalPlace`, `PlaceCandidate`, optional membership/cache model
   - storage: likely `PersonalPlaceStore` JSON following existing `*Store` pattern
   - candidate generation strategy
   - Candidate Quality Validation report format for the top 20 candidates
   - contextual entry flow from parent Memory/detail, not a global Home popup
   - `PersonalPlace` / `PhotoPlaceMembership` / future provisional match separation
   - candidate dismiss identity policy
   - privacy guardrails for home/work/private repeated places
   - UI entry points and copy
   - how app views apply confirmed Personal Place names
   - how file movement remains separate
   - risks and test cases
4. Suggest small patch sequence for Codex.

Gemini should not:

- edit code before the design is accepted.
- add broad POI scoring.
- auto-rename albums/files.
- auto-exclude flight/drive-through places.

### Codex Responsibilities

Codex should own implementation and device verification:

1. Review Gemini design and trim MVP scope.
2. Implement small patches only after scope is clear.
3. Keep code split where practical instead of adding more bulk to `MainActivity`.
4. Run:
   - `testDebugUnitTest`
   - `assembleDebug`
   - device install / ADB checks
5. Verify on phone with real test folders.
6. Commit after each stable patch.

### Suggested MVP Patch Order

1. Add `DiscoverySnapshotStore` so analysis results can be reused without Gallery albums.
2. Add `DiscoverySnapshotMapper`.
3. Add `MemoryRepository` / `MemoryRecord` adapter so users can browse discovered memories before any Gallery album is created.
4. Add discovery-only Memory detail using original photo URIs, not `relativePath`.
5. Add no-location skip metadata to the snapshot/cache path.
6. Add pure repeated-place candidate grouping logic with tests.
7. Add Candidate Quality Validation output for top candidates; no UI yet.
8. Tune thresholds/radius based on real data.
9. Add `PersonalPlace`, `PlaceCandidate`, `PhotoPlaceMembership`, and store models after the candidate contract is clear.
10. Add contextual recommendation surface inside parent Memory/place detail only after candidate quality is acceptable.
11. Add candidate photo review and confirm/edit place name dialog.
12. Apply confirmed Personal Place names in PhotoPlace internal views only.
13. Add management affordance: edit/delete/hide recommendation.
14. Later, add explicit "create album from this place" flow if still needed.

### Open Questions For Next Session

- Candidate radius:
  - start conservative, maybe 100-200m for personal clusters.
  - known venues may use custom smaller/larger radii only after sample review.
- Candidate threshold:
  - minimum photo count and minimum date spread should avoid one-off noise.
- Candidate Quality Validation:
  - define what "good enough" means before UI work starts.
  - track false positive types such as station/road/parking lot/mall clusters.
- Sensitive places:
  - home/work-like repeated private places should not be loudly surfaced without careful wording.
- Flight/drive-through:
  - keep as badge/review category later, not auto-delete/exclude.
- Existing users:
  - Personal Place recommendations should be generated from existing history/media without forcing album reorganization.
