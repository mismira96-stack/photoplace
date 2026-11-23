# 출시 전 추가 검증 — Codex

- 날짜: 2026-10-19 (Asia/Seoul)
- 대상: HEAD `1c8d683` 대비 현재 working tree의 증분 분석 및 홈 캐시 변경.
- 성격: Antigravity 리뷰 후 추가 검증. 전체 코드에 대한 독립적인 전수 리뷰를 완료했다는 의미는 아님.
- 원문: [Antigravity 종합 리뷰](REVIEW_Incremental_Analysis_Home_Cache_2026-10-19.md). 원문은 수정하지 않음.
- 판정: 자동 검사 통과. 새 GPS 사진의 MIXED 장소 NEW 검증은 Pre-release 상태 유지. 이번에 발견하여 확정한 Blocker는 없음.

## P0 구조 점검

MainActivity 변경은 +71/-362, 순감 291줄. 현재 diff에서 스캔 위임, 컨트롤러 연결, 렌더링 콜백 및 생명주기 연결을 확인했다. 홈 정책은 HomeDashboardPolicy/Controller에 위치하며, 스캔은 IncrementalMediaScanner에 위임한다. 파일 크기 감소 자체가 P0 준수의 근거는 아니며 실제 추가 코드의 책임으로 판단한다.

## 실행한 검사

| 검사 | 결과 |
|---|---|
| `gradlew.bat testDebugUnitTest --rerun` | PASS, 247 tests / 0 failures / 0 errors. 캐시 결과만 사용하지 않고 테스트 작업 재실행 |
| `gradlew.bat testDebugUnitTest assembleDebug` | BUILD SUCCESSFUL. 디버그 빌드 확인이며 릴리스 빌드 검증은 아님 |
| `git diff --check` | PASS. LF/CRLF 정규화 경고만 출력 |
| MainActivity diff 및 홈 캐시 정책/저장/요청 조정 코드 | 아래 설명 정정 항목과 함께 확인 |

## 실기기에서 추가 확인한 범위

기기 `R5KL503VHQR`의 설치된 PhotoPlace를 실행하여 확인했다. 이번 점검에서는 APK 재설치나 앱 데이터 초기화를 수행하지 않았다.

- 발견 기록에서 성남 카드의 NEW 표시가 현재 보임. 이것만으로 새 GPS 사진 추가 경로를 검증했다고 판정하지 않음.
- 발견 기록의 하남은 사진 6장으로 표시되며, 날짜별 목록(10월 18일 / 9월 19일)이 열림.
- 10월 18일 썸네일을 눌러 인앱 Memory 뷰어의 `1/2` 및 `Memory 사진` 표시 확인.
- 위치 앨범 탭의 별도 `하남에서` 레코드는 105개로 표시됨. 해당 화면의 썸네일은 Android 연결 앱 선택창으로 연결됨. 앱 선택 없이 취소함. 이 경로를 발견 기록 인앱 뷰어와 혼동하여 검증 완료로 기록하지 않음.
- 마지막에 PhotoPlace 홈으로 복귀. 이번 실행에서는 유효한 PhotoPlaceHome 계측 로그를 얻지 못했으므로 신규 성능 수치는 제시하지 않음.
- 사진 이동/복사/삭제, 앨범 생성, 메모 편집, 장소찾기 재분석은 이번 실기기 점검에서 실행하지 않음.

## Pre-release

### PR-1: 새 GPS 사진을 통한 MIXED 장소 NEW 및 반복 보고 검증

- 관련: `app/src/test/java/com/example/gallerysorter/MemoryBrowserStateTest.java:17`, `DiscoverySnapshotControllerTest.java`.
- 조건: 기존 위치 앨범과 발견 기록이 함께 있는 성남에 위치 정보가 포함된 새 사진이 추가됨.
- 남은 절차: 실제 새 사진 촬영/추가 → 장소찾기 실행 → 해당 장소 NEW 및 추가 사진 수 확인 → 동일 소스로 다시 실행하여 추가 장소 보고 0 확인 → 앱 재실행 후 상태 확인.
- 영향: 자동 테스트는 통과했으나 실제 EXIF/MediaStore/화면 결합 경로 검증은 남음.
- 제안: 내일 사용자의 실제 촬영으로 확인. GPS를 합성하거나 기존 사진을 몰래 복사하여 검증하지 않음.

## Follow-up: 외부 리뷰 설명의 정확성 보완

### FU-1: 요청 번호 기반 무효화로 설명된 부분 정정

- 관련: `app/src/main/java/com/example/gallerysorter/HomeRefreshRequests.java:4`, `HomeDashboardController.java:120`, `HomeDashboardController.java:143`, `MainActivity.java:176`.
- 원문에는 HomeRefreshRequests의 requestId 시퀀스가 오래된 결과를 차단한다고 기재되어 있으나 실제 구현에는 requestId가 없음.
- 실제 구현은 단일 워커, running/pending 병합, 현재 target의 attach 상태 및 closed 상태 확인임. 진행 중 요청이 있으면 후속 갱신을 예약함.
- 영향/조건: 빠른 홈 재진입 또는 외부 변경 중에는 이전 캡처 결과가 후속 갱신 전에 잠깐 표시될 가능성까지 배제하는 구조는 아님. 현재 확인으로 데이터 훼손 또는 영구 누락을 입증한 것은 아님.
- 제안: 리뷰의 보장 문구를 구현에 맞게 해석하고, 겹치는 갱신 도중 최신 결과만 렌더해야 한다면 별도의 요청 세대 검증을 후속 검토.

### FU-2: 미디어 변경 감지 보장 범위

- 관련: `app/src/main/java/com/example/gallerysorter/IncrementalMediaScanner.java:90`.
- 파일 내용의 모든 바이트를 해시하지 않으며, signature 및 MediaStore version/size/duration/generation/좌표 등 조회된 필드를 비교함.
- 조건/영향: 해당 필드가 그대로인 원시 바이트 변경까지 감지한다고 보장할 수 없음.
- 제안: 원문의 “1바이트라도 변경되면 재분석”은 “지문에 포함된 필드 변경 시 재분석”으로 이해할 것. 전수 파일 해싱은 이번에 추가하지 않음.

### FU-3: 원자적 저장과 장애 보장 구분

- 관련: `app/src/main/java/com/example/gallerysorter/HomeDashboardStore.java:26`.
- 임시 파일과 ATOMIC_MOVE를 사용하며 실패 시 false, 읽기 실패 시 null을 반환함. 모든 저장 장애나 전원 차단에 대한 영구 보존 보장을 뜻하지 않음.
- 조건/영향: 저장 실패 시 최신 캐시가 디스크에 반영되지 않을 수 있음. 이 파일은 재구성 가능한 요약 캐시임.
- 제안: 리뷰를 “부분 쓰기 노출을 줄이고 실패 시 캐시 미사용/재구성을 허용”하는 범위로 해석.

## 남은 출시 절차

새 GPS 사진 실기기 검증 완료 후 릴리스 절차 수행. 이번 작업에서는 버전 변경, 릴리스 산출물 생성, Git 커밋/푸시 및 스토어 배포를 수행하지 않았다.

## 추가 야간 점검 — 폰 없이 완료한 범위

사용자의 후속 요청으로 Antigravity 원문과 스캔/캐시/스냅샷 코드를 추가 대조했다. PreviewRefreshGate의 실패/취소/동시 변경 방어, IncrementalAnalysisSession의 정책 검사 및 저장 경계, DiscoverySnapshotController의 URI 비교, MemoryMediaResolver의 exact link 우선권도 읽었다. 이번 추가 점검에서 제품 Java 코드 수정은 하지 않았다.

### 회귀 테스트 추가 및 결과

- `HomeDashboardCacheTest.failedTemporaryWritePreservesLastValidSummary`: 임시 파일 경로가 디렉터리여서 쓰기가 실패할 때 기존 정상 캐시의 바이트와 revision이 유지되는지.
- `HomeDashboardCacheTest.interruptedTemporaryWriteIsIgnoredAndNextSaveRecovers`: 부분 JSON인 임시 파일을 읽지 않고 마지막 확정 캐시를 사용하며 다음 저장으로 복구되는지.
- `HomeDashboardCacheTest.invalidCountryCountsDiscardWholeCacheAndAllowRecovery`: 음수 국가 앨범 수량으로 손상된 캐시를 폐기하고 다시 정상 저장할 수 있는지.
- `IncrementalAnalysisSessionTest.obsoletePolicyCannotReuseFingerprintOrLegacyAnalysis`: 현재 정책과 다른 캐시를 기존 지문/레거시 경로 모두에서 재사용하지 않고 재분석하는지.
- 실행: `gradlew.bat testDebugUnitTest` **PASS, 251 tests / 0 failures / 0 errors**. 테스트 소스 수정 후 컴파일 및 테스트 작업이 실제 실행됨.
- `git diff --check` PASS. 제품 코드 변경이 없으므로 앞서 성공한 debug 빌드 이후 제품 산출물 내용 변경은 없음. 이번 점검은 release 빌드/서명 검증이 아님.

### Follow-up FU-4: 프로젝트 기존 lint 실패

- 실행: `gradlew.bat lintDebug` **FAIL — 179 errors / 59 warnings**. 자동 검사가 모두 통과했다고 기록하지 않는다.
- 오류 종류: 179건 모두 `WrongConstant`, 위치는 모두 `app/src/main/java/com/example/gallerysorter/MainActivity.java`.
- 대표 위치: `MainActivity.java:547`, `root.setOrientation(1)`; 권장 상수는 `LinearLayout.VERTICAL`.
- 근거: lint XML의 오류별 `errorLine1`을 각 파일의 `git show HEAD:<path>` 내용과 대조. **179/179 오류의 해당 소스 문장이 HEAD에도 존재**, 미일치 0건. 새 홈/증분 분석 클래스에 보고된 lint 이슈는 없음. HEAD를 별도 빌드하여 lint 전체 개수까지 비교한 것은 아님.
- 영향: 현재 저장소는 lint를 통과하지 못한다. 이번 성능 변경이 추가한 오류라는 증거는 없지만, lint 통과를 배포 필수 조건으로 쓰면 별도 정리가 필요하다. 컴파일/단위 테스트/debug APK 생성 성공과 lint 실패는 구분해야 한다.
- 수정 제안: 별도 정리 작업에서 기존 숫자 상수를 대응하는 Android 상수로 치환하고 lint 재실행. 이번 패치에서 179개 기존 UI 구문을 대규모 수정하거나 lint baseline/무시 규칙을 추가하지 않음.
- 보고서: `app/build/reports/lint-results-debug.html`, `app/build/reports/lint-results-debug.xml`. 빌드 디렉터리 산출물이므로 영구 근거 요약은 본 문서에 보존.

### 원문 리뷰 추가 정정

- `DiscoverySnapshotController`는 실제로 `hasNewPhotoRef()`에서 기존 URI 집합의 `contains()`를 사용한다. 원문에 적힌 `containsAll`, `updatedPlaceCount`는 현재 구현의 정확한 메서드/변수명이 아니다. 동일 URI 재분석을 0으로 집계하는 의미와 회귀 테스트는 확인했다.
- 원문에서 평균 26ms 등으로 일반화한 값은 이전 실기기 warm Back pre-draw 측정 범위에 가깝다. 모든 cold start, 전체 Activity launch, 모든 폰에 대한 보장이 아니다.

### 최종 인계

- 폰 없이 가능한 이번 리뷰 근거 대조, 추가 실패 경로 테스트 및 lint 확인 완료.
- 남은 Pre-release: 실제 새 GPS 사진의 성남 MIXED NEW + 동일 소스 재실행 0 보고. 폰 없이 대체 검증 완료로 처리하지 않음.
- 기존 lint 부채와 기존 홈/상세 수량·위치 앨범 화면 경로 차이는 출시 판단에서 별도로 인지할 것.
- 테스트 4개 이외 제품 코드는 이번 야간 점검에서 바꾸지 않았다. 사용자 미디어/앱 데이터/버전/배포 상태 변경 없음.
