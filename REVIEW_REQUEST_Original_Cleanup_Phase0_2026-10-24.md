# 안티 코드 리뷰 요청 — 원본 정리 Phase 0

사용자가 V2 설계 승인 후 ‘시작’ 지시하여0단계만 구현했습니다. Phase1 원장이나 새 휴지통 실행을 진행하지 말고 이번 패치의 경계와 차단을 리뷰해 주세요.

## 기준 및 대상

- 소스: C:\work\projects\PhotoPlace.
- 설계: ORIGINAL_CLEANUP_DESIGN_V2_2026-10-24.md 및 안티 DESIGN APPROVED 원문.
- 이번 요약/검증: REVIEW_Original_Cleanup_Phase0_2026-10-24.md.
- **정확한 Activity 기준:** `.debug/cleanup-phase0/MainActivity.before.java` 대 현재 MainActivity. scoped diff는 `.debug/cleanup-phase0/activity-phase0.patch`. Git HEAD 대비 전체 diff에는 이전 작업이 포함돼 있습니다.
- 신규 CleanupFeatureGate.java / CleanupFeatureGateTest.java / LegacyCleanupIsolationTest.java.

## 우선 리뷰 질문

1. P0: Activity의 +5/-353줄 중 새 비즈니스/계산 분기가 있는가? policy 위임 및 UI binding 외 추가가 없어야 한다. 줄수 감소만으로 PASS하지 말아주세요.
2. P1: 원본 버튼·compact card·결과 action·옛 dialog·직접 request·직접 is_trashed 변경·preview 중복 후보 수집·request21 결과 처리의 우회가 남아 있는가?
3. 후보 수와 old resultCode=-1/0/unknown으로 실행을 활성화하거나 기록을 지울 수 없는가? legacy 진입은 안내만 해야 한다.
4. createTrashRequest sink 자체를 제거했으며 gate false도 유지한다. future gate 활성화가 옛 실행을 되살리는 경로는 없는가?
5. 사진 복사 중복 판정과 source scanning/cache/Memory resolver, 기존 후보 파일의 읽기/저장 본문이 보존됐는가? source hashes와 snapshot 비교 근거를 확인해 주세요.
6. hasPendingOriginalCleanup을 ‘실행 가능한 후보’ 정책으로 막은 영향으로 홈/결과 화면의 다른 정상 경로가 숨겨지거나 깨지지는 않는가? 기존 상태 저장/복사 성공 표시와 gate 표시를 혼동하지 않는가?
7. 옛 success 결과 callback은 실제 OS 작업이 끝났더라도 검증 없이 성공으로 주장하지 않는다. 기존 기록을 보존한 채 차단 안내만 하는 처리가 적절한가?
8. 안전성 테스트 범위: 수정 전2개실패 → 후통과. Debug/Release각336개, 두 APK빌드 및 lintVitalRelease 통과. constant pool 검사의 과도한 가정/빠진 실행 경로가 없는가?

## 검증 한계/후속 범위

- 기기/미디어 조작, 설치/배포, 버전 증가 없음. Android UI 및 실제 시스템 요청 테스트는 하지 않음.
- 기존 clearPendingOriginalCleanup 및 신규복사시 초기화 동작 자체는 이번에 바꾸지 않았습니다. legacy 클릭/결과에 의한 변경만 차단했습니다. 원장 누적/복구는 Phase1입니다.
- 새로운 원장/해시/MemoryDependencyGuard/사후감사 구현은 없습니다. default-off 유지 상태입니다.
- 임의 테스트 수 목표를 추가하지 말고 실제 기능 불변/실패 시나리오를 기준으로 판정해 주세요.

결과는 REVIEW_Original_Cleanup_Phase0_Antigravity_2026-10-24.md에 Blocker/Pre-release/Follow-up, 파일/줄/조건/수정 제안과 함께 별도 저장해 주세요. 승인 없이 제품 코드/사용자 사진/원문 보고서를 수정하지 말아주세요.
