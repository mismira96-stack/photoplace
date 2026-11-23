# 발견기록 탭 진입 지연 — 수정 및 검증

- Codex,2026-10-19. 사용자 제보: 상세2,107개 목록뿐 아니라 발견기록 탭 자체도 느림.
- 상태: 소스 준비/모음 집계 비동기화, 중복 repository 읽기 제거, revision 재진입 캐시 구현. **284 tests PASS**, debug 빌드 PASS. 실기기 체감·외부 리뷰 전.

## 확인한 병목

showMemoryBrowserScreen이 UI 스레드에서 loadMemoryRepository를 수행하고, addMemoryCollectionCards가 다시 loadMemoryRepository를 호출했다. MemoryCollectionCardRenderer 내부 summarize도 각 모음 멤버의 실제 Gallery 사진을 읽어 수량을 계산했다. 탭 뼈대가 표시되기 전에 repository/live filter/앨범 조회 및 모음 집계가 반복될 수 있었다.

## 구현과 P0/P1

- MemoryBrowserLoadController / MemoryBrowserEntry: worker에서 repository1회로 장소 records/state와 모음 summaries를 준비. collections/aliases도 worker에서 읽음.
- MemoryCollectionCardSummary: 기존 summarize 로직을 순수 projection으로 추출. URI 중복/stale/빈 URI 제외, 실제 Gallery source 반영. 조회 실패나 멤버 identity 미해석은 incomplete로 두고 재사용 기준점으로 삼지 않음.
- MemoryCollectionCardRenderer는 renderPrepared만 받아 UI 표시. 내부 source 조회/집계 entrypoint를 제거하여 메인 스레드로 다시 들어갈 경로를 없앰.
- 기존 상세 로더 스케줄링을 MemoryScreenLoadController<T>로 추출. MemoryDetailLoadController는 상세 source/completeness를 공급하는 얇은 wrapper로 변경. 탭/상세 모두 한 건씩 key/scope/stable revision 캐시, 이전 root/요청 세대/라이프사이클 검사와 진행 안내를 공유.
- MemoryLoadNavigation은 로딩 중 Back을 기존 화면 종료 대신 로딩 취소로 소비. 탭 선택은 이미 표시 중인 Home을 다시 선택하는 경우에도 pending 요청을 먼저 취소하여 늦은 발견 탭 진입을 차단.
- MainActivity는 컨트롤러 생성/load/bind/cancel/close 및 UI callback만 추가. 기존 onBackPressed 판단은 handleBackNavigation으로 이동하고 pending 정책을 전용 클래스로 위임. 전체 working tree MainActivity +152/-415, 순감263줄. 새 계산을 MainActivity에 추가하지 않음.
- 미디어/메모/identity/OrganizationLink 원본 쓰기·삭제 없음. 기존 discovery-only/앨범 포함 source 범위, sort/NEW 정책을 변경하지 않음.

## 검증

- MemoryBrowserEntryTest5건: 모음의 사진/영상/중복/stale/빈 URI 집계, Gallery-only 멤버 실제수량/조회횟수, 실패/미해석 캐시금지와 복구, source 범위 및 모음 없을 때 Gallery 조회 안 함, 같은 revision warm 재진입 시 모음 Gallery 조회 반복 없음/변경 시 재조회.
- MemoryLoadNavigationTest3건: pending Back취소, 일반 Back보존, 현재 Home탭 재선택도 pending 취소.
- `gradlew.bat testDebugUnitTest assembleDebug` PASS(283개 시점), warm Gallery 조회 횟수 테스트1건 추가 후 `testDebugUnitTest` **284 tests / 0 failures / 0 errors**. 테스트 추가 뒤 제품 코드는 바뀌지 않아 전달 APK와 일치.
- `git diff --check` PASS. 기존 lint179 errors 부채는 별개이며 새 lint 전체 통과를 주장하지 않음.
- adb기기0대로 실제 Android View overlay/시스템 Back/탭반응성/새APK 설치 검증은 아직 없음.

## Pre-release 및 한계

- 첫 진입 source 시간 자체를0으로 만든 것은 아니며 기존 화면 반응/진행 표시와 warm 재사용 개선. UI의 카드 view 생성/검색·필터·정렬 및 썸네일 작업은 남음.
- PhotoPlaceBrowser / PhotoPlaceDetail의 sourceMs,uiWaitMs,bindMs를 분리. bind완료가 실제 첫 프레임과 같지는 않음.
- Home/탭/상세 worker에서 기존 단기 앨범 캐시 supplier를 공유하는 부분은 외부 리뷰 필요. source변경/권한/API29/unknown일 때는 재조회가 기본이며 source공급함수 자체의 기존 캐시정책을 변경하지 않음.
- 조회가 실패한 모음/identity 미해석 모음이 있으면 전체 탭 결과의 stable cache를 막으므로 다음 진입에도 source비용이 발생할 수 있음. 불완전한0개를 영구 캐시하는 것보다 정확성을 우선.
- View 재사용/RecyclerView 도입이 아니므로 장소 카드 수가 매우 많을 때 UI binding 비용은 별개.
- 모음 상세 화면 source 비동기화, 전체 수량 source 통일, 파일명 교차중복 정책은 별도 OPEN.

## 전달 APK

- `G:\내 드라이브\Codex APK\PhotoPlace-1.3.6-discovery-tab-20261019-debug.apk`
- 생성2026-10-19 **21:44:59 KST**,9,310,247bytes.
- SHA256 `E695E4875C5B51F0609637989E59234B7A946AF2E442084B630BCCD663C98C2B`, 로컬원본/Drive마운트 일치.
- 이전 APK는 유지. version1.3.6/debug, source추가수정/테스트284개와 제품코드 일치. 직접폰설치/앱데이터삭제/스토어배포 없음.

## 실기기 확인 순서

홈→발견기록 첫 진입 → 홈→발견기록 재진입 → 로딩 중 Home/다른탭/Back → 검색 표시/검색어/모음 만들기 → 성남 상세→확대→Back → Gallery변경 후 탭 재진입. 메모/모음 membership/NEW와 실제 수량 유지, 늦은 요청의 화면 덮어쓰기 없는지 확인.
