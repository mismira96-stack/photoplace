# 폴드 전체 화면 로딩 안내 배치 수정

Codex, 2026-10-19. 사용자: 펼친 전체 화면에서 로딩 안내의 레이아웃이 어색함. **312 tests / debug build PASS**, 펼친 기기 캡처 확인, 외부 리뷰 전.

## 원인

MemoryScreenLoadController.showNotice는 Activity content의 전체 폭(MATCH_PARENT)에 좌우18dp/하단90dp 고정 여백으로 안내를 붙였다. 본문과 하단 탭은 넓은 창에서600dp 폭으로 가운데 정렬된다. 펼친 폰에서는 안내만 양옆으로 넓어지고, 시스템 바·작업표시줄·하단 탭의 실제 높이와90dp가 맞지 않아 탭 위로 겹칠 수 있었다. 기존 unfolded UI dump에서도 안내 label과 하단 탭의 y 범위가 겹쳤다.

## 수정 / P0 / P1

- `app/src/main/java/com/example/gallerysorter/MemoryLoadingNoticeLayout.java:11`: 순수 배치 계산 클래스로 분리. 현재 viewport의 가용 폭 안에서 최대600dp 본문 폭−좌우18dp에 해당하는 안내 폭을 계산하고 가운데 배치한다. 하단은 실제 viewportBottom보다12dp 위에 놓는다. 좁은 창/오프셋 있는 viewport도 부모 범위로 제한한다.
- `app/src/main/java/com/example/gallerysorter/MemoryScreenLoadController.java:124`: 기존 화면 root의 첫 자식 ScrollView를 본문 viewport로 사용한다. 해당 구조가 아니면 origin을 기준으로 사용한다. getLocationInWindow로 Activity content 좌표로 변환해 시스템 바 고정 높이를 가정하지 않는다.
- `app/src/main/java/com/example/gallerysorter/MemoryScreenLoadController.java:138`: parent/viewport layout 변화에 맞춰 배치를 다시 계산한다. 값이 바뀔 때만 LayoutParams를 갱신하고, clear/cancel/완료/close에서 두 layout listener를 해제한다. 기존 요청 세대/원래 화면 guard 및250ms 지연 표시 정책은 유지한다.
- 이번 MainActivity 추가 변경0줄. 미디어/스냅샷/메모/identity/열람 정책 변경 없음. 기존 데이터 보호JSON7개 해시가 직전 counts-seen 검증 이후 동일하다. source/query 속도를 바꾸는 수정은 아니다.

## 검증

- `gradlew.bat testDebugUnitTest assembleDebug`: **312 tests / 0 failures / 0 errors**, debug build PASS. `git diff --check` PASS. 이전 lint 부채는 별도다.
- 추가3개 테스트: 펼친932 폭에서600 본문 정렬/실제 탭 위 간격, 접힌475 폭에서439 안내 폭/바 높이 변경 추종, 좌측 inset 있는 viewport 및 극히 좁은 창의 범위 제한.
- 연결 SM-F971N의 펼친 화면2448×1848에서 데이터 보존 업데이트 후, 홈→발견기록을 누르고 실제 로딩 안내가 떠 있는 시점의 캡처를 확인했다. 안내가 홈 본문/하단 탭의 가운데 폭과 맞고 탭 위에 간격을 둔다. 캡처는 Git 제외 `.debug/device-20261019/loading-layout-expanded.png`. 개인 사진이 포함되어 보고서에 이미지를 복제하지 않는다.
- 실제 접힌 하드웨어 상태와 split-window/키보드/매우 큰 글꼴에서의 직접 확인은 아직 수행하지 않았다. 순수 배치 테스트와 layout 추종 구현을 근거로 하며 모든 실기기 크기에서 확인했다고 주장하지 않는다.

## Pre-release / 외부 리뷰 요청

MemoryScreenLoadController.java:124~173의 viewport 선택/좌표 변환/layout callback 해제와 MemoryLoadingNoticeLayout.java:11의 폭·하단 계산을 검토해 주세요. 접힘/펼침 전환, 좌우 시스템 inset, landscape/split-window, 로딩 중 취소/오류/회전 시 잔류 View/listener 여부를 확인해 주세요. origin이 일반 bottom-tab root가 아닐 때 fallback 배치도 확인 대상이다.

전체 리뷰 결과는 루트 `REVIEW_Loading_Notice_Layout_Antigravity_2026-10-19.md`에 Blocker / Pre-release / Follow-up, 파일·줄 번호, 재현 조건, 영향, 수정 제안과 함께 저장해 주세요. 이전309-test 패치 승인과 구분해 주세요.

## APK

최신 빌드를 폰에 설치하고 홈으로 복귀했다. Drive `PhotoPlace-1.3.6-loading-layout-20261019-debug.apk`,9,325,799bytes, 빌드22:35:59 KST,version1.3.6/code36/debug 유지. SHA256 `D5FA2FE858FC9B9C2094E070D42738717C7DBEB385C8569A146B12DE5913080F` 로컬/Drive 동일. 이전 파일 보존, 커밋/스토어 배포 없음. [Drive APK](https://drive.google.com/file/d/19ysQyie2xsQb7JhAeD-lchD8ZshymEzn/view).
