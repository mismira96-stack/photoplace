# 1.3.8(38) 성남 추가 기능 실기기 테스트 설치

- 사용자 요청: 앱 삭제 없이 폰에 설치하여 성남으로 확인.
- 변경: 기존 MIXED 앨범 추가 패치 + versionCode38/versionName1.3.8. 앱 표시 이름 ‘앨범 정리’ 유지.
- `bundleRelease` 성공. 제품 코드 검증은 직전 Debug/Release 각327 tests 통과 결과. 버전 변경 후에는 bundleRelease만 실행.
- AAB: `C:\work\projects\PhotoPlace\photoplace-1.3.8-code38-test.aab`.
- AAB SHA256: `BB40EFC3038A4863ADD49D663F2956239F91FBB99E6BD32F39579498CA766BDE`.
- 내부 테스트 초안38 업로드/커밋 완료. 후속 API 재조회: production37 completed, internal38 draft +37 completed. 다른 트랙 출시/검토 제출 없음.
- 업로드 로그의 한국어 출시명은 터미널 문자 인코딩으로 깨져 표시됨. API 전달/저장된 실제 이름의 정확성은 이번 재조회에서 별도 대조하지 않음.

## 데이터 유지 설치

- Google Play generatedapks.list + download_media로 bundle38의 Play 서명 universal APK를 받음. 일반 download는204 No Content였고 미디어 다운로드 메서드로 성공.
- APK: `C:\work\projects\PhotoPlace\photoplace-1.3.8-code38-play-signed.apk`, 2,809,019 bytes.
- SHA256: `2819E024A57CDB4A1396E989BB6F081D9067CC21CFC58DD3C296C7DE93AC664D`.
- 설치본 및 다운로드 APK signing certificate SHA256 모두 `B9F9D963A583DD74D03B5485B827FEA997AFEB17100CB0B201A01AA264D6D31D`.
- apksigner 검증 및 aapt2의 package/version/SDK/label 확인 후 `adb install -r` 실행: Success.
- 대상 기기 R5KL503VHQR, com.photoplace.app. 기존 앱 제거/데이터 초기화/캐시 삭제하지 않음.
- 설치 후 version38, 최초 설치 시각2026-10-20 00:24:37 유지, 사진·영상·위치 정보 읽기 권한 유지 확인.
- 앱 실행 후 기존 해외 기록 및 성남 사진2259/영상30, 수원950/24 등 홈 데이터 표시 확인. 비공개 앱 파일 원문 해시는 접근 불가하여 메모/모음 파일 단위 전후 불변을 증명한 것은 아님.
- 에이전트는 성남 추가 확인창/실제 복사를 누르지 않음. 현재 앱 홈에 열어둠. 사용자가 성남 상세→앨범에 추가→추가 예정 수량/기존 중복 확인 순서로 검증.

## 남은 확인

- 성남 ‘앨범에 추가’ 표시 및 준비 수량, UI 대기/캐시 재진입, 실제 복사 후 일관성은 사용자 테스트 대기.
- 원본 휴지통 비활성화 유지. 새 사진 생성이나 기존 파일 이동/삭제를 에이전트가 수행하지 않음.
- 출시 노트: `release-notes-1.3.8-test-ko-KR.txt`. 로그 및 설치 전후 정보: `.debug/device-20261024/test38-*`.
