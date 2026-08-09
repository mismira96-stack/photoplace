# PhotoPlace Agent Shared TODO

Updated: 2026-08-09

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
- `WORKLOG_2026-07-26.md`
  - Date-based work record for the WorkManager/background-sort stabilization day.
- `Photoplace_V2_Personal_Place_PRD.md`
  - Personal Place PRD draft. Encoding may look broken in some terminals, but the final Gemini handoff section was already understood by Gemini.
- `Photoplace_V2_Epic_PRD_2026-08-09.md`
  - V2 PRD reorganized by Epic/P0/P1/P2 with code-based implementation status after 1.2.7.
- `MEMORY_PERSONALIZATION_P1_DESIGN.md`
  - P1 Memory Personalization design for display name, memo, and user cover storage. Use this before coding personalization UI.

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
- Latest Play release/draft baseline: `versionCode 28` / `versionName 1.2.7` / `targetSdk 36`
- Latest pushed search checkpoint: `0ba4c4f Polish sort history search affordance`
- Release AAB: `photoplace-1.2.7-code28-sort-history-search-api36.aab`
- Release APK: `photoplace-1.2.7-code28-sort-history-search-api36.apk`
- Play Console status: 1.2.7 production draft uploaded by API. Final "Send for review" remains manual.
- Latest confirmed device result:
  - Japan album naming no longer shows `中央区` in the user's test.
  - Existing test folder notification looked normal; the odd notification sequence seems tied to the Japan/travel folder case.
  - Already-sorted duplicate items no longer create a misleading pending original-cleanup count.
  - `위치 없음 0개` no longer shows an empty no-location preview/focus screen.
- Current WIP after 1.2.7 adds the first Memory Personalization storage layer. Check `git status` before editing and do not discard local changes.
- Keep avoiding the earlier broad Geocoder candidate scoring experiment. It caused POI overclassification.

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

1. Memory Personalization displayName MVP on top of the new store.
2. Search displayName integration.
3. No-location UX decision flow.
4. International address normalization design.
5. Result/detail UI consistency pass.
6. Personal Place MVP design/implementation.

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
3. No-location folder move option (design + confirm UX):
  - Problem: users with thousands of no-GPS photos repeatedly re-scan the same files.
  - Do not default-enable file movement.
  - Add a preview-time confirm checkbox/card only when no-location count is high.
  - Suggested copy:
    - `위치 정보 없는 항목 2,034개`
    - `다음 정리 때 다시 검사하지 않도록 별도 폴더로 이동할까요?`
    - Checkbox: `위치 정보 없는 항목을 PhotoPlace/위치 없음 폴더로 이동`
    - Warning: `원본 파일 위치가 바뀝니다. 사진은 삭제되지 않으며 갤러리에서 계속 볼 수 있어요.`
  - Keep default OFF; if user chooses it, remember preference but still surface it in preview.
  - Start with photos only or very explicit video handling.
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

## Immediate Next Session Checklist

1. Confirm Play Console status for 1.2.6:
  - draft submitted manually or review pending
  - no extra foreground-service declaration issue
2. Smoke-test the Play/installed 1.2.6 build:
  - Japan folder still does not show `中央区`.
  - Original cleanup prompt does not show the already-sorted count.
  - `위치 없음 0개` has no empty preview.
  - Existing test folder notification is still normal.
3. On friend's device:
  - install latest build or confirm Play update.
  - run only "rebuild discovered places" first; no need to fully sort again if folders already exist.
  - check overseas history country list.
  - check sort history count vs actual generated album count.
  - collect sample photos/logs if mismatch remains.
4. If overseas history still fails:
  - inspect `album_summary_history.json` / restored summary metadata path.
  - verify `StoredAlbumSummary.countryName`, `addressLine`, `adminArea`, `albumName`, and `representativeUri`.
5. If 5,300-folder count reproduces:
  - do not patch blindly.
  - first determine whether count means generated albums, history rows, photo groups, or raw media rows.
6. Next feature candidates, in recommended order:
  - sort history search
  - no-location folder move confirm option
  - international address normalizer design
  - Personal Place MVP
  - moving/in-flight badges
  - all-photos result detail
  - resume-from-stop sorting

## Latest Late-Night Device Findings - 2026-08-06

Keep from the current WIP if final device testing still agrees:

- Japan album naming: `中央区` no longer appears after the Sapporo/Hokkaido context fix.
- Original cleanup count: already-sorted/duplicate items are no longer added to pending original-trash candidates.
- Empty no-location UI: when location-missing count is `0`, the no-location preview section is hidden and focus screen entry is blocked with a toast.

Still unresolved:

- OS notification progress is not reliable enough. It may stall, then restart with a different total (`1 / 178`) before completion.
- Treat notification as follow-up architecture work, not a quick release blocker patch, unless a minimal phase-separation fix is identified.

## Personal Place Guardrails

- Do not strengthen automatic POI classification broadly.
- Personal Place is a user-confirmed layer, not automatic folder renaming.
- Saving a personal place must not move/copy files by itself.
- Actual gallery album creation/move must be a separate explicit confirmation.
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
- preview/home recommendation card
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

- The app finds repeated GPS clusters or strong known-place candidates.
- The app recommends a memory place, for example:
  - "Photos near Seoul Arts Center were found."
  - "Remember this place as 'Seoul Arts Center'?"
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

1. Add `PersonalPlace` model and `PersonalPlaceStore`.
2. Add pure candidate grouping logic with tests.
3. Add non-invasive recommendation surface in app UI.
4. Add confirm/edit place name dialog.
5. Apply confirmed Personal Place names in PhotoPlace internal views only.
6. Add management affordance: edit/delete/hide recommendation.
7. Later, add explicit "create album from this place" flow if still needed.

### Open Questions For Next Session

- Candidate radius:
  - start conservative, maybe 100-200m for personal clusters.
  - known venues may use custom smaller/larger radii only after sample review.
- Candidate threshold:
  - minimum photo count and minimum date spread should avoid one-off noise.
- Sensitive places:
  - home/work-like repeated private places should not be loudly surfaced without careful wording.
- Flight/drive-through:
  - keep as badge/review category later, not auto-delete/exclude.
- Existing users:
  - Personal Place recommendations should be generated from existing history/media without forcing album reorganization.
