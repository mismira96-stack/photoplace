# PhotoPlace 1.3.17 (code53) 기기 테스트 설치

- 사용자 요청: 연결된 폰에 최신 UI/백그라운드·복귀 수정본 설치.
- R5KL503VHQR, SM_F971N에 `adb install -r` 성공.
- 설치 후 실제 패키지: com.photoplace.app / versionCode 53 / versionName 1.3.17 / targetSdk 36.
- firstInstallTime 2026-10-20 00:24:37 유지, lastUpdateTime 2026-10-25 21:04:06. 앱 삭제/데이터 초기화하지 않음.
- APK: photoplace-1.3.17-code53-background-test-play-signed.apk (2,866,363 bytes).
- APK SHA256: 91097F128A88FDBCAE67F254DB45003BD3D3C5E91F98265EC8B76CED12116B31.
- APK signer SHA256: b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d (기존 Play 설치본과 동일).
- AAB: photoplace-1.3.17-code53-background-test.aab.
- Debug/Release 각425 단위 테스트 통과, bundleRelease 성공. 기존 deprecated Java/API 경고 유지.

## Play 상태
- Play 서명 APK를 얻기 위한 internal `1.3.17 Background Resume Test` / code53 / `draft`만 저장.
- production code51 `halted`, code39 `completed`를 설치 시점에 재확인.
- 프로덕션 제출/출시 재개하지 않음. Drive APK는 여전히 code52이며 이번 작업에서 업로드하지 않음.

## 기기 확인 순서
1. 원본 정리 진입 및 새 전체 진행 화면 확인.
2. 사용자가 이번에 옮긴 원본 중 필요한 소량을 먼저 복원해 실행 확인.
3. 배치 경계 확인은 301장으로 수행. 다음 묶음 대기에서 '나중에 계속', 앱 재진입, 완료 수량 유지 확인.
4. 승인 후 보존 확인 중 홈/화면 잠금, 복귀 및 알림 확인. 원본 이동은 시스템 승인에서만 수행.
5. 위치앨범의 복사본 및 기록/메모 보존 확인.

사용자 사진의 복원·휴지통 이동은 이번 설치 작업에서 수행하지 않음. 상세 구현/남은 게이트: REVIEW_Cleanup_Background_Resume_2026-10-25.md.
