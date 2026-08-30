# 앨범정리 TODO

## Current Execution Plan (2026-08-24)

이 섹션이 현재 구현 순서의 단일 기준이다. 아래 날짜별 항목은 맥락과 세부 설계를 보존하는 참고용이다.

### P0 - 작은 UI 정리

- [x] 발견 기록과 위치 앨범 상단 영역의 배경색/여백 톤을 통일한다.
  - 두 탭을 오갈 때 상단 배경이 달라 보이지 않도록 같은 surface 정책을 사용한다.
- [x] 발견 상세 날짜 섹션의 빨간 세로 구분선을 앱의 purple accent로 교체한다.
  - 경고처럼 읽히지 않고, 발견/위치 앨범 전체 컬러 톤과 맞아야 한다.
- [x] 위치 앨범 헤더를 `위치 앨범 N개`로 변경하고 `총 N개 장소 발견` 표현을 제거한다.
- [x] 사진 수 표현을 `사진 N장`으로 통일한다. 사진/동영상 혼합 총계에는 필요한 경우에만 `항목 N개`를 사용한다.
- [x] 설정 최상위 화면의 Back 버튼을 제거한다. 하위 설정 화면만 Back을 유지한다.
- [x] 발견 상세 진입 중 Bottom Navigation의 `발견` 선택 상태를 유지한다.
- [x] 상세 하단 복귀 CTA를 `다른 장소 보기`로 변경해 상단 Back과 목적을 구분한다.
- [x] 발견 상세 상단 요약에서 중복된 `발견한 장소` 라벨과 설명 문구를 제거하고 사진 수·기간만 남긴다.
- [x] 발견 탭의 `발견한 장소를 위치 앨범으로 만들기` CTA는 현재 위치와 강도를 유지한다.
  - Display First, Organize Optional은 Organize Hidden이 아니다. 발견 후 바로 Gallery 정리를 원하는 사용자 흐름도 유지한다.
- [x] 발견 기록 상단의 안내 문구를 제거하고, 위치 앨범과 같은 정보 요약(`발견한 장소 N곳 · 사진 N장 · 기간`)으로 교체한다.

### P0 - 발견 기록 정합성 조사와 표시 개선

- [ ] 새 분석 뒤 같은 장소가 다시 `새로 발견됨`으로 보이거나, 새 장소가 0곳으로 보이는 재현을 먼저 코드/덤프 기준으로 확정한다.
  - source scope 병합, `duplicateInTarget`, snapshot merge/replace, live filter를 함께 점검한다.
- [x] 이번 분석의 새로 갱신된 장소 수를 기존 `placeKey` 존재 여부가 아닌 media URI 기준으로 계산한다.
  - 같은 URI를 다시 분석하면 0곳, 기존 장소에 새 URI가 추가되면 그 장소는 이번 분석 신규 항목으로 집계한다.
- [x] Discovery live filter의 `Pictures/*에서` 문자열 휴리스틱을 제거하고, `AlbumSummaryHistoryStore`에서 읽은 실제 위치 앨범 경로와만 일치시킨다.
- [x] 발견 카드에서 이번 분석에 새 media URI가 들어온 장소를 최상단에 노출하고 `NEW` / `이번에 +N장`으로 표시한다.
  - `NEW`는 완전히 새로운 placeKey에만 한정하지 않는다. 기존 장소에 새 사진이 추가된 경우도 표시한다.
  - `DiscoveryPhotoRef.firstSeenSnapshotVersion`을 URI별로 보존해, 같은 폴더를 재분석해도 기존 사진 전체가 NEW가 되지 않도록 한다.
  - 현재는 카드 정렬/배지로 노출한다. 별도 `이번에 사진이 추가된 장소` 섹션과 분석 완료 dialog의 장소 수·사진 수 분리는 후속 polish로 둔다.
  - 후속 테스트 보강: 동일 `+N장` 장소 간 최신 날짜 정렬, `ORGANIZED_ALBUM`/`MIXED` NEW 억제, URI 장소 재분류 뒤 first-seen 보존.
- [ ] 앱 삭제/데이터 초기화 후 discovery snapshot이 복원되지 않는 현재 한계를 안내하고, `발견 기록 다시 구성하기` UX를 설계한다.

### P1 - Incremental Analysis / Memory lifecycle

- [x] `MediaAnalysisStore`를 이미지 스캔에 연결해 이미 분석한 사진의 EXIF/Geocoder 재분석을 건너뛴다.
  - 현재 `ANALYZED`/`NO_LOCATION`, normalized location result와 media identity/signature를 저장한다.
  - 신규/복사/이동/변경/policy 변경/GPS 추가 미디어만 재분석한다.
  - cache hit도 발견/위치 없음 카운트와 Memory ref에는 포함한다.
- [x] `MediaAnalysisStore` 1단계: signature별 `ANALYZED`/`NO_LOCATION` 결과를 JSON 파일에 원자 저장·복원하는 독립 저장소와 단위 테스트를 추가했다.
  - 이미지 스캔은 cache hit에도 기존 `PhotoItem`을 만들며 결과 목록과 snapshot에 포함한다.
- [ ] 동영상 스캔에도 같은 캐시 계약을 별도 패치로 연결한다.
- [ ] retryable `FAILED` 상태와 재시도 정책은 실제 실패 원인/사용자 경험이 확인된 뒤 별도 설계한다.
- [ ] MediaStore reconciliation으로 외부 삭제/변경된 미디어를 live Memory에서만 제거한다.
- [ ] 현재 `NoLocationCache`는 이 설계와 단위 테스트가 완료되기 전 재활성화하지 않는다.

### P1 - 발견 기록에서 선택적으로 위치 앨범 만들기

- [ ] 발견 상세에 `이 장소를 위치 앨범으로 만들기` CTA를 추가한다.
  - 선택한 Memory 한 곳의 live discovery media만 기존 `SortInputStore` -> `SortWorker` 흐름으로 전달한다.
  - 확인창은 장소명, 사진/동영상 수, 이미 정리된 중복 제외 수를 보여주고 `원본은 유지돼요`를 명시한다.
  - 성공한 장소만 실제 `AlbumSummaryHistoryStore`에 기록하고 위치 앨범에서 보이게 한다.
- [ ] 현재 전역 `발견한 장소를 위치 앨범으로 만들기` CTA는 장소별 생성 UX가 실기기 검증된 뒤 `여러 장소 선택` 보조 동작으로 재검토한다.
  - 기본 흐름은 `발견해서 보기`이며, Gallery 앨범 생성은 사용자가 특정 기억에 대해 선택하는 행동으로 둔다.

### P2 - 원본 정리 이력과 선택적 Cleanup Handoff

- [ ] 사용자가 확인 후 실제 휴지통 이동/삭제를 완료한 원본 미디어만 별도 cleanup event로 기록하는 모델을 설계한다.
  - 후보/분석 결과 전체가 아니라 완료 이벤트만 기록한다.
  - 최소 식별값 후보: 삭제 시각, display name, MIME type, 촬영 시각, size, MediaStore id/URI(진단용), 정리 세션 id.
  - GPS, 주소, 개인 장소명은 기본 handoff 데이터에 포함하지 않는다.
- [ ] 향후 별도 Google Photos cleanup 앱과의 연결은 명시적 사용자 동의 기반의 export/import로 검토한다.
  - 두 앱이 내부 저장소를 직접 공유하지 않는다.
  - 외부 앱의 실제 Google Photos 라이브러리 접근 가능 범위와 정책은 구현 전에 별도 검증한다.

### P1 - 대용량 안정성 및 복구

- [ ] 10k+ 사진/수천 장 장소에서 상세 진입, 날짜 grouping, 스크롤, 메모리/OOM을 측정한다.
- [ ] 필요 시 visible cap을 제거하지 않고 lazy photo grid/paging으로 전환한다.
- [ ] 분석 중단 후 이어하기를 위한 checkpoint와 foreground/background 복구 정책을 설계한다.

### P1 - 날짜별 Memory Note (다음 구현 우선순위)

- [ ] **Phase 0 - stable memory key 계약을 먼저 확정한다.**
  - `MemoryPersonalizationKey.forSummary()`의 `relativePath` 키를 날짜 메모에 재사용하지 않는다. 물리 앨범 이동/통합 시 메모가 끊길 수 있다.
  - 발견 기록과 위치 앨범이 같은 장소를 가리킬 때 공유 가능한 논리 key를 `MemoryRecord.placeKey` / country / adminArea 기반으로 설계한다.
  - 장소명이 재분류되거나 country/admin 정보가 바뀐 경우의 alias/migration 정책을 먼저 문서화한다. 임의의 title 문자열 병합은 금지한다.
- [ ] **Phase 1 - `MemoryDateNoteStore` 저장 기반을 추가한다.**
  - key: `stableMemoryKey + dateKey(yyyyMMdd)`.
  - value: 한 줄 text, createdAtMillis, updatedAtMillis. 사진 원본/thumbnail은 저장하지 않는다.
  - 별도 `memory_date_notes.json`을 사용하고 tmp/bak 원자 저장, 손상 파일을 빈 값으로 덮어쓰지 않는 정책을 적용한다.
  - 저장/복원, 빈 메모 삭제, JSON 손상 backup 복구, stable key/date key 격리 단위 테스트를 먼저 작성한다.
- [ ] **Phase 2 - 발견 상세 날짜 섹션에서 작성/수정한다.**
  - 날짜 헤더 아래에 메모가 있으면 한 줄을 표시하고, 없으면 작은 `이 날의 기억 남기기` 액션만 노출한다.
  - 입력은 짧은 한 줄로 제한하고, 저장/수정/삭제가 명확히 구분되게 한다.
  - 사진 그리드/더 보기 paging과 독립적으로 동작해야 한다.
- [ ] **Phase 3 - lifecycle 회귀를 검증한다.**
  - 재분석, 동일 장소에 새 사진 추가, 사진 일부 live-filter 제외 뒤에도 메모가 유지돼야 한다.
  - 발견에서 위치 앨범 생성/이동한 뒤에도 같은 논리 장소·날짜의 메모가 이어지는지 검증한다.
- [ ] **Gemini 설계 리뷰 게이트**
  - stable key 충돌, discovery/organized alias, JSON 손상/저장 실패, 앨범 이동 뒤 메모 보존을 우선 검토받는다.
- [ ] **기억을 꺼내보기**: 날짜별 메모가 있는 사용자에게만 홈에서 다시 볼 수 있는 조건부 섹션을 제공한다.
  - 대표 사진, 장소명, 날짜, 메모 첫 줄을 표시하고 해당 Memory detail로 연다.
  - 메모가 하나도 없을 때는 홈에 빈 카드나 새 탭을 만들지 않는다.
  - 이는 날짜별 메모 MVP 검증 뒤에 진행하며, 독립적인 Memory Dashboard는 그 다음 단계로 보류한다.
- [ ] **발견 기록 가상 기억 통합**: 파일을 바꾸지 않고 여러 Memory를 사용자 이름으로 묶는 `MemoryCollection`을 구현한다.
- [ ] **위치 앨범 실제 통합**: 별도 기능으로, PhotoPlace 생성 앨범만 선택해 새 Gallery 폴더로 실제 이동한다.
  - 두 통합은 UI, 저장 모델, 실패/되돌리기 정책을 절대 공유하거나 혼동하지 않는다.

### 보류

- [ ] 복잡한 추천 시스템, 확장 Memory Dashboard, AI 전면 도입, 월/연도별 실제 Gallery 폴더 생성.
- [ ] 드래그 앤 드롭 통합. MVP는 long-press 다중 선택으로 유지한다.

## 2026-08-22 Display First 후속

- [x] 분석 완료 dialog의 primary action을 `발견한 장소 보기`로 변경.
- [x] Preview dialog의 legacy `앨범 만들기`를 제거하고, 앨범 생성은 발견 tab의 사용자 확인 CTA로 단일화.
- [x] 분석 직후 홈의 중복 대형 앨범 만들기 CTA는 숨김.
- [x] `발견한 장소 둘러보기`를 photo-first 카드 grid로 변경(일반 폭 2열, 넓은 화면 3열).
- [x] Memory detail에서 처음 48장을 빠르게 보여준 뒤 `사진 더 보기`로 다음 48장을 현재 화면 아래에 이어서 표시한다.
- [ ] 장기적으로 Memory detail을 수백/수천 장까지 안전하게 볼 수 있는 lazy date grid로 전환.
- [ ] 앱 내부 사진 viewer 또는 같은 장소 사진 간 자연스러운 좌우 탐색 제공.
- [x] 발견 장소 검색 추가. 1차 검색 대상은 장소명, 국가명/국가 alias, 도시명, 연도/월.
- [x] Memory Browser를 `발견` 메인 tab으로 승격하고 `정리 기록`을 `위치 앨범`으로 명확히 구분.
- [ ] 발견 탭 검색을 실제 장소 수가 많은 사용자 데이터로 탐색성 검증.
- [x] Gallery에서 삭제·휴지통 이동된 DiscoverySnapshot 항목을 발견 카드/상세에서 live 제외.
- [ ] 수천~수만 개 snapshot에서 MediaStore live-filter 진입 성능 측정.
- [x] 발견 탭의 전역 CTA를 `발견한 장소를 위치 앨범으로 만들기` flow로 연결.
  - 장소별 선택 UI는 MVP에서 제외하고 현재 DiscoverySnapshot에 있는 정리 가능 항목 전체를 대상으로 한다.
  - 실행 전 장소 수, 대상 수, 사진 복사/동영상 이동, 위치 없음 제외를 확인하는 dialog 제공.
  - 기존 duplicate/이미 정리됨 검사를 그대로 적용해 같은 항목을 다시 만들지 않는다.
  - 완료 후 `AlbumSummaryHistoryStore`에는 실제 성공 결과만 기록하고 `위치 앨범`에서 확인.
  - 앱 재시작 후 previewItems가 없을 때는 조용히 실행하지 않고 재분석 안내 또는 snapshot adapter를 사용한다.
  - 상세의 임시 `이 장소를 앨범으로 정리` 버튼은 제거한다.
- [x] 위치 앨범으로 이동된 동영상이 동일 MediaStore ID 때문에 발견 탭에 남는 문제 수정.
- [x] 발견 탭에서도 백그라운드 분석/정리 진행 배너 표시.
- [x] 기존 위치 앨범에 동일 파일이 있는 `duplicateInTarget` 항목은 발견 snapshot에서 제외. 기존 앨범에 없는 새 항목은 발견에 유지.
- [x] 분석 완료 후 홈 `자세히 보기`가 legacy 정리 결과가 아니라 발견으로 이동하도록 lifecycle 분리.
- [x] 분석 완료 dialog의 legacy 앨범/결과 CTA와 organizer 통계를 제거하고 발견 완료 UI로 단순화.
- [x] 홈의 legacy `새 장소 / 위치 없음 / 정리 완료` 요약 바 제거.
- [x] crash/업데이트 후 로컬 위치 분석 progress가 가짜 진행 상태로 복원되지 않도록 수정.
- [x] 서로 다른 분석 폴더를 순서대로 확인해도 기존 DiscoverySnapshot 기록이 덮어써지지 않고 URI 단위로 병합되도록 수정.
- [x] 발견 기록 카드에 이번 분석의 새 사진 여부를 `NEW`와 `이번에 +N장`으로 표시하고 상단 우선 정렬한다.
  - 기존 발견 장소는 아래에서 계속 탐색/검색 가능하다.
- [ ] 분석 완료 dialog와 홈 안내 문구에서 파일 수와 장소 수를 분리한다.
  - 예: `이번에 새로 발견한 장소 7곳 · 사진 174장`.
  - 위치 없음/이미 위치 앨범에 있는 파일 수는 보조 정보로 약하게 표기한다.
- [ ] 앱 삭제/데이터 초기화 후 발견 기록 복원 UX를 설계한다.
  - 위치 앨범은 MediaStore/정리 기록에서 다시 보이지만, discovery snapshot은 앱 내부 파일이라 삭제 시 복원되지 않는 현재 한계를 명시한다.
  - 설정에 `발견 기록 다시 구성하기`를 제공해 저장된 분석 폴더를 재분석할 수 있게 한다. 사진과 Gallery 앨범은 삭제하지 않는다는 안전 문구를 포함한다.
- [ ] 발견/위치 앨범이 공유하는 Memory detail stable key는 위 `날짜별 Memory Note` Phase 0에서 확정한다.
- [ ] 앨범 생성 후 발견 UI에서는 숨기되 snapshot/personalization 원본을 보존하는 lifecycle 회귀 테스트 추가.
- [ ] 발견 상세의 날짜 그룹 단위 앱 내부 사진/동영상 스와이프 viewer 추가.

### 제품 결정

- 반복 장소 추천은 당장 구현하지 않는다. 장소별 날짜 탐색과 검색만으로 충분한지 먼저 확인한다.
- 추천 데이터는 향후 `최근 다시 찾은 장소`, `여러 번 방문한 장소`, `1년 전 오늘` 같은 재발견 섹션 후보로 유지한다.
- PhotoPlace Memory 구조와 Gallery 앨범 구조는 동일할 필요가 없다.
- `발견`은 DiscoverySnapshot만 표시하고 Gallery 앨범 수/상태를 카드와 상세에 합산하지 않는다.
- Gallery 앨범 열기와 관리 책임은 `위치 앨범`에만 둔다. MIXED 상태는 내부 정합성 용도로만 유지한다.
- Organize Optional은 장소별 선택이 아니라 `앱 안에서만 보기`와 `발견한 장소 전체 앨범 생성` 중 사용자 선택을 뜻한다.
- 해외 Gallery 정리 단위는 국가/여행 세션/도시 중 어느 것이 좋은지 별도 POC한다. 현 단계에서 국가당 1앨범으로 고정하지 않는다.

### 상세 UI/UX 백로그 (현재 실행 순서는 상단 계획 우선)

#### 발견과 위치 앨범 역할 구분

- [x] 발견 tab의 `발견한 장소를 위치 앨범으로 만들기` CTA는 현재 위치/강도를 유지한다.
  - 앨범 생성에 익숙한 사용자가 발견 후 바로 정리할 수 있어야 하며, 현재 Memory Viewer 사용성도 확보됐다.
- [ ] 위치 앨범 header의 `총 N개 장소 발견`을 `위치 앨범 N개` 중심 문구로 변경한다.
- [ ] 사용자 노출 사진 수 표현을 `N개 사진`에서 `사진 N장`으로 통일한다. 동영상 혼합 시 `항목 N개`가 필요한 경계는 별도 확인한다.
- [ ] 설정 root 화면의 Back 아이콘을 제거한다. 설정 내부 하위 화면에만 Back을 유지한다.
- [ ] `발견 장소 다시 만들기`를 `장소 목록 다시 구성하기` 등 비파괴 의미가 분명한 문구로 검토하고 `사진과 앨범은 삭제되지 않아요` 설명을 추가한다.

#### P1 - Memory detail polish

- [ ] 발견 상세에서 bottom navigation의 `발견` 선택 상태가 유지되는지 회귀 확인한다.
- [ ] 상세 하단 `발견한 장소로 돌아가기`를 유지할 경우 `다른 장소 둘러보기`처럼 상단 Back과 다른 목적임을 명확히 한다.
- [ ] 상세 상단 요약 정보밀도를 축소한다.
  - 장소 제목과 `사진 N장 · 날짜 범위`만 우선 노출하고, 중복되는 `발견한 장소` label 및 설명 문구, 불필요한 `사진 보기` 섹션 제목은 제거/축소를 검토한다.
- [ ] 날짜 header accent를 경고처럼 보이지 않는 muted blue 또는 저채도 coral로 비교한다.
- [ ] 날짜별 한 줄 메모 UI는 상단 `날짜별 Memory Note` Phase 2에서 구현한다.

#### P0 - Media lifecycle / incremental analysis / Memory sync

- [ ] `LocationAnalysisCache` 단독 TODO를 V2.1 `MediaAnalysisStore`로 교체한다. 설계는 `MEDIA_LIFECYCLE_MEMORY_SYNC_DESIGN.md`를 따른다.
  - media identity/signature별로 `ANALYZED`, `NO_LOCATION`, retryable `FAILED`, 정규화된 위치 결과를 저장한다.
  - cache hit은 EXIF/Geocoder만 생략하며, `PhotoItem`/Memory/위치 없음 카운트를 숨기지 않는다.
  - 신규/복사/이동/변경/policy 변경/GPS 추가 미디어는 반드시 재분석한다.
  - 단위 테스트: 신규 사진 누락 금지, 수정/이동/복사 무효화, GPS 추가 재분석, 위치 없음 카운트 유지, 손상 복구, 정책 변경 무효화.
- [ ] 재분석과 분리된 MediaStore reconciliation을 추가한다.
  - indexed media ref가 실제로 존재하는지 먼저 확인하고, 삭제된 사진만 Memory live ref에서 제거한다.
  - 사진 0장 + 메모 없음은 정리 가능; 사진 0장 + 메모 있음은 메모를 보존하고 unavailable 상태를 표시한다.
- [ ] `발견 기록`과 `위치 앨범`을 동일 Memory의 다른 projection으로 재정의한다.
  - 발견 기록은 `새로 분석됨`/`아직 위치 앨범 없음` 필터이며, 앨범 생성이 Memory 자체를 삭제하지 않는다.
  - 장기 root tab 이름은 `기억`으로 검토하고, 내부 상단 섹션/필터에 `이번에 발견`, `정리 전`, `전체 기억`을 둔다.
  - 현재 `DiscoverySnapshotMapper`의 `duplicateInTarget` 제외 규칙을 즉시 뒤집지 않는다. 먼저 MediaAnalysisStore와 Memory projection 경계를 만든다.
- [ ] 현재 비활성 `NoLocationCache`는 `MediaAnalysisStore` 테스트가 갖춰지기 전 재활성화하지 않는다.
- [ ] 앱 삭제/데이터 초기화 후 `기억 다시 구성하기` UX를 추가한다. 선택 폴더를 재분석하며 사진/Gallery 앨범은 삭제하지 않는다.

#### P1 - 발견 기록의 가상 기억 통합 (사용자 이름 보존)

- [ ] **제품 결정**: 발견 기록에서는 실제 Gallery 파일을 건드리지 않는 가상 기억 통합을 제공한다.
  - 예: `삿포로에서`, `오타루에서`, `비에이에서`를 선택 → `2026 여름 일본 여행` 입력 → 발견 기록 안에서 하나의 기억 묶음으로 다시 본다.
  - `MemoryCollection`: stable id, user display name, member memory stable keys, createdAt, updatedAt.
  - 이름/메모/멤버 구성은 Gallery 앨범 생성, 이동, 삭제와 독립적으로 보존한다.
- [ ] 별도 JSON store + temp/bak 원자 저장 + 손상 파일을 빈 값으로 덮어쓰지 않는 정책을 구현한다.
- [ ] 발견 기록 카드 long-press → 다중 선택 mode → `기억으로 묶기` → 이름 입력 → 저장 UX를 구현한다.
- [ ] 저장한 묶음은 발견 기록 상단 `내 기억 모음` 섹션에 사용자 이름, 멤버 수, 대표 썸네일로 보여준다.
- [ ] 단위 테스트: 저장/복원, 이름 변경, 멤버 추가/제거, missing memory, corrupt JSON, Gallery/위치 앨범에 영향 없음.

#### P1 - 위치 앨범의 실제 통합 (사용자 이름 + Move)

- [ ] **제품 결정**: 위치 앨범의 통합은 가상 묶음이 아니다. 선택한 실제 Gallery 위치 앨범의 사진과 동영상을 사용자가 입력한 새 폴더로 **이동**한다.
  - 예: `삿포로에서`, `오타루에서`, `비에이에서` 선택 → `2026 여름 일본 여행` 입력 → `Pictures/2026 여름 일본 여행/`으로 이동.
  - 원본 사진은 이미 위치 앨범 복사본 정리 정책에 따라 별도로 존재할 수 있으나, 이번 기능은 **생성된 위치 앨범 안의 항목만** 다룬다.
  - 통합 후 `위치 앨범`에는 사용자가 입력한 새 이름의 실제 폴더가 남는다.
  - Memory의 위치/날짜/메모는 Gallery 경로 변경과 독립적으로 보존한다.
- [ ] **Phase 0 - 안전 계약과 범위 확정**:
  - 대상은 PhotoPlace가 생성·기록한 위치 앨범만 허용한다. 임의의 Gallery 폴더는 선택 대상으로 열지 않는다.
  - 새 target path가 기존 위치 앨범/선택한 source path와 충돌하면 시작 전 중단한다.
  - 동일 파일명 충돌, source album missing, 권한 부족, 일부 항목 실패의 정책을 명시한다.
  - 기존 `MediaCopyEngine`은 사진 복사/동영상 이동 용도이므로 통합에 직접 재사용하지 않는다.
- [ ] **Phase 1 - 통합 action 저장 모델과 순수 계획**:
  - `LocationAlbumMergePlan`: source relative paths, target relative path, selected album summary keys, media counts.
  - `LocationAlbumMergeAction`: action id/time, 항목별 original/target URI 또는 MediaStore id, 성공/실패 상태, source path.
  - 파일 기반 action store는 temp/bak 원자 저장을 사용하며, 작업 전 계획을 먼저 저장한다.
  - 단위 테스트: target path validation, source/target 중복 차단, 동일 파일명 충돌, empty/missing album, JSON 손상 복구.
- [ ] **Phase 2 - 단순 선택 UX (드래그는 보류)**:
  - `위치 앨범` 카드 long-press → 다중 선택 mode → 하단 `통합` → 새 앨범 이름 입력 → 명확한 이동 확인 dialog.
  - 확인 dialog에는 `사진/동영상 N개를 새 위치 앨범으로 이동합니다`, 기존 source 폴더가 비어질 수 있음, 원본 사진 삭제와 무관함을 명시한다.
  - 드래그 앤 드롭은 첫 MVP에 넣지 않는다. selection mode가 One UI와 Fold/일반 화면에서 더 예측 가능하다.
- [ ] **Phase 3 - 통합 전용 move engine + 기록 갱신**:
  - 사진/동영상 모두 MediaStore `relative_path`를 target으로 변경하는 `LocationAlbumMergeEngine`을 별도 구현한다.
  - 성공한 항목만 target summary에 반영하고, source summaries는 merged/empty 상태로 history에 남긴다.
  - 앱 포그라운드/백그라운드 중단, partial failure, 앱 재시작 후 action 복구를 처리한다.
  - 최소 rollback은 항목별 원래 `relative_path`로 되돌리는 명시 action으로 설계하고, 대용량 실기기 검증 후 노출한다.
- [ ] **Phase 4 - 실기기 검증**:
  - 2개 앨범의 사진만, 동영상 포함, 5개 앨범 대량, 파일명 충돌, 중간 중단, Gallery 외부 삭제 각각 확인.
  - 통합 후 Gallery/위치 앨범/발견 및 Memory detail의 thumbnail·검색·메모가 깨지지 않는지 확인한다.
- [ ] 10k 이상 discovery refs에서 live-filter, 검색, 전역 CTA prepare 시간과 메모리를 측정한다.
- [ ] 중단 후 처음부터 재분석하지 않는 checkpoint/이어하기를 별도 설계한다.

#### P2 - Empty-state illustration system

- [x] 발견 및 위치 앨범의 빈 화면에만 PhotoPlace illustration을 적용하고 실제 photo-first 화면과 tab icon에는 적용하지 않는다.
- [ ] 다음 실기기 확인에서 illustration의 입체감/테두리가 강하면 alpha와 asset style을 더 단순한 2D 선·면 중심으로 조정한다.

## 2026-08-16 긴급 추가 (요청: 발견 장소 다시보기 CTA 관련)

- 문제: 분석을 다시 돌리면 `발견한 장소 둘러보기` 대신 장소별 앨범 생성/정리 CTA가 다시 노출되는 현상 보고됨.
- 목표: 기본 흐름은 "발견한 장소 둘러보기(Discovery-backed preview)" → 사용자가 원할 때 `jump`로 앨범 생성/정리로 이동하도록 유지.
- 조치 항목:
  - [x] 재분석(re-run) 완료 후 discovery preview 진입을 primary로 제공하고 홈의 앨범 생성 CTA를 숨김.
  - [x] preview 완료 dialog에서 앨범 생성은 secondary 선택으로 유지.
  - 이 동작은 `MemoryRepository`/`DiscoverySnapshotController` 경계에서 보장되도록 단위 테스트 추가.
  - 스모크 테스트 항목에 "재분석 후 discovery-only preview 유지" 검증 케이스 추가.

## 2026-07-25 아침 실기기 체크리스트

- 설치된 APK:
  - `app/build/outputs/apk/debug/app-debug.apk`
  - 마지막 설치 확인: 2026-07-25 새벽, Galaxy 기기 `R5KYB048EER`
- 테스트 데이터:
  - 사용자가 테스트용 폴더를 남겨둠.
  - 약 4087개 파일이 중복 상태로 있어 `이미 정리됨`, 재탐색 비용, 결과 화면 성능 검증에 사용 가능.
- 먼저 확인할 것:
  1. 앱 첫 진입 속도
     - 기대: 홈은 즉시 뜨고, 해외 기록/최근 발견한 장소는 약간 뒤에 자연스럽게 표시.
     - 특히 `원본 사진 휴지통 이동` pending이 있어도 홈이 느려지지 않아야 함.
  2. 정리 진행률
     - 메인 진행 카드의 진행률/숫자가 계속 갱신되는지 확인.
     - notification 진행률은 너무 자주 깜빡이지 않고 1초/퍼센트 변화 정도로 갱신되는지 확인.
     - 알림 클릭 시 앱으로 진입하는지 확인.
  3. 결과 화면 성능
     - `결과 보기`, `확인 필요` 진입이 이전보다 괜찮은지 체감 확인.
     - 아직 4000장 previewItems를 메모리에서 그룹핑하므로 첫 진입이 약간 걸릴 수 있음.
  4. 결과 앨범 상세
     - 결과 화면에서 장소/앨범 row 클릭 시 대표사진 1장이 아니라 3열 사진 그리드가 보이는지 확인.
     - Back 또는 `결과로 돌아가기`가 결과 화면으로 자연스럽게 복귀하는지 확인.
     - 48개 초과 그룹에서 `먼저 48개만 보여줍니다` 안내가 어색하지 않은지 확인.
  5. 메인 UI
     - 메인 상단의 중복 `백그라운드 진행 중` 배너는 제거됨.
     - 작업 중 다른 페이지에서는 진행 배너가 계속 보이는지 확인.
  6. 장소명 회귀 확인
     - `송파동/장지동/Songpagu/Mapo-gu`가 새 앨범으로 갈라지지 않고 구 단위로 모이는지 확인.
     - `에버랜드`는 유지.
     - `봉은사 코엑스 북문`은 새로 다시 생성되는지 여부만 확인. 기존 앨범으로만 보이면 과거 테스트 흔적으로 분류.
- 4087개 테스트 폴더 검증 순서:
  1. 앱 첫 진입 시간 체감 확인.
  2. 테스트 폴더를 선택하고 `정리 시작`.
  3. preview 결과에서 `이미 정리됨` 개수가 대부분으로 나오는지 확인.
  4. `새 항목만 정리` 버튼이 보이면 실행 전 문구가 불안하지 않은지 확인.
  5. 결과 화면에서 `이미 정리됨`, `위치 없음`, `장소 리스트` 숫자가 납득되는지 확인.
  6. 장소 row 하나를 눌러 3열 사진 그리드 상세가 빠르게 뜨는지 확인.
  7. Back으로 결과 화면 복귀 후 홈/정리기록 전환이 버벅이지 않는지 확인.
  8. 같은 폴더로 한 번 더 `정리 시작`을 눌렀을 때 재탐색 시간이 얼마나 걸리는지 기록.
- 기록하면 좋은 숫자:
  - 앱 첫 진입 체감 시간.
  - preview 완료까지 걸린 시간.
  - 결과 보기 첫 진입 시간.
  - 같은 폴더 두 번째 preview 완료까지 걸린 시간.
  - 새로 생성된 이상한 장소명 목록.
- 자동 테스트:
  - `PlaceNamePolicyTest` 추가.
  - `MediaAnalysisSignatureTest` 추가.
  - `MovementClassifierTest` 추가.
  - `testDebugUnitTest assembleDebug` 통과.

## 남은 핵심 TODO 우선순위

### P0. 진짜 백그라운드 작업 소유권 이전

- 현재:
  - ForegroundService/notification/progress store는 있음.
  - 실제 scan/copy job은 아직 Activity worker 중심.
  - 앱 업데이트/프로세스 종료 시 작업 자체가 이어지는 것은 아니고 stale progress 복구만 함.
- 준비 완료:
  - `BACKGROUND_PROCESSING_PLAN.md` 추가.
  - `SortInputStore` 추가: Worker가 사용할 `previewItems` snapshot을 내부 JSON으로 저장/복원 가능.
  - 아직 production flow에는 연결하지 않음. 내일 WorkManager 연결 전 안전 발판.
- 다음 목표:
  - WorkManager 또는 ForegroundService가 실제 scan/copy job을 소유.
  - 앱을 나갔다 와도 작업 유지.
  - 앱 복귀 시 진행 중 작업에 attach.
  - 완료 notification + 앱 내부 결과 화면까지 연결.
- 주의:
  - 동영상 이동/SAF/MediaStore 권한 요청은 화면 상호작용이 필요하므로 preview/권한 단계와 실제 background execution을 분리해야 함.

### P0. 이미 정리된 파일 재탐색 비용 줄이기

- 현재:
  - 복사/이동 단계에서는 이미 정리된 항목을 중복 처리하지 않음.
  - 하지만 preview 단계는 전체 파일을 다시 훑어서 4000장 이상에서 시간이 걸림.
- 준비 완료:
  - `MediaAnalysisSignature` 추가.
  - 기존 `NoLocationCache`도 같은 signature builder를 사용하도록 정리.
  - signature 테스트로 파일 수정일, media type, source folder 변경 시 캐시 키가 달라지는지 검증.
- 다음 목표:
  - `AnalyzedMediaStore` 또는 `SortHistoryStore` 추가.
  - 원본 uri/displayName/dateModified/dateTaken/mediaType/sourceFolder/targetRelativePath 기반 signature 저장.
  - 수정되지 않은 파일은 EXIF/Geocoder 재호출 없이 이전 분석 결과 재사용.
- 주의:
  - `NoLocationCache` 회귀로 `정리할 항목 0개`가 발생했던 전력이 있으므로, 캐시 적용 전 테스트 케이스부터 만들 것.

### P1. 결과/정리 기록 탐색 UX 확장

- 결과 앨범 상세:
  - 현재는 previewItems 기반으로 최대 48개 썸네일 표시.
  - 다음은 paging/더 보기 및 실제 생성 앨범 MediaStore lazy load 연결.
- 정리 기록:
  - 사용자가 가장 많이 들어갈 가능성이 높음.
  - 최근 정리 기록을 날짜/장소/사진 수 기준으로 찾기 쉽게 개선.
  - “이미 정리됨”을 사용자가 이해할 수 있게 최근 작업 이력과 연결.

### P1. 위치 품질 / 이동 중 기록 표시

- 준비 완료:
  - `PlaceNamePolicy`로 장소명 정책을 `MainActivity`에서 분리.
  - 서울 로마자 구명, 서울 행정동, 강한 POI, 출입구/북문 노이즈, 밀리토피아시티 회귀 테스트 추가.
  - `MovementClassifier` / `MovementType` 추가.
  - altitude 또는 사진 간 거리/시간 기반으로 `STILL`, `MOVING`, `IN_FLIGHT`를 판별하는 테스트 추가.
- 비행 중 사진:
  - 자동 제외하지 말고 `비행 중` 또는 `이동 중`으로 표시하는 방향.
  - 후보 신호: altitude, 시간 간격, GPS 점프 거리/속도.
- 다음 연결:
  - EXIF altitude 읽기 추가 가능 여부 확인.
  - 같은 촬영 세션 내 이전/다음 GPS와 시간 차이로 이동 속도 계산.
  - 해외 기록/장소 상세에 badge만 표시하고 자동 제외는 하지 않음.
- 차량/기차 이동:
  - 자동 제외는 위험.
  - 먼저 표시/분류만 하고 사용자가 이해할 수 있게 만드는 쪽.
- POI:
  - `에버랜드`, `롯데월드`, 공항 같은 강한 장소만 우선 유지.
  - 역/게이트/출입구/상가/지하 같은 후보는 과분류 방지.

## 2026-07-25 4000장 실기기 테스트 후 다음 Step

- 현재 브랜치: `codex/photoplace-v2-bg-wip`
- 확인:
  - 다운로드 테스트 폴더 약 4000장 preview/정리 완료.
  - 완료 notification 수신 확인.
  - 알림 클릭 시 앱 진입 확인.
  - 정리 중 APK 재설치/앱 재시작으로 남은 stuck progress를 복구하는 안전망 추가.
  - `active=false`로 완료 상태가 내려가는 것 확인.
  - `송파동/장지동` 같은 서울 행정동은 `송파구`로 모이는 방향 확인.
  - `강남구` fallback 확인.
  - `에버랜드`는 의미 있는 대표 POI로 감지 확인.
  - `봉은사 코엑스 북문`은 기존 앨범으로 남아 보임. 새로 생성되는지 여부는 추가 확인 필요.
- 사용자 테스트 메모:
  - 앱 진입이 오래 걸림. 원인은 정리 후 `원본 사진 휴지통 이동` pending URI 3600개가 SharedPreferences에 저장되어 홈 진입 때 매번 XML/JSON/Uri 파싱을 하던 문제로 확인.
  - `확인 필요` / `결과 보기` 화면 진입도 오래 걸림.
  - 결과 화면에서 `정리될 앨범`을 누르면 대표사진만 보여 아쉬움. 사용자는 해당 장소의 사진 전체를 보고 싶어함.
  - 정리 중 `멈추기`를 누르고 다시 시작하면 처음부터 전체 탐색을 다시 하는 점이 체감됨.

### P0. 앱 진입 / 확인 필요 / 결과 보기 성능 개선

- 완료:
  - 홈 summary 로딩은 저장된 정리 기록 JSON 우선으로 변경해 기존 앨범 MediaStore 재스캔을 줄임.
  - 결과 화면 초기 렌더링 수를 줄임:
    - 앨범 그룹 `60개 -> 24개`
    - 위치 없음 상세 `120개 -> 60개`
  - 홈의 해외 기록/최근 발견한 장소 섹션은 첫 화면 표시 후 지연 렌더링.
  - 원본 휴지통 이동 pending URI 목록을 SharedPreferences에서 내부 파일 `pending_original_cleanup.json`으로 이동.
  - 홈 진입 시에는 URI 전체를 읽지 않고 pending count만 읽음.
  - 기존 SharedPreferences pending URI 데이터는 자동 마이그레이션.
  - 테스트 기기에서 `album_sorter.xml`이 약 229KB에서 277B로 감소.
  - 마이그레이션 후 cold start 측정값이 약 175ms로 개선됨.
  - 결과 화면의 장소/앨범 row를 누르면 대표사진 1장 대신 앱 내부 사진 그리드 상세로 진입.
  - 결과 상세는 현재 previewItems에서 필요한 그룹만 다시 묶어 최대 48개 썸네일을 먼저 표시.
- 문제:
  - `확인 필요`, `결과 보기`는 아직 4000장 previewItems가 메모리에 있을 때 최초 그룹핑 비용이 남아 있을 수 있음.
- 목표:
  - 결과 화면은 그룹 summary부터 빠르게 표시하고 상세 렌더링은 필요할 때 나눠 처리.
- 구현 후보:
  - 결과 리스트는 상위 그룹만 먼저 렌더링하고, 스크롤/상세 진입 시 lazy load.
  - 썸네일 로딩 throttling/cancel 처리.

### P0. 이미 정리된 파일 재탐색 비용 줄이기

- 현재:
  - 복사/이동은 중복을 피하지만, preview 스캔은 처음부터 다시 훑음.
  - `멈추기` 후 다시 시작하거나 이미 정리된 폴더를 재테스트할 때 시간이 오래 걸림.
- 목표:
  - 이미 정리된 항목은 preview 초반에 빠르게 판별.
  - 이전 분석 결과를 재사용해 EXIF/Geocoder 호출을 줄임.
- 주의:
  - 위치 없음 cache에서 `정리할 항목 0개` 회귀가 있었으므로 캐시 무효화 조건과 테스트 케이스를 먼저 만든 뒤 적용.

### P0. 진짜 백그라운드 처리 연결

- 현재:
  - ForegroundService/notification/progress store 기반은 있음.
  - 실제 정리 소유권은 아직 Activity worker 중심.
  - 앱 업데이트/프로세스 재시작 시 작업 자체는 이어지지 않고, stale progress 복구만 함.
- 목표:
  - WorkManager 또는 ForegroundService가 실제 scan/copy job을 소유.
  - 앱을 나가도 진행 유지.
  - 앱 복귀 시 진행률과 결과에 attach.
  - 완료 notification은 최종 결과 요약을 표시.
- 주의:
  - SAF/MediaStore 권한, 동영상 이동 권한 요청은 사용자 상호작용이 필요하므로 작업 전 preview/권한 단계와 분리 설계 필요.

### P1. 결과 앨범 상세 View

- 완료:
  - 장소/앨범 그룹 클릭 시 앱 내부 상세 화면 표시.
  - 대표사진 1장이 아니라 해당 그룹 사진 그리드 제공.
  - 날짜 범위, 전체 개수, 동영상 badge 표시.
- 다음:
  - 48개 이후 항목을 더 보기/paging으로 확장.
  - 실제 생성 앨범 상세는 MediaStore lazy load와 연결.

### P1. 장소명 / POI 정책 추가 정리

- 현재 정책:
  - `에버랜드`, `롯데월드` 등 강한 목적지는 POI 유지.
  - `북문/입구/출입구/gate`, `지하`, `상가`, `오피스텔`, `아파트` 등은 POI 후보 제외.
  - 서울 행정동/법정동은 `구` fallback으로 모음.
- 추가 확인:
  - `봉은사 코엑스 북문`이 새 앨범으로 다시 생성되는지 확인.
  - 기존 앨범으로만 보이면 과거 테스트 흔적으로 보고, 향후 병합/정리 관리에서 처리.
  - 일반 지하철역(`장지역`)을 POI에서 제외할지 결정. 큰 역/공항은 allowlist 후보.

### P1. 알림 진행률 throttle

- 완료:
  - 앱 내부 진행률/ProgressStore는 계속 갱신.
  - 시스템 notification은 시작/완료, 퍼센트 변경, 또는 1초 경과 기준으로만 갱신.
- 내일 확인:
  - 4000장 테스트에서 notification progress가 너무 뜸하거나 과하게 튀지 않는지 확인.

## 2026-07-24 백그라운드 테스트 후 남은 TODO

- 현재 브랜치: `codex/photoplace-v2-bg-wip`
- debug APK를 폰에 설치해 4000장 테스트 준비 완료.
- `assembleDebug`, `assembleRelease` 성공.

### P0. 이미 정리된 파일 재스캔 비용 줄이기

- 현재 상태:
  - 다시 정리를 돌리면 복사 단계에서는 `이미 정리됨`으로 중복 복사를 피한다.
  - 하지만 스캔/위치 분석은 전체 파일을 다시 훑기 때문에 4000장/30000장 사용자에게 여전히 느리다.
- 개선 방향:
  - “이미 정리된 파일”을 스캔 초반에 빠르게 판별하거나 이전 분석 결과를 재사용.
  - 후보 상태를 명확히 분리:
    - `새로 정리할 항목`
    - `이미 정리된 항목`
    - `위치 정보 없음`
    - `실패/건너뜀`
  - 이미 정리된 항목이 대부분이면 CTA를 숨기거나 `새 항목만 정리`로 표시.
  - 이 작업은 정리 이력/최근 정리 기록 UX와 연결됨.
- 설계 후보:
  - `SortHistoryStore` 또는 `AnalyzedMediaStore` 추가.
  - 키 후보: 원본 uri, displayName, dateModified, dateTaken, media type, targetRelativePath.
  - 파일 수정/권한 변경/소스 폴더 변경 시 캐시 무효화 조건 필요.
  - 기존 `NoLocationCache`는 현재 비활성화 상태이므로 같은 실수를 반복하지 않게 invalidation 테스트 먼저 필요.

### P0. 알림 진행률/진입

- 완료:
  - 알림 클릭 시 앱 진입 `PendingIntent` 추가.
  - WorkManager `SystemForegroundService` manifest `foregroundServiceType="dataSync"` override 추가.
  - release lint 통과 확인.
- 남은 확인:
  - 4000장 테스트 중 알림 진행률이 실제로 실시간 갱신되는지 확인.
  - 앱 복귀 시 `SortProgressStore` 값이 홈 진행 UI에 자연스럽게 복원되는지 확인.

### P1. Back 키 불응 점검

- 작업 중 Back 키가 여러 번 눌러도 안 먹는 경우가 있음.
- 확인 지점:
  - `onBackPressed()`
  - `OnBackInvokedCallback`
  - `isWorking`
  - `blockNavigationWhileWorking()`
  - 정리 기록 탭과 홈 복귀 흐름
- 기대 동작:
  - 정리 중에는 명확한 안내를 보여주고 작업은 유지.
  - 정리 완료 후에는 이전 화면/홈 복귀가 예측 가능해야 함.

## 2026-07-23 V2 1차 시작 - 해외 기록 / Memory View

- 완료:
  - 홈을 `Memory Dashboard` 방향으로 조정.
  - `해외 기록` 섹션을 홈 첫 화면에 노출.
  - 해외 기록 카드는 최근 발견한 장소 카드와 같은 형태로 구성:
    - 작은 썸네일
    - 국가명
    - 사진 수
    - 월 단위 날짜 범위
  - 해외 기록 카드를 누르면 앱 내부 filtered memory view로 이동.
  - 기존 파일 이동/복사 구조는 유지하고, view만 앱 내부에서 제공.
  - `StoredAlbumSummary` / `album_summary_history.json`에 확장 필드 추가:
    - `countryName`
    - `adminArea`
    - `addressLine`
  - 기존 사용자 마이그레이션:
    - 설정의 `발견 장소 다시 만들기`로 기존 `Pictures/*에서/` 앨범을 재스캔.
    - 업데이트 후 권한이 있으면 자동 1회 백그라운드 백필 실행.
    - 기존 영어/한글 해외 장소명 fallback 매핑 추가: 일본, 호주, 괌 등.
  - `MemoryItem`, `MemoryGroup`, `OverseasMemoryGrouper`를 별도 파일로 분리.
  - 위치 없음 1차 캐시 구현:
    - `NoLocationCache` 별도 파일 추가.
    - 파일 signature는 `uri + 파일명 + date_modified + date_added + datetaken + 사진/동영상 여부`.
    - 같은 파일이면 다음 스캔에서 EXIF/비디오 위치 읽기를 생략하고 `위치 없음` 처리.
    - 파일이 수정되면 signature가 달라져 자동 재검사.
  - 홈/정리기록 전환 버벅임 완화:
    - `loadRecentAlbumSummariesForUi()` 30초 메모리 캐시 추가.
    - 홈에서 해외 기록/최근 장소가 같은 summary 목록을 재사용.
  - `정리할 항목 0개 + 위치 없음만 있음` 상태에서 홈의 `확인 필요` 카드가 계속 남지 않도록 숨김.
  - 시작 카드 높이와 홈 여백 축소로 해외 기록이 첫 화면에 들어오도록 조정.
- 검증:
  - `assembleDebug` 성공.
  - 연결된 Galaxy 기기에 debug APK 설치 성공.
  - 홈 첫 화면에서 해외 기록 3개 국가 표시 확인: 일본, 호주, 괌.
  - 정리기록 탭 진입 및 시스템 Back 복귀 확인.
- 남은 확인:
  - 실제 기존 사용자 업데이트 흐름에서 자동 1회 백필이 너무 오래 걸리거나 첫 화면을 방해하지 않는지 확인.
  - `발견 장소 다시 만들기` 후 해외 기록 국가/날짜/개수 갱신 확인.
  - 국가 fallback 매핑이 국내 로마자 장소(`Mapo-gu`, `Songpa-gu` 등)를 해외로 오인하지 않는지 추가 확인.
  - 위치 없음 캐시가 권한 변경 또는 사진 메타데이터 변경 후 재검사되는지 확인.

## 다음 큰 작업 후보

### P0. 정리 작업 백그라운드 안정화
- 사용자 피드백: 앱을 나갔다 오면 정리가 끊겨 다시 돌려야 한다고 느낌.
- 현재 구조:
  - 정리 작업이 `MainActivity` 내부 `ExecutorService`에서 실행됨.
  - `onDestroy()`에서 worker가 종료될 수 있어 장시간 정리에 취약.
- 권장 방향:
  - 정리 작업 로직을 `MainActivity`에서 분리.
  - `ForegroundService` + notification 진행률로 이동.
  - 앱 복귀 시 진행 중 작업 상태에 다시 attach.
  - 이미 정리된 항목은 기존 중복/target 체크로 이어서 진행.
  - 원본 사진 휴지통 이동은 사용자 확인이 필요한 단계이므로 앱 화면에서만 처리.
- 주의:
  - 작은 패치로 넣기보다 구조 분리 작업과 함께 별도 브랜치/패치로 진행.
  - `SortJob`, `SortProgress`, `AlbumSummaryStore`, `NoLocationCache` 같은 단위 분리 검토.

## 2026-07-08 V1.1.10 진행
- 완료:
  - 정리 완료 후 원본 사진 URI 목록을 저장해 앱 재시작 후에도 `원본 사진 휴지통으로 이동`을 이어서 표시.
  - 지난 정리 원본만 별도로 처리하는 결과 화면 추가.
  - 휴지통 이동 전 이미 사라진 원본 URI는 제외하고 남은 원본만 처리.
  - 원본/중복 오해를 줄이도록 휴지통 이동 문구와 결과 요약 문구 개선.
  - 비서울 POI 장소명 후보 보강: `분당서울대학교병원` 같은 병원/대학교/공원/역 후보 우선.
- 검증:
  - `assembleDebug` 성공.
  - `bundleRelease` 성공.
- 남은 확인:
  - 실제 기기에서 정리 완료 후 앱 종료/재실행 시 원본 휴지통 카드가 유지되는지 확인.
  - 원본 휴지통 이동 승인/취소 후 상태가 기대대로 유지되는지 확인.
  - 분당서울대학교병원 케이스가 실제 Geocoder 결과에서 `분당서울대학교병원에서`으로 나오는지 확인.

## 2026-07-11 주말 UX 마무리
- 완료:
  - 메인 상단 `새 장소 / 위치 없음 / 정리 완료` 통계 블록을 각각 탭 가능한 보기 진입점으로 변경.
  - `보기` 칩 크기를 키워 버튼처럼 인지되도록 개선.
  - `위치 없음` 보기에서 샘플 1장만 보이는 문제 개선: 총 개수 요약 + 실제 항목 리스트 표시.
  - 위치 없음 항목은 최대 120개까지 안정적으로 표시하고, 나머지는 추가 개수로 안내.
  - 결과 화면 제목/설명 문구를 포커스별로 분리: `장소 리스트`, `위치 없는 사진`, `생성된 앨범`.
  - 기억 편집 팝업을 기본 AlertDialog에서 앱 카드 스타일 Dialog로 교체.
  - 원본 사진 휴지통 이동 확인창을 앱 카드 스타일 Dialog로 교체.
  - 분석할 폴더 선택 팝업 하단 버튼 스타일을 앱 Dialog 버튼과 맞춤.
  - 홈 하단 결과 카드 문구를 짧게 정리하고 `자세히 보기` CTA를 추가.
- 검증:
  - `assembleDebug` 성공.
- 다음 확인:
  - 실제 폰에서 위치 없음 2천 개 이상 케이스가 버벅이지 않는지 확인.
  - `보기` 칩 크기/터치감이 충분한지 확인.
  - 메모 편집/휴지통 확인 팝업이 작은 화면에서 잘리지 않는지 확인.
- 보류:
  - 위치 없음 skip/cache는 핵심 위치 분석 경로에 영향이 있어 이번 주말 APK에서는 제외.
  - 다음 패치에서 파일 ID/수정시간/권한 변경 기준의 캐시 무효화 조건을 먼저 설계한 뒤 적용.

## V1.1.9 후보

### 1. 장소명 품질 개선: 분당서울대학교병원 `대병원에서` 이슈
- 다음 패치에 우선 검토한다.
- 목표: `대병원에서`처럼 잘린/어색한 장소명이 아니라 `분당서울대학교병원에서`처럼 사용자가 알아볼 수 있는 앨범명을 선택.
- 구현 후보:
  - 비서울 주소에서도 `featureName`, `subLocality`, `thoroughfare`, `addressLine` 후보를 함께 평가.
  - `병원`, `대학교`, `공원`, `역`, `공항`, `미술관`, `박물관`, `예술의전당` 등 POI성 단어가 있는 후보를 우선.
  - 너무 짧은 조각, 행정구역만 남은 조각, 의미 없는 잘린 이름은 제외.
- 테스트 케이스:
  - 분당서울대학교병원 -> `분당서울대학교병원에서`
  - 서울 구 단위/동 단위 사진 기존 결과 유지
  - 일반 비서울 지역 사진이 너무 세부 POI로 과하게 바뀌지 않는지 확인

### 2. Dialog UI Polish
- 분석할 폴더 선택 팝업을 앱 카드 스타일로 교체.
- 기억/메모 편집 팝업을 앱 카드 스타일로 교체.
- 기본 Android 팝업 느낌을 줄이고 버튼/입력창/체크박스 여백을 앱 UI와 맞춘다.
- 키보드가 올라오는 작은 화면에서 레이아웃 깨짐 확인.

### 3. 정리 결과 UI Polish
- 현재 문제:
  - 홈 하단 `정리 결과` 카드가 긴 안내 문장을 한 덩어리로 보여줘 테스트앱처럼 보인다.
  - `정리 완료`, `새 장소`, `위치 없음`, `남은 원본`, `정리 기록 확인`이 한 문단에 섞여 우선순위가 약하다.
  - 카드 우측 화살표만 있고 실제 다음 행동이 충분히 명확하지 않다.
- 개선 방향:
  - 결과 카드를 `완료 상태 + 핵심 숫자 + 다음 행동` 구조로 재구성.
  - 제목 예: `정리가 끝났어요`
  - 서브라인 예: `7개 정리 완료 · 새 장소 1개`
  - 위치 없음은 별도 낮은 톤으로 표시: `위치 정보 없는 2,222개는 정리 제외됐어요`
  - 삭제 불안 문구는 짧게: `사진 원본은 삭제되지 않아요`
  - CTA를 명확히: `정리 기록 보기`
  - 긴 문장 대신 2~3개 짧은 행 또는 mini stat row로 구성.
- 레이아웃 후보:
  - 상단: 체크 아이콘 + `정리가 끝났어요`
  - 중간: `정리 완료 7개`, `새 장소 1개`, `정리 제외 2,222개`
  - 하단: `정리 기록에서 앨범을 확인할 수 있어요` + `정리 기록 보기`
  - 카드 높이는 현재보다 약간 커져도 좋지만 문단형 설명은 피한다.
- 검증:
  - 위치 없음이 매우 많은 케이스에서도 실패/미완료처럼 보이지 않아야 한다.
  - 사용자가 다시 `앨범 정리 시작`을 누르지 않고 `정리 기록`으로 이동해야 함을 이해해야 한다.

### 4. 다음 UX 문구 보강
- 진행 중 문구에 `사진 원본은 삭제되지 않습니다`를 추가.
- 위치 정보 없는 항목은 다음 실행에서 무조건 다시 검사하지 않도록 1차 skip 캐시 설계 검토.
- 단, 위치 정보가 나중에 생기는 파일/권한 변경/파일 수정시간 변경을 고려해 캐시 무효화 조건을 같이 설계한다.

### 5. 원본 사진 휴지통 이어하기 상태 저장
- 사용자 피드백: 정리 후 원본 사진이 갤러리에 남아 있으면 “사진이 중복됐다”고 오해하는 사례가 있음.
- 목표: 정리 완료 후 앱을 나갔다 들어와도 `지난 정리의 원본 사진 휴지통 이동` 액션을 계속 보여준다.
- 정리 완료 시 저장할 최소 상태:
  - 정리 세션 ID / 완료 시각
  - 복사 완료된 원본 사진 URI 목록
  - 복사된 앨범/장소 요약
  - `originalCleanupPending = true`
  - 휴지통 이동 완료/실패/이미 없음 개수
- UX 문구:
  - `원본 사진이 아직 갤러리에 남아 있어요.`
  - `정리된 앨범은 유지되고, 원본 사진만 휴지통으로 이동합니다.`
  - 버튼명: `원본 사진 휴지통으로 이동`
- 처리 원칙:
  - 다시 앨범 정리를 실행하지 않고, 저장된 원본 목록으로 후속 작업만 이어간다.
  - 사용자가 직접 지웠거나 이동해서 URI가 사라진 항목은 `이미 없음`으로 처리한다.
  - 실패 항목은 개수로 안내하고, 성공한 항목은 pending 목록에서 제외한다.
- 검증:
  - 정리 완료 후 앱 재시작해도 원본 휴지통 이동 카드가 유지된다.
  - 휴지통 이동 완료 후 카드는 사라지거나 `원본 정리 완료` 상태로 바뀐다.
  - 원본 일부가 이미 삭제된 상태에서도 앱이 튕기지 않고 결과를 안내한다.

## V2 Product North Star

### 한 줄 정의
- 사진을 정리하는 앱에서 사진을 `의미로 재구성하는 앱`으로 확장한다.
- V2의 핵심 정의: 앨범 정리 앱이 아니라, 사진을 `기억 구조`로 변환하는 시스템.

### V1 -> V2 변화
- 위치 기반 자동 폴더 생성 -> 의미 기반 태그 + 필터 구조
- 정리 결과 중심 -> 기억 해석 중심
- 갤러리 중심 이동 -> 앱 내 의미 탐색 중심
- 폴더 구조 -> 태그 기반 구조

### 핵심 개념
- Memory State: 사진 원본을 직접 탐색하게 하기보다, 분석 후 의미 상태를 저장한다.
  - 예: `photo -> location: 성남, tags: [집, 회사], time: 2024, poi: optional`
- Tag 기반 UX: 폴더/계층보다 태그와 필터가 탐색의 중심이 된다.
- View = Filtered Memory: 같은 데이터라도 장소, 기간, 해외, 태그에 따라 다른 기억 뷰로 보여준다.

### 시스템 구조
1. Gallery source
2. 1회 분석
3. Memory Engine
4. Tag DB / JSON state
5. 앱 내 filtered memory view

### V2 핵심 UX
- Home = Memory Dashboard
  - 성남, 수원, 일본, 호주 같은 장소/국가/기억 묶음 표시
  - 최근 발견한 장소와 해외 기록을 메인 첫 화면에서 보여준다.
- Tag Filter Bar
  - 집, 회사, 여행, 아이사진, 2024 등
  - 태그 클릭 시 앱 내부 뷰가 즉시 필터링된다.
- Place Detail
  - 장소명, 세부 태그, 날짜 범위, 메모, 대표 썸네일을 보여준다.
- Memory Note
  - 사용자가 직접 기억을 추가한다.
  - 예: `특허 소송 때문에 서울 방문`

### V2에서 의도적으로 하지 않는 것
- 폴더 안 폴더 구조
- 파일 선택 기반 앨범 생성
- 중복 제거 기능
- 위치 수동 편집
- 갤러리 중심 브라우징

### UX 원칙
- 갤러리는 주 UI가 아니라 fallback이다.
- 모든 탐색은 앱 내부에서 끝나는 것을 기본으로 한다.
- 구조 대신 의미를 보여준다.

### 성공 기준
- 앱 재방문 이유가 생긴다.
- 태그/필터 사용률이 증가한다.
- 사용자가 `이날 뭐였지?`를 앱 안에서 탐색한다.
- 갤러리 이동 비율이 줄어든다.

## V2 방향성: Memory View 기반 탐색

### 제품 방향
- 폴더 계층을 직접 탐색하게 하는 앱이 아니라, GPS/시간/장소 단서를 기반으로 만든 `기억 뷰`를 앱 안에서 보여주는 방향으로 확장한다.
- 갤러리 앱으로만 보내는 구조에서 벗어나, 앱 안에서 장소/기간/해외 기록을 필터링해 보는 화면을 제공한다.
- 실제 파일 이동/복사 구조는 유지하되, 사용자가 보는 탐색 단위는 폴더가 아니라 `장소`, `여행`, `기간`, `해외 기록`, `태그`가 된다.

### V2 1차 목표: 해외 기록 메인 노출
- 메인 화면에 `해외 기록` 섹션을 추가한다.
- 작은 썸네일 카드로 해외 장소/여행 묶음을 보여준다.
- 각 카드에는 대표 썸네일, 장소명/국가명, 사진 수, 날짜 범위를 함께 표시한다.
- 해외 기록 카드를 누르면 해당 해외 앨범/장소를 한꺼번에 모은 filtered memory view로 이동한다.
- 이 화면은 파일 시스템 폴더 브라우징이 아니라 앱 내부 필터 뷰로 구현한다.

### Filtered Memory View 후보
- 장소별 필터: `수원에서`, `청주에서`, `분당서울대학교병원에서`
- 기간별 필터: 연도/월/여행 기간
- 해외 기록 필터: 국내가 아닌 주소/국가명을 가진 사진 묶음
- 태그 후보: 해외, 병원, 학교, 공원, 역, 음식, 카페 등. 처음부터 자동 태그를 과하게 하지 말고 장소명 품질 개선과 연결해서 점진 적용한다.

### Codex 방향 문장
Build a memory-based photo organization system that replaces folder hierarchy with tag-based semantic grouping derived from GPS/time clustering, and renders all navigation inside the app as filtered memory views rather than file system browsing.

### 구현 메모
- 기존 `StoredAlbumSummary`/정리 기록 JSON을 확장해서 memory view의 데이터 소스로 쓸 수 있는지 먼저 검토한다.
- 앱 내부 뷰는 썸네일, 날짜 범위, 사진 수, 장소명, 원본 앨범 경로를 가진 summary 모델이 필요하다.
- 해외 판별은 `countryName`, `addressLine`, `adminArea` 기반으로 시작하되, 한국 주소 예외 처리를 명확히 둔다.
- 파일을 다시 이동하지 않고도 memory view가 동작해야 한다.
- 갤러리 열기는 보조 액션으로 유지하고, 기본 탐색은 앱 내부 filtered view로 제공한다.

## V1 사용자 테스트 피드백

### 1. 메인 화면을 사용자 목표 중심 Flow로 재구성
- 현재 4개 버튼이 각각 독립 기능처럼 보여 첫 사용자가 헷갈림.
- 실제 목표는 `사진/동영상 정리` 하나이므로 메인 액션을 단순화한다.
- 추천 구조:
  - Primary: `정리 시작`
  - Secondary: `결과 보기`
  - Separate/Safe action: `원본 삭제`
- `미리보기`, `앨범으로 정리`, `결과 보기`는 하나의 흐름 안에서 자연스럽게 이어지게 만든다.

### 2. 버튼/화면 이름 정리
- `미리보기`라는 이름이 사용자에게 어색하다는 피드백.
- 후보:
  - `정리 시작`
  - `정리할 항목 확인`
  - `앨범 정리`
- 메인에 버튼이 하나만 남는다면 `앨범 정리` 또는 `정리 시작`이 더 자연스러움.

### 3. 정리 Flow
1. `정리 시작`
2. 정리 예정 항목 확인
   - 예: 남해 94개, 압구정 21개, 위치 없음 238개
3. 미리보기 화면에서 바로 `앨범으로 정리`
4. 정리 진행
5. 정리 완료 후 `결과 보기`
6. 필요 시 `원본 삭제`

### 4. 진행 상태 UI 개선
- 오늘 추가한 진행 카드는 기존보다 훨씬 나아졌음.
- 다음 개선:
  - 더 깔끔한 진행 카드 디자인
  - `위치 정보 분석 중`
  - `525 / 1295개 완료 · 41%`
  - 진행바 색/높이/여백 다듬기
  - `중지` 버튼은 진행 카드 안에 작게 유지

### 5. 원본 삭제는 별도 액션 유지
- 사진은 복사, 동영상은 이동이므로 원본 삭제는 사진 원본만 대상으로 유지.
- 문구는 안전하게:
  - `복사된 원본 삭제`
  - `사진 원본 삭제`
  - 삭제 전 확인 팝업 유지

## 내일 우선순위
1. 메인 4개 버튼 구조를 `정리 시작` 중심으로 재배치
2. 미리보기/결과 화면에서 바로 정리 실행 흐름 연결
3. 진행 카드 UI 한 번 더 다듬기
4. 문구 전체 재점검: 개발자 용어 줄이기
5. 빌드 후 폰 설치, Google Drive APK/AAB 업데이트

## 2026-06-11 안정화 메모
- 6월 9일 동일 사진 3개를 비교함: 원본 `DCIM/Camera`, 삼성 갤러리 복사본 `Download`, 앱 복사본 `Pictures/성남에서`.
- 세 파일 SHA256이 동일해서 EXIF 차이는 없음.
- 차이는 MediaStore/파일 시간 쪽:
  - 삼성 갤러리 복사본은 파일 `mtime`과 MediaStore `date_modified`가 원본 촬영 시간으로 유지됨.
  - 앱 복사본은 파일 `mtime`과 MediaStore `date_modified`가 복사 시점으로 바뀜.
  - 카카오톡 recent 노출 원인은 이 차이일 가능성이 높음.
- 앱 복사 후 실제 파일 `lastModified`를 촬영 시간으로 복원하도록 수정함.
- 미리보기만 실행한 상태에서는 원본 삭제 버튼이 절대 노출되지 않도록 상태를 분리함.
- 원본 삭제 노출 조건:
  - 실제 앨범 정리 완료
  - 삭제 가능한 사진 원본 목록 존재
  - 아직 삭제되지 않음
- `정리 대상 폴더`의 `선택` 버튼은 메인 CTA처럼 보여서 `폴더 변경 >` 보조 액션으로 낮춤.
- 비교 보고서: `C:\Users\mismi\Documents\Codex\GallerySorter\reports\media-compare-20260609_233202.md`

## UX 개선 후보
- 진행 중 화면에서 실시간으로 발견된 장소를 보여주기:
  - 예: `속초 38`, `강동 22`, `청주 14`
  - 사용자가 앱이 실제로 일하고 있다는 느낌을 받게 함.
- 결과 화면 텍스트 축소:
  - `정리 결과`
  - `이미 정리됨 1개`
  - `위치 없음 1398개`
- `정리 예정 앨범`은 `정리 결과`, `발견된 장소`, `발견된 앨범` 중 하나로 정리.
- 선택된 폴더 일부 노출:
  - `카메라`
  - `다운로드`
  - `카카오톡`
  - `외 2개`
- 날짜 이상값 필터:
  - 1969년, 2093년 같은 값은 `날짜 정보 없음` 처리.
- V2 후보:
  - 장소별 결과에서 날짜별/월별/연도별 세분화.
  - POI/별칭 기반 앨범명.

## 현재 UX 결정사항
- 메인 CTA 이름은 `앨범분류 시작` 쪽이 더 자연스럽다.
- `정리 시작`, `정리 실행`, `결과 보기`, `사진 원본 삭제`, `폴더 선택`을 같은 레벨의 4개 기능처럼 노출하지 않는다.
- 메인 화면은 하나의 목표인 `사진/동영상 앨범 분류`를 시작하는 화면이어야 한다.
- `정리 실행`은 메인에 독립 버튼으로 두지 않고, 미리보기/결과 화면에서 확인 후 실행하게 한다.
- `사진 원본 삭제`는 위험 액션이므로 메인 주요 액션 영역에서 빼고, 정리 완료 이후 또는 결과 화면 맥락에서만 노출한다.
- 버튼 안 보조문구는 제목보다 작고 연한 회색으로 낮춘다. 보조문구가 제목처럼 보이면 안 된다.
- 결과 화면의 `바로 정리하기` 버튼은 리스트 아래가 아니라 통계 카드 아래, `정리될 앨범` 섹션 위에 둔다.
- 진행 카드는 하나로 묶는다: `위치 정보 분석 중` + `525 / 1295개 완료 · 41%` + 진행바 + 작은 `중지`.

## 새 채널 시작용 요약
프로젝트 경로: `C:\Users\mismi\Documents\Codex\GallerySorter`

현재 구현:
- 앱 이름: `앨범정리`
- 패키지: `com.photoplace.app`
- 사진/동영상 모두 미리보기 대상 포함
- 사진은 위치별 앨범으로 복사
- 동영상은 위치별 앨범으로 이동
- 삼성 갤러리 동영상 GPS는 `MediaMetadataRetriever`로 읽음
- 폴더 선택 가능
- 진행 카드/프로그레스바 있음
- APK/AAB는 `G:\내 드라이브\Codex APK`에 덮어쓰기 중

다음 확인:
- 메인 단순화 UI가 실제 폰에서 어색하지 않은지
- `앨범분류 시작` 버튼 높이/보조문구 크기
- 결과 화면 상단의 `바로 정리하기` 위치
- 동영상 이동이 실제 기기에서 권한 문제 없이 되는지

## 2026-06-25 V1.1/V2 장소명 품질 개선 후보

### 비서울 POI 장소명 우선순위 개선
- 사례: 분당서울대학교병원에서 찍은 사진이 `대병원에서`으로 생성됨.
- 원인 추정:
  - 현재 비서울 주소는 `locality -> subAdminArea -> adminArea -> countryName` 순서로 장소명을 선택함.
  - Android Geocoder가 `locality`에 잘린 값 또는 애매한 값(`대병원`)을 반환하면 POI명(`분당서울대학교병원`)보다 먼저 사용됨.
- 개선 방향:
  - 비서울도 `featureName`, `subLocality`, `thoroughfare`, `addressLine`을 후보에 포함.
  - 너무 짧거나 의미 없는 조각은 제외.
  - `병원`, `대학교`, `공원`, `역`, `공항`, `미술관`, `박물관`, `예술의전당` 같은 POI성 이름은 우선 사용.
  - 단, 비서울 전체 장소명 규칙에 영향이 있으므로 V1 출시 직전에는 수정하지 않고 V1.1/V2에서 테스트 후 반영.
# 2026-07-24 백그라운드 리팩터 중단 지점 / 다음 TODO

현재 브랜치: `codex/photoplace-v2-bg-wip`

현재 상태:
- 로컬 debug APK를 폰에 설치해서 테스트 준비 완료.
- 기존 릴리즈 앱은 서명 충돌 때문에 삭제 후 debug APK 설치함.
- `SortJob` 경로로 Activity 정리 실행을 우회시킨 첫 버전이므로 실기기 정리 테스트 필요.
- `assembleDebug testDebugUnitTest` 통과.

남은 P0/P1 이슈:

1. 작업 중 Back 키가 여러 번 눌러도 안 먹는 경우가 있음.
   - 정리 중 화면/정리 기록 탭/홈 복귀 사이에서 Back 이벤트가 막히거나 늦게 처리되는지 확인.
   - `isWorking`, `blockNavigationWhileWorking()`, `onBackPressed()`, `OnBackInvokedCallback` 흐름 점검.
   - 정리 중에는 명확히 toast 또는 진행 화면 유지, 정리 완료 후에는 이전 화면/홈 복귀가 예측 가능해야 함.

2. 알림 진행률이 실시간 업데이트되지 않음.
   - `SortForegroundService.update()` 호출 빈도와 notification id/channel 갱신 확인.
   - 알림 클릭 시 앱으로 진입하도록 `PendingIntent` 추가 필요.
   - 앱 진입 시 `SortProgressStore`의 최신 진행률을 홈 진행 UI에 복원해야 함.
   - 릴리즈 빌드 전 WorkManager lint 수정 필요:
     - `androidx.work.impl.foreground.SystemForegroundService`에 `android:foregroundServiceType="dataSync"` manifest override 추가.

3. 이미 정리된 항목을 다시 정리하려는 UX 개선.
   - 사용자가 `장소별 앨범 만들기`를 누르지 않고 멈춤/새 정리를 반복하면, 이미 처리된 항목 상태가 헷갈림.
   - 새로 돌리면 현재는 `이미 정리됨` 3000개처럼 나오므로 실제 중복 복사는 피하는 것으로 보임.
   - 하지만 UX상 “이미 정리된 항목은 다시 하지 않음”을 더 명확히 보여줘야 함.
   - 미리보기/결과 화면에 다음 상태를 분리 표시:
     - 새로 정리할 항목
     - 이미 정리된 항목
     - 위치 정보 없음
     - 실패/건너뜀
   - 이미 정리된 항목이 대부분이면 기본 CTA를 숨기거나 `새 항목만 정리`로 표시.
   - 이 피드백이 곧 “정리 이력/최근 정리 기록을 보여달라”는 요구와 연결됨.

다음 구현 순서 제안:
1. 실기기에서 4000장 다운로드 폴더 테스트.
2. `SortJob` 경로로 사진 복사/동영상 이동/완료 기록/중단이 정상인지 확인.
3. 알림 클릭 PendingIntent + 진행률 갱신 수정.
4. 이미 정리됨 UX 개선.
5. Back 키 처리 점검.
6. 그 다음 WorkManager 실제 연결 재개.
## Memory viewer follow-up

- [ ] 발견 상세에서 선택한 날짜 그룹의 사진/동영상을 앱 내부에서 좌우로 넘겨보는 뷰어 추가
- [ ] 앱 내부 뷰어에서 현재 항목 위치, 동영상 재생, 원본 앱으로 열기 제공

## Discovery cache and new-place correctness (next P0)

- [ ] Mark places newly added by the latest completed analysis in `발견 기록`.
  - Show a compact `NEW` badge on cards/list rows whose place key was absent from the pre-analysis Discovery snapshot.
  - Keep the badge scoped to the latest successful analysis; it must not permanently mark historical records as new.
  - New-place ordering should be newest-first while the marker is active, without changing the user's normal sort order after acknowledgement.
  - Persist only the latest-analysis place-key set (or equivalent analysis token), not presentation-only flags per photo.
  - Opening a place may acknowledge that one badge; provide an explicit, predictable point to clear all remaining NEW markers.
- [ ] Reproduce and fix: reanalyzing the same `Band` folder after adding a new `안성에서` photo reported `새로 발견한 장소 0곳`.
  - Trace MediaStore selection, duplicate filter, snapshot mapper, merger, and `DiscoverySnapshotUpdate` baseline comparison.
  - Add a regression test for “existing snapshot + new URI/new place = newPlaceCount 1”.
  - Confirm the new group appears in Discovery on device before changing caching behavior.
- [ ] Add `LocationAnalysisCache` only after the new-place regression is resolved.
  - Cache normalized place results and `LOCATION_NONE`, never merely hide items.
  - Cache hit must still rebuild Preview/Discovery counts and cards.
  - New, copied, moved, changed, GPS-added, or policy-version-changed media must miss the cache.
