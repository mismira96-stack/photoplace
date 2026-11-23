# PR Draft: Harden discovery preview (preview-first, no auto-album)

## 요약
- 문제: 재분석(re-run) 시 자동으로 장소별 앨범 생성/정리 CTA가 다시 노출되어 사용자가 의도치 않게 앨범을 생성할 수 있음.
- 목표: "발견한 장소 둘러보기(Discovery preview)"를 우선으로 유지하고, 사용자가 명시적으로 'jump'할 때만 앨범 생성/정리 CTA를 노출하도록 플로우를 분리한다.

> 이 PR은 설계·테스트·수동검증 체크리스트 초안입니다. 실제 코드 변경(구현)은 별도 PR에서 수행합니다.

## 제안된 설계(개념적)
- 책임 분리:
  - `DiscoverySnapshotController`: snapshot 저장/읽기(현재 동작 유지).
  - `MemoryBrowserController` (신규, thin): UI-표시용 state 생성과 CTA 노출 정책 결정.
  - `MainActivity`: navigation/wiring만 담당하도록 유지.
- 동작 규칙:
  - 재분석 시 snapshot 생성 → discovery preview 상태만 갱신. 자동으로 앨범 생성 CTA를 노출하지 않음.
  - Preview 완료 후 `앱에서 보기`로 진입한 경우에만 해당 detail에서 `앨범으로 정리 (jump)` CTA를 노출.
  - Organized album 항목은 기존대로 `정리된 앨범`으로 표시.

## 권장 단위테스트 목록
1. Existing: `DiscoverySnapshotControllerTest.savePreviewItemsPersistsDiscoverySnapshot` — 통과 확인
2. `MemoryBrowserControllerTest.previewOnlyDoesNotExposeOrganizeCTA`
   - DiscoverySnapshot + empty organizedAlbums → `showOrganizeCTA == false`
3. `MemoryBrowserControllerTest.previewThenJumpEnablesOrganizeCTA`
   - simulate entering from preview-complete dialog → `showOrganizeCTA == true`
4. `MemoryRepositoryTest.reanalysisDoesNotCreateStoredAlbumSummary`
   - save snapshot + re-run analysis → no writes to `AlbumSummaryHistoryStore` (mocked)
5. `MemoryBrowserStateTest.staleDiscoveryRefsFiltered`
   - detail filters stale sourceUris; CTA disabled when no sourceUris
6. Integration: `MainActivityIntegrationTest.showMemoryBrowserButtonVisibility`
   - empty state → no CTA; non-empty → CTA visible; organize CTA hidden until jump

## 수동 스모크 테스트 체크리스트
- Re-run analysis → 확인:
  - 홈/결과 화면에서 즉시 `앨범 만들기` CTA가 보이지 않아야 함.
  - 결과 dialog에서 `앱에서 보기`가 보이고, 탭 시 discovery preview로 진입.
  - preview detail에서 `앨범으로 정리`는 사용자가 명시적으로 jump을 선택했을 때만 표시.
- 권한/삭제 시나리오: 권한 철회 또는 소스 삭제 후 detail 열기 시 앱이 크래시 나지 않고 placeholder/메시지 표시.
- 대형 스냅샷: 수천장에서 preview가 빠르게 열리고 organize CTA는 기본적으로 숨김.

## 변경되지 않을 파일(검토대상)
- `DiscoverySnapshotController.java`
- `MemoryRepository.java`
- `MemoryBrowserState.java`
- `MainActivity.java`

## PR 템플릿(본문 예시)
- 제목: Harden discovery preview: keep discovery-only preview separate from auto-album creation
- 본문:
  - 문제 설명 (재분석 시 자동 앨범 노출)
  - 설계 요약 (위 참조)
  - 포함된 테스트 목록 및 수동 검증 체크리스트
  - 마이그레이션/롤백 계획: snapshot 읽기/backup 복구 정책 유지

## 다음 단계 제안
1. 위 단위테스트 템플릿 파일 생성 (JUnit) — 작은 테스트부터 추가
2. `MemoryBrowserController` 인터페이스 설계 및 단위 테스트 작성
3. `MainActivity`에서 CTA 노출 로직을 controller로 이동(작은 커밋들로 분할)

---
*작성자: 자동 문서 초안 — 검토 후 코드 변경 PR 생성 권장*
