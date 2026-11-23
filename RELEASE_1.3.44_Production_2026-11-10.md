# PhotoPlace 1.3.44 / code80 프로덕션 제출 — 2026-11-10

- 사용자 실기기 확인 및 출시 승인: ‘확인했어 업데이트 출시하자’.
- 동일 검증 AAB 재사용, 재빌드 없음. SHA256: 34beffb09686be3f395f863632a2e8d62a450ca21e94471cc5608b155b020340.
- API에서 production 1.3.44/code80 전체 출시(status completed) 변경 및 validate/commit 성공.
- Console 게시 개요 실제 확인: 검토 중인 변경사항 > 프로덕션 > 1.3.44 전체 출시 시작. 현재 ‘일반적으로 발견되는 문제에 대한 빠른 검사 실행 중’, 성공 시 자동 심사 전송. 관리형 게시 사용 중지이므로 승인 후 자동 공개 흐름.
- 공개 완료 아님. 현재 자동 검사/심사 대기. 사용자 추가 버튼 조작 불필요. 다른 미제출 변경사항은 화면에 없음. 내부 draft80/기존 테스트 트랙 변경하지 않음.
- release-notes-1.3.44-ko-KR.txt 전체 변경점 등록 완료.
- 증거: .debug/collection-back80/production-submitted.png / production-promote.log / production-inspect-before.log.
- APK: photoplace-1.3.44-code80-play-signed.apk. SHA256: 0a69912d3f6d25279f6232cc0f2658c1b2d8da0b0d7e0d8e354884ad7677f6b9.
- 사전 검증 Debug/Release 각522 PASS, API33/API36 Back/IME/공유 큐 지연 재현 수정/캐시 재사용/종료 PASS, 기존 Story 회귀 PASS. 사용자 기기 사진 복귀 확인 완료.
