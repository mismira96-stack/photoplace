# PhotoPlace OrganizationLink Worker Lifecycle 리뷰 요청

## 대상

커밋 체인:

- `8cd8192` Add organization link persistence
- `1243d8a` Harden organization link replay semantics
- `2380645` Persist organization context through sort worker
- `5b71235` Persist confirmed organization results idempotently

리뷰 기준 브랜치: `codex/photoplace-v2-bg-wip`

## 변경 요약

- `OrganizationLink`와 `MemoryOrganizationLinkStore`로 Memory/Collection stable identity와 Gallery output 경로를 연결하는 저장 기반을 추가했습니다.
- `OrganizationRequest`의 requestId, subject identity, 앨범명/경로를 sort input JSON에서 SortWorker를 거쳐 sort result JSON까지 선택적으로 전달합니다. 기존 일반 정리 요청은 메타데이터 없이 동작합니다.
- persisted worker result가 확인한 성공 항목으로만 `OrganizationLink`를 구성합니다. 복사 성공이 없거나 copied item의 target path가 요청과 다르면 link를 만들지 않습니다.
- 결과 처리 전에 앨범 이력(requestId), `path:` alias, OrganizationLink를 기록합니다. 저장이 실패하면 result JSON을 유지하고 재시도하며, 재처리에서 history/link가 중복 생성되지 않도록 했습니다.
- `SUCCESS`, `PARTIAL`, `MISSING` lifecycle을 지원합니다. Gallery output이 사라져도 Memory 자체를 삭제하지 않는 구조입니다.

## 이번 범위 밖

- Memory 상세의 single-place 앨범 생성 CTA는 아직 없습니다. 현재 화면에서는 OrganizationRequest를 보내지 않으므로 사용자에게 보이는 정리 흐름은 바뀌지 않았습니다.
- `MemoryRepository`의 exact-link 조회와 Gallery media resolver는 아직 연결하지 않았습니다.
- 실기기에서 Gallery 파일을 생성하거나 Play 출시 검증을 수행하지 않았습니다.
- 기존 `AlbumSummaryHistoryStore` 레코드를 이름만으로 OrganizationLink에 backfill하지 않습니다. backfill 정책은 미정입니다.

## 집중 리뷰 요청

1. requestId 기반 재처리가 실제로 멱등적인가? history, alias, link 저장 사이에서 프로세스가 종료될 때 중복·누락·불일치가 생길 수 있는가?
2. `SortResultStore`의 결과를 소비하기 전에 persistence를 완료하고, 실패 시 JSON을 유지해 재시도하는 흐름이 안전한가?
3. `OrganizationLinkFromSortResult`가 잘못된/불완전한 결과를 link로 만들 수 있는가? copied count, sorted items, target path 및 partial/cancel 상태 처리를 확인해 주세요.
4. `AlbumSummaryHistoryStore.appendSession(requestId, ...)`의 중복 방지와 기존 requestId 없는 세션의 호환성이 적절한가?
5. `path:` alias가 이미 다른 stable ID에 연결된 경우의 동작이 안전하고 명확한가? alias 충돌 중에도 exact OrganizationLink를 기록하는 현재 정책이 적절한가?
6. `SUCCESS` / `PARTIAL` / `MISSING` 상태, 재정리 이력 보존 및 `findLatestUsableLink()` 판정에 lifecycle 오류가 없는가?
7. 일반 정리 요청에 metadata가 없을 때 기존 Worker/history 동작이 회귀하지 않는가?
8. Worker → result → persistence 통합을 실제 CTA에 연결하기 전에 보완해야 할 blocker나 테스트 공백은 무엇인가?

## 검증

- `testDebugUnitTest`: 통과
- `assembleDebug`: 통과
- `git diff --check`: 통과
- 실기기 검증은 미수행

## 리뷰 결과 형식

- Blocker: 재현 가능한 문제와 파일/함수 위치
- Recommendation: Worker/UI 연결 전에 반드시 고칠 항목과 후속 항목 구분
- Test gaps
- 최종 판단: 승인 / 수정 후 승인 / 보류

코드 수정은 하지 말고 리뷰만 부탁합니다.
