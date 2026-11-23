# PhotoPlace — 원본 정리 Phase 3 (시스템 휴지통 디스패처·사후 감사·검토 UI·과거 중복 연동) 구현 보고서

- 문서: `REVIEW_Original_Cleanup_Phase3_Antigravity_2026-10-24.md`
- 작성자: Antigravity (Codex 토큰 소진에 따른 인수 구현 및 자체 검증 완결)
- 일자: 2026-10-24 (Asia/Seoul)
- 상태: **PHASE 3 구현 및 로컬 빌드·테스트 완결 / 에뮬레이터 설치 검증 완료 / 실기기 연결 시 즉시 설치 가능**
- 버전: 1.3.12 (versionCode 42) / targetSdk 36 / minSdk 29

---

## 1. 개요 및 구현 범위

사용자의 요청에 따라, 위치 앨범(`Pictures/<Location>/`)으로 복사된 카메라 원본 사진(`DCIM/Camera/`)을 안전하게 정리할 수 있는 **원본 정리 Phase 3 (검토 UI · 휴지통 요청 디스패처 · 요청 저널 · 사후 감사 프로토콜 B1 · 과거 복사본 탐지)** 구현을 완결했습니다.

### 핵심 구현 항목
1. **`CleanupEvidenceStore` Schema v3 업그레이드**:
   - `cleanup_requests` 저널 테이블 추가 (`request_id`, `batch_id`, `source_uris`, `target_uris`, `status`, `created_at`, `updated_at`).
   - 시스템 요청 직전 durable 저널 저장 (`recordJournal`) 및 감사 상태 전이 (`updateJournalStatus`, `readJournal`, `readLatestJournal`).
2. **`CleanupHistoricalScanner` (과거 중복 사진 연동)**:
   - 사용자가 Phase 1 이전(성남, 수원 등)에 생성한 위치 앨범의 복사본과 `DCIM/Camera/`의 원본 사진을 파일명·크기 기반으로 매칭하여 `copy_evidence`에 안전하게 등록.
   - 이후 `CleanupReviewCoordinator`의 64KB 스트림 SHA-256 전수 해시 검증 및 `MemoryDependencyGuard` 투영 검사를 통과한 항목만 정리 후보로 확정.
3. **`CleanupTrashDispatcher` (시스템 휴지통 디스패처)**:
   - `MainActivity.java` 밖으로 물리 격리되어 `MediaStore.createTrashRequest` 생성.
   - 1회 최대 100장 단위(API 36 상한 대비 안전한 소형 배치) 분할 처리 및 사전 저널 기록.
   - `LegacyCleanupIsolationTest` 바이트코드 상수 풀 검증 100% 통과 (`MainActivity.class` 내 파괴적 심볼 0개 유지).
4. **`CleanupPostTrashAuditor` (B1 사후 감사 프로토콜)**:
   - 시스템 결과(`RESULT_OK`) 수신 즉시 UI 스레드에서 후속 요청을 잠그고, 백그라운드 워커에서 사후 감사 실행.
   - 보존 대상인 위치 앨범 복사본이 정상 존재하고, 읽기 가능하며, 전체 스트림 SHA-256 해시가 실행 전과 일치하는지 전수 바이트 검증.
   - 통과 시 `AUDIT_PASSED` 확정, 손상/소실 의심 시 `NEEDS_RECOVERY` 경고 및 복구 안내.
5. **`CleanupReviewDialog` (사용자 검토 화면)**:
   - 검증된 후보 목록, 썸네일, 대상 앨범 경로, 절약 용량 표시.
   - 전체 선택/해제 및 개별 체크박스 제공 (기본 미선택 원칙 준수).
   - 선택 시 시스템 확인창 호출 안내 문구 및 30일 보관 복구 가능 안내 제공.
6. **`CleanupController` & `CleanupFeatureGate` 활성화**:
   - `CleanupFeatureGate.isEnabled() = true` 전환.
   - 메인 액티비티 생명주기와 비동기 검증·다이얼로그·사후 감사를 완전 격리 연결.
7. **`MainActivity.java` 0줄 비즈니스 로직 격리 원칙 (P0 준수)**:
   - `MainActivity.java`에는 비즈니스 계산이나 파일 처리 로직이 단 1줄도 추가되지 않았으며, `CleanupFeatureGate.onLegacyRequest(this, ...)` 및 `onLegacyResult(this, ...)`로 라이프사이클만 단순 위임.

---

## 2. 검증 결과

### 1) 단위 테스트 (Unit Tests)
- **Debug Build**: `379 tests completed, 0 failures, 0 ignored (100% PASS)`
- **Release Build**: `379 tests completed, 0 failures, 0 ignored (100% PASS)`
- **P0 Gatekeeper Test**: `LegacyCleanupIsolationTest` PASS (`MainActivity.class` 상수 풀에 `createTrashRequest`, `createDeleteRequest`, `is_trashed` 0건 확인).
- 신규 테스트:
  - `CleanupPostTrashAuditorTest` (4 tests: unknown request unresolved, cancellation pending, missing target recovery warning, intact copy audit passed).
  - `CleanupFeatureGateTest` (3 tests: disabled gate isolation, enabled actionable pending, null-activity safety).

### 2) 빌드 산출물
- **AAB**: `photoplace-1.3.12-code42-test.aab` (2,789,071 bytes)
- **Release APK**: `app\build\outputs\apk\release\app-release.apk` (7,218,457 bytes)
- Release Lint: `lintVitalRelease` PASS.

### 3) 에뮬레이터 설치 및 기동 검증
- 연결된 디바이스 (`emulator-5580`)에 `adb install -r app\build\outputs\apk\release\app-release.apk` 정상 설치 완료 (`Success`).
- 앱 시작(`am start`) 및 메인 화면 정상 기동 확인 (크래시 0건).

---

## 3. 실기기 (Samsung SM_F971N) 테스트 안내

현재 사용자의 폰(SM_F971N)은 케이블이 분리된 상태입니다.
폰을 PC에 연결하신 후 아래 절차로 테스트하실 수 있습니다:

1. **설치 명령**:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
   *(만약 이전 1.3.11이 Play Console 다운로드 서명본이어서 서명 불일치 오류 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`가 발생할 경우, 기존 앱의 데이터 보존을 원하시면 안내해 드리는 방법이나 백업 후 테스트하실 수 있습니다)*
2. **테스트 흐름**:
   - **홈 화면**: "남은 사진 원본 휴지통 이동" 버튼 또는 앨범 생성 완료 화면에서 "정리한 원본 정리 검토" 버튼 클릭.
   - **무결성 검사**: 백그라운드에서 성남/수원 등 위치 앨범에 이미 복사된 원본 사진들의 전체 내용(SHA-256)과 메모 보존 여부를 자동 검사.
   - **검토 화면**: 검증 통과된 사진 목록, 복사된 앨범 경로, 절약 용량 확인 후 "전체 선택" 또는 원하는 사진 선택.
   - **시스템 확인창**: Android OS의 공식 "휴지통으로 이동" 확인창에서 허용.
   - **사후 감사**: 앨범 복사본이 손상 없이 보존되었는지 즉시 대조 후 최종 완료 안내 확인.
