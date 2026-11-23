# 원본 정리 Phase 0 — legacy 실행 차단

- 날짜: 2026-10-24 (Asia/Seoul).
- 사용자 진행 지시: 승인된 V2 설계에 따라0단계 시작.
- 범위: 기본 차단 게이트, 기존 휴지통 실행/후보 재수집/성공 후 상태 삭제 연결 제거, 실행 UI 제거. 원장/해시/새 원본 정리 화면/휴지통 활성화는 구현하지 않음.
- 제품 코드의 로컬 검증만 수행. 폰 조작·설치/사진 변경/Play 업로드·출시/버전 증가/커밋·푸시 없음.

## 변경 전 실패 재현

- LegacyCleanupIsolationTest의 Activity 컴파일 결과 검사2개를 먼저 추가해 실행.
- `testDebugUnitTest --tests '*LegacyCleanupIsolationTest'`: **2 tests / 2 failures**.
- 기존 Activity에 createTrashRequest 및 직접 is_trashed 변경 심볼이 남아 있음을 탐지했다. 시스템 확인창이나 실제 사용자 사진으로 테스트하지 않았다.
- 로그 `.debug/cleanup-phase0/before-tests.log`.

## 구현 내용

### CleanupFeatureGate.java

- isEnabled()=false. 사용자 설정/권한/후보 수/완료 플래그로 켤 수 없음.
- hasActionablePending은 현재 어떤 후보 수에서도 실행 가능 여부를 false로 반환한다. 후보 수 자체는 변경하지 않는다.
- onLegacyRequest는 사용 불가 안내만 전달한다. 파일/MediaStore/Activity/원장 접근 권한이나 실행 callback을 받지 않는다.
- onLegacyResult는 옛 성공/취소/알 수 없는 응답을 성공 증거로 해석하지 않고 같은 안내만 전달한다. gate가 나중에 열리더라도 retired legacy 요청을 다시 실행하는 코드가 없다.

### MainActivity.java

- :792 기존 숨겨진 원본 버튼을 명시적으로 GONE 처리. 다른 코드가 enabled 값을 바꾸더라도 이 버튼은 표시되지 않으며, 강제로 click 콜백을 호출해도 아래 차단 delegate만 실행한다.
- :3522 deleteCopiedOriginals는 CleanupFeatureGate.onLegacyRequest(this::showToast) 위임만 남김.
- :4287 hasPendingOriginalCleanup은 게이트 정책에 위임하여 legacy 원본 정리 UI/버튼 활성화를 막음.
- :4489 부근 REQUEST_DELETE_ORIGINALS 결과는 게이트에 위임 후 return. RESULT_OK여도 후보/preview/recent keys를 비우거나 ‘원본 이동 완료’로 처리하지 않는다.
- requestTrashCopiedOriginals / showOriginalTrashConfirmDialog 및 휴지통 요청 sink 제거.
- reinforceTrashState의 직접 is_trashed update sink 및 사용하지 않는 옛 원본 prune/apply helper 제거. 현재 read-only is_trashed 조건은 유지.
- collectCleanupOriginalUrisFromPreview와 기존 m7... 클릭 처리 제거. 파일명 중복 추정치를 원본 후보로 추가/저장한 뒤 휴지통 실행하는 연결 없음.
- legacy cleanup compact card/원본 정리 전용 화면/결과 action의 렌더링과 호출 제거. 새 대체 원본 정리 UI는 만들지 않음.

## P0 / P1 점검

- **P0:** 이번0단계 직전 snapshot 대비 MainActivity +5/-353. 새 판단/계산은 추가하지 않았고 기존 진입과 result는 전용 게이트 위임, UI binding만 추가. 전체 기존 Activity가 순수 lifecycle라는 뜻은 아니다.
- **P1:** 실제 요청 sink를 제거해 단순 버튼 숨김만으로 차단을 주장하지 않는다. 원본/복사본/미디어 기록은 기기에서 변경하지 않았다.
- 정확한 이번 Activity diff: `.debug/cleanup-phase0/activity-phase0.patch`. 기준 파일: `.debug/cleanup-phase0/MainActivity.before.java`. HEAD 전체 diff에는 이전 October19 및 MIXED/진행 패치가 섞여 있으므로 이번 변경량으로 오인하지 않는다.

## 보존 확인

직전 snapshot과 다음 Activity 메서드 본문이 **문자열 단위 동일**함을 확인:

- hasDisplayNameInPath / duplicateFileSignature — 기존 사진 복사 중복 판정.
- savePendingOriginalCleanup / clearPendingOriginalCleanup / loadPendingOriginalCleanup / ensurePendingOriginalUrisLoaded — 기존 후보 이력 저장/읽기 함수.

다음 파일 SHA256은 시작 전후 동일(차이0):

- MediaCopyEngine.java, MediaStoreAlbumLookup.java.
- IncrementalMediaScanner.java, PreviewRefreshGate.java.
- MemoryMediaResolver.java.

이는 관련 소스를 변경하지 않았다는 근거이며, 전체 앱 동작이나 실기기 성능의 무회귀 보장은 아니다. 기존 save/clear 함수 자체는 남아 있고 새 복사 시작/기존 마이그레이션 등의 기존 호출에 의한 후보 초기화 동작까지 해결한 것은 아니다. 이번 차단은 legacy 클릭/옛 결과 응답이 새로 후보를 수집하거나 지우는 경로를 없앤 범위다. 원장 누적/복구는 Phase1에서 다룬다.

## 검증 결과

- 변경 후 targeted gate/isolation 검사 통과.
- `gradlew.bat testDebugUnitTest testReleaseUnitTest assembleDebug assembleRelease --console=plain`: **BUILD SUCCESSFUL**.
- Debug336 / Release336 tests, failures0/errors0/skipped0.
- CleanupFeatureGateTest3개: 후보 수와 관계없이 default-off, 반복 legacy 요청의 안내/성공 오표시 방지, 옛 성공·취소·알 수 없는 응답 차단.
- LegacyCleanupIsolationTest2개: Android Activity를 인스턴스화하지 않고 컴파일 constant pool을 읽어 requestTrash/delete API 및 직접 is_trashed 컬럼 literal 부재 확인. SDK 실제 요청 동작을 테스트한 것은 아니다.
- 전체 기존 테스트를 함께 실행하여 복사/Memory/캐시의 기존 단위 회귀를 확인.
- `lintVitalRelease` 통과. 전체 lintDebug/lintRelease는 실행하지 않음. 기존 Java8/deprecated/unchecked 및 CRLF 정규화 경고는 남음.
- 소스 전체 검색에서 createTrashRequest/createDeleteRequest/직접 put("is_trashed")/collectCleanupOriginalUrisFromPreview 호출0 확인. 읽기 전용 휴지통 제외 쿼리는 보존.
- `git diff --check` 통과.
- 로그 `.debug/cleanup-phase0/after-targeted-tests.log`, `full-verification.log`.
- 로컬 APK: app/build/outputs/apk/debug/app-debug.apk 및 app/build/outputs/apk/release/app-release.apk. 버전은39 그대로이며 **이미 배포한39와 다른 로컬 코드**다. 새 릴리스로 업로드하거나 기존 폰에 설치하지 않았다. 정식39 AAB 보관본은 변경하지 않음.

## Pre-release / 후속

1. 안티의0단계 코드 리뷰 후 Phase1 진행 여부 결정. 설계 승인과 구현 승인 구분.
2. Android 실제 화면에서 메뉴 미노출/옛 callback 복구 시 동작은 이번에 확인하지 않음. 이 패치를 배포한다면 새 버전과 Play 서명 검증이 필요하며 앱 삭제로 서명 충돌을 우회하지 않는다.
3. 상수풀 검사는 Activity의 직접 sink 회귀를 탐지하는 범위다. 향후 다른 클래스/리플렉션/새 controller를 도입할 때는 별도 실행 정책과 테스트를 검토해야 한다.
4. 과거 후보 기록 누락/초기화, 증거 원장/내용 검증/MemoryDependencyGuard/사후감사 등은 아직 구현하지 않았다. 원본 정리는 계속 사용 불가.

## Phase 0 실기기 설치 완료 — 1.3.10 (40)
- 사용자 요청에 따라 앱 삭제 없이 Play 앱 서명 APK로 `adb install -r` 성공 (Samsung SM_F971N).
- versionCode 40 / versionName 1.3.10 / targetSdk 36 / 앱 이름 `앨범 정리` 확인.
- firstInstallTime `2026-10-20 00:24:37` 유지, READ_MEDIA_IMAGES/VIDEO 권한 granted 유지.
- MainActivity 실행 및 홈 UI 확인: 기존 성남 2266장, 수원 950장 등 장소 카드 표시. 이는 UI 관찰이며 전체 데이터 무결성 검증을 의미하지 않음.
- 사진 복사/이동/휴지통 조작은 수행하지 않음. Phase 0 원본 정리 차단 유지. Phase 1 미구현.
- Play internal은 code40 draft 용도로만 갱신. 프로덕션 배포/검토 제출 안 함 (기존 code39 completed).
- 빌드 `bundleRelease` 성공. 기존 Phase0 Debug/Release 각336개 테스트 통과 이후 버전만 변경.
- AAB: `photoplace-1.3.10-code40-test.aab`, SHA256 `BE0518AEDF707078887140B68CA6ADF54B023FF57F4296AA11C14199F206FB84`.
- 설치 APK: `photoplace-1.3.10-code40-play-signed.apk`, SHA256 `A06154C06E05779C0EF6A2A2AA73226D6A693BFC60A6D949906B10BD552C1A2D`.
- 서명 SHA256 `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d` 일치 확인.
- 로그/화면: `.debug/cleanup-phase0/test40-build.log`, `test40-upload.log`, `test40-ui.xml`.
- 안티 Phase0 코드 승인 보고서 확인. 사용자의 실제 앨범 복사/검색 회귀 확인은 대기.
