**요약**

- 이번 커밋은 Preview 완료 흐름을 Display-First 원칙으로 조정하고 `발견` 탭을 메인으로 승격시키는 변경입니다. 다이얼로그의 기본 동작이 `발견한 장소 보기`(primary)로, `앨범 만들기`는 보조 액션(secondary)으로 유지되어 요구사항을 충족합니다.

**확인사항**

- **Preview dialog (primary/secondary):** 기본 버튼은 `발견한 장소 보기`이며 보조 버튼이 `앨범 만들기`로 구현되어 있습니다. 동작은 [app/src/main/java/com/example/gallerysorter/MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java#L1420-L1468) 참조.
- **중복 CTA 숨김:** 미리보기 완료 후 홈의 대형 `앨범 만들기` CTA는 적절히 숨김 처리(`copyButton.setVisibility(8)`) 되어 중복 노출이 발생하지 않습니다 ([MainActivity](app/src/main/java/com/example/gallerysorter/MainActivity.java#L1996-L2040)).
- **결과 기반 앨범 생성 경로 유지:** 결과 화면과 `startCopyFromResultScreen()` 흐름은 유지되어 사용자가 결과에서 앨범 생성을 이어갈 수 있습니다.
- **그리드 분리:** `MemoryBrowserGridRenderer`가 카드 렌더링을 담당하고 `MainActivity`는 navigation/thumbnail wiring만 수행해 책임 분리가 적절합니다 ([MemoryBrowserGridRenderer](app/src/main/java/com/example/gallerysorter/MemoryBrowserGridRenderer.java)).
- **열 수/반응형 계산:** 열 분기 기준은 `>= dp(600) ? 3 : 2`이며 Worklog도 600dp로 정정되었습니다. 펼친 Galaxy Fold 실기기에서 3열을 검증한 제품 결정입니다 ([MemoryBrowserGridRenderer.columnCount](app/src/main/java/com/example/gallerysorter/MemoryBrowserGridRenderer.java#L1-L40)).
- **텍스트 잘림 처리:** 카드의 제목/날짜는 한 줄 `singleLine` + 말줄임 처리로 설정되어 있어 레이아웃 파괴는 방지되지만 긴 한국어 장소명은 잘림(말줄임) 됩니다. 필요 시 2줄 허용 등 UI 조정 권장.
- **하단 네비게이션:** `BottomNavigationRenderer`는 `홈 | 발견 | 위치 앨범 | 설정` 탭을 렌더링하고, 탭 선택은 `navigateToTopLevelTab()`에 의해 적절히 매핑됩니다. 상세 화면은 탭 하이라이트를 제거하도록 처리되어 있어 Back 흐름도 올바르게 동작합니다 ([BottomNavigationRenderer](app/src/main/java/com/example/gallerysorter/BottomNavigationRenderer.java) 및 [MainActivity 탭 처리](app/src/main/java/com/example/gallerysorter/MainActivity.java#L7768-L7848)).
- **Discovery 경계(데이터):** `MemoryRepository.discoveryMemories()`는 organized albums와 병합하지 않고 discovery-only 그룹만 반환합니다. 상세 조회는 `DiscoverySnapshotController.loadBrowserDetail()`에서 `repository.discoveryMemories()`를 통해 통일되어 있어 발견 UI가 gallery 앨범 수를 합산하지 않습니다 ([MemoryRepository](app/src/main/java/com/example/gallerysorter/MemoryRepository.java) 및 [DiscoverySnapshotController](app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java)).
- **썸네일 / 동영상 재생 배지:** `MemoryPhotoThumbnailRenderer`가 분리되어 있고 동영상일 경우 재생 배지(어두운 원형 배경 + 흰색 ▶)를 추가합니다 ([MemoryPhotoThumbnailRenderer](app/src/main/java/com/example/gallerysorter/MemoryPhotoThumbnailRenderer.java)).

**이슈 및 권고**

- **열 임계값:** 코드와 문서 모두 600dp로 통일되었습니다. 별도 코드 변경은 필요하지 않습니다.
- **최대 콘텐츠 너비(dp(600)) 정책 재검토:** 매우 넓은 화면(태블릿/펼친 폰)에서 중앙 고정 레이아웃으로 좌우 여백이 생깁니다. 의도된 동작인지 확인하고, 필요 시 최대 너비를 상향하거나 가변 레이아웃 정책을 적용하세요.
- **텍스트 정보 손실 우려:** 긴 한국어 장소명은 현재 한 줄 말줄임 처리로 노출 정보가 줄어듭니다. UX 중요도에 따라 2줄 허용 또는 폰트/패딩 조정 권장.
- **임시 버튼 연결 제거 확인:** 발견 상세의 `이 장소를 앨범으로 정리` 버튼이 전체 결과로 이동하는 임시 연결은 차후 제거 예정으로 문서화되어 있으니 코멘트/추적 이슈로 남겨두세요.
- **새 앨범 생성 전 사용자 확인:** 새 Gallery 앨범 생성은 반드시 사용자 확인 후 실행되도록 유닛/통합 테스트로 보장하세요.

**테스트 제안 (우선순위 높은 항목)**

- **UI 레이아웃 스모크 테스트:** 일반 폰(폭 약 360–420dp)에서 2열, 넓은 화면(≥600dp)에서 3열로 렌더링되는지 스크린샷 비교. 카드 이미지 크롭, 타이틀 말줄임, 빈 공간 확인.
- **Preview → 발견 흐름:** Preview 완료 다이얼로그의 primary 버튼(`발견한 장소 보기`)이 `showMemoryBrowserScreen()`을 호출하고, secondary(`앨범 만들기`)는 `startCopyFromPreviewContext()`를 호출하는지 확인(행동 기반 테스트).
- **Navigation / Back 흐름:** 발견 목록 → 상세 → 시스템 Back(또는 UI Back) 시 목록으로, 목록에서 Back 시 이전 top-level(주로 홈)으로 복귀하는지 확인.
- **Discovery vs Organized 경계:** `MemoryRepository.discoveryMemories()` 반환 단위 테스트로 discovery-only 레코드만 반환하는지, `MemoryRepository.memories()`가 병합된 결과를 제공하는지 검증.
- **Sort/Copy 견고성 테스트:** `SortWorker` 또는 백그라운드 정리 재시작/중단/실패/재시도 경로에서 `previewItems` 부재 처리, 히스토리 기록, 중복 검사 동작을 확인.

**다음 패치 제안(개요)**

- DiscoverySnapshot / previewItems 경계 문서화: discovery snapshot은 read-only view로서 UI만 소비하고, 실제 앨범 생성은 기존 `SortWorker`/`SortInputStore`를 재사용하도록 명확히 문서화.
- `SortWorker` 재사용 계획: 기존 백그라운드 정리 로직을 재활용해 사용자 확인 이후 album 생성 워크플로우를 트리거하도록 설계(중복 검사 및 실패 재시도 포함).
- UI 변경: 확정한 600dp breakpoint가 일반폰 2열/펼친 Fold 3열에서 유지되는지 회귀 검증.
- E2E 테스트 시나리오: Preview → 발견 탐색 → 전역 CTA(`발견한 장소 모두 위치 앨범으로 만들기`) → 사용자 확인 → `SortWorker` 실행 → 성공/실패/중단 케이스 검증.

**참고 코드 위치**

- Preview dialog 및 버튼 동작: [app/src/main/java/com/example/gallerysorter/MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java#L1420-L1468)
- 홈 CTA(복사/정리 버튼) 제어: [app/src/main/java/com/example/gallerysorter/MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java#L1996-L2040)
- Grid 렌더러: [app/src/main/java/com/example/gallerysorter/MemoryBrowserGridRenderer.java](app/src/main/java/com/example/gallerysorter/MemoryBrowserGridRenderer.java)
- Bottom tabs 렌더러: [app/src/main/java/com/example/gallerysorter/BottomNavigationRenderer.java](app/src/main/java/com/example/gallerysorter/BottomNavigationRenderer.java)
- Discovery 경계 및 병합: [app/src/main/java/com/example/gallerysorter/MemoryRepository.java](app/src/main/java/com/example/gallerysorter/MemoryRepository.java)
- Discovery 상세 로드 통일: [app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java](app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java)
- 썸네일 + 동영상 배지: [app/src/main/java/com/example/gallerysorter/MemoryPhotoThumbnailRenderer.java](app/src/main/java/com/example/gallerysorter/MemoryPhotoThumbnailRenderer.java)

## 후속 패치 리뷰 반영

- `MemoryBrowserSearch`가 발견 record만 대상으로 장소명, 국가 code/alias, 행정구역, 주소, 연도/월 검색을 담당합니다.
- `MemorySearchHeaderRenderer`가 발견 검색/닫기 아이콘과 입력창을 담당해 `MainActivity`에는 상태와 callback wiring만 남깁니다.
- `DiscoverySnapshotLiveFilter`가 snapshot의 MediaStore ID를 500개 단위로 조회하고 삭제·휴지통 항목을 read-time projection에서 제외합니다.
- LiveFilter는 JSON 원본을 즉시 덮어쓰지 않으며, 권한 또는 provider 조회 실패를 삭제로 간주하지 않고 기존 snapshot을 유지합니다.
- 삭제 항목 제외 후 group item/photo/video count, date range, cover를 다시 계산하고 빈 group은 발견 화면에서 제거합니다.

### 후속 검증 결과

- JVM unit test와 Debug build 성공.
- 실기기에서 Gallery 삭제 후 `성남 12개 -> 10개` 갱신 및 빈 썸네일 제거 확인.
- 검색 아이콘, 입력창, 닫기 및 `2026` 검색 결과 4개 확인.
- 국가 alias와 `8월` 검색은 단위 테스트로 확인.

### 남은 비차단 항목

- 수천~수만 개 snapshot에서 MediaStore 묶음 조회 진입 시간 측정.
- 실제 장소가 많은 사용자 데이터로 발견 검색 탐색성 검증.
- 날짜 그룹 단위 앱 내부 사진/동영상 viewer.
- 전역 `발견한 장소 모두 위치 앨범으로 만들기`와 WorkManager 연결.
