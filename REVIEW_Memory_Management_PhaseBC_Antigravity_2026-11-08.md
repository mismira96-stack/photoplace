# Phase B/C Independent Code Review: Memory View Manual Place Assignment & Intentional Media Trash

- **Date**: 2026-11-08
- **Reviewer**: Antigravity (Independent Falsification & Architecture Reviewer)
- **Target Request**: [REVIEW_REQUEST_Memory_Management_PhaseBC_2026-11-08.md](file:///C:/work/projects/PhotoPlace/REVIEW_REQUEST_Memory_Management_PhaseBC_2026-11-08.md), [REVIEW_READY.md](file:///C:/work/projects/PhotoPlace/REVIEW_READY.md), [REVIEW_READY_Memory_Management_PhaseBC_2026-11-08.json](file:///C:/work/projects/PhotoPlace/REVIEW_READY_Memory_Management_PhaseBC_2026-11-08.json)
- **Snapshot Hash Verified**: `16c17966ad50135d980c8267227c91c23029300e52d6b3dab183de7d11f90ff1` ([REVIEW_SNAPSHOT_Memory_Management_PhaseBC_2026-11-08.json](file:///C:/work/projects/PhotoPlace/REVIEW_SNAPSHOT_Memory_Management_PhaseBC_2026-11-08.json))
- **Validation Scope**:
  - `release_ready`: `false`
  - `physical_device_validated`: `false` (API 33 emulator + JVM tests verified; real-device validation remains mandatory before production release)
- **Overall Verdict**: **CONDITIONAL APPROVAL (Fix Blocker B-1 & Pre-Release Issues B-2 / B-3 Before Real-Device Rollout)**

---

## 1. Executive Summary

Codex's Phase B/C implementation delivers a remarkably solid, well-isolated architecture for **Manual Place Assignment**, **Album Creation Bridge & Crash Recovery**, **Note-Only Card Retention**, and **Intentional Media Trash (API 30+)**.

1. **Architecture & P0 Gatekeeper (`MainActivity.java` Isolation)**:
   - [`MainActivity.java`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java) was touched only at minimal wiring/delegation points (lifecycle resume hook at [`L369-L381`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L369-L381), sort policy wiring at [`L1908`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L1908), detail button entry at [`L8302`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8302), discovery video policy wiring at [`L8611`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8611), [`L8762`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8762), [`L8779`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8779), and note-only rendering hook at [`L8970-L9004`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8970-L9004)).
   - All new UI and domain state machines live in dedicated classes (`MemoryManagementActivity`, `MemoryManagementController`, `ManualPlaceStore`, `ManualPlaceCopyBridge`, `MemoryTrashExecution`, `MemoryTrashJournal`, `MemoryTrashAuditWorker`, `MediaMutationCoordinator`, `MemoryNoteProjection`).
2. **Why Verdict is Conditional**:
   - During code-level falsification, we identified **1 concrete UI/Action wiring bug (Blocker B-1)** where single-place album creation (`showSingleMemoryOrganizeConfirmation`) still hardcodes `moveVideos = false`, plus **2 pre-release state/threading issues (B-2, B-3)** that should be fixed before real-device testing.

---

## 2. Evaluation of Requested Falsification Points

| # | Review Focus Area (from Review Request) | Status | Verification Evidence |
|---|---|---|---|
| 1 | **P0 Gatekeeper**: Minimal wiring in `MainActivity.java`, no new business logic in `MainActivity` | **PASS** | Verified in [`MainActivity.java`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java). All management UI runs in [`MemoryManagementActivity.java`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MemoryManagementActivity.java). |
| 2 | **Crash Boundary**: `ManualPlaceStore` SQLite assignment / receipt vs `MemoryOrganizationLinkStore` JSON commit | **PASS** | [`ManualPlaceStore.java:78-82`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/ManualPlaceStore.java#L78-L82) uses `PRAGMA synchronous=FULL`. [`ManualPlaceCopyBridge.java:24-48`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/ManualPlaceCopyBridge.java#L24-L48) records `PENDING` before copy, `targetUri` after insert, and `completeCopy` (`ASSIGNED` + `outputPath` + `galleryCopy=true`) in SQLite. Even before [`ManualReceiptWorker.java`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/ManualReceiptWorker.java) replays JSON links, [`ManualPlaceSnapshot.java:63-67`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/ManualPlaceSnapshot.java#L63-L67) injects `"path:" + outputPath -> stableMemoryId` aliases directly from SQLite. |
| 3 | **Logical Date Preservation**: `unknown (0)` vs today's date, `MemoryDependencyGuard` false-positive `DATE_SECTION_CHANGED` prevention | **PASS** | [`ManualPlaceStore.java:268-274, 340-341`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/ManualPlaceStore.java#L268-L274) and [`ManualGalleryPolicy.java:19, 37-40`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/ManualGalleryPolicy.java#L19-L40) preserve `logicalDateEpochMs = 0L` across copy and MediaStore gallery reload. Verified by [`MemoryManagementPolicyTest.java:9-20`](file:///C:/work/projects/PhotoPlace/app/src/test/java/com/mismi/gallerysorter/MemoryManagementPolicyTest.java#L9-L20) and [`probe-dates-flow.log:26-28`](file:///C:/work/projects/PhotoPlace/.debug/manual-place-2026-11-08/probe-dates-flow.log#L26-L28). |
| 4 | **Intentional Trash State Machine**: API 29 guard, pre-verification, 50-item audit checkpoints, `REQUESTED` cancel, `1000+1` batching, `batchUnknown -> NEEDS_ATTENTION` | **PASS** | [`MemoryTrashExecution.java:26-135`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MemoryTrashExecution.java#L26-L135), [`MemoryTrashJournal.java`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MemoryTrashJournal.java), and [`MemoryTrashAuditWorker.java`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MemoryTrashAuditWorker.java) (ordinary `Worker`, no background FGS crash risk). Verified by [`MemoryManagementPolicyTest.java:22-91`](file:///C:/work/projects/PhotoPlace/app/src/test/java/com/mismi/gallerysorter/MemoryManagementPolicyTest.java#L22-L91) and [`probe-boundary.log`](file:///C:/work/projects/PhotoPlace/.debug/manual-place-2026-11-08/probe-boundary.log). |
| 5 | **User-Facing Trash Copy**: Accurate physical file warning without false duplicate-preservation claims | **PASS** | [`MemoryManagementActivity.java:234-243`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MemoryManagementActivity.java#L234-L243) states `"선택한 실제 파일 N개를 기기 휴지통으로 이동해요. 다른 앱에서도 보이지 않게 돼요."` and displays storage/folder paths (`storageSummary`). |
| 6 | **Mutual Exclusion & Main-Thread Safety**: `MediaMutationCoordinator` across Sort / Cleanup / Trash + Manual Video Album Creation | **NEEDS FIX (B-1, B-2, B-3)** | Cross-operation file locking works, but see **Blocker B-1** (single-place manual video wiring), **Issue B-2** (`SortInputStore.write` on main thread), and **Issue B-3** (unassigning/reassigning a `galleryCopy` item). |

---

## 3. Detailed Findings (Must Fix Before Real-Device Rollout)

### [Blocker B-1] Single-place album creation (`showSingleMemoryOrganizeConfirmation`) hardcodes `moveVideos = false`, breaking manual video-only memories in the UI

- **Severity**: **High (Functional Blocker for Manual Video Album Creation from Detail Screen)**
- **Location**:
  - [`MainActivity.java:8675-8700`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8675-L8700) (`showSingleMemoryOrganizeConfirmation`)
  - [`DiscoveryOrganizeConfirmDialog.java:86-90, 115-117`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/DiscoveryOrganizeConfirmDialog.java#L86-L117) (`showSinglePlace`)
- **Code Evidence**:
  In Phase B/C, [`MemoryAlbumAction.canPrepare`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MemoryAlbumAction.java#L13-L30) was updated to allow manual video-only records (`ref.mediaKind == MediaKind.PHOTO || ref.manualAssigned`), and [`MainActivity.java:8611`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8611), [`L8762`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8762), and [`L8779`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8779) were updated to compute `OrganizationMediaPolicy.shouldMoveDiscoveryVideos(shouldMoveVideos(), ...)`.
  However, [`MainActivity.showSingleMemoryOrganizeConfirmation`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8677-L8699) (invoked when clicking `"앨범 만들기"` / `"새 사진 N장 앨범에 추가"` on a single Memory detail screen, or returning from `MemoryManagementActivity` via `EXTRA_ORGANIZE_MEMORY_ID`) still hardcodes `false`:
  ```java
  // MainActivity.java:8677-8699
  if (preparation.actionableCount(false) <= 0) {
      int excludedVideoCount = preparation.excludedVideoCount(false);
      showToast(excludedVideoCount > 0
              ? "Memory 사진은 정리하지 않았어요. 동영상은 Memory 화면 연결을 준비한 뒤 정리할 수 있어요."
              : "새로 만들거나 추가할 위치 사진이 없어요.");
      return;
  }
  new DiscoveryOrganizeConfirmDialog(this).showSinglePlace(
          preparation,
          false,
          () -> startPreparedDiscoveryAlbums(Collections.singletonList(preparation)));
  ```
  And inside [`DiscoveryOrganizeConfirmDialog.java:115`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/DiscoveryOrganizeConfirmDialog.java#L115):
  ```java
  if (preparation.excludedVideoCount(false) > 0) {
      card.addView(buildDetailRow("동영상", "이번 앨범 만들기에서 제외"));
  }
  ```
- **Why Probe Missed It**:
  [`MemoryManagementProbe.flow()`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MemoryManagementProbe.java#L116-L119) called `MemoryAlbumAction.prepare(...)` and `DiscoveryAlbumItemAdapter.toPhotoItems(prep.items, true)` directly, bypassing `MainActivity.showSingleMemoryOrganizeConfirmation(...)`.
- **Failure Scenarios**:
  1. **Manual Video-Only Place**: User assigns 1 or more locationless videos to a manual place and taps `"위치 앨범 만들기"` in `MemoryManagementActivity` (or `"앨범 만들기"` in Memory detail). `preparation.actionableCount(false)` returns `0`, toasts `"Memory 사진은 정리하지 않았어요..."`, and refuses to open the dialog!
  2. **Mixed Manual Photo + Manual Video Place**: Dialog receives `moveVideos = false` and tells the user `"동영상 1개 제외"` (`DiscoveryOrganizeConfirmDialog.java:86, 115`), yet when the user confirms, `startPreparedDiscoveryAlbums` (`MainActivity.java:8762`) computes `moveVideos = true` (when video preference is on) and moves the video anyway!
- **Required Fix**:
  1. In [`MainActivity.showSingleMemoryOrganizeConfirmation`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L8677-L8699), compute `boolean moveVideos = OrganizationMediaPolicy.shouldMoveDiscoveryVideos(shouldMoveVideos(), preparation);` and pass `moveVideos` to `preparation.actionableCount(moveVideos)`, `preparation.excludedVideoCount(moveVideos)`, and `showSinglePlace(preparation, moveVideos, ...)`.
  2. In [`DiscoveryOrganizeConfirmDialog.showSinglePlace`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/DiscoveryOrganizeConfirmDialog.java#L115), check `preparation.excludedVideoCount(moveVideos) > 0` instead of `preparation.excludedVideoCount(false) > 0`.

---

### [Pre-Release Issue B-2] `SortInputStore.write()` runs on the UI thread in `startBackgroundSortWorker()`, executing `FileLock`, journal SHA-256 reads, `WorkManager.get()`, and `fsync` while swallowing conflict messages

- **Severity**: **Medium-High (UI Thread Blocking / Swallowed Mutual-Exclusion Message)**
- **Location**:
  - [`MainActivity.java:1954-1965`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L1954-L1965) (`startBackgroundSortWorker`)
  - [`SortInputStore.java:24-51`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/SortInputStore.java#L24-L51) (`write`)
  - [`MediaMutationCoordinator.java:36-58, 92-104`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MediaMutationCoordinator.java#L36-L104) (`reserve`, `sortWorkActive`)
- **Code Evidence**:
  As noted in Codex's self-observation ([REVIEW_REQUEST_Memory_Management_PhaseBC_2026-11-08.md:76](file:///C:/work/projects/PhotoPlace/REVIEW_REQUEST_Memory_Management_PhaseBC_2026-11-08.md#L76)), `MainActivity.startBackgroundSortWorker()` runs on the main thread and calls `new SortInputStore(this).write(...)`:
  1. `SortInputStore.write()` calls `MediaMutationCoordinator.reserve(context, "sort")`, which acquires an OS `FileLock`, reads `CleanupExecutionJournal` and `MemoryTrashJournal`, calls `WorkManager.getInstance(c).getWorkInfosForUniqueWork(SortWorker.WORK_NAME).get()` (blocking Future on the main thread), and `fsync`s `media_mutation_reservation.json`.
  2. Furthermore, in [`MainActivity.java:1964`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L1964), `catch (Exception unused)` catches the `IOException` thrown by `MediaMutationCoordinator.reserve()` (`"진행 중인 사진 휴지통 이동을 먼저 마치거나 중단해 주세요."` or `"진행 중인 원본 사진 정리를 먼저 마치거나 중단해 주세요."`):
     - In `runDiscoveryAlbumSort()`, `startBackgroundSortWorker` returns `false` and shows generic `"백그라운드 정리를 시작하지 못했어요."` instead of the specific conflict message.
     - Worse, in `runCopy()` ([`MainActivity.java:1931-1934`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MainActivity.java#L1931-L1934)), if `startBackgroundSortWorker` returns `false`, it **falls back to legacy `this.worker.execute(...)`**, bypassing `MediaMutationCoordinator` reservation entirely!
- **Required Fix**:
  - Do not fall back to legacy `this.worker.execute(...)` when `MediaMutationCoordinator` rejects a reservation due to an active trash/cleanup session; surface the specific conflict message to the user.
  - Move the heavy `SortInputStore.write()` JSON serialization + `fsync` off the UI thread (or perform reservation + input persistence asynchronously before enqueuing `SortWorker`).

---

### [Pre-Release Issue B-3] Unassigning or reassigning an already-copied album item (`galleryCopy == true`) in `ManualPlaceStore` leaves the original item's `ManualMediaAssignment` active

- **Severity**: **Medium (State Consistency Edge Case After Album Copy)**
- **Location**:
  - [`ManualPlaceStore.java:258-291, 333-348`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/ManualPlaceStore.java#L258-L348) (`assign`, `unassign`, `completeCopy`)
  - [`EffectiveMediaPlaceResolver.java:53-57`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/EffectiveMediaPlaceResolver.java#L53-L57) (`project`)
- **Code Evidence**:
  When a locationless photo `S` (e.g., in `Download/`) is assigned to Manual Place `P` and organized into `Pictures/P에서/` as copy `C`:
  - `completeCopy()` writes two rows in `manual_assignments`:
    1. Copy `C`: `placeId = P, copyTargetKey = "", galleryCopy = true`
    2. Original `S`: `placeId = P, copyTargetKey = C.assetKey, galleryCopy = false`
  - When `EffectiveMediaPlaceResolver.project()` runs, it hides `S` from Place `P` because `copy != null && copy.placeId.equals(a.placeId)` ([`EffectiveMediaPlaceResolver.java:54-55`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/EffectiveMediaPlaceResolver.java#L54-L55)), displaying only `C` in Place `P`.
  - If the user later opens `MemoryManagementActivity` for Place `P`, selects `C` (which is the URI in `detail.sourceUris`), and taps **"위치 연결 해제" (`unassign`)**:
    - `ManualPlaceStore.unassign()` deletes only `C`'s row (`WHERE asset_key = C.assetKey`).
    - Because `copy` (`C`) is now `null` while `S` still has `placeId = P, copyTargetKey = C.assetKey`, on the very next reload `EffectiveMediaPlaceResolver.project()` sees `copy == null` and **suddenly resurrects the un-trashed original `S` back into Place `P`**!
    - Similarly, if the user reassigns `C` from Place `P` to Place `Q` (`assign`), `C.placeId` becomes `Q` (`!copy.placeId.equals(S.placeId)`), so `S` suddenly reappears in Place `P` while `C` moves to Place `Q`.
- **Required Fix**:
  - In [`ManualPlaceStore.unassign()`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/ManualPlaceStore.java#L283-L288): when deleting `asset_key = K`, also delete (or unassign) any source row where `copy_target_key = K` (and if `K` itself has a non-empty `copy_target_key`, delete that paired row too).
  - In [`ManualPlaceStore.assign()`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/ManualPlaceStore.java#L258-L281): if reassigning a row `K` that is referenced by `copy_target_key = K`, update the source row's `place_id` to the new `placeId` as well (and preserve `gallery_copy` on `K` if it is already in a gallery album folder, or clear the stale link cleanly).

---

### [Minor / Follow-up C-1] Fallback title `"사진 없는 기록"` in `MemoryNoteProjection.retain()` for legacy GPS places

- **Severity**: **Low (UX Polish)**
- **Location**: [`MemoryNoteProjection.java:20-34`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/mismi/gallerysorter/MemoryNoteProjection.java#L20-L34)
- **Observation**:
  For manual places with 0 photos, `EffectiveMediaPlaceResolver.project()` preserves the manual place group with its real place title (`"Note Only Probe"`), which is great. For a GPS-derived memory that loses all photos and is no longer present in `rawSnapshot`, `MemoryNoteProjection.retain()` synthesizes a card titled `"사진 없는 기록"` so date notes are never lost. If `organizedAlbums` or link history has a known place title for that `stableMemoryId`, using that title before falling back to `"사진 없는 기록"` would make the retained note card even clearer.

---

## 4. Real-Device Validation Gate (Required Before `release_ready = true`)

Per [REVIEW_READY.md](file:///C:/work/projects/PhotoPlace/REVIEW_READY.md), after addressing **B-1, B-2, and B-3**, the following physical-device checks remain mandatory before marking the feature release-ready:

1. **API 29 Device / Image**: Verify manual place assignment works end-to-end while intentional trash UI and execution remain cleanly disabled with the guidance message.
2. **Android 14+ (API 34+) Partial Photo Access ("Selected Photos Only")**: Verify permission state transitions during `createTrashRequest` and manual album copy.
3. **Android 15/16 Samsung Galaxy Real Device (One UI Gallery)**:
   - Verify single-place and multi-place manual video + photo album creation (`Pictures/<장소명>에서/<날짜>/`).
   - Verify `createTrashRequest` system dialog approval, rejection, and OS Gallery Trash restore reconciliation (`RESTORED` / `TRASHED` states).
4. **Process Death & Device Reboot**: Verify `MemoryTrashAuditWorker` and `ManualReceiptWorker` resume cleanly without duplicate copies or orphaned reservations.
