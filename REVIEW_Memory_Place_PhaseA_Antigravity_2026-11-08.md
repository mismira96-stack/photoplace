# Antigravity 코드 리뷰 보고서 — 수동 장소/미해결 미디어 Phase A

- **검토일**: 2026-11-08
- **검토자**: Antigravity (Gemini)
- **검토 대상**:
  - 요청서: [REVIEW_REQUEST_Memory_Place_PhaseA_2026-11-08.md](file:///C:/work/projects/PhotoPlace/REVIEW_REQUEST_Memory_Place_PhaseA_2026-11-08.md)
  - 준비 표식: [REVIEW_READY_Memory_Place_PhaseA_2026-11-08.json](file:///C:/work/projects/PhotoPlace/REVIEW_READY_Memory_Place_PhaseA_2026-11-08.json)
  - 스냅샷 매니페스트: [REVIEW_SNAPSHOT_Memory_Place_PhaseA_2026-11-08.json](file:///C:/work/projects/PhotoPlace/REVIEW_SNAPSHOT_Memory_Place_PhaseA_2026-11-08.json)
  - 설계서: [DESIGN_Memory_Trash_Manual_Place_2026-11-08.md](file:///C:/work/projects/PhotoPlace/DESIGN_Memory_Trash_Manual_Place_2026-11-08.md) (V1.2)
- **검토 범위**: Phase A 기반 코드 (`ManualPlaceStore`, `ManualPlaceSnapshot`, `ManualPlaceInventory`, `UnresolvedMediaRecord`, `UnresolvedMediaCapture`, `IncrementalMediaScanner`, `MemoryIdentityRegistryStore`, `HomeRevisionReader`, `CleanupMemorySnapshotReader`, 단위 테스트 및 에뮬레이터 프로브)

---

## 1. 최종 판정 (Phase A 범위): **CONDITIONAL (조건부 승인 — Phase B 진입 시 Blocker 2건 즉시 보완)**

> **주의**: 본 판정은 **Phase A(읽기·저장·식별자·revision 기반)에 대한 조건부 승인**이며, 수동 장소 지정 및 일반 휴지통 이동 두 기능의 전체 완료나 출시 승인이 아닙니다.

### 판정 요약
1. **P0 / P1 게이트 및 설계 V1.2 B1 계약 이행 우수**:
   - [`MainActivity.java`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MainActivity.java) SHA-256 해시(`39ec3c85cc6583d8918a92d6363bfa4e737ff46a8c4c1acffcd00d3135ded9a9`)가 기존과 100% 동일하며 비즈니스 로직 침투가 0줄입니다(P0 PASS).
   - [`ManualPlaceStore`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceStore.java)가 단일 SQLite `PRAGMA synchronous=FULL` 트랜잭션을 권위로 삼고, [`MemoryIdentityRegistryStore`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryIdentityRegistryStore.java)가 `manual:` 별칭에 대해 2차 JSON 쓰기 없이 **Merge-on-Read** 및 충돌 시 Fail-Closed(`IllegalStateException`)로 동작합니다.
   - [`CleanupMemorySnapshotReader`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMemorySnapshotReader.java#L132-L133)에서 `manualSnapshot.hasAssignments()`가 참이면 즉시 원본 정리 투영을 중단(Fail-Closed)하여, Phase B의 전체 투영이 완성되기 전에 수동 지정 사진이 잘못 정리되는 경로를 원천 차단했습니다.
2. **조건부 승인(Conditional) 사유**:
   - 아래 **Blocker A-1(단 1장의 음수 타임스탬프 사진이 전체 미해결 인벤토리 수집을 폐기하는 문제)**과 **Blocker A-2(`ManualPlaceSnapshot`의 거대 JSON 다중 재파싱 병목 및 `reconcileInventory`의 `before` 스냅샷 누락)** 2건은 Phase B에서 실제 사용자 기기 데이터를 다룰 때 인벤토리 유실 및 심각한 지연/OOM을 유발하므로 Phase B 착수 시 반드시 함께 수정해야 합니다.

---

## 2. 요청서 7대 중점 검토 항목 점검 결과

| 검토 항목 | 결과 | 코드 근거 및 평가 |
| --- | --- | --- |
| **1. SQLite version 0 초기화 롤백 vs 손상 DB 구분 및 동시 변경 감지** | **PASS** | [`ManualPlaceStore.java:122-132`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceStore.java#L122-L132): `version == 0`일 때 `sqlite_master`에 테이블이 있으면 손상/비버전 DB로 판단해 예외를 던지고, 테이블이 없을 때만 최초 롤백 빈 파일로 안전 처리함. [`CleanupMemorySnapshotReader.java:124-125`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMemorySnapshotReader.java#L124-L125)에서 생성 전후 revision 불일치 시 `RevisionChanged` 발생. |
| **2. 단일 행 대용량 payload 회피 및 identity 읽기 시 inventory 파싱 격리** | **부분 보완 필요 (Blocker A-2)** | SQLite 테이블(`manual_state` vs `manual_inventory`)과 `readIdentities()` 경로는 완벽히 분리되어 `CursorWindow` 한계를 피함. 단, 전체 읽기(`read(false)`) 시 `ManualPlaceSnapshot` 인메모리 표현이 다시 모든 행을 단일 JSON 문자열(`payload`)로 합쳐 4~5회 재파싱하므로 개선 필요. |
| **3. 선택 폴더 범위 교체의 타 폴더 보존 및 assignment 보존** | **보완 필요 (Blocker A-2)** | SQLite `DELETE FROM manual_inventory WHERE path=?` 덕분에 타 폴더 행과 `manual_state`의 `assignments`는 보존됨. 그러나 `ManualPlaceStore.reconcileInventory()`가 `before`를 `identitiesOnly=true`로 읽어 `ManualPlaceInventory.reconcile()`에 넘기므로, 타 폴더에 동일 `asset_key`가 다른 URI로 존재하는지 인메모리 `reconcile()`이 검사하지 못함. |
| **4. canonical alias 충돌 차단 및 legacy JSON 우회 방지** | **PASS** | [`MemoryIdentityRegistryStore.java:66-119`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryIdentityRegistryStore.java#L66-L119): `manual:` 접두사는 JSON에 새로 쓰거나 다른 ID로 등록할 수 없으며, [`ManualPlaceSnapshot.java:61-62`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceSnapshot.java#L61-L62)에서 기존 JSON과 ID가 다르면 즉시 `IllegalStateException("Manual alias conflict")` 발생. |
| **5. 정상 스캔 성공과 inventory 저장 실패 분리 + Freshness 계약** | **보완 필요 (Blocker A-1, Pre-release A-1)** | `UnresolvedMediaCapture.finish()`가 예외를 삼켜 정상 스캔을 보호하는 점은 좋으나, 단 1개의 음수 타임스탬프 행 때문에 전체 배치가 폐기되는 문제(Blocker A-1)와 미확보 상태를 UI가 알 수 없는 문제(Pre-release A-1)를 해결해야 함. |
| **6. Cleanup의 임시 차단 Fail-Closed 여부** | **PASS** | [`CleanupMemorySnapshotReader.java:97, 124-126, 132-133`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMemorySnapshotReader.java#L132-L133): `readIdentities()` 실패 시 예외 중단, `hasAssignments()`가 true이면 명시적 메시지와 함께 중단. |
| **7. `MainActivity` 및 기존 복사/정리/휴지통 실행 정책 무변경** | **PASS** | `MainActivity.java`, `MediaCopyEngine.java`, `SortJob.java`, `CleanupExecutionPolicy.java` 모두 해시 일치 및 무변경 확인. |

---

## 3. 상세 발견 사항 (Blocker / Pre-release / Follow-up)

### 🚨 [Blocker A-1] 단 1장의 음수 타임스탬프(`takenAt < 0`) 미디어가 전체 미해결 인벤토리 수집을 통째로 폐기함

- **대상 설계 절**: 5절 (`위치 없음의 정확한 범위`), 12절 (B4 `미해결 입력`)
- **실제 코드 근거**:
  - [`UnresolvedMediaRecord.java:13-21`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/UnresolvedMediaRecord.java#L13-L21):
    ```java
    || size < 0 || modified < 0 || takenAt < 0)
        throw new IllegalArgumentException("Incomplete unresolved media identity");
    ```
  - [`UnresolvedMediaCapture.java:21-38`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/UnresolvedMediaCapture.java#L21-L38):
    ```java
    rows.add(new UnresolvedMediaRecord(..., item.takenAt == null ? 0 : item.takenAt.getTime()));
    } catch (Exception error) {
        incomplete = true;
        rows.clear();
        Log.w("ManualPlaceInventory", "Incomplete inventory identity; prior snapshot retained", error);
    }
    ```
  - **기존 코드와의 대조**: [`DiscoverySnapshotMapper.java:183`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotMapper.java#L183), [`MemoryPhotoSection.java:191`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryPhotoSection.java#L191), [`MediaAnalysisEntry.java:30`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MediaAnalysisEntry.java#L30)은 모두 `Math.max(0L, takenAtMillis)`로 음수 타임스탬프를 `0L`로 정규화합니다.
- **실패 시나리오**:
  - 사용자 기기의 카카오톡 수신 사진, 다운로드 이미지, 오래된 스캔본 등 위치 없는 사진 중에는 EXIF 날짜가 `1970-01-01` 이전이거나 타임존 오프셋으로 인해 `item.takenAt.getTime() < 0`(예: `1969-12-31` → 음수 epoch millis)인 파일, 또는 MediaStore에서 `_size`나 `date_modified`가 음수로 읽히는 행이 흔히 존재합니다.
  - 현재 구현에서는 5,000장의 위치 없음 사진 중 **단 1장이라도 `takenAt < 0`(또는 `modified < 0`, `size < 0`)이면 `UnresolvedMediaRecord` 생성자가 예외를 던지고, `UnresolvedMediaCapture.observe()`가 `incomplete = true; rows.clear()`를 실행해 나머지 4,999장의 정상 미해결 사진까지 전부 버립니다.**
- **구체적 수정 제안**:
  1. `size`, `modified`, `takenAt`은 물리 파일 신원(`volume`, `mediaVersion`, `kind`, `mediaId`, `uri`)이 아니라 부가 메타데이터이므로, `UnresolvedMediaCapture.observe()` 또는 `UnresolvedMediaRecord` 생성자에서 `Math.max(0L, size)`, `Math.max(0L, modified)`, `Math.max(0L, takenAt)`으로 정규화하세요.
  2. 또한 `IncrementalMediaScanner.java:73`의 SQLite `relative_path IN (?)` 매칭과 `ManualPlaceInventory.reconcile()`의 Java `Set.contains(row.relativePath)` 비교 사이에 대소문자/슬래시 정규화 차이로 전체 배치가 롤백되지 않도록, `scannedPaths`와 `row.relativePath`를 동일한 정규화 헬퍼(또는 스캔 쿼리에 사용된 경로 기준)로 맞추고 단위 테스트에 `takenAt < 0` 케이스를 추가하세요.

---

### 🚨 [Blocker A-2] `ManualPlaceSnapshot`의 거대 JSON 다중 재파싱 병목(3,500건 3.9초 소요 원인) 및 `reconcileInventory`의 `before` 스냅샷 불일치

- **대상 설계 절**: 12절 (`Phase A 구현 경계`), 요청서 검토 항목 2·3번
- **실제 코드 근거**:
  - [`ManualPlaceStore.java:69, 76, 83`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceStore.java#L69-L83):
    - 69줄에서 `before = snapshot(cursor, true)`로 읽어오므로 `before`의 `"unresolved"`는 항상 빈 객체(`{}`)입니다.
    - 따라서 76줄의 `ManualPlaceInventory.reconcile(before, paths, rows)`는 기존 DB에 저장된 타 폴더의 `manual_inventory` 행들을 전혀 모른 채 `incoming`만 넣은 JSON 문자열을 직렬화·파싱하고, 83줄에서 `next.identitiesPayload()`로 그 `"unresolved"`를 다시 버립니다.
  - [`ManualPlaceStore.java:98-104`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceStore.java#L98-L104) & [`ManualPlaceSnapshot.java:20-30, 74-84`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceSnapshot.java#L20-L30):
    - `store.read()` 호출 시 `manual_inventory`의 모든 행을 `JSONObject`로 파싱해 `root`에 넣은 뒤 `root.toString()`으로 수 메가바이트짜리 단일 JSON 문자열을 만듭니다.
    - 이어서 `new ManualPlaceSnapshot(revision, root.toString())` 생성자가 호출되면서:
      1. `new JSONObject(payload)`로 전체 JSON 재파싱 (2회차)
      2. `root.toString()` 재직렬화
      3. `mergedAliases()` 내부 `writableCopy()`에서 전체 JSON 재파싱 (3회차)
      4. 생성자 말미 `unresolved()` 검증 내부 `writableCopy()`에서 전체 JSON 재파싱 및 전 행 객체 생성 후 폐기 (4회차)
      5. 호출자가 `snapshot.unresolved()`를 호출할 때 `writableCopy()`에서 전체 JSON 재파싱 및 전 행 객체 재생성 (5회차)
- **실패 시나리오**:
  1. 에뮬레이터 프로브에서 3,500건 저장+조회에 **3,929ms(약 4초)**가 소요된 주원인이 바로 이 5중 전체 JSON 직렬화/재파싱입니다. 실기기에서 위치 없는 사진(카톡/스크린샷/다운로드)이 10,000~25,000장일 경우 `store.read()` 1회에 수십 MB의 임시 문자열과 `JSONObject` 트리가 5번씩 할당되어 **심각한 UI 지연 및 `OutOfMemoryError`**를 일으킬 수 있습니다.
  2. `ManualPlaceStore.reconcileInventory()`에서 `before`가 기존 `manual_inventory`를 포함하지 않으므로, 스캔 범위 밖 폴더에 동일 `asset_key`가 상이한 URI로 존재하는 비정상 상태를 `ManualPlaceInventory.reconcile()`(`29-32줄`)이 검출하지 못합니다.
- **구체적 수정 제안**:
  1. `ManualPlaceSnapshot`에서 `unresolved` 컬렉션을 JSON 문자열(`payload`) 내부에 합쳐 두지 말고, **작은 `identitiesPayload` 문자열(`places`, `assignments`만 포함)과 파싱이 끝난 불변 `List<UnresolvedMediaRecord> unresolved`(또는 `Map<String, UnresolvedMediaRecord>`) 필드로 분리**하세요.
  2. 이렇게 분리하면 `writableCopy()`, `mergedAliases()`, `hasAssignments()`는 항상 수 KB 이하의 작은 `identitiesPayload`만 다루고, `manual_inventory`의 각 행 JSON은 DB 읽기 시 딱 1번만 파싱되어 메모리 사용량과 조회 시간이 1/5 이하로 줄어듭니다.
  3. `ManualPlaceStore.reconcileInventory()`에서도 SQLite가 직접 `DELETE FROM manual_inventory WHERE path=?`를 수행한다면, `ManualPlaceInventory.reconcile()`에는 전체 JSON 재직렬화 없이 `incoming` 범위/중복 검증과 `revision + 1` 계산만 수행하도록 역할을 명확히 일치시키세요.

---

### ⚠️ [Pre-release A-1] 구버전 업그레이드 직후 및 보조 저장 실패 시 ‘미해결 인벤토리 미확보(Stale/Uncaptured)’ 상태 식별 계약 필요

- **대상 설계 절**: 5절, 12절 (B4), 요청서 검토 항목 5번
- **실제 코드 근거**:
  - [`ManualPlaceStore.java:42, 45`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceStore.java#L42): DB 파일이 없으면 `ManualPlaceSnapshot.empty()`(`revision = 0`, `unresolved = []`) 반환.
  - [`UnresolvedMediaCapture.java:40-48`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/UnresolvedMediaCapture.java#L40-L48): `incomplete == true`이거나 `reconcileInventory` 예외 발생 시 로그만 남기고 조용히 종료.
- **실패 시나리오**:
  - 기존 1.3.20 사용자가 앱을 업데이트한 직후에는 `discovery_snapshot.json`은 존재하지만 `manual_places.db`는 아직 생성되지 않았습니다(`revision == 0`). 또한 스캔 중 디스크 일시 오류로 `reconcileInventory`가 실패한 경우에도 `unresolved`는 비어 있거나 이전 상태에 머뭅니다.
  - Phase B에서 메모리뷰의 ‘위치 없음’ 진입점을 만들 때 `snapshot.unresolved().isEmpty()`만 보고 `"위치 없는 사진이 없어요"`라고 표시하면, 실제로는 기기에 위치 없는 사진이 수천 장 있는데도 아직 인벤토리 캡처가 안 된 상태를 사진 0장으로 오안내하게 됩니다.
- **구체적 수정 제안**:
  - Phase B 진입 시 `manual_state`에 **인벤토리 캡처 완료 여부(예: `inventoryCaptured = true` 및 마지막 캡처 스캔 시각/상태)**를 함께 기록하거나, `revision == 0`(또는 마지막 캡처 실패 플래그)일 때 메모리뷰가 `"위치 없는 사진을 정리하려면 사진 다시 찾기가 필요해요"` 안내 및 증분 스캔 연동을 제공하도록 Freshness 계약을 추가하세요.

---

### ⚠️ [Pre-release A-2] Phase B 투영 시 이미 정리된 위치앨범(`Pictures/<장소>에서/`) 내 무좌표 복사본의 ‘위치 없음’ 재유입 방지

- **대상 설계 절**: 6절 (1, 6항), 12절 (B3, B4)
- **실제 코드 근거**:
  - [`IncrementalMediaScanner.java:98-102`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/IncrementalMediaScanner.java#L98-L102): 스캔된 모든 `item.noLocation == true` 항목을 `unresolved.observe(...)`에 전달함.
- **실패 시나리오**:
  - 위치 없음 사진을 수동 장소에 지정하고 위치앨범(`Pictures/경복궁에서/`)으로 복사하더라도 원본과 복사본 파일의 EXIF에는 GPS가 없습니다. 이후 스캔 범위에 해당 폴더가 포함되면 복사본도 `item.noLocation == true`이므로 `manual_inventory`에 들어올 수 있습니다.
- **구체적 수정 제안**:
  - Phase B의 `EffectiveMediaPlaceResolver` 구현 시, `manual_inventory`에서 ‘위치 없음’ 목록을 구성할 때 **(1) 이미 정리된 위치앨범 경로(`DiscoverySnapshotLiveFilter.organizedRelativePaths`)에 있는 파일**과 **(2) 활성 `ManualMediaPlaceAssignment`(원본 및 receipt 승계된 복사본 `assetKey`)가 존재하는 파일**을 반드시 필터링하여 제외하세요.

---

### 📌 [Follow-up A-1] `ManualPlaceStore.read(db, false)`의 SQLite 읽기 트랜잭션 래핑

- **대상 파일 및 줄 번호**: [`ManualPlaceStore.java:91-112`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ManualPlaceStore.java#L91-L112)
- **제안**: 현재 `manual_state` 조회 전후로 `revision`을 두 번 비교하여 동시 쓰기를 감지하고 있는데, `read(db, false)` 전체를 `db.beginTransactionNonExclusive()` ~ `db.endTransaction()`으로 감싸면 다중 프로세스/커넥션 환경에서도 SQLite 스냅샷 격리가 깔끔하게 보장됩니다.

---

## 4. Phase B 진입 확인 조건 (Exit Criteria for Phase A → Phase B)

1. **[필수]** `UnresolvedMediaRecord` / `UnresolvedMediaCapture`에서 음수 `takenAt` / `modified` / `size`를 `Math.max(0L, ...)`로 정규화하고, 음수 타임스탬프 행이 포함되어도 나머지 미해결 인벤토리가 정상 저장됨을 단위 테스트로 검증할 것 (**Blocker A-1**).
2. **[필수]** `ManualPlaceSnapshot`이 `unresolved` 목록을 거대 JSON 문자열 재파싱 없이 불변 컬렉션으로 보유하도록 개선하여 대량 인벤토리(3,500건+) 조회 시 불필요한 5중 JSON 파싱을 제거할 것 (**Blocker A-2**).
3. **[필수]** Phase B의 `EffectiveMediaPlaceResolver` 설계/구현 시 인벤토리 미캡처(`revision == 0` 또는 저장 실패) 상태 구분(**Pre-release A-1**)과 정리된 앨범 경로 제외(**Pre-release A-2**)를 포함할 것.
