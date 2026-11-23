# 성남 2,107개 — 카드에서 날짜별 목록 진입 지연

- Codex, 2026-10-19. 사용자에게 느린 구간을 다시 확인한 결과 **사진 확대/Back이 아니라 성남 카드→날짜별 사진 목록**이었다. 사용자가 수량2,107개를 알려줌.
- 이번 패치: 상세 데이터 해석 백그라운드 이동/동일 revision 재진입 캐시/날짜 formatter 재사용. 기존 뷰어/Back/smooth 수정도 APK에 포함.
- 상태: 자동 검증 및 debug 전달 완료. 실제 기기 성능 계측/외부 리뷰/사용자 재확인 전.

## P0 / 데이터 불변

- MainActivity: load controller 생성, 화면 로드 요청/결과 binding, lifecycle cancel/close만 연결. 기존 동기 query/실패 판단을 전용 컨트롤러로 이전했다. 전체 working tree MainActivity diff +123/-398, 순감275줄. 줄 감소 자체가 격리의 근거는 아니며 실제 추가 코드 책임으로 판단할 것.
- 새 source/cache/요청 정책은 MemoryDetailLoadController / MemoryDetailLoadSession에 위치.
- 미디어/파일/메모/identity/링크/발견 snapshot 쓰기 없음. 메모 키와 날짜 grouping 규칙을 변경하지 않음. 캐시는 현재 Activity 메모리에 상세1건만 보관.

## 확인한 구조와 수정

1. 기존 showMemoryBrowserDetailScreen에서 loadLiveMemoryAlbumSummaries → snapshot/repository/live filter → Gallery resolver → 날짜 section 생성이 UI 스레드에서 끝날 때까지 화면 전환이 진행되지 않았다. 최초 grid는 이미48개로 제한되어 있어도 이 앞의2,107개 데이터 해석 비용은 지불함.
2. MemoryDetailLoadController의 전용 single worker에서 위 source 해석을 수행. 기존 화면은 유지하며 하단에 작은 진행 안내를 표시. 준비되면 기존 binding 함수를 호출. 오래 걸리는 MediaStore query를 중간에 강제 중단하는 것은 아니며 지난 결과를 표시하지 않도록 요청 세대/원래 root attach 상태로 방어.
3. 같은 canonical key+앨범 포함 여부+HomeRevisionReader revision일 때 직전 MemoryBrowserDetail을 재사용. key/source 범위가 다르거나 version/generation/local file stat/권한이 달라지거나 revision이 unknown이면 새로 읽음. source read 전후 token이 바뀌면 재사용 기준점을 만들지 않음.
4. Gallery UNAVAILABLE 결과는 mediaResolved=false. 같은 revision이어도 오류 결과를 캐시하거나 viewer return의 정상 기준점으로 쓰지 않음. 확인된 빈 결과와 구분.
5. MainActivity는 source 결과의 revision을 MemoryViewerReturnController에 넘기고, 실제 UI bind 후 토큰을 다시 확인하는 기존 Back 안전성을 유지.
6. MemoryPhotoSection의 yyyyMMdd formatter를 각 ref마다 new하지 않고 section build당1개 재사용. 지역/시간대/날짜키 포맷은 유지하며 formatter를 스레드 간 공유하지 않음.

## 자동 검증

- MemoryDetailLoadSessionTest 신규6건: 변경 없는 같은 place/scope 재사용, 다른scope/changed/unknown은 새로 읽기, 해석 도중 source변경, 지난 queued/in-flight 요청 무시, 실패/null 후 복구, UNAVAILABLE 결과 기준점 금지.
- MemoryMediaCountsTest 신규1건:2,107개 전체 보존, 첫48/다음48/마지막43개 page, 날짜 내 최신순 확인. 시간 제한을 단언한 성능 벤치마크는 아님.
- 기존 MemoryBrowserStateTest에 조회 실패 mediaResolved=false 단언 추가.
- `gradlew.bat testDebugUnitTest assembleDebug`: **PASS, 276 tests / 0 failures / 0 errors**.
- `git diff --check`: PASS. 기존 줄바꿈 경고/lint 부채는 별도, 이번 lint 전체 통과를 주장하지 않음.

## 한계 및 Pre-release

- **cold 첫 진입의 source 조회 시간 자체를0으로 만든 것은 아님.** 진행 중 화면 반응 유지와 같은 데이터 warm 재진입 재사용이 목표.
- 실제 Android root 이탈/빠른 탭/Back/회전/앱 전환 및 loading overlay는 폰에서 확인해야 함. 순수 요청/캐시 정책만 자동 테스트함.
- source supplier는 기존 MainActivity의 album summary cache 함수를 worker에서 사용. 기존 home worker와 공유되는 단기 캐시 접근/초기화 상호작용도 외부 리뷰 필요. 캐시는 결과를 수정해 저장하는 경로가 아님.
- 날짜 헤더/메모 UI binding 일부는 여전히 UI 스레드에서 파일 조회하며 첫48개 view 생성도 UI 작업. PhotoPlaceDetail의 sourceMs/uiWaitMs/bindMs로 다음 병목을 분리해야 함. bindMs는 첫 실제 프레임 시간과 같지 않음.
- 모음 상세 전체 source 생성은 이번 비동기 상세 controller 대상이 아니며 기존 동기 경로가 남음.
- 메모리 수량의 source 범위/파일명 기반 교차 중복 정책은 별도 OPEN.

## 전달 APK

- Drive: `G:\내 드라이브\Codex APK\PhotoPlace-1.3.6-detail-entry-20261019-debug.apk`.
- 생성:2026-10-19 **21:30:28 KST**. 이전 오전09:09/09:19/09:30 파일은 유지.
- 크기9,300,363 bytes. SHA256 `D7394F925C006F462A5BB7F5CEBDD5BCDC12C2165835B66313D827B12AF3E78C` 원본/Drive 마운트 일치.
- version1.3.6/debug 유지, 직접 폰 설치/앱 데이터 초기화/스토어 배포 없음.

## 사용자 확인 순서

### 21:30 detail-entry APK 사용자 피드백

사용자가 **하단 로딩 안내가 보이고, 로딩 표시 덕분에 좀 나아졌다**고 보고했다. 이는 진행 피드백의 체감 개선이며 실제 source 소요시간 단축 또는 전체 성남 지연 해결의 계측 증거로 간주하지 않는다. **Back도 약간 느리다**는 잔여 제보가 있어 미해결로 유지한다. 새 계측 로그가 없어 revision/provider 확인, fallback 재조회, 재부착/layout 비용 중 어느 원인인지 확정하지 않는다. 다음 검증에서는 PhotoPlaceViewer returnReused/checkMs/reattachMs와 PhotoPlaceDetail sourceMs/uiWaitMs/bindMs를 구분한다. 이 피드백만으로 추가 추정성 APK를 배포하지 않는다.

성남 카드 첫 진입의 진행 안내/응답성과 목록 뜨는 시간 → Back 후 같은 성남 재진입 → 다른 장소 → 성남 재진입 → 사진 확대/넘김/Back 회귀. 새 사진/갤러리 변경 뒤에는 최신 수량·목록이 반영되는지 확인.
