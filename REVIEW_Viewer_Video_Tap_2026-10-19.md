# 동일 동영상의 진입 경로별 재생 차이 — 수정 및 자체 검증

Codex, 2026-10-19. 외부 리뷰 및 실기기 검증 전.

## 원인과 재현

사용자는 같은 동영상도 어떤 때는 재생되고 어떤 때는 Gallery 안내가 나온다고 제보했다. 목록에서 영상 직접 선택과 사진 뷰어에서 다음/스와이프로 영상 도착이라는 경로 차이를 설명한 뒤 사용자가 이에 동의했다.

- MainActivity.java의 memoryPhotoThumbnailRenderer는 VIDEO 항목을 openMediaUri로 전달하여 외부 기기 재생 앱을 연다.
- MemoryPhotoViewer는 영상에 도착하면 이미지를 비우고 Gallery 안내만 표시했다. 하단 Gallery 버튼은 동작했지만 영상 영역 자체는 터치를 소비하고 재생 동작이 없었다.
- 따라서 파일이 같아도 경로별 UI/행동이 달랐다. 실제 파일의 코덱/접근 실패를 재현한 것으로 간주하지 않는다. PhotoPlace에는 현재 자체 동영상 재생기가 없다.

## 수정 / P0 / P1

- `app/src/main/java/com/example/gallerysorter/MemoryPhotoViewer.java:92`: 영상 영역은 “눌러서 재생”, 보조 문구는 기기 동영상 재생 앱으로 연다는 안내로 변경.
- `app/src/main/java/com/example/gallerysorter/MemoryPhotoViewer.java:132`: 영상 영역 click을 현재 MemoryPhotoItem의 Host.openExternal에 연결. 목록의 직접 재생과 동일한 URI/MIME 전달 경로를 사용한다.
- `app/src/main/java/com/example/gallerysorter/MemoryPhotoViewer.java:157`: 시스템 touch slop 안의 탭만 performClick으로 전달. 기존 이전/다음 및 스와이프는 유지하며 영상 도착 자체로 외부 앱을 자동 실행하지 않는다. 클릭 접근성 경로도 동일하다.
- `app/src/main/java/com/example/gallerysorter/MemoryViewerVideoAction.java:14`: 영상/유효 URI 조건은 전용 순수 클래스에 격리. 사진/빈 URI/null 항목은 외부 실행하지 않는다.
- 이번 MainActivity 추가 변경0줄. 원본 미디어/MediaStore/스냅샷/메모/링크 저장 변경 없음. 다중 장소 캐시 수정 포함.

## 검증

- `gradlew.bat testDebugUnitTest assembleDebug`: **293 tests / 0 failures / 0 errors**, debug 빌드 PASS. `git diff --check` PASS.
- 새3개 테스트: 동일 video item/URI 전달, 사진/null/빈 영상 URI 실행 금지, 미세 손 움직임과 스와이프/수직 드래그 구분.
- 테스트는 순수 동작 정책 검증이며 실제 Android 터치 dispatch/기기 플레이어 실행 검증은 아니다. 이전 확인 당시 adb 연결 기기0대. 기존 lint 부채는 별도다.

## Pre-release

대상 MemoryPhotoViewer.java:132 및 :157. 같은 영상을 목록에서 직접 선택해 재생 → 복귀 → 인접 사진을 선택하고 다음으로 영상 도착 → 중앙을 눌러 재생해 동일 파일인지 확인. 영상 위 좌우 스와이프/수직 드래그는 외부 앱을 열지 않아야 한다. TalkBack click, 외부 플레이어에서 Back, pause/복귀 시 기존 상세 복원 정책도 확인해야 한다. 외부 앱이 설치되지 않았거나 URI 접근/코덱에 실패하는 문제까지 해결했다고 주장하지 않는다.

## Follow-up

자체 영상 재생기 도입은 별도 작업이다. 이번 수정은 기존 두 경로의 명시적 재생 동작을 연결한다. 발견기록 첫 진입의 전체 repository/live-filter 준비와 수량 일관성 과제는 OPEN 유지한다.

## 외부 리뷰 요청

이 보고서와 MemoryPhotoViewer.java / MemoryViewerVideoAction.java / MemoryViewerVideoActionTest.java를 검토해 주세요. 결과는 프로젝트 루트 `REVIEW_Viewer_Video_Tap_Antigravity_2026-10-19.md`에 Blocker / Pre-release / Follow-up, 파일·줄 번호, 재현 조건, 영향, 수정 제안과 함께 저장해 주세요. 이전 패치 승인과 이번 패치 승인을 구분해 주세요.
