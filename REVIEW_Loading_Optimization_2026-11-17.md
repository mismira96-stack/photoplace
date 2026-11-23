# PhotoPlace 발견 기록 및 상세 로딩 최적화 코드 리뷰 (2026-11-17)

> **문서 상태**: 코드 리뷰 완료 보고서 (Reviewed by Antigravity)  
> **검토 대상**: Codex 작성 로딩 최적화 및 복귀 갱신 로직  
> **관련 커밋/파일**: `PersonalPlaceStore.java`, `MemoryBrowserGridRenderer.java`, `MemoryBrowserEntry.java`, `MemoryDetailLoadController.java`, `DiscoverySnapshotController.java`, `DiscoveryAutoRefresh.java`, `MainActivity.java`  
> **관련 문서**: [WORKLOG_2026-11-17.md](file:///c:/Users/mismi/Documents/Codex/GallerySorter/WORKLOG_2026-11-17.md) · [REVIEW_NonUI_2026-11-17.md](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_NonUI_2026-11-17.md)

---

## 1. 🚨 [P0 원칙 검증] MainActivity 비대화 절대 금지 (0줄 격리 원칙)

* **검증 결과: PASS (완벽 준수)**
* **상세 내역**:
  - `MainActivity.java`의 Git diff 분석 결과: `+519 / -1400` (순감소 **-881줄** 유지).
  - 새로 추가된 로직은 비즈니스/계산 로직이 아닌, 신규 컨트롤러(`MemoryDetailLoadController`, `DiscoveryAutoRefresh`)의 생명주기 연결(`onPause`, `onDestroy`) 및 단순 위임 1줄(`showMemoryBrowserDetailScreen(..., browserEntry)`)뿐입니다.
  - 그리드 최적화, 스냅샷 캐싱, 데이터 재사용, 리비전 비교 등 모든 핵심 도메인 로직은 100% 별도 전용 클래스(`MemoryBrowserEntry`, `PersonalPlaceStore`, `DiscoveryAutoRefresh`)로 완벽히 격리되었습니다.

---

## 2. 🔒 [P1 원칙 검증] 데이터 안전성 및 무결성 규칙

* **검증 결과: PASS (완벽 준수)**
* **상세 내역**:
  1. **MediaStore / 파일 불변 원칙 (Read-only Projection)**:
     - `PersonalPlaceStore.snapshotByParent` 및 `MemoryBrowserEntry.detailForRevision`은 메모리 내 기존 레코드를 읽기 전용 프로젝션으로 재사용합니다. 디스크 파일의 생성/이동/삭제/변조는 일체 발생하지 않습니다.
  2. **사용자 메모 및 불변 식별자 보존**:
     - 상세 화면 데이터 재사용 시 자칫 발생할 수 있는 메모 동기화 지연을 방지하기 위해, `DiscoverySnapshotController.withCurrentNotes`에서 최신 `MemoryDateNoteStore.readAll()`과 `stableId` 매핑을 즉시 재투영하도록 안전장치를 적용했습니다.
  3. **원본 휴지통 이동(Trash) 영구 차단 유지**:
     - 원본 삭제/휴지통 기능은 엄격히 비활성화 상태를 유지하고 있습니다.

---

## 3. 핵심 변경점 및 기술 분석

### ① 발견 기록 2열 카드 렌더링 파일 I/O 절감 (PersonalPlaceStore & MemoryBrowserGridRenderer)
* **기존 병목**:
  - 2열 카드에 바로가기 칩(예: `[우리집]`, `[회사]`)을 띄우기 위해 카드 1개를 그릴 때마다 `PersonalPlaceStore`와 `MemoryIdentityRegistryStore`를 디스크 파일에서 매번 동기적으로 읽음. (장소 65곳 기준 약 65회 파일 I/O 발생).
* **개선 구조**:
  - `MemoryBrowserGridRenderer.render` 시작 시 `PersonalPlaceDetailPresenter.loadCardShortcuts(context)`를 호출하여 **단 1회만** 스냅샷(`snapshotByParent`)을 메모리 맵으로 생성.
  - 각 카드는 `cardShortcuts.apply(item.memoryKey)`로 O(1) 메모리 조회를 수행하여 불필요한 디스크 I/O를 100% 제거.
  - 3열/4열 컴팩트 모드에서는 칩이 숨겨지므로 스냅샷 로드 자체를 건너뜀(`compact ? null : ...`).

### ② 발견 기록 카드 ➡️ 상세 진입 데이터 재사용 (MemoryBrowserEntry & MemoryDetailLoadController)
* **기존 병목**:
  - 발견 기록 탭에서 이미 모든 장소의 미디어 및 사진 정보를 메모리에 올려둔 상태임에도, 카드를 눌러 상세 화면에 들어갈 때마다 전체 레포지토리와 앨범들을 디스크에서 다시 쿼리·파싱하여 첫 진입에 5~9초 소요.
* **개선 구조**:
  - 카드를 터치할 때 현재 브라우저의 스냅샷 엔트리(`MemoryBrowserEntry`)를 함께 전달.
  - 리비전이 일치하면 `entry.detailForRevision(...)`을 통해 이미 메모리에 적재된 `MemoryRecord`와 미디어 정보를 0ms에 가깝게 즉시 재사용.
  - 리비전 불일치 또는 외부 딥링크/홈 진입 시에는 기존의 안전한 전체 쿼리 경로(`loadBrowserDetail`)로 fallback 보장.
  - **측정치**: 에뮬레이터 기준 상세 진입 데이터 단계 130ms, 화면 구성 324ms (체감 로딩 지연 대폭 감소).

### ③ 최초 탭 진입 시 사전 로딩 작업 승격 (MemoryBrowserLoadController & promotePreload)
* **기존 병목**:
  - 앱 시작 시 홈 화면에서 백그라운드로 발견 기록을 사전 로딩(`preload`)하고 있던 중 사용자가 '발견 기록' 탭을 터치하면, 진행 중이던 사전 로딩 작업을 취소(`cancel()`)하고 동일한 조회를 처음부터 다시 시작하는 비효율 발생.
* **개선 구조**:
  - `loader.promotePreload(requestKey, bind, error)`를 도입하여, 이미 실행 중인 백그라운드 사전 로딩이 있으면 이를 즉시 포그라운드 작업으로 승격(Promote)하여 완료 결과를 그대로 이어받음.
  - 중복 작업 취소 및 재실행 비용을 완전히 제거하여 첫 탭 진입 체감 지연 최소화.

### ④ 앱 복귀 시 자동 갱신 재시도 보강 (DiscoveryAutoRefresh.BusyRetry)
* **기존 이슈**:
  - 카메라 촬영 후 앱으로 돌아올 때 백그라운드에서 앨범 확인이나 미리보기 작업이 실행 중(`busy`)이면 자동 갱신이 1회 생략되고 영영 다시 트리거되지 않는 엣지케이스 존재.
* **개선 구조**:
  - `BusyRetry` 클래스를 통해 `busy` 상태일 경우 500ms 간격으로 최대 20회(약 10초간) 재확인을 안전하게 예약.
  - 작업이 완료되는 즉시 새로운 사진을 감지하여 갱신하며, 화면을 떠나거나 사용자가 수동 갱신을 누르면 즉시 취소하여 배터리·CPU 낭비 차단.

### ⑤ 글자 크기(Font Scale) 확대 시 홈 배너 레이아웃 적응
* **기존 이슈**: 글자 배율 1.3배 이상 기기에서 고정 높이 제약으로 설명 텍스트가 2px로 압축 잘림.
* **개선 구조**: 최소 94dp 높이를 확보하되 내용 길이에 따라 유동적으로 늘어나도록 패딩 및 레이아웃 조정.

---

## 4. 코드 품질 및 안전성 검토 (Code Review Findings)

### [Blocker / Pre-release 이슈]
* **발견되지 않음 (0건)**:
  - 데드락 위험 없음: `synchronized` 블록 범위가 기존과 동일하며 최소화됨.
  - 메모리 누수 위험 없음: `MainActivity.onDestroy()` 및 `onPause()`에서 모든 로더와 컨트롤러가 정상 `cancel()` / `close()`됨.
  - Fallback 안전성 확보: 데이터 버전 불일치 시 기존 로더로 자연스럽게 전환됨.

### [Follow-up 보강 완료]
1. **`PersonalPlaceDetailPresenter.java:488` 널 안전성 방어**:
   - `bindCardShortcuts` 파라미터 `cardPlaces`에 대해 `cardPlaces == null || cardPlaces.isEmpty()` 검사를 추가하여 잠재적 NPE 원천 차단 완료.
2. **실기기 체감 확인 (사용자 검증 권장)**:
   - 사진 5,500장 및 장소 65곳이 있는 실제 사용자 폴드(`SM-F971N`)에서 카드를 누르고 상세에 들어갈 때의 빠른 진입 체감 확인 필요.

---

## 5. 단위 테스트 및 빌드 검증

* **Unit Test**: 568개 테스트 전체 통과 (Debug & Release)
  - `DiscoveryAutoRefreshSessionTest`, `PersonalPlaceTest`, `MemoryBrowserEntryTest` 등 신규 로직 테스트 포함.
  - 결과: `BUILD SUCCESSFUL in 11s` (실패·오류 0개)
* **Lint / Proguard**:
  - `lintVitalRelease`, `assembleDebug`, `assembleRelease` 정상 통과 확인.

---

## 6. 최종 종합 의견

Codex의 금번 로딩 최적화 작업은 **P0(MainActivity 0줄) 및 P1(데이터 안전성) 원칙을 철저히 준수**하면서도, 불필요한 디스크 I/O와 중복 쿼리를 정확히 짚어내어 메모리 레벨에서 매우 깔끔하게 해결한 **모범적인 리팩토링**입니다.

따라서 **"승인 (Approved)"** 판정을 내리며, 사용자의 실기기 확인 후 릴리즈 단계로 안전하게 진행하실 수 있습니다.
