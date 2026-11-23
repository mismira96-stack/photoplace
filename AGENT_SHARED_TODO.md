## 최신 2026-11-10 code80: 기억 모으기 복귀 썸네일 placeholder 수정
- 사용자 추가 캡처로 정확한 증상 확인. legacy공유 thumbnail queue backlog제어재현 beforeFAIL/전용loader afterPASS.
- shared cache/닫힌화면취소/lifecycle/Story회귀 및 Debug·Release522 PASS. MainActivity는 delegate wiring만.
- ISSUE_Collection_Back_White_Screen_2026-11-10.md / WORKLOG_2026-11-10.md / REVIEW_READY.md 참고.
- 공식APK Drive 전달 및 사용자 실기기80 수용 대기, production 제출 보류. 아래 초반 ‘원인미확정/화면전체흰색’ 기록은 캡처 전 해석임.

## 최우선 2026-11-10: 기록 모으기 Back 흰 화면 — 출시 보류
- code79 폰 설치 후 신규 신고. ISSUE_Collection_Back_White_Screen_2026-11-10.md / WORKLOG_2026-11-10.md 참조.
- API33/36 system/header/keyboard Back은 native 정상, 원인 미확정. 제품 소스 미수정. USB 미연결 및 상세 경로 답변 대기.
- 실기기 재현/최소 수정/사용자 확인 전 production 제출 금지. 아래 기존 준비 완료 기록으로 출시하면 안 됨.

## 최신 code79 / 1.3.43 — 최종 실기기 확인 대기
- 공유 목록 최신 촬영일순(다른 흐름 불변), 공유 loading 문구, 캡션 중복·폭 정렬 수정.
- 편집시 축척 유지/입력최소16sp/상단사진을 위로 올려키보드위 캡션확보. Done후 정상 card fit.
- Debug/Release522, native1~4장+공유/기존단일회귀 PASS. MainActivity hash동일. REVIEW_READY.md / WORKLOG 마지막 항목 참고.
- 사용자 출시 의도 있으나 수정본 실기기 수용은 미완료. production 제출 전 마지막 사용자 확인 필요.

## 최신 code77 / 1.3.41 폰 설치 완료 — 사용자 확인 대기
- 공유 독립 아이콘/휴지통에는 공유 없음, 한 장 여백 축소, 2/3세로·4장2×2, 카드 직접 캡션/사진·스타일2탭 구현.
- 실제 키보드가 헤더를 밀거나 캡션을 가리지 않도록 전용 inset 처리(native PASS). Debug/Release 각519 PASS.
- 폰 install -r, Play서명/Dex 동일 및 Drive폴더 APK SHA확인. 사용자77 수용 후에만 출시. WORKLOG/REVIEW_READY 최신 항목 참조.

## 최신 code77: 공유는 휴지통 밖 독립 아이콘
- 기존 code76은 실기기 테스트 중. code77 진입분리 구현/native PASS, Instagram 여백 결과 피드백 대기. WORKLOG 마지막 항목 참조.
- 휴지통 화면 내 Instagram 배치는 사용자 거절. 공유 아이콘은 상세 휴지통 옆; 선택 구현만 재사용.

## code76 실기기 후속: 중복 카드 통합 사용자 확인 완료
- 사용자 “합쳐졌네”. code76 install -r 완료/기존 데이터 유지. 뷰어/Instagram 공유 최종 확인 대기, 출시 승인 없음.

## 최신 2026-11-09: code76 사용자 검증 준비
- code75 프로덕션 completed 확인. code76은 별도 테스트, 사용자 승인 전 공개 출시 금지.
- 재설치 중복 카드 연결 / 뷰어 원본 우선 디코드 / 기존 선택 화면 Instagram1~4장 공유 및 투명 카드 PNG 구현.
- Debug/Release 각517, API33 복구·뷰어·선택공유·기존1장 회귀 검증 PASS. MainActivity code75 해시 동일.
- REVIEW_REQUEST_Record_Viewer_Selection_2026-11-09.md 및 WORKLOG 최신 항목 참고. 폰 실제 데이터/Instagram 왕복 수용은 아직 미완료.

## 최신: ANTI Story 복원 최소 수정 code69 설치/사용자 확인 대기 (2026-11-08)

- code68은 거절된 버전. ANTI4파일 완전 복원 검증 후 미리보기 높이/사진 조절/상단 공유 텍스트만 최소 적용.
- 폰69 설치 완료, Debug/Release 각501 및 API33 UI PASS. 실제 폰7개/사용자 수용은 미완료. 출시 금지.
- REVIEW_REQUEST_Story_Restored_Minimal_2026-11-08.md / RELEASE_1.3.33_Device_Validation_2026-11-08.md / WORKLOG 최신 항목 참조.

## 최신 설치: 1.3.30/code66 (2026-11-08 19:57 KST)

- 안티 최종 수정 소스 빌드 후 Play 서명 APK로 폰 업데이트 설치/앱 실행 완료. 기존 데이터 유지.
- Debug/Release 각499개 PASS, 릴리즈 빌드 및 source/dex/서명 검증 통과.
- 기록: RELEASE_1.3.30_Test_2026-11-08.md / WORKLOG_2026-11-08.md 최신 항목.
- production56 유지, internal66 draft. 사용자 기능 테스트 대기, 공개 출시 아님.

## 최신 체크포인트: UI/메모리뷰 후속 작업 중 (2026-11-08 16:07 KST)

- 상세 인계: [HANDOFF_UI_MEMORY_2026-11-08.md](HANDOFF_UI_MEMORY_2026-11-08.md), 최신 진행은 WORKLOG_2026-11-08.md 끝 항목.
- 폰은 code61/1.3.25. 이후 소스 수정은 미설치. Debug/Release 각482개+assembleDebug PASS, 새 UI 렌더링 미검증.
- 우선: 휴지통 후 Back의 메모리뷰 stale 사진 갱신 → 중복 앱 확인창 제거/OS 승인 직접 연결 → 반복 안내 제거 → 기록 내 검색 → R2 비동기 시작/영상단독 문구 → 홈 축약.
- 사용자가 RECORD/SINGLE trash-only로 범위를 좁혔으므로 리뷰 R2-UI-1 메뉴 복원 권고는 자동 적용하지 않음. 독립 재리뷰 시 범위 확인 필요.
- 상태 WORK_IN_PROGRESS / release_ready=false. 아래 과거 READY/미구현/완료 내용은 당시 기록이며 현재 상태는 이 항목과 인계 문서를 따른다.

## 최신: Phase A 안티 A-1/A-2 수정 R1 리뷰 준비 (2026-11-08)

- 먼저 읽기: REVIEW_Memory_Place_PhaseA_Codex_Followup_2026-11-08.md / 표식: REVIEW_READY.md
- 수정: 음수 부가 메타데이터 정규화, inventory 불변 목록/identity 분리, DB scope 밖 key/URI 충돌 검증, SQL/Java 경로 기준 일치.
- 검증: Debug/Release 각453개, assembleDebug, API33 실제3500행/충돌 롤백/재시작/scanner/cache/cancel PASS. MainActivity 동일.
- 읽기 전용 beginTransactionNonExclusive는 API33에서 실제 거부되어 미채택. 전후 revision 검사 유지.
- 다음: 독립 재리뷰 및 Phase B freshness/정리된 복사본 제외/수동 연결·앨범 receipt·노트 전용 기록. 일반 휴지통은 미구현. 전체 기능 완료 아님.

## 최신: 수동 장소/미해결 미디어 Phase A 코드 리뷰 준비 (2026-11-08)

- 표식: REVIEW_READY.md / REVIEW_READY_Memory_Place_PhaseA_2026-11-08.json
- 요청: REVIEW_REQUEST_Memory_Place_PhaseA_2026-11-08.md / 작업 기록: WORKLOG_2026-11-08.md
- 설계 V1.2: 안티 B1~B5 보완 계약, 구현은 Phase A 저장·ID·revision·기존 스캔 보존 기반에 한정.
- 검증: Debug/Release 각446개, assembleDebug, API33 실제 SQLite3500행/재시작/scanner-cache-cancel 통과. MainActivity 해시 무변경.
- 다음: B-1 사용자 장소 연결·투영/앨범 receipt/노트 전용 기록, C 일반 휴지통. 두 기능 전체 완료/출시 준비 아님.
- 사용자는 표식만 요청. 외부 리뷰 메시지/안티 자동 실행/폰 설치/Play 제출 없음.

## 최신 설계: 메모리뷰 휴지통 및 위치 없음 수동 장소 지정 (2026-11-08)

- 설계 초안: DESIGN_Memory_Trash_Manual_Place_2026-11-08.md
- 안티 리뷰 요청: REVIEW_REQUEST_Memory_Trash_Manual_Place_2026-11-08.md
- 작업 기록: WORKLOG_2026-11-08.md
- 상태: 설계만 작성, 구현·설치·출시 없음. 기존 code56 작업 트리 보존.
- 방향: 실제 선택 파일의 일반 휴지통 / GPS 무변경 수동 장소 연결 / 기존 기록 0개 지원 / 기존 앨범 생성 연계.
- 다음: 저장 원자성, 실제 파일 식별, 날짜 메모·ID 보존, 앨범 receipt와 cleanup guard 연동을 우선 설계 리뷰.

## 최신 마무리: code56 승인 및 사용자 E2E 확인 완료 (2026-10-25)

- 사용자 보고로 Google Play 승인 완료 확인.
- 오늘 촬영한 사진의 발견기록 -> 위치앨범 생성 -> 원본정리 전체 흐름 정상 확인.
- 출시 및 사용자 수용 확인 완료. 다음 개선 과제는 기존 후속 TODO로 분리.
- 아래 '검토 중/승인 대기'는 이전 시점 기록임. 최종 결과: RELEASE_1.3.20_2026-10-25.md 하단.

## 최신 상태: code56 프로덕션 출시 제출 (2026-10-25)

- 사용자 최신 명시 지시로 리뷰받은 1.3.20(code56) 출시 진행. 과거 'production51 중지 유지/제출 금지'는 이 새 출시 요청으로 대체됨.
- production code56 전체 출시 설정 validate/commit 성공. 콘솔 실제 상태: 빠른 자동 검사 진행 중 -> 성공 시 자동 심사 전송. 승인 후 자동 게시(관리형 게시 꺼짐).
- **게시 완료로 단정하지 않음.** 자세한 결과/증빙/후속은 RELEASE_1.3.20_2026-10-25.md.
- 소스282파일/기존APK/AAB 해시 일치, tests Debug/Release 각433 및 Release 빌드/lintVital PASS.
- 현재 코드/버전 그대로 제출했으며 추가 수정/버전 증가 없음. 이후 수정은 별도 버전으로 검토한다.
- 다음 후속: 메인 스레드 I/O, enqueue/watch 경쟁과 관찰 전환, 장시간 감사 체크포인트, 취소 알림/초기 검증 문구/이어하기 팝업 UX. 독립 리뷰의 절대 안전 단정은 따르지 않음.

## 최신 인계: PhotoPlace 1.3.20 (code56) 독립 코드 리뷰 완료 - 조건부 프로덕션 배포 승인 (2026-10-25)

- **리뷰 결과 보고서:** [REVIEW_Cleanup_Code56_Antigravity_2026-10-25.md](REVIEW_Cleanup_Code56_Antigravity_2026-10-25.md)
- **최종 판정:** **조건부 프로덕션 출시 승인 (CONDITIONALLY APPROVED FOR PRODUCTION)**
- **P0/P1 검증:**
  - P0 게이트: `MainActivity.java` baseline과 SHA-256(`39ec3c85...`) 100% 일치. 비즈니스 로직 추가 0줄. (PASS)
  - P1 게이트: 무단 삭제/파일 변경 전면 차단, OS `createTrashRequest` 경유, 바이트 SHA-256 대조, 원본 복구 동선 완비. (PASS)
- **테스트 및 빌드:** Debug/Release 각 433개 단위 테스트 전원 통과 (실패 0, 오류 0, 스킵 0). Release 번들/APK 정상.
- **핵심 발견 및 권고 (비차단):**
  - [Pre-release 권고] `prepare()` 및 `recovery()` 내 메인 스레드 `journal.save()`(fsync) 호출을 백그라운드 워커 스레드로 이전 권고 (차기 1.3.21).
  - [Follow-up UX] `PAUSED` 상태 앱 재진입 시 `offerContinue` 반복 팝업을 1회 닫기 후 동일 세션 내 자동 억제 권고.
- **출시 조치:** 프로덕션 출시 트랙 제출 가능.

## 이전 인계: 원본 정리 code56 독립 리뷰 대기 (2026-10-25)

- 사용자: 정리 완료, 코드 리뷰를 거쳐 출시. 지금은 리뷰할 수 있게 자료를 남기는 단계.
- 리뷰 시작: [REVIEW_REQUEST_Cleanup_Code56_2026-10-25.md](REVIEW_REQUEST_Cleanup_Code56_2026-10-25.md).
- 고정 소스: [파일 해시/테스트 manifest](REVIEW_SNAPSHOT_Cleanup_Code56_2026-10-25.json), `.review/cleanup-code56-2026-10-25/source.zip`.
- code56 현 소스 테스트 Debug/Release 각433, 실패0/오류0/스킵0. Release 및 lintVitalRelease PASS. MainActivity 이전 UI baseline 동일.
- 남은 Pre-release 판단 항목을 요청서에 명시했다. 코드 리뷰 전 무조건 출시 승인으로 취급하지 않는다.
- 현재 폰 1.3.20(code56), internal56 draft, production51 halted/code39 completed. 이 인계 중 외부 제출/출시/설치/사진 조작 없음.
- 아래 이전 경과 기록보다 이 최신 인계와 code56 요청서를 우선한다.

> 최신 상태 (2026-10-25, 사용자 중단 요청): 1.3.15(51) 프로덕션 출시 중단 완료. 신규 사용자에게 1.3.9 제공. 이미 설치된 1.3.15는 유지. 사용자 UI 확인 및 명시적 재출시 요청 전까지 재제출/출시 재개 금지.

> 최신 상태 (2026-10-25): 원본 정리 1.3.15(51) 구현·실기기 검증·프로덕션 전체 출시 제출 완료. Google 자동 사전 검사 중이며 심사 승인 후 자동 배포. 실제 검토 3,508장 가능 / 5장 보류, 사용자 원본 이동 0회. 상세: RELEASE_1.3.15_2026-10-25.md / REVIEW_Original_Cleanup_Execution_2026-10-25.md.

# PhotoPlace Agent Shared TODO

Updated: 2026-10-19

This file is the shared handoff note for Codex and Gemini CLI. Keep it short, current, and safe to act on.

## Latest Checkpoint - 2026-10-25 (Critical Code Audit & Edge-Case Vulnerability Analysis)

- CRITICAL CODE AUDIT COMPLETED: Antigravity conducted an in-depth, skeptical code review on WorkManager audit, single-thread polling, and MediaProvider timing.
- Critical Audit Report: REVIEW_Cleanup_Critical_Audit_2026-10-25.md
- **Identified Critical Edge Cases**:
  1. [Critical] Android 12+ FGS Background start restriction in CleanupAuditWorker: setForegroundAsync().get() can throw ForegroundServiceStartNotAllowedException if user backgrounds app after OS trash dialog, causing infinite retry loops. Needs try-catch fallback.
  2. [Major] Single-thread executor saturation by 750ms polling loop in CleanupExecutionController.watchAudit().
  3. [Major] MediaProvider post-trash reindexing delay: 750ms (3 x 250ms) timeout in CleanupAuditWorker is too tight for 300 photos on slow I/O, risking false-alarm NEEDS_RECOVERY.
  4. [Moderate] progress() destroys and recreates full Dialog repeatedly, risking window flicker and BadTokenException.
  5. [Minor] Redundant createNotificationChannel() IPC on every progress tick.

## Checkpoint - 2026-10-25 (WorkManager Background Audit & Resume UX Review - APPROVED FOR HARDENING)

- WORKMANAGER BACKGROUND AUDIT & RESUME UX CODE REVIEW COMPLETED: Antigravity reviewed the separation of post-trash audit to WorkManager, session resume without error toast, and CleanupExecutionScreen full-height progress view.
- Review Report: REVIEW_Cleanup_Background_WorkManager_Audit_2026-10-25.md
- Verdict: **APPROVED FOR CODE HARDENING (HOLD FOR DEVICE VALIDATION)**.
  - P0 Gatekeeper: 100% PASS (MainActivity.java unchanged, 0 business logic lines).
  - P1 Gatekeeper: 100% PASS (Worker is strictly read-only, foreground OS confirmation mandatory, 300-item batch preserved, idempotent state transitions).
  - Tests: All 425 unit tests PASS in Debug & Release (failures: 0, ignored: 0).
  - Release Status: Production rollout remains safely on hold until Android 16 device validation.

## Checkpoint - 2026-10-25 (Production 1.3.15 Code Review Complete - APPROVED)

- PRODUCTION CODE REVIEW COMPLETED: Antigravity conducted comprehensive code review on 1.3.15 (code 51) and the trash execution & recovery pipeline.
- Review Report: REVIEW_Original_Cleanup_Production_Code_Review_2026-10-25.md
- Verdict: **APPROVED FOR PRODUCTION**.
  - P0 Gatekeeper: 100% PASS (MainActivity.java only delegates resume/pause in 2 lines, 0 business logic lines).
  - P1 Gatekeeper: 100% PASS (Read-only projection, zero hard deletes, OS trash dialog, B1 audit, fsynced atomic journal, rollback recovery).
  - Device Validation: 3,508 eligible / 5 held on SM_F971N.
  - Tests: 412 unit tests PASS in Debug & Release.
  - Production Status: Submitted to Google Play Production track, awaiting automated checks & auto-release.

## Checkpoint - 2026-10-25 (Architecture Redesign for Step Flow & Item-level Isolation)

- STEP-BY-STEP FLOW SCREEN & ITEM-LEVEL ISOLATION REDESIGN: Antigravity reviewed user feedback on popup number confusion ("3,513쌍", "3,778쌍", "모음·대표사진 연결 확인 필요 3,778쌍", "전체 보류 사유를 개별 문제처럼 표시").
- Full Architecture Design Document: `DESIGN_Original_Cleanup_Step_Flow_2026-10-25.md`
- Collaborative Review Document: `REVIEW_Original_Cleanup_Step_Flow_Design_2026-10-25.md`
- **Key Architectural Decisions agreed with Codex**:
  1. **사진 중복 집계 제거 (Unique Source Deduplication)**: Group pairs by `sourceUri`. Count and display unique photos ("고유 원본 N장") instead of confusing candidate pairs.
  2. **개별 사진 단위 의존성 검사 및 보류 격리 (Item-level Dependency Guard & Isolation)**: Eliminate whole-set (Set-wide) single-failure rejection in `MemoryDependencyGuard`. Partition items into `ELIGIBLE` (clean duplicates, ~3,710 photos) vs `HELD` (specific photos with cover/memory links, ~3 photos).
  3. **팝업 폐기 -> 단계별 진행 화면 (Step-by-Step Flow Screen)**: Replace `AlertDialog` with dedicated full-flow view (`CleanupStepFlowRenderer` / `Presenter`): Step 1 (Scan & SHA-256 analysis), Step 2 (Review & Safe separation with tabs: 정리 가능 / 보류됨), Step 3 (System trash & Post-trash audit).
  4. **진입 동선 2개 확정**: 1) 위치 앨범 상단 '원본 정리' 헤더 버튼, 2) 앨범 생성 완료 화면 보조 버튼.
  5. **P0/P1 Gates**: `MainActivity.java` strictly 0 business logic lines (all UI/calculations isolated in dedicated renderers/resolvers). MediaStore read-only projection + OS trash dialog maintained.

## Checkpoint - 2026-10-25 (Code 44)

- ORIGINAL CLEANUP "0장" ROOT CAUSES IDENTIFIED & FIXED IN 1.3.12 (code 44): Antigravity investigated user feedback ("으 아직도 0장나옴 ㅋㅋ 뭐하냐 ㅋ"). Full report saved to `REVIEW_Original_Cleanup_Phase3_Followup_2026-10-25.md`.
- Status: **ROOT CAUSES RESOLVED, ALL UNIT TESTS PASS (382 tests), PLAY-SIGNED APK 1.3.12 (code 44) READY FOR USER PHONE**.
  - **Root Cause 1 (MemoryDependencyGuard rejection)**: Candidate photos outside Discovery memory groups had `ids == null` and failed evaluation. Fixed and verified in `MemoryDependencyGuardTest`.
  - **Root Cause 2 (ContentResolver SQL query unparameterized Korean strings)**: `CleanupHistoricalScanner` queried `"relative_path LIKE 'Pictures/%에서/'"` with raw string literals instead of parameterized `selectionArgs`. Replaced with parameterized `selectionArgs = new String[]{"Pictures/%에서/", "Pictures/%에서", "Pictures/%에서/%"}`.
  - **Root Cause 3 (Premature cancellation race in CleanupController)**: `coordinator.evaluate(..., () -> !progress.isShowing())` evaluated `!progress.isShowing()` on background thread before window layout finished. Replaced with explicit `AtomicBoolean canceled`.
  - **P0/P1 Gates Preserved**: `MainActivity.java` 0 business logic lines (0 lines added/modified). Full SHA-256 byte verification and read-only projection maintained.
  - **Build & Artifact**: Bumped versionCode to 44. Built `bundleRelease`, uploaded to Play internal track draft, downloaded and verified official Play-signed APK `photoplace-1.3.12-code44-play-signed.apk` (cert `b9f9d963...`). Installed and verified on `emulator-5580`. Ready for real-phone testing.

## Checkpoint - 2026-10-25 (Code 43)

- MULTI-SOURCE ORIGINAL DUPLICATE DISCOVERY & 1.3.12 (code 43) FIX: Antigravity investigated user feedback ("야 뭐하나 제대로 된게없노..ㅠ 없다고 나온다," "원본사진보관소가, 여기저기있을텐데,"). Full report saved to `REVIEW_Original_Cleanup_MultiSource_Antigravity_2026-10-24.md`.
- Status: **DISCOVERY FIX COMPLETED, 381 TESTS PASS (Debug & Release), PLAY-SIGNED APK 1.3.12 (code 43) READY**.
  - Root Cause Diagnosed: MediaStore `DISTINCT relative_path` query crashed with `IllegalArgumentException`, `album_history.json` was missing, and scanner was restricted to `DCIM/%`. Real device audit revealed 3,513 duplicates (3,408 in `Download/`, 98 in `DCIM/Camera/`, 7 in `Download/Quick Share/`).
  - Fixed `CleanupHistoricalScanner`: Aggregates all location albums from `AlbumSummaryHistoryStore`, `SortResultStore`, `MemoryOrganizationLinkStore`, legacy files, and valid MediaStore queries. Scans all non-album image directories (`Download/`, `DCIM/Camera/`, custom folders) outside target albums.
  - Bounded 100-item review batch: Matches `CleanupTrashDispatcher.MAX_BATCH_SIZE = 100` and Binder transaction limit. Review evaluation takes < 0.5s.
  - Improved `CleanupController`: Added explicit "취소" button and UI thread progress throttling.
  - Unit tests: Added `CleanupHistoricalScannerTest.java` (path normalization and non-target folder preservation). 381 tests PASS.
  - P0/P1 preserved: `MainActivity.java` unchanged (0 lines).
  - Built `photoplace-1.3.12-code43-test.aab`, uploaded to Play Console internal track, and downloaded official Play-signed APK `photoplace-1.3.12-code43-play-signed.apk` (cert `b9f9d963...`). Ready to install on user's phone!

## Checkpoint - 2026-10-24

- PHASE 3 IMPLEMENTATION COMPLETE (Original Cleanup Phase 3 - Trash Dispatcher, Post-Trash Auditor, Review UI & Historical Scanner): Antigravity finished the full end-to-end implementation per user request. Full report saved to `REVIEW_Original_Cleanup_Phase3_Antigravity_2026-10-24.md`.
- Status: **PHASE 3 COMPLETED, 379 TESTS PASS (Debug & Release), APK 1.3.12 (code 42) BUILT & INSTALLED ON TEST EMULATOR**.
  - Built `CleanupEvidenceStore` (Schema 3: added `cleanup_requests` journal table with `recordJournal`, `updateJournalStatus`, `readJournal`).
  - Built `CleanupHistoricalScanner`: Discovers historical camera photos duplicated into known PhotoPlace location albums for safe user cleanup review.
  - Built `CleanupTrashDispatcher`: Isolated `MediaStore.createTrashRequest` caller and 100-item batch splitter. `LegacyCleanupIsolationTest` byte-code symbol check 100% PASS.
  - Built `CleanupPostTrashAuditor`: Off-thread B1 post-trash audit verifying destination survival and full SHA-256 byte match.
  - Built `CleanupReviewDialog`: Interactive review UI displaying verified candidate photos, album destinations, recoverable storage, and explicit selection.
  - Built `CleanupController` & updated `CleanupFeatureGate` (enabled = true).
  - P0 preserved: `MainActivity.java` contains 0 business logic lines (lifecycle delegation only).
  - Built `app\build\outputs\apk\release\app-release.apk` (code 42 / 1.3.12) and `photoplace-1.3.12-code42-test.aab`. Tested & running on emulator-5580. Ready for user phone test!

- PHASE 2 IMPLEMENTATION & HANDOFF (Original Cleanup Phase 2 - Target Verification, MemoryDependencyGuard & Review Coordinator): Antigravity took over implementation upon Codex token exhaustion. Full report saved to `REVIEW_Original_Cleanup_Phase2_Antigravity_2026-10-24.md`.
- Status: **PHASE 2 COMPLETED & READY FOR CODEX PEER REVIEW**. P0 (MainActivity 0-line rule strictly preserved, 0 lines modified), P1 (100% read-only projection, zero file mutations, trash gate remains false), 375 tests PASS (0 failures, 0 errors, 0 skipped, +24 new unit tests).
  - Built `CleanupPairVerifier` (off-thread full-stream SHA-256 byte comparison with TOCTOU mutation detection).
  - Built `MemoryDependencyGuard` (set-wide projection check preserving stable IDs, date sections, date notes, and collection memberships).
  - Built `CleanupCandidate`, `CleanupReviewAssessment`, and `CleanupReviewCoordinator` (two-tier verification orchestrator with progress callback and cached check reuse).
  - Built `CleanupMemorySnapshotReader` (strict primary JSON reader) adapting to `CleanupReviewCoordinator.SnapshotProvider`.
  - Updated `CleanupEvidenceStore` (Schema 2 with `cleanup_content_checks` table, `readCandidates` and `readCheck`) adapting to `CleanupReviewCoordinator.EvidenceLedger`.
  - Added unit test suite: `CleanupPairVerifierTest` (10 tests), `MemoryDependencyGuardTest` (9 tests), and `CleanupReviewCoordinatorTest` (5 tests).
  - Device kept safe: User disconnected phone for tonight. Ready for Codex to review in the morning and proceed to Phase 3 (Review Screen UI).

- EXTERNAL CODE REVIEW COMPLETE (Album Flow Real-Device Follow-up: Button Persistence, Scroll Reset & Count Icons): Antigravity reviewed `REVIEW_Album_Flow_Followup_2026-10-24.md` and `ISSUES_Album_Flow_2026-10-24.md`. Full report saved to `REVIEW_Album_Flow_Followup_Antigravity_2026-10-24.md`.
- Verdict: **APPROVED for merge (코드 반영 승인)**. P0 (MainActivity 0-line business logic, 7-line pure UI routing helper `openCompletedAlbums()`), P1 (data read-only, no MediaStore writes, trash disabled), 351 tests PASS (0 failures, 0 errors, 0 skipped).
  - Issue B resolved: `MemoryAlbumAction.canPrepare(record, resolvedRefs)` hides addition button on detail view when all original photos are already copied.
  - Issue C resolved: completion view "새로 만든 앨범 보기" resets `recentPlacesScrollY = 0` and search filters to reveal the latest album at top.
  - Issue D resolved: `MemoryCardCountRenderer` restores `[photo icon] 996장 · [video icon] 12개` with full accessibility text.
  - Issue A tracked: Cheongju/Seocho-dong/Yongin button absence held in investigation without hasty changes.
  - Ready for device test of 1.3.11 (41) and subsequent **Original Cleanup Phase 2**.

- EXTERNAL CODE REVIEW COMPLETE (Original Cleanup Phase 1 - Evidence Store & Discovery Count Presentation): Antigravity reviewed `REVIEW_REQUEST_Original_Cleanup_Phase1_2026-10-24.md` and `REVIEW_Original_Cleanup_Phase1_2026-10-24.md`. Full report saved to `REVIEW_Original_Cleanup_Phase1_Antigravity_2026-10-24.md`.
- Verdict: **APPROVED for merge (코드 반영 승인)**. P0 (0 lines added/modified in MainActivity, SHA-256 verified), P1 (evidence decoupled from copy failure, all records UNVERIFIED & content_sha256 NULL, trash gate permanently false), 346 tests PASS (0 failures, 0 errors, 0 skipped).
  - Built `CleanupEvidenceRecord`, `CleanupEvidenceSession`, `CleanupEvidenceStore`, and `MediaCopyReceipt`.
  - Batch checkpointing: 50 items or 5s synchronous interval; failure safely disables evidence session without impacting photo copying or worker retry.
  - Discovery count clarified: "사진 996장 · 동영상 12개" / legacy fallback "사진·동영상 121개 (이전 기록)" without guessing.
  - Ready for Codex to commence **Phase 2: Target Verification & MemoryDependencyGuard (해시 검증 및 의존성 가드)**.

- EXTERNAL CODE REVIEW COMPLETE (Original Cleanup Phase 0 - Legacy Cleanup Neutralization & Feature Gate Isolation): Antigravity reviewed `REVIEW_REQUEST_Original_Cleanup_Phase0_2026-10-24.md` and `REVIEW_Original_Cleanup_Phase0_2026-10-24.md`. Full report saved to `REVIEW_Original_Cleanup_Phase0_Antigravity_2026-10-24.md`.
- Verdict: **APPROVED for merge (코드 반영 승인)**. P0 (0 lines added to MainActivity, net -348 lines for Phase 0, net -656 lines vs HEAD), P1 (media read-only, trash completely disabled, album duplicate detection preserved), 336 tests PASS (0 failures, 0 errors, 0 skipped).
  - Legacy sinks (`createTrashRequest`, `createDeleteRequest`, direct `is_trashed` column mutation) 100% physically eliminated from `MainActivity.java` and verified by bytecode Constant Pool parser in `LegacyCleanupIsolationTest`.
  - Delegation to `CleanupFeatureGate` for disabled notice; actionable pending disabled.
  - Ready for Codex to commence **Phase 1: Evidence Store (증거 원장 구축)**.

- DESIGN REVIEW COMPLETE: Original Cleanup Design V2 (`ORIGINAL_CLEANUP_DESIGN_V2_2026-10-24.md`). Full report saved to `REVIEW_Original_Cleanup_Design_V2_Antigravity_2026-10-24.md`.
- Verdict: **DESIGN APPROVED (설계 최종 승인)**. 3 Blockers (B1 Post-trash audit, B2 MemoryDependencyGuard set-wide projection, B3 Phase 0 legacy trash gate) fully resolved. Phase 0 implementation approved.

- DESIGN REVIEW COMPLETE: Original Cleanup Design (`ORIGINAL_CLEANUP_DESIGN_2026-10-24.md`). Full report saved to `REVIEW_Original_Cleanup_Design_Antigravity_2026-10-24.md`.
- Verdict: **CONDITIONAL DESIGN APPROVAL (설계 조건부 승인)**. P0/P1 compliant. Answered 6 design questions. Identified 3 Blockers (post-trash destination survival audit, Memory/DateNote dependency verification guard, legacy MainActivity trash code neutralization) before Phase 1 implementation.

- EXTERNAL REVIEW COMPLETE (331-test MIXED album addition & working progress banner patch): Antigravity reviewed `REVIEW_REQUEST_Mixed_Album_Progress_2026-10-24.md` and `WORKLOG_2026-10-24.md`. Full report saved to `REVIEW_Mixed_Album_Progress_Antigravity_2026-10-24.md`.
- Verdict: **APPROVED for merge (코드 반영 승인)**. P0 (0 lines added to MainActivity, net -308 lines vs HEAD), P1 (data immutable, user confirmed album addition), 331 tests PASS (0 failures, 0 errors).
  - Patch A: MemoryAlbumAction fixes MIXED single-place addition, enforces existing Pictures/ path and prevents folder creation/tampering.
  - Patch B: WorkingProgressBanner & WorkingProgressPresentation dynamic banner reveals progress in detail view upon job start without MainActivity clutter.
  - Device verification: Play-signed code 39 installed; user confirmed Seongnam album addition (373 -> 2,266 photos). Visual check of progress bar progression on code 39 pending next run.

## Checkpoint - 2026-10-19

- LATEST316 BACK ANCHOR: MemoryScrollAnchor/MemoryBrowserScrollController preserve selected place/collection card offset across next matching browser bind, including NEW reorder. No old root/bitmap retention; WeakReference active scroll. MainActivity binding only.316tests/build PASS; device Yongin title Y1152 before/after, column changed due NEW. Collection/search manual follow-up stopped when phone switched to user's other app.7protectedJSON hashes same. Back-scroll APK installed/Drive SHA22F58FDB2E86DF741A18F872C0E53DB6C7D527B87417E152C6E082744F40CF3A. Report REVIEW_Back_Scroll_Restore_2026-10-19.md.
- Gemini5proposal assessed in REVIEW_Performance_Proposal_Assessment_2026-10-19.md (official Android refs). Actual device5015refs/photoIDs4805/video210 ->11 LiveFilter batchqueries, NOT thousands. No stage timing proves600~700ms; generation+version exists for whole-screen warm reuse, cold filter skip needs persisted validated output+scope/success guards. Prefetch needs no-UI prepare/coalescing and first-frame protection. RGB565/downsample already fallback. Recommend stabilize current release then stage measurement/filter reuse/prefetch; no new implementation from proposal review alone.

- LATEST312 LOADING LAYOUT: unfolded notice previously used full window +90dp fixed bottom while body/tabs centered600dp. Now MemoryLoadingNoticeLayout positions width within viewport centered600dp/18dp sides, above actual ScrollView bottom by12dp; controller follows parent/viewport layout and cleans listeners. MainActivity no additional edits, data7JSON unchanged.312tests/debugbuild PASS, unfolded2448x1848 screenshot visually checked; folded hardware/splitwindow/largefont pending. Latest installed/Drive loading-layout APK SHA D5FA2FE858FC9B9C2094E070D42738717C7DBEB385C8569A146B12DE5913080F. REVIEW_Loading_Notice_Layout_2026-10-19.md includes external review request. No source performance change.

- EXTERNAL REVIEW COMPLETE (309-test discovery counts, seen feedback & delayed notice): Antigravity reviewed `REVIEW_Discovery_Counts_Seen_Feedback_2026-10-19.md`. Full report saved to `REVIEW_Discovery_Counts_Seen_Feedback_Antigravity_2026-10-19.md`.
- Verdict: **APPROVED for merge (코드 반영 승인)**. P0 (0 business lines in MainActivity, net -260 lines), P1 (7 JSON hashes verified, prefs separated), 309 tests PASS. Seongnam count discrepancy completely eliminated (card and detail both 2,252 photos + 31 videos). Seen version persistence and 250ms delayed notice verified. Remaining: real-device GPS photo test for NEW re-appearance.

- LATEST309: discovered/MIXED cards now use actual resolver counts, shared Gallery reads with collection summary; prepared counts/NEW survive search filtering. Pure legacy album-only estimate retained to avoid scanning unrelated albums. Device Seongnam2252photos+31videos / Suwon854+11 / Songpa339+7 confirmed. User selected NEW cleared after successful detail; MemorySeenStore independent prefs by discoveryGroup key/version, no source-cache invalidation. Songpa NEW/+288 gone after restart/search, other unread badges remain. Delay notice250ms; cold Songpa506ms still legitimately shows loading.309tests/debugbuild/diffcheck PASS. Counts-before/after7JSON hashes unchanged. Latest installed/Drive counts-seen-feedback APK SHA894FAD6975C1C62019F78F116C5A59F4AFC7280019277F647F0F4FE17D95BA65. REVIEW_Discovery_Counts_Seen_Feedback_2026-10-19.md includes full report/review request. Real newGPS reappearance and external review pending.

- EXTERNAL REVIEW COMPLETE (300-test device performance & snapshot cache patch): Antigravity reviewed `REVIEW_REQUEST_Device_Discovery_Performance_2026-10-19.md` and `REVIEW_Device_Discovery_Performance_2026-10-19.md`. Full report saved to `REVIEW_Device_Discovery_Performance_Antigravity_2026-10-19.md`.
- Verdict: **APPROVED for merge (코드 반영 승인)**. P0 (0 lines added to MainActivity, net -263 lines), P1 (data immutable, 7 JSON hashes verified), 300 tests PASS. Seongnam 176-count discrepancy (card 2,107 vs detail 2,283) confirmed as Pre-release diagnostic follow-up; cold-start 944ms source load tracked as Follow-up. Distinct from prior 290-test approval.

- LATEST DEVICE VALIDATION: phone connected, prior installed APK matched21:59. Data-preserving final update installed, local/Drive/pulled APK SHA256 AFDF0DF39C0F61793680476594325EAE2381C074BC7C97DE020DC85F9FF63F54. Home left open. Detail warm2ms/Back6ms confirmed; grid and viewer tap both open Samsung MoviePlayer, swipe alone doesn't. Protected7JSON hashes unchanged. Added exact-content SnapshotDecodeCache+16K char reads,300tests/debugbuild PASS. Firsttab944ms remains; Seongnam card2107 vs actual detail2283 reproduced and OPEN. Reports/request REVIEW_Device_Discovery_Performance_2026-10-19.md / REVIEW_REQUEST_Device_Discovery_Performance_2026-10-19.md. Previous Antigravity290-test conditional approval not coverage for this new change; no0ms/UI-freeze-eliminated claim.

- LATEST VIDEO TAP: user confirmed same-file inconsistent playback corresponded to grid direct external open vs next/swipe into viewer's inert Gallery overlay. Overlay now explicitly tappable to same Host.openExternal(current item), tiny movement only; swiping never auto-launches player. MemoryViewerVideoAction pure policy +3tests; MainActivity no additional edits. **293 tests/debug build PASS**. REVIEW_Viewer_Video_Tap_2026-10-19.md includes external review request. Device gesture/player/back verification pending; no in-app video player added. Includes multi-place cache patch.

- EXTERNAL REVIEW COMPLETE (290-test patch): Antigravity reviewed `REVIEW_REQUEST_Discovery_Multi_Place_Cache_2026-10-19.md`, `REVIEW_REQUEST_Discovery_Tab_Load_2026-10-19.md`, and `REVIEW_REQUEST_Memory_Detail_Entry_2026-10-19.md`. Full reports saved to `REVIEW_Discovery_Tab_Load_Antigravity_2026-10-19.md` and `REVIEW_Memory_Detail_Entry_Antigravity_2026-10-19.md`.
- Verdict: **APPROVED for merge (코드 반영 승인)**. MainActivity net -263 lines (+152/-415), P0/P1 pass, 290 tests PASS.

- LATEST MULTI-PLACE CACHE: user confirms viewer Back better, Seongnam/Suwon still show loading and reports converting their albums to memories (physical operation unverified). Single-detail cache caused alternating-place misses. Detail LRU now3places/10,000 URI refs; changed/unknown revision drops all old entries. Browser keeps default1entry. No new MainActivity edits. **290 tests/debug build PASS**. REVIEW_Discovery_Multi_Place_Cache_2026-10-19.md and REVIEW_REQUEST_Discovery_Multi_Place_Cache_2026-10-19.md. First detail still builds full repository/live-filter/records; UI binding and device timing remain OPEN. Do not claim fully resolved.

- LATEST DISCOVERY TAB FIX: worker prepares one repository+records/state+collection summaries, renderer only binds prepared data, shared MemoryScreenLoadController handles revision cache/origin/lifecycle. Pending Back cancels entry and even visible Home re-selection cancels old request via MemoryLoadNavigation. **284 tests PASS**, debug build PASS. No device measurement yet.
- Drive `PhotoPlace-1.3.6-discovery-tab-20261019-debug.apk` created21:44:59. Current report/request REVIEW_Discovery_Tab_Load_2026-10-19.md / REVIEW_REQUEST_Discovery_Tab_Load_2026-10-19.md. New refactor not covered by prior approval. Source may still take time cold; UI bind/all-place card creation and collection detail remain separate.

- USER TESTED detail-entry: bottom loading notice appears, experience somewhat better because progress is visible; Back still slightly slow. Do not equate this with measured source-time improvement or complete fix. Need returnReused/check/reattach and detail source/uiWait/bind logs before further targeted changes. No new production patch from this feedback alone.

- LATEST CLARIFICATION: Seongnam slow path is **place card→date grid**, user reports2,107items, NOT viewer Back. Added MemoryDetailLoadController/Session for off-UI source resolution, latest-origin guarded UI callbacks, one stable key/scope/revision detail cache, unavailable result non-cacheable; reused one date formatter per section build. MainActivity load/bind/lifecycle only. **276 tests/assembleDebug/diff check PASS**.
- Drive APK `PhotoPlace-1.3.6-detail-entry-20261019-debug.apk` created21:30:28 KST (different from09:30smooth). Reports/request REVIEW_Memory_Detail_Entry_2026-10-19.md / REVIEW_REQUEST_Memory_Detail_Entry_2026-10-19.md. External review/device timings pending. Cold source cost remains; warm reuse/UI responsiveness are intended. Date-note/header I/O and collection source remain limitations.

- USER STILL REPORTS SEONGNAM SLOW ON NEW VERSION after smooth/backfix. Not resolved. Need exact slow segment (place→grid / image load-navigation / viewer Back) and real-device logs. adb currently has0devices. Rechecked beginDetail/detailBound wiring in memory/collection detail. No speculative production changes/new APK after this report yet.

- LATEST VIEWER FLASH FOLLOW-UP: user says Back improved, first Next/Next flashes. Cache miss used to replace current bitmap with preview/placeholder. ViewerImageTransition now keeps current bitmap until next ready, keeps preview for first entry and clears old photo on failed next load. No new MainActivity changes. **269 tests/assembleDebug PASS**. Drive `PhotoPlace-1.3.6-viewer-smooth-20261019-debug.apk` created09:30:19, includes Back fix. Reports/request: REVIEW_Viewer_Image_Transition_2026-10-19.md / REVIEW_REQUEST_Viewer_Image_Transition_2026-10-19.md. External approval/device verification pending.

- LATEST BACK FIX (after external260-test approval): MemoryViewerReturnController/ViewerReturnState reattach exact prior detail only when stable render-time source revision still matches. Capture preserves root/scroll/loaded pages; pause/config/destroy invalidate. Source changes/unknown/working fall back to existing rebuild. MainActivity lifecycle/binding only. **265 tests/assembleDebug PASS**, no phone verification yet.
- New Drive file created09:19:30: `Codex APK/PhotoPlace-1.3.6-viewer-backfix-20261019-debug.apk` (preserves previous09:09file). Report `REVIEW_Viewer_Back_Return_2026-10-19.md`; external request `REVIEW_REQUEST_Viewer_Back_Return_2026-10-19.md`. Do not treat prior approval as this patch's approval.

- Antigravity result acknowledged and checked: `REVIEW_Memory_Count_Viewer_Followup_2026-10-19.md`. Preserve approval as external verdict, but not whole-app count resolution; report overstates unconditional home/detail equality, fallback 2048/RGB_565 and newly clean lint. Still uninstalled/device checks pending; user reported second GPS photo discovery succeeded, not all NEW/repeat-zero steps.

- EXTERNAL REVIEW COMPLETE: Antigravity reviewed `REVIEW_REQUEST_Memory_Count_Viewer_2026-10-19.md`. Report saved to `REVIEW_Memory_Count_Viewer_2026-10-19.md`.
- Verdict: **APPROVED for merge / CONDITIONAL RELEASE APPROVAL**.
  - P0 Gate: MainActivity net -301 lines (+81/-382), zero business/decode logic added. PASS.
  - P1 Gate: MediaStore read-only, trash blocked, user metadata preserved. PASS.
  - 260 unit tests PASS (0 failures, 0 errors), `git diff --check` PASS.
  - Pre-release check: real-device viewer swiping responsiveness + Seongnam GPS photo verification.

- VIEWER FOLLOW-UP: dedicated MemoryViewerImageLoader worker, same-URI cached preview, 16MiB viewer cache, generation cancellation/stale completion rejection, clear on video/detach/close. MainActivity wiring only. **260 tests/debug build PASS**, APK NOT installed. See `REVIEW_Memory_Viewer_Performance_2026-10-19.md`.
- This fixes possible viewer image queue starvation behind grid thumbnails. Place→detail and viewer Back→detail synchronous queries/view rebuilding remain separate. Await user clarification of slow segment; no measured device speedup or external approval claimed.

- NEW USER COUNT REPORT: investigation + first display fix recorded in `REVIEW_Memory_Count_Consistency_2026-10-19.md`. **256 tests and assembleDebug PASS**, APK NOT installed (phone disconnected). Do not infer the prior approval covers this patch.
- Resolved detail headers now count the actual date sections; discovery-only cards/home share photo/video-aware counts; duplicate URI sections, stale and empty refs are excluded. Unknown Gallery read != confirmed empty. No new MainActivity changes in this count patch.
- Still OPEN before release: discovery-only vs album-included source scope differs by screen/key, repository MIXED aggregate uses Math.max (not union), and resolver filename-only cross-source dedupe may omit distinct same-name files. Do not claim whole-app counts fixed. See report for exact evidence/tests/handoff.

- Codex overnight follow-up COMPLETE: `REVIEW_PreRelease_Verification_2026-10-19.md`, linked from `REVIEW_INDEX_2026-10-19.md`. Preserve Antigravity original separately; corrections to review claims are in the follow-up.
- Added 4 failure/recovery/policy regression tests; **251 tests PASS**. No additional production Java changes. Debug build and diff check PASS.
- **lintDebug FAIL: 179 errors / 59 warnings**. All errors are MainActivity WrongConstant; all 179 error source statements also exist in HEAD (not a separately linted HEAD build). Do not describe all static checks as passing. Existing debt documented, no suppression/baseline added.
- Current device showed Seongnam NEW; Hanam Discovery 6-photo date grid and internal viewer opened. Separate 105-photo location-album screen uses external chooser; do not conflate these UI paths. Fresh GPS-photo end-to-end test remains OPEN.

- Antigravity comprehensive review COMPLETED for `REVIEW_REQUEST_Incremental_Analysis_2026-10-18.md` and `REVIEW_REQUEST_Home_Cache_2026-10-18.md`. Full report saved to `REVIEW_Incremental_Analysis_Home_Cache_2026-10-19.md`.
- Verdict: **CONDITIONAL RELEASE APPROVAL**.
  - P0 Gate: `MainActivity.java` net -291 lines (+71/-362), zero business/decision logic. PASS.
  - P1 Gate: `MediaStore` read-only, trash permanently disabled, user metadata/notes preserved. PASS.
  - 247 unit tests PASS (0 failures), `git diff --check` PASS.
  - Pre-release check remaining: Real-device verification of Seongnam `NEW` badge with a GPS-tagged photo.
- Antigravity is monitoring for next review requests. When ready, Codex can add a new `REVIEW_REQUEST_*.md` file.

## Latest Checkpoint - 2026-10-18

- LATEST Home follow-up: cached summary first + unchanged query skip implemented outside MainActivity; **247 tests PASS**, debug installed. Current Home review: `REVIEW_REQUEST_Home_Cache_2026-10-18.md` (not covered by prior phase1 approval).
- Home cache ~1.4KB, only canonical destination/title/cover/count/country summaries. Fresh detail resolution retained. Warm Back pre-draw 26–36ms vs uncached 1,263ms; cold cached pre-draw 111–319ms, Activity launch separately 261–976ms. Logs distinguish timing boundaries. Root view reuse is still deferred.
- Overall MainActivity current diff +71/-362, net -291 vs HEAD. Home load/cache/country card logic lives in dedicated classes. Data files/MediaStore untouched apart from disposable app caches and normal analysis snapshots.

- NEW latest follow-up: phase1 approval relayed by user covers 226-test patch only. Current incremental-analysis/MIXED NEW/lifecycle follow-up has **237 tests PASS** and its own `REVIEW_REQUEST_Incremental_Analysis_2026-10-18.md`; do not treat it as already reviewed.
- Both photos/videos now reuse fingerprinted analysis while reconciling the full lightweight inventory. Real new-photo device test: 3,218 hits / 1 analyzed, ~2.4s total. Cached inventory-only reconciliation ~1.5s; initial video cache fill ~8.3s. Original trash and media unchanged.
- MIXED NEW suppression removed with tests; wording now says places receiving new media. New test photos had no GPS, so GPS-bearing NEW device check is still pending. Two earlier 'new place again' runs actually added 1 then 4 source items.
- OPEN user report: Home entry feels slow. Home lookup/UI restoration is unchanged and must be profiled separately; snapshot-first Home display is not implemented.

- Source repository: `C:\work\projects\PhotoPlace`. The OneDrive Photo Place folder is attachments only.
- Read the 2026-10-18 section at the top of `TODO.md` and `WORKLOG_2026-10-18.md` for the current performance priority; dated sections below are historical.
- Added `PreviewRefreshGate` / `PreviewRefreshController`: persist a checkpoint only after a successful stable complete scan; skip analysis when MediaStore version/generation, folders, permissions, policy, item count and snapshot revision still match. API 29/unknown/limited access uses the normal scan path.
- Device full scan 3,213 items took 23.7s; unchanged preflight 16–31ms including after process restart. This is preflight timing, not app startup or total UI latency. Changed media still triggers the existing full scan in this first phase.
- Completion with zero new places and unchanged checks use a quiet inline notice, with no blocking popup.
- Fixed the legacy `하남에서` 105-photo card: existing organizedAlbum path is readable inside the app even without an OrganizationLink. Exact links retain priority; no alias backfill or media mutations. Date grid and internal viewer confirmed on device.
- 226 unit tests and debug build pass; installed with app data retained. Notes/collections/identity/links/personalization/history files matched the pre-test backup byte-for-byte after the initial patch.
- Follow-up: preflight now preserves existing views, progress starts only for an actual scan, pending taps coalesce, inline notices are reused. User confirmed flicker gone; latest build adds subtle button feedback and a brief checking state.
- OPEN user report: after adding home photos, Seongnam has no NEW badge and may be reported as new again on another analysis. Do not mark this fixed. See `REVIEW_REQUEST_Refresh_Performance_2026-10-18.md` for Antigravity working-tree review and limitations.
- Next: cached Home display first, then incremental added/modified analysis + deletion/move reconciliation and an explicit full reanalysis action. Existing Back delay, count consistency, Collection smoke tests and partial-link policy remain in TODO. Do not infer current Play status from September notes.

## Latest Checkpoint - 2026-08-24

- `1a14883`: URI-based Discovery update counts, managed-album-path live filtering, and small UI polish.
- `d55293a`: prevents a ref excluded from an incoming discovery snapshot from deleting its existing discovery record during repeat analysis.
- Current local patch: preserves `DiscoveryPhotoRef.firstSeenSnapshotVersion` across URI reanalysis, then shows recently updated Discovery cards first with `NEW` and `이번에 +N장`.
- Discovery card totals now use `사진 N장`; `NEW` means new media in the latest analysis, not a newly created place key.
- Gemini review of `f56c38e`: no blocker. Deferred test-only coverage is recorded in `TODO.md` (same-count ordering, organized/MIXED suppression, URI place reclassification).
- Next visual polish is intentionally deferred: Discovery/Location Albums header surface consistency.
- Current local WIP: `MediaAnalysisStore` is an isolated, tested file-backed cache foundation only. Do not wire it into `loadSourceImages`/`loadSourceVideos` without preserving cache-hit `PhotoItem` output and adding integration tests.
- Device smoke result: after a clean reinstall, Band analysis showed 17 places; a repeat analysis showed 0 newly updated places and Discovery History stayed at 17.
- The missing two places from the earlier 19-place observation are not a repeat-merge loss. They remain part of the separate `duplicateInTarget` identity-policy investigation.
- Incremental analysis cache remains unimplemented and disabled. Start with a tested `MediaAnalysisStore`; do not re-enable the old `NoLocationCache`.

## Latest Checkpoint - 2026-08-25

- `MemoryPhotoPage` paging now keeps a date grid visually continuous when a single date crosses the 48-photo page boundary; the next page does not repeat that date header.
- Regression coverage now includes an offset past the final photo and `MediaAnalysisStore` recovery from a corrupt main file using its `.bak` file.
- `MediaAnalysisStore` remains deliberately isolated. Its first scan-loop integration must reuse cached values to create normal `PhotoItem`/Discovery records while skipping only EXIF and Geocoder work.
- P1 product direction: after date-level memos are validated, show a conditional Home section named `기억을 꺼내보기` only for users who have saved memos. Do not add an empty state or another permanent tab.

## Cache Integration Checkpoint - 2026-08-26

- Read `MEDIA_ANALYSIS_CACHE_INTEGRATION_PLAN_2026-08-26.md` before wiring the store into scanning.
- First implementation is images only. Keep videos on the existing path.
- The scan loop must load the file cache once into a run-local index, reconstruct `LocationResult` on a hit, and always continue to `buildPhotoItem()`.
- Persist staged cache entries only after a normal completed scan. Cancellation or a failed scan must not write partial new cache state.
- Do not reuse or re-enable `cachedNoLocationResult()` / `NoLocationCache` as the implementation shortcut.

## Image Cache Integration Checkpoint - 2026-08-29

- `MediaAnalysisStore` is now connected to `loadSourceImages()` only through `ImageAnalysisCacheSession`.
- The session loads the file cache once per image scan, reconstructs a normal `LocationResult` on hit, and only stages new entries until a normally completed scan writes them atomically.
- Cache hits still pass through `buildPhotoItem()` and therefore keep Preview, Discovery History, and no-location totals intact.
- Current MediaStore GPS invalidates a cached `NO_LOCATION` entry for that image.
- Device smoke test passed: the large folder became visibly faster on repeat analysis; repeated runs preserved the same visible counts; the device created `media_analysis_cache.json` successfully.
- Videos still use the legacy, disabled no-location path. Do not extend this patch to video scanning without a separate test/review pass.

## Workspace And Shared Docs

Project root:

- `C:\Users\mismi\Documents\Codex\GallerySorter`

Read these first:

- `AGENT_SHARED_TODO.md`
  - Current shared status, stable baseline, open TODOs, and guardrails.
- `TODO.md`
  - Read `Current Execution Plan (2026-08-24)` first. It is the single current priority order; lower dated sections are detailed/historical context.
- `MEDIA_LIFECYCLE_MEMORY_SYNC_DESIGN.md`
  - V2.1 lifecycle design. It explicitly separates Discovery History virtual `MemoryCollection` from Location Albums physical MediaStore move merge.
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

## Current WIP Note - Display First / Memory Browsing MVP

Current local WIP now includes the first visible Memory Browsing MVP wiring.

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
- Keep new Display First domain/data logic outside `MainActivity`.
- Some temporary UI rendering is currently wired in `MainActivity` for MVP validation; split this into a renderer/controller before the next large UI patch.
- Discovery-only memories are wired to Preview and a minimal in-app Memory Browser.
- Home entry, Search integration, and per-memory Gallery album creation are not implemented yet.

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

2026-08-15 pre-UI repository update:

- Added `MemoryRepository` as the first read-only facade for Memory browsing first.
- It converts discovery-only groups and organized albums into `MemoryRecord`.
- It does not write discovery-only records into `AlbumSummaryHistoryStore`.
- It merges records only when the exact `memoryKey` matches; it does not guess by title/country/city.
- Added `MemoryBrowserState` / `MemoryBrowserItem` / `MemoryBrowserDetail` as the UI-facing adapter layer.
- Discovery-only detail stays `sourceUri` based and excludes stale refs.
- Added edge tests for empty `sourceUri` and all-stale/missing source refs.
- Gemini review prompts are in `WORKLOG_2026-08-15.md`.

2026-08-15 MVP UI wiring update:

- Added `DiscoverySnapshotController` as the thin bridge from Preview analysis results to `DiscoverySnapshotStore` / `MemoryRepository`.
- Preview analysis completion saves a discovery snapshot.
- Preview completion dialog now offers `앱에서 보기` to open the Memory Browser before Gallery album creation.
- Result screen shows `발견한 장소 둘러보기` when discovery or organized memory data exists.
- Memory Browser list/detail can show discovery-only source URI thumbnails in-app.
- Discovery-only UI is labeled as `앨범 만들기 전`; organized albums remain `정리된 앨범`.
- Discovery-only detail still does not write to `AlbumSummaryHistoryStore` and does not fake `StoredAlbumSummary`.
- `이 장소를 앨범으로 정리` is still a bridge back to the existing result screen, not true per-memory organization.
- Device install/smoke test is pending because no ADB device was connected during this checkpoint.
- Gemini review prompt for this patch is in `WORKLOG_2026-08-15.md`.

2026-08-15 device stabilization / detail model update:

- Added a Home entry to reopen saved Memory Browser data without rerunning analysis.
- Memory Browser now merges discovery and live organized albums by conservative place identity when exact `memoryKey` differs.
- Memory Browser uses live album filtering so deleted/moved Gallery albums do not keep showing stale Gallery actions.
- Organized album opening now delegates to Samsung Gallery album opening instead of opening only the first media item.
- Source-folder dialog has a reentry/loading guard to avoid stacked dialogs.
- Memory detail has a date-section model (`MemoryPhotoSection`) and renders date headers in the in-app photo grid.
- The visible photo cap remains for safety; do not remove it until the detail grid becomes lazy/paged.
- Thumbnail taps now pass image/video MIME types instead of `*/*` to reduce chooser prompts.
- Validation:
  - `.\gradlew.bat testDebugUnitTest`
  - `.\gradlew.bat assembleDebug`

2026-08-15 Gemini blocker follow-up:

- Hardened `DiscoverySnapshotJson` schema reading.
  - Missing schema is still compatible.
  - Future/unsupported schema is treated as unreadable.
  - Non-array `groups` is treated as unreadable.
  - `DiscoverySnapshotStore` now routes those semantic corrupt cases through backup recovery / write refusal.
- Added `OrganizePlaceService` as a side-effect-free plan builder.
  - It filters stale or empty source refs from a discovery memory.
  - It computes a target relative path but does not write `SortInputStore` or start WorkManager yet.
- Added tests for schema hardening and organize-plan filtering.
- Device smoke checklist is documented in `WORKLOG_2026-08-15.md`.

## Immediate Next Session Checklist

Start from:

- Branch: `codex/photoplace-v2-bg-wip`
- Commit: latest Memory Browsing MVP checkpoint in git log.

Before coding:

1. Check `git status`.
2. Read this file, `WORKLOG_2026-08-14.md`, `MEMORY_BROWSING_FIRST_DESIGN.md`, and `Photoplace_V2_Epic_PRD_2026-08-09.md`.
3. Keep replies/reviews in Korean when prompting Gemini.

Next code patch after reviewing the MVP UI wiring:

1. Address Gemini review findings if any.
2. Device smoke test the Preview -> `앱에서 보기` -> Memory Browser -> detail flow.
3. Decide whether the temporary `MainActivity` Memory Browser UI should be extracted into a renderer/controller before more UI work.
4. Improve user-safe handling for missing/deleted/permission-denied source photos if phone testing exposes rough edges.
5. Design the Android-aware adapter that converts `OrganizePlaceService.Plan` into `PhotoItem` / `SortInputStore` for true per-memory album creation.
6. Do not write discovery-only rows to `AlbumSummaryHistoryStore`.
7. Do not use empty `relativePath` as a fake organized album.
8. Keep no-location repeated-analysis prevention as a separate cache/snapshot patch; do not re-enable the old `NoLocationCache` without the v2 safety tests below.

Focused checks for the next UI-facing patch:

- discovery-only memories appear before Gallery album creation.
- organized album actions remain Gallery-based.
- detail handoff by `memoryKey` opens source refs for discovery-only records.
- empty state works when no snapshot/history exists.
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

## V2.1 Media lifecycle / Memory sync (design accepted, implementation not started)

- Read `MEDIA_LIFECYCLE_MEMORY_SYNC_DESIGN.md` before changing repeat-analysis caching, Discovery lifecycle, or location-album merge behavior.
- Do not treat `DiscoverySnapshot` as the permanent Memory source or as the analysis cache.
- Target split: `MediaStore` is live-media truth; `MediaAnalysisStore` avoids repeated analysis; `Memory` is the logical location/date/memo view; Gallery albums are optional organization output.
- Album creation must not delete a Memory. `발견 기록` is a new/unorganized filter; the long-term parent concept is `기억`.
- First merge MVP is user-named virtual `AlbumCollection`; do not implement physical folder merge until explicit confirmation, result record, partial-failure, and cleanup/undo policy are designed.


0. Discovery record persistence, new-results UX, and analysis cache (next P0):
  - `DiscoverySnapshot` now merges records across sequential source-folder analyses (`9beef8b`), but it is app-private storage and is deleted when the app/data is deleted.
  - Do not call this problem only a cache problem: the app needs both a persistent discovery-result UX and a separate `LocationAnalysisCache` that avoids repeated EXIF/Geocoder work.
  - Add a top section in Discovery: `이번에 새로 발견한 장소`.
    - Show newly discovered place cards and a place count, separate from file count.
    - Copy must distinguish `새 장소 7곳` from `사진 174장`; the current `앱에서 볼 항목 174개` is a file count.
  - Add a safe `발견 기록 다시 구성하기` flow for reinstall/data-clear recovery. It must reanalyze saved source folders, never delete photos or Gallery albums.
  - `LocationAnalysisCache` contract:
    - key by stable media identity + metadata signature; result is either `LOCATION_NONE` or normalized location data usable to rebuild `PhotoItem`.
    - a cache hit skips expensive location lookup, not user-visible inventory. It must still restore the correct discovered place or no-location count.
    - invalidate on new/copy/move/modified media, policy changes, or GPS becoming available.
    - never revive the old dangerous “new files become 0” behavior; test this before enabling.
  - Suggested Gemini task: design the cache schema/invalidation rules and tests only. Do not modify `MainActivity` or re-enable `NoLocationCache` without approval.

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
  - Current code has `NoLocationCache`, but it is intentionally disabled through `isNoLocationCacheEnabled() == false` after the earlier dangerous "new items show as 0" regression.
  - Correct v2 behavior is not to remove files from the preview inventory. It should only skip the expensive `readLocation()` path when an unchanged file was already confirmed as no-location.
  - Cache hit must still create a `PhotoItem` with `noLocation=true`, so `위치 없음 N개` remains visible to the user.
  - Cache hit must still be excluded from Memory Browser groups and album organization candidates.
  - Store enough signature data to skip unchanged no-location items:
    - mediaStoreId and sourceUri when available
    - displayName
    - date_modified, date_added, datetaken
    - size and duration when available
    - video/image flag
    - size/path signal if available
    - no-location analysis result timestamp/policy version
  - If MediaStore already exposes latitude/longitude, never trust the no-location cache; re-run location analysis.
  - If signature differs in any meaningful field, treat as cache miss and re-run location analysis.
  - Prefer a file-backed JSON store with temp/write + backup recovery, schema version, policy version, trim/TTL, and tests. SharedPreferences v1 is not enough for the final version.
  - Required tests before enabling:
    - new file is not hidden by cache
    - same URI with changed modified/size invalidates cache
    - copied/moved media with different URI/mediaStoreId misses cache
    - MediaStore GPS added after a no-location cache entry forces reanalysis
    - cache hit keeps no-location preview count
    - corrupt cache restores from backup or safely resets
    - policy version change invalidates old entries
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

## 2026-10-24 — Seongnam MIXED album addition local patch

- User reports Seongnam Discovery >2200 photos vs physical location album373. Suwon subsequently confirmed normal by user.
- Reproduced missing MIXED detail action with failing regression test, then added MemoryAlbumAction for local photo-addition eligibility, known Pictures path pinning, existing-target preflight, and existing duplicate skip. Activity changes are delegation/label bindings. Global bulk organization and scanner/cache/trash code unchanged.
- Debug327/Release327 tests PASS, assembleRelease PASS. Report: REVIEW_Mixed_Album_Addition_2026-10-24.md.
- NOT installed/published. Phone uses Play signing certificate, local APK uses upload key; never uninstall to bypass signature mismatch. Next validation requires appropriate Play-signed test update or separate controlled test setup. Real candidate count, filename identity limitations and one-off post-sort missing-record report remain OPEN.

## 1.3.8(38) Play-signed test installed — 2026-10-24
User authorized phone test without uninstall. Uploaded38 to internal DRAFT only; production37 unchanged. Downloaded Play generated universal APK through generatedapks.download_media, verified same device signing certificate, adb install -r SUCCESS. FirstInstallTime/permissions and existing Home place records retained. No photos copied/deleted by agent. Phone Home open; user to test Seongnam Album Add preview. This supersedes the prior NOT installed note for the MIXED patch. See RELEASE_1.3.8_Test_2026-10-24.md.

## Latest — 1.3.9(39) progress fix test installed
User confirmed Seongnam38 copy completed but detail had no progress, unlike Suwon. Reused existing SortProgressStore/worker/UI loop; extracted always-bound hidden WorkingProgressBanner and presentation. Activity adds delegates only. Debug331/Release331 tests and release build PASS.39 internal draft/Play-signed universal APK/adb install -r SUCCESS, firstInstallTime retained. User's prior Seongnam operation increased physical album photos373 to2266.39 actual progress increment/background return still requires device test. No agent-triggered media copy/trash/cache reset. Full handoff: WORKLOG_2026-10-24.md; review request: REVIEW_REQUEST_Mixed_Album_Progress_2026-10-24.md.

## Anti review received / evidence corrections
REVIEW_Mixed_Album_Progress_Antigravity_2026-10-24.md says APPROVED for Merge. Current code/counts and existing331 Debug/Release XML match; no new confirmed product blocker. Original kept intact. Follow-up report REVIEW_Mixed_Album_Progress_Followup_2026-10-24.md corrects physical-video30 vs Home aggregate, unverified private JSON byte preservation, and blanket perfect-fix/no-lifecycle-risk claims.39 actual progress/UI return test remains Pre-release. No product/device changes or new test execution in this review verification.

## Production submission — 1.3.9(39)
User explicitly requested end-to-end production release without manual final clicks. Reused hash-verified39 bundle, created production draft with public Korean notes, checked nonblocking deobfuscation warning, saved100% rollout, submitted the sole production change through final confirmation. Publishing UI now shows review changes/production1.3.9; quick checks ongoing, managed publishingOFF so approval leads to automatic publication. Not yet confirmed live; no monitor created. No source/device/media changes this submission. Details RELEASE_1.3.9_2026-10-24.md; screenshot reports/production-1.3.9-submitted-20261024.png. Earlier test-only status notes are historical.39 progress real-device validation and original-trash work remain open.

## Original cleanup design only / Anti review request
User explicitly said do not implement yet; establish direction and review with Anti. ORIGINAL_CLEANUP_DESIGN_2026-10-24.md proposes one review/execution flow for after-sort and later grouped cleanup, PhotoPlace local photo originals only, independent source-to-actual-output evidence, legacy read-only matching, selected-pair byte verification/reverification, Memory metadata preservation, <=2000 URI system batches, and separate performance budget. REVIEW_REQUEST_Original_Cleanup_Design_2026-10-24.md ready for Anti. No implementation or trash activation authorized by these documents. No product/device/media/release changes this design task.

## Original cleanup design V2 — review only

Anti returned CONDITIONAL DESIGN APPROVAL with post-trash audit, Memory dependency guard, and legacy execution isolation requirements. V2 specifies async target survival/content audit after locking subsequent batches, source-excluded Memory/date/collection projection and whole-selection guards, and a default-off legacy execution gate as Phase0. It rejects a zero-millisecond completion guarantee and final-only evidence flush; proposes bounded transactional checkpoints while requiring durable request journaling before system prompts. Shared filename duplicate logic is retained for copying. No implementation or device/media/release changes.

Read ORIGINAL_CLEANUP_DESIGN_V2_2026-10-24.md and REVIEW_Original_Cleanup_Design_Followup_2026-10-24.md. Original design and Anti report remain intact. REVIEW_REQUEST_Original_Cleanup_Design_2026-10-24.md now points to V2 questions. Await Anti re-review and the user's decision to implement.

## Original cleanup V2 DESIGN APPROVED received

Confirmed Anti's V2 report: B1/B2/B3 resolved at design level. Updated V2 status and TODO. No implementation yet; user's prior direction was design/review first. Next implementation scope, when authorized, is Phase0 default-off gate and legacy cleanup execution isolation only. Keep copy dedupe, caches and user records intact. Review examples50–100 records/checkpoint and340+ tests are not fixed requirements; use measured thresholds and meaningful scenarios. No code/device/media/release changes in this acknowledgment.

## Original cleanup Phase0 implemented — Anti code review pending

User authorized starting Phase0 after design approval. Added default-off CleanupFeatureGate; retired legacy requests/results only show unavailable notice. Removed Activity trash/delete request sink, direct is_trashed mutation, old confirmation/prune/success-state cleanup, preview filename-duplicate to cleanup-candidate connection, and legacy cleanup UI. Existing source scanning/copy dedupe/cache/Memory resolution are unchanged. Narrow Activity delta +5/-353 vs `.debug/cleanup-phase0/MainActivity.before.java`; patch saved beside snapshot. Six legacy persistence/dedupe method bodies matched exactly and five protected source hashes matched.

Before patch: isolation2 tests failed. After: Debug336/Release336 PASS, assembleDebug/assembleRelease and lintVitalRelease PASS; no new full lint/device tests. Local version39 APKs are NOT the already submitted39; no phone install, media mutation, version bump, upload or production changes. Phase1 evidence ledger is not implemented. See REVIEW_Original_Cleanup_Phase0_2026-10-24.md and REVIEW_REQUEST_Original_Cleanup_Phase0_2026-10-24.md for Anti review. Keep cleanup disabled until later validated stages.

- 2026-10-24: 안티 Phase0 코드 승인 확인. 사용자 요청으로 1.3.10(40) Play 서명 APK 덮어쓰기 설치 완료, 홈 표시 확인. 사용자 회귀 확인 대기. Phase1 미착수. 프로덕션 배포 없음. 상세 WORKLOG_2026-10-24.md.

## Original Cleanup Phase 1 + 발견 기록 수량 표시 (2026-10-24)
- 안티 Phase0 승인 및 사용자의 다음 단계 지시로 증거 원장 구현. Phase1 자체 검증 완료·안티 코드 리뷰 대기. Phase2 이상 미착수.
- 실제 반환 source/target URI·바이트 수를 작업별 기록. 최대50건/항목 간5초 SQLite FULL 트랜잭션 체크포인트. 저장 실패는 복사 결과와 분리, 해당 세션 증거만 중단.
- 모든 기록은 COPY_OBSERVED_UNVERIFIED, 내용 해시 NULL. 기존 duplicateInTarget·검색 캐시·메모/별칭 로직 보존. CleanupFeatureGate=false 유지.
- MainActivity는 Phase0 scoped patch 재구성본과 완전 일치, 이번 변경0줄.
- 자동검사 Debug346/Release346 실패·오류·skip0, Debug/Release APK 및 lintVitalRelease 성공.
- 연결폰 별도 패키지(com.photoplace.evidenceprobe, 미디어 권한 없음)로 실제 SQLite replay/rollback/reopen/쓰기거부/페이지 및 SortJob+fake copy engine 성공·실패·취소·영상 제외 검사 통과.
- 실제 probe 프로세스 강제 종료 후 커밋50건만 복구. 미기록17건·미완료 트랜잭션1건은 후보 근거로 남기지 않음 확인.
- 합성3000건/60checkpoint: 총297ms, checkpoint227ms. 실제 사진 복사/검색 성능 보장값 아님. 테스트 도구 제거 완료.
- 발견 기록 아이콘 제거: 사진996장·동영상12개 텍스트 표시. 전체121개는 종류 미구분 예전 이력 합계 fallback임을 확인해 사진·동영상121개(이전 기록)으로 표시. 숫자 오류를 확정한 것은 아님. 추가 전앨범 스캔 없음.
- 사용자 앱은 아직 Phase0 1.3.10(40). Phase1 미설치·미배포. 실제 MediaCopyEngine+사용자 선택 사진 처리량/진행 UI 검증은 남음.
- 상세: REVIEW_Original_Cleanup_Phase1_2026-10-24.md / REVIEW_REQUEST_Original_Cleanup_Phase1_2026-10-24.md. 재현 도구: verification/cleanup-evidence-probe/README.md.

## 앨범 흐름 실기기 제보 및 Phase1 안티 승인 수신 (2026-10-24)
- 사용자 제보를 ISSUES_Album_Flow_2026-10-24.md에 A~E로 분리했다. 청주·서초동·용인 만들기 버튼 미노출은 아직 내부 원본 연결 상태를 확인하지 못해 조사 중으로 유지.
- 읽기 전용 UI에서 청주 상세 만들기 버튼 없음, 평창 생성 후 위치 앨범 첫 카드 표시 확인. MediaStore에서 청주 사진행121개/서초동310개 존재 확인. 사진 조작 없음.
- 로컬 수정: 추가 버튼은 이미 읽은 detail projection에 남은 원본 사진이 있을 때만 표시. 완료 화면 보기3곳은 UI 검색/스크롤 초기화 후 기존 앨범 탭 이동. 사진/동영상 아이콘 복원+장/개 단위, 접근성은 전체 명칭.
- MainActivity 후속 변경은 UI routing/state binding만; 정책 판단은 MemoryAlbumAction에 격리. 상세 diff .debug/album-flow/activity.patch. Phase1 자체의 Activity0줄 사실과 이후 UI 변경을 구분한다.
- 최종 Debug351/Release351 전건통과, APK Debug/Release 및 lintVitalRelease 성공. 실제 새 UI 폰 확인은 설치 전이라 대기. 사용자 앱 Phase0 1.3.10(40) 유지.
- 안티 Phase1 승인 보고서 원문 확인. 원장 승인 수신을 기록하되 이후 UI 변경 승인으로 소급하지 않음. Phase2 미착수, gate=false 유지.
- 상세 자체리뷰/안티 후속 질문: REVIEW_Album_Flow_Followup_2026-10-24.md.

## 1.3.11(41) 테스트 설치 완료 (2026-10-24)
- 사용자 진행 지시로 Phase1+앨범 UI 수정본을 폰에 Play 서명 APK로 install -r 성공. firstInstallTime/사진 권한 유지. 사용자 사진 복사·이동·삭제 자동 수행 없음.
- internal41 draft만 갱신, production39 completed 유지 확인. 정식 배포·검토 제출 안 함.
- bundleRelease 성공. DEX 심볼/인증서/버전 확인. 홈 및 발견 기록의 아이콘+950장/23개·성남2,266장/30개 표시 확인.
- 새 복사 작업 진행률/완료 보기 최상단/불필요한 추가 버튼 숨김은 사용자 확인 대기. 원본 정리 gate=false, Phase2 미구현.
- 청주·서초동·용인 조사 중 폰이 브라우저로 바뀌어 뒤늦게 스크롤 입력을 중단. 사용자에게 알림. 다른 앱 내용은 진단 근거에서 제외, 해당 장소 내부 연결 상태 미확인 유지.
- 상세 산출물 SHA/로그/사용자 확인순서: RELEASE_1.3.11_Test_2026-10-24.md. 이슈목록 상태도 갱신.

## 보류 — 예전 합계 표시 문구 (2026-10-24, 사용자: 이따가 수정)
- [ ] 다음 수정 묶음에서 `사진·동영상 N개 (이전 기록)` → `사진·동영상 합계 N개`로 변경.
- 예전 사진/오래된 날짜라는 의미가 아님. 종류 구분 없는 저장 이력의 합계이며 사진/영상 분리를 추정하지 않는다.
- 이번 요청은 보류 기록만. 즉시 코드 변경/재설치/배포 없음.

## 2026-10-25 Codex 인수 검토
- 폰은1.3.12 code43, 로컬/준비APK는code44. 사용자가44 미설치를 확인. code44의 실기기0장 해결 여부는 아직 미검증.
- 기본 Debug382/Release382 통과 재확인. 별도 .debug init-script 부정 재현3개는 모두 실패: 해시 기준 없는 사후감사 통과, target0 journal 통과, 현재 읽기 불가 파일도 기존MATCH 캐시로 eligible.
- 실제 metadata 읽기만 조사: Download/Camera7229행 중 datetaken NULL1419행. 중복 후보 개수 아님. CleanupMediaAccess 전컬럼nonnull 조건이 이들을UNREADABLE로 분류할 수 있음.
- 그 외 최종 실행 재검증/게이트/감사잠금·재시작복구, 추정앨범패턴 범위,0개 토스트의 탈락사유 은폐를 검토 과제로 정리.
- production코드 변경/폰설치/휴지통·사진조작/배포 없음. MainActivity code41대비 scoped diff는 UI binding/delegation 범위 확인.
- 상세 REVIEW_Original_Cleanup_Takeover_2026-10-25.md. 현재 원본 실행 안전성은 HOLD, 먼저 읽기 전용 단계별 진단과 보호 경계 수정.

## 1.3.13(45) 검토 전용 설치 완료
- 사용자의 설치 요청 수행. 44를 그대로 설치하지 않고 최종 휴지통 실행을 잠근 검토판45를 구성.
- 매번 실제 내용 재검사, datetaken NULL의 선택적 허용, 전체 과거 후보 탐색 및 실시간 진행률, 후보/내용일치/연결 보류 진단 추가. MainActivity 변경0줄.
- Debug385/Release385 전건통과, bundleRelease 성공. Google Play 앱서명45 APK로 install-r 성공, 최초설치시각·사진권한 유지. internal draft만, 정식배포없음.
- 실제 휴지통 버튼·최종 Dispatcher 모두 잠금. 사후 감사·journal·선택 직전 검증의 미해결 차단과제는 유지. 사용자 사진 이동·삭제 수행없음.
- 사용자가 메인 버튼에서 대량 검토 결과를 확인할 예정. 상세 REVIEW_Original_Cleanup_ReadOnly_Test_2026-10-25.md.

## code45 실기기 결과 및 사용자 UX 피드백
- 팝업 조회: 새 과거후보3513쌍, 전체검사3778쌍, 내용일치3778쌍, 검토통과0, 연결보류3778표시.
- 검색은 정상 수행됐으나 전역 dependency status를 모든 후보에 복사해 전원에게 같은 개별 사유처럼 표시하는 문제 확인. 대표사진/이력 URI 참조 몇 개가 전체를 막았는지 분리 필요. 현재 실제 차단 URI 수는 미확인.
- 검색3513과 전체3778은 다른 집계 범위. 기존265기록 영향, 물리적으로 같은 source/target 기록이 중복될 수 있음. 실제사진/용량 source별 dedup 필요.
- 사용자 요구: 기본 progress popup을 진행 화면으로 개선, 홈의 큰 휴지통 버튼 노출 위치 재검토. 제안: 위치앨범 상단 원본정리 + 앨범 완료 후 보조 검토 진입.
- 전체집합 가드는 유지하고, 개별보류 분리 후 남은 집합을 다시 검증해야 함. 실행 잠금 유지. 후속 UI/모델 미구현·미배포.
- 상세 REVIEW_Original_Cleanup_Result_UX_2026-10-25.md. 기존 (이전기록)→합계 문구 변경 TODO도 보류 상태 유지.

## 1.3.14(46) 원본 검토 UX·개별 보류 개선 설치 완료
- 사용자 진행 지시로 설명만 하지 않고 수정·회귀·Android 화면검증·Play서명 설치까지 완료.
- 동일 source/target 작업 기록을 한 번만 검사. 표시·용량은 원본URI별 한번만, 모든 검증쌍 증거는별도보존.
- 실제 보류 source와 전역 미완료 분리. 보류를 뺀 나머지전체를 재검증, revision 변경/오류/취소/8pass한도는미통과. 참조출처(앨범정리이력·사용자대표사진 등) 개별표시.
- 홈 큰버튼제거→위치앨범상단 원본정리. 단계별전체진행화면, 결과의 보류사진필터/비동기유계썸네일. 이전기록문구→사진·동영상합계도반영.
- MainActivity scoped+1/-1 UI위임만. 검색/복사엔진변경없음. Debug396/Release396, 빌드·lintVitalRelease통과.
- 별도 headless emulator5582 합성3장 UI검증(2통과1보류), 필터후row1/이동버튼disabled 확인. helper제거 및직접시작한에뮬종료.
- 1.3.14 code46 Play앱서명 install-r 성공, 기존최초설치일/권한유지. internal draft만, 정식배포없음. 실제휴지통실행잠금유지.
- 실제개인사진 재검토결과는 사용자확인대기. 사후감사·journal·최종실행재검증미해결과제는계속유지.
- 상세 REVIEW_Original_Cleanup_Review_UX_2026-10-25.md / 독립리뷰 REVIEW_REQUEST_Original_Cleanup_Review_UX_2026-10-25.md.


## 2026-10-25 원본 정리 실행 보강 (code49)
- 전용 실기기 301장 이동·복구/취소/복사본 변경/프로세스 재시작 검증 완료.
- 실행 전 전체 선택 검증, durable journal, 실제 상태·해시 감사, 복구 경로 보강.
- code47 실제 3513장 내용 일치/전체 SNAPSHOT_CHANGED 확인 → code49 지연 snapshot 및 연결 단계 진행/작업 내부 캐시 보강.
- 409 Debug / 409 Release, lintVitalRelease / bundleRelease 통과.
- 검증·리뷰 요청: REVIEW_Original_Cleanup_Execution_2026-10-25.md / REVIEW_REQUEST_Original_Cleanup_Execution_2026-10-25.md.
- 최종 실제 사진 검토 및 프로덕션 제출 상태는 해당 리뷰 문서 끝에 추가 예정.


## 최종 결과: 1.3.15 / code51
- 원본 정리 실행 기능 활성화, 실제 사진3508장 정리 가능/5장 개별 보류 확인. 사용자 원본 이동0회.
- 최종412 Debug/412 Release 및 release 빌드/lint 통과.
- Play 공식 서명 설치 및 기존 데이터 유지.
- 프로덕션 1.3.15(51) 전체 출시 제출 완료. Console: 검토 중인 변경사항 / 일반적으로 발견되는 문제에 대한 빠른 검사 실행 중. 검사 성공 후 자동으로 심사 전송, 관리형 게시 꺼짐으로 승인 후 자동 배포. 아직 공개 배포 완료라고 주장하지 않음.
- RELEASE_1.3.15_2026-10-25.md / REVIEW_Original_Cleanup_Execution_2026-10-25.md / REVIEW_REQUEST_Original_Cleanup_Execution_2026-10-25.md 참고.


## 원본 정리 UI 테스트 code52 — Drive 전달 (2026-10-25)
- 사용자 요청으로 1.3.16(code52) Play 서명 APK를 Drive Codex APK에 업로드 완료.
- https://drive.google.com/file/d/1mwC4TsIKHmsclxGbo8k5LW7pPWY88pa2/view
- internal=draft, production51=halted 확인. 사용자 테스트 전 프로덕션 재제출 금지.
- 원본 폴더 안내/보존 앨범 구분/그룹 및 전체 선택 개선. Debug/Release 각417 테스트 통과.
- 상세: REVIEW_Original_Cleanup_UI_Followup_2026-10-25.md


## 사용자 실사용 제보: 자리 비운 후 재진입 오류 (미해결)
- 사용자: 진행 중 폰을 두고 갔다가 앱에 진입하니 오류 발생, 휴지통에 한 번 들어갔다 돌아온 뒤 정상화.
- 오류 발생 단계/정확한 문구, 휴지통이 갤러리인지 앱 내 동선인지 미확인. 정상 완료나 사진 보존을 확인한 것으로 간주하지 않음.
- 정적 확인: CleanupExecutionController.resume은 활성 저널을 다시 감사하며, MediaStore 조회/해시/의존성 확인 실패 시 중단 또는 NEEDS_RECOVERY로 남김. 갤러리 방문이 상태 갱신을 유발했는지는 추정일 뿐 원인 미확정.
- 후속: 사용자 단계/문구 확인 후 CleanupExecution 로그, 저널 상태 및 MediaStore 실제 상태를 대조. 화면 복귀/잠금·해제/프로세스 재생성 시나리오 재현 필요.
- 코드 수정/추가 배포 없음. 프로덕션 code51 중지 및 code52 테스트 상태 유지.


## 원본 정리 실사용 후속 진행 (배포 보류)
- TODO_Cleanup_Resume_Background_2026-10-25.md로 복귀/WorkManager/실행 UI/배치 확대/재현 테스트를 분리.
- 사용자 확인: 실제 원본 이동은 완료. 오류 발생 단계 미확정.
- 활성 작업 오류 안내 수정, 승인 후 감사 WorkManager 분리, 원형 실행 팝업 교체, 나중에 계속 상태 보관, 진입 키보드 해제 구현.
- 상태 전이 테스트 포함 Debug/Release 각424건 통과. 별도 런타임 검증 진행 중.
- REVIEW_Cleanup_Background_Resume_2026-10-25.md에 안전 경계 및 남은 게이트 기록.
- production51 중지 유지. Drive code52는 이전 빌드. 사용자 폰 설치/사진 변경 없음.

- 후속 최종 검증: Debug/Release 각425건 통과. API33 합성 1쌍 WorkManager 완료/중복 enqueue/프로세스 재시작 후 HOME 전환 통과. 360dp·큰 글꼴 XML 확인. API36 실기기·301장 OS 승인·픽셀 시각 검증은 미완료라 배포 보류 유지.


## code53 테스트 폰 설치 완료
- 사용자 요청으로 1.3.17(code53) Play 서명 APK를 R5KL503VHQR에 install -r 성공. 최초 설치 시각 유지.
- internal53 draft, production51 halted 재확인. 프로덕션 제출/Drive 업로드/사진 복원·이동 없음.
- 상세: RELEASE_1.3.17_Test_2026-10-25.md. 이제 Android16 폰 사용자 검증 대기.


## 실기기 1500/3508 완료 뒤 새 검사 2008장 복귀 문제
- 폰 화면/알림 수량 대조: 3508-1500=2008. 검토 화면 선택0장 대기 상태 확인.
- 일부 완료 뒤 NOT_APPLIED를 CANCELED로 닫는 분기를 PAUSED로 보존하도록 수정. 이전 기기 원장 상태는 직접 확인하지 못했으므로 정확한 트리거 미확정.
- 안티 critical audit 재검토 및 UI 통일 변경: REVIEW_Cleanup_Critical_Audit_Response_2026-10-25.md.
- 최신 소스 수정만 수행. 폰은 code53 유지, 새 설치/출시/사진 변경 없음.
- 후속: LiveData 관찰 전환, 일반 Worker 장시간 감사 재개 검증, 작은 진행 카드 시각 검증.


## 1.3.18(code54) 실제 폰 업데이트 설치 완료
- 사용자 요청으로 UI/남은2008장 테스트를 위해 Play서명 APK install -r 성공, 최초 설치 시각 유지.
- APK에서 CleanupUiStyle/CleanupDependencyRetry 실제 포함 확인. Debug/Release 각429 tests 통과.
- internal54 draft, production51 halted 확인. 제출/출시 재개 및 사진 조작 없음.
- RELEASE_1.3.18_Test_2026-10-25.md에 파일/해시/기기검증/남은 리뷰 항목 기록.


## code55 1000장 테스트 업데이트 설치
- 사용자 요청으로 1.3.19(code55) Play서명APK install -r 성공. 기존300장 원장/완료 수량 유지, 명시적 이어하기 때 남은 구간만1000장으로 전환.
- 녹색 진행률/단계 마침표/최종 확인 진행 안내 반영. tests 각433 PASS.
- RELEASE_1.3.19_Test_2026-10-25.md 및 REVIEW_Cleanup_Batch1000_2026-10-25.md 참고.
- 폰은 이전 묶음 복사본 확인 화면 관측. 시스템승인 클릭/사진조작 없음. production51 halted 유지.

- 사용자 실기기 피드백: 보라색 버튼/탭과 녹색 진행바가 어색해 녹색 적용을 취소. CleanupUiStyle.PROGRESS=PURPLE, 활성 단계/실행 수량도 보라색으로 통일. 소스만 수정했으며 진행 중인 code55 폰 테스트에는 재설치하지 않음. 다음 빌드 반영 예정.

- code55 사용자 실기기 테스트 결과: 정리를 모두 마친 뒤 앱에서 중복 사진이 없다고 표시된다고 사용자 확인. 이는 사용자 관측이며 모든 사진 수동 열람이나 휴지통 영구 비우기 검증을 에이전트가 수행한 것은 아님. 보라색 원복 code56 테스트 설치 준비 요청을 계속 진행.


## 1.3.20(code56) 보라색 원복 폰 설치 완료
- 사용자 요청으로 R5KL503VHQR에 Play 서명 APK 업데이트 성공, 최초설치시각 유지.
- 진행바/단계/수량 보라색 통일. 1000장 및 이어하기 기능 변경 없음.
- RELEASE_1.3.20_Test_2026-10-25.md 기록. internal56 draft / production51 halted 확인. 사진/휴지통 조작 없음.


## 최종 콘솔 확인: 제출38 검토 중
- 제출 세부정보에서 변경사항1개 / 프로덕션1.3.20 전체 출시 시작 / 소스API / 제출번호38 / 2026-10-25 오후10:21 / 상태검토중 확인.
- 사용자 추가 클릭 없음. 승인 후 자동 게시. 아직 실제 게시완료 상태로 주장하지 않음.
- 추가 증빙: .debug/cleanup-review-2026-10-25/play-production56-submission38.jpg.

## 2026-11-08 메모리 관리 B/C 리뷰 대기
- 구현/API33 통합 검증 완료, 독립 리뷰 미완료: REVIEW_REQUEST_Memory_Management_PhaseBC_2026-11-08.md
- 표식: REVIEW_READY.md / REVIEW_READY_Memory_Management_PhaseBC_2026-11-08.json
- 검증: 각470 tests, Debug/Release 빌드 PASS, 실제 OS5개 취소/승인·재진입 및 helper1001개 감사 테스트 PASS.
- 촬영일/날짜 메모 불변 수정 포함. 실제 폰/신규 Android 버전 검증 및 출시 별도.

## 2026-11-08 R2 / 사용자 폰 설치
- 안티 B-1/B-2/B-3 수정: REVIEW_Memory_Management_PhaseBC_Codex_Followup_2026-11-08.md
- 각474 tests + bundleRelease + API33 후속 flow PASS. READY_FOR_REREVIEW, 독립 재승인 전.
- Play서명1.3.21(code57) R5KL503VHQR install -r/앱 실행 확인. 사용자 데이터 유지 업데이트, 실제 기능은 사용자 테스트 대기.
- internal57 draft / production56 completed 유지, 제출/공개 출시 없음. RELEASE_1.3.21_Test_2026-11-08.md 참조.


## 2026-11-08 최종 중단 — Story code71 HOLD
- [ ] 미리보기 사진 이중 렌더(큰 배경 사진 + 폴라로이드 내부 사진) 및 검은 상하 영역 원인 확인/수정.
- [ ] 가로/세로 바깥 배경 제거 및 문구/날짜 오른쪽 정렬 실렌더 확인.
- 사용자 오늘 작업 중단 요청. 자동 수정/빌드/출시 금지. 다음 구현 전 싱크업.
- HANDOFF_Story_Bugs_2026-11-08.md 먼저 읽을 것. code71 설치됐지만 사용자 반려.


## 2026-11-09 Story code72 — 再開/修正/Google Drive 전달
- 사용자 재개 지시 후 code71 이중 사진/검은 영역을 불투명 단색 렌더로 수정. 가로/세로 fit, 내용 크기 카드 중앙, ‘성남에서’/날짜 오른쪽 정렬.
- Debug/Release 각504 PASS 및 API33 native6종/하드웨어/PNG/gesture PASS. MainActivity 및 다른 기능 무변경.
- [x] 같은 서명 Play APK 확보 및 Google Drive 웹 파일 확인.
- [ ] 사용자 실기기 Instagram 결과 확인 및 독립 리뷰.
- WORKLOG_2026-11-09.md / REVIEW_REQUEST_Story_Single_2026-11-09.md 참고. 공개 출시 금지.


## 2026-11-09 最終 code74 — 카드 외곽 제거/뷰어 UI/저장
- [x] 공유 파일은 카드-only PNG + Instagram sticker URI. 가로/세로 외곽 사각 배경 제외.
- [x] 뷰어 ⋮→위치 핀, 앱 스타일 팝업. 기존 기능 연결 유지.
- [x] 자동 저장 대신 상단 명시적 저장 아이콘. 갤러리 PhotoPlace Stories 결과 PNG 저장.
- [x] 각504 tests/native 카드·저장·팝업 PASS, 릴리즈 빌드 및 Drive 웹 최종 파일 확인.
- [ ] 사용자 실제 Instagram 확인 및 독립 리뷰. 공개 출시 금지.
- REVIEW_REQUEST_Story_Card_Save_Viewer_2026-11-09.md / WORKLOG_2026-11-09.md 참고. code72/73 대신 **1.3.38/code74** 대상.


## 2026-11-09 code75 미리보기 최종 확인
- [x] 가로/세로 편집 카드 확대(fit85%/94%), header/tab 영역 보존.
- [x] 실제 공유/저장 PNG code74와 byte-identical, 관련 도메인/MainActivity 무변경.
- [x] 504 tests 및 API33 native6종/gesture/save/header PASS, Play 서명 APK 준비.
- [ ] 사용자 code75 미리보기 확인 후 요청한 출시 진행. 현재 제출/공개 출시 금지.
- REVIEW_REQUEST_Story_Preview_Fit_2026-11-09.md / WORKLOG_2026-11-09.md 참고.


## 2026-11-09 사용자 code75 승인 → 프로덕션 제출 완료
- [x] 사용자 출시 승인: “츨시하자 고고쓍”. 기존 확인 전 제출 금지 조건 충족.
- [x] 동일 code75 AAB SHA256 확인 후 프로덕션1.3.39 전체 출시 제출.
- 콘솔 Google 빠른 자동 검사 진행 중, 이후 검토. 승인 후 자동 게시(관리형 게시 OFF). 공개 게시 완료는 아직 미확인.
- RELEASE_1.3.39_Production_2026-11-09.md / WORKLOG 참고. 새 기능/추가 배포/자동 모니터 실행하지 않음.

- [x] 2026-11-10 code80 사용자 수용 후 production 제출. 자동 검사/심사 대기, 승인 후 자동 게시. RELEASE_1.3.44_Production_2026-11-10.md 참조.
