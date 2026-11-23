# Memory Browsing MVP 리뷰 (commit c79d11a + 로컬 변경)

검토 기준
- 최신 푸시: `c79d11a Harden discovery memory merge`
- 현행 working tree 포함(로컬: Memory detail 날짜 헤더 UI 조정 포함)
- 참고 문서: `AGENT_SHARED_TODO.md`, `WORKLOG_2026-08-15.md`, `DISPLAY_FIRST_ORGANIZE_OPTIONAL_DESIGN_2026-08-10.md`

요약
- `발견한 장소 둘러보기`를 discovery-backed 전용으로 제한한 결정은 MVP 관점에서 타당.
- MIXED merge 조건을 보수화한 방향(정확한 `memoryKey` 우선, fallback에 adminArea/date-overlap 추가)은 안전성 측면에서 적절.
- Memory detail의 날짜별 섹션 모델(`MemoryPhotoSection`/`MemoryPhotoItem`)과 UI 조정은 모델/뷰 분리가 잘 되어 있음.
- 48개 visible cap, thumbnail MIME 전달 개선, NoLocationCache v2 보류 결정 모두 합리적.

블로커 (릴리스 전 필수 조치)
1. MIXED 자동병합 오탐 위험
   - fallback 병합(예: `countryCode + canonicalPlaceName` + (adminArea OR date-overlap))으로 변경했음에도 ambiguous 사례 존재 가능.
   - 권장: ambiguous 케이스는 자동 병합 대신 ‘후보’ 표기하거나 telemetry로 수집 후 정책 조정.
2. 권한(권한철회) vs 앨범 삭제 판정
   - `liveState=MISSING`를 즉시 삭제로 처리하면 권한 회수 상황에서 UX 오해 발생.
   - 권장: `UNKNOWN` 상태 추가 및 권한 재시도/권한 안내 흐름 구현.
3. Detail UI 대량 노출 위험
   - 48개 cap은 안전하나 cap 초과 경로에 대해 lazy grid/RecyclerView 전환 또는 페이징/더보기 핸들러 필요.

논블로커 (권장 개선)
- `MemoryRepository.discoveryMemories()` 분리: 적절. 네이밍을 `discoveryOnlyMemories()` 등으로 명확화 권장.
- adminArea/date-overlap threshold를 명시하고 단위테스트로 검증 권장.
- 날짜 포맷/연도 표시 로직은 renderer로 완전 위임(로케일 고려).
- thumbnail MIME 전달: 대부분 기기에서 chooser 감소에 효과적, 벤더별 실제 기기 검증 권장.
- NoLocationCache v2 보류: 안전 판단. TODO에 signature/invalidaton/tests가 완전한지 확인.

구체적 권장 작업(우선순위)
1. (긴급) Merge ambiguity 완화 패치 + 단위테스트 추가
   - fallback 병합 조건: placeKey match AND (adminArea equal OR dateOverlap >= X)
   - ambiguous 케이스에 대한 단위테스트(동일 placeKey 다른 admin/date) 추가
2. Live-state/권한 분기 보강
   - `MISSING` -> `UNKNOWN` 분기, 권한 에러 안내 및 재시도 버튼
3. Detail UI 보호
   - 48개 cap 외에 ‘더보기’/페이징 트리거, 동시 썸네일 디코드 제한 도입
4. MainActivity 분리 계획 수립
   - 최소 분리: `DiscoverySnapshotController`, `MemoryBrowserController`, `MemoryDetailRenderer` (MainActivity는 navigation/wiring만)
5. Device smoke test 실행(아래 체크리스트 기반)

Play 배포 전 필수 기기 Smoke Test
- Preview → `앱에서 보기`: discovery-only 항목만 표시되는지
- discovery detail: 날짜 헤더/섹션 정렬(newest-first), 장소명 중복 제거 확인
- thumbnail tap: 이미지/비디오 뷰어 열림, chooser 과다 노출 없음(삼성 포함)
- organized-only 앨범은 `정리 기록`에서만 표시되는지
- 삭제/이동된 소스: detail 열기 시 앱 crash 없음, placeholder/메시지 표시
- 권한 철회 시: UNKNOWN/권한 안내 흐름 정상 동작
- 대형 스냅샷(수천장)에서 홈/디테일 진입 시 ANR/OOM 여부 확인

다음 액션 제안
- A: 제가 `MemoryRepository`의 merge 조건을 보수적으로 패치하고 관련 단위테스트(3–4개)를 추가해서 PR 초안 만들겠습니다. (권장, 우선순위 높음)
- B: 또는 Play 전용 기기 Smoke Test 단계별 ADB 명령 포함 체크리스트를 상세화해 드리겠습니다.

원하시면 즉시 A 또는 B 중 하나를 실행하겠습니다.
