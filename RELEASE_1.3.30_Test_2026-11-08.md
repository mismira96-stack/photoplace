# 1.3.30(code66) 사용자 폰 테스트 설치 — 2026-11-08

- 요청: 안티가 마지막 수정 후 APK 준비를 마치지 못한 상태에서, 최신 코드 빌드 및 연결된 폰 설치.
- 소스: `C:/work/projects/PhotoPlace`. 안티의 최신 기능 코드는 그대로 유지하고 버전만65/1.3.29 →66/1.3.30으로 변경.
- 기존 Play code65 APK는18:18 준비본, MainActivity/MemoryPhotoViewer 최종 수정은19:41~19:42로 더 최신이므로 재사용하지 않음.
- 빌드: `testDebugUnitTest testReleaseUnitTest assembleRelease bundleRelease` 성공22초. Debug499 / Release499, failures0/errors0/skipped0. lintVitalRelease PASS. 기존 Java8/deprecated/unchecked 경고는 남음.
- 기존 설치본은 Play signer이므로 로컬 upload-signed APK를 덮어 설치하지 않음. 기존 승인된 내부 draft 서명 경로로 code66 APK 다운로드. **검토 제출/공개 출시하지 않음**.
- 서명 SHA256: `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d` 확인.
- 패키지 `com.photoplace.app`, versionCode66, versionName1.3.30, minSdk29/targetSdk36 확인.
- 소스 스냅샷350파일 변경0. 로컬 최신 릴리즈 APK와 Play APK의 classes.dex 바이트 일치.

## 산출물

- 설치용 APK: `photoplace-1.3.30-code66-ui-test-play-signed.apk` (5,840,284 bytes).
- APK SHA256: `bb0416c47fa6fe8905d7429331eb2432a921060cbc144fe153dbb4969bb9adf0`.
- 서명 생성에 사용한 AAB: `photoplace-1.3.30-code66-ui-test.aab`.
- AAB SHA256: `db5c4ff73163695a530ab6d17e1162063dbe71a33326ee3cbcd4cb258ca42bb5`.
- 로컬 upload-signed 빌드: `photoplace-1.3.30-code66-upload-signed.apk` — 폰에는 이 파일을 설치하지 않음.
- 테스트 안내: `release-notes-1.3.30-test-ko-KR.txt`.

## 실제 기기 관측

- 기기: R5KL503VHQR.
- 설치 전: 1.3.28/code64.
- `adb install -r` 성공. 앱 삭제/데이터 초기화 없음.
- 설치 후: 1.3.30/code66, `lastUpdateTime=2026-11-08 19:57:08`.
- 원래 `firstInstallTime=2026-10-20 00:24:37` 유지.
- MainActivity `am start -W` Status ok, cold launch252ms. 최상단 resumed Activity도 PhotoPlace MainActivity 확인.
- 개인 사진 휴지통 이동/위치 변경/공유는 에이전트가 실행하지 않음. 기능 실기기 수용 검증은 사용자가 수행할 단계.
- 최종 Play 조회: production56 completed 유지, internal66 draft + 기존37 completed 유지.

## 증빙과 범위

- `.debug/manual-place-2026-11-08/code66-codex-build.log`
- `.debug/manual-place-2026-11-08/code66-source-snapshot.json`
- `.debug/manual-place-2026-11-08/code66-apk-signature.txt`
- `.debug/manual-place-2026-11-08/code66-play-upload.log`, `code66-play-download.log`, `code66-play-final.json`
- `.debug/manual-place-2026-11-08/code66-phone-install.log`
- 릴리즈 준비 완료/새 기능 독립 리뷰 완료로 확대하지 않음. **테스트 APK 빌드 및 폰 업데이트 설치 완료, release_ready=false**.
