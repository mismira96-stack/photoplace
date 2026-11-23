# PhotoPlace Memory Collection UI 분리 및 Discovery 동영상 이동 차단벽 최종 코드 리뷰

**일자:** 2026-09-13  
**브랜치:** `codex/photoplace-v2-bg-wip`  
**검토 기준 문서:** `REVIEW_REQUEST_Collection_Selection_Video_Guard_2026-09-13.md`  
**작성자:** Antigravity / Gemini

**최종 판정:**
- **이번 변경 Checkpoint:** **최종 승인 (Final Approved - Renderer 분리 완벽, Worker 입력 동영상 100% 차단 확인)**
- **Play 프로덕션 정식 출시:** **보류 유지 (Hold - 공용 Memory Media Resolver 구현 및 실기기 스모크 대기)**

---

## 1. 코드 검토 결과 (Findings - 심각도 순)

### 1. [Low - 테스트 보강 권고] `DiscoveryAlbumItemAdapter` 단위 테스트 부재
- **위치:** `DiscoveryAlbumItemAdapter.java:18-28` (`toPhotoItems`)
- **내용:** `DiscoveryAlbumItemAdapter.toPhotoItems(preparedItems, includeVideos)`의 `(item.video && !includeVideos)` 필터링 로직이 이번 동영상 차단벽의 핵심 관문인데, 이에 대한 단위 테스트가 없습니다.
- **권고:** `includeVideos = false`일 때 동영상 아이템이 완전히 제외되고 사진만 남는지, `includeVideos = true`일 때 둘 다 유지되는지 검증하는 `DiscoveryAlbumItemAdapterTest`를 추가하면 영구적인 회귀 방어가 가능합니다.

### 2. [Low - 사소한 타입 안전성] `MainActivity.runCopy()`의 Raw List 타입 사용
- **위치:** `MainActivity.java:2043`
- **내용:** `final ArrayList arrayList = new ArrayList(this.previewItems);`에서 제네릭 타입 매개변수가 생략되어 있습니다.
- **권고:** `final ArrayList<PhotoItem> arrayList = new ArrayList<>(this.previewItems);`로 명시적 타입 지정을 권장합니다.

### 3. [Pass - 훌륭한 리팩터링] 3대 Renderer 분리 및 `MainActivity` 책임 축소
- **위치:**
  - `MemoryCollectionCardRenderer.java`
  - `MemoryCollectionSelectionRenderer.java`
  - `MemoryCollectionDetailRenderer.java`
- **평가:** UI 레이아웃 빌딩, 카드 스타일링, 원형 선택 인디케이터, 48장 페이징 그리드, 액션 버튼들이 전용 클래스로 완벽히 추출되었습니다. `MainActivity`에는 각 렌더러를 호출하고 리스너 콜백을 받는 10~15줄의 얇은 라우팅 코드만 남아 God Activity 문제가 깔끔하게 해소되었습니다.

---

## 2. Discovery 동영상 이동 차단벽 심층 검증 (핵심 검토 사항)

### 📌 검증 결론: **모든 발견(Discovery) 경로에서 Worker 입력 직전에 100% 완벽 차단됨**

사용자의 전역 `move_videos` 설정이 켜져 있든 꺼져 있든 상관없이, 발견 기록의 모든 정리 경로에서 동영상은 **Worker 입력(`previewItems` ➡️ `SortInputStore` ➡️ `SortWorker`)으로 전달되기 전에 완전히 제외**됩니다.

#### 1) 단일 Memory 정리 경로 (`showSingleMemoryOrganizeConfirmation`)
1. Memory 상세에서 "이 장소만 위치 앨범으로 만들기" 클릭.
2. `MainActivity.java:9414`: `DiscoveryOrganizeConfirmDialog.showSinglePlace`에 `moveVideos = false` 전달 ➡️ "동영상 N개 제외" 문구 정상 고지.
3. 확인 시 `startPreparedDiscoveryAlbums(preparation, stableMemoryId)` 진입 (`MainActivity.java:9468`).
4. `DiscoveryAlbumItemAdapter.toPhotoItems(preparation.items, OrganizationMediaPolicy.shouldMoveDiscoveryVideos(shouldMoveVideos()))` 호출.
   - `OrganizationMediaPolicy.shouldMoveDiscoveryVideos(...)`는 설정값에 무관하게 항상 `false` 반환.
   - `(item.video && !includeVideos)`에 걸려 **모든 동영상이 `previewItems` 생성 단계에서 완벽히 탈락**.
5. `this.previewItems`에는 **오직 사진만 적재**됨.
6. `runCopy(organizationRequest)` 호출 ➡️ Worker로 전달되는 `arrayList`에 동영상 0개 확인.

#### 2) 전체 발견 기록 일괄 정리 경로 (`showDiscoveryOrganizeConfirmation`)
1. 발견 브라우저에서 "발견한 장소를 위치 앨범으로 만들기" 클릭.
2. `MainActivity.java:9321`: `OrganizationMediaPolicy.shouldMoveDiscoveryVideos(shouldMoveVideos())` (= `false`) 전달 ➡️ "동영상은 Memory 화면에서도 계속 볼 수 있도록 이번 정리에서 제외됩니다" 문구 출력.
3. 확인 시 `startPreparedDiscoveryAlbums(preparation, "")` 진입 (`MainActivity.java:9448`).
4. 동일하게 `DiscoveryAlbumItemAdapter.toPhotoItems(..., false)`가 적용되어 **모든 동영상 배제**.
5. `this.previewItems`에 사진만 담기며, Worker로 전달되는 `arrayList`에 동영상 0개 확인.

#### 3) 동영상만 있는 장소 / 빈 항목 / 중복 케이스 방어
- **동영상만 있는 장소:** `previewItems.isEmpty()` 분기(`MainActivity.java:9480`)로 빠져 Worker를 아예 실행하지 않고 토스트 안내 후 안전 복귀.
- **사진은 모두 기존 앨범에 있고 동영상만 남은 경우:** `countCopyableItems <= 0` 분기(`MainActivity.java:9493`)에서 Worker를 실행하지 않고 상황별 토스트 고지 후 위치 앨범 탭으로 이동.

#### 4) 불필요한 권한 팝업 방어
- `needsVideoWritePermission()`(`MainActivity.java:1788`)은 `previewItems` 내의 실제 이동 대상 동영상을 검사합니다. 동영상이 이미 걸러졌으므로 불필요한 동영상 권한 요청 팝업이 뜨지 않습니다.

#### 5) 홈(Home) 탭 일반 폴더 정리와의 격리
- 홈 탭의 일반 폴더 정리는 `startPreparedDiscoveryAlbums`를 타지 않으므로, 사용자의 전역 `move_videos` 설정이 의도대로 유지됩니다.

---

## 3. 내부 뷰어(Internal Photo Viewer) 경계 확인

- `TODO.md`의 `P1. 날짜/장소 범위 PhotoPlace Photo Viewer` 항목에:
  - *"날짜 섹션의 썸네일을 누르면 chooser/외부 Gallery 대신 PhotoPlace 내부 뷰어를 기본으로 열고, 해당 place + date의 전체 미디어 목록을 source로 사용한다"*
  - *"Gallery에서 열기는 보조 액션으로 유지한다"*  
  가 정확히 명시되어 제품 방향에 완전히 부합함을 확인했습니다.

---

## 4. 검증 결과 요약

- **`./gradlew.bat testDebugUnitTest`:** **통과 (20 tasks, 4s)**
- **`./gradlew.bat assembleDebug`:** **통과 (최신 APK 빌드 완료)**
- **`git diff --check`:** **통과 (공백/포맷팅 결함 없음)**

| 구분 | 판정 | 설명 |
| :--- | :---: | :--- |
| **Renderer 분리 및 동영상 차단벽 Checkpoint** | **최종 승인 (Final Approved)** | `MainActivity` 슬림화 완료, Worker 입력 단 동영상 100% 누락 확인 |
| **Play 프로덕션 정식 출시** | **보류 유지 (Hold)** | Gallery output을 읽어오는 **공용 Memory Media Resolver** 구현 및 실기기 스모크 테스트 완료 시까지 릴리즈 보류 유지 |
