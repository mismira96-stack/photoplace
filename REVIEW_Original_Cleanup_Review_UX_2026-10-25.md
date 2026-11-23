# 원본 정리 검토 화면·집계·개별 보류 수정 — 1.3.14(46)

- 사용자 지시: 설명으로 작업을 멈추지 않고 실제 수정·검증·설치까지 진행.
- 인수 대상: code45 실제 결과(새 후보3513쌍/전체3778쌍/모두 내용일치/전체 연결보류).
- 상태: **검토 전용46 설치 완료**. 실제 휴지통 실행 잠금 유지. 실행 안전성 차단 과제를 모두 해결한 것은 아니다.

## P0 / P1

- MainActivity scoped diff: **+1/-1**, 홈 버튼 addView 제거 및 위치 앨범 헤더의 CleanupReviewEntry.addAction 위임만 변경. 후보 계산·필터·검증·진행 로직은 전용 클래스에 있다.
- `.debug/cleanup-review-2026-10-25/MainActivity.code46.patch`로 이전 code45 정규화 텍스트 대비 범위를 확인했다.
- CleanupExecutionPolicy=false, UI 이동 버튼 disabled 및 최종 Dispatcher 거부 유지. 개인 사진의 복사·이동·휴지통 요청, 메모/alias/모음 재작성 없음.
- 기존 검색·사진 분석·복사 엔진은 이번 수정에 포함하지 않았다. 원장 전체를 지워 숫자를 맞추지 않았다.

## 1. 집계 경계

- Coordinator는 동일 source URI/target URI 쌍을 작업 ID와 무관하게 중복 제거한다. 같은 쌍의 관찰이 여러 번이면 최신 관찰을 사용하며 현재 전체 내용은 다시 검사한다. 원장의 다른 증거 행은 보존한다.
- Assessment는 화면의 원본 수와 용량을 **고유 source URI 기준**으로 계산한다. 한 원본의 보존 복사본이 여럿이어도 원본·용량을 한 번만 센다.
- 모든 검증 쌍은 evidenceCandidates에 보존하고, 화면의 대표 행만 합친다. 표시 대표 행을 향후 실제 실행의 전체 보호 근거로 사용하면 안 된다. 실행 전에는 모든 필요한 보존 대상을 다시 결합해야 한다.
- 식별은 현재 raw MediaStore URI 기준이다. 서로 다른 URI alias가 같은 실제 파일을 가리키는 경우의 canonical volume/id 신원 통합은 실제 실행 전 별도 검토 항목이다.
- 내용 검사 진행률의 단위는 비교 작업 건수이고 결과의 단위는 원본 사진 장수다. 여러 보존 대상이 있으면 두 숫자는 다를 수 있지만, 기존 작업 기록 때문에 같은 물리 URI 쌍을 재검사하는 중복은 제거했다.

## 2. 전역 보류와 개별 사유 분리

- MemoryDependencyGuard.inspect가 전역 상태와 명시적으로 확인된 blockedSources를 별개로 반환한다. 기존 check는 이 보고서의 전역 상태를 반환하므로 전체 집합 안전 정책을 유지한다.
- 사진별 순환·직접 URI 참조·알려진 alias/link/output 미확인·날짜/읽기 경로 문제만 해당 source에 연결한다. 전역 메모 변경/범위 미확인은 모든 사진의 개별 참조 문제로 꾸미지 않는다.
- SnapshotReader는 alias/link/output 검증이 부족한 **실제 영향 source**를 unverifiedSources로 기록한다. 관련 그룹 밖의 사진까지 동일 사유로 세지 않는다.
- 명시된 보류 원본을 뺀 뒤 **남은 전체 집합**의 before/after projection을 다시 읽어 검사한다. 개별 PASS를 단순히 합치지 않는다. 최대8번의 보수적 패스 안에 완료되지 않거나 알려지지 않은 범위가 있으면 전역 보류를 유지한다.
- 검사 종료 시 Reader의 파일 내용/MediaStore revision token을 새로 확인한다. 바뀌면 모든 통과 자격을 취소하고 SNAPSHOT_CHANGED로 보류한다. 취소·projection 오류도 통과로 바꾸지 않는다.
- 어떤 연결인지 ‘모음 기록 / 날짜별 메모 / 사용자 대표사진 / 앨범 정리 이력’ 출처를 해당 URI에만 붙인다. 과거 이력의 참조를 현재 실제 대표사진과 동일하다고 확정한 것은 아니며, 여전히 보수적으로 보류한다. 자동으로 그 참조를 고치거나 지우지 않는다.

## 3. 화면과 진입점

- 홈의 큰 원본 휴지통 버튼 배치를 제거했다. **위치 앨범 상단 ‘원본 정리’**를 상시 진입점으로 추가했다. 앨범 생성 완료 후 보조 검토 진입은 유지한다.
- ProgressDialog 대신 전체 높이의 CleanupProgressScreen: 사진 찾기 → 내용 확인 → 기록 연결 확인. 총량이 불명인 단계에는 진행률을 꾸미지 않고, 내용 검사 때 실제 진행 건수를 표시한다. 중지 버튼 및 Activity 종료 신호로 취소한다.
- 한 번에 하나의 검토만 실행한다. 수명이 끝난 Activity에 결과 창을 띄우지 않도록 확인하고, 백그라운드 검토의 Store는 완료 시 닫는다.
- 결과도 전체 화면으로 바꾸고 원본 수·내용 일치 수·기록 연결 확인 수·확인 필요 수를 보여준다. 개별 사진의 보류 출처와 ‘확인 필요한 사진만 보기’ 필터를 추가했다. 전역 미완료는 별도로 설명한다.
- 전체 선택/체크박스는 실행 잠금 중 숨기며, 휴지통 이동은 꺼져 있다. 결과 목록은 보류 항목을 먼저 보여준다.
- 썸네일은 UI 스레드에서 원본 전체를 디코딩하지 않고 160px thumbnail을2개 스레드/유계24개 대기열로 읽는다. 화면 종료 시 닫고 재사용된 row에는 이전 이미지가 붙지 않게 한다.
- 사용자 보류 요청도 함께 반영: `사진·동영상 N개 (이전 기록)` → **사진·동영상 합계 N개**. 사진/영상 종류를 추정하지 않았다.

## 검증

- 최종 `testDebugUnitTest testReleaseUnitTest bundleRelease`: **Debug396/Release396, 실패·오류·skip0**, 빌드 성공. 별도의 assembleDebug/assembleRelease 및 lintVitalRelease도 통과.
- 추가 회귀11개: 같은 쌍의 작업 간 중복 제거/한 원본의 다중 복사본 용량,100장 중1개 직접 참조 시99개 전체 재검증, 명시적 미검증 원본 분리, 범위 불명 시 전역 보류, revision 변경 시 자격 취소, 보류를 뺀 나머지도 실패하면 미통과, 메모 변경 전역 사유, 정확한 URI별 연결 출처, 순환과 무관 원본 구분, 알려지지 않은 범위 유지.
- 기존 날짜·모음·순환·캐시 재사용 방지·실행 잠금 회귀를 유지했다. 진단 문구 테스트는 사진 장수 및 실제 개별 사유를 검사하도록 갱신했다.
- 합성 데이터 전용 helper package를 **별도 headless emulator-5582**에 설치해 실제 Android UI 렌더링 확인. 사용자 폰 UI는 조작하지 않았다.
- 진행/결과 캡처를 눈으로 확인한 뒤 결과 화면의 내부 AlertDialog 높이 문제를 수정했다. 최종 결과는 목록이 공간을 쓰고 닫기/잠금 버튼이 하단에 유지된다.
- synthetic fixture3장(2통과·1보류)에서 ‘확인 필요만 보기’ 클릭 후 row1개만 표시, 이동 버튼 enabled=false를 XML로 확인했다. 썸네일은 테스트 패키지에 미디어 권한이 없어 placeholder이며 실제 사용자 사진을 읽은 시각 검증이 아니다.
- 테스트 helper 제거 및 직접 시작한 read-only headless emulator 종료 완료.
- 로그/캡처: `.debug/cleanup-review-2026-10-25/test46-final.log`, `test46-full.log`, `ui46-progress-final.png`, `ui46-result-final.png`, `ui46-filter.xml`.

## 설치 기록

- 1.3.14 / versionCode46 / target36 / 앱 이름 ‘앨범 정리’.
- internal draft만 등록. 프로덕션 배포·검토 제출 없음.
- AAB `photoplace-1.3.14-code46-test.aab`: SHA256 `44DC07EB38B2EE96A759006909B85D2C469CDC57402F4BFADD116E630C3E9149`.
- 설치 APK `photoplace-1.3.14-code46-play-signed.apk`: SHA256 `3E12F1C629B41E196FC11EBC14B95FC609E65E051449F1B1CAF9E38C742931C8`.
- Google Play 앱 서명 SHA256 `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d` 확인 후 SM_F971N에 `adb install -r` 성공.
- 폰 versionCode46/versionName1.3.14, 최초 설치2026-10-20 00:24:37 및 사진/영상 권한 유지. 삭제/초기화 없음.
- 실제 다운로드 APK의 DEX에서 새 진행 화면/진입점/개별 사유/실행 정책 심볼을 확인했다.

## 남은 확인과 실제 실행 게이트

- 사용자의 실제3513개 후보를 새 빌드로 다시 검사한 수치·보류 사진 분리 결과는 아직 대기다. 합성100장 테스트 결과를 개인 라이브러리 결과로 보고하지 않는다.
- 이 진행 화면은 Activity 수명을 고려하는 전경 UI이며 WorkManager를 통한 프로세스 종료 후 작업 재개/배경 완료 알림을 구현한 것은 아니다.
- 사후 감사 expected hash/size 대조·durable 요청 복구·선택 직전 검증·출처/미디어 신원 등 인수 검토의 미해결 과제는 유지한다. Review의 대표 행/검토 통과 수만으로 실제 휴지통 실행을 켜면 안 된다.
- 후속 리뷰 요청: REVIEW_REQUEST_Original_Cleanup_Review_UX_2026-10-25.md.
