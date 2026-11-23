# 1.3.21(code57) 사용자 폰 테스트 설치

- 요청: 기존 데이터 유지 업데이트 설치, 사용자가 직접 테스트.
- artifact: photoplace-1.3.21-code57-memory-test-play-signed.apk
- APK SHA256: 97c328be90f45efc81e512f54145f840bf1f428d3cbf71bfbb1f24e6be45a93c
- AAB: photoplace-1.3.21-code57-memory-test.aab
- AAB SHA256: b1d16fcabd5af0a3ade2de9f59d594b33796c30ae41f9835bde5da2f0a2e9a4a
- Play signer: b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d (설치 전/후 APK 동일 서명 확인)
- R5KL503VHQR / SM_F971N: adb install -r Success, versionCode57/versionName1.3.21, MainActivity topResumedActivity 확인.
- firstInstallTime 2026-10-20 00:24:37 유지. lastUpdateTime 2026-11-08 15:03:04.
- 실제 폰의 사진·동영상 선택/이동/휴지통 승인 조작은 하지 않았다. 앱 업데이트/실행만 수행.
- Play API 확인: internal57 draft + 기존37 completed 보존, production56 completed 유지. 검토 제출/출시 없음.
- 안티 리뷰가 준비 중 도착해 B-1/B-2/B-3을 먼저 수정했다. 독립 재리뷰는 대기. 후속 문서와 R2 snapshot/READY 참조.
- 검증: Debug/Release 각474개, 실패0 오류0 스킵0 / bundleRelease PASS. API33 후속 통합 flow PASS.

테스트 순서: 홈 사진 찾기로 최근 위치 없음 inventory 확보 → 메모리 목록 위치 없는 사진 정리 → 소량 선택 후 기존/새 위치기록 연결 → 기록 보기/날짜 확인 → 앨범 생성 → 복사본 장소 변경·해제. 일반 휴지통은 메모리 상세의 사진 선택 · 정리 또는 단건 이 사진 정리에서 실제 선택한 파일을 대상으로 OS 확인한다. 기존 중복 원본 정리와 분리된 기능이다.
