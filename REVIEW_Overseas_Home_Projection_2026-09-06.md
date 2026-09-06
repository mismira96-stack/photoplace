# PhotoPlace Overseas Home Projection Review

작성일: 2026-09-06  
대상: `OverseasCountryProjection` 홈 연결 및 discovery 진입 패치

## 변경 요약

- 홈 `해외 기록`의 입력을 기존 `AlbumSummaryHistoryStore` 단독에서
  `OverseasCountryProjection` 기반으로 확장했다.
- 발견 Memory가 있는 해외 국가는 Gallery 앨범을 만들지 않아도 홈 국가 카드에 표시된다.
- 발견 Memory가 있는 국가 카드를 누르면 해당 국가명으로 `발견` 목록을 열어 장소 리스트를 보여준다.
- 발견 Memory가 없는 국가, 즉 기존 위치 앨범만 있는 국가는 기존 Gallery 상세 흐름을 유지한다.
- 발견/정리 데이터는 projection 안에서 별도 source로 유지한다.
- Gallery 파일, MediaStore row, DiscoverySnapshot, 날짜 메모는 변경하지 않는다.

## 코드 경로

```text
MainActivity.loadHomeMemorySectionsAfterFirstDraw()
  -> already live-filtered homeAlbumSummaries
  -> DiscoverySnapshotController.repository(homeAlbumSummaries).discoveryMemories()
  -> OverseasCountryProjection.build(discoveryRecords, albumSummaries)
  -> home overseas country cards
```

발견 국가 카드 클릭:

```text
country card
  -> memoryBrowserSearchVisible = true
  -> memoryBrowserSearchQuery = countryName
  -> showMemoryBrowserScreen()
```

위치 앨범만 있는 국가 카드 클릭:

```text
country card
  -> OverseasMemoryGrouper.buildOverseasGroups(organizedAlbums)
  -> existing Gallery detail screen
```

## 검증 결과

- `testDebugUnitTest`: 통과
- `assembleDebug`: 통과
- 연결 단말 디버그 APK 설치: 성공
- AndroidX Startup 누락으로 발생했던 이전 시작 크래시: clean rebuild 후 재현되지 않음

## 성능 확인 및 보완

초기 구현에서 홈 첫 렌더 중 `loadMemoryRepository()`를 호출해 MediaStore live-filter가 중복 실행될 수 있었다.
현재는 이미 계산된 `homeAlbumSummaries`를 `DiscoverySnapshotController.repository(...)`에 재사용하여
홈 진입 중 MediaStore 전체 조회가 중복되지 않는다.

## Gemini 확인 요청

### Blocker 여부

- 홈 첫 진입 성능 저하가 남아 있는지
- discovery-only 국가가 홈에 정상 표시되는지
- organized-only 국가의 기존 상세 회귀 여부
- discovery와 organized가 모두 있는 국가에서 중복/카운트 혼동이 있는지
- 국가 카드 클릭 후 발견 검색 결과가 올바른 장소만 보여주는지

### 현재 의도된 제한

- 아직 stable Memory ID 기준의 discovery/organized 완전 dedupe는 하지 않는다.
- 국가 카드의 통합 날짜/장소 상세 화면은 아직 연결하지 않는다.
- Gallery 앨범 생성이나 파일 이동은 하지 않는다.
- 동일 국가의 발견 장소와 기존 위치 앨범은 현재 카드 내부 source count가 합산될 수 있으므로 UX 검토가 필요하다.

## 다음 단계

1. Gemini 코드 리뷰 및 단말 3상태 확인
   - discovery-only
   - organized-only
   - both-source
2. 국가 카드 상세를 `날짜 -> 장소 -> 메모 -> 사진` Memory projection으로 연결
3. stable Memory ID와 Gallery organization link를 연결해 중복 없는 lifecycle projection 구현
4. 이후 발견 기록의 MemoryCollection 선택/그룹 생성 UI 진행
