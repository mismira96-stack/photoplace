# 원본 정리 인수 검토 — code44 준비본과 폰 code43

- 날짜: 2026-10-25 (Asia/Seoul)
- 요청: 안티의 마지막 작업을 인수하고, 홈 버튼 실행 시 0장인 상태의 코드와 다음 수정 지점을 파악한다.
- 결론: **P0 UI 격리 통과 / 실제 휴지통 실행 경로는 HOLD**. 0장 진단과 데이터 보호 경로를 함께 수정해야 한다. 제품 코드를 이번 인수 검토에서 변경하지 않았다.

## 1. 설치 상태와 기존 보고서의 범위

- 폰 SM_F971N / R5KL503VHQR: **1.3.12, versionCode43**, 최초 설치 시각 2026-10-20 00:24:37.
- 로컬 app/build.gradle: **versionCode44, 1.3.12**. `photoplace-1.3.12-code44-play-signed.apk` 2,833,595bytes 존재.
- 사용자는 잠들기 전 code44를 설치하지 못했다고 확인했다. 따라서 폰에서 본 기존 0장 결과는 code44 수정 결과를 검증한 것이 아니다. **code44가 실제 폰의 0장을 해결했는지도 아직 미확인**이다.
- 안티의 마지막 보고서는 REVIEW_Original_Cleanup_Phase3_Followup_2026-10-25.md. 그 설명·승인을 이번 인수 검토의 검증 결과와 혼동하지 않는다.
- 안티가 보고한 이름+크기 일치 3,715건은 후보 탐색 근거다. 해당 수치 자체가 전체 바이트 동일·의존성 보존이 검증된 정리 가능 사진 수를 뜻하지 않는다.

## 2. P0 Gatekeeper 선행 확인

- code41 당시 MainActivity를 `.debug/album-flow/MainActivity.before.java` 및 당시 scoped patch로 재구성해 현재와 대조했다.
- 이후 추가는 원본 버튼의 UI 배치/표시, 콜백의 Activity 전달 및 cleanup 위임이다. 후보 필터/해시/의존성 판단을 MainActivity에 추가한 것으로는 확인되지 않았다.
- 정확한 범위: `.debug/cleanup-review-2026-10-25/MainActivity.since-code41.patch`.
- 이 판정은 누적 Git HEAD diff 전체에 대한 새 승인이나 다른 모듈 안전성 승인을 의미하지 않는다.

## 3. 확인된 차단 과제

### B1 — 사후 감사가 계산한 해시를 비교하지 않음 (Blocker)

- 위치: `CleanupPostTrashAuditor.java:60,80,97`, `CleanupEvidenceStore.java:54,59`.
- 사후 감사는 대상 스트림을 읽고 해시가 비어 있지 않은지만 본다. 요청 journal에는 source/target URI 목록만 저장되며 실행 전 전체 해시/기준 바이트 수가 없다.
- 따라서 대상 파일이 읽을 수 있는 다른 내용으로 바뀌어도 `AUDIT_PASSED`가 될 수 있다. 주석의 ‘기존 검사값과 비교’는 실제 구현되지 않았다. 원본의 실제 휴지통 상태도 대조하지 않는다.
- 또한 source/target 수를 Math.min으로 줄여 검사하므로 target 목록이 빈 잘못된 journal도 0건 검사 후 성공할 수 있다.
- 재현: 별도 테스트에서 예상 해시 없는 임의 바이트를 제공한 경우 및 source1/target0 journal 모두 잘못된 성공을 확인했다.
- 수정 방향: 실행 직전 검증한 쌍·바이트 수·전체 해시·미디어 식별 근거를 durable journal에 저장한다. 항목 수·ID·중복·빈 요청을 검증하고, 대상의 실제 전체 스트림 길이/해시 및 원본 상태를 기준과 비교한다. 증거 없음/접근 불명/부분 결과는 성공으로 처리하지 않는다.

### B2 — 오래된 MATCH 캐시만으로 현재 접근 불가능한 사진도 후보가 됨 (Blocker)

- 위치: `CleanupReviewCoordinator.java:63-69`, `CleanupTrashDispatcher.java:36-67`, `CleanupReviewCoordinatorTest.java:212`.
- 저장된 MATCH와 비어 있지 않은 해시가 있으면 현재 metadata/스트림을 다시 확인하지 않는다. 캐시의 사진 identity/generation/검사 시각 유효성을 검사하지 않는다.
- Dispatcher도 선택 시점의 메모리 후보를 그대로 사용하며 실행 직전 전체 내용·현재 선택 집합·후속 상태를 재검증하지 않는다.
- 재현: 원본·복사본 모두 현재 읽을 수 없게 만든 FakeAccess에 기존 MATCH를 넣으면 정리 가능 후보1개가 나온다. 기존 테스트는 스트림 openCalls0을 성공 조건으로 삼고 있어 이 누락을 잡지 못한다.
- 수정 방향: 캐시는 검토 성능 참고용으로만 사용한다. 현재 미디어 identity와 metadata를 확인하고 실제 실행 전에는 선택 집합을 대상으로 강한 재검증을 수행해야 한다. 실패한 검사/스냅샷 변경은 과거 MATCH로 대체하지 않는다.

### B3 — 실행 게이트·요청/감사 잠금·재시작 복구가 설계보다 부족함 (Blocker)

- 위치: `CleanupFeatureGate.java:11`, `CleanupTrashDispatcher.java:36-67`, `CleanupController.java:34,129,143`.
- 실행 게이트가 기본 true로 바뀌어 있다. 최종 Dispatcher에는 게이트/검증 상태 검사가 없다.
- pending requestId는 컨트롤러 메모리 필드에만 연결된다. 프로세스 재시작 시 durable 요청을 먼저 복구·대조하는 경로가 확인되지 않았다. callback 직후 AUDIT_PENDING 저장/새 배치 잠금도 구현이 부족하다.
- 선택한 후보가100개를 넘으면 첫100개만 사용하고 잔여 실행/사용자 안내가 없다. 사용자 선택 원본 URI 중복 제거도 최종 경계에서 확인되지 않는다.
- 수정 방향: 검토와 실행 가능 여부를 분리하고, 최종 실행 경계에서 상태·기능 게이트·선택 집합 검증을 강제한다. 미해결 journal을 우선 처리하며 감사 통과 전 후속 요청을 막는다. 실제 미디어는 별도 승인된 폐기용 대상에서 검증해야 한다.

## 4. 0장 경로에서 확인한 진단 문제

### D1 — 여러 이유가 같은 ‘원본 없음’으로 축약됨

- `CleanupHistoricalScanner.scanAndRegister()`는 예외를0으로 바꾼다. 내부 MediaStore/과거 저장소 조회에도 오류를 삼키는 경로가 있다.
- `CleanupReviewCoordinator.java:84-88`은 projection 실패를 UNVERIFIED_IDENTITY로 축약한다.
- `CleanupController.java:88-91`은 최종 eligible이 비면 후보0개인지, 내용 불일치인지, 읽기 불가인지, 의존성 미검증인지 구분하지 않고 같은 토스트를 낸다.
- 다음 수정의 첫 단계는 **대상 앨범 수 → 탐색 후보 → 내용 일치/탈락 사유 → 의존성 사유 → 최종 선택 가능 수**를 보여주는 읽기 전용 진단이다. 숫자를 늘리기 위해 안전 가드를 일괄 우회하지 않는다.

### D2 — nullable 촬영일도 모두 ‘읽기 불가’로 처리

- 위치: `CleanupMediaAccess.java:29`.
- 요청한 모든 컬럼이 non-null이어야 한다. datetaken이 비어 있어도 즉시 예외가 나고 verifier는 UNREADABLE을 반환한다.
- 실제 폰을 파일 변경 없이 metadata만 조회했다. `Download/% OR DCIM/Camera/`, pending/trash 제외 시 **7,229행 중 datetaken NULL 1,419행**. 그 외 조사한 size/volume/date_modified/generation/path/mime 컬럼의 NULL은0행이었다.
- 이 수치는 조사 조건의 행 수이지 중복/해시 일치 후보 수가 아니다. **NULL 촬영일이 전체0장을 설명한다고 단정하지 않는다.** 다만 촬영일 부재와 실제 파일 읽기 실패를 구분해야 하는 확인된 경로다.
- 다음 수정은 필수 신원 정보와 선택적인 촬영일을 구분하고 기존 UI 날짜 해석과 일치하도록 처리해야 한다. 날짜를 임의로 생성해 가드를 통과시키면 안 된다.

### D3 — 외부 원본의 ‘의존성 없음’과 ‘범위 미확인’을 구분해야 함

- `MemoryDependencyGuard.java:59-63`의 null 참조 허용은, 알려진 의존성을 빠짐없이 조사했다는 전제라면 일반 외부 원본을 지원하는 데 필요할 수 있다.
- 현재 complete 플래그가 어떤 참조 범위의 완전성을 보장하는지 별도 검증이 필요하다. 특히 SnapshotReader는 실제 참조가 있는 그룹에서 alias/link가 없으면 complete=false로 전체 선택을 보류한다.
- 한 항목의 미검증이 전체 선택의 eligible을0으로 만들 수 있으므로 이 상태를 진단해야 한다. 이를 무조건 PRESERVED로 바꾸는 것은 해결책이 아니다.
- SnapshotReader.token은 계산되지만 Coordinator가 시작/종료 token을 대조하지 않는다. 스냅샷 변경을 실제로 차단하는 연결이 필요하다.

### D4 — 과거 대조의 대상 범위가 이름 패턴으로 확장됨

- `CleanupHistoricalScanner.java:151-170,298-307`은 앱의 정확한 이력 연결 외에 `Pictures/...에서/...` 이름 패턴도 PhotoPlace 대상 앨범으로 인정한다.
- 이는 승인된 V2의 ‘이름/경로 패턴만으로 앱 출력과 Memory 연결을 입증하지 않음’보다 범위가 넓다.
- exact history/link 기반 대상과 일반 패턴 추정 대상을 구분해야 한다. 역사적 쌍은 원래 복사 증거와 같은 출처로 취급하지 않는다. 이름+크기는 검색 조건일 뿐 해시/연결 증명이 아니다.

## 5. 실제 실행한 검증

1. 기본 `testDebugUnitTest testReleaseUnitTest`: **Debug382 / Release382, failures/errors/skipped0**. 로그와 요약을 재현 테스트 전에 따로 저장했다.
2. production 수정 없이 `.debug/.../reprosrc`에 세 가지 부정 테스트를 추가하고 별도 init script로만 실행: **3개 모두 실패**. 이는 테스트 환경 오류가 아니라 위 B1/B2의 잘못된 통과를 재현한 AssertionError이다.
3. P0 scoped MainActivity diff 생성. 실제 폰의 package 버전 및 source metadata 읽기 확인.
4. UI 버튼 클릭, 새 APK 설치, 사진 복사/이동/휴지통 요청, 앱 데이터 초기화, 배포는 수행하지 않았다.

재현 명령 (의도한 실패, 기본 CI에는 포함되지 않음):

```powershell
.\gradlew.bat -I .debug/cleanup-review-2026-10-25/repro.gradle testDebugUnitTest --tests com.example.gallerysorter.CleanupSafetyReviewReproducerTest --console=plain
```

증거:

- `.debug/cleanup-review-2026-10-25/baseline-tests.log`, `baseline-summary.json`
- `negative-repro.log`, `negative-repro-results.xml`, `reprosrc/.../CleanupSafetyReviewReproducerTest.java`
- `source-metadata.txt` (개인 metadata, 로컬 진단용), `MainActivity.since-code41.patch`

## 6. 이어갈 순서

1. 기존 정상 앨범/검색 기능을 유지하며 검토·실행 게이트를 분리한다.
2. 0개 토스트 대신 검증 단계별 읽기 전용 사유와 건수를 노출한다. 실제 폰의 미검증/NULL 날짜 등 탈락 원인을 여기서 좁힌다.
3. 오래된 검증 캐시와 최종 선택 재검증 경계를 수정한다. 스냅샷 변경/선택 부분집합/순환/다중 타깃을 재검증한다.
4. 사후 감사의 expected hash/size journal 및 요청 상태 복구를 완성한다. 이번 재현3개를 정상 회귀 테스트로 편입해 통과시킨다.
5. 승인된 폐기용 사진으로 OS 확인·취소·프로세스 종료·외부 복사본 변경·감사·복구 안내를 검증한 뒤 사용자 원본 실행으로 진행한다.

현재 단계는 인수 검토·수정 준비 완료다. code44 설치 완료나 휴지통 기능 안전성 승인으로 보고하지 않는다.
