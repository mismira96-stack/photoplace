# Review Request: Memory Collection UI Separation and Discovery Video Guard

Date: 2026-09-13
Branch: `codex/photoplace-v2-bg-wip`
State: Working tree changes; not committed or pushed

## Review Scope

Please review the current working-tree diff against `HEAD`. Treat the existing uncommitted changes as user work and do not modify files. Focus on correctness, regressions, and whether the stated media-safety boundary is actually enforced by the code.

### 1. Memory Collection renderer separation

New UI rendering is separated from `MainActivity` into:

- `MemoryCollectionCardRenderer`
- `MemoryCollectionSelectionRenderer`
- `MemoryCollectionDetailRenderer`

`MainActivity` retains data/store access, stable-ID creation, screen state/navigation, and shared editor integration. Verify that this is a meaningful responsibility split and that no selection/detail view construction remains unnecessarily embedded in the Activity.

The selection screen was adjusted toward the supplied visual reference:

- thumbnail, place title, media count, and date are shown as separate row content;
- selection uses a circular indicator and a lightly tinted selected row;
- already-collected members remain disabled;
- selected count and create CTA update together.

The Discovery browser's collection section was also polished without changing grouping behavior:

- Collection cards show a `기억 모음` badge and up to three member names followed by `외 N곳`;
- the `기억 모으기` CTA uses a light tint, while the bulk Gallery action is now labeled `위치 앨범 만들기`;
- the individual grid has a `발견한 장소` heading;
- grouped members remain folded from the default grid but are still searchable; no `전체 보기` action was added without a destination screen.

Check selection toggling, disabled-member behavior, layout refresh/rotation restoration, accessibility labels, and back navigation for regressions introduced by the extraction/polish.

### 2. Discovery video movement guard

Device testing confirmed that a video had been moved to a location album and then disappeared from Discovery. The current Memory detail still reads live Discovery references; it does not yet resolve linked Gallery output media.

Until the shared Memory media resolver exists, the current change intends to:

- exclude videos from both single-Memory and bulk Discovery organization before they reach the copy/move Worker, regardless of the global `move_videos` preference;
- explain excluded videos in the Discovery organization confirmation UI;
- leave generic non-Discovery organization behavior governed by the existing user preference;
- avoid claiming that previously moved videos are restored. Those require the future resolver.

Review whether this is enforced on every Discovery organization route, including retries and empty/photo-duplicate cases. Confirm that the worker input cannot still contain a video when the global preference is enabled and that result counts/messages remain accurate. Do not infer that a UI-only disabled option is sufficient; trace the actual `PhotoItem` list passed to the worker.

### 3. Internal Photo Viewer boundary

The user confirmed that tapping a Memory thumbnail currently reaches an external-app chooser. Internal same-place + same-date swipe viewing remains a separate P1 task and is not implemented in this diff. Confirm the TODO reflects PhotoPlace's internal viewer as the primary action, with external Gallery opening only as a secondary action.

## Changed/Relevant Files

- `app/src/main/java/com/example/gallerysorter/MainActivity.java`
- `app/src/main/java/com/example/gallerysorter/MemoryCollectionCardRenderer.java`
- `app/src/main/java/com/example/gallerysorter/MemoryCollectionSelectionRenderer.java`
- `app/src/main/java/com/example/gallerysorter/MemoryCollectionDetailRenderer.java`
- `app/src/main/java/com/example/gallerysorter/DiscoveryAlbumItemAdapter.java`
- `app/src/main/java/com/example/gallerysorter/DiscoveryOrganizeConfirmDialog.java`
- `app/src/main/java/com/example/gallerysorter/OrganizationMediaPolicy.java`
- `app/src/test/java/com/example/gallerysorter/OrganizationMediaPolicyTest.java`
- `TODO.md`
- `WORKLOG_2026-09-13.md`

Other unrelated dirty and untracked files exist in the working tree; do not include them in review conclusions unless a direct dependency is demonstrated.

## Verification Already Run

- `.\gradlew.bat testDebugUnitTest assembleDebug`: PASS
- `git diff --check`: PASS
- Debug APK installed with `adb install -r` on `R5KL503VHQR` without clearing app data; app launch confirmed.
- No album creation or media move was triggered during this validation.
- Visual interaction smoke for the updated selection screen remains pending.

## Expected Review Output

1. Findings first, severity ordered, with exact file/line references.
2. Specifically state whether video exclusion is enforced at the actual Worker input boundary for every Discovery path.
3. Identify any test gaps or cases that should be added before commit.
4. Separate release blockers from polish recommendations.
5. Do not edit code or documents.
