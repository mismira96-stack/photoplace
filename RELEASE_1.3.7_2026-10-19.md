# PhotoPlace 1.3.7 출시 준비

- 날짜: 2026-10-19 (Asia/Seoul)
- 사용자 요청: 현재 상태로 AAB 생성 및 출시 준비, 오늘 작업 마무리.
- 소스: HEAD `1c8d6832b5e43278cef83012ab6b8753f59624ab`와 현재 미커밋 작업 트리. 제품 기능 추가 수정 없이 버전만 1.3.6(36) → 1.3.7(37).
- 패키지: `com.photoplace.app`
- AAB: `C:\work\projects\PhotoPlace\photoplace-1.3.7-code37-release.aab`
- Drive 복사본: `G:\내 드라이브\Codex APK\photoplace-1.3.7-code37-release.aab`
- 크기: 2,763,510 bytes
- SHA256: `9577154BB790DF722DD39A1F06B69B3C481DFD25F8294FBA10C181CCC0E3EA8E`
- 로컬, Drive 마운트 복사본, Play API bundle SHA256 동일.

## 검증

- `gradlew.bat testReleaseUnitTest bundleRelease --console=plain`: BUILD SUCCESSFUL.
- 릴리스 단위 테스트 319개, failures 0, errors 0.
- `jarsigner -verify`: jar verified. 자체 서명 인증서의 신뢰 체인/타임스탬프 및 POSIX 속성 경고 있음.
- Java 8 source/target 지원 중단 예정, deprecated API 및 unchecked 사용 경고 있음.
- `git diff --check`: 통과, 줄바꿈 정규화 경고만 있음.
- 전체 lint 재실행은 하지 않음. 기존 lint 부채는 REVIEW_PreRelease_Verification_2026-10-19.md 참조.
- 새 GPS 사진의 NEW 재등장 실기기 검증은 미완료. 기존 성능 개선이 모든 로딩 지연 해소를 의미하지 않음.

## Play 업로드

- production 트랙에 1.3.7 / code37 / status=draft 업로드 및 커밋 성공.
- changesNotSentForReview=true는 Google API가 거절하여 기존 업로드 스크립트의 draft 커밋 대체 경로로 완료.
- 커밋 후 별도 API 재조회: 1.3.7(37) draft, 기존 1.3.6(36) completed 유지 확인.
- Google API에 저장된 한국어 출시 노트 및 AAB 해시 확인.
- 최종 검토 전송/공개 출시는 수행하지 않음. photoplace-release 스킬에 따라 Play Console 정책 경고 확인 및 '검토를 위해 전송'은 사용자 수동 단계.
- 빌드 로그: `release-1.3.7-build.log`, 업로드 로그: `release-1.3.7-upload.log`.
- 출시 노트: `release-notes-1.3.7-ko-KR.txt` (홈/발견 로딩, 수량/NEW, 사진 전환/Back/스크롤 복원, 영상 연결/넓은 화면 안내 개선).
