# 메모리 수량 불일치 조사 및 1차 수정 — Codex

- 날짜: 2026-10-19 (Asia/Seoul)
- 제보: 위치 앨범이 있는 메모리와 발견기록만 있는 메모리의 수량이 전반적으로 다르게 보임.
- 기준: HEAD `1c8d683` 대비 기존 working tree 위의 추가 수정. 이전 Antigravity 승인은 이 추가 수정의 승인을 뜻하지 않음.
- 판정: **1차 표시 계산 수정 및 자동 검증 완료. 전체 수량 통일/실기기 확인은 미완료이며 출시 보류 유지.**

## P0 / P1 게이트

- 이번 수량 패치에서 MainActivity를 추가 수정하지 않았다. 전체 기존 diff는 +71/-362로 유지됨. 새 수량 계산은 `MemoryMediaCounts`, 데이터 모델/프로젝션에 격리했다.
- MediaStore 쓰기, 원본 파일 복사/이동/삭제, identity/alias/OrganizationLink/메모 변경 없음.
- 기존 `MemoryRecord`와 앨범 히스토리를 고쳐 쓰지 않는다. 화면에서 실제 해석한 미디어 목록의 수만 표시한다.
- 원본 휴지통 차단은 유지한다.

## 확인된 원인과 조치

### P1-A — 상세 상단의 저장 집계와 실제 사진 목록 불일치: FIXED IN CODE

- 위치: `app/src/main/java/com/example/gallerysorter/MemoryBrowserState.java:302`.
- 기존 동작: 목록/저장 레코드의 itemCount로 만든 item.countText를 상세 상단에서 그대로 사용. 상세 본문은 별도의 MemoryMediaResolver 결과를 사용함.
- 재현: 저장된 하남 앨범 집계 105, 실제 GalleryReader 결과 3개 → 그리드는 3개인데 헤더는 105장. 발견 refs에 stale/빈 URI가 있으면 헤더에도 여전히 포함됨.
- 근거: 기존 테스트에 실제 표시 수량 단언 3개를 추가한 뒤 `MemoryBrowserStateTest` 실행 시 3개 모두 실패(legacy 105→3, stale 3→2, 빈 URI 2→1). 수정 후 전체 통과.
- 수정: 상세 상단은 실제 날짜 section에 들어간 사진/영상 수를 사용. Gallery 조회 실패는 0장으로 단정하지 않고 `불러오기 필요`, 확인된 미디어 없음은 0장.

### P1-B — 영상까지 전부 사진 N장으로 표기: FIXED FOR DISCOVERY/RESOLVED DETAIL

- 위치: `MemoryBrowserState.java:173`, `MemoryMediaCounts.java`, `HomeRecentPlacesResolver.java:65`.
- 조건: discovery refs에 사진과 영상이 혼합되거나 영상만 있음.
- 수정: 발견 전용 카드·해석 완료 상세·홈에서 사진/영상 구분을 공용 formatter로 처리. 예: `사진 1장 · 동영상 1개`, `동영상 1개`.
- 앨범 히스토리에는 매체별 구분이 없으므로 미조회 앨범 요약은 `사진·동영상 N개`라고 표시. 영상 수를 임의 추정하지 않음.

### P1-C — 중복 URI가 날짜 그리드에도 중복 표시될 가능성: FIXED IN CODE

- 위치: `MemoryPhotoSection.java:38`.
- 조건: 같은 sourceUri가 refs에 두 번 들어옴. 날짜가 다르더라도 하나의 실제 미디어임.
- 수정: 날짜 섹션 구성 전에 URI 중복 제거. 공용 수량 계산도 stale/빈 URI/중복 URI 제외.
- 다른 URI를 가진 다른 파일은 이 단계에서 합치지 않는다. 기존 Gallery/Discovery 교차 중복 처리는 기존 resolver 정책을 유지.

### Pre-release D — MIXED 저장 집계와 화면별 조회 범위: OPEN

- 위치: `MemoryRepository.java:274`~276, `MemoryRepository.java:113`, `MemoryRepository.java:133`, `DiscoverySnapshotController.java:163`.
- MIXED 레코드의 저장형 item/photo/video 집계는 두 source의 Math.max 값이다. 이 값은 실제 중복 제거 합계가 아니다. 앨범 2개+발견 2개 중 한 개가 같은 파일이라면 실제 3개인데 max는 2개이며, 과거 앨범 집계가 10이면 max는 10개가 될 수도 있다.
- 이번 수정은 홈/상세의 실제 resolver 결과를 기준으로 표시하여 해당 화면이 이 max 값을 정확한 수량으로 사용하는 문제를 막는다. 레코드의 집계 자체는 변경하지 않았다.
- `discoveryMemories()`는 발견 전용 레코드를 반환하고, `memories()`는 호환되는 앨범과 합친 레코드를 반환한다. 홈과 발견 목록은 같은 장소명이어도 서로 다른 범위를 보여줄 수 있다. `memory(key)`도 discovery 키 우선 조회라 화면 진입 키에 따라 resolver에 전달되는 레코드가 달라질 수 있다.
- 따라서 **발견 카드와 앨범 포함 상세의 모든 숫자가 항상 같아졌다고 주장하지 않는다.** MIXED 목록의 미조회 집계도 여전히 실제 미디어 수로 확정할 수 없다.
- 후속 수정 방향: 목록/상세가 참조할 메모리 source 범위를 통일하고, 목록 숫자도 해당 resolver 요약으로 제공. 상세 클릭마다 이름만으로 alias를 새로 연결하거나 원본을 움직이는 방식은 사용하지 않는다. 목록 전체 앨범 조회를 UI 스레드에 추가해서 이전 성능 개선을 되돌리지 않아야 한다.
- 검증 조건: 실제 하남/성남의 홈·발견 카드·상세 날짜 그리드를 동일 source 범위로 비교. 현재 폰 미연결로 실제 전체 수량 대조는 하지 못함.

### Follow-up E — 파일명 기반 Gallery/Discovery 중복 판정의 한계: OPEN

- 위치: `MemoryMediaResolver.java`, `mediaFileKey()` / `withNewDiscovery()`.
- 기존 resolver는 매체 종류+정규화 파일명을 중복 판정에 사용. 내용이 다른 같은 이름의 새 파일이 이미 Gallery에 있으면 제외될 수 있음.
- 이번 패치에서 원래 중복 정책을 변경하지 않음. 정확한 교차 source 수량의 보장을 위해서는 per-media identity 또는 추가 동일성 근거가 필요하며 별도 검토해야 함.

## 테스트 및 빌드

- `gradlew.bat testDebugUnitTest assembleDebug`: **PASS, 256 tests / 0 failures / 0 errors**.
- `git diff --check`: PASS (기존 LF/CRLF 정규화 경고 있음).
- 단언을 보강한 기존 테스트 3개는 수정 전 실패→수정 후 통과.
- 신규 테스트 5개: 중복 URI grid/count 일치, 사진/영상+stale/누락 혼합, 영상 전용, 부풀려진 discovery aggregate 무시, MIXED 실제 union/header/home 일치.
- MIXED fixture: 과거 앨범 집계10, 실제 앨범2, discovery2(복사본1+새 항목1) → 실제 목록3, 홈3/상세3 일치. 과거 레코드는10으로 보존됨.
- APK: `app/build/outputs/apk/debug/app-debug.apk` (debug). 폰 미연결이므로 설치·실기기 수량 검증은 하지 않음.
- lint: 앞선 점검에서 기존 MainActivity WrongConstant 179 errors/59 warnings로 FAIL. 이번 패치 후 lint를 다시 실행한 것은 아니며 통과로 표기하지 않음.

## 다음 작업자를 위한 인계

### 사용자 제공 Antigravity 분석 캡처 추가

사용자가 어제 요청한 분석 캡처(303293.jpg)를 제공했다. 제안은 MemoryRepository.merge의 Math.max를 실제 합집합 수량으로 바꾸고, MIXED 목록도 MemoryMediaCounts로 계산하여 화면 간 일치시키자는 내용이다. 캡처의 `existing + incoming`은 단순 덧셈으로 구현하면 안 된다. 예: 앨범100장 중80장이 발견 refs에도 있으면 발견80+신규1=81과 더해181이 아니라101이어야 한다. 반대로 서로 겹치지 않으면 합계가 맞는다. **목표(동일 source 범위의 중복 제거 합집합)는 동의하되, 집계 숫자 두 개만으로 정확한 교집합을 알 수 없다.**

현재 실제 코드의 merge는 여전히 Math.max이며 캡처 제안의 구현 완료 증거는 없다. 기존 OPEN D/E 및 리뷰 요청서에 이 목표/중복 방어를 이미 포함했다. 리뷰 시 이 캡처를 추가 배경으로 사용하고 “단순 더하기 금지”, “UI 스레드의 앨범 전수 재조회 금지”, “원본/alias 자동 변조 금지”를 함께 확인할 것. 이 추가 사항은 사용자 제공 분석의 기록이며 외부 리뷰 승인 판정은 아니다.

1. 위 1차 수정에 대한 외부 리뷰. 관련 코드: MemoryMediaCounts / MemoryBrowserState / MemoryPhotoSection / HomeRecentPlacesResolver 및 해당 테스트.
2. OPEN D/E의 source 범위·교차 source 동일성 정책을 해결하여 목록까지 정확한 수량을 일관되게 제공.
3. 폰 연결 후 debug 설치 및 하남/성남의 홈·목록·상세·사진/영상 수를 비교. 현재 설치 버전에는 이번 1차 수정이 반영되지 않았음.
4. 사용자가 두 번째 GPS 사진은 정상 발견됐다고 알려줌. 첫 사진은 삼성 갤러리에서도 위치가 없었음을 직접 확인함. 이 사실만으로 수량 이슈 또는 반복 보고 검증을 완료 처리하지 않음.
5. 버전 변경/커밋/푸시/스토어 배포는 수행하지 않았다.
