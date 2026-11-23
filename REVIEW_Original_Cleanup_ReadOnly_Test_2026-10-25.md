# 원본 정리 검토 전용 테스트 — 1.3.13(45)

사용자의 설치 요청에 따라, 발견된 실행 안전성 오류를 우회하지 않고 **검토만 가능한** 빌드를 만들었다. 후보·검사·보류 사유를 실제 폰에서 확인하기 위한 버전이며 휴지통 실행 기능 완료를 뜻하지 않는다.

## 변경

- `CleanupExecutionPolicy.isEnabled() = false` 도입. 최종 `CleanupTrashDispatcher.dispatch`의 첫 경계에서 거부하며 DB journal·OS API에 도달하지 않는다. 검토 진입용 CleanupFeatureGate는 유지한다.
- 검토 다이얼로그에 실행 잠금 안내를 표시하고 휴지통 버튼을 비활성화한다. 긴 진단 내용은 별도 ‘검사 상세 보기’로 열어 목록/버튼이 밀리지 않게 했다.
- `CleanupReviewCoordinator`: 과거 MATCH 캐시를 실행 자격처럼 재사용하지 않고 매번 두 파일 전체를 다시 검사한다. 내용 검사 루프 중 진행률을 전달한다.
- `CleanupMediaAccess`: datetaken NULL만 명시적 unknown 값으로 기록해 내용 검사를 허용한다. 크기·URI 식별 관련 필수 필드의 NULL 차단은 유지한다. 촬영일을 임의 생성하지 않는다.
- 과거 사진 탐색의 첫100쌍 제한을 제거했다. 결과는 실제 후보 쌍을 기준으로 표시하며 전체 중복 원본 수라고 단정하지 않는다. 현재 탐색기 자체의 경로/이름+크기 검색 범위는 기존44와 같고 새로운 해시 추정 규칙을 추가하지 않았다.
- 후보 없음과 검사 탈락을 구분: 과거 검색 후보 수, 검사 수, 내용 일치 수, 검토 통과 수, 읽기/크기/내용/연결 보류 사유를 표시한다. 후보가 있어도 전원 연결 검증에서 보류될 수 있다.
- MainActivity 제품 코드 변경 없음. 인수 시점 scoped patch를 재구성해 현재 정규화된 텍스트와 차이0을 확인했다. 바이트 단위 해시는 달라져 현재 SHA256은 `ADA09520E157E4373365EE4DFDE4B77820911A727CC17A04926AAD29C7948F96`이다. 해시가 동일하다고 보고하지 않는다.

## 검증

- `testDebugUnitTest testReleaseUnitTest bundleRelease`: Debug385/Release385, failures/errors/skipped0, 빌드 성공.
- 기존 cache 재사용 테스트를 ‘캐시가 있어도 현재 읽기 불가이면 탈락’ 회귀로 수정했다.
- 신규 CleanupReviewSafetyTest: 최종 Dispatcher가 Android/DB 접근 전에 거부함, 후보0과 후보 존재·검증 탈락의 진단 구분, 촬영일 NULL만 허용하고 필수 신원 필드는 여전히 차단함 확인.
- 최종 UI 레이아웃 조정 후 bundleRelease 재생성 완료. git diff --check 공백 오류 없음.
- Google 생성 APK의 DEX에 실행 정책/진단 클래스와 metadata 처리 메서드가 포함됨을 확인했다.
- 미해결 사후 감사 해시 비교·journal 상태 복구·선택 직전 재검증·후보 출처/의존성 범위는 인수 보고서의 차단 과제로 유지한다. **실행을 잠근 것이 이 과제들을 해결한 것은 아니다.**
- 사용자 사진의 실제 대량 검토 결과 및 취소/화면 수명 검증은 사용자가 새 버전에서 수행할 예정. 개발자가 검토 버튼/시스템 휴지통을 조작하지 않았다.

## 설치

- 버전1.3.13 / code45 / target36, 앱 표시 이름 ‘앨범 정리’.
- internal 트랙 draft만 업로드하여 Google Play 앱 서명 universal APK를 받았다. 프로덕션 배포/검토 제출 없음.
- AAB `photoplace-1.3.13-code45-test.aab`, SHA256 `190A44E4A5A2F029FE0478FC6D5E4112743C3152429244F192E97EFAE7B53F4C`.
- 설치 APK `photoplace-1.3.13-code45-play-signed.apk`, SHA256 `72BF0EED83672D8DEE33D0B10C7E51519BE7DE2A689E2DB5A9AE2A0EC445669C`.
- 서명 SHA256 `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d` 검증 후 `adb -s R5KL503VHQR install -r` 성공.
- 폰 versionCode45/versionName1.3.13 확인, 최초 설치 시각2026-10-20 00:24:37 및 READ_MEDIA_IMAGES/VIDEO 권한 유지. 앱 삭제/데이터 초기화 없음.

로그: `.debug/cleanup-review-2026-10-25/test45-build.log`, `test45-final-bundle.log`, `test45-upload.log`.

## 사용자 확인

메인 원본 정리 버튼 → 대량 파일 검사 진행률 → 결과/검사 상세의 후보 수·내용 일치 수·보류 사유 확인. 0이어도 어디서 탈락했는지 구분한다. 휴지통 이동은 계속 꺼진 상태여야 한다.
