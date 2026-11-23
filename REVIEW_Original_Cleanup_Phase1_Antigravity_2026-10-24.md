# 원본 정리 Phase 1 (증거 원장 구축 및 복사 연결) 코드 리뷰 보고서

- **검토자:** Antigravity (Google DeepMind)
- **일자:** 2026-10-24
- **검토 대상:**
  - [`REVIEW_REQUEST_Original_Cleanup_Phase1_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_REQUEST_Original_Cleanup_Phase1_2026-10-24.md)
  - [`REVIEW_Original_Cleanup_Phase1_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_Original_Cleanup_Phase1_2026-10-24.md)
  - [`CleanupEvidenceRecord.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupEvidenceRecord.java)
  - [`CleanupEvidenceSession.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupEvidenceSession.java)
  - [`CleanupEvidenceStore.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupEvidenceStore.java)
  - [`MediaCopyReceipt.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MediaCopyReceipt.java)
  - [`MediaCopyEngine.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MediaCopyEngine.java)
  - [`SortJob.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/SortJob.java)
  - [`SortJobResult.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/SortJobResult.java)
  - [`SortResultStore.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/SortResultStore.java)
  - [`MemoryCardCountPresentation.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryCardCountPresentation.java)
  - [`MemoryCardCountRenderer.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryCardCountRenderer.java)
  - [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)
- **판정:** **코드 반영 승인 (APPROVED for Merge) — Phase 1 완결 확인 및 Phase 2(해시 검증·Memory 가드) 착수 승인**

---

## 1. P0 / P1 원칙 및 핵심 검증 요약

### 🚨 [P0 원칙] MainActivity 비대화 절대 금지 (0줄 격리 원칙) — 통과 (PASS)
- 이번 Phase 1 패치에서 [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)의 신규 코드 추가 및 수정은 **0줄**입니다.
- Phase 0 완료 시점의 해시값(`72BA13D89DA96748BE7CFE170F4C344BAE82F2FF204767834D93D939BC7036C9`)과 100% 동일함을 확인했습니다.
- 증거 기록 세션 및 SQLite 원장은 전적으로 [`SortJob.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/SortJob.java) 및 백그라운드 Worker 경로에서만 격리되어 실행됩니다.

### 🔒 [P1 원칙] 데이터 안전성 및 무결성 규칙 — 통과 (PASS)
- **증거 실패와 사진 복사 성공의 완전한 격리:**
  - [`SortJob.processItem()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/SortJob.java#L87)에서 복사 `try/catch` 블록 바깥에서만 `evidence.observe()`를 호출합니다.
  - 디스크 풀 또는 DB 잠금 등 원장 쓰기 실패가 발생하더라도 `CleanupEvidenceSession`이 즉시 `disabled = true`로 전환되어 복사 루프는 중단 없이 정상 완료되며, Worker 재시도(Retry)로 이어지지 않습니다.
- **정리 자격 부여 원천 차단 (UNVERIFIED 보장):**
  - 모든 증거 행의 상태는 `COPY_OBSERVED_UNVERIFIED`로 하드코딩되어 있으며, `content_sha256`은 무조건 `NULL`로 기록됩니다. 원장에 존재한다는 사실만으로 삭제/휴지통 자격이 발생하지 않습니다.
  - [`CleanupFeatureGate.isEnabled()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupFeatureGate.java#L7)는 여전히 `false`를 유지하고 있습니다.
- **불변 식별자 및 중복 복사 방지 로직 보존:**
  - `duplicateInTarget`, `hasDisplayNameInPath`, `duplicateFileSignature` 알고리즘이 전혀 변경되지 않았으며, 사진 정리 복사본 중복 방지 기능이 완벽히 유지됩니다.

### 🧪 자동 빌드 및 단위 테스트 검증
- `./gradlew.bat testDebugUnitTest testReleaseUnitTest`: **Debug 346개 / Release 346개 전건 통과 (0 failures, 0 errors, 0 skipped)**.
- `CleanupEvidenceRecordTest` (2 tests): 길이 프레이밍 기반 `recordId` 무충돌 검증, 동일 URI/클라우드/비디오/음수 바이트 차단 검증.
- `CleanupEvidenceSessionTest` (7 tests): 50건 체크포인트, 5초 경과 체크포인트, 디스크 장애 시 버퍼 유계 및 무재시도 격리, finally 플러시, 프로세스 강제 종료 시 미커밋 테일 누락 보존 검증.
- `MemoryCardCountPresentationTest` (4 tests): 사진/동영상 단위 보존, 예전 합계 fallback `사진·동영상 X개 (이전 기록)` 표기 검증.
- `git diff --check`: 포맷/공백 오류 없음 (**PASS**).

---

## 2. 7대 핵심 검토 항목 분석

| 검토 항목 | 분석 및 검증 결과 | 판정 |
|---|---|---|
| **1. 실제 Target URI 기록 및 불순 데이터 차단** | `MediaCopyEngine.copy()`가 반환한 `MediaCopyReceipt.targetUri`를 기록하며, 실패/건너뜀/동영상(`item.video`)은 즉시 반환되어 원장에 들어가지 않음. 정규식 패턴 검사로 미디어스토어 로컬 이미지 외의 입력은 철저히 거부됨. | **PASS** |
| **2. 체크포인트 주기 및 장애 격리** | 50건 또는 5초(항목 처리 간격) 주기 체크포인트 적용. `SortJob`의 `finally`에서 남은 버퍼 플러시 및 세션 닫기를 보장함. 저장 실패 시 `disabled=true`로 추가 버퍼 증가를 차단함. | **PASS** |
| **3. SQLite 트랜잭션 및 스키마 건전성** | `PRAGMA synchronous=FULL`, `UNIQUE(job_id, source_uri, target_uri)`, 트랜잭션 일괄 커밋 적용. `onUpgrade` 시 파괴적 테이블 삭제를 금지하고 예외 발생으로 보호. Keyset pagination(`sequence > ?`) 지원. | **PASS** |
| **4. 증거 오류의 복사/Worker 영향 차단** | `evidence.observe`가 복사 `try/catch` 외부에 위치하며, 모든 예외를 세션 내부에서 흡수하여 `SortJobResult.failedCount` 증가나 Worker 재시도를 유발하지 않음. `CopyEngine` 인터페이스 도입으로 테스트 격리 완료. | **PASS** |
| **5. 미검증 상태 및 해시 미부여 보장** | `state='COPY_OBSERVED_UNVERIFIED'` 및 `content_sha256=NULL` 확인. `recordId`의 SHA-256은 키 중복 방지용 길이 프레이밍 해시일 뿐 파일 내용 해시가 아님을 확인. | **PASS** |
| **6. MainActivity 0줄 및 기존 경로 불변** | `MainActivity.java` 파일 해시 변경 0, 신규 로직 0줄. 복사 중복 방지, 검색 캐시, 사용자 메모 저장소 불변 확인. | **PASS** |
| **7. 발견기록 수량 명확화** | `MemoryCardCountPresentation`을 통해 `사진 996장 · 동영상 12개`로 글자 및 단위를 명확히 노출하고, ImageSpan을 제거하여 렌더링 안정성 확보. 미구분 합계는 `사진·동영상 121개 (이전 기록)`으로 정직하게 표현. | **PASS** |

---

## 3. 차기 단계(Phase 2: 대상 검증 및 의존성 가드) 진행 승인

Phase 1 (증거 원장 구축)이 설계 규격 및 안전 수칙을 완벽히 준수하여 구현되었으므로, **Phase 2 (해시 검증·대상 실존 감사·MemoryDependencyGuard)** 단계로의 진입을 승인합니다.

### 📋 Phase 2 작업 시 준수 지침
1. **내용 해시(Content SHA-256) 비동기 계산:**
   - 복사 과정 중 해시를 계산하지 않은 원칙을 유지하며, 별도 백그라운드 검증 세션에서 원본과 복사본의 전체 바이트를 읽어 SHA-256 일치 여부를 검증하고 원장의 `content_sha256`을 갱신할 것.
2. **사후 대상 생존 감사 (Post-cleanup Audit 대비):**
   - 대상 파일이 디스크에 실제 온전히 존재하는지(`file.exists() && length == copiedBytes`) 검사할 것.
3. **MemoryDependencyGuard (불변 식별자 보존):**
   - 원본 정리 대상 사진이 현재 날짜별 사용자 메모(`MemoryDateNote`)나 모음(`MemoryCollection`)의 커버/참조로 사용 중인 경우, 이동된 대상 복사본으로의 투영(Projection) 연결이 안전하게 확보되었는지 세트 단위로 검증할 것.
4. **기능 활성화 게이트 유지:**
   - Phase 2 완료 및 사용자 수동 승인 전까지 `CleanupFeatureGate.isEnabled()`는 상시 `false`를 유지할 것.
