# PhotoPlace 증분 분석 및 홈 대시보드 캐시 종합 코드 리뷰 보고서

- **일자**: 2026-10-19
- **검토 기준 커밋**: `1c8d683` (HEAD) 대비 Working Tree 전체 변경 사항
- **참고 문서**: 
  - `REVIEW_REQUEST_Incremental_Analysis_2026-10-18.md`
  - `REVIEW_REQUEST_Home_Cache_2026-10-18.md`
  - `REVIEW_REQUEST_Refresh_Performance_2026-10-18.md`
- **준수 원칙**: `AGENTS.md` / `GEMINI.md` (P0 MainActivity 격리 원칙, P1 데이터 안전성 및 무결성)
- **최종 판정**: **조건부 출시 승인 (Conditional Release Approval)**
  *(코드베이스 및 테스트 무결성 100% 통과. 출시 직전 성남 GPS 실기기 NEW 뱃지 1건 수동 확인 후 배포 권장)*

---

## 1. 종합 평가 및 게이트 통과 현황

| 검증 영역 | 결과 | 요약 |
| :--- | :---: | :--- |
| **P0 Gate: MainActivity 격리** | **PASS** | `MainActivity` 순감 -291줄 (+71 / -362). 비즈니스·판단·집계 로직 0줄 격리 준수 |
| **P1 Gate: 데이터 안전성 & 무결성** | **PASS** | `MediaStore` 및 원본 파일 수정/삭제 0건, 원본 휴지통 차단 영구 유지, 메모/ID 보존 |
| **증분 미디어 분석 정확도** | **PASS** | Row-v2 지문 기반 변경 감지, 트랜잭션형 Staged Commit, `NoLocationCache` 비활성화 유지 |
| **성남 NEW 뱃지 및 반복 보고** | **PASS (조건부)** | `MIXED` 타입 새 사진 누적 반영, URI 비교 기반 재실행 중복 방지 확인 (실기기 검증 대기) |
| **홈 대시보드 캐시 무결성** | **PASS** | 캐시 스냅샷 즉시 렌더링(~26ms), 경량 리비전 검사(~29ms), 원자적 파일 교체(tmp/atomic move) |
| **자동화 테스트 및 정적 검사** | **PASS** | `./gradlew.bat testDebugUnitTest` 247개 전원 통과 (0 failures), `git diff --check` 통과 |

---

## 2. P0 / P1 필수 게이트 심층 검증

### 🚨 [P0 게이트] MainActivity 비대화 절대 금지 (0줄 격리 원칙) — **PASS**
- **코드 라인 변동**: 기존 11,000줄 이상의 `MainActivity.java`에서 **71줄 추가 / 362줄 제거 (순감 291줄)**.
- **분리 및 격리 상세**:
  - 해외/국가별 카드 렌더링 및 클릭 이벤트: `HomeCountryCardsRenderer.java`로 완전 추출.
  - 홈 대시보드 캐시 로딩, 리비전 비교, 무변경 스킵, 백그라운드 갱신 조정: `HomeDashboardController.java`, `HomeDashboardPolicy.java`, `HomeRevisionReader.java`, `HomeDashboardStore.java`로 100% 분리.
  - `MainActivity`는 컨트롤러의 콜백을 받아 뷰 컨테이너에 `addView` 하거나 화면 전환 메서드를 호출하는 **순수 라이프사이클 및 UI 바인딩 역할**만 수행합니다.

### 🔒 [P1 게이트] 데이터 안전성 및 무결성 규칙 — **PASS**
1. **MediaStore / 파일 불변 원칙 (Read-only Projection)**:
   - 증분 분석기(`IncrementalMediaScanner`) 및 홈 리비전 리더(`HomeRevisionReader`)는 `ContentResolver.query()`로 읽기 전용 프로젝션만 조회합니다.
   - 디스크 쓰기는 앱 내부 샌드박스의 캐시 파일(`home_dashboard_cache.json`, `.nomedia` 디렉터리 내 스캔 캐시)에만 국한되며, 사용자 미디어 원본 파일이나 MediaStore 레코드를 변경하지 않습니다.
2. **원본 휴지통 이동(Trash) 영구 차단 유지**:
   - 원본 사진/동영상의 휴지통 이동 및 삭제 기능은 비활성화 상태가 엄격하게 유지되고 있습니다.
3. **사용자 메모 및 불변 식별자 보존**:
   - 날짜별 메모와 `stableMemoryId + dateKey` 식별 체계는 `MemoryBrowserState`와 브라우저 파이프라인에서 그대로 유지되며, 이번 캐시 도입으로 인한 구조적 변경이나 유실 위험이 없습니다.

---

## 3. 심층 영역별 검증 상세

### 1) 증분 분석의 정확성과 누락 가능성
**대상 클래스**: `IncrementalMediaScanner`, `IncrementalAnalysisSession`, `PreviewItemProjection`

- **Row-v2 지문(Fingerprint) 기반 변경 감지**:
  - Base Signature(`mediaStoreVersion`, `bucketId`, `rawFolderId`)를 1차 대조합니다.
  - 행(Row)별로 `id`, `dateModified`, `size`, `duration`, `generationModified`, `latitude`, `longitude`를 조합한 Fingerprint를 검사합니다.
  - 파일 수정, 크기 변경, 위치 좌표 보정 등이 발생할 경우 즉시 캐시 미스로 분류되어 무거운 분석을 수행합니다.
- **작업 취소/실패 시 안전성 (Staged Commit)**:
  - 스캔 중 취소(`isCancelled()`)되거나 예외가 발생하면 `session.finish(false, ...)`가 호출되어 스테이징된 항목이 폐기됩니다. 불완전한 상태가 온디스크 캐시나 체크포인트로 확정되지 않습니다.
- **레거시 캐시 마이그레이션**:
  - Fingerprint가 없는 구버전 캐시 항목은 `toLegacyAnalysisItem()`을 통해 1회 안전하게 마이그레이션되며, 변경 감지 시 최신 Fingerprint로 갱신됩니다.
- **`NoLocationCache` 비활성화 확인**:
  - `isNoLocationCacheEnabled()`가 `false`로 고정되어 있어, 위치 없는 사진이 의도치 않게 캐시에서 누락되는 현상이 원천 차단되어 있습니다.

### 2) 성남 NEW 표시와 반복 보고
**대상 클래스**: `MemoryBrowserState`, `DiscoverySnapshotController`, `DiscoveryResultText`

- **`MIXED` 타입의 NEW 뱃지 누락 결함 해결**:
  - 기존 코드의 `sourceType != DISCOVERED_ONLY` 조건문으로 인해 앨범과 발견 장소가 합쳐진 성남(`MIXED`) 카드에서 새 사진 수량이 집계되지 않던 버그가 수정되었습니다.
  - `MemoryBrowserState`에서 `record.getFirstSeenSnapshotVersion() == currentVersion`인 항목을 정확히 `recentAddedCount`로 누적하도록 개선되었습니다.
- **동일 사진 재분석 시 중복 보고 방지**:
  - `DiscoverySnapshotController.updateFor()`에서 직전 스냅샷의 URI 집합(`existingUris`)과 현재 URI를 `containsAll`로 대조합니다.
  - 동일한 사진으로 다시 스캔할 경우 `hasNewUris == false`가 되어 `newPlaceCount` 및 `updatedPlaceCount`가 0으로 정상 유지됩니다.
- **사용자 안내 문구 정확화**:
  - `DiscoveryResultText`에서 기존 "새로 발견한 장소 N곳"을 실제 의미에 부합하도록 "새 사진·동영상이 추가된 장소 N곳"으로 분리 렌더링합니다.

### 3) 홈 대시보드 캐시의 표시·갱신 무결성
**대상 클래스**: `HomeDashboardSnapshot`, `HomeDashboardStore`, `HomeDashboardController`, `HomeDashboardPolicy`, `HomeRevisionReader`, `HomeRefreshRequests`, `HomeCountryCardsRenderer`

- **표시 즉시성 및 무변경 스킵**:
  - 홈 진입 시 디스크의 직전 스냅샷(JSON)을 파싱하여 UI 스레드에서 즉시 카드를 그립니다(~26ms).
  - 이후 백그라운드 태스크에서 MediaStore 최신 상태 및 로컬 변경 토큰(`revisionToken`)을 대조(~29ms)하며, 변경이 없으면 무거운 전체 미디어 쿼리를 전면 생략합니다.
- **불완전 카드 및 예외 환경 방어**:
  - 스냅샷의 `complete` 플래그가 `false`이거나(카드가 덜 구성된 채 종료됨), 권한 상태가 변경되었거나, API 29 이하(generation 감지 불가 환경)인 경우 무변경 스킵을 비활성화하고 전체 재조회를 수행합니다.
- **캐시 쓰기 원자성 (Crash Resilient)**:
  - `HomeDashboardStore`에서 임시 파일(`.tmp`)에 직렬화 후 `StandardCopyOption.ATOMIC_MOVE`로 교체하므로 앱 강제 종료 시에도 캐시 손상이 발생하지 않습니다.
- **경쟁 상태(Race Condition) 방지**:
  - `HomeRefreshRequests`와 요청 일련번호(`requestId`) 체계를 통해, 백그라운드 갱신 작업이 끝났을 때 현재 활성화된 요청 번호와 일치하지 않으면 UI 갱신을 무시하여 구버전 데이터의 화면 덮어쓰기를 방지합니다.
- **상세 화면과의 격리**:
  - 캐시된 홈 카드를 클릭하더라도 상세 화면(`showMemoryBrowserDetailScreen`, `openCountry`)은 항상 라이브 미디어 저장소를 직접 조회하므로 최신 미디어가 온전히 표시됩니다.

---

## 4. 발견 사항 상세 (Severity Findings)

### [Low / Minor] 홈 카드 수량과 상세 화면 헤더 수량의 일시적 표기 차이 가능성
- **대상**: `HomeDashboardSnapshot.java`, `MainActivity.java`
- **조건**: 외부 카메라/갤러리 앱에서 새 사진을 촬영한 직후 PhotoPlace로 복귀했을 때, 홈 화면의 카드는 캐시된 직전 수량(예: 42장)을 표시하고 있는 상태에서 카드를 클릭하여 상세 화면으로 진입하는 경우.
- **영향**: 홈 카드에는 `42장`으로 표시되었으나 상세 화면 상단 헤더에는 라이브 조회 결과인 `43장`으로 표시될 수 있습니다.
- **분석 및 제안**: 홈 진입 성능(~26ms)을 확보하기 위해 가벼운 캐시를 우선 보여주고 상세 진입 시 정확한 실시간 데이터를 보여주는 의도된 설계(Design Trade-off)입니다. 백그라운드 리비전 갱신이 완료되면 홈 카드도 43장으로 동기화되므로, 현재 단계에서는 릴리스를 진행하고 후속 개선 과제로 유지하는 것이 적절합니다.

---

## 5. 분류별 조치 항목 및 릴리스 판정

### 🔴 출시 차단 이슈 (Release Blockers)
- **0건 (없음)**

---

### 🟡 출시 전 실기기 확인 필수 항목 (Pre-release Real-Device Checks)
1. **성남 카드 실제 GPS 촬영 사진 NEW 뱃지 점등 테스트**:
   - 코드상으로는 `MIXED` 타입에 대해 `recentAddedCount`가 정상 집계되도록 수정되었으나, 실제 기기에서 GPS 태그가 포함된 새 사진을 촬영/추가했을 때 앨범과 발견 장소가 합쳐진 성남 카드에 파란색 `NEW` 뱃지가 시각적으로 정상 렌더링되는지 확인 필요.
   - **확인 절차**:
     1. 기기에서 성남 지역 위치 태그가 포함된 사진 1장 촬영 또는 기기로 전송.
     2. 앱 실행 후 홈 화면의 '성남·송파구' 카드에 `NEW` 뱃지 및 새 사진 카운트가 노출되는지 확인.
     3. 앱 종료 후 재실행했을 때 중복 추가 보고 없이 안정적으로 유지되는지 확인.
2. **하남 레거시 앨범 및 날짜별 그리드 열람 확인**:
   - 하남 앨범 진입 시 레거시 앨범 데이터가 정상 렌더링되고 인앱 뷰어가 부드럽게 열리는지 확인.

---

## 🏁 최종 판정
**Ready for Pre-release Real-Device Validation (조건부 출시 승인)**  
P0 `MainActivity` 격리, P1 데이터 불변성, 247개 단위 테스트 및 Git diff 포맷 검사가 모두 통과되었습니다. 위의 **실기기 성남 NEW 뱃지 수동 확인** 1단계만 완료된 후 즉시 출시 버전 태깅 및 배포를 진행할 수 있습니다.
