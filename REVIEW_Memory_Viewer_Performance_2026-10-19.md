# 발견기록 사진 뷰어 지연 — 조사 및 수정

- 작성: Codex, 2026-10-19 (Asia/Seoul)
- 제보: 발견기록 뷰어가 사진 수가 많을수록 느리게 느껴짐.
- 판정: 확대 이미지 대기열 병목 수정 및 자동 검증 완료. 실기기 계측/회귀 확인 및 외부 리뷰 전이며 전체 발견기록 진입 성능 개선 완료를 뜻하지 않음.

## P0 / P1 게이트

- MainActivity의 변경은 전용 로더 필드/생성/이미지 바인딩 위임/clear 콜백/onDestroy 정리에 한정. 기존 뷰어 로딩 판단·대기열 코드를 제거하고 별도 클래스로 이전.
- 새 판단/캐시/취소 정책은 MemoryViewerImageLoader / LatestImageLoadQueue에 격리.
- 원본 파일/MediaStore/메모/발견 스냅샷/identity/앨범 연결에 쓰기 없음. 버전/배포 상태 변경 없음.

## P1 — 확인한 병목 구조: 수정

- 관련: `MainActivity.java:177`의 thumbnailWorker(2개 스레드), `MainActivity.java:120`의 최초 상세 썸네일 48장 제한, `MainActivity.java:8670`의 뷰어 로딩.
- 수정 전: 상세 그리드/홈 썸네일과 뷰어의 큰 이미지가 같은 FIFO worker를 사용. 상세를 연 직후 사진 탭 시 그리드 요청 뒤에서 대기할 수 있음. 더보기로 요청을 추가할수록 대기 작업이 늘 수 있음.
- 수정 후: 뷰어는 전용 단일 worker. 큰 이미지를 읽는 동안 이미 읽힌 동일 URI의 작은 썸네일을 먼저 표시. 그리드의 남은 요청 완료를 기다리지 않음.
- 구조상 가능한 병목을 확인한 것이며 실제 제보 지연의 전부를 이것으로 확정한 것은 아님.

## 취소 및 화면 무결성

- LatestImageLoadQueue는 요청 세대를 비교하여 지난 요청의 대기 중 decode/완료/오류 콜백을 무시.
- Android provider 썸네일 요청에는 CancellationSignal 전달. 새 이미지/영상 전환/view detach/Activity 종료 시 이전 요청 취소.
- 영상으로 넘겼는데 앞선 사진 decode가 뒤늦게 완료되어 영상 화면에 사진을 다시 표시하는 경우도 차단.
- 원본 stream fallback은 try-with-resources로 닫고 해상도를 샘플링. 플랫폼 fallback decode 자체가 즉시 취소된다는 보장은 없으며 완료 후 취소를 확인하여 UI에 반영하지 않음.
- 요청 최대 2048px. 전용 캐시는 byte 기준 16MiB. 기존 32-entry 그리드 캐시에 큰 뷰어 이미지를 넣지 않음. 화면 bitmap/진행 중 decode까지 총 16MiB로 제한한다는 의미는 아님.
- 읽기 실패 시 현재 URI의 preview 또는 placeholder 유지. EXIF 방향은 provider loadThumbnail의 기존 동작을 따르며 fallback 회전 개선은 이번 범위 아님.

## 자동 검증

LatestImageLoadQueueTest 4건 추가: 빠른 넘김의 오래된 decode 생략/취소, 지난 UI 완료가 최신 사진을 덮어쓰지 않음, 영상 전환/뷰어 이탈 후 완료 무시, 오류 후 재시도 및 지난 오류 무시.

- `gradlew.bat testDebugUnitTest assembleDebug`: PASS, **260 tests / 0 failures / 0 errors**.
- `git diff --check`: PASS (기존 줄바꿈 정규화 경고 있음).
- 기존 lint 실패는 별도 보고서 참조. 이번 변경에 대해 lint 전체 통과를 주장하지 않음.
- 폰 미연결. APK 설치/실기기 성능 수치는 확인하지 않음.

## Pre-release

- 사진 많은 장소: 상세 직후 사진 탭, 빠른 넘김, 사진→영상, Back, 회전/폭 변경, 외부 Gallery 왕복 확인.
- 수정 전후 실제 탭→사진 표시 시간 계측. PhotoPlaceViewer 로그의 decodeAndQueueMs는 bitmap bind까지이며 전체 화면 첫 프레임 시간과 다름.
- preview 선표시는 그리드 캐시에 해당 URI가 남아 있을 때만 가능. 다른 사진을 현재 사진의 preview로 쓰지 않음.

## Follow-up — 별개 지연 경로

- 장소 카드→사진 목록: showMemoryBrowserDetailScreen의 repository/live filter/앨범 조회/날짜 section 생성은 동기 경로에 남음.
- 뷰어→Back: closeMemoryPhotoViewer가 상세/모음을 다시 구성하므로 재조회/그리드 재생성 비용이 남음.
- 느린 구간을 묻는 질문을 남겼으며, 답변 전에 모든 상세 진입 성능이 해결됐다고 처리하지 않음.
- 목록 source 범위/교차 source 동일성 OPEN 항목은 REVIEW_Memory_Count_Consistency_2026-10-19.md에 별도 유지.

## 인계

- 코드: MemoryViewerImageLoader.java, LatestImageLoadQueue.java, MemoryPhotoViewer.java, MainActivity 바인딩.
- APK: app/build/outputs/apk/debug/app-debug.apk (debug, 미설치).
- 이전 Antigravity 승인은 이 새 패치에 대한 승인이 아님. 외부 리뷰와 실기기 확인 필요.
