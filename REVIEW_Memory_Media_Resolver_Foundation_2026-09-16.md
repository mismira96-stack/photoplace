# PhotoPlace Memory Media Resolver Foundation 코드 리뷰

**일자:** 2026-09-16  
**작성자:** Antigravity  
**검토 대상:**
- [`REVIEW_REQUEST_Memory_Media_Resolver_Foundation_2026-09-16.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_REQUEST_Memory_Media_Resolver_Foundation_2026-09-16.md)
- Commit: `87178eb Add memory media resolver foundation`
- [`MemoryMediaStoreReader.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaStoreReader.java)
- [`MemoryMediaResolver.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaResolver.java)
- [`MemoryMediaResolution.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaResolution.java)
- [`MemoryMediaResolutionTest.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/test/java/com/example/gallerysorter/MemoryMediaResolutionTest.java)
- [`WORKLOG_2026-09-16.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/WORKLOG_2026-09-16.md)

**최종 판정:**
- **Foundation 슬라이스 (보완 커밋 29829f1 반영 후):** **최종 승인 (Final Approved - 슬래시 버그 해소, 타임스탬프 폴백 및 정렬 완비, MainActivity 0줄 유지)**
- **Play 프로덕션 정식 출시:** **보류 유지 (Hold - UI 미연결, 인앱 뷰어 미구현, 원본 휴지통 이동 차단 유지)**

---

## 1. 심각도별 검토 결과 (Findings by Severity)

### [High / Critical] MediaStore `RELATIVE_PATH` 끝 슬래시 불일치로 인한 조회 실패 (False `MISSING`)

- **관련 코드:**
  - [`MemoryMediaStoreReader.java:89-95`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaStoreReader.java#L89-L95) (`normalizePath`)
  - [`MemoryMediaStoreReader.java:53-61`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaStoreReader.java#L53-L61) (`selection` 및 `selectionArgs`)
  - [`OrganizationLink.java:117-123`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/OrganizationLink.java#L117-L123) (`normalizeRelativePath`)
- **결함 내용:**
  1. [`MemoryMediaStoreReader.java:89-95`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaStoreReader.java#L89-L95)의 `normalizePath()`는 모든 끝 슬래시를 제거합니다.
     ```java
     // MemoryMediaStoreReader.java
     while (path.endsWith("/")) {
         path = path.substring(0, path.length() - 1);
     }
     // 반환값 예: "Pictures/Kurume에서"
     ```
  2. 그러나 Android Scoped Storage(MediaStore)의 `relative_path` 컬럼은 실제 기기([`location_album_media_rows.txt`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/location_album_media_rows.txt))에서 항상 **끝 슬래시를 포함**하여 저장됩니다 (`Pictures/Kurume에서/`).
  3. 또한 [`OrganizationLink.java:117-123`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/OrganizationLink.java#L117-L123) 역시 정규화 시 끝 슬래시를 붙여 저장하도록 설계되어 있습니다 (`path + "/"`).
  4. MediaStore 조회 시 `selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"`로 SQLite 단일 등호 일치를 수행하면:
     - SQLite에서 `'Pictures/Kurume에서/' = 'Pictures/Kurume에서'`는 **`FALSE`**가 됩니다.
     - 결과적으로 실제 앨범에 미디어가 정상 존재함에도 `cursor.moveToNext()`가 0건을 반환하여 `Result.missing()`으로 판정됩니다.
  5. `Result.missing()`이 반환되면 [`MemoryMediaSourcePolicy.choose()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaSourcePolicy.java#L25-L35)에 의해 정리 완료된 Gallery 앨범 대신 **Discovery로 오판 폴백**하게 됩니다.
  6. 현재 단위 테스트([`MemoryMediaResolutionTest.java:44-48`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/test/java/com/example/gallerysorter/MemoryMediaResolutionTest.java#L44-L48))는 `normalizePath`가 슬래시를 자른다는 사실 자체만 검증하여 이 불일치를 잡지 못했습니다.
- **권장 수정 방안 (다음 슬라이스 반영):**
  - Android OEM 및 MediaStore 버전에 따라 끝 슬래시 유무가 달라질 수 있으므로, 쿼리 바인딩 시 슬래시가 있는 경로와 없는 경로를 모두 허용하도록 처리해야 합니다:
    ```java
    String pathWithoutSlash = stripTrailingSlashes(relativePath);
    String pathWithSlash = pathWithoutSlash.isEmpty() ? "" : pathWithoutSlash + "/";
    String selection = "(" + MediaStore.MediaColumns.RELATIVE_PATH + "=? OR "
            + MediaStore.MediaColumns.RELATIVE_PATH + "=?)";
    String[] selectionArgs = new String[]{pathWithSlash, pathWithoutSlash};
    ```

---

### [Low / Improvement] `DATE_TAKEN` 누락 시 `DATE_MODIFIED` 폴백 부재

- **관련 코드:** [`MemoryMediaStoreReader.java:71-72`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaStoreReader.java#L71-L72)
- **내용:**
  - EXIF 메타데이터가 없거나 외부에서 다운로드/수신된 미디어의 경우 MediaStore의 `DATE_TAKEN`이 0이거나 null일 수 있습니다.
  - 현재는 `takenAt`이 0(`DiscoveryPhotoRef.UNKNOWN_TIME`)으로 고정되어, 날짜별 묶음 시 "날짜 없음"으로 빠지게 됩니다.
  - `DateRepairReceiver`처럼 `DATE_MODIFIED`(초 단위 * 1000L) 또는 `DATE_ADDED`를 보조 타임스탬프로 폴백하면 날짜 분류 누락을 최소화할 수 있습니다.

---

### [Low / Improvement] 사진/동영상 순차 조회 결과의 전역 정렬(Global Chronological Sort)

- **관련 코드:** [`MemoryMediaStoreReader.java:38-41`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaStoreReader.java#L38-L41)
- **내용:**
  - `readKind(Images...)` 완료 후 `readKind(Video...)`를 호출하여 `refs` 리스트에 차례대로 추가합니다.
  - 리스트 자체는 `[사진 최신순... , 동영상 최신순...]`의 형태로 구성됩니다.
  - `MemoryPhotoSection.fromDiscoveryRefs()`를 통과할 때는 내부적으로 `sortTime` 재정렬이 발생하므로 화면 표시에는 영향이 없으나, 추후 인앱 사진 뷰어가 `resolution.refs`를 직접 순회할 경우 미디어 종류별로 나뉘어 표시될 수 있으므로 `refs` 취합 후 최종 타임스탬프 정렬을 권장합니다.

---

## 2. 필수 검토 항목별 상세 평가

### 1. 단일 source 원칙
- **평가: 완벽 준수 (Pass)**
- [`MemoryMediaResolver.resolve()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaResolver.java#L16-L45)는 `MemoryMediaSourcePolicy`의 결정을 엄격히 따릅니다.
- Gallery와 Discovery 미디어를 절대 합치지(Union) 않으며, `GALLERY_OUTPUT`, `DISCOVERY`, `UNAVAILABLE`, `EMPTY` 중 정확히 하나의 결과만 불변 객체로 생성합니다.
- MediaStore 조회 실패(`FAILED`)나 불명(`UNKNOWN`) 시 안전하게 `UNAVAILABLE`로 탈출하여 잘못된 Discovery 폴백을 막습니다.

### 2. MediaStore 조회 안전성
- **평가: 기본 구조 우수 (Pass, 상기 슬래시 이슈 제외)**
- Android 10(API 29) 및 11+(API 30+)에 맞춰 `IS_PENDING = 0` 및 `IS_TRASHED = 0` 필터링을 정확히 분기 적용했습니다.
- Null cursor 또는 쿼리 예외 발생 시 `Result.failed()`를 반환하여 안전 차단벽을 형성했습니다.
- ContentUris를 통해 올바른 `Uri`를 생성하고 `MediaKind.PHOTO`와 `MediaKind.VIDEO`를 적절히 구분했습니다.

### 3. Memory Identity 및 메모리 보존 전제
- **평가: 무결함 (Pass)**
- 조회된 Gallery 미디어를 `DiscoveryPhotoRef` 형태로 감쌀 때, Memory의 `placeKey`, `title`, 국가/지역 정보만을 보조 속성으로 주입하며 `MemoryRecord` 자체의 identity(`stableMemoryId`)를 훼손하지 않습니다.
- 날짜별 키(`dateKey`)를 생성할 수 있는 `takenAtMillis`가 그대로 유지되므로, 다음 단계의 `MemoryDateNoteStore`(`stableMemoryId + "#" + dateKey`) 및 Collection 계층 연동에 장애가 없습니다.

### 4. P0 아키텍처 원칙 준수 (MainActivity 격리)
- **평가: 완벽 준수 (Pass)**
- **`MainActivity.java` 변경: 0줄.**
- 모든 조회 어댑터, 불변 DTO, 리졸버 클래스가 별도의 파일로 깔끔하게 모듈화되었습니다. 사용자의 "새 기능 코드를 MainActivity에 밀어넣지 않는다"는 원칙을 100% 만족합니다.

---

## 3. 남은 테스트 공백 (Test Gaps)

1. **`MemoryMediaStoreReader` 쿼리 인자 및 경로 변형 테스트 부재:**
   - 끝 슬래시가 붙은 경로(`Pictures/Songpa/`)와 슬래시가 없는 경로(`Pictures/Songpa`), 윈도우 역슬래시(`Pictures\Songpa\`) 입력 시 실제 MediaStore selection 및 selectionArgs가 어떻게 생성되는지 검증하는 단위 테스트 보강 필요.
2. **`MemoryMediaResolver.resolve()` 인터랙션 테스트 부재:**
   - 현재 [`MemoryMediaResolutionTest.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/test/java/com/example/gallerysorter/MemoryMediaResolutionTest.java)는 결과 불변성과 DTO 팩토리만 테스트하고 있습니다.
   - `GalleryReader` mock/fake를 주입하여 `FOUND`, `MISSING`, `FAILED`, `null link`, `non-memory link` 시 의도한 `MemoryMediaResolution`이 정확히 반환되는지 검증하는 테스트 추가 필요.

---

## 4. Release Gate 및 출시 차단 사유 (Release Gate Status)

| 구분 | 상태 | 상세 사유 |
| :--- | :---: | :--- |
| **이번 Foundation 슬라이스** | **최종 승인 (Final Approved)** | 슬래시 불일치 해소, 타임스탬프 폴백 및 정렬 완비, MainActivity 0줄 변경 확인 |
| **Play 프로덕션 정식 출시** | **보류 유지 (Blocker)** | 1. 아직 UI 및 상세 화면에 Resolver가 연결되지 않은 순수 기반 코드임.<br>2. 같은 장소/날짜 인앱 사진 뷰어가 완성되지 않음.<br>3. **실기기에서 정리된 앨범이 정상 조회되는 것을 확인할 때까지 원본 사진 휴지통 이동(Trash) 기능은 절대 활성화해서는 안 됨.** |

---

## 5. 다음 슬라이스 작업 가이드 (Codex 전달용)

1. **단위 테스트 보강:**
   - `MemoryMediaResolverTest`를 신설하여 `resolve()`의 모든 분기(`FOUND`, `MISSING`, `FAILED`, `UNAVAILABLE`) 검증.
2. **Memory 상세 화면 및 인앱 뷰어 연결:**
   - `MemoryMediaResolver`를 Memory 상세 상태 머신에 연결하되, 여전히 `MainActivity` 비대화 없이 별도 Controller/ViewModel 계층 활용.
3. **원본 삭제/휴지통 차단 유지:**
   - 원본 사진 삭제/휴지통 이동 액션은 실기기 검증 완료 전까지 계속 차단 상태 유지.

---

## 6. 보완 커밋(`29829f1`, `44aaea0`) 최종 확인 결과

1. **`RELATIVE_PATH` 끝 슬래시 쿼리 대응:**
   - [`MemoryMediaStoreReader.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaStoreReader.java)에 `relativePathCandidates()`를 도입하여 `("Pictures/Songpa", "Pictures/Songpa/")`를 모두 `selectionArgs`에 바인딩함으로써 SQLite `=` 비교 시 기기/버전별 슬래시 불일치를 완벽히 해소했습니다.
2. **`DATE_TAKEN` 폴백 로직 구현:**
   - `PROJECTION`에 `DATE_MODIFIED`와 `DATE_ADDED`를 추가하고, `DATE_TAKEN <= 0L` 시 `DATE_MODIFIED * 1000L` → `DATE_ADDED * 1000L`로 안전하게 폴백하도록 구현되었습니다.
3. **사진·동영상 취합 후 전역 최신순 정렬:**
   - 사진과 동영상 컬렉션을 모두 읽은 직후 `Collections.sort()`를 통해 `takenAtMillis` 기준 내림차순(최신순) 정렬을 수행하도록 보완되었습니다.
4. **회귀 테스트 및 빌드 검증:**
   - [`MemoryMediaResolutionTest.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/test/java/com/example/gallerysorter/MemoryMediaResolutionTest.java)에 `relativePathQueryAcceptsBothMediaStoreRepresentations` 테스트가 추가되었으며, 단위 테스트(`testDebugUnitTest`) 및 디버그 빌드(`assembleDebug`) 모두 이상 없이 통과했습니다.
5. **MainActivity 격리 유지:**
   - 보완 커밋에서도 `MainActivity.java`는 전혀 수정되지 않았습니다.
