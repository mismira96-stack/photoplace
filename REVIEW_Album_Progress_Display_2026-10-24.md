# 상세 화면 앨범 작업 진행 표시 누락

## 현상과 실기기 근거

- 날짜: 2026-10-24 (Asia/Seoul), Play 서명 테스트38 설치 후 사용자가 성남 추가 작업 실행.
- 사용자가 수원 때는 진행률이 있었으나 성남 상세에서 시작하니 없다고 보고. 홈/Back 시 백그라운드 계속 진행 안내가 표시됐다고 보고.
- 진행 중 UI dump `.debug/device-20261024/progress-window.xml`: 성남 상세, 앨범에 추가, 사진2259/영상30 및 날짜 그리드만 존재. 진행 문구/수량/배너 없음.
- 같은 시간 앱 로그에서 작업 알림4101이 계속 갱신됨. `progress-completed-logcat.txt`에서 01:15:13.060 SortWorker SUCCESS 확인. 사용자가 작업 완료를 보고했다.
- 이 성공 로그는 실제 복사 파일 전부의 내용 무결성이나 누락 없음을 증명하지 않는다. 이번 점검은 진행 UI 문제를 대상으로 한다.

## 기존 구현과 원인

- 기존 코드: SortWorker → SortProgressStore → Activity의 기존 progressUpdateRunnable/refresh → renderProgress → updateProgressBanner. 새로운 작업자/저장소/폴링 루프를 만들 필요가 없다.
- 기존 addWorkingBanner는 화면을 만들 때 isWorking=true인 경우에만 배너 및 TextView 참조를 생성했다. 비작업 상태로 열린 상세에서 확인창을 통해 작업을 시작하면 화면을 다시 만들지 않으므로 배너 참조가 null인 채로 남는다.
- 기존 renderProgress는 홈의 progressBar/progressText 등 참조가 없으면 배너 갱신 전에 return하는 구조였다. 화면별 표시가 홈 전용 참조에 종속돼 있었다.

## 수정 범위

- `WorkingProgressBanner.java`: 기존 addWorkingBanner 표시를 전용 뷰로 분리. 모든 기존 addWorkingBanner 호출 지점에서 숨겨진 슬롯을 미리 붙이고 작업 시작 시 노출한다. 배너에 준비 상태, 수량/퍼센트, 수평 진행 막대 표시. 작업 종료 시 GONE.
- 현재 attached content의 tagged banner를 찾아 갱신하므로 이전 화면 TextView를 붙잡고 갱신하지 않는다. 별도 스레드/타이머/MediaStore 접근 없음.
- `WorkingProgressPresentation.java`: 진행 값과 문구의 표시용 변환/클램프만 담당. 기존 activeProgress/SortProgressStore 수치 사용. 새 진행 저장 체계를 추가하지 않았다.
- `MainActivity.java`: addWorkingBanner/updateProgressBanner는 위 전용 뷰 호출로 대체. setWorking에서 현재 배너 갱신 연결. renderProgress는 홈 전용 뷰 early return 전에 배너 갱신을 호출한다.
- 불필요한 progressBannerTitleText/progressBannerDetailText 필드를 제거했다. 신규 판단/퍼센트 계산은 Activity에 추가하지 않았다.
- 작업 실행/복사/휴지통/증분 분석/홈 캐시/상세 미디어 캐시 코드는 수정하지 않음. 전체 Activity에 대한 전수 리뷰 완료를 의미하지 않는다.

## 자동 검증

- WorkingProgressPresentationTest 4개: idle→준비→수치→완료→다음 작업, 백그라운드 복귀 수치, 음수/초과/최대 정수 클램프, 준비 문구 fallback.
- `testDebugUnitTest testReleaseUnitTest assembleRelease`: BUILD SUCCESSFUL, Debug331 / Release331 tests, failures0 / errors0.
- git diff --check 통과. CRLF 정규화 경고와 기존 Java8/deprecated/unchecked 빌드 경고 있음. 전체 lint는 실행하지 않음.
- 로그: `.debug/device-20261024/progress-fix-verification.log`.

## Pre-release / 리뷰 항목

1. 새 배너를 적용한 빌드에서 실제 숫자 증가/준비 표시/완료 숨김은 자동 JVM 테스트만으로 입증되지 않는다. 새 실기기 작업에서 확인해야 한다.
2. 작업 중 앱 Home/Back/시스템 홈/복귀, 다른 화면에서 시작, cached detail 재부착, 접힘/펼침에서 현재 화면의 배너가 갱신되는지 확인.
3. 배너는 본문 상단 슬롯이므로 사용자가 아래로 스크롤하면 화면 밖일 수 있다. 이번 패치는 고정 overlay로 바꾸지 않는다.
4. attached view 탐색은 기존 progress 갱신 시 UI 트리에서만 수행한다. 파일/미디어 조회를 추가하지 않았으나 큰 화면 트리에서의 UI 비용은 실기기 미계측.
5. 완료 전후 isWorking/Store active 전이와 Worker 결과 저장 순서는 기존 구조를 유지했다. stale 진행 상태/복구 전체 설계를 새로 검증한 것은 아니다.

## 설치 상태

표시 패치는 테스트39(1.3.9)로 내부 초안 등록 후 Play 서명 universal APK를 받아 adb install -r로 설치했다. 최초 설치 시각 유지 및 기존 Home 기록 표시 확인. 산출물 해시는 WORKLOG_2026-10-24.md 최종 상태 참조. 표시 패치의 실제 진행률 증가는 사용자 후속 작업에서 검증해야 하며 에이전트가 임의 사진 복사를 실행하지 않았다.
