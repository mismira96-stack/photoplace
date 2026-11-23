# Antigravity 코드 리뷰 보고서 (R1) — 수동 장소/미해결 미디어 Phase A 후속 보완

- **검토일**: 2026-11-08
- **검토자**: Antigravity (Gemini)
- **검토 대상**:
  - 후속 보완 문서: [REVIEW_Memory_Place_PhaseA_Codex_Followup_2026-11-08.md](file:///C:/work/projects/PhotoPlace/REVIEW_Memory_Place_PhaseA_Codex_Followup_2026-11-08.md)
  - 준비 표식: [REVIEW_READY.md](file:///C:/work/projects/PhotoPlace/REVIEW_READY.md), [REVIEW_READY_Memory_Place_PhaseA_2026-11-08.json](file:///C:/work/projects/PhotoPlace/REVIEW_READY_Memory_Place_PhaseA_2026-11-08.json)
  - 스냅샷 매니페스트: [REVIEW_SNAPSHOT_Memory_Place_PhaseA_2026-11-08.json](file:///C:/work/projects/PhotoPlace/REVIEW_SNAPSHOT_Memory_Place_PhaseA_2026-11-08.json) (`08dcff971026e00691ee409a3afa0686a0f913c043c4c9e8dcae5de7bc6f5c2a`)
  - 직전 리뷰 보고서: [REVIEW_Memory_Place_PhaseA_Antigravity_2026-11-08.md](file:///C:/work/projects/PhotoPlace/REVIEW_Memory_Place_PhaseA_Antigravity_2026-11-08.md)
  - 설계서: [DESIGN_Memory_Trash_Manual_Place_2026-11-08.md](file:///C:/work/projects/PhotoPlace/DESIGN_Memory_Trash_Manual_Place_2026-11-08.md) (V1.2)

---

## 1. 최종 판정 (Phase A 범위): **APPROVED (Phase A 기반 승인 — Phase B 진입 가능)**

> **중요 경계**: 본 판정은 **Phase A(미해결 인벤토리 캡처·SQLite 단일 커밋 경계·canonical alias merge-on-read·revision 무효화·수동 지정 존재 시 원본 정리 Fail-Closed 차단)** 범위의 **승인(APPROVED)**입니다. 전체 수동 장소 지정 및 일반 휴지통 이동 기능의 완료나 출시 승인이 아니며, Phase B/C의 Pre-release 게이트가 그대로 유지됩니다.

### 판정 요약
1. **초기 리뷰의 Blocker 2건(`A-1`, `A-2`) 완벽 해소**:
   - **Blocker A-1 해소**: [`UnresolvedMediaRecord.java:25-28`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/UnresolvedMediaRecord.java#L25-L28)에서 물리 신원(`uri`, `volume`, `mediaVersion`, `kind`, `mediaId`) 검증은 엄격히 유지하되, 부가 메타데이터인 `size`, `modified`, `takenAt`은 기존 날짜 정책과 동일하게 `Math.max(0L, ...)`로 정규화하여 1970년 이전 타임스탬프 사진 1장이 전체 미해결 인벤토리를 폐기하던 문제를 해결했습니다. 또한 [`ManualPlacePaths.java`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlacePaths.java)와 [`IncrementalMediaScanner.java:44, 73`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/IncrementalMediaScanner.java#L44-L73)의 `relative_path COLLATE BINARY IN (...)`로 스캔 쿼리와 인벤토리 범위 비교 기준을 정확히 일치시켰습니다.
   - **Blocker A-2 해소**: [`ManualPlaceSnapshot.java:14-50`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceSnapshot.java#L14-L50)이 작은 `identitiesPayload`(`places`, `assignments`만 포함하며 `unresolved`는 빈 객체임을 강제)와 파싱 완료된 불변 `List<UnresolvedMediaRecord> unresolved`를 분리 보유하도록 재설계되었습니다. 또한 [`ManualPlaceStore.java:80-104`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceStore.java#L80-L104) 및 [`ManualPlaceInventory.java:15-42`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceInventory.java#L15-L42)에서 스캔 범위 밖(`outside-scope`) 기존 DB 행과의 `asset_key`/URI 충돌을 SQLite 트랜잭션 내부에서 직접 검증·롤백하도록 수정되었습니다.
   - **성능 및 실측 개선 확인**: API 33 에뮬레이터 3,500건(긴 파일명 포함) SQLite 프로브([followup-sqlite-probe.txt](file:///C:/work/projects/PhotoPlace/.debug/manual-place-2026-11-08/followup-sqlite-probe.txt))에서 저장+전체 읽기 시간이 기존 **3,929ms → 370ms (`writeMs=165`)**로 **약 10.6배 단축**되었습니다.
2. **P0 / P1 게이트 준수**:
   - [`MainActivity.java`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MainActivity.java) SHA-256(`39ec3c85cc6583d8918a92d6363bfa4e737ff46a8c4c1acffcd00d3135ded9a9`) 무변경 유지(P0 PASS).
   - 기존 복사/정리/휴지통 실행 정책(`MediaCopyEngine`, `SortJob`, `CleanupExecutionPolicy` 등) 무변경 및 `CleanupMemorySnapshotReader`의 Fail-Closed 보호 유지(P1 PASS).

---

## 2. R1 보완 사항 상세 검증 결과

| 항목 | 직전 상태 | R1 검증 결과 | 코드 및 테스트 근거 |
| --- | --- | --- | --- |
| **Blocker A-1**: 음수 메타데이터 정규화 및 스캔 경로 정규화 일치 | `size/modified/takenAt < 0` 시 전체 배치 폐기 | **해소 (PASS)** | [`UnresolvedMediaRecord.java:25-28`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/UnresolvedMediaRecord.java#L25-L28), [`ManualPlacePaths.java:10-23`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlacePaths.java#L10-L23), [`IncrementalMediaScanner.java:44, 73`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/IncrementalMediaScanner.java#L44-L73), 단위 테스트 [`ManualPlaceFoundationTest.java:152-163, 209-225`](file:///C:/work/projects/PhotoPlace/app/src/test/java/com/example/gallerysorter/ManualPlaceFoundationTest.java#L152-L163) |
| **Blocker A-2a**: `ManualPlaceSnapshot` 거대 JSON 다중 재파싱 제거 | 3,500건 조회 시 5중 JSON 파싱(3,929ms) | **해소 (PASS)** | [`ManualPlaceSnapshot.java:14-50, 74-91`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceSnapshot.java#L14-L50): `identitiesPayload`와 불변 `List<UnresolvedMediaRecord>` 분리. 3,500건 `save+readMs=370` ([followup-sqlite-probe.txt](file:///C:/work/projects/PhotoPlace/.debug/manual-place-2026-11-08/followup-sqlite-probe.txt)) |
| **Blocker A-2b**: SQLite `reconcileInventory`의 범위 밖(`outside-scope`) `asset_key` 충돌 검증 | `before`가 `identitiesOnly`여서 범위 밖 충돌 미검출 | **해소 (PASS)** | [`ManualPlaceStore.java:80-95`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceStore.java#L80-L95) & [`ManualPlaceInventory.java:36-41`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceInventory.java#L36-L41): 범위 밖 동일 `asset_key` 발견 시 `payload`를 읽어 URI 불일치 시 `IllegalStateException` 발생 및 트랜잭션/revision 롤백 ([`ManualPlaceFoundationProbe.java:64-76`](file:///C:/work/projects/PhotoPlace/verification/cleanup-execution-probe/src/main/java/com/example/gallerysorter/ManualPlaceFoundationProbe.java#L64-L76)) |
| **Follow-up A-1**: `OPEN_READONLY` 커넥션의 `beginTransactionNonExclusive()` | 제안 사항 | **검증 후 종료 (PASS)** | API 33 실측에서 `OPEN_READONLY` 핸들에 `beginTransactionNonExclusive()` 호출 시 `SQLiteReadOnlyDatabaseException` 발생 확인([`ManualPlaceFoundationProbe.java:96-105`](file:///C:/work/projects/PhotoPlace/verification/cleanup-execution-probe/src/main/java/com/example/gallerysorter/ManualPlaceFoundationProbe.java#L96-L105)). `LOCK` 동기화 + 전후 `revision` 재검증([`ManualPlaceStore.java:118, 134-138`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceStore.java#L118-L138)) 유지가 타당함. |

---

## 3. 남은 과제 및 Phase B / Phase C 진입 시 필수 게이트 (Pre-release)

Phase A의 모든 블로커는 해소되었으므로 **Phase B 진입을 승인**합니다. Phase B 및 Phase C 구현 시 아래 Pre-release 요구사항을 반영해 주세요.

1. **[Phase B 필수 — Pre-release A-1] 미해결 인벤토리 Freshness 계약**:
   - 구버전 업그레이드 직후(`manual_places.db` 미생성 또는 인벤토리 미캡처)나 보조 저장 실패 상태와, 실제로 기기에 위치 없는 사진이 0장인 상태를 구분하여 UI가 오안내하지 않도록 해야 합니다.
2. **[Phase B 필수 — Pre-release A-2] 이미 정리된 위치앨범(`Pictures/<장소>에서/`) 내 무좌표 복사본의 ‘위치 없음’ 재유입 차단**:
   - `EffectiveMediaPlaceResolver` 투영 시 이미 정리된 위치앨범 경로(`organizedRelativePaths`)의 미디어와 활성 `ManualMediaPlaceAssignment`가 존재하는 미디어는 ‘위치 없음’ 버킷에서 제외해야 합니다.
3. **[Phase B 필수 — 설계 B2~B5]**:
   - `EffectiveMediaPlaceResolver` 단일 권위 투영(홈/메모리뷰/장소상세/검색/앨범생성/원본정리 일관성)
   - 앨범 복사/이동 완료 시 `MediaCopyReceipt` 기반 복사본 `assetKey` 자동 승계
   - 원본 정리 후 사진이 0장이 된 수동 장소 및 날짜 메모(`MemoryDateNoteStore`)의 `NOTE_ONLY` 보존
   - Phase B 완료 전까지 `CleanupMemorySnapshotReader`의 `manualSnapshot.hasAssignments()` Fail-Closed 가드를 해제하지 말고, Phase B에서 원본 정리 대상 계산이 수동 지정 사진을 완전히 반영한 뒤 연동할 것
4. **[Phase C 필수 — 일반 휴지통 이동]**:
   - `CleanupExecutionPolicy`(원본 정리 전용)와 분리된 `MediaTrashRequestPolicy` 및 `MemorySelectionTrashService` 구현, `createTrashRequest` 취소/일부 실패 시 실제 MediaStore 상태 재조회 기반 갱신
