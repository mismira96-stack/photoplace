# 뷰어 최초 다음 사진 깜빡임 — 수정 및 검증

- Codex, 2026-10-19.
- 사용자 확인: viewer-backfix APK의 Back 복귀는 나아진 것으로 보이나, 최초로 다음/다음을 누를 때 깜빡임.
- 원인 후보를 코드에서 확인: MemoryViewerImageLoader가 cache miss마다 현재 bitmap을 작은 preview 또는 placeholder로 즉시 교체한 뒤 큰 이미지를 다시 표시. 특히 preview가 없으면 빈 프레임에 가까운 임시 화면이 끼어듦.

## 수정

- 신규 ViewerImageTransition는 기존 bitmap이 있으면 다음 이미지가 준비될 때까지 유지하고 큰 이미지로 직접 교체. 다음 사진의 작은 preview가 있어도 기존 큰 사진→작은 preview→큰 사진의 중간 단계는 생략.
- 첫 진입처럼 표시 중인 bitmap이 없으면 선택한 URI의 preview 또는 placeholder를 사용.
- 새 이미지 실패/null이면 현재 URI의 preview 또는 placeholder로 전환. 이전 사진을 새 URI의 최종 결과로 남기지 않으며 preview가 없으면 접근성 설명도 실패로 변경.
- MemoryViewerImageLoader의 기존 최신 요청 token/취소 검사를 통과한 콜백에서만 transition 완료/실패 적용.
- MainActivity 추가 변경 없음. MediaStore/파일/메모/identity/링크 쓰기 없음.
- 현재 사진을 유지하는 짧은 로딩 구간에는 counter는 이미 새 인덱스일 수 있음. 화면을 비우는 flash를 없애는 선택이며, 로딩 자체를 없앤 것은 아님.

## 검증

- ViewerImageTransitionTest 4건: cache miss 다음 사진에서 빈 프레임 없음, 작은 preview 중간 표시 방지, 최초 진입 preview 유지, 실패/null 시 이전 사진 잔류 방지.
- `gradlew.bat testDebugUnitTest assembleDebug`: PASS, **269 tests / 0 failures / 0 errors**.
- `git diff --check`: PASS. 기존 lint 부채는 별개이며 이번 lint 재실행/통과를 주장하지 않음.
- 실기기 시각 확인은 아직 하지 않음. 사용자 폰에서 최초cache miss/반복 넘김/빠른 넘김/사진→영상→사진/Back 회귀 확인 필요.
- 외부 리뷰 승인 전. 이전260-test 승인 범위에 포함되지 않음.

## 전달

- Drive 마운트: `G:\내 드라이브\Codex APK\PhotoPlace-1.3.6-viewer-smooth-20261019-debug.apk`.
- 생성: 2026-10-19 09:30:19, 기존09:09/09:19 파일 유지.
- SHA256 `DED235A76458663642360EBFC8F5C532FCA7961353C32D24FFC7208029C5FECC`, 원본/Drive 마운트 hash 동일.
- 버전1.3.6 debug 유지. 별도 원본삭제/앱데이터 초기화/스토어 배포 없음.
