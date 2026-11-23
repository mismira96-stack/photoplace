# PhotoPlace 1.3.39 (code75) 프로덕션 제출 기록

날짜: 2026-11-09 (KST)
사용자 승인: “츨시하자 고고쓍” — code75 확인 후 프로덕션 출시 요청.

## 실제 결과
- Google Play 프로덕션 1.3.39 / versionCode75 / 전체 출시 시작 제출 완료.
- 콘솔 게시 개요: **검토 중인 변경사항 → 일반적으로 발견되는 문제에 대한 빠른 검사 실행 중**.
- 목록에는 프로덕션 1.3.39 전체 출시 시작 1건만 표시. 다른 스토어/정책 변경은 함께 제출하지 않음.
- 검사가 성공하면 자동 검토 전송. 관리형 게시 사용 중지됨을 확인하여 승인 후 자동 게시. 실제 공개 게시/승인 완료로 주장하지 않음.
- 사용자가 추가로 눌러야 하는 제출 버튼 없음. 승인까지 비동기 대기 상태이며 자동 모니터/새 배포 작업은 만들지 않음.

## 동일 빌드 검증
- 이미 Play에 업로드된 code75 bundle을 그대로 재사용. 소스 변경/새 버전/재빌드 없음.
- AAB: photoplace-1.3.39-code75-device-validation.aab
- 로컬 및 Play bundle SHA256 동일: 608b2ae9ebe11090b1c7a8b2f9fc1eaf07609d4b7db0b82d8094f4df6347d8e3.
- 사용자 확인 APK: photoplace-1.3.39-code75-play-signed.apk
- APK SHA256: d76a6061785070f78059308a44ead6da8ef0a3ccdea47e2e4391f49e6a5ea92f.
- 이전 빌드 검증: Debug/Release 각504 unit PASS, API33 native6종/저장/gesture/header PASS. 가로·세로 공유/저장 PNG code74와 byte-identical. 자세한 구현 증빙은 WORKLOG_2026-11-09.md.

## 출시 메모
release-notes-1.3.39-ko-KR.txt:
사진을 보고 정리하고 공유하는 흐름을 개선했어요.
• 발견 기록에서 사진을 선택해 위치기록에 연결하거나 휴지통으로 보낼 수 있어요.
• 위치 없는 사진을 기존 또는 새 위치기록으로 정리할 수 있어요.
• 사진 한 장을 폴라로이드 카드로 꾸며 Instagram 스토리에 공유할 수 있어요.
• 사진 크기·위치, 문구·서체·글자 크기를 조절하고 원할 때 갤러리에 저장할 수 있어요.
• 카드 미리보기와 위치 변경 화면의 사용성을 개선했어요.

## 진행 및 증빙
- API 사전 조회: production56 completed / internal75 draft. 게시 개요에 다른 변경사항 없음을 확인.
- production PUT code75/status completed(전체 출시 설정) 및 validate 성공.
- 첫 commit의 changesNotSentForReview=true는 Google이 자동 검토 전송 대상이라 거부. 실패 edit는 DELETE하여 미반영. 사용자 출시 승인이 있으므로 해당 파라미터 없이 정상 재실행.
- 정상 commit id: 15650020037170134948.
- 내부 테스트 및 다른 테스트 트랙 설정은 그대로 유지. 프로덕션만 변경.
- API 로그: .debug/release75-preflight.log / release75-promote.log / release75-promote-retry.log.
- 콘솔 최종 증빙: .debug/release75-2026-11-09/production-submitted.png.
- 게시 개요: https://play.google.com/console/u/0/developers/7774153056624295744/app/4974837404467270049/publishing
