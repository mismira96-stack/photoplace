# PhotoPlace Discovery UI 및 앨범 정리 확인 흐름 종합 코드 리뷰

**일자:** 2026-09-13
**브랜치:** `codex/photoplace-v2-bg-wip`
**검토 기준 문서:** `REVIEW_REQUEST_Discovery_UI_Organize_Confirm_2026-09-13.md`
**작성자:** Antigravity

**최종 판정:**
- **이번 UI 및 확인 다이얼로그 개선 Checkpoint:** **최종 승인 (Final Approved - 레이아웃 밀도 최적화, 헤더 액션 통합, 동영상 차단벽 완벽)**
- **Play 프로덕션 정식 출시:** **보류 유지 (Hold - 다음 세션 Memory 미디어 resolver 및 내부 사진 뷰어 대기)**

---

## 1. 9대 검토 항목 심층 답변

### Q1. 검색창 상단 배치 및 상태/포커스/스크롤 보존 검증
- **판정: 완벽함 (Pass)**
- [`MainActivity.java:9020`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L9020)에서 `searchHeader.header`가 최상단에 배치되어 일관된 시각적 위계를 갖춥니다.
- 검색어(`memoryBrowserSearchQuery`) 상태와 포커스(`searchInput.requestFocus()`)가 온전히 유지됩니다.
- 검색 모드(`searching == true`)에서는 `hideCollectedMemoryRecords`가 예외 처리되어, 모음에 묶인 장소를 포함한 모든 발견 기록을 빠짐없이 검색할 수 있습니다.

### Q2. 기억 모음 및 개별 장소 액션의 스코프 및 도달 가능성 검증
- **판정: 매우 직관적이고 깔끔함 (Pass)**
- **기억 모음 섹션:** [`MemoryCollectionCardRenderer.java:42-64`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryCollectionCardRenderer.java#L42-L64)에서 `내 기억 모음` 헤더 우측에 `＋ 모으기` 액션이 배치되어 모음 카드가 있을 때나 없을 때나 자연스럽게 진입 가능합니다.
- **발견한 장소 섹션:** [`MainActivity.java:9574-9598`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L9574-L9598)에서 `발견한 장소` 헤더 우측에 `위치 앨범 만들기` 텍스트 버튼이 배치되어 기존의 거대한 독립 배너보다 정보 밀도가 크게 개선되었습니다.

### Q3. 장소 상세 타이틀 옆 콤팩트 `앨범 만들기` 액션 및 사전 확인 흐름
- **판정: 안전하고 비파괴적 (Pass)**
- [`MainActivity.java:9650`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L9650), [`createMemoryAlbumHeaderAction`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L9763-L9788)을 통해 장소 상세 상단 헤더에 단정하고 세련된 `앨범 만들기` 버튼이 위치합니다.
- 클릭 시 즉시 작업을 시작하지 않고 `showSingleMemoryOrganizeConfirmation()`을 통해 사전 비동기 준비(`DiscoveryAlbumOrganizer.prepare`) 및 중복 검사를 거쳐 확인 다이얼로그를 띄웁니다.
- 다이얼로그에서 "취소"를 누르면 `dialog.dismiss()`만 수행되며 어떠한 미디어/상태 변경도 발생하지 않습니다.

### Q4. 전체 중복(Duplicate-Only) 시 동작 검증
- **판정: 정확하고 정직한 처리 (Pass)**
- 모든 사진이 이미 정리된 경우(`countCopyableItems <= 0`):
  - 가짜 Worker를 실행하거나 가짜 `OrganizationLink`를 생성하지 않습니다.
  - `"모두 기존 위치 앨범에 정리되어 있어요."` 또는 `"새 사진은 이미 정리되어 있어요. 동영상은 Memory 화면 연결 준비 후 정리할 수 있어요."`라는 명확한 상황별 토스트를 띄우고 위치 앨범 탭(Tab 2)으로 안내합니다.

### Q5. 확인 다이얼로그 카운트 및 안내 문구 정확성
- **판정: 정확함 (Pass)**
- 사진 수, 제외되는 동영상 수, 이미 정리된 앨범 내 중복 수가 혼동 없이 명확히 구분되어 출력됩니다.
- *참고:* 중복 판정은 파일명 정규화 기반이므로 동명 이인 파일의 오탐 가능성이 있으나, 이는 기존 시스템의 공지된 정책/한계로 정상 관리되고 있습니다.

### Q6. 다이얼로그 구조 및 좁은 화면/대형 폰트 가독성
- **판정: 우수함 (Pass)**
- 다이얼로그 최대 폭이 `Math.min(dp(520), screenWidth - dp(36))`로 안전하게 클램핑되어 태블릿과 소형 폰 모두에서 여백과 줄바꿈이 안정적입니다.

### Q7. Discovery 동영상 이동 차단벽 유지 확인
- **판정: 100% 철통 방어 (Pass)**
- `OrganizationMediaPolicy.shouldMoveDiscoveryVideos()`가 무조건 `false`를 반환하며, `DiscoveryAlbumItemAdapter.toPhotoItems(..., false)`에서 `(item.video && !includeVideos)` 필터링을 통해 **Worker 입력(`previewItems`) 단계에서 동영상이 완전히 배제**됩니다.
- 홈(Home) 탭의 일반 폴더 정리는 이 경로를 타지 않으므로 전역 설정이 정상 유지됩니다.

### Q8. 사전 미디어 변경/삭제 및 조기 링크 기록 방지 검증
- **판정: 완벽함 (Pass)**
- 실제 Worker가 복사를 완료하고 검증된 성공 결과가 나올 때까지 어떠한 파일 이동, 삭제, 휴지통 액션, `OrganizationLink` 영속화도 발생하지 않습니다.

### Q9. 미흡한 테스트 및 기존 벌크 경로 회귀 검증
- **단위 테스트 제안:** `DiscoveryAlbumItemAdapterTest`를 추가하여 `toPhotoItems(items, false)` 호출 시 동영상 필터링이 누락되지 않음을 영구 보증하는 단위 테스트 추가 권장.
- **벌크 정리 회귀:** 벌크 정리 역시 동일한 사전 검사 및 동영상 제외 파이프라인(`startPreparedDiscoveryAlbums`)을 공유하므로 회귀가 없습니다.

---

## 2. 릴리즈 게이트 및 다음 세션 로드맵 확인

- **명문화된 출시 조건(Release Gate):**
  1. exact `OrganizationLink` 인식형 **공용 Memory Media Resolver** 구현.
  2. 같은 장소·날짜 범위의 미디어를 PhotoPlace 내부에서 스와이프 탐색하는 **인앱 날짜 사진 뷰어** 구현.
  3. 상세 화면 복귀 시 스크롤 위치 및 날짜 메모 유지 확인.
  4. 원본 휴지통 이동은 위 리졸버가 완전히 검증될 때까지 엄격히 보류 유지.

---

## 3. 검증 결과 요약

- **`./gradlew.bat testDebugUnitTest`:** **통과 (20 tasks, 4s)**
- **`./gradlew.bat assembleDebug`:** **통과 (최신 APK 빌드 완료)**
- **`git diff --check`:** **통과 (공백/포맷팅 결함 없음)**

오늘 진행된 리팩터링과 UI 개선은 기술 부채를 크게 덜어내고 앱의 완성도를 한 단계 끌어올렸습니다. 오늘 작업은 여기까지 마무리하시고 편안히 쉬셔도 좋습니다!

---

## 리뷰 후 보완 사항

- Antigravity가 권고한 `DiscoveryAlbumItemAdapterTest` 공백을 보완했다.
- 실제 `toPhotoItems` 변환 루프가 호출하는 `shouldInclude` 조건에 대해 Discovery 영상 제외, 사진 포함, 명시적 영상 허용 호환 동작, null/빈 URI 거부를 JVM 단위 테스트로 추가했다.
- 이 보완은 필터 조건을 테스트 가능한 순수 함수로 분리한 것이며, Gallery 처리 동작은 변경하지 않는다.
- 보완 후 `testDebugUnitTest`, `assembleDebug`, `git diff --check` 모두 통과했다. 테스트 대상은 worker input 필터 조건이며 UI/기기 동작은 바뀌지 않아 재설치하지 않았다.
