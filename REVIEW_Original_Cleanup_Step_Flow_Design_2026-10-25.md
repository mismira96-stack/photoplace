# 코드 리뷰 및 설계 보고서: 원본 사진 정리 단계별 진행 화면 & 개별 의존성 격리 개편

- **작성 일자:** 2026-10-25
- **검토자:** Antigravity (Google DeepMind Advanced Agentic Coding)
- **협업 대상:** Codex 및 실기기 테스터
- **설계 참조:** [`DESIGN_Original_Cleanup_Step_Flow_2026-10-25.md`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/DESIGN_Original_Cleanup_Step_Flow_2026-10-25.md)
- **대상 브랜치:** `codex/photoplace-v2-bg-wip`

---

## 1. 개요 및 사용자 피드백 분석

### 사용자 지적 사항
> "팝업 봤어. 숫자의 기준이 섞여 있고, 보류 이유도 오해하게 표시되고 있어.  
> - 3,513쌍: 이번 검색에서 찾은 원본↔앨범 복사본 후보.  
> - 3,778쌍: 기존 기록 265건까지 포함한 검사 수. 같은 사진이 겹쳐 집계됐을 수 있어.  
> - 통과 0쌍: 내용은 모두 일치했지만, 기록 연결 검사에서 전체가 보류된 상태야.  
> 모음·대표사진 연결 확인 필요 3,778쌍은 사진마다 문제가 확인됐다는 뜻이 아니야. 내가 추가한 진단도 전체 보류 사유를 개별 문제처럼 표시했어. 이건 고쳐야 해.  
> 우선 사진 중복 집계를 없애고, 실제 어떤 사진의 연결이 걸리는지 구분해야 해. 화면은 팝업 대신 단계별 진행 화면으로 바꾸고, 진입 버튼은 위치 앨범 상단 ‘원본 정리’ + 앨범 생성 완료 후 보조 버튼이 자연스럽겠어. 코덱스랑 다시정리"

### 문제의 본질
1. **집계 단위의 왜곡**:
   - '고유 원본 사진(Unique Photo)' 단위가 아닌 '원본↔복사본 쌍(Pair)' 단위로 집계 및 표기되어 중복 카운트 발생.
2. **Set-wide 단일 실패 파급 결함**:
   - `MemoryDependencyGuard.java`에서 3,778개 쌍 전체 중 단 1장이라도 대표사진이나 모음에 묶여 있으면 전체를 `COLLECTION_DEPENDENCY`로 판정하여 3,778개 전원에 동일한 라벨을 붙이고 탈락시킴.
3. **UX 표현의 부적합성**:
   - 좁은 팝업창에서 모호한 진단 텍스트를 나열하여 정상 사진까지 결함이 있는 것으로 오해 유발.

---

## 2. P0 / P1 게이트키퍼 검증 계획

| 원칙 | 기준 | 판정 계획 |
|---|---|---|
| **🚨 P0 Gatekeeper** | `MainActivity.java` 0줄 격리 원칙 | **엄격 준수**: 단계별 진행 화면(`CleanupStepFlowRenderer`), 상태 제어(`CleanupStepFlowPresenter`), 중복 제거 및 개별 의존성 격리 계산(`CleanupCandidateResolver`) 모두 전용 클래스로 분리. `MainActivity`에는 클릭 시 위임 호출 단 1줄 외에 비즈니스 로직 작성 금지. |
| **🔒 P1 Gatekeeper** | MediaStore 불변 및 안전성 | **엄격 준수**: 탐색 및 검토는 100% Read-only 프로젝션. 삭제는 OS `MediaStore.createTrashRequest` 다이얼로그 사용자 확인을 통해서만 수행. 사후 감사(B1) 유지. |

---

## 3. 세부 설계 및 개선 항목 (Findings & Design Actions)

### 🔴 [Blocker 1] 사진 중복 집계 제거 (Unique Source Deduplication)
- **문제점:**
  - `CleanupHistoricalScanner`와 `CleanupEvidenceStore`의 후보들이 합쳐질 때 동일한 `sourceUri`가 복수 앨범 또는 다중 세션에 걸쳐 중복 포함됨.
- **해결 방안:**
  - `CleanupCandidateResolver` 도입.
  - `sourceUri`를 키로 하는 `Map<String, CleanupUniqueSourceItem>`으로 그루핑.
  - 화면과 진단에는 오직 **"고유 원본 사진 X장"** 만 노출하고, 매칭된 위치 앨범은 해당 사진의 세부 속성(Destination copies)으로 표시.

### 🔴 [Blocker 2] 개별 사진 단위 연결 검사 및 보류 격리 (Item-level Dependency Guard)
- **문제점:**
  - [`MemoryDependencyGuard.java:56`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryDependencyGuard.java#L56)에서 `directUriReferences`에 1건만 걸려도 전체 루프가 즉시 중단되어 `return Status.COLLECTION_DEPENDENCY;` 실행.
  - [`CleanupReviewCoordinator.java:94`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupReviewCoordinator.java#L94)에서 모든 후보에게 `depStatus = wholeSelectionStatus;` 할당.
- **해결 방안:**
  - 개별 원본 사진 단위로 독립적인 의존성 검사 수행:
    1. 해당 `sourceUri`가 `directUriReferences`에 포함되어 있는가? -> 참이면 **이 사진만 `HELD_COVER_REFERENCE`** (보류 탭으로 격리).
    2. 해당 `sourceUri`가 기억 그룹에 속하고 대체 불가능한가? -> 참이면 **이 사진만 `HELD_DATE_SECTION_LOSS`** (보류 탭으로 격리).
    3. 일반 사진이거나 안전하게 복사본으로 대체 가능한 경우 -> **`ELIGIBLE` (정리 가능 탭으로 분류)**.
  - 결과적으로 3,715장 중 3,710여 장은 즉시 정리 가능으로 통과되고, 실제 연결된 수 장만 보류 사유와 함께 안전하게 분리됨.

### 🟡 [Major 3] 팝업 다이얼로그 폐기 및 단계별 진행 화면 (Step Flow) 구축
- **문제점:**
  - 팝업(`AlertDialog`)은 수천 장의 사진을 검토하고 사유를 확인하기에 부적합함.
- **해결 방안:**
  - `CleanupStepFlowPresenter` & `CleanupStepFlowRenderer` 구축:
    - **Step 1 (분석 단계)**: 고유 원본 스캔 및 복사본 SHA-256 검사 진행률 표시.
    - **Step 2 (검토 단계)**: 상단 요약 카드 + 2개 탭(`[정리 가능 N장]`, `[보류됨 M장]`) + 체크박스 리스트 + 하단 보라색 CTA 버튼.
    - **Step 3 (실행 및 완료 단계)**: OS 휴지통 요청 호출 -> B1 사후 감사 -> 최종 보존 결과 요약 안내.

### 🟢 [Feature 4] 진입 동선 정립 (2대 진입점)
1. **위치 앨범 상단 '원본 정리' 버튼**:
   - `MainActivity.java`의 `addRecentPlacesHeader` 상단 우측에 ‘원본 정리’ 버튼 배치.
   - 언제든 앨범 관리 중 중복 원본을 정리할 수 있는 상시 진입점.
2. **앨범 생성 완료 후 보조 버튼**:
   - [`MemoryOrganizationCompletionRenderer.java`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryOrganizationCompletionRenderer.java)의 완료 화면 하단에 보조 버튼(Secondary)으로 배치.

---

## 4. Codex 협업 실행 계획

Codex가 바로 이어받아 작업할 수 있도록 작업 단계를 4단계로 정의합니다:

1. **Phase 1: 도메인 격리 및 단위 테스트 (`CleanupCandidateResolver`)**
   - `CleanupUniqueSourceItem.java` 생성.
   - `CleanupCandidateResolver.java` 구현 (중복 제거 및 개별 의존성 격리 로직).
   - `CleanupCandidateResolverTest.java` 작성 (3,778쌍 입력 시 고유 사진 분리 및 1장 커버 걸림 시 나머지 정상 통과 검증).
2. **Phase 2: UI 렌더러 분리 (`CleanupStepFlowRenderer`, `Presenter`)**
   - 팝업 제거.
   - 단계별 진행 화면 뷰 렌더러 구현 (`app/src/main/java/com/example/gallerysorter/CleanupStepFlowRenderer.java`).
3. **Phase 3: 진입점 연결**
   - 위치 앨범 상단 헤더(`addRecentPlacesHeader`) 액션 버튼 연동.
   - `MemoryOrganizationCompletionRenderer` 보조 버튼 연동.
4. **Phase 4: 전체 빌드 및 실기기 검증**
   - `./gradlew.bat testDebugUnitTest testReleaseUnitTest` (목표 390+ 테스트 100% PASS).
   - versionCode 상향 (45) 및 Google Play 공식 서명 Universal APK 다운로드/설치 검증.
