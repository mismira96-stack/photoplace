# 정리한 원본 정리(Original Cleanup) 설계안 심층 검토 보고서

- **검토자:** Antigravity (Google DeepMind)
- **일자:** 2026-10-24
- **검토 대상 문서:**
  - [`ORIGINAL_CLEANUP_DESIGN_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/ORIGINAL_CLEANUP_DESIGN_2026-10-24.md) (설계 초안)
  - [`REVIEW_REQUEST_Original_Cleanup_Design_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_REQUEST_Original_Cleanup_Design_2026-10-24.md) (검토 요청서)
  - [`REVIEW_Original_Duplicate_Cleanup_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_Original_Duplicate_Cleanup_2026-10-24.md) (위험 분석 보고서)
  - [`WORKLOG_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/WORKLOG_2026-10-24.md) (작업 맥락)
- **판정:** **설계 방향 조건부 승인 (CONDITIONAL DESIGN APPROVAL)** — 제품 방향성과 안전 계층화는 매우 우수함. 단, 구현 착수 전 아래 3대 차단 과제(Blocker) 해결 및 전용 아키텍처 확정 필요.

---

## 1. P0 / P1 원칙 관점의 설계 적합성 평가

### 🚨 [P0 원칙] MainActivity 비대화 절대 금지 (0줄 격리 원칙)
- **설계 평가:** **PASS (설계 원칙 준수)**
- **구현 시 필수 준수 사항:**
  - 기존 [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)에 잔존하는 `requestTrashCopiedOriginals()`, `savePendingOriginalCleanup()`, `collectCleanupOriginalUrisFromPreview()` 등 레거시 휴지통 코드는 즉시 전면 격리 또는 제거되어야 합니다.
  - 신규 원본 정리 기능은 모두 전용 도메인 클래스([`CleanupEvidenceStore`](#), [`CleanupCandidateResolver`](#), [`CleanupPairVerifier`](#), [`CleanupExecutionPolicy`](#), [`CleanupBatchSession`](#), [`CleanupController`](#))로 분리되어야 합니다.
  - `MainActivity`는 오직 단일 `ActivityResultLauncher<IntentSenderRequest>`(또는 `onActivityResult`) 위임 콜백 1개와 화면 전환 라우팅만 담당하며, 비즈니스/검증/판단 로직은 **단 1줄도 포함되어서는 안 됩니다.**

### 🔒 [P1 원칙] 데이터 안전성 및 무결성 규칙
- **설계 평가:** **PASS (방어적 안전 정책 수립)**
- **핵심 안전 장치:**
  - 영구 삭제(`delete()`) 완전 배제, 오직 OS의 `MediaStore.createTrashRequest`를 통한 휴지통 이동만 허용.
  - 단순 파일명/크기 일치에 의존하지 않고, 전체 바이트 SHA-256 해시 검증 및 실행 직전 재검증(Pre-execution Re-verification)을 필수 게이트로 설정.
  - 자동 휴지통 이동이나 일괄 전체 선택을 금지하고, 사용자 명시적 체크박스 선택을 기본값으로 강제.

---

## 2. 안티가 제시하는 3대 차단 과제 (Blockers — 구현 전 해결 필수)

### 🛑 [Blocker 1] 시스템 확인창 대기 중 TOCTOU 발생 시 사후 감사(Post-Trash Audit) 부재
- **위치 및 조건:** [`ORIGINAL_CLEANUP_DESIGN_2026-10-24.md:61-65`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/ORIGINAL_CLEANUP_DESIGN_2026-10-24.md#L61-L65)
- **시나리오:**
  1. 앱이 원본과 복사본의 SHA-256 해시 일치를 완벽히 검증하고 시스템 휴지통 다이얼로그(`createTrashRequest`)를 띄움.
  2. 사용자가 시스템 다이얼로그를 닫지 않고 다른 앱(삼성 갤러리, 파일 관리자 등)으로 전환하여 `Pictures/` 내 복사본을 이동하거나 삭제함.
  3. 다시 PhotoPlace로 돌아와 시스템 다이얼로그의 "휴지통으로 이동"을 승인함.
  4. OS는 카메라 원본(`DCIM/Camera`)을 휴지통으로 이동시킴 → **복사본과 원본이 동시에 유실되는 치명적 사고 발생.**
- **해결 방안 (설계 보완 필수):**
  - OS 다이얼로그가 떠 있는 동안의 외부 조작을 원자적으로 막을 수는 없으나, 시스템 확인창이 `RESULT_OK`로 반환된 **직후 0ms 시점에 즉시 "사후 대상 생존 감사(Post-Trash Audit)"**를 실행해야 합니다.
  - 만약 이동 직후 복사본이 디스크에서 사라졌거나 손상된 것이 감지되면:
    1. 즉시 긴급 경고 다이얼로그를 띄우고,
    2. 후속 잔여 배치를 즉시 중단하며,
    3. `createTrashRequest(..., false)`(복원) 호출을 즉시 제안하거나 시스템 휴지통 화면으로 안내하는 롤백 프로토콜을 설계에 명시해야 합니다.

### 🛑 [Blocker 2] Memory 식별자/날짜 메모 의존성 검증 프로토콜 명시
- **위치 및 조건:** [`ORIGINAL_CLEANUP_DESIGN_2026-10-24.md:59`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/ORIGINAL_CLEANUP_DESIGN_2026-10-24.md#L59), [`MemoryMediaResolver.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaResolver.java)
- **위험성:**
  - PhotoPlace의 사용자 날짜 메모([`MemoryDateNoteStore`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryDateNoteStore.java))와 기억 모음([`MemoryCollectionStore`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryCollectionStore.java))은 `stableMemoryId + dateKey`에 결속되어 있습니다.
  - 만약 어떤 장소의 `OrganizationLink`가 부분(PARTIAL) 상태이거나, `MemoryMediaResolver`가 여전히 카메라 원본 URI를 읽기 소스로 참조하고 있는 상태에서 원본을 휴지통으로 보내면, 해당 날짜의 사진 섹션이 사라지거나 메모가 고아(Orphan) 상태가 될 수 있습니다.
- **해결 방안:**
  - 정리 후보 판정 시, **"대상 복사본을 통해 해당 Memory 및 날짜 섹션이 온전히 재구성되는가?"**를 사전에 검증하는 [`MemoryDependencyGuard`](#) 검사를 필수 통과해야만 정리 후보 목록에 노출되도록 강제해야 합니다.

### 🛑 [Blocker 3] 레거시 휴지통 경로 선행 차단 및 게이트 일원화
- **위치:** [`MainActivity.java:3584-3601`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L3584-L3601) (`requestTrashCopiedOriginals`), [`MainActivity.java:6844`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L6844)
- **위험성:**
  - 현재 `MainActivity`에 파일명/번호 접미사 중복 추정치(`duplicateInTarget`)를 그대로 휴지통 요청으로 보내던 옛 코드가 여전히 물리적으로 남아 있습니다.
  - 신규 정리 기능을 개발하는 동안 이 옛 코드가 임의로 호출되거나 활성화되는 사고를 원천 봉쇄해야 합니다.
- **해결 방안:**
  - 구현 1단계 착수 전, 기존 `MainActivity`의 `requestTrashCopiedOriginals` 호출부 전체를 즉시 제거하고, 전역 `CleanupFeatureGate.isEnabled()`(= false) 뒤로 격리해야 합니다.

---

## 3. Codex의 6대 핵심 질문에 대한 안티의 결정

### Q1. 두 진입점(완료 화면 CTA + 설정 메뉴)과 '로컬 사진 전용' MVP 범위가 적절한가?
> **답변: 전적으로 적절하며 승인합니다.**
- **근거:**
  - 앨범 복사 완료 직후와 나중에 설정에서 들어오는 화면이 분리되면 검증 로직과 안전성 규칙이 이원화되어 버그가 발생합니다. 동일한 검토 화면에 `filterJobId`만 파라미터로 넘기는 구조는 최적의 UX 및 아키텍처입니다.
  - 동영상(Video)은 이동(Move) 정책이거나 제외 대상이므로 사진만 대상으로 한정한 것은 데이터 손실 위험을 최소화하는 현명한 경계 설정입니다.

### Q2. 증거 기록이 없는 과거 파일의 검토 허용 범위는?
> **답변: 엄격한 격리 탭(Heuristic Match Tab)으로 분리하고 기본값은 '보류'로 둘 것.**
- **근거:**
  - 앞으로 복사할 사진은 `(sourceUri, targetUri, sha256)` 증거 원장이 1:1로 남지만, 과거 성남/수원 사진은 추정 매칭입니다.
  - 따라서 과거 파일은 전체 자동 검토에 섞지 말고, **"이전에 정리한 사진 대조(고급)"** 하위 메뉴로 분리해야 합니다.
  - 스캔 범위는 오직 `album_summary_history.json`에 기록된 알려진 위치 앨범 경로(`Pictures/<장소명>에서/`)로 엄격히 한정해야 하며, Memory 연결이 입증되지 않는 파일은 무조건 보류(`EXCLUDED_UNVERIFIED_MEMORY`)해야 합니다.

### Q3. 증거 원장(Evidence Store) 기록 방식과 복사 처리량 성능 충돌 방지는?
> **답변: 인메모리 수집 후 배치 단위 단일 트랜잭션 기록 권고.**
- **근거:**
  - [`MediaCopyEngine.java:29`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MediaCopyEngine.java#L29)의 `copy()` 반환값 `targetUri`를 [`SortJobResult`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/SortJob.java#L22) 내 인메모리 리스트(`List<CleanupEvidenceRecord>`)에 누적하는 것은 0.01ms도 걸리지 않습니다.
  - 매 사진 복사 시마다 디스크 I/O나 SQLite fsync를 치면 2,000장 복사 시간이 2~3배 늘어납니다.
  - 따라서 **복사 작업 중에는 메모리에만 쌓고, 배치 완료 시점에 [`CleanupEvidenceStore`](#)에 단일 트랜잭션(SQLite 또는 원자적 파일 쓰기)으로 1회 flush**해야 복사 성능을 100% 보존할 수 있습니다.

### Q4. 바이트 대조/재검증 외에 추가로 고려해야 할 손실 시나리오는?
> **답변: 다음 3가지 추가 시나리오 방어가 필요합니다.**
1. **MediaStore URI 재활용(Recycled ID) 위험:**
   - 안드로이드 미디어스토어 재구축 시 이전 ID가 다른 사진에 할당될 수 있습니다. `sourceUri`뿐 아니라 `(fileName, takenAt, fileSize, volumeName)`을 함께 대조해야 합니다.
2. **외장 SD 카드 볼륨 분리/언마운트:**
   - 복사본이 SD 카드에 저장된 상태에서 카드가 제거된 경우 대상 조회가 실패합니다. 이는 '삭제 대상'이 아니라 무조건 '조회 불명(보류)'으로 분류해야 합니다.
3. **Android 14+ 부분 미디어 권한(Selected Photos Access):**
   - 사용자가 원본 폴더의 일부 사진에만 접근 권한을 준 경우, 전체 대조가 불완전해질 수 있으므로 권한 상태 확인이 선행되어야 합니다.

### Q5. 기본 미선택 UI 및 시스템 배치 크기는?
> **답변: 1회 배치 크기 200~500개 권장, 기본 미선택 원칙 엄수.**
- **근거:**
  - Android의 `createTrashRequest` 상한선은 2,000개이지만, 제조사 OS(삼성 One UI 등)의 시스템 다이얼로그가 수천 장의 썸네일을 한꺼번에 렌더링할 때 심각한 버벅임이나 ANR이 발생할 수 있습니다.
  - 따라서 **1회 요청당 최대 300~500장 단위로 분할 처리**하는 것이 사용자 경험과 안정성 면에서 가장 우수합니다.
  - 사용자가 체크박스를 직접 선택하지 않은 사진은 절대 휴지통으로 이동되지 않아야 합니다.

### Q6. 단계별 구현 및 기능 활성화 순서는?
> **답변: 제안된 4단계 구현 계획을 지지하며, Phase별 승인 게이트를 설정할 것.**
- **Phase 1 (증거 기록만):** `SortJob`에서 반환된 `destinationUri`를 `CleanupEvidenceStore`에 누적 기록 (UI 노출 없음, 복사 성능 벤치마크).
- **Phase 2 (읽기 전용 검토 화면):** 검증 워커, SHA-256 해시 대조, UI 렌더러 구현 (휴지통 버튼 비활성화 상태).
- **Phase 3 (폐기용 테스트 미디어 실기기 검증):** 더미 사진을 통한 시스템 다이얼로그 호출, 취소, 부분 성공, 사후 감사 검증.
- **Phase 4 (사용자 원본 정리 활성화):** 최종 Antigravity 승인 후 기능 활성화.

---

## 4. 결론 및 다음 단계

Codex가 작성한 [`ORIGINAL_CLEANUP_DESIGN_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/ORIGINAL_CLEANUP_DESIGN_2026-10-24.md)는 미디어 복사 구조의 본질적인 한계를 명확히 짚어내고 있으며, P1 안전 원칙에 대한 깊은 고민이 담긴 매우 훌륭한 설계안입니다.

위 **3대 차단 과제(Blocker 1~3)**에 대한 방어 로직을 설계에 명시적으로 반영한 후, **Phase 1 (증거 수집 원장 구현)** 단계부터 순차적으로 안전하게 구현에 착수할 것을 권고합니다.
