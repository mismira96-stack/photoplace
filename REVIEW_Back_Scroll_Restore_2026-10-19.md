# 발견기록 Back 위치 복원 — 수정 및 검증

Codex, 2026-10-19. 사용자 요청: 상세에서 Back 시 목록 맨 위 대신 마지막 보던 위치 복귀. **316 tests / debug build PASS**, 장소 카드 기기 검증 완료. 모음/검색의 추가 실기기 검증 및 외부 리뷰 전.

## 변경 / P0 / P1

- MainActivity는 controller 필드 생성, 장소/모음 선택 콜백 capture, 목록 바인딩 후 bound, destroy시 clear만 연결한다. 신규 좌표/복원 범위/카드 검색/경계 판단은 별도 클래스에 있다. 기존 Back의 화면 목적지 판단을 추가 변경하지 않는다.
- `MemoryBrowserGridRenderer.java:111`, `MemoryCollectionCardRenderer.java:69`: place/collection namespace의 내부 CardKey tag로 카드에 안정적인 현재 키를 부착한다. 사진 썸네일 tag와 충돌하지 않는다.
- `MemoryBrowserScrollController.java:19`: 누른 카드의 viewport 상대 Y, 현재 scrollY, 검색어 및 앨범 포함 범위를 저장한다. ScrollView는 WeakReference로만 참조한다. 이전 root/비트맵/미디어 목록을 추가 보관하지 않는다.
- `MemoryBrowserScrollController.java:27`: 같은 범위의 다음 browser bind에서 한 번 복원. 첫 pre-draw에 실제 새 카드 위치를 찾아 이전 viewport offset이 되도록 scrollTo한다. NEW 해제 정렬로 카드가 다른 행으로 옮겨져도 해당 카드 기준이다. 다른 검색/다른 source 범위에는 적용하지 않는다.
- `MemoryScrollAnchor.java:18`: 카드가 없으면 기존 scrollY로 fallback하고 콘텐츠 길이 범위로 clamp한다. 정수 차이 계산은 long으로 해 overflow를 방지한다.
- 기존 사진 뷰어↔상세 root 복원 코드는 바꾸지 않는다. 앱 프로세스 재시작 뒤 목록 위치 영구 보존 기능은 아니다. 미디어/스냅샷/메모/identity에 쓰기 없음. 이번 기기 테스트의 정상 상세 열람에 따른 NEW prefs 갱신만 기존 승인된 동작대로 발생했다.

## 검증

- `gradlew.bat testDebugUnitTest assembleDebug`: **316 tests / 0 failures / 0 errors**, build PASS. `git diff --check` PASS. 기존 lint 부채 별도.
- 새 MemoryScrollAnchorTest4건: 일부 가려진 카드의 기존 위치, NEW 정렬 이동 후 같은 상대 offset, 삭제/필터/콘텐츠 축소 fallback·clamp, 다른 source/검색어 복원 차단.
- 펼친 SM-F971N에서 목록을 스크롤해 용인 카드 상세 진입→시스템 Back. 전 제목 bounds `[504,1152][824,1211]`, 후 `[1005,1152][1435,1211]`. NEW가 없어져 열/행 순서가 달라져도 제목 Y=1152를 유지하고 같은 카드가 보였다. 원래 열의 X까지 고정하는 기능은 아니다. source는 return에서6ms/reused=true 관측.
- `.debug/device-20261019/scroll-place-before.xml`,scroll-place-after.xml에 UI 근거 저장(Git 제외). 보호JSON7개 scroll-before/after.sha256 동일.
- 이후 폰이 다른 앱으로 전환돼 추가 자동 화면 조작을 중단했다. 기억 모음·검색 결과 복귀와 헤더 Back의 직접 확인은 미완료이며, 별도 구현 연결/순수 모델 테스트를 근거로 한다.

## Pre-release / Follow-up

대상 MemoryBrowserScrollController.java:27~45. 기억 모음/검색 결과→상세→Back, 로딩 취소 후 다른 카드, 카드 삭제/키 변경, 접힘/펼침·창 크기 변경 중 복귀에서 확인 필요. 현재는 마지막 bookmark 한 건을 다음 일치 browser bind에서 소비하므로 탭을 돌아 다시 들어오는 경우에도 pending bookmark가 적용될 수 있다. 프로세스 재시작·모든 화면의 완전한 내비게이션 상태 보존을 주장하지 않는다. 기존 viewer fallback 재구성 시 더보기 페이지 범위 등은 이번 수정 범위 밖이다.

## APK / 리뷰 요청

`PhotoPlace-1.3.6-back-scroll-20261019-debug.apk`,22:43:19 KST,9,329,947bytes. SHA256 `22F58FDB2E86DF741A18F872C0E53DB6C7D527B87417E152C6E082744F40CF3A` 로컬/Drive/기기 설치본 pull 일치. version1.3.6/code36/debug, 폰 데이터 보존 업데이트. 이전 APK 유지, 출시/커밋 없음. [Drive APK](https://drive.google.com/file/d/1B_e26pbgLlMSFdfSQfzyRwUoU_8rhNTY/view).

위 클래스/renderer tag/MainActivity callback binding 및 테스트를 검토하고 결과는 `REVIEW_Back_Scroll_Restore_Antigravity_2026-10-19.md`에 저장해 주세요. P0/P1 우선, 각 발견 사항은 Blocker / Pre-release / Follow-up, 파일·줄, 재현, 영향, 수정 제안을 포함해 주세요.
