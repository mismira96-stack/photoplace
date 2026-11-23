# MIXED 위치 앨범 추가 및 상세 진행 표시 코드 리뷰 보고서

- **검토자:** Antigravity (Google DeepMind)
- **일자:** 2026-10-24
- **검토 대상:** [`WORKLOG_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/WORKLOG_2026-10-24.md), [`REVIEW_REQUEST_Mixed_Album_Progress_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_REQUEST_Mixed_Album_Progress_2026-10-24.md), [`REVIEW_Mixed_Album_Addition_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_Mixed_Album_Addition_2026-10-24.md), [`REVIEW_Album_Progress_Display_2026-10-24.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_Album_Progress_Display_2026-10-24.md), [`MemoryAlbumAction.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryAlbumAction.java), [`WorkingProgressBanner.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/WorkingProgressBanner.java), [`WorkingProgressPresentation.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/WorkingProgressPresentation.java), [`MemoryBrowserState.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserState.java), [`DiscoveryOrganizeConfirmDialog.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoveryOrganizeConfirmDialog.java), [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)
- **판정:** **코드 반영 승인 (APPROVED for Merge)** — 331 tests 통과, P0/P1 원칙 완벽 준수, MIXED 앨범 경로 고정 및 상세 진행 배너 분리 검증

---

## 1. P0 / P1 원칙 및 핵심 검증 요약

### 🚨 [P0 원칙] MainActivity 비대화 절대 금지 (0줄 격리 원칙) — 통과 (PASS)
- 이번 두 패치(MIXED 기존 앨범 추가 + 상세 진행 표시)에서 [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)에 **새로운 비즈니스, 도메인, 판단, 계산 로직은 0줄 추가**되었습니다.
- 오히려 기존 `MainActivity`에 흩어져 있던 프로그레스 퍼센트 계산, 문자열 포맷팅, 단일 앨범 가드 코드가 전용 클래스로 추출/위임되면서 Git HEAD(`1c8d683`) 대비 **순감소 -308줄 (+172 / -480)**로 비대화가 추가 해소되었습니다.
- 신규 로직은 모두 단일 책임 클래스([`MemoryAlbumAction`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryAlbumAction.java), [`WorkingProgressBanner`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/WorkingProgressBanner.java), [`WorkingProgressPresentation`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/WorkingProgressPresentation.java))로 격리되었습니다.

### 🔒 [P1 원칙] 데이터 안전성 및 무결성 규칙 — 통과 (PASS)
- **MediaStore / 파일 불변 원칙:** 디스크의 미디어 파일을 복사/이동/삭제하지 않는 Read-only Projection을 준수했습니다. 실제 성남 사진 추가(373장 → 2,266장)는 사용자가 앱 내 확인 다이얼로그를 통해 직접 승인하여 실행되었습니다.
- **원본 휴지통 이동(Trash) 영구 차단 유지:** 이번 패치에서 휴지통 관련 코드나 권한 변경은 일절 없습니다.
- **사용자 메타데이터 보존:** `stableMemoryId + dateKey` 식별 체계 및 핵심 JSON 파일들의 보존이 유지되었습니다.

### 🧪 자동 빌드 및 단위 테스트 검증
- `./gradlew.bat testDebugUnitTest`: **331 tests 전건 통과 (0 failures, 0 errors, 0 skipped)**
  - `MemoryAlbumActionTest` (7건): 경로 고정, 중복+영상 제외, stale/빈 URI 필터링, 경로 위조 방지 등 전건 PASS.
  - `WorkingProgressPresentationTest` (4건): idle→준비→수치→완료 전이, 백그라운드 복귀 수치, 음수/초과 클램프 전건 PASS.
  - `MemoryBrowserStateTest`: 신규 MIXED 상세 액션 회귀 테스트 PASS.
- `git diff --check`: 포맷/공백 오류 없음 (**PASS**).

---

## 2. 패치별 상세 기술 검토

### 패치 A: MIXED 기존 앨범 추가 경로 ([`MemoryAlbumAction.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryAlbumAction.java))

| 검토 항목 | 구현 분석 및 검증 결과 | 상태 |
|---|---|---|
| **`canOrganize` vs `canAddNewItems` 경계** | 기존에는 `canOrganize == true`인 신규 발견 장소만 버튼이 떴으나, 이미 위치 앨범이 존재하는 `MIXED` 장소에서 `canAddNewItems == true`일 때 [`MemoryAlbumAction.canPrepare`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryAlbumAction.java#L9)가 true를 반환하고 라벨을 `"앨범에 추가"`로 분기함. | **PASS** |
| **기존 경로 고정 및 폴더 오탐색 방지** | `record.organizedAlbum.relativePath`를 엄격히 검증(`Pictures/` 하위, path traversal `..` 차단). 커스텀 `AlbumLookup`을 통해 장소 이름 기반 다른 폴더 탐색을 영구 차단하고 기존 경로를 강제 고정함. | **PASS** |
| **대상 앨범 부재 시 방어** | 기존 앨범 경로에 사진이나 영상이 실제로 존재하지 않으면(`lookup.find` 결과 부재), 새 폴더를 임의로 만들지 않고 `"기존 위치 앨범을 확인하지 못했어요. 사진은 변경하지 않았어요."` 예외를 발생시켜 작업을 즉시 중단함. | **PASS** |
| **영상 제외 및 중복 건너뜀** | 영상 파일은 앨범 정리 대상에서 제외되며(`OrganizationMediaPolicy`), 이미 위치 앨범에 존재하는 파일은 `duplicateInTarget` 로직으로 건너뜀. 모든 사진이 이미 추가된 경우 0-copy 안내로 안전하게 유도됨. | **PASS** |

### 패치 B: 상세 화면 작업 진행 표시 ([`WorkingProgressBanner.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/WorkingProgressBanner.java), [`WorkingProgressPresentation.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/WorkingProgressPresentation.java))

| 검토 항목 | 구현 분석 및 검증 결과 | 상태 |
|---|---|---|
| **화면 생성 후 작업 시작 시 배너 노출** | 기존에는 화면 빌드 시 `isWorking == true`일 때만 배너를 그렸으나, [`WorkingProgressBanner.attach`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/WorkingProgressBanner.java#L49)를 통해 숨겨진(`GONE`) 슬롯을 미리 부착해 두고, 상세 화면에서 작업을 시작하면 `setWorking(true)`가 호출될 때 즉시 `VISIBLE`로 전환되어 프로그레스가 노출됨. | **PASS** |
| **기존 파이프라인 재사용** | 별도의 스레드나 타이머, 폴링 루프를 신설하지 않고, 기존 `SortWorker` → `SortProgressStore` → `progressUpdateRunnable` 갱신 파이프라인의 수치를 그대로 활용함. | **PASS** |
| **뷰 수명주기 및 메모리 누수 방지** | Activity 필드에 이전 화면의 `TextView` 참조를 직접 보관하지 않고, `content.findViewWithTag(TAG)`를 통해 현재 화면에 부착된 배너만 찾아 갱신하므로 화면 전환이나 캐시 재부착 시 메모리 릭이나 크래시 위험이 없음. | **PASS** |
| **수치 클램핑 및 비정상값 방어** | [`WorkingProgressPresentation`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/WorkingProgressPresentation.java#L12-L17)에서 `total == 0`일 때 무한 루프나 NaN 대신 Indeterminate 상태로 전환하고, `current`가 `total`을 초과하거나 음수가 되는 경우를 완벽히 클램핑함. | **PASS** |

---

## 3. 실기기 동작 확인 결과 (기록 대조)

- **성남 앨범 추가 실행:**
  - 사용자 실행 전: 위치 앨범 사진 373장, 영상 19개
  - 사용자 실행 후: 위치 앨범 **사진 2,266장, 영상 30개**로 정상 증분 복사 확인.
  - 백그라운드 `SortWorker` 정상 SUCCESS 로그 확인.
- **설치 및 배포 상태:**
  - 테스트 빌드 **1.3.9 (code 39)**가 Play 서명 universal APK로 실기기에 무결하게 업데이트 설치됨 (앱 데이터 및 기존 기록 보존).

---

## 4. 발견 사항 및 후속 과제 (Findings)

### 📋 [Pre-release] 1.3.9 배너 실제 증분 시각 확인
- **내용:** 사용자가 성남 추가 작업을 실행했던 빌드는 1.3.8(code 38)이었으며, 진행 배너 패치는 1.3.9(code 39)에 반영되었습니다.
- **권고:** 단위 테스트(`WorkingProgressPresentationTest`)는 통과했으므로, 실기기에서 다음 사진 추가 작업 시 진행 막대와 % 수치가 눈으로 올라가는지 1회 육안 확인을 권장합니다.

### 📋 [Follow-up] 수원 간헐적 '기록 없음' 팝업 관측
- **내용:** 작업 직후 드물게 '기록 없음' 팝업이 발생했다는 제보가 있었으나 송파구 테스트에서는 미재현되었습니다.
- **권고:** `OrganizationLink` 저장과 화면 복귀 시점 사이의 레이스 컨디션 여부를 차기 마일스톤에서 지속 모니터링합니다.

---

## 5. 최종 결론

이번 수정은 사용자가 제기한 **성남 앨범 추가 버튼 누락**과 **상세 화면 진행 표시 누락** 두 가지 문제를 완벽하게 해결했습니다.
P0 원칙에 따라 `MainActivity`의 코드를 오히려 308줄 감소시키고, 단일 책임 원칙에 따라 격리 구현되었습니다.

**코드 반영 승인 (APPROVED for Merge)** 합니다.
