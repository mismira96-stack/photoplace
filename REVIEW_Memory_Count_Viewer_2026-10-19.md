# PhotoPlace 메모리 수량 1차 수정 및 확대 뷰어 로딩 종합 코드 리뷰 보고서

- **일자**: 2026-10-19
- **검토 기준**: HEAD `1c8d683` 대비 최신 Working Tree 전체 변경 사항
- **검토 요청서**: `REVIEW_REQUEST_Memory_Count_Viewer_2026-10-19.md`
- **참고 문서**:
  - `REVIEW_Memory_Count_Consistency_2026-10-19.md`
  - `REVIEW_Memory_Viewer_Performance_2026-10-19.md`
  - `REVIEW_PreRelease_Verification_2026-10-19.md`
  - `REVIEW_Incremental_Analysis_Home_Cache_2026-10-19.md`
- **준수 원칙**: `AGENTS.md` / `GEMINI.md` (P0 MainActivity 격리 원칙, P1 데이터 안전성 및 무결성)
- **최종 판정**: 
  - **코드 반영(Merge) 판정**: **승인 (APPROVED)**
  - **전체 앱 출시 판정**: **조건부 출시 승인 (Conditional Release Approval - 실기기 뷰어 체감 및 성남 GPS 확인 후 배포)**

---

## 1. 종합 평가 및 게이트 검증 요약

| 검증 항목 | 결과 | 세부 요약 |
| :--- | :---: | :--- |
| **P0 Gate: MainActivity 격리** | **PASS** | 전체 diff +81 / -382 (순감 301줄). 로더 바인딩 및 생명주기 위임만 존재, 비즈니스/로딩 판단 0줄 격리 준수 |
| **P1 Gate: 데이터 안전성 & 무결성** | **PASS** | `MediaStore` 및 원본 파일 수정/삭제 0건, 원본 휴지통 차단 영구 유지, 레코드/스냅샷 원본 불변 |
| **메모리 수량 정합성 (1차 패치)** | **PASS** | 상세 헤더와 실제 날짜 섹션 수량 일치, stale/빈 URI/중복 URI 제거, 사진/동영상 구분 포맷 정립 |
| **확대 뷰어 로딩 및 대기열 분리** | **PASS** | 전용 1-worker 분리, `LatestImageLoadQueue` 세대 기반 취소, 16MiB LruCache, 영상 전환 시 사진 덮어쓰기 차단 |
| **자동화 테스트 검증** | **PASS** | `./gradlew.bat testDebugUnitTest` **260개 전원 통과 (0 failures, 0 errors)**, `git diff --check` 통과 |
| **정적 검사 (Android Lint)** | **KNOWN DEBT** | 179 errors (전부 `MainActivity.java`의 `WrongConstant`, 179개 문장 모두 HEAD에도 존재). 이번 패치 신규 오류 0건 |

---

## 2. P0 / P1 필수 게이트 심층 검증

### 🚨 [P0 게이트] MainActivity 비대화 절대 금지 (0줄 격리 원칙) — **PASS**
- **코드 라인 변동**: 기존 11,000줄 이상의 `MainActivity.java`에서 **81줄 추가 / 382줄 제거 (순감 301줄)**.
- **뷰어 및 수량 관련 MainActivity 변경 범위**:
  - `MemoryViewerImageLoader` 필드 선언, `onCreate()`에서 인스턴스화, `onDestroy()`에서 `close()` 호출.
  - `loadThumbnailIntoPreservingCurrent()`를 `memoryViewerImageLoader.load()`로 1줄 위임.
  - `MemoryPhotoViewer.Host` 콜백에 `clearThumbnail()` 1줄 위임.
  - 기존 `MainActivity`에 흩어져 있던 뷰어 이미지 디코딩, 캐시 검사, 비동기 스레드 풀 할당 등의 판단 코드가 [`MemoryViewerImageLoader.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryViewerImageLoader.java)로 완전 추출됨.
- **판정**: **완벽 격리 준수 (PASS)**.

### 🔒 [P1 게이트] 데이터 안전성 및 무결성 규칙 — **PASS**
1. **원본 미디어 및 MediaStore 불변**:
   - `MemoryViewerImageLoader`는 `ContentResolver.loadThumbnail()` 및 `openInputStream()`만 사용하는 순수 읽기 전용 I/O입니다.
   - 디스크 쓰기나 MediaStore 레코드 수정/삭제는 전무합니다.
2. **불변 식별자 및 원본 데이터 보존**:
   - `MemoryPhotoSection`의 URI 중복 제거는 뷰 렌더링을 위한 인메모리 프로젝션에서만 수행되며, `discovery_snapshot.json`이나 `album_summary_history.json` 원본 파일에는 일체 쓰기 작업을 하지 않습니다.
   - `stableMemoryId + dateKey` 메모 체계와 `OrganizationLink` 무결성이 완벽히 유지됩니다.

---

## 3. 세부 영역별 심층 기술 검토

### 1) 메모리 수량 불일치 1차 수정
**주요 대상**: `MemoryMediaCounts.java`, `MemoryBrowserState.java`, `MemoryPhotoSection.java`, `HomeRecentPlacesResolver.java`

- **실제 grid/sourceUris와 상세 Header 수량의 일치 (P1-A 해결)**:
  - 기존에는 저장된 `item.countText`를 상세 상단에서 재사용하여 하남 앨범의 경우 저장 집계 105장 vs 실제 그리드 3장 불일치가 발생했습니다.
  - [`MemoryBrowserState.java#L302`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserState.java#L302)에서 실제 생성된 `sections`를 기반으로 `MemoryMediaCounts.fromSections(sections).text()`를 호출하도록 수정되어, 상세 헤더와 그리드 항목 수가 **100% 일치**하게 되었습니다.
- **Gallery 조회 실패와 확인된 0장 상태의 분리**:
  - `resolution.source == UNAVAILABLE`인 경우 `detail.withUnavailableCount()`를 통해 `"불러오기 필요"`로 명확히 표시하여, 일시적 권한/프로바이더 실패를 "사진 0장"으로 오인하지 않도록 방어했습니다.
- **동일 URI 중복 제거 및 인덱스 정합성 (P1-C 해결)**:
  - [`MemoryPhotoSection.java#L38-L41`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryPhotoSection.java#L38-L41): `seenUris.add(ref.sourceUri)`로 날짜 섹션 빌드 전 동일 URI를 최초 출현 순서대로 1건만 유지합니다.
  - `sourceUris`와 `photoSections`가 동일한 `validRefs`에서 생성되므로 뷰어의 `initialIndex` 및 탭 위치가 어긋나지 않습니다.
- **사진 / 동영상 구분 표기 포맷 (P1-B 해결)**:
  - [`MemoryMediaCounts.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaCounts.java)를 통해 발견 전용 및 상세에서 `사진 X장 · 동영상 Y개`, `동영상 N개`로 정확히 분리 표기됩니다.
  - 미조회 레거시 앨범 히스토리는 사진/영상 구분이 저장되어 있지 않으므로 `사진·동영상 N개`로 정직하게 표기하여 사용자 기만을 방지했습니다.

---

### 2) 의도적으로 OPEN된 수량 문제에 대한 출시 관점 평가
요청서에서 검토를 요청한 미해결(OPEN) 항목에 대한 아키텍처 평가입니다:

1. **`MemoryRepository.merge()`의 `Math.max` 집계**:
   - **현상**: 저장형 `MemoryRecord`의 집계 필드는 여전히 `Math.max(existing, incoming)`를 유지하고 있습니다.
   - **영향 평가**: 이번 1차 수정으로 홈 화면(`HomeRecentPlacesResolver`)과 상세 화면(`MemoryBrowserDetail`)은 실제 `MemoryMediaResolver`의 해석 결과를 직접 카운트하므로 **실제 사용자가 보게 되는 핵심 화면의 수량 불일치는 완전히 해결**되었습니다.
   - **판정: [Non-blocking Follow-up]**. 발견 목록(List)에서 미조회 상태의 `MIXED` 카드 수량을 완벽한 실시간 합집합으로 만들려면 목록 표시 단계에서 모든 앨범의 MediaStore 조회를 동기로 수행해야 하므로 이전 세션에서 달성한 홈/목록 진입 성능(~26ms)을 심각하게 퇴행시킬 위험이 있습니다. 따라서 현재의 1차 격리는 매우 훌륭한 엔지니어링 절충이며, 출시를 차단할 사유가 아닙니다.
2. **`discoveryMemories()` vs `memories()`의 소스 범위 차이**:
   - 발견 전용 화면과 정리된 앨범 포함 화면의 목적이 다른 도메인 분리이므로, 강제로 합치지 않은 현재 설계가 타당합니다.
3. **파일명 기반 교차 중복 판정**:
   - 다른 폴더에 동일한 파일명이 있을 때의 잠재적 중복 판정은 이번 패치에서 도입된 것이 아닌 기존 레거시 정책입니다.

---

### 3) 발견기록 확대 이미지 대기열 개선 (Viewer Performance)
**주요 대상**: `MemoryViewerImageLoader.java`, `LatestImageLoadQueue.java`, `MemoryPhotoViewer.java`

- **그리드 대기열과의 완전한 분리**:
  - 기존에는 상세 그리드 썸네일(최초 48장)을 디코딩하는 2-스레드 FIFO `thumbnailWorker`를 뷰어의 대형 이미지 디코딩도 공유하여, 사진 탭 시 그리드 작업이 끝날 때까지 뷰어가 대기하는 병목이 있었습니다.
  - `MemoryViewerImageLoader`에 **전용 단일 스레드(`newSingleThreadExecutor`)**를 도입하여 그리드 로딩과 뷰어 로딩의 대기열을 완전히 물리적으로 분리했습니다.
- **요청 세대(Generation) 기반 레이스 컨디션 차단**:
  - [`LatestImageLoadQueue.java#L20-L33`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/LatestImageLoadQueue.java#L20-L33): 사용자가 뷰어에서 사진을 빠르게 좌우로 넘길 때, `generation` 번호를 증가시키고 이전 디코딩 작업의 `CancellationSignal`을 즉시 호출합니다.
  - 백그라운드 디코딩이 뒤늦게 끝나더라도 현재 요청 세대와 일치하지 않으면 UI 스레드 반영(`ready.accept`)이 조용히 폐기됩니다.
- **사진 → 동영상 전환 시 화면 덮어쓰기 방어**:
  - [`MemoryPhotoViewer.java#L223`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryPhotoViewer.java#L223): 동영상으로 전환되거나 빈 항목으로 갈 때 `host.clearThumbnail(image)`를 호출하여 이전 사진 요청을 취소하고 이미지를 비웁니다.
  - 동영상 재생 화면 위에 뒤늦게 완료된 사진 비트맵이 덮어씌워지는 치명적 깜빡임/버그가 원천 방지되었습니다.
- **미리보기(Preview) 선표시를 통한 체감 지연 단축**:
  - 대형 이미지(최대 2048px)를 디코딩하는 동안, 그리드 캐시(`thumbnails`)에 이미 로드되어 있는 동일 URI의 작은 썸네일을 즉시 화면에 먼저 표시(`target.setImageBitmap(preview)`)한 뒤 고해상도로 부드럽게 교체합니다.
- **메모리(RAM) 안전성**:
  - 뷰어 전용 `LruCache`의 크기를 바이트 기준 **16 MiB (`16 * 1024 * 1024`)**로 엄격히 제한했습니다.
  - `options.inPreferredConfig = Bitmap.Config.RGB_565`를 적용하여 픽셀당 메모리를 ARGB_8888 대비 50% 절감했습니다.
  - 다운샘플링(`inSampleSize`)을 통해 2048px을 초과하는 대용량 원본 파일도 안전한 크기로 축소 로딩합니다.
- **생명주기 및 뷰 Detach 안전성**:
  - `View.OnAttachStateChangeListener`를 등록하여 뷰어가 닫히거나 뷰가 분리(`onViewDetachedFromWindow`)되면 즉시 활성 요청을 취소하고 참조를 정리합니다. Activity 종료 시 `close()`를 통해 스레드 풀을 안전하게 셧다운(`shutdownNow()`)합니다.

---

## 4. 발견 사항 상세 (Findings by Severity)

### 🔴 출시 차단 이슈 (Blocker)
- **0건 (없음)**

---

### 🟡 출시 전 실기기 확인 항목 (Pre-release Checks)
1. **많은 사진이 포함된 장소에서 뷰어 반응성 체감 확인**:
   - **확인 대상**: 사진 50장 이상이 포함된 장소 상세 화면에 진입하자마자 첫 번째 사진 탭 → 좌우 빠른 스와이프 → 동영상 항목으로 스와이프.
   - **체크 포인트**: 그리드 로딩 중에도 뷰어가 즉시 뜨는지, 작은 프리뷰에서 고해상도로 자연스럽게 전환되는지, 동영상 화면 위에 이전 사진이 번쩍거리며 덮어씌워지지 않는지 확인.
2. **성남/하남 장소의 홈 카드 vs 상세 헤더 수량 일치 확인**:
   - 홈 화면 카드의 수량과 상세 화면 상단 헤더의 수량이 동일하게 일치하는지 확인.
3. **(이전 회차 이월) 실제 GPS 사진 추가 시 성남 NEW 뱃지 점등 확인**:
   - 실외 촬영 또는 갤러리 위치 추가(성남)된 새 사진으로 '장소찾기' 실행 시 성남 카드에 `NEW` 뱃지 노출 확인.

---

### 🟢 후속 개선 과제 (Follow-up Tasks)
1. **발견 기록 목록(List)의 `MIXED` 카드 집계 2차 개선**:
   - 현재는 홈 카드와 상세 화면의 수량이 완벽히 일치하지만, 발견 목록 카드에서는 `Math.max` 기반의 저장형 집계가 남아 있습니다.
   - UI 스레드를 차단하지 않는 백그라운드 리비전 체계를 활용하여 목록 카드의 표시도 실시간 합집합으로 맞추는 작업 권장 (Phase 2).
2. **`MainActivity.java`의 기존 179개 `WrongConstant` lint 기술부채 정리**:
   - `root.setOrientation(1)`과 같은 기존 UI 상수들을 Android 정식 상수(`LinearLayout.VERTICAL`)로 치환하여 빌드 로그의 lint 경고/오류 정리.
3. **`MemoryMediaResolver` 파일명 기반 교차 중복 판정 고도화**:
   - 동일 파일명이 서로 다른 폴더에 존재할 경우를 대비하여 `mediaStoreId` 또는 파일 크기/수정일을 포함한 복합 식별자 도입 검토.

---

## 🏁 최종 종합 판정

### 1. 코드 반영(Merge) 판정: **[승인 (APPROVED)]**
- `MainActivity` 순감 301줄 유지 및 P0 0줄 격리 완벽 준수.
- P1 데이터 불변성 및 MediaStore 안전성 100% 보장.
- 260개 단위 테스트 전원 통과 (`PASS`), Git diff 포맷 오류 0건.
- 수량 표기 통일(사진/영상 분리) 및 뷰어 대기열 분리 구현 완성도가 매우 높음.

### 2. 전체 앱 출시 판정: **[조건부 출시 승인 (Conditional Release Approval)]**
- 코드베이스 자체는 즉시 릴리스 빌드를 생성할 수 있는 안정된 상태입니다.
- 위의 **[출시 전 실기기 확인 항목 3가지]**를 사용자의 실제 기기에서 눈으로 최종 확인하신 후 정식 릴리스 빌드(버전 코드 증가 및 AAB 생성)를 진행하시는 것을 최종 승인합니다.
