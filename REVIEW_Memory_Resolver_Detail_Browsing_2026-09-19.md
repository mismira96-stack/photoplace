# PhotoPlace Memory Resolver Detail Browsing 코드 리뷰

**일자:** 2026-09-19  
**작성자:** Antigravity  
**검토 대상:**
- Commit: `862b3bd Connect memory resolver to detail browsing`
- [`DiscoverySnapshotController.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java)
- [`MemoryBrowserState.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserState.java)
- [`MemoryRepository.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryRepository.java)
- [`TODO.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/TODO.md) 및 [`WORKLOG_2026-09-19.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/WORKLOG_2026-09-19.md)

**최종 판정:**
- **상세 화면 미디어 리졸버 연결:** **최종 승인 (Approved - MainActivity 0줄 격리 유지, 단일 소스 원칙 및 메모 보존 완비)**
- **단위 테스트 보강:** **권장 (Recommendation - 신규 메서드 usableMemoryLink 및 GalleryReader 연동 테스트 추가 권장)**
- **Play 프로덕션 정식 출시:** **보류 유지 (Hold - 내부 사진 뷰어 미구현, 원본 휴지통 이동 차단 유지)**

---

## 1. 아키텍처 및 핵심 원칙 검증

### [P0 원칙 준수] MainActivity 비대화 방지 (0줄 변경)
- **평가: 완벽 준수 (Pass)**
- 커밋 `862b3bd`에서 [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)는 **단 1줄도 변경되지 않았습니다.**
- `MainActivity`는 이미 `discoverySnapshotController().loadBrowserDetail()`을 호출하고 있었으며, 리졸버 및 GalleryReader 주입은 [`DiscoverySnapshotController`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java)와 [`MemoryBrowserState`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserState.java) 내부에서 완결되었습니다.
- 사용자의 "새 기능을 MainActivity에 밀어넣지 않는다"는 원칙을 최우선으로 준수했습니다.

### [단일 소스 원칙] Gallery 우선 및 Discovery 폴백 (No Union Guarantee)
- **평가: 무결함 (Pass)**
- [`MemoryBrowserState.detail()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserState.java#L64-L76)에서 [`MemoryMediaResolver.resolve()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaResolver.java#L16-L45)를 호출하여 반환된 `resolution.refs`만을 단독 소스로 사용합니다.
- usable exact Memory `OrganizationLink`가 있고 Gallery 조회가 `FOUND`이면 Gallery 미디어만을 사용합니다.
- Gallery 조회가 `MISSING`인 경우에만 라이브 Discovery 미디어로 안전하게 폴백합니다.
- 두 소스가 무분별하게 합쳐지거나 중복 노출(Union)되는 위험이 원천 차단되었습니다.

### [데이터 무결성] Stable Memory ID 및 날짜별 메모 보존
- **평가: 무결함 (Pass)**
- [`MemoryRepository.usableMemoryLink()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryRepository.java#L154-L163)는 `stableIdByAlias.get(record.memoryKey)`를 통해 불변 `stableMemoryId`(`mem_<UUID>`)를 역참조한 뒤 최신 usable `OrganizationLink`를 검색합니다.
- 반환된 `resolution.refs`는 [`MemoryPhotoSection.fromDiscoveryRefs()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryPhotoSection.java#L31-L67)를 통해 날짜별 섹션(`dateKey`)으로 분할됩니다.
- [`MainActivity.java:10023-10026`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L10023-L10026)에서 `stableMemoryId + section.dateKey`로 `MemoryDateNoteStore`의 기존 메모를 조회하므로, Gallery 앨범 미디어로 전환된 이후에도 사용자가 작성한 날짜별 메모가 유실되지 않고 100% 동일하게 렌더링됩니다.

---

## 2. 세부 코드 검토 및 개선 권장 사항 (Findings & Test Gaps)

### 1. [Medium / Test Gap] 신규 연결 경로에 대한 단위 테스트 부재
- **현상:**
  - 이번 커밋에서 새로 추가된 `MemoryRepository.usableMemoryLink()` 및 오버로딩된 `MemoryBrowserState.detail(memoryKey, repository, galleryReader)`에 대한 단위 테스트가 추가되지 않았습니다.
  - 기존 테스트들은 `galleryReader = null`인 상태로만 실행되고 있어, 실제 `GalleryReader`가 주입되었을 때의 동작(Gallery 우선 반환, 폴백 분기 등)을 JVM 테스트 레벨에서 자동 검증하지 못하고 있습니다.
- **권장 사항:**
  - `MemoryBrowserStateTest`에 mock/fake `GalleryReader`를 주입하여:
    1. usable `OrganizationLink` + Gallery 미디어 존재 시 `GALLERY_OUTPUT` 미디어가 `detail.photoSections`에 반영되는지 검증.
    2. `OrganizationLink` 부재 또는 Gallery `MISSING` 시 Discovery 미디어로 폴백되는지 검증.
  - `MemoryRepositoryTest`에 `usableMemoryLink()`가 최신 `organizedAtMillis`의 SUCCESS/PARTIAL 링크를 정확히 반환하는지 검증하는 테스트 추가 권장.

### 2. [Low / Design] `DiscoverySnapshotController`의 테스트 주입성 (DI)
- **현상:**
  - [`DiscoverySnapshotController.java:17-24`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java#L17-L24)에서 `galleryReader` 필드는 `private`이며, `Context`를 받는 기본 생성자에서만 초기화됩니다.
  - 테스트용 생성자(`store, clock, liveFilter, identityRegistryStore, organizationLinkStore`)에는 `galleryReader` 매개변수가 없어 테스트 코드에서 fake `GalleryReader`를 주입하기 어렵습니다.
- **권장 사항:**
  - 생성자 오버로딩에 `MemoryMediaResolver.GalleryReader galleryReader`를 추가하거나 package-private setter/생성자를 제공하면 컨트롤러 레벨의 회귀 테스트 작성이 훨씬 수월해집니다.

### 3. [Low / Defensive] `usableMemoryLink`의 Memory ID 직접 전달 방어
- **현상:**
  - [`MemoryRepository.java:158`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryRepository.java#L158)에서 `stableIdByAlias.get(record.memoryKey)`만 조회합니다.
  - 현재는 `record.memoryKey`가 항상 alias(예: `KR|Seoul|Songpa-gu`)이므로 정상 동작하지만, 만약 향후 `record.memoryKey` 자체가 이미 `mem_<UUID>` 형태인 레코드가 전달되면 `stableIdByAlias`에서 조회되지 않을 수 있습니다.
- **권장 사항:**
  ```java
  String stableId = stableIdByAlias.get(record.memoryKey);
  if (stableId == null && record.memoryKey != null && record.memoryKey.startsWith("mem_")) {
      stableId = record.memoryKey;
  }
  ```
  형태의 간단한 방어 코드를 두면 더욱 견고합니다.

---

## 3. 검증 결과 요약

- **`./gradlew.bat testDebugUnitTest`:** **PASS (20 tasks, 2s)**
- **`./gradlew.bat assembleDebug`:** **PASS (31 tasks, 1s)**
- **`git diff --check`:** **PASS (코드 포맷 무결)**

---

## 4. Release Gate 상태 및 후속 과제

| 구분 | 상태 | 설명 |
| :--- | :---: | :--- |
| **Memory Resolver 상세 연결 슬라이스** | **최종 승인 (Final Approved)** | `MainActivity` 0줄 유지, 단일 소스 원칙 및 `stableMemoryId + date` 메모 보존 확인 |
| **Play 프로덕션 정식 출시** | **보류 유지 (Hold)** | 1. **앱 내부 사진 뷰어(Photo Viewer) 미완성:** 현재 썸네일 탭 시 외부 앱 chooser 인텐트 호출 상태.<br>2. **원본 사진 휴지통 이동(Trash) 차단 유지:** 실기기에서 Gallery-only 사진 조회가 완벽히 검증될 때까지 원본 삭제/이동 기능은 계속 비활성화되어야 함. |

### 다음 최우선 구현 과제 (Next P0)
1. **앱 내부 날짜 Photo Viewer 구현:**
   - 썸네일 탭 시 외부 chooser 대신 인앱 사진 뷰어로 전환.
   - resolver가 반환한 해당 stable Memory + 날짜의 전체 미디어를 소스로 활용하여 좌우 스와이프 및 현재 위치/전체 개수 표시.
   - 뒤로가기(Back) 시 상세 화면 스크롤 위치 복귀.
2. **단위 테스트 보강:**
   - `MemoryBrowserStateTest` 및 `MemoryRepositoryTest`에 리졸버 연동 테스트 추가.
