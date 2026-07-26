# PhotoPlace Agent Shared TODO

Updated: 2026-07-26

This file is the shared handoff note for Codex and Gemini CLI. Keep it short, current, and safe to act on.

## Workspace And Shared Docs

Project root:

- `C:\Users\mismi\Documents\Codex\GallerySorter`

Read these first:

- `AGENT_SHARED_TODO.md`
  - Current shared status, stable baseline, open TODOs, and guardrails.
- `WORKLOG_2026-07-26.md`
  - Date-based work record for the WorkManager/background-sort stabilization day.
- `Photoplace_V2_Personal_Place_PRD.md`
  - Personal Place PRD draft. Encoding may look broken in some terminals, but the final Gemini handoff section was already understood by Gemini.

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
- Latest release candidate commit: `bbcfb3b Bump version for 1.2.2 release`
- Latest release build: `versionCode 23` / `versionName 1.2.2` / `targetSdk 36`
- Release AAB: `app\build\outputs\bundle\release\photoplace-1.2.2-code23-api36-release.aab`
- Play Console review was submitted on 2026-07-26 after completing the foreground service declaration/video.
- Latest confirmed device result: home, sort history, sort result, background sort, notification entry, original-trash confirmation popup, and already-sorted re-run flow looked stable in user testing.
- Keep avoiding the earlier broad Geocoder candidate scoring experiment. It caused POI overclassification.

## Completed Today

- WorkManager-based background sort path is in place and tested with a large local test set.
- Sort progress, completion notification, and notification app entry were verified.
- Back during sorting now shows a choice dialog: continue in background, stop sorting, or cancel.
- Already-sorted and missing-album states were reconciled against live MediaStore data.
- Empty/stale thumbnails from deleted albums were hidden or recovered via fallback.
- Home performance regression from pending-original cleanup loading was reduced.
- Home/result/history navigation bugs were patched:
  - result/detail fallback now routes to stored history when in-memory result detail is gone.
  - bottom Home tab works from result/history screens.
  - home CTA says "View sort history" when only stored history is available.
- Pending original cleanup CTA is visible in both:
  - Home, above overseas memories when pending originals exist.
  - Sort history, at the top of the screen.
- Result screen performance was significantly improved by avoiding eager construction of thousands of original-cleanup URI entries.
- Home return now shows the main screen quickly and delays heavier pending-original cleanup validation.
- Completed-result counts now fall back to live already-sorted items when recently-sorted in-memory markers are gone.
- POI naming is now allowlist-based for the first safe set:
  - Everland
  - Lotte World
  - Seoul Arts Center
  - Bundang Seoul National University Hospital
  - Incheon International Airport
- Known bad POI/detail fallbacks such as random stations, schools, cafes, access points, and lake/store names should fall back to administrative names.
- Release `1.2.2` / code `23` was built and submitted to Play review.

## Known Open TODOs

1. Wait for Play review result and collect user feedback after asking testers to update.
2. Personal Place PRD should be analyzed before implementation.
3. Airplane / drive-through location handling is not implemented. Prefer visible badges such as "in flight" / "moving", not automatic exclusion.
4. Everland/Yongin and other POI edge cases should be handled later with real sample evidence, not broad POI promotion.
5. Result album detail should eventually support viewing more/all photos, not only representative thumbnails.
6. Process-death recovery can be hardened further for completed Worker results.
7. Split `MainActivity` further once the current release settles:
  - background sort coordinator
  - result screen renderer/model
  - original cleanup state/store
  - memory/home sections

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
