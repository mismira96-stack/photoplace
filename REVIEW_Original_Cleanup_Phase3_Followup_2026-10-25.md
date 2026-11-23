# 코드 리뷰 보고서: 원본 사진 정리(중복 사진 검토) "0장" 버그 원인 분석 및 해결 (1.3.12 code 44)

- **작성 일자:** 2026-10-25
- **검토자:** Antigravity (Google DeepMind Advanced Agentic Coding)
- **대상 버전:** PhotoPlace 1.3.12 (versionCode: 44)
- **협업 연동 대상:** Codex 및 실기기 테스터 (SM_F971N)
- **관련 커밋/브랜치:** `codex/photoplace-v2-bg-wip`

---

## 1. 개요 및 사용자 보고 사항

- **사용자 제보:**
  - "으 아직도 0장나옴 ㅋㅋ 뭐하냐 ㅋ"
  - "원본사진보관소가 여기저기 있을텐데"
  - "내가 테스트하려고 download폴더랑 카메라에 남겨둔 게 많아가지고 (폰 기본 중복사진 찾기에는 엄청 많이 나옴)"
- **실기기 미디어 감사 결과 (SM_F971N):**
  - 기기 내 총 3,715장의 중복 대상 사진 확인:
    - `Download/`: 3,408장
    - `DCIM/Camera/`: 98장
    - `Download/Quick Share/`: 7장 등
    - 목적지 위치 앨범: `Pictures/용인에서/`, `Pictures/성남에서/`, `Pictures/Kurume에서/` 등 5,400+장 보관 중.
- **핵심 질문:** 실제 위치 앨범에 동일한 사진(이름+크기 일치)이 3,715장이나 있는데, 왜 앱에서는 "정리할 수 있는 복사된 원본 사진이 없습니다" (0장) 토스트가 발생했는가?

---

## 2. P0 / P1 게이트키퍼 검증

| 원칙 | 기준 | 판정 | 세부 내용 |
|---|---|---|---|
| **🚨 P0 Gatekeeper** | `MainActivity.java` 0줄 격리 원칙 | **PASS (100% 준수)** | 이번 작업에서 `MainActivity.java`에는 비즈니스/검토/판단 로직이 **단 1줄도 추가/수정되지 않음** (0 lines modified). 모든 로직은 `MemoryDependencyGuard`, `CleanupHistoricalScanner`, `CleanupController`에서 격리 처리. |
| **🔒 P1 Gatekeeper** | MediaStore / 파일 불변 원칙 | **PASS (100% 준수)** | 디스크 파일 복사/이동/삭제 없음. 모든 검증 및 후보 탐색은 Read-Only 프로젝션. 삭제는 OS 공식 다이얼로그(`MediaStore.createTrashRequest`)를 통해서만 실행. |
| **🔒 서명 무결성** | Google Play 공식 서명 유지 | **PASS** | `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d` 인증서 검증 완료. 기기 재설치 시 기존 데이터 무손실 유지. |

---

## 3. 발견된 근본 원인 (Root Causes) 및 해결 내역

### 🔴 [Blocker 1] `MemoryDependencyGuard`의 외부 소스 사진 전량 탈락 버그
- **발생 위치:** [`MemoryDependencyGuard.java#L58-L63`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryDependencyGuard.java#L58-L63)
- **원인 분석:**
  - `CleanupCandidate.isEligible()`은 `contentStatus == MATCH`와 `dependencyStatus == PRESERVED`를 **동시에** 요구합니다.
  - `Download/`, `DCIM/Camera/` 등 일반 폴더에 있는 사진들은 PhotoPlace의 "발견(Discovery) 메모리 그룹"에 속하지 않으므로, `before.sourceMemories.get(source)`가 `null`을 반환합니다.
  - 기존 코드:
    ```java
    Set<String> ids = before.sourceMemories.get(source);
    if (ids == null || ids.isEmpty() || !ids.equals(after.sourceMemories.get(source)))
        return Status.UNVERIFIED_IDENTITY;
    ```
  - `ids == null`인 경우 메모리 의존성이 없으므로 안전하게 보존(Pass)되어야 함에도, 무조건 `UNVERIFIED_IDENTITY`를 반환했습니다.
  - 이로 인해 기기 내 3,715장 후보 전원이 `isEligible() == false` 판정을 받아 `eligibleCandidates().isEmpty()`가 `true`가 되었고, "정리할 수 있는 복사된 원본 사진이 없습니다" 토스트가 즉시 발생했습니다.
- **해결책 적용:**
  - `ids == null || ids.isEmpty()`일 때 `afterIds`도 비어있으면 메모리 의존성이 없는 일반 원본 사진으로 판단하고 `continue;`하여 정상 `PRESERVED` 통과 처리.
  - `MemoryDependencyGuardTest`에 `sourceOutsideDiscoveryMemoriesPassesWithoutMemoryDependencies` 단위 테스트 추가 완료.

---

### 🔴 [Blocker 2] `CleanupHistoricalScanner`의 비파라미터화(Raw Literal) SQLite 쿼리
- **발생 위치:** [`CleanupHistoricalScanner.java#L152-L188`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupHistoricalScanner.java#L152-L188)
- **원인 분석:**
  - 위치 앨범 경로(`Pictures/%에서/`)를 ContentResolver에 질의할 때, SQL `selection` 문자열 내부에 한글 따옴표 리터럴(`"relative_path LIKE 'Pictures/%에서/'"`)을 직접 인라인으로 작성.
  - Android ContentProvider IPC 구조상 비ASCII 한글 문자열이 `selectionArgs` 없이 바인딩되면 Android MediaProvider 버전에 따라 파싱 실패, 인코딩 불일치 또는 빈 Cursor가 반환될 수 있음.
  - `MainActivity.java:3674`에서는 항상 `"relative_path LIKE ?"`와 `selectionArgs = new String[]{"Pictures/%에서/"}` 바인딩을 사용함.
- **해결책 적용:**
  - `loadKnownAlbumRelativePaths()`와 `queryTargets()` 모두 파라미터 바인딩(`?`)과 `selectionArgs` 체계로 전환.
  - `"Pictures/%에서/"`, `"Pictures/%에서"`, `"Pictures/%에서/%"` 3가지 변형 경로를 모두 바인딩하여 안전성 확보.

---

### 🟡 [Pre-release 3] `CleanupController`의 `isShowing()` 비동기 레이스 컨디션
- **발생 위치:** [`CleanupController.java#L72`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupController.java#L72)
- **원인 분석:**
  - `coordinator.evaluate(..., () -> !progress.isShowing())` 형태로 다이얼로그 노출 여부를 취소 토큰으로 사용함.
  - Android에서 `progress.show()` 호출 직후 백그라운드 스레드가 먼저 시작되면 윈도우 레이아웃이 완료되기 전 `isShowing()`이 `false`를 반환할 수 있음.
  - 이 경우 `coordinator.evaluate` 첫 줄에서 취소된 것으로 간주하고 즉시 빈 목록(`Collections.emptyList()`)을 반환하는 간헐적 레이스 컨디션 위험 존재.
- **해결책 적용:**
  - `AtomicBoolean canceled = new AtomicBoolean(false)` 명시적 플래그 도입.
  - 다이얼로그 `OnCancelListener` 및 "취소" 버튼 클릭 시에만 `canceled.set(true)` 되도록 변경하여 스레드 안전성 확보.

---

## 4. 검증 결과

### 1) 단위 테스트 (Unit Tests)
- `./gradlew.bat testDebugUnitTest testReleaseUnitTest` 실행:
  - **총 382개 테스트 통과 (0 failures, 0 errors, 0 skipped)**
  - 신규 단위 테스트 통과:
    - `MemoryDependencyGuardTest.sourceOutsideDiscoveryMemoriesPassesWithoutMemoryDependencies`
    - `CleanupHistoricalScannerTest.isTargetAlbumPath_identifiesKnownAndPatternAlbums` (`Pictures/Kurume에서/`, `Pictures/서초에서/` 등)

### 2) 빌드 및 패키징
- `versionCode`를 43에서 **44**로 상향 조정.
- `bundleRelease` 빌드 완료: `photoplace-1.3.12-code44-test.aab` (2,790,042 bytes).
- Google Play 내부 테스트 트랙 초안 등록 및 공식 Universal APK 다운로드 완료:
  - 파일명: `photoplace-1.3.12-code44-play-signed.apk` (2,833,595 bytes)
  - 서명 인증서: `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d` (Google Play 공식 릴리스 키 일치)
- 에뮬레이터(`emulator-5580`) 설치 및 구동 검증 완료 (`versionCode=44 minSdk=29 targetSdk=36`).

---

## 5. 실기기 테스트 가이드 (사용자/Codex 협업)

휴대폰(Galaxy Fold, `R5KL503VHQR`) 연결 시 아래 명령어로 즉시 업데이트 설치가 가능합니다:

```bash
adb -s R5KL503VHQR install -r photoplace-1.3.12-code44-play-signed.apk
```

### 테스트 시나리오:
1. 앱 실행 후 홈 화면 상단 또는 결과 화면에서 **"원본 사진 휴지통으로 이동" (또는 원본 정리 메뉴)** 탭.
2. 프로그레스 다이얼로그 노출:
   - "원본 사진 정리 검토: 위치 앨범 복사본과 원본의 내용 일치 여부를 검증하고 있습니다..."
3. 검토 다이얼로그(`CleanupReviewDialog`) 정상 노출 확인:
   - 이전 "0장" 대신, `Download/` 및 `DCIM/Camera/`에 있는 사진들의 썸네일과 크기, 목적지 앨범 이름이 리스트로 정상 표시되는지 확인.
4. "휴지통으로 이동" 버튼 탭 시:
   - Android 시스템 다이얼로그("PhotoPlace에서 사진 X장을 휴지통으로 이동하도록 허용하시겠습니까?") 팝업.
   - 사용자 승인 후 B1 사후 감사("복사본 안전 점검")가 정상 완료되는지 확인.
