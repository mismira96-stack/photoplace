# 원본 정리 Phase 0 (레거시 실행 차단 및 게이트 격리) 코드 리뷰 보고서

- **검토자:** Antigravity (Google DeepMind)
- **일자:** 2026-10-24
- **검토 대상:**
  - [`REVIEW_REQUEST_Original_Cleanup_Phase0_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_REQUEST_Original_Cleanup_Phase0_2026-10-24.md)
  - [`REVIEW_Original_Cleanup_Phase0_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_Original_Cleanup_Phase0_2026-10-24.md)
  - [`CleanupFeatureGate.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupFeatureGate.java)
  - [`CleanupFeatureGateTest.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/test/java/com/example/gallerysorter/CleanupFeatureGateTest.java)
  - [`LegacyCleanupIsolationTest.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/test/java/com/example/gallerysorter/LegacyCleanupIsolationTest.java)
  - [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java) (scoped diff: `.debug/cleanup-phase0/activity-phase0.patch`)
- **판정:** **코드 반영 승인 (APPROVED for Merge) — Phase 0 완결 확인 및 Phase 1(증거 원장 구축) 착수 승인**

---

## 1. P0 / P1 원칙 및 핵심 검증 요약

### 🚨 [P0 원칙] MainActivity 비대화 절대 금지 (0줄 격리 원칙) — 통과 (PASS)
- 이번 Phase 0 패치에서 [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)의 신규 비즈니스/계산 로직 추가는 **0줄**입니다.
- 오히려 위험했던 옛 휴지통 다이얼로그, 미디어스토어 휴지통 요청 싱크, 중복 후보 수집 등 비대화 코드들이 대거 삭제되면서:
  - Phase 0 단독 diff: **+5 / -353 (순감소 -348줄)**
  - Git HEAD(`1c8d683`) 대비 누적 diff: **+177 / -833 (순감소 -656줄)**
- 추가된 코드는 버튼 숨김(`setVisibility(View.GONE)`) 및 전용 게이트 위임([`CleanupFeatureGate.onLegacyRequest`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupFeatureGate.java#L15), [`hasActionablePending`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupFeatureGate.java#L11), [`onLegacyResult`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupFeatureGate.java#L20))뿐입니다.

### 🔒 [P1 원칙] 데이터 안전성 및 무결성 규칙 — 통과 (PASS)
- **MediaStore / 파일 불변 원칙:** 디스크 미디어 복사/이동/삭제 없음.
- **원본 휴지통 싱크 원천 제거:**
  - `createTrashRequest`, `createDeleteRequest` 호출 싱크가 `app/src/main/java` 전체에서 물리적으로 완전 제거됨 (0건 확인).
  - 직접 `is_trashed` 컬럼을 갱신하던 `reinforceTrashState()`가 완전히 제거됨 (`is_trashed`는 읽기 전용 쿼리 필터에만 잔존).
- **기존 사진 복사 중복 방지 알고리즘 보존:**
  - [`MainActivity.hasDisplayNameInPath()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L3266) 및 [`duplicateFileSignature()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L3318) 본문이 100% 동일하게 보존되어 위치 앨범 정리 시 중복 복사 방지 기능에 일체의 영향이 없음.

### 🧪 자동 빌드 및 단위 테스트 검증
- `./gradlew.bat testDebugUnitTest testReleaseUnitTest`: **336 tests 전건 통과 (0 failures, 0 errors, 0 skipped)**
- `LegacyCleanupIsolationTest`: 컴파일된 `MainActivity.class` 바이트코드 상수 풀(Constant Pool)을 직접 파싱하여 파괴적 API 및 직접 컬럼 뮤테이션 심볼이 0개임을 검증.
- `CleanupFeatureGateTest`: 후보 수 및 `resultCode`와 무관하게 상시 `false` 반환 및 안내 토스트 전달 검증.
- `git diff --check`: 포맷/공백 오류 없음 (**PASS**).

---

## 2. 세부 검토 질문에 대한 정밀 분석

| 검토 항목 | 분석 및 검증 결과 | 판정 |
|---|---|---|
| **1. Activity 신규 로직 유입 여부** | 추가된 코드는 `deleteCopiedOriginals()`, `hasPendingOriginalCleanup()`, `onActivityResult(REQUEST_DELETE_ORIGINALS)`에서 게이트 메서드를 호출하는 5줄뿐이며, 순수 위임/UI 바인딩임. | **PASS** |
| **2. 우회 경로 차단 완결성** | `deleteOriginalsButton` 숨김, `addPendingOriginalCleanupCompactCard()` 삭제, `addOriginalDeleteAction()` 삭제, `showOriginalTrashConfirmDialog()` 삭제, `collectCleanupOriginalUrisFromPreview()` 삭제 등 모든 UI/배경 경로가 물리적으로 차단됨. | **PASS** |
| **3. 게이트의 독립적 안전성** | [`CleanupFeatureGate`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupFeatureGate.java)는 `isEnabled() == false`이며, `onLegacyRequest`와 `onLegacyResult`는 `isEnabled()`와 무관하게 영구적으로 안내 토스트만 반환하도록 하드코딩되어 향후 신규 기능 활성화 시에도 레거시 경로가 부활하지 않음. | **PASS** |
| **4. 파괴적 싱크 제거** | 앱 전체 소스코드 검색 결과 `createTrashRequest`, `createDeleteRequest`가 전무하며, `is_trashed`는 읽기 조건(`= 0`)으로만 사용됨. | **PASS** |
| **5. 핵심 파일 무결성** | `MediaCopyEngine.java`, `MediaStoreAlbumLookup.java`, `IncrementalMediaScanner.java`, `PreviewRefreshGate.java`, `MemoryMediaResolver.java` 등 핵심 모듈의 SHA-256 해시가 변경 전후 100% 일치함. | **PASS** |
| **6. 홈/결과 화면 정상 경로 보존** | `hasPendingOriginalCleanup()`이 항상 `false`를 반환하더라도, 이는 오직 옛 휴지통 배너/버튼 표시에만 관여하므로 홈 대시보드, 앨범 복사 완료 안내("원본은 그대로 보관 중이에요"), 발견기록 브라우징 등 모든 정상 기능은 완벽히 정상 동작함. | **PASS** |

---

## 3. 차기 단계(Phase 1) 진행 승인

Phase 0 작업이 완벽하게 완료되었으므로, **Phase 1 (증거 원장 구축: Evidence Store)** 단계로의 진입을 승인합니다.

### 📋 Phase 1 작업 시 준수 지침
1. **복사 성능 보존 (인메모리 버퍼링 + 주기적 체크포인트):**
   - V2 설계에 합의된 대로, `SortJob` 실행 중 매 사진 복사마다 디스크 fsync를 치지 말고, `SortJobResult` 내 인메모리 버퍼에 `(sourceUri, targetUri, takenAt, size, sha256)`를 누적한 뒤 50~100장 단위(또는 배치 완료 시)로 [`CleanupEvidenceStore`](#)에 원자적 체크포인트 Flush를 수행할 것.
2. **P0 0줄 격리 유지:**
   - 증거 원장 저장/조회/복구 로직은 순수 전용 클래스(`CleanupEvidenceStore`, `CleanupEvidenceRecord`)에 구현하고, `MainActivity`는 호출하지 않거나 단순 의존성 주입만 유지할 것.
3. **기존 복사 실패와 증거 저장 실패의 분리:**
   - 사진 복사 자체의 성공/실패(`SortJobResult`)와 정리 증거 원장 기록의 성공/실패를 명확히 분리하여, 증거 기록 실패로 인해 정상 복사된 사진이 재시도되거나 롤백되는 부작용이 없도록 할 것.

---

## 4. 최종 결론

Phase 0 패치는 레거시 휴지통의 위험성을 완전히 뿌리뽑고, `MainActivity`의 코드를 대폭 정돈한 훌륭한 리팩토링 및 안전 게이트 패치입니다.

**코드 반영 승인 (APPROVED for Merge)** 하며, **Phase 1 구현**을 시작해도 좋습니다.
