# 실기기 발견기록 후속 / 스냅샷 해석 캐시 리뷰 요청

프로젝트 AGENTS.md/GEMINI.md를 먼저 읽고 P0 MainActivity 신규 판단0줄 / P1 데이터 보존을 우선 확인해 주세요.

이번 추가 범위: DiscoverySnapshotStore.java, DiscoverySnapshotDecodeCache.java, DiscoverySnapshotStoreTest.java, DiscoverySnapshotDecodeCacheTest.java. 기존 HEAD1c8d683 대비 누적 working tree의 앞선 비동기/다중 장소/영상 탭 패치와 구분해 주세요. 전체 근거/파일·줄/실측/한계는 REVIEW_Device_Discovery_Performance_2026-10-19.md에 있습니다.

검토할 점:

- 같은 길이/mtime라도 내용이 다르면 재해석하는지, missing/corrupt/unsupported/backup 복구/저장 실패가 옛 캐시로 가려지지 않는지.
- 반환 snapshot/하위 모델 불변성, synchronized 접근, 인스턴스별4M char+모델 보관 한계, 큰 입력은 표시하되 캐시하지 않는 정책.
- readTextFile의16K char 읽기와 개행 보존이 기존 JSON/저장 보호 동작에 영향을 주는지.
- 300 tests PASS/debug build PASS와 실기기 관측을 확인하되, 첫 탭 지연이나 성남 카드2,107 대 상세2,283 불일치가 해결됐다고 간주하지 않을 것.

전체 결과는 루트 `REVIEW_Device_Discovery_Performance_Antigravity_2026-10-19.md`에 저장해 주세요. 각 발견 사항에 Blocker / Pre-release / Follow-up, 파일·줄, 재현, 영향, 수정 제안을 포함해 주세요. 이번 패치의 승인 여부를 이전290-test 승인과 구분해 주세요.
