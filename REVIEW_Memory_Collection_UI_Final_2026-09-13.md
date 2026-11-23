# PhotoPlace Memory Collection UI 슬라이스 최종 종합 코드 리뷰 (아키텍처 구조 개선 요구)

**일자:** 2026-09-13  
**작성자:** Antigravity  
**검토 대상:**
- `REVIEW_REQUEST_Memory_Collection_UI_Final_2026-09-13.md`
- `app/src/main/java/com/example/gallerysorter/MainActivity.java` (신규 547줄 추가)
- `app/src/main/java/com/example/gallerysorter/MemoryCollectionMembership.java`
- `app/src/test/java/com/example/gallerysorter/MemoryCollectionMembershipTest.java`
- `TODO.md` 및 `WORKLOG_2026-09-13.md`

**최종 종합 판정:**
- **기능 동작 검증:** **통과 (Pass - 단위 테스트, 화면 회전, 비파괴 해제, 접근성 충족)**
- **아키텍처 및 코드 구조:** **【반려 및 분리 리팩터링 필수 (Refactor Required Before Commit)】**
- **Play 프로덕션 출시:** **보류 유지 (Hold - 공용 Media Resolver 선행 및 실기기 스모크 대기)**

---

## 1. 긴급 구조적 문제 지적: MainActivity 비대화 (God Activity) 재발 방지

기존 레거시 코드는 이미 프로덕션에 출시된 상태였기에 안정성을 위해 대규모 리팩터링을 지양했으나, **"신규 기능 구현 시에는 MainActivity에 코드를 직접 밀어넣지 않고 독립 컴포넌트(Renderer/Controller)로 분리한다"**는 원칙이 프로젝트의 핵심 설계 규약입니다.

그러나 이번 Memory Collection UI 슬라이스에서는 **547줄에 달하는 순수 UI 렌더링 및 뷰 바인딩 코드가 전부 `MainActivity.java`에 인라인으로 추가**되어, 파일 크기가 11,400줄에 육박하게 되었습니다.

프로젝트 내에 이미 모범적으로 분리된 선례들(`MemoryOrganizationCompletionRenderer`, `MemoryBrowserGridRenderer`, `MemorySearchHeaderRenderer`)이 존재함에도 불구하고, 이번 신규 화면들이 Activity에 직결된 것은 명백한 기술 부채이자 아키텍처 원칙 위배입니다. 

따라서 **커밋 전 전용 Renderer 클래스로의 분리 리팩터링을 필수로 요구**합니다.

---

## 2. 필수 분리 리팩터링 설계 (약 480~500줄 분리)

`MainActivity.java`에서 UI 구성 책임을 분리하여 다음 3개의 독립 Renderer 클래스로 추출해야 합니다:

### 1) `MemoryCollectionCardRenderer.java` (신규 추출)
- **책임:** 메인 발견 브라우저 화면의 "내 기억 모음" 카드 섹션 렌더링
- **추출 대상 메서드:**
  - `addMemoryCollectionCards()`
  - `collectionCardSummary()`
  - `CollectionCardSummary` 내부 클래스
- **인터페이스:**
  - `Context`, `ThumbnailLoader` 주입
  - `Listener`: `void onCollectionClicked(String collectionId)`

### 2) `MemoryCollectionSelectionRenderer.java` (신규 추출)
- **책임:** 기억 모으기 장소 다중 선택 화면 렌더링, 체크박스 상태 토글, 하단 CTA 버튼 카운트 갱신 및 이름 입력 다이얼로그
- **추출 대상 메서드:**
  - `showMemoryCollectionSelectionScreen()` 내 스크롤/목록/체크박스 생성 로직
  - `promptMemoryCollectionTitle()`
- **인터페이스:**
  - `Listener`: `void onCollectionCreated(String title, List<Member> members)`, `void onCancel()`

### 3) `MemoryCollectionDetailRenderer.java` (신규 추출)
- **책임:** 모음 상세 화면의 `날짜 -> 장소 -> 날짜 메모 -> 사진` 계층 뷰 렌더링, 48장 페이징(`addGroupMemoryPhotos`), 상단 액션 바(이름 변경, 해제) 렌더링
- **추출 대상 메서드:**
  - `showMemoryCollectionDetailScreen()` 내 뷰 빌딩 로직
  - `addGroupMemoryPhotos()`
- **인터페이스:**
  - `Listener`: `void onRenameRequested(String collectionId, String currentTitle)`, `void onDissolveRequested(String collectionId)`, `void onBack()`

---

## 3. 리팩터링 후 MainActivity의 역할

리팩터링이 완료되면 `MainActivity.java`에는 다음과 같이 10~15줄 내외의 순수 조정(Routing/Controller) 코드만 남게 됩니다:

```java
// 예시: MainActivity에 남을 깔끔한 라우팅 메서드
private void showMemoryCollectionDetailScreen(final String collectionId) {
    MemoryCollection collection = findCollection(collectionId);
    if (collection == null) {
        showToast("기억 모음을 찾지 못했어요.");
        showMemoryBrowserScreen(this.memoryBrowserShowsOrganizedSources);
        return;
    }
    GroupMemoryDetail detail = resolveCollectionDetail(collection);
    
    // UI 빌딩은 분리된 렌더러에 완전히 위임
    View view = new MemoryCollectionDetailRenderer(this, thumbnailLoader).render(
            collection, detail, new MemoryCollectionDetailRenderer.Listener() {
                @Override public void onRename(...) { ... }
                @Override public void onDissolve(...) { ... }
                @Override public void onBack() { showMemoryBrowserScreen(...); }
            });
    setContentViewWithBottomTabs(view, 1);
}
```

---

## 4. 기능 및 동작성 검증 현황 (기능 자체는 완벽)

- **화면 회전 복원:** `refreshCurrentScreenForLayoutChange()`에서 선택 상태(`selectedKeys.retainAll`) 및 상세 화면 유지 정상.
- **비파괴적 해제/이름 변경:** `memory_collections.json` 메타데이터만 수정하며 원본 미디어, 날짜 메모, 위치 앨범 무변경 보장.
- **검색 예외:** 모음 멤버가 기본 그리드에서는 접히고 검색(`searching == true`)에서는 개별 노출 보장.
- **단일 일자 포맷 및 접근성:** 당일치기 날짜 중복 제거 및 대표 사진 TalkBack 설명 반영 완료.
- **자동 검증 통과:**
  - `./gradlew.bat testDebugUnitTest`: 통과 (20 tasks, 3s)
  - `./gradlew.bat assembleDebug`: 통과 (APK 빌드 완료)
  - `git diff --check`: 통과

---

## 5. 결론 및 행동 지침

1. **현재 작업물을 커밋하기 전에 반드시 위의 3대 Renderer 분리 리팩터링을 선행할 것.**
2. 분리 완료 후 테스트(`testDebugUnitTest`)와 빌드(`assembleDebug`)를 통과한 상태에서 최종 커밋을 생성할 것.
3. 이 구조적 정리가 완료되어야 향후 **공용 Memory Media Resolver** 작업 시 `MainActivity`를 더 이상 어지럽히지 않고 안전하게 확장할 수 있음.
