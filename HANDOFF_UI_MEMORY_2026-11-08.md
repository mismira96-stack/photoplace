# PhotoPlace 인계 — 2026-11-08 16:03 KST 체크포인트

이 문서는 채팅 이동 시 이어서 작업하기 위한 **구현 중 상태**다. 완료/출시/독립 리뷰 승인 표식이 아니다. 사용자가 먼저 WORKLOG와 남은 작업을 정리해 달라고 요청해 여기서 상태를 고정한다.

## 시작할 때

- 실제 소스: `C:\work\projects\PhotoPlace`. OneDrive Photo Place는 구 첨부 폴더다.
- `AGENTS.md`, `GEMINI.md`, `UI_GUIDE.md`를 먼저 읽는다. MainActivity에는 생명주기·뷰 바인딩·위임만 허용한다. 도메인/필터/집계/판단은 전용 클래스로 분리한다.
- 오래된 수정·미추적 파일이 매우 많다. 광범위한 reset/clean/일괄 commit 금지.
- 실제 사진·EXIF/GPS·촬영 날짜·메모·stableMemoryId 보존. 실제 폰 사진을 임의로 휴지통 이동/복원하지 않는다. 사용자가 테스트한다.
- 현재 단계는 테스트 수정. **프로덕션 제출 지시 없음**. 기존 출시 code56과 새 테스트 버전을 혼동하지 않는다.

## 설치/빌드와 검증된 기준점

- 폰 `R5KL503VHQR` 연결됨(16:03 확인), 에뮬레이터 `emulator-5584` 연결됨.
- 마지막으로 설치·실행 확인한 폰 버전: **1.3.25 / code61**.
- 설치는 항상 `adb -s R5KL503VHQR install -r ...` 업데이트. uninstall/clear 금지. Play 서명이어야 기존 데이터 유지 설치 가능.
- code61 APK: `photoplace-1.3.25-code61-ui-test-play-signed.apk`.
- SHA256: `2062267566e5d90cd24e9c46e20df32b6d5dece7bbebc292403ebd5ed2be9bb5`.
- Play 앱 서명 SHA256: `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d`.
- code61 최초 설치 시각 `2026-10-20 00:24:37` 유지, 업데이트 `2026-11-08 15:50:44` 확인.
- app/build.gradle은 아직 code61/1.3.25. **다음 테스트 서명 빌드는 code62/1.3.26으로 증가**해야 한다. 현재 수정분은 code61 설치 APK에 들어 있지 않다.
- code61 기준 Debug/Release 각482개 PASS. `.debug/manual-place-2026-11-08/polish61-build.log` bundleRelease 성공.
- API33 전용 테스트 패키지 `com.photoplace.memoryprobe`에서 final61 `polish`, `grid`, `viewer`, `flow` PASS. `final61-*.log` 참조. 개인 사진 변경 없이 전용 fixture로 검증했다.
- UI 캡처: 같은 디렉터리 `final61-place-picker-preview.png`, `final61-trash-confirm-preview.png`, `final61-grid-preview.png`. 더미 썸네일과 실제 폰 사용자 검증을 구분한다.
- 현재 체크포인트 수정 후 검증 로그는 `checkpoint62-build.log`(파일명은 차기 작업 식별자이며 앱 버전을 올렸다는 의미 아님). 결과는 WORKLOG 최신 항목 참조.

## code61까지 구현되어 설치된 기능

- 위치 없는 사진을 기존/신규 위치기록에 연결, 원본 GPS/날짜 무변경. 사진·동영상 receipt와 날짜/메모 연결 유지.
- 발견기록/단건 뷰어에서 실제 선택 파일의 일반 휴지통 이동. 기존 중복 원본 정리와 별개.
- 선택 화면 3열 가상화 격자, 썸네일 비동기/유계 캐시. 파일명·경로는 기본 화면에서 숨기고 길게 누른 정보에서 표시.
- 검색 가능한 장소 선택 시트, 실제 최근 선택, 실제 대표 썸네일, 이름/수량/날짜, 새 기록 입력.
- 앱 소유 팝업 스타일 적용, 시스템 바 insets, 뷰어의 불필요한 갤러리 열기 제거.
- 휴지통 감사 DONE 후 정리 화면 내 목록 자동 복귀. **메인 메모리뷰까지 뒤로 갔을 때의 갱신은 아래 새 버그로 별도 확인 필요**.
- 비동기 SortStartController에서 충돌 예약 및 입력 저장/Worker enqueue. 단, 성공 안내가 너무 일찍 표시되는 UI 호출부는 아직 아래 미해결.

## 체크포인트에서 실제로 수정한 내용 (아직 폰 미설치)

1. `MemoryManagementController`: RECORD/SINGLE의 “휴지통으로 보낼 사진을 선택하세요”와 ADD 정상 완료 후 상시 안내 문구 제거. 빈 status는 GONE. 오류는 status를 다시 표시하도록 처리.
2. 선택 0개일 때 하단 문구를 “사진을 선택해 주세요” → “선택 0개”로 바꿈. **이후 사용자가 하단의 ‘사진 1장 선택’도 불필요하다고 지적해, 하단 수량 제거/상단 한 곳만 수량 표시로 추가 수정해야 한다. 아직 미반영.**
3. `MemoryManagementEntry.detailOptions`: 텍스트 대신 작은 휴지통 ImageView. 클릭 영역은 요약 렌더러에서48dp. 접근성 description은 기존 “사진 정리” 유지.
4. `MemoryDetailSummaryRenderer`: 카드 상하 여백16→10dp, 날짜 위10→6dp, 액션68→48dp. 사진 격자/앨범 추가 액션은 보존.
5. `MemoryPhotoViewer`: 작은 휴지통 아이콘 옆/아래 “정리” 라벨 제거,48dp 클릭 영역 유지.
6. `DiscoverySortStartFeedback.java`, `MemoryAlbumConfirmationText.java` **헬퍼 파일만 생성. 호출부 연결/전용 테스트는 아직 안 됐다. 수정 완료로 취급하지 않는다.**

## 사용자 최신 결정 (이전 리뷰/시안보다 우선)

- 명확한 상황을 다시 설명하는 문구는 지저분하므로 제거한다. 제목/선택 상태/버튼으로 충분하면 반복하지 않는다. 오류·권한·복구 등 실제 다음 행동이 필요한 안내는 남긴다.
- “사진을 선택해 주세요”, “휴지통으로 보낼 사진을 선택하세요”, 하단 “사진 1장 선택” 등 제거. 상단 한 곳의 짧은 선택 수만 사용.
- **앱 소유 ‘휴지통으로 이동할까요?’ 확인 팝업 자체 제거** 요청 확정. 파일 재확인/영속 저널/배치/감사는 그대로 유지하고 Android 공식 `createTrashRequest` 승인창으로 직접 진행한다. 시스템 승인 우회 아님.
- 발견기록 상세/단건 화면은 **휴지통 중심**으로 좁힌다. 장소 변경·연결 해제 UI는 사용자가 불필요하다고 판단한 현 범위. 위치 없는 사진 화면은 기록 추가 흐름.
- 발견기록 목록뿐 아니라 **각 기록 안에서도 검색 필요**. 목록은 기존 `MemorySearchHeaderRenderer`/`MemoryBrowserSearch` 검색이 이미 존재한다. 기록 안 검색은 새 구현 필요.
- 세부 기록 검색 제안: 작은 검색 아이콘, 날짜·메모·파일명·사진/동영상 종류 검색. 현재 로드한 데이터만 읽고, 사진 내용 AI검색/새 전체 스캔 없음. 이 범위를 사용자에게 설명했으며 아직 구현 안 됐다.
- 원본 정리/사진 정리 큰 강조 버튼 대신 작은 휴지통 아이콘. 요약 카드 밀도 축소. 사진 격자는 주인공으로 유지.
- 홈 반복 결과 설명 축소, 내부 처리량 ‘확인한 발견 항목 N개’ 제거, 결과 카드 높이 축소.
- 새 UI는 PhotoPlace 기존 색상·간격·타이포·카드·버튼을 재사용. 기본 Android 최종 외형 금지(진짜 OS 승인창 예외).

## 가장 먼저 처리할 새 기능 버그 — 미해결

사용자 보고: **사진을 휴지통으로 이동한 뒤 다시 Back하면 이전 사진이 메모리뷰에 보인다.**

확인된 코드 근거:

- `MemoryManagementController.afterTrash()`는 정리 Activity 자체의 rows만 reload한다.
- `MainActivity.onResume()`는 최근 앨범 요약 캐시는 invalidate하지만, `refreshActivePlaceDetailAfterExternalChange()`는 `recentPlaceDetailMode`만 처리한다. 발견기록 상세(`memoryBrowserDetailMode`)나 그 뷰어를 갱신하는 분기가 없다.
- `MemoryViewerReturnController`는 onPause에서 invalidate되지만, **현재 화면에 이미 바인딩된 메모리뷰 자체는 다시 불러오지 않는다**. 따라서 외부 정리 Activity 후 Back에서 오래된 thumbnail/view가 남을 가능성이 높다.
- `HomeRevisionReader`에는 MediaStore generation과 manual revision이 들어 있다. `MemoryDetailLoadController`/`MemoryScreenLoadController`는 revision별 캐시를 쓴다. 새 resume/return 전용 컨트롤러에서 변경 감지 → origin 기록 재로딩을 위임하고 Main에는 판단 로직 추가하지 말 것.
- 실제 MediaStore에서 이동이 실패한 것인지/이전 화면만 남는 것인지는 **추가 실기기 재현으로 확정 필요**. 현재는 사용자 보고와 코드상 갱신 누락까지 확인했고 원인 확정/수정 완료 아님.
- 노트만 남는 기록 및 날짜 섹션은 계속 접근 가능해야 한다. 메모리 파일을 삭제하거나 discovery_snapshot을 강제로 고쳐 해결하지 않는다.

## 남은 작업 순서

### P1: 정확성/갱신
1. 휴지통 이동→정리 화면→Back→원래 기록/뷰어에서 이동한 파일이 사라지는지 회귀 재현. 취소 시 원본 보존, 감사 미완료/실패 시 오판 금지.
2. 전용 return/resume 컨트롤러로 기록 상세/뷰어/필요한 목록 갱신. 불필요한 전체 스캔과 검색 성능 회귀 피하기.
3. 사용자 요청대로 앱 자체 확인창 제거. `confirmTrash()`에서 실제파일 검증을 수행하는 `MemoryTrashExecution.start(chosen,route)`로 직접 연결. OS 승인/취소 테스트를 전용 fixture에서 재실행.

### R2 리뷰 보완
4. `REVIEW_Memory_Management_PhaseBC_R2_Antigravity_2026-11-08.md`의 R2-UI-1은 최신 사용자 범위와 충돌. 메뉴를 자동 복구하지 말고 followup에 ‘의도적 trash-only 범위 변경, 독립 리뷰 재확인 필요’로 설명한다. 기존 unassign 저장 API 자체를 임의 제거하지 않는다.
5. R2-UI-2: `MainActivity.startPreparedDiscoveryAlbums()` 약8650의 `runCopy()` 직후 `isWorking` 기반 성공 UI 제거. `startBackgroundSortWorker()` 약1932 실제 started 콜백으로 성공 안내 위임. 동영상 권한 승인 재진입 약4381에도 같은 조기 안내 분기 있으므로 함께 제거. 새 `DiscoverySortStartFeedback` 헬퍼를 연결한다. pending flag는 성공/실패/권한 취소 시 정확히 정리.
6. R2-UI-3: `DiscoveryOrganizeConfirmDialog` 약88의 사진0장 문자열을 `MemoryAlbumConfirmationText.added(photos,videos,moveVideos)`로 위임. 영상 단독 “새로 넣을 동영상 1개” 표시, 중복/제외 수량 보존. 새 헬퍼는 아직 미연결.

### UI/검색
7. 하단 선택 안내/선택 수 제거, 상단에만 “선택 N · 전체 M”로 압축. 진행/오류 status가 GONE에 가려지지 않게 검증.
8. 발견기록 상세 내 검색 구현. 기존 날짜별 페이지 renderer와 메모 preview 재사용. 검색 지우면 원래 전체 섹션, 검색 중에도 파일/날짜/ID 변경0. note-only 섹션·검색결과0·대량사진 성능 확인. 기존 목록 검색 재사용/시각 통일.
9. 날짜 메모 연필 의미를 작은 ‘메모’ 라벨로 보강(아직 미구현). 메모가 있으면 기존 한 줄 preview 보존.
10. 홈 `DiscoveryResultText.summary`의 내부 처리량 제거, 반복 inline완료 notice 억제. `PreviewRefreshNoticeRenderer`는 결과 카드가 보이면 완료 안내 중복 표시 안 함. 검사 중 상태/오류는 유지. `discoveryResultSummary` 복원 경로도 같은 helper 위임.
11. 홈 결과 카드 padding/중복 hint 축소, 주요 CTA/사진콘텐츠 보존. 미디어 종류가 불명확한 합계를 사진 N장으로 왜곡하지 않음. updatedPlaces를 ‘새 장소’로 오표기하지 않음(기존 테스트).
12. UI_GUIDE 및 AGENTS/GEMINI 강화: 유사 화면 우선 재사용, 새 패턴은 guide 정의 후 적용, 기본 AlertDialog/ListView/Spinner/버튼 외형 금지(커스텀 가상화 위젯 사용 자체 금지 아님), 리뷰 질문 ‘기존 화면과 시각적으로 같은 앱처럼 보이는가’. 반복 안내 제거 원칙도 기록.

### 검증/인계/테스트 APK
13. 관련 단위 테스트(검색/비동기 성공 안내/영상단독 수량/갱신)와 Debug/Release, 에뮬레이터 실제 렌더링 확인. 기존482개 PASS를 신규 수정 검증으로 재사용하지 않는다.
14. helper의 기존 polish 테스트는 **앱 확인창이 존재한다는 계약**이므로 사용자 최신 요구대로 갱신한다. 삭제 기능 안전 검증 자체를 지워 PASS 만들지 않는다.
15. code62/1.3.26 버전 증가, 릴리즈 빌드/Play 서명/설치 `-r`. 테스트 서명을 위한 internal draft만 사용. 새 변경 프로덕션 제출 아님.
16. WORKLOG/리뷰 요청/색인/TODO/소스 hash snapshot 갱신 후에만 READY 재개. 현재 READY JSON과 snapshot은 R2/code57 기준으로 오래됐으므로 신뢰 금지.

## 빌드/프로브/서명 실행 참고

- Java: `C:\Program Files\Android\Android Studio\jbr`; SDK `%LOCALAPPDATA%\Android\Sdk`.
- `gradlew.bat testDebugUnitTest testReleaseUnitTest bundleRelease --console=plain`.
- 프로브: `verification/memory-management-probe`, 앱 ID `com.photoplace.memoryprobe`, instrumentation `com.example.gallerysorter.MemoryManagementProbe`. 실제 메인 앱/폰에 fixture 넣지 않는다.
- 서명 도구: `.debug/manual-place-2026-11-08/play-device-test.cjs`의 inspect/upload/download. 자격증명은 외부 파일, 내용/토큰 출력 금지. 필요시 `C:/Users/mismi/.codex/skills/photoplace-release/SKILL.md`를 적용한다.
- 다음 env: PP_TEST_CODE=62 / PP_TEST_AAB=photoplace-1.3.26-code62-ui-test.aab / PP_TEST_NOTES=release-notes-1.3.26-test-ko-KR.txt / PP_TEST_APK=photoplace-1.3.26-code62-ui-test-play-signed.apk.
- phone install 전 사용자 테스트/진행중 Worker 상태 확인. 앱 강제종료/교체로 진행 중 작업 끊지 않는다.

## 리뷰 협업 상태

- R2 안티 보고서는 backend B1/B2/B3 VERIFIED. UI action omission은 현 사용자 결정과 범위 충돌. 완전 독립 승인이라고 요약하지 않는다.
- 사용자가 안티에게 직접 메시지 보내는 대신 **파일 표식만** 요청했다. 외부 메시지 전송/자동화 생성하지 않는다.
- 폴더 안 새 리뷰 파일은 활성 작업 중 확인 가능하지만, 채팅 종료 후 무인 상시 모니터링은 설정하지 않았다.
- 다음 에이전트는 이 문서와 최신 WORKLOG 기준으로 재개. ‘다 구현/설치됨’이라고 답하지 않는다.
