# PhotoPlace Background Processing Plan

## Current State

- `SortJob` already owns the copy/move loop for a prepared `List<PhotoItem>`.
- `SortWorker` exists, but it is only a foreground Worker shell.
- `SortProgressStore` stores active/current/total/context and lets the Activity recover stale UI state.
- `SortForegroundService` and `SortNotificationHelper` handle ongoing/completed notifications.
- The blocking gap: `previewItems` live only in `MainActivity` memory, so a Worker cannot resume the real job after process death.

## Prepared Tonight

- `SortInputStore` added.
  - Writes a `sort_input_snapshot.json` file with:
    - `shouldMoveVideos`
    - created timestamp
    - all `PhotoItem` fields needed by `SortJob`
  - Reads the snapshot back into `PhotoItem` objects.
  - Not wired to production flow yet.
- `MediaAnalysisSignature` added for future analysis cache keys.
- `PlaceNamePolicy` and `MovementClassifier` are now testable pure policy classes.

## Recommended Implementation Order

### Step 1. Persist Sort Input Before Copy

- In `MainActivity.runCopy()`:
  - after video permission checks
  - before `setWorking(true, ...)`
  - write `new SortInputStore(this).write(previewItems, shouldMoveVideos)`
- If write fails:
  - keep current Activity worker path
  - show no blocking error

### Step 2. Let `SortWorker` Run `SortJob`

- In `SortWorker.doWork()`:
  - read `SortInputStore.Snapshot`
  - if empty, finish progress and return success
  - create `MediaCopyEngine` with application context
  - create `SortJob`
  - update `SortProgressStore` and Worker foreground notification from `onItem`
  - run job
  - persist result
  - clear input snapshot only after result persistence succeeds

### Step 3. Persist Worker Result

- Add `SortResultStore`.
- Store:
  - copied count
  - skipped count
  - failed count
  - canceled
  - sorted uris
  - copied original uris
  - compact log
  - completed timestamp
- `MainActivity` reads this store on resume and updates:
  - `previewItems` sorted flags
  - pending original cleanup file
  - summary history
  - result screen mode

### Step 4. Switch Launch Path

- Replace direct Activity worker execution with `WorkManager.enqueueUniqueWork`.
- Keep a fallback:
  - if Worker enqueue fails, run current Activity worker.
- Keep `requestCancel()` writing cancel state somewhere Worker can read.

### Step 5. Cancellation Contract

- Add `SortCancelStore` or use Worker cancellation APIs.
- Worker should treat cancellation as:
  - stop after current item
  - save partial result
  - show stopped notification
  - leave remaining items visible as `새 항목만 정리`

## Safety Rules

- Do not move original photo trash cleanup into Worker. Trash requires user confirmation.
- Do not enable `NoLocationCache` until cache invalidation tests are in place.
- Do not auto-exclude `IN_FLIGHT` or `MOVING`; only display badges first.
- Do not delete `sort_input_snapshot.json` until result persistence succeeds.

## Morning Device Checks

- Start a 4087-item duplicate-folder preview.
- Start sort, press Home, return from notification.
- Confirm progress resumes in-app.
- Stop midway and restart.
- Confirm already sorted items are not copied again.

## 2026-07-25 Update

### Done

- `SortWorker` now runs the persisted `SortInputStore` snapshot through WorkManager.
- `SortResultStore` persists copied/skipped/failed/canceled counts, sorted URIs, copied original URIs, and compact logs.
- `MainActivity.runCopy()` now enqueues `SortWorker` first and keeps the old Activity executor path as fallback.
- `MainActivity` polls Worker results and attaches the completion result screen when the app resumes.
- `requestCancel()` now cancels the unique WorkManager sort work.
- Pending original cleanup now stores the related album relative paths and clears stale cleanup state when the generated albums were deleted externally.
- Verified:
  - `testDebugUnitTest assembleDebug` passed.
  - Debug APK installed on Galaxy device.
  - External album deletion no longer leaves the "original cleanup needed" card stuck.

### Remaining Weekend Work

- Device test the WorkManager path with the 4087-item Download duplicate folder:
  - start sort
  - press Home
  - return from notification
  - confirm progress and result attach
  - stop midway and restart
- Throttle Worker foreground updates if notification progress is too chatty on the device.
- Harden cancellation so a stopped Worker always persists a partial result before finishing.
- Add process-death recovery:
  - if the app process is killed after Worker completion, rebuild enough result context from `SortResultStore` and `album_summary_history.json`.
- Split `MainActivity` further after this stabilizes:
  - original cleanup state/store
  - background sort coordinator
  - result screen rendering

### Gemini CLI Split

- Good Gemini tasks:
  - read PRD/TODO docs and produce a clean prioritized issue list
  - review place-name policy test cases and suggest missing examples
  - design process-death recovery acceptance tests
  - review Korean UX copy for "safe sorting" and original cleanup
- Keep Codex tasks:
  - Android code edits that touch `MainActivity`
  - device install/logcat/adb verification
  - WorkManager, MediaStore, and permission behavior fixes
  - commits and release build prep
