# 폰 설치/아이콘 및 정리 직후 기록 안내 점검

- 날짜: 2026-10-24 (Asia/Seoul), 기기 R5KL503VHQR.
- 앱: Play 스토어 설치 com.photoplace.app 1.3.7(37), targetSdk36.
- 제품 코드 수정 0줄. P0 신규 MainActivity 로직 추가 없음. 앱 재설치/데이터 초기화/강제 종료/캐시 초기화 및 사용자 사진 이동/복사/삭제는 하지 않았다.
- 기기에는 UI dump와 screenshot 진단 파일만 생성했다. 로컬 진단은 `.debug/device-20261024/`에 보관.

## 아이콘 미표시 — Follow-up

- 사용자: 홈뿐 아니라 앱스 목록/검색에도 보이지 않음.
- dumpsys package: user0 installed=true, hidden=false, suspended=false, enabled=0(default). 이 hidden 필드는 Samsung 런처의 숨긴 앱 설정과 동일하지 않다.
- MAIN/LAUNCHER resolve 및 query-activities: 활성 MainActivity 1개 확인.
- 현재 resumed Activity가 PhotoPlace이고 홈 UI 렌더링도 확인했다. 앱 삭제/실행 항목 누락으로 설명되지 않는다.
- 삼성 런처 숨김 설정은 직접 확인하지 못했다. 추정으로 확정하지 않는다. 앱스 검색 옆 더보기 → 설정 → 홈 및 앱스 화면에서 앱 숨기기의 숨긴 목록을 확인할 것.
- 삼성 공식 안내: https://www.samsungsvc.co.kr/solution/40761
- launcher 데이터 삭제나 앱 재설치는 사용하지 않는다.

## 중복 원본 — 읽기 전용 확인

- MediaStore의 is_trashed=0 사진 조회에서 Pictures/성남에서/ 373행, Pictures/수원에서/ 950행, DCIM/Camera/ 2,132행 확인. 영상은 이번 집계에 포함하지 않았다.
- Camera와 파일명+크기가 같은 후보: 성남 1행, 수원 50행. 이 값은 중복 전체 수량이나 내용 동일성 확정 수량이 아니다. Camera 이외 원본 폴더, 이름 변형, 다대다 관계는 미포함.
- 성남 원본/대상 URI ID 177561/308781, 각 988,492 bytes: 읽은 전체 바이트 SHA256 동일.
- 수원 원본/대상 URI ID 297745/315351, 각 1,175,059 bytes: 읽은 전체 바이트 SHA256 동일.
- 따라서 성남/수원 각각 최소 한 쌍의 실제 내용 중복을 확인했다. 어떤 파일도 휴지통 후보로 등록하거나 변경하지 않았다.

## 정리 직후 ‘기록 없음’ 안내 — Pre-release 재현 필요

- 처음 점검한 홈 UI에 정리 완료 848개 · 새 장소 1개 표시.
- 사용자 후속 정정: 지금은 정상이며, 수원 위치 앨범 만들기가 끝난 직후에 팝업이 나왔음.
- 일시적 안내로 기록하며, 지속적인 Discovery 삭제/손상으로 단정하지 않는다. 당시 화면/원인 로그가 없어 정확한 원인은 미확정.
- 소스상 MemoryScreenLoadController.java:76에서 detail supplier가 null이면 ‘기록을 찾지 못했어요.’, :91에서 예외 시 ‘기록을 불러오지 못했어요.’ 안내 경로가 있다. 사용자 문구와 동일 사건이라는 증거는 없다.
- 다음 재현 시 정리 완료→Memory 복귀의 key/includeOrganizedSources/OrganizationLink 저장 및 read revision 순서를 계측한다. 데이터 재생성/전수 분석/캐시 삭제를 재현의 선행 조치로 쓰지 않는다.
- Play release는 non-debuggable로 run-as 접근 불가. 개인 앱 저장소의 snapshot/link/메모 파일을 직접 대조하지 못했으며 우회하거나 debug APK로 덮어쓰지 않았다.

## 검증 범위

ADB 설치/런처 조회, 현재 화면 읽기, MediaStore 읽기, 표본 2쌍 바이트 해시 대조, 관련 코드 정적 읽기. 제품 수정이 없어 자동 테스트/빌드 재실행 없음. 아이콘 문제와 일시적 안내의 원인 확정 및 수정은 미완료.

## 송파구 사용자 재현 시도 — 2026-10-24

- 사용자가 직접 송파구 앨범 정리를 실행했고 ‘재현 안 됨’으로 보고. 에이전트는 폰 조작 없이 logcat만 수집했다.
- 기기 로그 시각 00:55:20.997 SortWorker 시작 → 00:55:26.823 SUCCESS. Worker 성공만으로 모든 미디어 복사 또는 전체 기능 무결성을 보장하지 않는다.
- 이후 00:55:32 browser source231ms/bind25ms, 00:55:34 detail source206ms/bind10ms 로그 확인.
- 00:55:44 browser reused=true source1ms/bind28ms, 00:55:45 Home unchanged=true/check12ms. 캐시 재사용 경로가 이번 실행에서 동작함을 확인했으며 스캔/분석 캐시 전체 성능 측정은 아니다.
- 수집한 해당 앱 로그에 screen load failed/FATAL EXCEPTION은 확인되지 않음. null detail의 ‘기록을 찾지 못했어요’ 경로는 별도 실패 로그가 없어 로그 부재만으로 발생하지 않았다고 입증할 수 없다. 사용자 관찰과 합쳐 이번 시도 미재현으로 분류한다.
- 전체 로그: `.debug/device-20261024/songpa-start-logcat.txt`, 앱 로그: `songpa-end-app-logcat.txt`, 선택 태그 실시간 로그: `songpa-live-logcat.txt`. 수집용 adb logcat 프로세스만 종료했고 앱은 종료하지 않았다.
- 제품 수정/테스트 APK 설치/캐시 초기화/에이전트에 의한 사진 변경 없음. 기존 수원 제보는 해결 처리하지 않고 미재현 상태 유지.

## 성남 MIXED 정리 버튼 누락 / 수원 사용자 정정

- 사용자 최종 정정: 수원은 정상, 성남이 이상함. 수원 복사 실패로 분류하지 않는다.
- 화면 dump `seongnam-window.xml`: 발견 기록 성남 사진2,259/영상30, 수원 사진950/영상24 표시.
- 읽기 전용 MediaStore 실조회: 성남 위치 앨범 Pictures/성남에서/ 사진373/영상19; 수원 Pictures/수원에서/ 사진950/영상12. 화면은 발견+기존앨범을 해석한 범위이며 물리 폴더 개수와 같다고 가정할 수 없다. 어느 성남 파일이 실제 누락됐는지는 미확정.
- 확인한 기능 공백: MemoryRepository.merge는 MIXED에 canOrganize=false, canAddNewItems=true(조건 충족 시)를 설정한다. MemoryBrowserDetail.from은 record.canOrganize로 정리 허용을 결정하고 MainActivity 상세 헤더도 detail.canOrganize만 사용한다. canAddNewItems는 모델 전달 외 실행 UI/명령 경로에 연결돼 있지 않다. 기존 MemoryRepositoryTest도 MIXED canOrganize=false를 기대한다.
- 결과: 기존 위치 앨범이 있는 성남은 발견 미디어가 더 남아 있어도 상세의 앨범 만들기 버튼이 사라질 수 있다. 모든 사진이 이미 복사됐음을 뜻하지 않는다.
- 후속 수정은 MIXED의 추가 후보 확인/대상 exact path 검증/중복 건너뛰기 정책을 별도 클래스로 설계하고 기존 신규 앨범 생성과 분리한다. 무조건 canOrganize=true로 바꾸거나 원본 휴지통을 활성화하지 않는다. 검색/분석 캐시는 유지한다.
- 제품 코드/미디어/캐시 변경 없음. 성남 경로의 파일별 부족분과 실제 정리 결과는 추가 대조 필요.
