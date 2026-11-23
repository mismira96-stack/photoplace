# Antigravity 설계 리뷰 보고서: 메모리뷰 휴지통 및 위치 없음 수동 장소 지정

- **검토일**: 2026-11-08
- **검토자**: Antigravity (Gemini)
- **검토 대상**:
  - [DESIGN_Memory_Trash_Manual_Place_2026-11-08.md](file:///C:/work/projects/PhotoPlace/DESIGN_Memory_Trash_Manual_Place_2026-11-08.md)
  - [REVIEW_REQUEST_Memory_Trash_Manual_Place_2026-11-08.md](file:///C:/work/projects/PhotoPlace/REVIEW_REQUEST_Memory_Trash_Manual_Place_2026-11-08.md)
- **현재 단계**: 설계 검토 (코드 수정·빌드·설치·기기 데이터 변경 없음)

---

## 1. 최종 판정: **CONDITIONAL (조건부 승인)**

### 판정 이유
1. **제품 방향 및 도메인 경계의 타당성**:
   - 원본 파일의 EXIF/GPS나 물리 폴더를 임의로 변조하지 않고 앱 내부 읽기 전용 투영(Projection)으로 장소를 연결한 뒤, 사용자가 원할 때 기존 위치앨범 생성(`DiscoveryAlbumOrganizer` → `SortWorker`) 흐름으로 이어지게 한 설계는 **PhotoPlace의 P1 원칙(데이터 안전성 및 무결성)**과 정확히 부합합니다.
   - 특히 **일반 휴지통 이동(사용자 선택 파일 숨김)**과 **기존 중복 원본 정리(복사본 보존 증명 필수)**의 목적·가드·컨트롤러를 분리하고, 단일 `MediaMutationCoordinator`로 동시 실행을 차단한 결정이 매우 훌륭합니다.
2. **조건부 승인(Conditional) 사유**:
   - 실제 코드베이스(`MemoryRepository`, `MemoryMediaResolver`, `DiscoverySnapshotMapper`, `DiscoverySnapshotLiveFilter`, `CleanupMemorySnapshotReader`, `MemoryDependencyGuard`, `MemoryAlbumAction`, `MemoryPhotoSection`)와 대조한 결과, **설계 11절의 5대 Blocker 영역에서 구현 전 설계 계약(Contract)을 구체화하지 않으면 데이터 불일치·런타임 예외(`IllegalStateException`)·기능 먹통이 발생하는 지점 5건**이 확인되었습니다.
   - 아래 **Blocker 1~5의 수정 제안을 설계안(또는 Phase A 상세 명세)에 반영하는 조건**으로 Phase A(읽기 전용 기반) 착수를 승인합니다.

---

## 2. P0 / P1 프로젝트 원칙 준수 점검

| 원칙 | 점검 결과 | 근거 및 보완 요구사항 |
| --- | --- | --- |
| **P0: `MainActivity` 0줄 격리 원칙** | **PASS (단, 콜백 위임 계약 명시 필요)** | 설계 7절·8절에서 `MemoryMediaSelectionController`, `ManualPlacePickerController`, `ManualPlaceAssignmentService`, `MemoryTrashController`, `MediaMutationCoordinator`로 책임을 분리함. 단, `MainActivity`의 `onActivityResult` / 생명주기 훅에서도 조건 분기 없이 컨트롤러에 1줄 위임하도록 진입 계약을 고정해야 함. |
| **P1-1: MediaStore / 파일 불변 및 읽기 전용 투영** | **PASS** | 수동 장소 지정 시 EXIF/파일명/폴더를 건드리지 않으며, 물리 파일 변경은 명시적인 앨범 생성(`SortWorker`)과 시스템 휴지통 승인(`createTrashRequest`)에서만 수행함. |
| **P1-2: 레거시 원본 휴지통 차단 유지** | **PASS** | 설계 2절·9절에서 `CleanupTrashDispatcher` 레거시 경로 재활성화 금지를 명시함. |
| **P1-3: 사용자 메모 및 `stableMemoryId + dateKey` 보존** | **보완 필요 (Blocker 1, 5)** | 노트 자동 이동 금지 정책은 타당하나, 현재 코드에서 사진이 0장이 된 기록은 필터링되어 노트에 접근할 수 없는 구조이므로 섹션 생성 규칙 보완이 필수임. |

---

## 3. 핵심 발견 사항 (Blocker / Pre-release / Follow-up)

### 🚨 [Blocker 1] `ManualPlaceStore`(SQLite)와 `MemoryIdentityRegistryStore`(JSON) 간 이중 저장소 불일치 및 `CleanupMemorySnapshotReader` 토큰 누락

- **대상 설계 절**: 4절 (`ManualPlaceRecord`, `원자성과 재시작`), 6절 (7항), 11절 (Blocker 1)
- **실제 코드 근거**:
  - [`MemoryIdentityRegistryStore.java:56-103`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryIdentityRegistryStore.java#L56-L103): `memory_identity_registry.json` 단일 파일을 temp/backup rename으로 갱신하며 SQLite 트랜잭션과 원자적으로 묶일 수 없음.
  - [`MemoryRepository.java:68, 158`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryRepository.java#L68): `stableIdByAlias.get(record.memoryKey)`를 통해 `OrganizationLink`를 조회함.
  - [`MemoryCollectionResolver.java:93`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryCollectionResolver.java#L93): `identityRegistry.findStableId(record.memoryKey)`로 모음집 멤버를 매칭함.
  - [`CleanupMemorySnapshotReader.java:22-24, 98-123, 189-191`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMemorySnapshotReader.java#L22-L24): 고정된 7개 JSON 파일(`NAMES`)만 읽어 SHA-256 `token`을 계산하며, `aliases.optString(group.memoryKey, "")`가 `"mem_"`으로 시작하지 않으면 `complete = false; unverifiedSources.addAll(impacted)`로 차단함.
  - [`HomeRevisionReader.java:33-36`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/HomeRevisionReader.java#L33-L36): 고정된 6개 JSON 파일의 `lastModified`/`length`만 revision 토큰에 반영함.
- **실패 시나리오**:
  1. 새 수동 위치기록(`ManualPlaceRecord`) 생성 시 SQLite에 `(placeId, stableMemoryId)`를 커밋한 직후 앱이 강제 종료되어 `memory_identity_registry.json` outbox 반영이 지연되면, 재시작 직후 `MemoryRepository`·`MemoryCollectionResolver`·`CleanupMemorySnapshotReader`가 `stableId`를 찾지 못해 앨범 연결과 노트·모음집 참조가 끊깁니다.
  2. `HomeRevisionReader`와 `CleanupMemorySnapshotReader`가 신규 SQLite DB의 revision을 포함하지 않으면, 원본 정리(Cleanup) 프리플라이트 검증 도중 수동 장소 연결이 변경되어도 [`CleanupExecutionPreflight.java:58, 76`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupExecutionPreflight.java#L58)의 `reader.isCurrent()`가 이를 감지하지 못하는 TOCTOU 구멍이 발생합니다.
- **구체적 수정 제안**:
  1. **읽기 시점 합성(Merge-on-Read)**: `ManualPlaceRecord`의 `memoryKey`(예: `"manual:" + placeId`)와 `stableMemoryId`(`"mem_" + uuid`)를 SQLite 트랜잭션 내에 1차 권위로 저장하고, alias 스냅샷을 읽는 모든 경로(`DiscoverySnapshotController.repository()`, `MemoryCollectionResolver`, `CleanupMemorySnapshotReader`)가 `memory_identity_registry.json` 맵 위에 `ManualPlaceStore`의 `(memoryKey -> stableMemoryId)`를 **읽기 시점에 합성**하도록 설계하세요. 이렇게 하면 JSON outbox 동기화 전이라도 읽기 원자성이 100% 보장됩니다.
  2. **Revision 토큰 통합**: [`HomeRevisionReader.java:33`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/HomeRevisionReader.java#L33) 및 [`CleanupMemorySnapshotReader.java:98`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMemorySnapshotReader.java#L98)의 해시/토큰 입력에 `ManualPlaceStore`의 커밋 `revision` 번호(또는 직렬화된 스냅샷 해시)를 필수로 포함하세요.

---

### 🚨 [Blocker 2] `MemoryMediaResolver.withNewDiscovery()`의 파일명 시그니처 중복 억제로 인한 수동 지정 사진 증발 및 선택 신원 유실

- **대상 설계 절**: 2절 (`MemoryMediaResolver`, `MemoryPhotoSection`), 3.3절 (`메모리뷰 → 휴지통 이동`), 11절 (Blocker 2)
- **실제 코드 근거**:
  - [`MemoryMediaResolver.java:57-103`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryMediaResolver.java#L57-L103): 갤러리 앨범(`GALLERY_OUTPUT`)이 존재하는 기록의 경우 `withNewDiscovery(galleryRefs, discoveryRefs)`를 호출하며, 이때 `mediaFileKey(ref)`(`ref.mediaKind.name() + ":" + MediaStoreAlbumLookup.fileSignature(ref.displayName)`)가 기존 갤러리 파일과 같으면 **`discoveryRefs` 항목을 건너뛰고 버립니다(`skipKnownFiles = true`)**.
  - [`MemoryAlbumAction.java:20-35`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryAlbumAction.java#L20-L35): `canPrepare(record, resolvedRefs)`는 `resolvedRefs`(`MemoryMediaResolver`의 반환값) 안에 아직 앨범으로 복사되지 않은 `originals` URI가 포함되어 있어야 `"앨범에 추가"` 버튼을 활성화합니다.
  - [`MemoryPhotoSection.java:167-172, 181-193`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryPhotoSection.java#L167-L172): `MemoryPhotoItem`은 `(sourceUri, mediaKind, mimeType, takenAtMillis)`만 보유하며, 파일의 실제 경로(`sourceRelativePath`)나 `mediaStoreId`, 출처 구분(갤러리 앨범 복사본 vs 원본 폴더)을 가지지 않습니다.
- **실패 시나리오**:
  1. 이미 갤러리 위치앨범(`Pictures/경복궁에서/`)에 `IMG_0001.JPG`가 들어 있는 상태에서, 사용자가 위치 없음에 있던 다른 사진(다른 날 찍었거나 다른 기기에서 받은 동명 파일 `IMG_0001.JPG` 또는 `IMG_0001 (1).JPG`)을 ‘경복궁’ 위치기록에 수동 지정합니다.
  2. `MemoryMediaResolver.withNewDiscovery()`가 `fileSignature("IMG_0001 (1).JPG") -> "img_0001.jpg"` 충돌로 판단하여 **방금 수동 지정한 사진을 상세 화면(`MemoryBrowserDetail`)에서 숨겨버리고**, `MemoryAlbumAction.canPrepare()`도 `false`가 되어 **앨범에 영영 추가할 수 없게 됩니다.**
  3. 메모리뷰에서 사진을 선택해 휴지통으로 보낼 때 `MemoryPhotoItem`에 `sourceRelativePath`와 `mediaStoreId`가 없어 설계 3.3절의 출처 요약(`위치앨범에 있는 파일 N개` vs `카메라/다운로드 등 파일 N개`)을 즉시 계산할 수 없습니다.
- **구체적 수정 제안**:
  1. `MemoryMediaResolver.withNewDiscovery()`에서 **수동 장소 지정(`ManualMediaPlaceAssignment`)으로 유입된 항목** 중 아직 해당 타깃 앨범으로의 복사 receipt가 없는 항목은 `fileSignature` 중복 억제(`skipKnownFiles`) 대상에서 제외하세요(또는 `CleanupEvidenceStore`/receipt로 원본-복사본 관계가 확인된 항목만 억제).
  2. `MemoryPhotoItem`에 `mediaStoreId`, `sourceRelativePath`, `displayName`, `isGalleryAlbumCopy` 필드를 확장하여 `MemoryMediaSelectionController`가 뷰어/그리드에서 선택한 정확한 물리 파일을 식별할 수 있게 명시하세요.

---

### 🚨 [Blocker 3] 기존 원본 정리(`CleanupMemorySnapshotReader` / `MemoryDependencyGuard`)와 수동 연결·앨범 Receipt 승계의 투영 불일치

- **대상 설계 절**: 6절 (5, 6, 7항), 11절 (Blocker 3)
- **실제 코드 근거**:
  - [`CleanupMemorySnapshotReader.java:131-134, 180-223`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMemorySnapshotReader.java#L131-L134): `discovery_snapshot.json`의 raw JSON 배열(`rawGroups`, `rawRefs`)과 파싱된 `DiscoverySnapshot`의 크기를 1:1 대조(`if (rawRefs.length() != group.photoRefs.size()) throw new IllegalStateException("Incomplete media references")`)하며, 오직 `discovery_snapshot.json` 내부 그룹만 순회합니다.
  - [`MemoryDependencyGuard.java:111-137`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryDependencyGuard.java#L111-L137): 원본(`sourceUri`) 삭제 전후로 해당 `mem_` 기록의 모든 `originalDates`(`dateKey`)에 대응하는 복사본(`targetUri`)이 남아 있는지 검증합니다.
  - [`DiscoverySnapshotLiveFilter.java:177-179`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotLiveFilter.java#L177-L179): `Pictures/...` 등 이미 정리된 위치앨범 경로(`organizedPaths`)에 있는 파일은 discovery live 목록에서 제외합니다.
  - [`SortJob.java:79-100`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/SortJob.java#L79-L100): 사진 복사 시 `evidence.observe(sourceUri, targetUri, ...)`를 호출하지만, `SortJobResult`에는 `sourceUri -> targetUri` 매핑 구조체가 없으며 동영상 이동(`item.video == true`) 시에는 `evidence.observe()`도 호출하지 않습니다.
- **실패 시나리오**:
  1. 위치 없음 사진 `S`를 수동 위치기록 `M`에 지정 → 위치앨범 만들기 실행(복사본 `T` 생성) → 이후 원본 정리(Cleanup) 실행.
  2. 만약 `CleanupMemorySnapshotReader`가 `discovery_snapshot.json`만 읽고 `ManualPlaceStore`를 읽지 않으면, `S`가 `M`에 속한다는 사실을 몰라 `M`의 날짜 섹션 보존 여부를 검사하지 못합니다. 반대로 `DiscoverySnapshot` 객체에만 수동 항목을 주입하면 [`CleanupMemorySnapshotReader.java:185`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMemorySnapshotReader.java#L185)의 `rawRefs.length() != group.photoRefs.size()` 검사에서 `IllegalStateException("Incomplete media references")`이 발생해 원본 정리가 전면 실패합니다.
  3. 원본 `S`가 원본 정리로 휴지통에 간 뒤 앱이 재검색을 수행할 때, 복사본 `T`는 원본 `S`와 마찬가지로 EXIF GPS가 없습니다. 만약 `M`이 `DISCOVERED_ONLY`로만 읽히거나 `OrganizationLink`가 유실된 경우, 복사본 `T`에 대한 수동 연결 승계(`T -> M`)가 저장되어 있지 않으면 사진이 위치기록에서 사라지거나 위치 없음으로 되돌아갑니다.
- **구체적 수정 제안**:
  1. **Receipt 기반 수동 연결 승계 계약 구체화**: `SortJob` 실행 시 생성되는 `MediaCopyReceipt`(`sourceUri -> targetUri`, 동영상 이동 포함)를 `SortJobResult`에 수집하고, `SortWorker` 완료 시 `ManualPlaceOrganizationBridge`가 수동 지정된 `sourceUri`의 `targetUri`(새 MediaStore 행 신원)를 `ManualPlaceStore`에 승계 연결(또는 `OrganizationLink` + 복사본 `assetKey` 바인딩)하도록 명시하세요.
  2. **`CleanupMemorySnapshotReader` 확장**: `CleanupMemorySnapshotReader`가 `discovery_snapshot.json`의 raw 길이 검증은 raw 파일에 대해서만 수행한 뒤, 그 위에 `ManualPlaceStore`의 활성 연결을 `EffectiveMediaPlaceResolver`로 합성하여 `sourceMemories`, `originalDates`, `dates`를 계산하도록 설계에 명시하세요.

---

### 🚨 [Blocker 4] GPS 없는 사진만 있는 최초 사용자의 Raw 입력 → `DiscoverySnapshotMapper` → `MemoryAlbumAction` → `SortJob` 단절

- **대상 설계 절**: 5절 (`위치 없음의 정확한 범위`), 6절 (3항), 11절 (Blocker 4)
- **실제 코드 근거**:
  - [`DiscoverySnapshotMapper.java:94-101`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotMapper.java#L94-L101): `if (item == null || item.noLocation || item.duplicateInTarget) return null;` 및 `PlaceNamePolicy.LOCATION_NONE.equals(placeKey)` 조건으로 인해 **위치 없음 항목은 `discovery_snapshot.json`에 단 1건도 저장되지 않습니다.**
  - [`MediaAnalysisStore.java:17-50`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MediaAnalysisStore.java#L17-L50) & [`MediaAnalysisEntry.java:19-37`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MediaAnalysisEntry.java#L19-L37): `media_analysis_cache.json`은 캐시 키(`signature`)별로 `STATUS_NO_LOCATION`과 `takenAtMillis`만 저장할 뿐, `DiscoveryPhotoRef`를 구성하는 데 필요한 `mediaStoreId`, `mimeType`, `displayName`, `sourceRelativePath` 전체 필드를 갖고 있지 않습니다.
  - [`MemoryAlbumAction.java:12-16, 28-34`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryAlbumAction.java#L12-L16): `ref.mediaKind == MediaKind.PHOTO`인 항목이 최소 1개 이상 있어야만 `canPrepare()`가 `true`를 반환합니다.
  - [`SortJob.java:64-68`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/SortJob.java#L64-L68): `if (item.noLocation)`이면 복사를 건너뜁니다.
- **실패 시나리오**:
  1. 기기에 GPS 없는 사진만 있는 사용자가 앱을 설치하면 `discovery_snapshot.json`의 `groups`가 비어 있을 뿐 아니라, 위치 없음 사진의 메타데이터 목록 자체가 스냅샷에 남아 있지 않아 메모리뷰의 "위치 없음" 진입점에서 읽어들일 스냅샷 소스가 없습니다.
  2. 사용자가 위치 없음에서 **동영상만** 선택하여 새 위치기록에 연결한 뒤 "위치앨범 만들기"를 시도하면, [`MemoryAlbumAction.java:13`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryAlbumAction.java#L13)이 `MediaKind.PHOTO`만 검사하기 때문에 앨범 생성이 차단됩니다.
- **구체적 수정 제안**:
  1. **Unresolved Media Snapshot / Source 명시**: 스캔(`IncrementalMediaScanner` / `DiscoverySnapshotController`) 시 위치 미해결(`NO_GPS`, `LOOKUP_FAILED`, `PERMISSION_LIMITED`) 항목도 `DiscoveryPhotoRef` 호환 레코드(`unresolved_media_snapshot.json` 또는 SQLite 테이블)로 보존하여 전체 재스캔 없이도 즉시 "위치 없음" 그리드와 수동 장소 지정의 입력으로 사용할 수 있게 Phase A 스키마에 포함하세요.
  2. **투영 시 `noLocation` 해제 및 동영상 단독 정리 정책**: `EffectiveMediaPlaceResolver`가 수동 연결된 항목을 `DiscoveryMemoryGroup`으로 투영할 때 `placeKey`/`placeName`을 채우고, `DiscoveryAlbumOrganizer` → `DiscoveryAlbumItemAdapter` 변환 시 `noLocation = false`가 보장됨을 테스트 조건으로 고정하세요. 또한 동영상만 있는 수동 위치기록에서도 앨범 생성(동영상 이동 옵션)을 허용할지 `MemoryAlbumAction.canPrepare()` 계약을 명확히 정의하세요.

---

### 🚨 [Blocker 5] 사진이 모두 빠진 기록(노트만 남은 기록)의 접근 불가 및 유령화 문제

- **대상 설계 절**: 6절 (8, 9, 10항), 11절 (Blocker 5)
- **실제 코드 근거**:
  - [`DiscoverySnapshotLiveFilter.java:70, 93`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotLiveFilter.java#L70): 그룹 내 유효 사진(`refs`)이 0개가 되면 `filterGroup()`이 `null`을 반환하여 해당 그룹을 아예 제거합니다.
  - [`MemoryRepository.java:169, 191`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryRepository.java#L169): `availableCount == 0`이면 `canOpenPhotos = false`가 됩니다.
  - [`MemoryBrowserState.java:335-344`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryBrowserState.java#L335-L344) & [`MemoryPhotoSection.java:33-72`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryPhotoSection.java#L33-L72): `MemoryPhotoSection.fromDiscoveryRefs(refs)`는 오직 존재하는 사진(`refs`)의 `takenAtMillis`를 기준으로만 날짜 섹션을 생성합니다.
  - [`MemoryCollectionResolver.java:128-139`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryCollectionResolver.java#L128-L139): 모음집 상세 역시 `refs`가 있는 날짜에 대해서만 `noteFor(stableMemoryId, dateKey)`를 붙입니다.
- **실패 시나리오**:
  - 사용자가 특정 위치기록(또는 위치 없음)의 `20260501` 날짜 섹션에 메모를 작성한 뒤, 해당 날짜의 사진을 모두 일반 휴지통으로 보내거나 다른 위치기록으로 수동 변경합니다.
  - 설계 6절 8항은 *"사진이 모두 빠져도 작성된 노트는 원래 기록에서 접근 가능하게 유지한다"*고 했으나, 현재 코드에서는 해당 날짜의 `refs`가 0개가 되는 순간 **날짜 섹션 자체가 생성되지 않아 노트가 화면에서 완전히 사라지며**, 기록 전체의 사진이 0장이 되면 `DiscoverySnapshotLiveFilter`와 `canOpenPhotos = false` 때문에 **기록 카드조차 열 수 없게 됩니다.**
- **구체적 수정 제안**:
  1. `MemoryDateNoteStore`에 유효한 노트가 1개 이상 존재하는 `stableMemoryId`는 사진이 0장이어도 `MemoryRepository` 투영에서 제거하지 않고 `canOpenPhotos = true`(메모 열람 가능)로 유지하세요.
  2. `MemoryPhotoSection` 생성 시 사진으로부터 만들어진 `dateKey` 섹션뿐 아니라, **`MemoryDateNoteStore`에 해당 `stableMemoryId`의 노트가 존재하는 `dateKey`에 대해서도 빈 사진 리스트(`photos = emptyList()`)를 가진 날짜 섹션을 병합 생성**하도록 `MemoryPhotoSection` 및 `MemoryCollectionResolver` 계약을 확장하세요.

---

### ⚠️ [Pre-release 1] Android 버전별 시스템 휴지통 API 계약 (`minSdk 29` vs `API 30+`)

- **대상 설계 절**: 2절 말미, 3.3절, 7절, 11절 (Pre-release 6)
- **실제 코드 근거**:
  - [`app/build.gradle:17`](file:///C:/work/projects/PhotoPlace/app/build.gradle#L17): `minSdk 29`, `targetSdk 36`.
  - [`CleanupMediaAccess.java:28, 35`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMediaAccess.java#L28): `if (Build.VERSION.SDK_INT < 30) throw new IllegalStateException("Android 11 required");`.
  - [`CleanupExecutionController.java:109`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupExecutionController.java#L109): `MediaStore.createTrashRequest(...)` 사용.
- **검토 및 수정 제안**:
  - Android 공식 API 기준 `MediaStore.createTrashRequest`와 `MediaColumns.IS_TRASHED`는 **API 30 (Android 11)**부터 지원됩니다. API 29(Android 10)에는 시스템 휴지통 요청 API가 없으며 `ContentResolver.delete()`는 즉시 영구 삭제입니다.
  - 설계 2절의 *"지원하지 않는 버전에서는 영구 삭제로 대체하지 않고 기능을 비활성화한다"*는 원칙에 따라, **일반 휴지통 이동 기능은 `Build.VERSION.SDK_INT >= 30`에서만 활성화**하고 API 29에서는 메뉴 비활성화(또는 안내 문구) 처리한다고 설계에 명확한 SDK 버전 숫자로 못 박으세요(수동 장소 지정은 API 29에서도 정상 지원 가능).

---

### ⚠️ [Pre-release 2] `MediaMutationCoordinator`의 프로세스 재시작/백그라운드 워커 간 직렬화 범위

- **대상 설계 절**: 3.3절 (6~7항), 7절, 8절, 11절 (Pre-release 7)
- **실제 코드 근거**:
  - [`CleanupExecutionController.java:21, 38-41, 88`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupExecutionController.java#L21): 인메모리 `AtomicBoolean busy`와 `CleanupExecutionJournal.LOCK` / `CleanupExecutionJournal.active(c)`를 사용.
  - [`SortWorker.java:13, 27`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/SortWorker.java#L13): `SortInputStore`와 `SortProgressStore`를 기반으로 별도 `WorkManager` 워커에서 실행됨.
- **검토 및 수정 제안**:
  - `MediaMutationCoordinator`가 단순 인메모리 락만 가지면 앱 프로세스가 백그라운드에서 재생성되었을 때 진행 중인 `SortWorker`(위치앨범 복사/동영상 이동)나 `CleanupAuditWorker`(원본 정리 검증)와 일반 휴지통 작업이 겹칠 수 있습니다.
  - `MediaMutationCoordinator.acquire()`는 인메모리 뮤텍스뿐 아니라 **`CleanupExecutionJournal.active(context)`, `MemoryTrashJournal.active(context)`, `SortProgressStore`(또는 `WorkManager`의 `photoplace_sort_work` 실행 상태)를 디스크/WorkManager 기준으로 교차 확인**하도록 명세하세요.

---

### ⚠️ [Pre-release 3] 좌표 없는 수동 기록(`LABEL_ONLY`)의 국가/해외 분류 및 폴더명 규칙

- **대상 설계 절**: 3.2절 (4항), 4절 (`ManualPlaceRecord`), 10절 (검증표 3행)
- **실제 코드 근거**:
  - [`MemoryRepository.java:344-354`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryRepository.java#L344-L354): `placeIdentityKey()`는 `countryCode`가 비어 있으면 `""`를 반환하여 자동 병합(`findCompatibleOrganizedRecord`)을 수행하지 않음.
  - [`OrganizePlaceService.java:47-54`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/OrganizePlaceService.java#L47-L54): `targetRelativePath(placeName)`는 `Pictures/<장소명>에서/` 형식으로 폴더 경로를 생성함.
- **검토 및 수정 제안**:
  - `MemoryRepository.placeIdentityKey()`가 빈 `countryCode`에 대해 자동 병합을 건너뛰는 현재 동작은 설계 4절의 *"같은 이름만으로 기존 기록과 자동 병합하지 않는다"*는 요구사항과 잘 맞습니다. 다만 기존에 이미 동명의 정리된 앨범이 있을 때 [`MediaStoreAlbumLookup.resolveTargetRelativePath()`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MediaStoreAlbumLookup.java#L27-L37)는 폴더명(`normalizedPlace`)만으로 기존 갤러리 폴더를 재사용하므로, **동명 다른 장소의 수동 기록으로 새 앨범을 만들 때 기존 동명 폴더에 합칠지 사용자에게 확인(또는 별도 폴더/명시적 연결 선택)할지** 정책을 분명히 해야 합니다.

---

### 📌 [Follow-up 1] 지도/장소 검색 Provider 선정과 구현 단계(Phase B) 분리

- **대상 설계 절**: 3.2절, 9절 (Phase B), 11절 (Pre-release 6)
- **실제 코드 근거**:
  - [`app/build.gradle:40-45`](file:///C:/work/projects/PhotoPlace/app/build.gradle#L40-L45) 및 [`AndroidManifest.xml`](file:///C:/work/projects/PhotoPlace/app/src/main/AndroidManifest.xml): 현재 프로젝트에는 Google Maps / Places / Naver / Kakao 등 외부 지도·장소 검색 SDK 의존성이나 API 키 설정이 전혀 없습니다(순수 Android `Geocoder` 역지오코딩만 사용 중).
- **권고 사항**:
  - 외부 지도/검색 SDK 도입은 API 키 발급·과금·쿼터·개인정보 처리방침 변경이 수반되므로, Phase B를 **Phase B-1(기존 위치기록 선택 + 장소 이름 직접 입력 + 앨범 연동)**과 **Phase B-2(`PlaceSearchProvider` 플러그인 및 지도 피커 연동)**로 나누어 Phase B-1을 먼저 완성·검증하는 구성을 강력히 권장합니다.

---

## 4. 리뷰 요청서 항목별 직접 답변 요약

1. **P0 MainActivity 격리 및 P1 파일·노트·불변 ID 보존**:
   - 컨트롤러 분리 설계는 P0에 부합합니다. P1의 불변 ID·노트 보존을 위해 **Blocker 1(Merge-on-Read alias)**과 **Blocker 5(노트 전용 빈 날짜 섹션 유지)**를 반영해야 합니다.
2. **Blocker 5개 반증 결과**:
   - 5개 모두 실제 코드 대조를 통해 구체적인 충돌 지점(`MemoryIdentityRegistryStore`, `MemoryMediaResolver.withNewDiscovery`, `CleanupMemorySnapshotReader.project`, `DiscoverySnapshotMapper.photoRefFrom`, `MemoryPhotoSection.fromDiscoveryRefs`)을 특정했으며, 위 3절에 해결책을 제시했습니다.
3. **일반 휴지통과 중복 원본 정리의 분리 및 동시 실행 차단**:
   - 목적 분리(복사본 증명 불필요 vs 복사본 증명 필수)가 명확하며, `MediaMutationCoordinator`가 디스크 저널(`CleanupExecutionJournal`, `MemoryTrashJournal`, `SortInputStore`)까지 교차 확인하면 안전하게 상호 배제됩니다.
4. **날짜 메모 부동(不動) 정책·노트만 남은 기록 접근성·지정 해제 후 물리 앨범 안내**:
   - 날짜 메모를 자동 이동/병합하지 않는 정책은 데이터 오염을 막는 가장 안전한 설계입니다. 단, 사진이 모두 빠진 뒤에도 노트에 접근하려면 **Blocker 5**의 수정이 필수입니다. 수동 지정 해제 시 갤러리에 이미 생성된 물리 앨범은 그대로 남는다는 안내 역시 P1 원칙에 부합합니다.
5. **직접 이름 입력 대안(`LABEL_ONLY`)의 전체 흐름 지원 여부**:
   - 좌표·국가코드 없이도 `ManualPlaceRecord` 생성 → `MemoryRecord` 투영 → `OrganizePlaceService.planFor()`(`Pictures/<이름>에서/`) → `SortWorker` 앨범 생성까지 막힘없이 이어질 수 있습니다(단, **Blocker 3, 4** 및 **Pre-release 3**의 동명 폴더 매칭 주의사항 반영 필요).

---

## 5. 단계별 구현 진입 및 확인 조건 (Exit Criteria)

| 단계 | 진입/완료 확인 조건 | 아직 불확실한 항목 (결정 필요) |
| --- | --- | --- |
| **Phase A (읽기 전용 기반)** | 1. `UnresolvedMediaStore`(위치 미해결 미디어 소스) 스키마 확정<br>2. `ManualPlaceStore` SQLite 트랜잭션 + `MemoryIdentityRegistryStore` Merge-on-Read 단위 테스트 통과<br>3. `HomeRevisionReader` 및 `CleanupMemorySnapshotReader` 토큰에 수동 저장소 revision 반영 | 위치 없음 소스를 `discovery_snapshot.json` 내부 별도 필드로 둘지 SQLite 테이블로 둘지 결정 |
| **Phase B (수동 장소 지정)** | 1. `MemoryMediaResolver.withNewDiscovery` 동명 파일 증발 방지 단위 테스트<br>2. `SortJob` → `ManualPlaceOrganizationBridge` receipt 승계 후 `CleanupMemorySnapshotReader` / `MemoryDependencyGuard` 통과 테스트<br>3. 사진 0장 + 노트 존재 기록의 상세 진입 및 노트 표시 테스트 | 1. 외부 장소 검색/지도 SDK 제공자 및 API 키 확보 여부<br>2. 동영상만 수동 지정된 기록의 단독 앨범 생성 허용 여부<br>3. 동명 다른 장소의 앨범 생성 시 폴더명 구분 규칙 |
| **Phase C (일반 휴지통)** | 1. `Build.VERSION.SDK_INT >= 30` 게이트 적용<br>2. `MediaMutationCoordinator`의 원본 정리/앨범 작업과의 상호 배제 테스트<br>3. 폐기용 테스트 미디어로 OS 승인/취소/부분 적용/프로세스 종료 복구/외부 복원(Tombstone) 실기기 검증 | Android 11~16(API 30~36) 기기에서 `createTrashRequest` 1회 배치 상한(기존 1,000건 유지 여부) 실기기 확인 |


---

## 6. [V1.1 / V1.2 반영] 최소 변경안 및 기존 코드 재사용·확장·신규 구현 비교표

### 6.1 핵심 호출 경로 3대 검증

1. **`IncrementalMediaScanner` → `IncrementalAnalysisSession` → `DiscoverySnapshotMapper` 미해결 항목 보존 경로**:
   - [`IncrementalMediaScanner.java:92-95`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/IncrementalMediaScanner.java#L92-L95)에서 모든 스캔 대상 미디어(위치 없음 포함)를 `PhotoItem`(`item.noLocation = true`, `item.locationKey = "위치없음"`)으로 만들어 `List<PhotoItem> items`로 반환합니다.
   - [`IncrementalAnalysisSession.java:39-44`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/IncrementalAnalysisSession.java#L39-L44) 및 [`ImageAnalysisCacheSession.java:50-55`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/ImageAnalysisCacheSession.java#L50-L55)는 위치 없음 항목도 `MediaAnalysisEntry.STATUS_NO_LOCATION`으로 `media_analysis_cache.json`에 보존합니다.
   - **누락 지점**: [`DiscoverySnapshotMapper.java:94-101`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotMapper.java#L94-L101)의 `photoRefFrom()`에서 `if (item == null || item.noLocation || item.duplicateInTarget) return null;` 및 `PlaceNamePolicy.LOCATION_NONE.equals(placeKey)`로 버리기 때문에 `discovery_snapshot.json`에서만 빠집니다.
   - **최소 변경안**: **새 전체 스캔을 만들 필요가 전혀 없습니다.** `DiscoverySnapshotMapper.photoRefFrom()`에서 `noLocation` 항목을 버리지 않고 예약된 그룹 키(예: `memoryKey = "discovery:__no_location__"`, `placeKey = "위치없음"`) 또는 `DiscoverySnapshot` 내 `unresolvedRefs`로 함께 보존하면 기존 증분 스캔·캐시·`DiscoverySnapshotMerger.replaceAnalyzedItems()`([`DiscoverySnapshotMerger.java:14-25`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotMerger.java#L14-L25))·`DiscoverySnapshotLiveFilter.filter()`([`DiscoverySnapshotLiveFilter.java:24-38`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotLiveFilter.java#L24-L38))를 그대로 100% 재사용할 수 있습니다!

2. **`PreparedItem` → `PhotoItem` → `SortJob` 수동 지정 항목 전달 경로**:
   - [`OrganizePlaceService.java:17-32`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/OrganizePlaceService.java#L17-L32)는 `record.discoveryGroup.photoRefs`와 `record`의 장소명을 읽어 `Plan`을 만듭니다.
   - [`DiscoveryAlbumOrganizer.java:138-150`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoveryAlbumOrganizer.java#L138-L150)은 `PreparedItem`을 만들 때 `locationKey = firstNonEmpty(ref.locationKey, plan.placeName)`을 넣습니다.
   - [`DiscoveryAlbumItemAdapter.java:29-43`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoveryAlbumItemAdapter.java#L29-L43)은 `PreparedItem`을 `PhotoItem`으로 변환할 때 **35번째 줄에서 이미 `noLocation = false`를 전달**하고 있습니다!
   - 따라서 투영 단계(`EffectiveMediaPlaceResolver`)에서 수동 지정된 `DiscoveryPhotoRef`를 대상 `MemoryRecord`의 `discoveryGroup.photoRefs`에 합쳐주기만 하면(단, `ref.locationKey`가 `"위치없음"`이면 `plan.placeName`으로 치환), `OrganizePlaceService` → `DiscoveryAlbumOrganizer` → `DiscoveryAlbumItemAdapter`(`noLocation = false`) → [`SortJob.java:64`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/SortJob.java#L64)까지 **기존 앨범 생성 파이프라인 코드 변경을 최소화한 채 GPS 없는 사진도 그대로 복사/이동**됩니다.

3. **기존 `Cleanup*` 실행 코드의 재사용 가능 범위 vs 분리 범위**:
   - **그대로 재사용 가능(저수준 어댑터)**: [`CleanupMediaAccess.java:22-65`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMediaAccess.java#L22-L65)의 `key(uri)`(`version:volume:_id`), `trashed(uri)`(`is_trashed == 1`), `metadata(uri)` 신원 확인 로직 및 `MediaStore.createTrashRequest` 인텐트 호출 패턴([`CleanupExecutionController.java:109-110`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupExecutionController.java#L109-L110)).
   - **재사용 금지(원본 정리 전용 도메인)**: [`CleanupPairVerifier`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupPairVerifier.java)(원본-복사본 SHA-256 비교), [`MemoryDependencyGuard`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryDependencyGuard.java)(삭제 후 동일 날짜에 복사본이 남아야 통과하는 가드), [`CleanupExecutionAudit`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupExecutionAudit.java)(복사본 보존 감사 및 `NEEDS_RECOVERY` 원본 복구 플로우).

---

### 6.2 모듈별 기존 클래스/줄 번호 기반 재사용·확장·신규 구현 결정표

| 제안 모듈/책임 | 관련 기존 클래스 및 줄 번호 | 결정 (재사용 / 소폭 확장 / 신규) | 구체적 최소 변경 근거 |
| --- | --- | --- | --- |
| **미해결(위치 없음) 미디어 수집** | [`IncrementalMediaScanner.java:92-95`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/IncrementalMediaScanner.java#L92-L95)<br>[`DiscoverySnapshotMapper.java:94-101`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotMapper.java#L94-L101)<br>[`DiscoverySnapshotMerger.java:14-25`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotMerger.java#L14-L25) | **소폭 확장 (신규 스캐너 불필요)** | `DiscoverySnapshotMapper.photoRefFrom()`에서 `LOCATION_NONE` 항목을 드롭하지 않고 예약 그룹(또는 `unresolvedRefs`)으로 스냅샷에 남기면 증분 스캔·병합·LiveFilter가 모두 재사용됨. |
| **수동 장소·미디어 연결 저장 (`ManualPlaceStore`)** | [`MemoryIdentityRegistryStore.java:56-103`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryIdentityRegistryStore.java#L56-L103)<br>[`HomeRevisionReader.java:33-36`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/HomeRevisionReader.java#L33-L36) | **단일 JSON Store(또는 SQLite 1개) 신규 + 기존 Registry 패턴 재사용** | 프로젝트의 모든 사용자 데이터(`memory_collections.json`, `memory_date_notes.json`, `memory_identity_registry.json`)가 temp+backup rename 원자적 JSON 스토어 패턴을 사용 중이며 `HomeRevisionReader`와 `CleanupMemorySnapshotReader`도 `filesDir` 내 JSON 파일을 해시함. 단일 `manual_place_state.json`(내부에 `places`, `assignments`, `stableIdByManualKey`, `revision`을 한 파일로 원자 저장)으로 두면 별도 SQLite+Outbox 없이 **단일 파일 rename으로 원자성과 스냅샷 해시 통합(`CleanupMemorySnapshotReader` 호환)을 한 번에 해결**할 수 있음! |
| **수동 연결 투영 (`EffectiveMediaPlaceResolver`)** | [`DiscoverySnapshotController.java:170-182`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java#L170-L182)<br>[`MemoryRepository.java:44-86`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryRepository.java#L44-L86)<br>[`MemoryMediaResolver.java:57-95`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryMediaResolver.java#L57-L95) | **신규 순수 함수 클래스 1개 + 기존 Resolver 소폭 확장** | `DiscoverySnapshotLiveFilter.filter()` 직후 `EffectiveMediaPlaceResolver.apply(snapshot, manualState)`를 호출해 `DiscoverySnapshot`의 그룹 간 `DiscoveryPhotoRef` 이동 및 수동 그룹 생성을 수행하면, 하위의 `MemoryRepository`, `MemoryBrowserState`, `OrganizePlaceService`, `CleanupMemorySnapshotReader`가 동일한 투영 결과를 자동으로 공유함. |
| **앨범 생성 및 Receipt 승계 (`ManualPlaceOrganizationBridge`)** | [`OrganizePlaceService.java:17-32`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/OrganizePlaceService.java#L17-L32)<br>[`DiscoveryAlbumItemAdapter.java:29-43`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/DiscoveryAlbumItemAdapter.java#L29-L43)<br>[`SortJob.java:79-100`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/SortJob.java#L79-L100)<br>[`SortWorker.java:52-55`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/SortWorker.java#L52-L55) | **기존 파이프라인 재사용 + `SortJobResult` receipt 확장** | 별도 Bridge 없이도 투영된 `MemoryRecord`를 그대로 `MemoryAlbumAction.prepare()`에 넣으면 `DiscoveryAlbumItemAdapter`가 `noLocation = false`로 변환하여 `SortJob`을 통과함. 단, `SortJob.processItem()`에서 `sourceUri -> receipt.targetUri` 쌍을 기록해 수동 연결을 복사본/이동본 URI로 승계하는 후처리 함수만 추가하면 됨. |
| **노트만 남은 기록 유지** | [`MemoryPhotoSection.java:33-72`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryPhotoSection.java#L33-L72)<br>[`MemoryDateNoteStore.java:28-35`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/MemoryDateNoteStore.java#L28-L35) | **소폭 확장** | `MemoryPhotoSection` 생성 시 해당 `stableMemoryId`의 노트 `dateKey` 목록을 받아 사진이 없는 날짜도 빈 섹션으로 유지. |
| **일반 휴지통 실행 (`MemoryTrashController`)** | [`CleanupMediaAccess.java:22-65`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupMediaAccess.java#L22-L65)<br>[`CleanupExecutionController.java:38-41, 98-111`](file:///C:/work/projects/PhotoPlace/app/src/main/java/com/example/gallerysorter/CleanupExecutionController.java#L38-L41) | **저수준 `CleanupMediaAccess` 재사용 + 경량 전용 Controller/Journal 신규** | `CleanupMediaAccess.key()/trashed()`는 그대로 재사용하고, 복사본 검증(`CleanupPairVerifier`)이 없는 경량 `MemoryTrashController` + `MemoryTrashJournal`로 분리. 동시 실행 방지는 `CleanupExecutionController.active(c)`와 `MemoryTrashController.active(c)` 상호 체크로 간단히 달성 가능. |
