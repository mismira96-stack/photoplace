# PhotoPlace V2.1 아키텍처 및 Memory/Discovery 정합성 리뷰

- **작성일**: 2026-08-24
- **대상 브랜치**: `codex/photoplace-v2-bg-wip`
- **참조 커밋**: `3c1c9aa`, `20adfa3`
- **참조 문서**: `TODO.md`, `MEDIA_LIFECYCLE_MEMORY_SYNC_DESIGN.md`

---

## Part 1. V2.1 두 통합 기능 아키텍처 리뷰

### 1. 두 통합 기능의 본질적 경계

| 구분 | 1. 발견 기록의 가상 기억 통합 (`MemoryCollection`) | 2. 위치 앨범의 실제 물리 통합 (`LocationAlbumMergeEngine`) |
| :--- | :--- | :--- |
| **적용 영역** | `발견 기록` / `기억` 탭 (앱 내부 논리 뷰) | `위치 앨범` 탭 (실제 기기 Gallery 폴더) |
| **파일/미디어 조작** | **전혀 없음** (0 Byte 이동/복사/수정) | **물리 이동 (Move)** (`relative_path` 일괄 변경) |
| **적용 대상** | 분석된 `MemoryRecord` (발견된 장소들) | **PhotoPlace가 생성한 실제 위치 앨범만** |
| **사용자 목적** | 삿포로/오타루/비에이를 `2026 여름 일본 여행`으로 앱 내에서 묶어보기 | 여러 생성 폴더를 `Pictures/2026 여름 일본 여행/`으로 합쳐 Gallery 정리 |
| **핵심 기술 과제** | 별도 JSON 영속화, 앨범 정리/미디어 삭제 시 보존성 | 사전 계획 저장, 파일명 충돌 해결, 원자적 이동, 복구/롤백 |

---

### 2. 블로커 (Blocker)

1. **물리 이동 시 개인화 데이터(`MemoryPersonalization`) 키 유실 위험**
   - **문제**: 현재 `MemoryPersonalizationKey.forSummary()`는 `path:<relativePath>`를 주 키로 사용합니다. 위치 앨범 통합으로 `Pictures/삿포로에서/`가 `Pictures/2026 여름 일본 여행/`으로 이동되면 `relativePath`가 변경되어 기존에 작성했던 사용자 메모/커스텀 이름이 유실될 수 있습니다.
   - **해결 방안**: 물리 앨범 통합 시 기존 소스 앨범의 `MemoryPersonalization` 엔트리를 타겟 키로 마이그레이션하거나, 메모의 식별자를 물리 경로가 아닌 장소/날짜 기반의 논리 `placeKey`/`memoryKey`에 영구 귀속시키는 브릿지 설계가 필수적입니다.
   - **참조 코드**: `app/src/main/java/com/example/gallerysorter/MemoryPersonalizationKey.java:9-17`

2. **물리 이동 시 동일 파일명 충돌(`_display_name`) 해결 정책 부재**
   - **문제**: 서로 다른 소스 앨범(`삿포로에서`, `오타루에서`)에 동일한 파일명(예: `IMG_0001.jpg`, `20260815_120000.mp4`)이 존재하는 경우, 동일한 `Pictures/2026 여름 일본 여행/`으로 `relative_path`를 변경하면 MediaStore 제약조건 위반 또는 파일 덮어쓰기/오류가 발생합니다.
   - **해결 방안**: `LocationAlbumMergeEngine` 실행 전 타겟 디렉터리 및 소스 항목 간 파일명 충돌을 사전 검사하고, 중복 시 `IMG_0001_1.jpg`와 같이 안전한 고유 접미사를 부여하는 네이밍 리졸버가 계획(`LocationAlbumMergePlan`) 단계에 포함되어야 합니다.

3. **MediaStore 권한 상실(`SecurityException`) 및 Scoped Storage 예외 처리**
   - **문제**: PhotoPlace가 생성한 파일이라도 앱 재설치나 OS 백그라운드 정리 후에는 앱의 소유권(Media Owner)이 해제될 수 있습니다. 이 상태에서 `ContentResolver.update()`로 `relative_path`를 수정하면 `RecoverableSecurityException` (Android 10) 또는 `SecurityException` (Android 11+)이 발생합니다.
   - **해결 방안**: 작업 전 소유권 여부를 확인하고, 필요한 경우 `MediaStore.createWriteRequest()`를 통해 사용자 일괄 승인 다이얼로그를 띄우는 단계가 `Phase 0/3`에 정의되어야 합니다.

4. **부분 실패(Partial Failure) 시 사전 계획(`LocationAlbumMergePlan`) 기반 복구/롤백 구체화**
   - **문제**: 100개 미디어 중 30개 이동 후 배터리 방전/프로세스 킬 등으로 중단되었을 때, 상태 복구 체계가 없으면 파일이 양쪽 폴더에 분산되고 히스토리가 불일치 상태가 됩니다.
   - **해결 방안**: 이동 시작 전 `LocationAlbumMergePlan`을 `temp/bak` 원자 저장으로 파일에 기록하고, 항목별 처리 상태(`PENDING`, `SUCCESS`, `FAILED`)를 실시간 갱신해야 합니다. 앱 재시작 시 미완료 계획이 감지되면 '이어서 완료' 또는 '원래 폴더로 되돌리기(Undo)'를 제공해야 합니다.

---

### 3. 권장 사항 (Recommendation)

1. **가상 기억 묶음(`MemoryCollection`)의 멤버 라이프사이클 정의**
   - 가상 묶음에 포함된 특정 Memory의 사진 일부/전체가 Gallery에서 삭제되거나 외부에서 변경된 경우의 뷰 갱신 정책을 명시해야 합니다 (사진이 0장이 되어도 사용자가 입력한 컬렉션 이름과 메모는 `unavailable` 상태로 보존).
   - 한 Memory가 여러 `MemoryCollection`에 중복 포함될 수 있는지(N:M 관계) 정책을 확정해야 합니다.
2. **가상 기억 묶음 내 '앨범으로 정리' CTA 경계 정의**
   - 사용자가 가상으로 묶어둔 `2026 여름 일본 여행` 상세 화면을 볼 때, 하단에 `앨범으로 정리` 액션이 노출될 경우:
     - (A) 개별 장소 앨범들(`삿포로에서`, `오타루에서`)로 분할 생성되는지,
     - (B) 가상 컬렉션 이름(`Pictures/2026 여름 일본 여행/`)으로 단일 앨범이 생성되는지 사용자에게 혼란이 없도록 다이얼로그 문구와 동작을 명확히 정의해야 합니다.
3. **물리 통합 후 빈 소스 앨범(`itemCount = 0`)의 UI 필터링**
   - 모든 사진/동영상이 이동된 소스 위치 앨범은 디스크 상에서 비어지거나 삭제됩니다.
   - `AlbumSummaryHistoryStore`에 `mergedTo` 상태를 기록하여, `위치 앨범` 탭 목록에서는 즉시 숨겨지고 복구/히스토리 트랙용으로만 유지되도록 처리해야 합니다.
4. **대용량 미디어 처리를 위한 백그라운드 실행 보장**
   - 수천 장의 사진/동영상 경로를 변경하는 작업은 UI 스레드나 일시적인 비동기 태스크 대신 `WorkManager` 또는 알림을 동반한 포그라운드 서비스 형태로 실행하여 OS에 의한 강제 종료를 방지해야 합니다.
5. **점진적 구현 순서(Delivery Order) 엄격 준수**
   - `MEDIA_LIFECYCLE_MEMORY_SYNC_DESIGN.md:73-80`의 5단계 순서는 매우 안전합니다.
   - 파일 변경이 없는 **4단계(가상 MemoryCollection)**를 먼저 출시하여 UI/UX를 검증한 후, 파일 시스템을 변경하는 **5단계(물리 LocationAlbumMerge)**를 분리 구현하는 방향을 강력히 권장합니다.

---

### 4. 보류 항목 (Deferred / On Hold)

1. **드래그 앤 드롭(Drag-and-Drop) 통합 인터랙션 보류**
   - One UI 및 Galaxy Z Fold와 같은 폴더블/대화면 기기에서 드래그 제스처는 오동작 및 스크롤 충돌 가능성이 높습니다.
   - `Long-press -> 다중 선택 모드 -> 하단 액션 바 -> 이름 입력`의 선형 인터랙션을 유지하는 것이 훨씬 안전합니다.
2. **임의의 외부 Gallery / DCIM / Download 폴더 병합 보류**
   - PhotoPlace가 직접 생성하고 관리하는 위치 앨범 이외의 시스템 폴더는 대상에서 제외해야 합니다.
3. **AI/자동 앨범 통합 추천 기능 보류**
   - 국가/도시 단위의 자동 물리 병합 제안은 오분류 시 사용자 갤러리 구조를 훼손하므로, 철저히 사용자 수동 선택 기반으로만 한정합니다.

---

## Part 2. Memory/Discovery 재분석·폴더 변경·앱 재설치 이상 현상 조사

### 1. 가장 가능성 높은 원인 순위

#### 🥇 1위: `DiscoverySnapshotController`의 새 장소 카운트 집계 로직 결함 & `duplicateInTarget`의 스냅샷 제외 (증상 1, 5)
- **원인 1-A (`newPlaceCount == 0` 또는 재분석 시 중복 집계)**:
  - `DiscoverySnapshotController.updateFor()`는 새로 들어온 그룹의 `placeKey`가 기존 스냅샷 전체의 `placeKeys(existing)`에 존재하는지만 단순 비교합니다.
  - 사용자가 기존에 `안성` 장소의 사진을 1장 가지고 있는 상태에서 `안성`의 **새로운 사진을 추가하고 동일 폴더를 재분석**하면, `existingPlaces.contains("안성")`이 `true`가 되어 **`newPlaceCount = 0`**이 됩니다 (새 사진이 추가되었는데도 "새로 발견한 장소 0곳"으로 표시).
  - 반대로 스냅샷 파일이 손상되었거나 `liveFilter`로 인해 비워진 상태에서 재분석하면 기존 장소들이 모두 신규로 잡혀 다시 "새로 발견됨"으로 뜹니다.
- **원인 1-B (`Band` 등 다른 폴더 이동 시 발견 누락)**:
  - `MainActivity.buildPhotoItem()` -> `hasDisplayNameInPath()`는 타겟 앨범 경로(예: `Pictures/안성에서/`)에 동일 파일명이 존재하는지 검사하여 `duplicateInTarget = true`를 설정합니다.
  - 사용자가 갤러리 앨범에 있던 사진을 `Band` 폴더로 복사/이동했을 때 타겟 앨범에 동일 파일명이 남아있으면 `duplicateInTarget = true`가 됩니다.
  - `DiscoverySnapshotMapper.photoRefFrom()`은 `item.duplicateInTarget == true`인 항목을 **스냅샷에서 완전히 제외(`return null`)**하므로, `Band` 폴더를 분석해도 해당 사진이 누락되어 실제 장소 수와 맞지 않게 됩니다.

#### 🥈 2위: 위치 캐시 부재 및 `NoLocationCache` 비활성화로 인한 전체 재분석 (증상 4)
- **원인**:
  - `MainActivity.isNoLocationCacheEnabled()`가 `false`를 반환하도록 하드코딩되어 있습니다.
  - 유효한 위치 분석 결과를 디스크에 보관하는 `MediaAnalysisStore`가 아직 없기 때문에, 분석을 실행할 때마다 매번 모든 사진에 대해 `mediaMetadataReader.readExifData()` (ParcelFileDescriptor open) 및 Geocoder 역지오코딩을 수행하여 속도가 느려집니다.

#### 🥉 3위: `DiscoverySnapshot`의 앱 내부 저장소(`filesDir`) 종속 (증상 3)
- **원인**:
  - `위치 앨범`(`AlbumSummaryHistoryStore`)은 실제 기기 저장소(`Pictures/*에서/`)의 MediaStore 상태를 기반으로 앱 재설치 시에도 `rebuiltFromExistingAlbums: true`로 자동 복구됩니다.
  - 반면 `DiscoverySnapshot`(`DiscoverySnapshotStore`)은 앱 내부 전용 디렉터리(`context.getFilesDir() / discovery_snapshot.json`)에만 저장되므로, 앱 삭제 시 OS에 의해 완전히 제거되어 재분석 전까지 복원되지 않습니다.

#### 4위: `DiscoverySnapshotLiveFilter`의 정적 문자열 규칙(`*에서`) 검사 (증상 2, 5)
- **원인**:
  - `DiscoverySnapshotLiveFilter.isOrganizedLocationPath()`는 `value.startsWith("Pictures/") && folderName.endsWith("에서")`로만 정리 여부를 판단합니다.
  - 외부에서 폴더명이 바뀌었거나, MediaStore ID 매핑이 어긋나면 `liveFilter`가 해당 사진을 라이브 목록에서 제거하여 브라우저에서 기존 기록이 사라진 것처럼 보입니다.

---

### 2. 각 원인의 실제 코드 위치

1. **`DiscoverySnapshotController.java` (L75-L107)**: `updateFor()`에서 `existingPlaces`와 단순 `placeKey` 포함 여부 비교로 인한 `newPlaceCount` 오계산.
2. **`DiscoverySnapshotMapper.java` (L94-L102)**: `photoRefFrom()`에서 `item.duplicateInTarget`일 때 무조건 `null` 반환하여 스냅샷 제외.
3. **`MainActivity.java` (L2643-L2657, L3212-L3256)**: `buildPhotoItem()` 및 `hasDisplayNameInPath()`에서 타겟 폴더의 파일명 중복으로 `duplicateInTarget` 플래그 생성.
4. **`MainActivity.java` (L2788-L2820)**: `isNoLocationCacheEnabled()`에서 `return false;`로 캐시 미동작.
5. **`DiscoverySnapshotLiveFilter.java` (L165-L196)**: `isOrganizedLocationPath()`에서 `endsWith("에서")` 하드코딩 필터링.

---

### 3. 재현 가능한 최소 테스트 시나리오

- **시나리오 1 (`newPlaceCount == 0` 재현)**:
  1. `DCIM/앨범 3`에 `안성` 사진 1장 배치 후 분석 실행 $\rightarrow$ `새로 발견한 장소 1곳` 다이얼로그 확인.
  2. `DCIM/앨범 3`에 `안성`의 새로운 사진 1장 추가.
  3. 동일 폴더 재분석 실행.
  4. **결과**: `placeCount = 1`, `newPlaceCount = 0`이 되어 다이얼로그에 `새로 발견한 장소 0곳`으로 보고됨.
- **시나리오 2 (`duplicateInTarget` 오분류로 인한 발견 누락 재현)**:
  1. `Pictures/삿포로에서/IMG_0001.jpg`가 존재하는 상태에서, `Pictures/Band/` 폴더에 `IMG_0001.jpg`를 복사.
  2. `Band` 폴더를 선택하고 분석 실행.
  3. **결과**: `hasDisplayNameInPath`에 의해 `duplicateInTarget = true`가 되고, `DiscoverySnapshotMapper`에서 제외되어 `발견된 장소 0곳`으로 누락됨.
- **시나리오 3 (재설치 후 발견 기록 증발 재현)**:
  1. 폴더 분석 완료 후 `발견 기록` 탭에서 장소 카드 확인.
  2. 앱 삭제 후 재설치 실행.
  3. **결과**: `위치 앨범`은 MediaStore에서 자동 복원되지만, `발견 기록`은 `discovery_snapshot.json` 삭제로 인해 비어 있음.

---

### 4. 데이터 모델 / Merge 정책 개선점

1. **`DiscoverySnapshotUpdate` 집계 정책 개편**:
   - `newPlaceCount`를 단순 `placeKey` 존재 여부가 아니라, **'이번 분석으로 새 미디어(URI)가 추가된 장소 수'**와 **'완전 신규 장소 수'**, **'신규 추가 사진 수'**로 명확히 분리 정의해야 합니다.
2. **`duplicateInTarget`과 Discovery 스냅샷의 분리**:
   - `duplicateInTarget`은 '갤러리 복사(정리)를 건너뛸지'를 결정하는 용도로만 사용하고, 논리적 '발견 기록(기억)'에서는 유효한 사진으로 수집해야 합니다 (`MEDIA_LIFECYCLE_MEMORY_SYNC_DESIGN.md:16` 원칙).
3. **`LiveFilter`의 휴리스틱 제거**:
   - `endsWith("에서")` 문자열 하드코딩 대신, 실제 `AlbumSummaryHistoryStore`에 기록된 앨범 경로 목록을 주입받아 정리 여부를 판단해야 합니다.

---

### 5. 작업 우선순위 구분

#### [지금 바로 적용 가능한 안전한 패치]
- **`DiscoverySnapshotController.updateFor` 계산 로직 수정**: 기존 스냅샷의 미디어 URI 세트와 incoming 미디어 URI 세트를 대조하여 새로 추가된 사진 수 및 신규 장소 수를 정확히 집계.
- **`DiscoverySnapshotLiveFilter`의 정리 앨범 경로 매칭 개선**: 하드코딩된 `endsWith("에서")` 대신 실제 관리되는 앨범 경로 세트를 매개변수로 전달받아 매칭.
- **`discoverySourceSignature` 식별자 강화**: 첫 번째 URI 기반(`preview:count:firstUri`) 대신 선택된 소스 폴더 경로 목록 및 파일 수/수정일시를 조합하여 스코프 식별성 향상.

#### [`MediaAnalysisStore` 설계 이후로 미룰 작업]
- **미디어 분석 캐시 활성화 (`MediaAnalysisStore`)**: 파일 시그니처 기반의 정합성 검증 테스트가 완료될 때까지 단편적인 `NoLocationCache`를 켜지 않고, `MediaAnalysisStore` 완성 후 일괄 활성화.
- **앱 재설치 후 '기억 다시 구성하기' UX**: 갤러리 앨범을 건드리지 않고 백그라운드에서 빠르게 인덱스만 재구축하는 파이프라인 구축 후 제공.

---

### 6. `MainActivity` 로직 비대화 방지 분리 구조 제안

```text
[Media Scan / Analysis]
      │
      ▼
DiscoveryAnalysisEngine (신규 분리: 스캔, EXIF, 지오코딩, 중복 판정 캡슐화)
      │
      ├─► MediaAnalysisStore (파일 시그니처 기반 캐시: ANALYZED / NO_LOCATION / FAILED)
      │
      └─► DiscoverySnapshotMapper ──► DiscoverySnapshotMerger
                                              │
                                              ▼
                                     DiscoverySnapshotStore (원자적 파일 저장)
                                              │
                                              ▼
                                     DiscoverySnapshotController
                                              │
[Presentation Layer]                          ▼
MainActivity (얇은 UI 위임자)  ◄──── MemoryRepository (통합 논리 뷰)
```

- **`DiscoveryAnalysisEngine` 도입**: `MainActivity`의 `loadSourceImages`, `loadSourceVideos`, `buildPhotoItem`, `hasDisplayNameInPath` 등 1,000줄 이상의 스캔/분석 코드를 데이터 계층 전담 엔진으로 분리합니다.
- **`MainActivity` 책임 최소화**: `MainActivity`는 분석 시작 요청과 완료 이벤트 수신, 프로그레스/다이얼로그 표시만 담당하도록 정리합니다.