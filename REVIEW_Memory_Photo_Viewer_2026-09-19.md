# PhotoPlace Memory Photo Viewer 종합 코드 리뷰

**일자:** 2026-09-19  
**작성자:** Antigravity  
**검토 대상:**
- [`REVIEW_REQUEST_Memory_Photo_Viewer_2026-09-19.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_REQUEST_Memory_Photo_Viewer_2026-09-19.md)
- Commits:
  - `25de144 Add in-app memory photo viewer`
  - `c40b037 Preserve memory detail position after viewer`
  - `00ecf6e Polish memory viewer navigation`
  - `07937f6 Refine memory viewer navigation controls`
  - `6c42cf4 Add memory viewer review request`
- 소스 코드:
  - [`MemoryPhotoViewer.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryPhotoViewer.java)
  - [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)
  - [`MemoryPhotoSection.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryPhotoSection.java)
  - [`MemoryCollectionDetailRenderer.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryCollectionDetailRenderer.java)

**최종 판정:**
- **내부 Photo Viewer 기능 및 단일 Source 연동:** **최종 승인 (Final Approved)**
- **기억 모음 상세(Collection Detail) 복귀 경로 및 화면 회전:** **수정 완료 확인 (Resolved - commit 784c62c)**
- **전환 시 번쩍임(Flicker UX):** **완화 완료 확인 (Resolved - commit 784c62c `loadThumbnailIntoPreservingCurrent`)**
- **실기기 Smoke Test:** **전체 통과 (Passed on R5KL503VHQR - commit a9bb484)**
- **Play 프로덕션 정식 출시:** **보류 유지 (Hold - Gallery-only 미디어 재진입 최종 확인 및 원본 휴지통 차단 유지)**

---

## 1. Blocker 및 P1 / P2 발견 사항 (Findings)

### [P1 Blocker] 기억 모음 상세(`memoryCollectionDetailMode`)에서 뷰어 진입 후 복귀 경로 유실 및 회전 시 강제 이탈

- **관련 코드:**
  - [`MainActivity.java:10266-10275`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L10266-L10275) (`closeMemoryPhotoViewer`)
  - [`MainActivity.java:412-427`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L412-L427) (`refreshCurrentScreenForLayoutChange`)
  - [`MainActivity.java:10207-10225`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L10207-L10225) (`showMemoryPhotoViewer`)
- **결함 내용:**
  1. **복귀 경로 유실:**
     - 사용자가 **기억 모음 상세 화면**에서 사진 썸네일을 탭하여 내부 뷰어로 들어간 뒤, 닫기(`‹`) 또는 하드웨어 Back 버튼을 누르면 [`closeMemoryPhotoViewer()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L10266)가 실행됩니다.
     - 그러나 `closeMemoryPhotoViewer()`는 다음과 같이 구현되어 있습니다:
       ```java
       if (!this.activeMemoryKey.isEmpty()) {
           showMemoryBrowserDetailScreen(this.activeMemoryKey);
       } else {
           showMemoryBrowserScreen(this.memoryBrowserShowsOrganizedSources);
       }
       ```
     - 기억 모음 상세(`activeMemoryCollectionId`)로 돌아가는 분기가 전혀 없기 때문에:
       - 이전에 열었던 일반 Memory의 키가 남아있으면 엉뚱한 다른 Memory 상세가 열립니다.
       - `activeMemoryKey`가 비어있으면 기억 모음 화면이 아닌 **최상위 메인 목록(`showMemoryBrowserScreen`)으로 강제 퇴출**당합니다.
  2. **화면 회전 시 뷰어 강제 종료:**
     - `showMemoryPhotoViewer()` 진입 시 `this.memoryCollectionDetailMode = false`로 끄지 않습니다.
     - 이 상태에서 화면을 가로/세로로 회전하면 `refreshCurrentScreenForLayoutChange()`가 호출되는데, 우선순위 상단에 있는 다음 조건이 먼저 평가됩니다:
       ```java
       if (this.memoryCollectionDetailMode && !this.activeMemoryCollectionId.isEmpty()) {
           showMemoryCollectionDetailScreen(this.activeMemoryCollectionId);
           return;
       }
       ```
     - 따라서 뷰어를 보던 중 화면을 돌리면 뷰어가 즉시 파괴되고 기억 모음 상세 화면으로 튕겨 나갑니다.
- **수정 방안:**
  - 뷰어 진입 전 복귀할 화면 타깃(`viewerBackTarget = BACK_TO_MEMORY_DETAIL` 또는 `BACK_TO_COLLECTION_DETAIL`)을 기록합니다.
  - `showMemoryPhotoViewer()` 진입 시 `this.memoryCollectionDetailMode = false`를 명시적으로 설정합니다.
  - `closeMemoryPhotoViewer()`에서 `viewerBackTarget`에 따라 `showMemoryCollectionDetailScreen(this.activeMemoryCollectionId)` 또는 `showMemoryBrowserDetailScreen(this.activeMemoryKey)`로 올바르게 분기 복귀하도록 수정합니다.

---

### [P2 / UX] "사진 더 보기" 페이징 상태 유실 (스크롤 복귀 한계)

- **관련 코드:** [`MainActivity.java:9739-9747`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L9739-L9747)
- **내용:**
  - 커밋 `c40b037`에서 `activeMemoryDetailScrollY`를 저장하고 `scrollView.post(() -> scrollTo(0, restoreY))`로 스크롤을 복원하도록 한 것은 매우 훌륭한 조치입니다.
  - 다만, 상세 화면을 다시 렌더링할 때 기본 페이징(48장)만 생성되므로, 48장 이후의 사진을 보기 위해 "사진 더 보기"를 눌러 96장 등으로 확장해 둔 상태였다면, 뷰어에서 복귀할 때 48장까지만 생성되어 원하는 스크롤 Y 위치까지 내려가지 못하고 중간에 멈출 수 있습니다.
  - 향후 페이징 표시 개수(`visiblePhotoCount`)도 함께 보존하여 렌더링하거나, Overlay 방식으로 ScrollView 자체를 유지하는 방향을 권장합니다.

---

### [P2 / 아키텍처] `indexOfPhoto` 유틸 분리

- [`MainActivity.java:10184-10195`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L10184-L10195)의 `indexOfPhoto`는 순수 리스트 탐색이므로 `MemoryPhotoSection.indexOf(item)`으로 이동하여 Activity 크기를 줄이는 것이 좋습니다.

---

## 2. 이전/다음 전환 시 번쩍임(Flicker) 원인 심층 분석

실기기에서 이전/다음 버튼을 누르거나 스와이프할 때 화면이 하얗게 번쩍이는 현상의 원인은 **`MainActivity.loadThumbnailInto`의 플레이스홀더 처리 방식과 뷰어의 어두운 배경 간의 충돌**입니다.

### 1. 실행 흐름 추적

```text
[이전/다음 탭] 
  → MemoryPhotoViewer.bind() 
  → host.loadThumbnail(image, uri, host.dp(900))
  → MainActivity.loadThumbnailInto() [라인 7850-7855]
```

[`MainActivity.java:7849-7855`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L7849-L7855):
```java
private void loadThumbnailInto(final ImageView imageView, final Uri uri, final int i, final String fallbackRelativePath) {
    final String str = uri.toString() + "#" + i;
    imageView.setTag(str);
    imageView.setImageDrawable(thumbnailPlaceholder()); // <--- [핵심 원인 1]
    Bitmap bitmap = this.thumbnailCache.get(str);       // <--- [핵심 원인 2]
    if (bitmap != null) {
        imageView.setImageBitmap(bitmap);
    } else {
        // 비동기 worker 디코딩 시작
        ...
```

### 2. 세부 원인

1. **캐시 확인 전 무조건 밝은 플레이스홀더 설정:**
   - 위 코드에서 보듯, `thumbnailCache.get(str)`을 조회하기도 전에 **라인 7851에서 `imageView.setImageDrawable(thumbnailPlaceholder())`가 무조건 실행**됩니다.
2. **플레이스홀더 색상과 뷰어 배경색의 극명한 대비:**
   - [`MainActivity.thumbnailPlaceholder()`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L7978-L7984)는 리스트/그리드용으로 만들어진 **밝은 연회색(`#EFF3FF`) 드로어블**입니다.
   - 반면 `MemoryPhotoViewer`의 배경은 **어두운 딥 블랙/남색(`Color.rgb(18, 20, 28) = #12141C`)**입니다.
   - 검은 배경 위에서 거대한 900dp 영역의 `ImageView`가 순식간에 **새하얀 네모 박스로 번쩍**했다가 사진으로 교체됩니다.
3. **캐시 미스 시 노출 시간 증가:**
   - 아직 메모리 캐시에 들어오지 않은 사진의 경우, `thumbnailWorker`가 디스크에서 900dp 비트맵을 디코딩하여 UI 스레드로 올릴 때까지(약 50~150ms) 밝은 사각형이 지속 노출되어 스트로보(플래시) 조명을 켠 것처럼 번쩍입니다.
4. **캐시 히트 시에도 발생하는 깜빡임:**
   - 메모리 캐시에 비트맵이 이미 있더라도, 한 프레임 내에서 `setImageDrawable(placeholder)` → `setImageBitmap(bitmap)`이 연속으로 호출되어 안드로이드 뷰 계층의 재렌더링 과정에서 미세한 잔상/깜빡임이 감지됩니다.

---

## 3. 번쩍임 해소를 위한 최소 수정 권장안 (Minimal Fix)

기존 그리드 썸네일 기능에 영향을 주지 않으면서 가장 작고 안전하게 번쩍임을 제거하는 2단계 조치입니다:

### 1단계: `loadThumbnailInto`에서 캐시 히트 시 플레이스홀더 생략 (가장 기본)
캐시에 비트맵이 있으면 플레이스홀더를 아예 거치지 않고 바로 세팅합니다:
```java
// MainActivity.java - loadThumbnailInto
final String str = uri.toString() + "#" + i;
imageView.setTag(str);
Bitmap bitmap = this.thumbnailCache.get(str);
if (bitmap != null) {
    imageView.setImageBitmap(bitmap); // 캐시 히트 시 플레이스홀더 없이 즉시 표시
} else {
    imageView.setImageDrawable(thumbnailPlaceholder());
    // worker 비동기 디코딩 진행...
}
```

### 2단계: 뷰어 로딩 시 기존 비트맵 유지 (Full-screen Viewer 표준 패턴)
뷰어에서는 새 사진이 로딩 중일 때 하얀 박스로 지워버리는 대신, **이전 사진을 화면에 그대로 남겨두고 새 사진이 디코딩 완료되었을 때 교체**하는 것이 자연스럽습니다.
- 방법: `Host.loadThumbnail` 또는 뷰어 전용 로더에서 캐시 미스 시 `setImageDrawable(placeholder)` 대신 아무것도 하지 않거나 어두운 투명색(`Color.TRANSPARENT`)을 유지하도록 처리.

### 3단계 (추가 권장): 인접 사진 1장 백그라운드 프리로드 (Preload)
- `bind()` 호출 시 현재 인덱스의 `index - 1`, `index + 1` 사진의 URI를 `thumbnailWorker`에 던져 `thumbnailCache`에 미리 넣어두면:
  - 사용자가 이전/다음 버튼을 누르거나 스와이프할 때 **100% 캐시 히트**가 되어 딜레이와 번쩍임이 완전히 사라집니다.

---

## 4. 필수 검토 항목별 종합 평가

### 1. 내부 뷰어 진입 경로
- **일반 Memory 상세:** **정상 동작 (Pass)** - 사진 탭 시 내부 뷰어로 진입하며, 동영상 탭 시 외부 플레이어 인텐트로 바로 연결됩니다.
- **기억 모음 상세:** **연동 완료되었으나 복귀 버그 존재 (P1)** - `MemoryCollectionDetailRenderer`가 `viewerSection`을 잘 주입하여 뷰어로 진입하지만, 닫을 때 기억 모음으로 돌아가지 못하는 상기 P1 버그 수정 필요.
- **뷰어 내부 동영상:** **정상 동작 (Pass)** - 뷰어 안에서 동영상 슬라이드 도달 시 안내문구와 `Gallery에서 열기` 버튼이 명확히 제공됩니다.

### 2. 미디어 Source / Identity / 날짜 메모
- **단일 소스 원칙:** **완벽 준수 (Pass)** - `MemoryMediaResolver`의 단일 소스 결과를 그대로 사용하며 절대 Union하지 않습니다.
- **날짜 메모 보존:** **완벽 준수 (Pass)** - 뷰어 진입 및 복귀 후에도 `stableMemoryId + dateKey`를 통한 기존 메모 조회가 정상 유지됩니다.

### 3. 네비게이션 / 상태 복원
- **인덱스 및 경계:** **정상 동작 (Pass)** - `clamp` 적용, 첫 장 이전 및 마지막 장 다음 버튼 비활성화(`alpha=0.4`) 정상 처리되었습니다.
- **스크롤 복원:** **대부분 만족 (Pass, commit c40b037)** - `activeMemoryDetailScrollY` 복원이 적용되었습니다.
- **화면 회전:** **기억 모음 상태에서 버그 존재 (P1)** - `memoryCollectionDetailMode` 플래그 미정리로 인해 회전 시 뷰어가 닫히는 현상 수정 필요.

---

## 5. 실기기 Smoke Test 및 Release Gate 판정

- **실기기 대상:** `R5KL503VHQR` (검증 완료 - 커밋 `a9bb484`)
- **실기기 확인 체크리스트:**
  1. [x] 일반 Memory 상세에서 사진 탭 시 내부 뷰어 정상 진입 확인
  2. [x] 사진 좌우 스와이프 및 이전/다음 버튼 정상 동작 확인
  3. [x] **기억 모음 상세에서 사진 탭 후 Back 시 기억 모음으로 정확히 복귀 확인 (P1 결함 해결 완료)**
  4. [x] **뷰어 사진 전환 시 기존 비트맵 유지로 흰색 플래시 번쩍임 완화 확인 (해결 완료)**
  5. [x] **화면 회전 시 뷰어가 유지되는지 확인 (해결 완료)**
  6. [x] 뷰어 복귀 후 날짜 메모(`stableMemoryId + dateKey`) 유지 확인
  7. [P2 Polish 기록] Back 시 상세 화면을 재렌더링할 때 발생하는 아주 짧은 flicker는 기능 blocker가 아니며, 향후 Overlay 방식으로 보완 가능한 P2 polish 항목으로 워크로그에 기록됨

| 구분 | 판정 | 설명 |
| :--- | :---: | :--- |
| **Photo Viewer 구현 및 Smoke Test** | **최종 승인 (Final Approved)** | 뷰어 UI, Resolver 연동, 기억 모음 복귀(P1), 번쩍임 완화 및 실기기 smoke test 전체 통과 완료 |
| **Play 프로덕션 정식 출시** | **보류 유지 (Hold)** | 1. Gallery-only 미디어(원본이 Gallery로 이동된 항목)의 실기기 재진입 최종 확인 대기.<br>2. **원본 사진 휴지통 이동(Trash) 기능은 계속 차단 유지.**<br>3. 버전 증가 및 Play 릴리즈 빌드 준비 절차 대기. |
