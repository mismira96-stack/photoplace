# 발견 장소 수량 문구·NEW 열람 해제·로딩 안내 유예 코드 리뷰 보고서

- **검토자:** Antigravity (Google DeepMind)
- **일자:** 2026-10-19
- **검토 대상:** [`REVIEW_Discovery_Counts_Seen_Feedback_2026-10-19.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_Discovery_Counts_Seen_Feedback_2026-10-19.md), [`MemoryBrowserMediaProjection.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserMediaProjection.java), [`MemorySeenPolicy.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemorySeenPolicy.java), [`MemorySeenStore.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemorySeenStore.java), [`DelayedLoadingNotice.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DelayedLoadingNotice.java), [`MemoryBrowserEntry.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserEntry.java), [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)
- **판정:** **코드 반영 승인 (APPROVED for Merge)** — 309 tests 통과, P0/P1 게이트 통과, 성남 수량 불일치 완전 해소 확인

---

## 1. P0 / P1 원칙 및 핵심 검증 요약

### 🚨 [P0 게이트] MainActivity 0줄 격리 원칙 — 통과 (PASS)
- [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)의 신규 비즈니스/도메인 로직 추가는 **0줄**입니다.
- Git HEAD(`1c8d683`) 대비 누적 diff는 **+156 / -416 (순감소 -260줄)**입니다.
- 이번 변경에서 `MainActivity`는 검색/필터링 시 워커에서 사전 계산된 수량 및 NEW 상태를 유지하도록 `prepared.retainingRecords(matches)`로 바인딩 위임만 수정되었습니다.
- 모든 수량 프로젝션, 갤러리 읽기 메모이제이션, 열람 상태 정책, 지연 안내 스케줄링은 전용 클래스로 분리되었습니다.

### 🔒 [P1 게이트] 데이터 안전성 및 무결성 규칙 — 통과 (PASS)
- **MediaStore / 디스크 불변:** 디스크 미디어 복사/이동/삭제 없음, MediaStore 레코드 수정 없음.
- **원본 휴지통 차단:** 영구 비활성화 유지.
- **메타데이터 무결성:** 핵심 JSON 7종(`album_summary_history`, `discovery_snapshot`, `memory_collections`, `memory_date_notes`, `memory_identity_registry`, `memory_organization_links`, `memory_personalization`)의 SHA256 해시가 변경 전후 100% 동일함을 유지.
- **열람 상태 저장소 분리:** 사용자가 열람한 버전 기록은 독립된 전용 SharedPreferences(`memory_seen_versions.xml`)에만 안전하게 기록되며 미디어 캐시나 원본 스냅샷을 오염시키지 않습니다.

### 🧪 빌드 및 단위 테스트 검증
- `./gradlew.bat testDebugUnitTest --rerun`: **309 tests 전건 PASS (0 failures, 0 errors, 0 skipped)**
  - 신규 9건 회귀 테스트(`DelayedLoadingNoticeTest` 2건, `MemoryBrowserEntryTest` 7건) 모두 정상 통과.
- `git diff --check`: 공백/개행 오류 없음 (**PASS**).
- Debug APK (`894FAD6975C1C62019F78F116C5A59F4AFC7280019277F647F0F4FE17D95BA65`) 빌드 일치 확인.

---

## 2. 주요 구현 및 기술 검증

### ① 발견 장소 카드 수량과 상세 화면 일치 ([`MemoryBrowserMediaProjection`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserMediaProjection.java))
- **이전 문제:** 이전 리뷰에서 지적된 Pre-release 사항(성남 카드 generic 2,107개 vs 상세 2,283개 수량 불일치).
- **해결 방식:**
  - 백그라운드 워커에서 발견 그룹이 있는 장소에 대해 상세 화면과 동일한 [`MemoryMediaResolver.resolve()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaResolver.java)를 호출하여 실제 MediaStore 참조를 산출합니다.
  - 이를 [`MemoryMediaCounts.fromRefs()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaCounts.java#L17)를 통해 사진과 동영상으로 분리하여 표기합니다.
  - **실기기 확인:** 성남 **사진 2,252장 · 동영상 31개**, 수원 **사진 854장 · 동영상 11개**, 송파구 **사진 339장 · 동영상 7개**로 카드와 상세 화면 수량 문구가 100% 일치합니다.
  - **I/O 최적화:** 무관한 순수 해외 레거시 앨범 수백 개를 카드 수량 때문에 불필요하게 쿼리하지 않고 기존 요약본을 유지하며, 단일 화면 준비 과정에서 [`memoize()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserMediaProjection.java#L38)를 통해 갤러리 읽기 중복을 방지했습니다.

### ② 정상 열람 시 NEW 뱃지 해제 및 새 사진 추가 시 재등장 ([`MemorySeenPolicy`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemorySeenPolicy.java), [`MemorySeenStore`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemorySeenStore.java))
- **동작 방식:**
  - 상세 화면 진입 후 정상적으로 미디어가 해석되고 사진을 볼 수 있는 상태([`canAcknowledge`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemorySeenPolicy.java#L27))일 때만 열람 버전을 기록합니다.
  - 조회 실패, 로딩 취소, 빈 목록일 때는 열람 처리하지 않습니다.
  - `snapshotVersion <= seenVersion`인 경우 `NEW` 및 `이번에 +N장` 뱃지가 해제됩니다.
  - 향후 신규 분석으로 해당 장소에 새 사진이 추가되어 더 높은 `snapshotVersion`이 부여되면 `NEW` 뱃지가 자동으로 다시 활성화됩니다.
  - 소스 캐시 전체를 무효화하지 않고, 캐시된 진입 모델에 가벼운 열람 버전만 덧씌우는 구조([`withSeenVersions`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserEntry.java#L24))로 구현되어 탭 재진입 4~5ms 속도를 보존합니다.

### ③ 빠른 로딩 시 불필요한 안내 팝업 유예 ([`DelayedLoadingNotice`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DelayedLoadingNotice.java))
- 송파구(약 300장) 등 비교적 가벼운 장소나 캐시 히트 시에도 로딩 알림 필이 깜빡이던 현상을 개선했습니다.
- **250ms 유예 기간(Grace Period)**을 두어, 250ms 이내에 완료되거나 취소된 요청은 안내가 전혀 뜨지 않도록 처리했습니다.
- 요청 generation 번호 관리를 통해 취소된 이전 요청의 지연 콜백이 뷰를 오염시키지 않도록 방어했습니다.

---

## 3. 발견 사항 및 후속 과제 (Findings)

### ⚠️ [Pre-release] 실기기 새 GPS 사진 촬영을 통한 NEW 재등장 E2E 검증
- **내용:** 단위 테스트(`MemoryBrowserEntryTest:40`)에서는 새 버전 새 사진 시 NEW 재등장이 완벽히 검증되었으나, 실제 실기기 카메라로 GPS가 포함된 새 사진을 촬영한 후 증분 분석을 돌려 열람 해제되었던 장소에 NEW가 정상 복구되는지 1회 확인이 필요합니다.

### 📋 [Follow-up] 첫 탭 Cold 진입 시간 모니터링
- 프로세스 콜드 스타트 후 첫 탭 진입은 디스크 I/O와 MediaStore 전체 검사로 인해 여전히 900~1,000ms 수준이 소요됩니다. 비동기 워커로 격리되어 UI 블로킹은 없으므로 릴리스에 지장은 없으나, 차기 마일스톤에서 점진적 카드 요약 프리페칭을 권장합니다.

---

## 4. 결론

본 309-test 패치는 사용자가 제기했던 **성남 카드-상세 수량 불일치**, **NEW 뱃지 미해제**, **불필요한 로딩 알림 깜빡임** 3가지 핵심 문제를 P0 원칙을 완벽히 지키며 견고하게 해결했습니다.

**코드 반영 승인 (APPROVED for Merge)** 합니다.
실기기에서 GPS 사진 1장 촬영 후 NEW 뱃지 재등장 테스트만 완료되면 출시 준비가 완료됩니다.
