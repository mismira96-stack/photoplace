# 독립 리뷰 요청 — 원본 정리 검토 UX와 개별 보류

2026-10-25. 사용자가 실기기 code45에서 본3513/3778/0의 혼란을 해결하기 위한 code46 변경이다. 상세 근거와 한계는 REVIEW_Original_Cleanup_Review_UX_2026-10-25.md.

## 검토 우선순위

1. P0: MainActivity는 `.debug/cleanup-review-2026-10-25/MainActivity.code46.patch`의 UI 배치+위임 +1/-1만 변경됐는가?
2. P1: CleanupExecutionPolicy=false 및 최종 Dispatcher 차단이 유지되는가? 실제 사후 감사/실행 기능을 완성했다고 오인하지 말아 달라.
3. MemoryDependencyGuard.Report가 실제 원본별로 확인한 문제와 전역 미완료를 구분하는가? complete=false를 임의의 무의존성 PASS로 바꾸는 경로가 없는가?
4. Coordinator가 보류 원본 제거 후 나머지 **전체 집합을 다시 project/check**하는가? 최대 패스 도달·오류·취소·revision 변경은 미통과를 유지하는가?
5. 동일 URI 쌍의 작업 간 중복 제거, 원본별 화면/용량 집계가 적절한가? 다중 target의 전체 증거가 evidenceCandidates에 유지되는가? 미래 실행 시 대표 행 하나만 사용하면 안 된다는 제한이 충분한가?
6. 실제 참조 출처의 사진별 라벨이 과거 이력을 현재 대표사진이라고 단정하지 않는가? 자동 메모/alias/이력 쓰기가 없는가?
7. CleanupProgressScreen, ReviewDialog/ThumbnailLoader의 취소·Activity 종료·thumbnail queue/재사용 및 결과 필터를 검토해 달라. 프로세스 재시작 작업 복구는 아직 범위 밖이다.
8. 홈 대신 위치앨범 상단 진입, 완료 후 보조 진입이 일반 검색/뒤로가기 흐름에 영향을 주지 않는가?

신규/주요 파일: MemoryDependencyGuard, CleanupMemorySnapshotReader, CleanupReviewCoordinator, CleanupReviewAssessment, CleanupReviewDiagnostics, CleanupProgressScreen, CleanupReviewEntry, CleanupReviewDialog, CleanupThumbnailLoader. 추가로 MemoryCardCountPresentation의 보류 문구 수정.

Debug396/Release396 및 빌드 통과. Android 합성 UI 캡처/필터검사 완료. 실제 사용자 대량 결과는 설치46 이후 사용자 확인 대기다. 기본 Git HEAD diff에는 이전 여러 세션 작업이 포함돼 있으므로 이번 변경으로 모두 귀속하지 말아 달라.
