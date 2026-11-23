# PhotoPlace OrganizationLink 후속 패치 리뷰 요청

## 기준

기존 리뷰: `REVIEW_OrganizationLink_WorkerLifecycle_2026-09-13.md`

이번 후속 변경은 기존 리뷰의 Blocker와 “지금 고칠 것” 권고를 반영하고, TODO의 다음 데이터 계층 단계 일부까지 진행했습니다.

## 변경 요약

- 결과 persistence를 `NOT_REQUIRED`, `PERSISTED`, `RETRYABLE_FAILURE`, `PERMANENT_FAILURE`로 구분합니다.
- 잘못된 link payload 및 `CONFLICT` / `INVALID` commit은 재시도하지 않고 기존 정리 결과 화면으로 빠져나옵니다.
- 일시 persistence 실패는 최대 3회 재시도한 뒤 결과 화면으로 빠져나오며, Memory 연결 저장 실패를 사용자에게 알립니다.
- 앨범 이력 저장을 link 검증/commit보다 먼저 시도합니다. 이력이 저장된 뒤 결과 화면을 표시할 때는 동일 저장을 다시 호출하지 않습니다. 재시도 한도 초과 시 일반 결과 처리에서 앨범 이력 저장을 한 번 더 시도합니다.
- OrganizationLink 시각은 `SortResultStore.Snapshot.completedAtMillis`를 사용하고, 값이 없는 이전 결과만 현재 시각을 fallback으로 사용합니다.
- `path:` alias가 다른 Memory에 이미 연결된 경우 alias를 덮어쓰지 않고, request의 exact OrganizationLink를 source of truth로 유지한다는 주석을 추가했습니다.
- `MemoryIdentityRegistryStore`의 alias snapshot 및 `MemoryOrganizationLinkStore` 데이터를 `MemoryRepository`에 전달합니다.
- Repository는 stable ID의 최신 usable `MEMORY` link가 가리키는 exact Gallery path에서만 Discovery와 organized album을 병합합니다. exact link의 output이 현재 목록에 없으면 동명 앨범 heuristic merge를 억제합니다.
- exact link가 없는 기존 데이터는 과거 heuristic fallback을 유지합니다.

## 집중 검토

1. 영구 실패와 재시도 가능한 실패 구분이 저장소 API의 결과와 정확히 대응하는가?
2. 최대 3회 재시도 후 result snapshot을 소비하는 fallback이 결과 화면/album history 보존 관점에서 안전한가?
3. history-first 순서와 `historyAlreadyPersisted` 분기가 성공, permanent failure, retry exhaustion, metadata 없는 legacy request에서 정확한가?
4. exact stable ID + latest usable link + normalized path 매칭이 서로 다른 이름의 album에도 올바르게 연결되고, missing output에서 heuristic merge를 막는가?
5. exact link 없는 legacy fallback 및 `discoveryMemories()` 경계에 회귀가 없는가?
6. alias/link JSON snapshot을 repository 생성 시 추가로 읽는 비용과 일관성은 허용 가능한가?
7. Memory media resolver와 single-place CTA를 연결하기 전에 추가해야 할 blocker/test gap은 무엇인가?

## 검증

- `testDebugUnitTest`: PASS
- `assembleDebug`: PASS
- `git diff --check`: PASS
- 새 테스트: retry 횟수 한도, Worker 완료시각/경로 정규화, exact-link 우선 merge 및 missing output 보호.
- 실기기 검증 및 Gallery mutation은 수행하지 않았습니다.

## 요청

코드 수정 없이 Blocker / Recommendation / Test gaps / 최종 판단을 작성해 주세요. 특히 3회 초과 시 결과 snapshot을 소비하는 정책이 적절한지와 exact-link 우선 merge의 상태 경계를 중점 검토해 주세요.
