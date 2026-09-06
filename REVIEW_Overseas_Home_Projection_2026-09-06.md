# PhotoPlace Overseas Home Projection Review

작성일: 2026-09-06  
대상: `OverseasCountryProjection` 홈 연결, mixed-source 목록, 홈 성능 보완

검토 대상 커밋:

- `574fe02` 홈 해외 Memory projection 연결
- `3bff7c6` 홈 projection/live-filter 백그라운드 이동
- `5877a70` 국가 카드에서 discovery/organized 목록 함께 열기
- `830edfa` organized Memory의 국가 식별 보강
- `67f7c6b` 해외 카드 source summary 2줄 표시

## 변경 요약

- 홈 `해외 기록`의 입력을 기존 `AlbumSummaryHistoryStore` 단독에서
  `OverseasCountryProjection` 기반으로 확장했다.
- 발견 Memory가 있는 해외 국가는 Gallery 앨범을 만들지 않아도 홈 국가 카드에 표시된다.
- 발견 Memory가 있는 국가 카드를 누르면 해당 국가명으로 Memory 목록을 열어 장소 리스트를 보여준다.
- 국가 카드에서 진입한 목록은 discovery와 organized Memory를 함께 포함한다.
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

국가 카드 클릭:

```text
country card
  -> memoryBrowserSearchVisible = true
  -> memoryBrowserSearchQuery = countryName
  -> showMemoryBrowserScreen(includeOrganizedSources = true)
  -> MemoryRepository.memories()
  -> 국가명 검색 결과에 discovery/organized 항목 함께 표시
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

### 2026-09-06 홈 진입 지연 보완

실기기에서 해외 기록 연결 후 홈 진입이 느려지는 현상을 확인했다. 원인은 live filter의 MediaStore 청크 조회와
MemoryRepository 생성이 UI 흐름에서 동기 실행되던 점이었다.

- 홈 첫 프레임에서는 화면 골격만 먼저 표시한다.
- `loadRecentAlbumSummariesForUi()`, live filter, MemoryRepository 생성은 `worker`에서 실행한다.
- 같은 계산 결과의 `MemoryRepository`를 발견 둘러보기 CTA와 해외 기록 projection에 공유한다.
- 계산이 끝난 뒤 UI thread에서는 기존 섹션을 한 번에 바인딩한다.

`testDebugUnitTest`, `assembleDebug`, 연결 단말 설치 및 앱 실행을 다시 확인했다.

### 실기기에서 남은 성능 확인점

홈 재진입의 무거운 MediaStore 계산은 worker로 이동했지만, 최초 앱 진입이 여전히 느리다면 다음 두 구간을
분리해서 측정해야 한다.

- `buildUi()` 자체의 동기 뷰 생성/레이아웃 측정 시간
- worker 완료 후 해외 카드 썸네일과 섹션을 한 번에 바인딩하는 UI 시간

따라서 최초 진입 지연은 “live-filter가 UI thread를 막는 문제”와 동일하다고 단정하지 않고, 단말에서
첫 프레임 표시 시점과 projection 완료 시점을 각각 log/trace로 측정한다.

## Gemini 확인 요청

### Blocker 여부

- 홈 첫 진입 성능 저하가 남아 있는지
- discovery-only 국가가 홈에 정상 표시되는지
- organized-only 국가의 기존 상세 회귀 여부
- discovery와 organized가 모두 있는 국가에서 중복/카운트 혼동이 있는지
- 국가 카드 클릭 후 발견 검색 결과가 올바른 장소만 보여주는지

### 현재 의도된 제한

- 아직 stable Memory ID 기준의 discovery/organized 완전 dedupe는 하지 않는다.
- 국가 카드의 기존 해외 상세 화면에 discovery 섹션을 합친 통합 상세 화면은 아직 연결하지 않는다.
- Gallery 앨범 생성이나 파일 이동은 하지 않는다.
- 동일 국가의 발견 장소와 기존 위치 앨범은 현재 카드 내부 source count가 합산될 수 있으므로 UX 검토가 필요하다.
- 발견과 위치 앨범이 모두 있는 국가 카드는 이제 `MemoryRepository.memories()` 기반의 국가 검색 목록으로 진입한다.
  기존 전체 발견 탭은 여전히 discovery-only로 유지해 전역 CTA가 정리된 앨범까지 다시 처리하지 않도록 했다.
- 국가 검색 목록에서의 source 병합은 현재 기존 repository의 place/date fallback 규칙을 사용하며,
  완전한 stable-ID organization link 기반 dedupe와 통합 상세 화면은 다음 단계다.

## 다음 단계

1. Gemini 코드 리뷰 및 단말 3상태 확인
   - discovery-only
   - organized-only
   - both-source
2. 국가 카드 클릭을 기존 해외 상세 화면으로 바꾸고, 상세 안에 `새로 발견한 장소`와 `위치 앨범` 섹션을 함께 제공
3. stable Memory ID와 Gallery organization link를 연결해 중복 없는 lifecycle projection 구현
4. 이후 발견 기록의 MemoryCollection 선택/그룹 생성 UI 진행
