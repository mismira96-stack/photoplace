# 원본 정리 UI 후속 수정 / 사용자 테스트 빌드

## 판정과 배포 상태
- 사용자 테스트용 준비 완료. 사용자 사용성 승인 전 프로덕션 재제출 금지.
- Google Play production: code 51 `halted`, code 39 `completed` 확인.
- code 52 / 1.3.16은 Play 서명 APK 확보를 위한 internal `draft`만 생성. 출시/심사 제출하지 않음.
- 사용자 요청으로 Google Drive > Codex APK에 APK 업로드 완료. 접근 권한은 소유자만(제한됨) 유지.
- 다운로드: https://drive.google.com/file/d/1mwC4TsIKHmsclxGbo8k5LW7pPWY88pa2/view

## 사용자 문제와 수정
- 수천 장 개별 목록 대신 원본 저장소/폴더 및 보존할 위치앨범별 접힌 그룹 표시.
- 원본의 실제 MediaStore relative_path, 파일명, volume_name을 별도 작업 스레드에서 300개 단위 읽기 전용 조회.
- 원본 폴더와 보존할 Pictures 위치앨범을 각각 '휴지통으로' / '그대로 유지'로 표시.
- 기본 미선택 유지. 정리 가능 전체 선택, 폴더 선택, 펼친 개별 선택 지원.
- 위치 정보를 읽지 못한 원본은 선택 불가. 보류 탭은 실행 불가 및 이유 표시.
- 선택 0건 버튼 비활성 이유 안내. 원본 N장 휴지통 이동 버튼과 진입 버튼 터치 영역 개선.
- CleanupReviewModel, CleanupReviewSourceLoader로 분리. MainActivity는 UI 작업 직전 baseline과 SHA-256 동일.
- 실행 엔진, 해시/Memory 검증, 사후 감사, 300장 배치, 저널링에는 이번 UI 변경 없음.

## 검증
- testDebugUnitTest / testReleaseUnitTest 각 417건 통과 (실패 0, 오류 0).
- bundleRelease 성공. git diff --check 통과.
- 신규 모델 테스트: 3000장 그룹/전체 선택, 보류 제외, 부분 선택 토글, 위치 미확인 차단, 저장소 구분.
- 별도 emulator-5582에서 합성 3513건 중 3508건 전체 선택 후 활성 버튼/선택 건수 확인. 사용자 사진 변경 없음.
- 실제 MediaStore에 만든 폐기용 합성 이미지 2건의 서로 다른 원본 폴더와 보존 목적지 구분 smoke test PASS.
- 작은 화면/글꼴 1.3배의 UI 확인 수행. 마지막 helper 재설치는 전용 에뮬레이터 내부 저장공간 부족으로 실패했으므로, 최신 보류 탭 비활성 문구는 빌드/단위 테스트 확인까지이며 실기기 사용자 확인 필요.
- 전용 emulator-5582 종료 완료. 다른 emulator-5580 및 사용자 폰 조작 없음.

## 테스트 파일
- photoplace-1.3.16-code52-ui-test-play-signed.apk (2,858,171 bytes)
- package com.photoplace.app / code 52 / name 1.3.16
- APK SHA256: E0B5F02F4A71919719936BD23BDBCB2AAC2BE1D5C73D5B8B6CE1800663E0E271
- signer SHA256: b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d
- 기존 Play 설치본과 동일 서명. 기존 앱 삭제 없이 업데이트 설치 가능. 이번에는 물리 폰에 설치하지 않고 Drive 전달.
- 증빙: .debug/cleanup-review-2026-10-25/drive-upload52.jpg

## 다음 확인
1. 업데이트 후 위치앨범 > 원본 정리에서 실제 원본 폴더/보존할 위치앨범 구분 확인.
2. 폴더 선택/전체 선택과 보류 탭/버튼 동작 확인.
3. 실제 휴지통 시험은 소량부터 진행하고 위치앨범 및 메모 보존 확인.
4. 사용자 결과 확인 전 출시 재개/프로덕션 제출 금지.


## 사용자 실사용 제보: 자리 비운 후 재진입 오류 (미해결)
- 사용자: 진행 중 폰을 두고 갔다가 앱에 진입하니 오류 발생, 휴지통에 한 번 들어갔다 돌아온 뒤 정상화.
- 오류 발생 단계/정확한 문구, 휴지통이 갤러리인지 앱 내 동선인지 미확인. 정상 완료나 사진 보존을 확인한 것으로 간주하지 않음.
- 정적 확인: CleanupExecutionController.resume은 활성 저널을 다시 감사하며, MediaStore 조회/해시/의존성 확인 실패 시 중단 또는 NEEDS_RECOVERY로 남김. 갤러리 방문이 상태 갱신을 유발했는지는 추정일 뿐 원인 미확정.
- 후속: 사용자 단계/문구 확인 후 CleanupExecution 로그, 저널 상태 및 MediaStore 실제 상태를 대조. 화면 복귀/잠금·해제/프로세스 재생성 시나리오 재현 필요.
- 코드 수정/추가 배포 없음. 프로덕션 code51 중지 및 code52 테스트 상태 유지.


## 342054.jpg 오류 문구 확인 및 제한적 수정
- 확인 문구: 원본 검토 중 오류가 발생했습니다: 이전 휴지통 작업을 먼저 확인해 ...
- 직접 원인: CleanupController.startReview의 active 저널 분기가 resume을 게시한 뒤 고의 IllegalStateException을 던져 정상 재개 경로에도 오류 토스트 발생.
- 수정: 검토 진행 화면을 먼저 닫고 정상 상태 확인 안내 후 resume에 위임하고 return. 예외 처리/미디어 검증/저널 상태를 우회하지 않음. MainActivity 변경 없음.
- 범위 제한: 백그라운드/잠금 후 남은 실제 작업 상태 및 휴지통 방문으로 정상화된 원인까지 해결한 것은 아님. 물리 폰 재현 필요.
- Drive code52 APK에는 이 후속 수정 미포함. 새 APK 업로드/Play 제출 없음.
