# 리뷰 요청 — 발견기록 탭 source/모음 집계 비동기화

저장소 `C:\work\projects\PhotoPlace`. AGENTS.md/GEMINI.md를 읽고, 결과를 **REVIEW_Discovery_Tab_Load_Antigravity_2026-10-19.md**에 저장해주세요. 이전260-test 승인은 이번284-test 패치의 승인이 아닙니다.

이번 작업은 REVIEW_Discovery_Tab_Load_2026-10-19.md에 상세 기록했습니다. 최신 working tree의 untracked 클래스/테스트도 직접 읽어주세요.

대상:

- 신규 MemoryBrowserLoadController / MemoryBrowserEntry / MemoryCollectionCardSummary / MemoryScreenLoadController / MemoryLoadNavigation.
- 변경 MemoryDetailLoadController(wrapper로 추출), MemoryCollectionCardRenderer(renderPrepared만), MainActivity(load/bind/lifecycle/Back 위임).
- 신규 MemoryBrowserEntryTest5건 / MemoryLoadNavigationTest3건 및 기존 MemoryDetailLoadSessionTest 회귀.

중점 검토:

1. P0 — MainActivity 신규 source/집계/캐시/Back판단 격리. 기존 판단 이동과 신규 정책을 구분.
2. 탭 source 준비와 모음 Gallery 집계가 worker에서 끝나고 renderer가 추가 쿼리하지 않는지. repository를 한 번만 사용하는지.
3. 같은key/scope/revision만 재사용, 실패/권한/unknown/미해석 멤버는 정상 기준점이 아닌지. source변경시 수량/NEW/멤버가 갱신되는지.
4. root이탈/다른탭/현재Home재선택/로딩중Back/빠른재요청/회전/앱전환 후 늦은 결과와 overlay누수 방어. 상세·탭 요청 서로 취소와 기존 viewer Back재사용 경로 회귀.
5. wrapper 추출 후 상세 async정책/진행표시/에러/캐시 한계 유지, 단기 앨범 캐시의 worker공유 상호작용.
6. 모음 membership·날짜·사진/영상·중복·실패표시·source범위 보존, 원본미디어/메모/identity/link 불변.

최종284 tests PASS, debugbuild/diffcheck PASS. 실제 폰의 latency/시각회귀는 미검증이며 cold source시간0 보장 아님. 기존 lint부채 존재.

결과는 Blocker/Pre-release/Follow-up으로 파일·줄·재현·영향·제안을 기록하고, 자동 검증/추론/실기기 미검증 및 코드반영/전체출시 판정을 구분해주세요.
