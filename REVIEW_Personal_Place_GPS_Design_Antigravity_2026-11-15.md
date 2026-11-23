# PhotoPlace 개인 장소(Personal Place GPS) 설계 리뷰 — Antigravity

- **리뷰 일시**: 2026-11-15 KST
- **대상 설계안**: [DESIGN_Personal_Place_GPS_2026-11-15.md](DESIGN_Personal_Place_GPS_2026-11-15.md)
- **연관 작업 기록**: [WORKLOG_2026-11-15.md](WORKLOG_2026-11-15.md), [PERSONAL_PLACE_PRD_REVIEW_2026-08-10.md](PERSONAL_PLACE_PRD_REVIEW_2026-08-10.md)
- **리뷰 판정**: **조건부 설계 승인 (CONDITIONAL DESIGN APPROVAL — 3가지 데이터/UI 계약 확정 후 구현 진행 가능)**

---

> 리뷰 이후 기록된 표시 방식과 반경·자동 분류 합의는 [설계서의 합의 사항](DESIGN_Personal_Place_GPS_2026-11-15.md#사용자antigravity-합의-확정-사항-2026-11-15-구현-기준)을 참고합니다. 아래 판정과 체크리스트는 리뷰 당시의 기록입니다.

## 0. P0 / P1 / UI_GUIDE 정합성 점검

- **P0 (`MainActivity` 0줄 격리)**: 설계안에 명시된 대로 GPS 원좌표 읽기/캐시(`PersonalPlaceGpsCacheStore`), 거리·반경 매칭(`PersonalPlaceMatcher`), 개인 장소 저장소(`PersonalPlaceStore`), 화면 준비 및 렌더링 클래스를 모두 전용 클래스로 분리하고 `MainActivity.java`에는 단순 진입/콜백 위임만 두어야 합니다.
- **P1 (데이터 무결성 및 원본 보호)**: 기존 `DiscoverySnapshot`, `EffectiveMediaPlaceResolver`(수동 장소 이동), 날짜별 메모(`MemoryDateNoteStore`), 안정 ID(`MemoryIdentityRegistryStore`), 원본 EXIF/갤러리 앨범을 변경하지 않는 **앱 내부 읽기 전용 오버레이 보기** 방향이 P1 원칙에 부합합니다.
- **UI_GUIDE 준수**: 개인 장소 이름 입력·후보 미리보기·장소 제외 확인은 기본 `AlertDialog`나 문자열 `ListView`를 쓰지 않고 [UI_GUIDE.md](UI_GUIDE.md)의 카드/시트/3열 썸네일 격자(`MemorySelectionStyle`, `MemorySelectionTile`)를 재사용해야 합니다.

---

## 1. 리뷰 요청 항목별 상세 검토 (Review Findings)

### ① 겹치는 장소·별도 POI·수동 연결·제외의 우선순위 및 오분류 대처

1. **단일 사진에 대한 판단 우선순위(Precedence Stack) 고정 필요**
   코드 구현 시 아래 1→6순위 계약을 명시적 순수 함수로 고정하는 것을 권장합니다:
   - **1순위 — 기존 수동 장소 이동 (`ManualPlaceStore` / `EffectiveMediaPlaceResolver`)**: 사용자가 성남 밖의 다른 기록으로 직접 옮긴 사진([EffectiveMediaPlaceResolver.java](app/src/main/java/com/example/gallerysorter/EffectiveMediaPlaceResolver.java) 라인 30~43)은 성남의 하위 개인 장소 후보나 자동 매칭 대상에서 즉시 제외.
   - **2순위 — 이미 별도 대형 POI로 분리된 기록**: `PlaceNamePolicy`에 의해 ‘예술의전당’ 등 독립 `memoryKey`로 묶인 사진은 성남의 개인 장소가 흡수하지 않음.
   - **3순위 — 개인 장소 명시적 제외 (`EXCLUDED`)**: 사용자가 해당 개인 장소에서 한 번 뺀 사진은 반경 안에 있어도 동일 규칙 버전에서 다시 자동 편입하지 않음.
   - **4순위 — 개인 장소 명시적 포함 (`INCLUDED` / 기준 사진)**: GPS 유무나 반경과 무관하게 해당 개인 장소에 확정 표시.
   - **5순위 — 단일 후보 자동 매칭 (`AUTO_MATCHED`)**: 상위 기록(성남) 내 개인 장소들 중 **오직 한 곳의 확정 반경(경계 여유 안쪽)**에만 들어갈 때 자동 연결.
   - **6순위 — 보류 (상위 기록 ‘성남’에만 유지)**: 두 개 이상의 개인 장소 반경(또는 경계 버퍼)에 걸치거나 GPS가 없는 사진.

2. **[Pre-implementation] 두 번째 개인 장소 생성 시 기존 `AUTO_MATCHED` 사진 보호**
   - **시나리오**: 사용자가 ‘우리집’을 먼저 만들어 50장이 `AUTO_MATCHED`로 들어간 뒤, 인접한 위치에 ‘단골 카페’를 새로 만들어 반경이 일부 겹친 경우.
   - **위험**: 재계산 시 겹친다는 이유로 기존 ‘우리집’의 `AUTO_MATCHED` 사진이 소리 없이 빠지면 사용자는 사진 유실로 인지합니다.
   - **권장**: 기존에 매칭된 `AUTO_MATCHED` 소속은 유지하되, 새 개인 장소 생성 미리보기에서 *“현재 ‘우리집’에 담긴 사진 N장이 근처에 있어요”*로 표시해 사용자가 원할 때만 새 장소로 가져오게(명시적 `INCLUDED` 전환) 합니다. 신규 촬영 사진부터만 중복 반경 자동 보류 규칙을 적용합니다.

3. **[Pre-implementation] 다중 기준 사진(Anchor) 분산 상한선(`maxAnchorSpreadMeters`) 정의**
   - 기준 사진 여러 장 중 거리 합이 최소인 대표점(Medoid)을 잡을 때, 사용자가 실수로 집 사진과 2km 떨어진 마트 사진을 함께 선택할 수 있습니다.
   - 기준 사진 간 최대 거리 또는 대표점으로부터의 거리가 상한(예: `100m~150m`)을 넘으면 반경을 계산하기 전에 *“서로 멀리 떨어진 사진이 함께 선택되었어요. 같은 장소의 사진만 골라 주세요”* 안내 후 재선택하게 하는 가드를 명시해야 합니다.

---

### ② 성남 상세의 별도 보기로 추가할 때 메모·안정 ID·검색·사진 수·공유 회귀 경로

> **권장 결정**: 첫 버전에서는 **성남을 상위 기록으로 그대로 유지(개인 장소에 담긴 사진도 성남 전체 보기에 계속 포함)**하고, 성남 상세 상단에 **필터 칩/하위 보기(`[전체 1,240] [우리집 85] [회사 42] [+ 장소 만들기]`)**를 두는 비파괴(Non-destructive) 구성을 강력히 권장합니다.

만약 성남 목록에서 개인 장소 사진을 빼버리면 아래 회귀가 발생할 수 있습니다:

1. **날짜 메모(`MemoryDateNoteStore`) 고아화 위험**
   - [DiscoverySnapshotController.java](app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java) 라인 176~177 및 205(`MemoryNoteProjection.retain`)는 `memory-id:mem_...#yyyyMMdd` 키와 해당 날짜의 사진 존재 여부를 기준으로 메모를 투영합니다.
   - 성남에서 사진을 빼면 집에서만 사진을 찍은 날의 메모가 성남 상세에서 사라지거나, [CleanupMemorySnapshotReader.java](app/src/main/java/com/example/gallerysorter/CleanupMemorySnapshotReader.java) 라인 201의 정규식(`memory-id:mem_.+#\d{8}`)과 충돌할 수 있습니다.
   - **권장**: 날짜 메모는 상위 기록(`mem_...`)에 그대로 귀속시키고, 개인 장소 하위 보기를 열었을 때는 해당 개인 장소 사진이 존재하는 날짜(`#yyyyMMdd`)의 메모만 필터링해 함께 보여줍니다.
2. **안정 ID 및 카드 사진 수(`itemCount`) 보존**
   - 성남 상위 기록의 `itemCount`, `coverUri`, 날짜 범위를 그대로 유지해야 발견 탭 카드 수량과 상세 전체 수량이 일치하며, 기존 ‘앨범 만들기’ 대상 수량도 흔들리지 않습니다.
3. **발견 탭 검색(`MemoryBrowserSearch`) 누락 방지**
   - [DiscoverySnapshotController.java](app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java) 라인 150~154의 `MemoryBrowserSearch.filter(...)`는 현재 `MemoryRecord`의 기본 필드만 검사합니다.
   - 사용자가 발견 탭 검색창에 **‘우리집’**이나 **‘회사’**를 입력했을 때 상위 기록(‘성남’)이 검색되거나 ‘성남 · 우리집’으로 바로 진입할 수 있도록, `MemoryBrowserSearch`에 상위 기록별 개인 장소 이름 인덱스를 연결해야 합니다.
4. **스토리 공유(`MemoryStoryComposer`) 기본 캡션 연동**
   - 개인 장소 하위 보기(‘우리집’) 안에서 사진을 선택해 스토리 공유를 누르면, `MemoryStoryCaption`의 기본 장소 문구가 `'성남에서'` 대신 선택된 개인 장소 이름(예: `'우리집'` 또는 `'성남 · 우리집'`)으로 전달되도록 뷰 컨텍스트만 주입하면 기존 1~4장 스토리 파이프라인을 수정 없이 재사용할 수 있습니다.

---

### ③ 원좌표 없는 기존 캐시 유지 + 수천 장 점진 조회 + 신규 사진 자동 갱신 연결

1. **신규 사진 자동 갱신 시 원좌표 무비용 적재 (Zero Extra I/O)**
   - [DiscoveryAutoRefresh.java](app/src/main/java/com/example/gallerysorter/DiscoveryAutoRefresh.java) 라인 109 → [IncrementalMediaScanner.java](app/src/main/java/com/example/gallerysorter/IncrementalMediaScanner.java) 라인 99~100을 보면, 신규/변경 사진은 이미 `reader.read(...)`를 통해 [MediaMetadataReader.java](app/src/main/java/com/example/gallerysorter/MediaMetadataReader.java)가 EXIF/Video 메타데이터에서 원좌표를 읽고 있습니다.
   - 단지 `LocationResult`와 `MediaAnalysisStore`에 원좌표를 남기지 않을 뿐이므로, **개인 장소가 1개 이상 등록된 상태라면 `MediaMetadataReader`가 신규 사진을 읽는 바로 그 시점에 `PersonalPlaceGpsCacheStore`에 `(assetKey -> lat, lng, status)`를 함께 저장**하면 앱 복귀 자동 갱신 시 파일을 다시 열 필요가 전혀 없습니다.
2. **기존 성남 수천 장의 점진적 조회(Lazy Backfill) 경계**
   - 개인 장소가 없는 사용자에게는 백필을 실행하지 않습니다.
   - 사용자가 성남 상세에서 기준 사진을 골라 **‘개인 장소 만들기’를 요청한 시점**에만 해당 `DiscoveryMemoryGroup.photoRefs`만 대상으로 배치 조회를 수행하고, 진행 상태를 표시하며, 중간에 취소/이탈하더라도 이미 읽은 항목은 캐시에 저장해 재진입 시 이어서 읽도록 구성합니다.
3. **[Pre-release 주의] Android 10+ (API 29+) `MediaStore.setRequireOriginal(uri)` 확인**
   - [IncrementalMediaScanner.java](app/src/main/java/com/example/gallerysorter/IncrementalMediaScanner.java) 라인 72의 Cursor `latitude`/`longitude` 컬럼은 Android 10+에서 폐기(Redacted)되어 `0.0`이나 `null`로 내려오는 경우가 많습니다.
   - 또한 [MediaMetadataReader.java](app/src/main/java/com/example/gallerysorter/MediaMetadataReader.java) 라인 27(`openFileDescriptor`)과 라인 36(`openInputStream`)에서는 `Uri`에 `MediaStore.setRequireOriginal(uri)`가 적용되어 있는지 반드시 확인해야 합니다. 개인 장소용 GPS 리더에서는 `Build.VERSION.SDK_INT >= 29` 및 `ACCESS_MEDIA_LOCATION` 권한 보유 시 `MediaStore.setRequireOriginal(uri)`를 경유해 마스킹되지 않은 원본 EXIF 좌표를 읽고, 권한 부족이나 일시 I/O 예외는 `READ_DEFERRED_OR_DENIED`로 분리해 영구 캐시하지 않아야 합니다.

---

### ④ 갤러리 복사·휴지통·원본 정리 오유입 방어 검토

1. **`CleanupMemorySnapshotReader`(원본 정리) 무결성 영향 없음 확인**
   - [CleanupMemorySnapshotReader.java](app/src/main/java/com/example/gallerysorter/CleanupMemorySnapshotReader.java) 라인 22~25 및 232~235는 `discovery_snapshot.json`의 그룹들이 `mem_...` ID와 `OrganizationLink`를 가질 것을 엄격히 요구합니다.
   - 개인 장소를 `EffectiveMediaPlaceResolver`나 `discovery_snapshot.json`에 넣지 않고 별도 `personal_places.json`으로 분리한 설계 덕분에, 원본 정리의 무결성 체크가 깨지거나 의도치 않은 갤러리 앨범 생성/복사로 유입될 위험이 없습니다.
2. **[Pre-implementation] 앨범 생성 및 원본 정리(카메라롤 원본 삭제) 후 개인 장소 멤버십 승계**
   - **시나리오**: 사용자가 성남 기록에서 ‘우리집’을 만들어 사진을 담아둔 뒤, 나중에 성남 기록을 **갤러리 앨범으로 정리하고 원본 정리를 실행해 카메라롤 원본(`sourceUri`)을 휴지통으로 보낸 경우**.
   - **위험**: `personal_places.json`의 명시적 포함/제외 멤버십이 카메라롤 `sourceUri` 문자열만 가리키고 있으면, 원본 정리 후 ‘우리집’에서 사진이 사라질 수 있습니다.
   - **권장**: 개인 장소 멤버십을 평가할 때 `sourceUri`뿐 아니라 성남 앨범으로 복사된 사본(`MediaCopyReceipt` / `CleanupEvidenceStore`의 `targetUri` 또는 `(displayName, takenAtMillis, size)` 시그니처)도 동일 사진으로 매칭해 멤버십이 정리된 앨범 사진으로 자연스럽게 이어지도록 설계해야 합니다.
3. **개인 장소 상세 내 ‘장소에서 빼기’ vs ‘휴지통 이동’ 액션 분리**
   - 개인 장소 하위 보기에서 사진을 선택했을 때 주 액션은 파일 삭제가 아니라 **‘이 장소에서 빼기’(앱 내부 개인 장소 멤버십에서만 제외)**여야 합니다. 실제 파일 휴지통 이동과 버튼/문구가 혼동되지 않도록 분리해야 합니다.

---

## 2. 구현 전 사용자·Codex 확정 체크리스트 (Decision Gate)

구현에 들어가기 전에 아래 3가지를 이 방향대로 고정할지 확인해 주세요:

- [ ] **결정 1 (상위 기록 유지)**: 첫 버전에서 성남 전체 기록은 그대로 유지하고, 성남 상세 내부의 **하위 필터/칩(`[전체] [우리집] [회사]`)**으로 개인 장소를 보여주는 비파괴 방식 채택.
- [ ] **결정 2 (장소 간 중복 소속)**: 같은 상위 기록(성남) 내 개인 장소들 사이에는 **1:1 소속**(한 사진은 ‘우리집’ 또는 ‘회사’ 중 한 곳에만 소속)으로 제한하고, 새 장소 추가 시 기존 자동 분류 사진을 소리 없이 빼앗지 않음.
- [ ] **결정 3 (원본 정리 후 멤버십 유지)**: 향후 성남 앨범 정리 및 원본 정리 후에도 개인 장소 멤버십이 깨지지 않도록 `sourceUri` 외에 파일 식별 시그니처(또는 복사본 URI 매핑)를 함께 보존.
