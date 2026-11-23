# 안티 설계 리뷰 요청 — 정리한 원본 정리

## V2 재검토 안내

원문 조건부 승인 수신 후 ORIGINAL_CLEANUP_DESIGN_V2_2026-10-24.md를 작성했습니다. V1과 안티 원문은 보존했습니다. V2의8절 질문과 REVIEW_Original_Cleanup_Design_Followup_2026-10-24.md를 함께 확인해 주세요. B1/B2/B3를 상태·실행순서로 구체화했고,0ms보장·최종1회flush·일괄레거시삭제는 보완했습니다. **구현 요청은 아닙니다.** 재리뷰 결과는 별도 REVIEW_Original_Cleanup_Design_V2_Antigravity_2026-10-24.md로 남겨 주세요.

사용자 요청: ‘즉시 구현하지 말고 방향을 잡은 뒤 안티와 리뷰’. **설계 리뷰만 요청하며 구현/배포/폰 조작은 하지 말아주세요.**

## 읽을 문서

1. ORIGINAL_CLEANUP_DESIGN_2026-10-24.md — 제품 방향, 증거/검증/실행, 성능 경계, 단계별 계획, 결정 질문.
2. REVIEW_Original_Duplicate_Cleanup_2026-10-24.md — 기존 원본 후보/휴지통 경로의 확인된 위험.
3. WORKLOG_2026-10-24.md — 현재 출시39 및 성남 사용자 작업 맥락. 원본 cleanup은 이번 출시에서 복구하지 않았음.
4. 필요시 MediaCopyEngine / SortJob / SortResultStore, MainActivity의 기존 requestTrashCopiedOriginals / savePendingOriginalCleanup / collectCleanupOriginalUrisFromPreview, MemoryMediaResolver / OrganizationLink/identity 저장을 대조.

현재 소스는 **C:\work\projects\PhotoPlace**. 과거 Documents/Codex/GallerySorter 링크를 현재 checkout으로 가정하지 말아주세요.

## 제안 핵심

- ‘나중에 모아서 원본 검토’를 중심으로 하고 정리 직후 CTA도 동일 화면/정책으로 연결.
- MVP는 PhotoPlace가 만든 로컬 사진 복사본과 원본만. 일반 중복/유사사진/영상/클라우드/영구삭제/자동정리는 제외.
- source→actual destination 누적 기록, 과거 파일은 legacy hint 기반 읽기 전용 대조. 파일명 중복만으로 휴지통 허용 금지.
- 사용자 선택 원본/대상 전체 내용검증 및 실행직전 재검증, Memory/메모/모음 보존, 작은 시스템 배치 및 재시작 journal.
- target36 createTrashRequest 최대2000 URI 및 API29 실행 제외를 명시. 확인창 동안 외부변경 TOCTOU를 완전히 막는다고 주장하지 않음.
- 현재 검색/분석 캐시에 해시나 전체 검증을 넣지 않음. 원본 검토 진입 시만 별도 취소 가능한 작업.

## 리뷰 요청

- P0 MainActivity에 판단/계산을 넣지 않는 구조인지.
- P1 원본 손실 시나리오(내용검증 후 외부변경, 결과/기록 불일치, URI 재사용, 출력이 원본으로 잘못 선정, 일부만 처리됨)가 남는지.
- stableMemoryId/dateKey/모음이 원본 URI 존재 여부와 분리돼 유지되는지. 연결 불명시 보류 기준이 충분한지.
- 과거 파일 포함 범위가 과도하지 않은지, 어떤 자동 후보 선정을 허용하고 어떤 경우 사용자 선택을 더 받아야 하는지.
- 증거 store 설계가 복사 성능/메모리와 충돌하지 않는지. per-copy 쓰기/해시 검증 순서/장애복구의 구체화 필요 사항.
- 두 진입점 하나의 실행 정책, 기본미선택/확인/보류 UX, 시스템 요청 취소·프로세스종료·부분성공 처리의 누락.
- 기존 legacy 휴지통 경로를 기능 게이트로 통일하는 단계와 단계별 승인 조건.

설계의 좋은 점 요약보다 **실패 시나리오, 범위 축소 제안, 구현 전 결정해야 할 것**을 우선해 주세요. 문서의 설계 주장을 구현/실기기 검증 완료로 바꾸지 말아주세요.

결과는 프로젝트 규칙에 따라 REVIEW_Original_Cleanup_Design_Antigravity_2026-10-24.md에 저장하고 Blocker(구현 전 설계 결정)/Pre-release/Follow-up, 근거 파일·줄·조건·수정 제안을 구분해 주세요. 원문 설계는 승인 없이 덮어쓰지 말고 별도 리뷰로 남겨주세요.
