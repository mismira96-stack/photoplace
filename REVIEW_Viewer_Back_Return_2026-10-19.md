# 확대 뷰어 Back 복귀 지연 — Codex 수정 및 검증 기록

- 날짜: 2026-10-19.
- 제보: 09:09 APK에서도 사진을 보다가 Back으로 목록에 돌아오면 여전히 느림.
- 확인: 기존 Drive APK의 CreationTime은09:09:43. 앞선 패치는 확대 이미지 대기열만 수정했고 Back 재조회 경로는 별도 OPEN이었다.
- 판정: Back 복귀용 화면 재사용 구현/자동 검증 완료. 실기기 체감·화면 상태 회귀 확인 및 신규 외부 리뷰 전. 기존 반영 승인을 이번 패치에 적용하지 않음.

## 원인

MainActivity.closeMemoryPhotoViewer가 showMemoryBrowserDetailScreen 또는 showMemoryCollectionDetailScreen을 다시 호출했다. Back마다 앨범 존재 확인, 스냅샷/live filter/repository, Gallery resolver, 날짜 목록/그리드/메모 뷰 생성이 반복되어 사진이 많으면 비용이 늘 수 있었다.

## P0 및 변경 범위

- 신규 클래스: MemoryViewerReturnController.java, ViewerReturnState.java.
- MainActivity: 전용 컨트롤러 생성, 상세 read 전후 beginDetail/detailBound 알림, 뷰어 진입 capture, 종료 restoreOrRebuild 위임, UI mode flag 복원 콜백, pause/config/destroy 무효화만 추가.
- 기존 fallback 네비게이션 조건은 새 정책을 추가하지 않고 rebuildMemoryPhotoViewerReturn으로 이동. source revision/뷰 재사용 여부/출발 화면 선택은 컨트롤러가 판단.
- MediaStore/원본/메모/identity/OrganizationLink 쓰기 없음. 캐시는 현재 Activity 내 View 참조1건이며 디스크 저장하지 않음.

## 동작 및 방어

1. 메모리/모음 상세를 만들기 전후 HomeRevisionReader로 토큰을 비교한다. MediaStore volume version/generation과 관련 앱 데이터 파일 stat가 빌드 중 변하면 재사용하지 않는다.
2. 뷰어를 열 때 원래 content root를 보관한다. 토큰은 뷰어 open 시점이 아니라 **상세를 실제 생성한 시점**의 값이다. 상세 표시 후 사진이 추가된 상황도 Back 시 토큰이 달라져 fallback한다.
3. 동일 token이면 Back에서 같은 root를 다시 붙인다. 기존 스크롤/더보기로 추가된 grid/메모 뷰/하단 탭을 유지하며 미디어 전체 재조회/그리드 재생성은 생략한다.
4. 외부 앱/잠금/백그라운드 전환(onPause), configuration 변경, Activity 종료 때 참조를 무효화한다. 외부 Gallery에서 사진을 변경한 뒤에는 기존 fresh rebuild를 사용한다.
5. API29/불완전 권한/알 수 없는 revision, 정리 작업 중, 다른 root 또는 parent가 남은 root는 fallback한다. 따라서 모든 환경의 Back이 재사용되는 것은 아니다.
6. 단일 originating view만 보관하며 Activity/프로세스 재생성을 넘어 복원하지 않는다. 유지한 grid와 viewer bitmap이 동시에 존재할 수 있으므로 많은 더보기 이후 RAM은 실기기에서 확인할 것.

## 자동 검증

- ViewerReturnStateTest 5건: 같은 revision의 동일 객체1회 반환, source/metadata 변경과 unknown token fallback, pause/config 무효화, unknown capture의 이전 root 해제, 상세 생성 중/상세 표시 후 변경 방어.
- `gradlew.bat testDebugUnitTest assembleDebug`: **PASS, 265 tests / 0 failures / 0 errors**.
- `git diff --check`: PASS (기존 줄바꿈 경고).
- 테스트는 순수 재사용 상태/토큰 정책 검증. 실제 Android View 재부착·스크롤·수명주기를 자동 실기기 테스트한 것은 아님.
- 기존 lint 부채는 그대로. 이번 패치 lint 재실행 또는 통과를 주장하지 않음.
- 폰 미연결로 설치/탭→Back 체감 계측은 아직 안 함. PhotoPlaceViewer 로그에 returnReused/재부착까지 경과시간을 기록하되 첫 프레임 시간과 혼동하지 말 것.

## 전달 APK

구글 드라이브 데스크톱에 기존09:09 파일을 유지하고 별도 파일을 추가했다.

- 경로: `G:\내 드라이브\Codex APK\PhotoPlace-1.3.6-viewer-backfix-20261019-debug.apk`
- Drive 파일 생성: 2026-10-19 09:19:30 (로컬/KST).
- SHA256: `0A5401FC6E0DB251D7B5BF894010E3A0C2D3917A5FB44378AF012E29F34EB82E`.
- 로컬 빌드 APK와 Drive 마운트 대상 해시 동일 확인. 클라우드 동기화 완료 UI/다운로드 링크 검증은 별도.
- debug/버전1.3.6 유지. 앱 삭제/앱 데이터 초기화/스토어 배포 없음.

## Pre-release / 다음 리뷰

- 최신 추가 제보: **새 버전에서도 성남이 느림**. 앞선 Back 개선 피드백 및 다음사진 깜빡임 후속과 구분하여 재개. 느린 구간(장소→목록 / 확대·넘김 / Back→목록)을 확인 질문으로 남김. 현재 adb 기기0대라 returnReused 로그/실제 경과시간은 미확보. 기존 beginDetail/detailBound 연결 위치를 다시 대조했으며 메모리/모음 상세에 연결된 상태임. 새 성능 수치나 원인을 확정하지 않고, 이번 제보만으로 추가 추정성 APK를 만들지 않음.

- 사용자 추가 제보: **성남에서 재현**. 09:09 이전 APK인지09:19 backfix APK인지 확인 질문을 남겼으며 아직 답변 전. 폰도 현재 adb 미연결이다. 이 제보를 backfix 실패/성공 중 어느 쪽으로도 임의 판정하지 않는다. 최신 APK에서도 재현되면 returnReused=false의 원인(unknown/changed revision, pause/config 무효화, root mismatch) 또는 returnReused=true의 재부착/레이아웃 비용을 먼저 구분할 것.

- 메모리 상세→사진→Back: 같은 스크롤, 더보기로 추가한 사진 수, 날짜 메모, 버튼/하단 탭, 재진입 확인.
- 모음 상세→사진→Back: 모음 날짜/장소 그룹과 모음 모드 복원 확인.
- 빠른 넘김/반복 Back, 외부 Gallery 왕복/실제 사진 변경, 접기·펼치기/회전, 잠금/백그라운드, 권한 변경 확인.
- 짧은 revision 조회는 여전히 Back UI 경로에 있고 provider 지연까지 제거한 것은 아님. 실제 returnReused 로그와 첫 프레임을 비교해야 함.
- 수량 source 범위 및 교차 source 중복 정책 OPEN은 이 패치와 별개로 유지.
