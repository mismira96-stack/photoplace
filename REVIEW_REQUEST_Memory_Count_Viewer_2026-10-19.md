# 리뷰 요청 — 메모리 수량 1차 수정 및 확대 뷰어 로딩

저장소: `C:\work\projects\PhotoPlace`

먼저 AGENTS.md / GEMINI.md를 읽어주세요. 수정 없이 리뷰부터 진행하고, 결과는 프로젝트 루트의 **REVIEW_Memory_Count_Viewer_2026-10-19.md**에 저장해주세요. Codex가 작성한 조사 보고서를 외부 승인으로 간주하지 말아주세요.

## 기준과 범위

- 현재 HEAD: `1c8d683`. 변경은 아직 working tree에 있으며 새 클래스/테스트 상당수가 untracked입니다. **git diff만으로는 전체 변경을 볼 수 없으므로 git status와 아래 명시 파일의 본문도 확인**해주세요.
- 앞서 Antigravity가 승인한 증분 분석/홈 캐시 이후의 추가 변경이 대상입니다. 이전 승인이 이번 수량/뷰어 패치를 포함하지 않습니다.
- 경계 커밋이 없으므로 명시 파일과 기존 보고서를 함께 사용하고, working tree 전체와의 상호작용도 확인해주세요. 발견한 문제가 기존인지 이번 변경인지 구분해주세요.
- MainActivity 현재 전체 diff: **+81/-382 (순감301줄)**. 줄 감소 자체가 P0 준수의 근거는 아니므로 추가된 판단 로직을 우선 검사해주세요.

## 1. 메모리 수량 불일치 1차 수정

문제: 저장된 앨범/발견 집계로 상세 헤더를 표시하지만 실제 사진 목록은 별도 resolver 결과를 사용하여 수가 달랐습니다. 영상도 전부 사진 N장으로 표시했습니다.

변경 파일 (경로 접두사 `app/src/main/java/com/example/gallerysorter/`):

- **MemoryMediaCounts.java** (신규): live refs/날짜 sections의 사진·영상 수량과 표시 문구.
- **MemoryBrowserState.java**: 발견 전용 카드 수량, 상세 실제 sections 수량, Gallery 조회 실패와 확인된 빈 결과의 문구 구분.
- **MemoryPhotoSection.java**: 날짜 section 구성 전에 동일 URI 중복 제거.
- **HomeRecentPlacesResolver.java**: 홈 수량 표시를 공용 helper로 연결.

테스트 (`app/src/test/java/com/example/gallerysorter/`): MemoryMediaCountsTest.java(신규), MemoryBrowserStateTest.java, HomeRecentPlacesResolverTest.java.

우선 확인할 사항:

1. 실제 grid/sourceUris 수와 상세 header 수가 일치하는지. stale/빈 URI/중복 URI 및 영상 전용/혼합 확인.
2. Gallery 조회 실패를 실제 0장으로 표시하지 않는지. known missing / failed / unknown 구분과 기존 exact link 우선권 유지.
3. 동일 URI 중복 제거가 날짜 메모 키·날짜 ownership·viewer initial index·모음 상세를 깨뜨리지 않는지.
4. 신규 formatter가 영상 수량을 사진으로 오표기하지 않는지. 미조회 레거시 앨범 히스토리는 종류별 수를 알 수 없어 사진·동영상 N개로 표기하는 변경의 적절성.
5. 최신 실제 목록의 수만 표시하고, 레코드/스냅샷/앨범 히스토리 원본은 쓰지 않는지.

수정 전 실패를 확인한 기존 단언: 히스토리105 vs 실목록3, stale 포함3 vs 표시2, 빈 URI 포함2 vs 표시1. 수정 후 통과.

### 의도적으로 OPEN인 수량 문제도 출시 관점에서 평가 요청

- MemoryRepository.merge는 MIXED 집계를 Math.max로 보유하므로 실제 union 합계가 아닙니다. 이번에는 resolved 홈/상세가 해당 집계를 표시하는 문제를 고쳤으나 미조회 MIXED 목록 집계는 남아 있습니다.
- discoveryMemories()/memories()/memory(key)의 source 범위가 달라 같은 장소명이라도 발견 목록과 앨범 포함 화면의 수가 다를 수 있습니다. 전체 화면 수량 통일 완료로 승인하지 말아주세요.
- 기존 MemoryMediaResolver의 매체종류+정규화 파일명 교차 source 중복 판정은 다른 내용의 같은 이름 파일을 누락시킬 가능성이 있습니다. 이번에 그 정책을 변경하지 않았습니다.
- 위 미해결이 현재 1차 수정의 병합/출시를 막아야 하는지 구분해 판단해주세요. 단순 더하기나 이름 기반 alias 자동 생성으로 수정하지 말아주세요.

## 2. 발견기록 확대 이미지 대기열 개선

문제: 상세 썸네일 최초48장과 확대 뷰어의 큰 이미지가 공통 2-thread FIFO worker를 사용해 확대 사진이 그리드 작업을 기다릴 수 있었습니다.

변경 파일:

- **MemoryViewerImageLoader.java** (신규): 전용 single-thread worker, 동일 URI 작은 preview 선표시, provider CancellationSignal, stream fallback, 최대2048px 요청, byte 기준16MiB viewer cache, view detach/close 처리.
- **LatestImageLoadQueue.java** (신규): 요청 세대로 지난 queued decode/완료/오류 무효화.
- **MemoryPhotoViewer.java**: 영상/빈 항목 전환 시 host.clearThumbnail 호출로 지난 사진 요청 취소.
- **MainActivity.java**: 로더 생성/위임/clear 콜백 및 onDestroy 정리만 연결. 기존 로딩 판단 코드를 전용 클래스로 이전.
- **LatestImageLoadQueueTest.java** (신규, test 경로): 빠른 넘김/늦은 결과/영상 및 이탈/오류 재시도 4건.

우선 확인할 사항:

1. MainActivity에 새 비즈니스 판단/계산이 들어갔는지 P0 게이트부터 확인.
2. 큰 사진 요청이 실제 그리드 대기열과 분리되는지. 빠른 넘김으로 작업이 쌓일 때 최신 요청 우선 동작과 메모리 보유량 확인.
3. 늦은 bitmap/오류가 다른 사진 또는 영상 화면을 덮어쓰지 않는지. cached hit 경로에서도 이전 요청 취소가 유지되는지.
4. attach 전 최초 로딩, detach, 재진입, 회전/폭 변경, Activity 종료와 worker rejection 안전성.
5. provider 취소와 stream fallback 자원 해제. fallback 디코드 자체는 즉시 취소되지 않을 수 있으며 결과 무시로 안전성을 지키는지.
6. bitmap cache의 크기 제한, 큰 bitmap 동시 보유, URI/size 키, 작은 preview 선택, 읽기 실패 표시 및 방향 처리 회귀.
7. 원본 미디어/메모/identity/앨범 연결에 쓰기나 삭제가 없는지.

이 변경은 확대 사진 로딩 대상입니다. 장소→사진 목록의 동기 repository/live filter/MediaStore 조회, viewer Back 시 상세 재생성은 별도 OPEN입니다. 실기기 성능 향상을 계측 완료한 것으로 판단하지 말아주세요.

## 검증 현황

- 최종 `gradlew.bat testDebugUnitTest assembleDebug`: **PASS, 260 tests / 0 failures / 0 errors**.
- `git diff --check`: PASS. 기존 LF/CRLF 경고 있음.
- 폰 미연결로 최신 수량/뷰어 패치 APK 미설치. 실기기 동작·체감속도 수치 미검증.
- 이전 lintDebug: **179 errors / 59 warnings로 FAIL**. 오류179건 모두 MainActivity WrongConstant이고 각 오류 source 문장은 HEAD에도 있었음. 이번 패치 후 lint 재실행 결과는 아직 없음. baseline/ignore를 추가하지 않았고 lint PASS로 표현하지 않습니다.
- APK: `app/build/outputs/apk/debug/app-debug.apk` (debug). 버전 증가/릴리스 빌드/커밋/푸시/스토어 배포 없음.

## 참고 보고서

- REVIEW_Memory_Count_Consistency_2026-10-19.md — 수량 재현/수정/OPEN 근거.
- REVIEW_Memory_Viewer_Performance_2026-10-19.md — 뷰어 구조/취소/제약.
- REVIEW_PreRelease_Verification_2026-10-19.md — 이전 검증 및 lint 부채.
- REVIEW_Incremental_Analysis_Home_Cache_2026-10-19.md — 이전 Antigravity 리뷰 원문 (이번 변경 승인 아님).

## 결과 형식

발견 사항을 **Blocker / Pre-release / Follow-up**으로 나누고 파일·현재 줄 번호·재현 조건·영향·수정 제안을 기록해주세요. 자동 테스트로 확인한 것과 코드 추론/실기기 미검증을 구분해주세요. 이 1차 패치의 코드 반영 가능 여부와 전체 앱 출시 가능 여부를 별개로 판정해주세요.
