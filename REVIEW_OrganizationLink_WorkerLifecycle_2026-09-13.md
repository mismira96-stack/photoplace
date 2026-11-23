# PhotoPlace OrganizationLink Worker Lifecycle 코드 리뷰

**일자:** 2026-09-13  
**대상 커밋:**
- 8cd8192 Add organization link persistence
- 1243d8a Harden organization link replay semantics
- 2380645 Persist organization context through sort worker
- 5b71235 Persist confirmed organization results idempotently

**리뷰 기준 브랜치:** codex/photoplace-v2-bg-wip  
**최종 판단:** **수정 후 승인 (Conditional Approval)**  

---

## 1. 개요 및 변경 요약

이번 변경 체인은 OrganizationRequest 메타데이터(requestId, subjectType, subjectId, albumName, relativePath)를 SortInputStore -> SortWorker -> SortResultStore 파이프라인 전체에 걸쳐 유실 없이 전달하고, 작업 완료 후 실제 성공한 항목만을 기반으로 AlbumSummaryHistoryStore, MemoryIdentityRegistryStore, MemoryOrganizationLinkStore에 멱등적으로 결과를 반영하는 수명주기 완결 작업을 수행했습니다.

---

## 2. 확인 요청 항목별 심층 분석

### ① requestId 기반 재처리의 멱등성 및 장애 복구 안전성
- **분석:**
  - persistOrganizationResult()는 AlbumSummaryHistoryStore -> MemoryIdentityRegistryStore -> MemoryOrganizationLinkStore 순으로 영속화합니다.
  - 도중에 앱이 강제 종료되거나 예외가 발생하더라도 SortResultStore의 스냅샷 파일이 삭제되지 않고 남아있으므로, 다음 앱 실행 시 동일 스냅샷이 다시 처리됩니다.
  - AlbumSummaryHistoryStore: containsRequestId()로 동일 requestId의 세션 중복 추가를 원천 차단합니다.
  - MemoryIdentityRegistryStore: 이미 등록된 path: alias가 동일 subjectId를 가리키면 정상(	rue) 반환합니다.
  - MemoryOrganizationLinkStore: sameRequest() 및 CommitResult.ALREADY_COMMITTED로 중복 링크 생성을 방지합니다.
  - **결론:** 각 저장소 단계별 멱등성 가드가 촘촘하여 단계 간 크래시가 발생해도 데이터 불일치나 중복 없이 재처리됩니다.

### ② SortResultStore 소비 전 persistence 완료 및 실패 시 재시도 흐름
- **분석:**
  - handleBackgroundSortResultIfAvailable()에서 persistOrganizationResult(snapshot)이 	rue를 반환한 이후에만 esultStore.clear()를 호출하므로, 데이터 저장 성공 전에 스냅샷이 조기 삭제되는 유실 위험이 없습니다.
  - **다만, 치명적 결함(Blocker 1)**: 영구적으로 복구 불가능한 검증 오류(예: link == null, CommitResult.CONFLICT, INVALID)가 발생했을 때도 무조건 alse를 반환하여 2초 주기의 ackgroundSortResultCheckRunnable 무한 재시도 루프에 빠지는 구조적 취약점이 존재합니다.

### ③ OrganizationLinkFromSortResult의 무결성 검증
- **분석:**
  - esult.copiedCount <= 0이거나 esult.sortedItems.size() != result.copiedCount인 경우 즉시 
ull을 반환하여 잘못된 결과가 링크로 승격되는 것을 차단합니다.
  - 복사된 모든 항목의 	argetRelativePath가 요청된 equest.relativePath와 일치하는지 
ormalizePath()를 통해 전수 대조합니다. 다른 앨범 경로로 들어간 사진이 섞여 있으면 링크 생성을 거부합니다.
  - esult.canceled || result.failedCount > 0인 경우 PARTIAL로 격하하고, 완벽히 성공했을 때만 SUCCESS를 부여합니다.
  - **참고 사항:** 대상 폴더에 이미 사진이 존재하여 copiedCount == 0, skippedCount > 0인 경우 링크가 생성되지 않습니다. 이는 실제 출력이 발생한 경우에만 링크를 생성한다는 설계 원칙에 부합합니다.

### ④ AlbumSummaryHistoryStore.appendSession의 requestId 호환성
- **분석:**
  - 일반 정리 요청처럼 equestId가 빈 문자열이거나 
ull인 경우 containsRequestId 검사를 건너뛰고 기존대로 새 세션을 생성하므로 기존 기능의 회귀가 없습니다.
  - equestId가 있는 경우에만 고유성을 검사하고 세션 JSON에 requestId 필드를 기록합니다.
  - 단위 테스트(AlbumSummaryHistoryRequestTest)로 완벽히 검증되었습니다.

### ⑤ path: alias 충돌 시의 동작과 exact OrganizationLink 우선 정책
- **분석:**
  - MainActivity.java:2202-2204:
    `java
    boolean aliasRegistered = memoryIdentityRegistryStore().registerAlias(request.subjectId, pathAlias);
    if (!aliasRegistered && memoryIdentityRegistryStore().findStableId(pathAlias).isEmpty()) {
        return false;
    }
    `
  - pathAlias가 이미 다른 Memory에 선점되어 있는 경우, egisterAlias는 alse를 반환하지만 indStableId는 비어있지 않으므로 I/O 실패가 아닌 alias 충돌로 판정하여 재시도 루프에 빠지지 않고 다음 단계인 OrganizationLink 커밋으로 진행합니다.
  - path: alias는 과거 1:1 휴리스틱 색인일 뿐이며, 차기 MemoryRepository 설계의 단일 진실 공급원(SSOT)은 OrganizationLink이므로, alias 충돌 상황에서도 exact link를 정상 기록하는 현재 정책은 정확하고 안전합니다.

### ⑥ SUCCESS / PARTIAL / MISSING 수명주기 일관성
- **분석:**
  - indLatestUsableLink()는 SUCCESS와 PARTIAL만 유효한 출력으로 취급하고, MISSING은 배제하며, 복수 링크 중 타임스탬프가 가장 최신인 링크를 반환합니다.
  - hasUsableOutput()도 indLatestUsableLink() != null에 위임하여 판정 기준이 일치합니다.

### ⑦ 메타데이터 없는 일반 정리 요청의 회귀 여부
- **분석:**
  - SortInputStore.write(items, shouldMoveVideos) (인자 2개 오버로드) 유지.
  - organizationRequest == null인 경우 persistOrganizationResult()가 즉시 	rue를 반환하고, 기존대로 saveAlbumSummaryHistory(..., ")를 1회만 호출하며 정상 완료 처리됩니다.
 - 기존 일반 정리 및 포그라운드 복사 흐름에 회귀가 없습니다.

---

## 3. Blocker (CTA 연결 전 필수 수정 필요)

### [Blocker 1] persistOrganizationResult 영구 실패 시 무한 2초 재시도 루프 발생
- **위치:** [MainActivity.java:2167-2170](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L2167-L2170), [2184-2208](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L2184-L2208)
- **문제 시나리오:**
 1. SortResultStore의 스냅샷에 organizationRequest가 포함되어 있으나, 결과 데이터 불일치로 OrganizationLinkFromSortResult.create()가 
ull을 반환하거나,
 2. memoryOrganizationLinkStore().commit(link)가 CONFLICT 또는 INVALID를 반환하는 경우.
 3. persistOrganizationResult()는 alse를 반환합니다.
 4. handleBackgroundSortResultIfAvailable()은 2초 뒤 his.backgroundSortResultCheckRunnable을 재호출합니다.
 5. 디스크의 sort_result_snapshot.json은 정적 파일이므로, **2초마다 영구히 동일한 실패를 반복하며 UI가 앨범으로 정리 중 상태에 영원히 갇히게 됩니다** (배터리 소모 및 결과 화면 진입 불가).
- **해결 방안:**
 - 단순 I/O 일시 장애(saveAlbumSummaryHistory 실패, CommitResult.FAILED)와 비가역적 유효성 실패(link == null, CONFLICT, INVALID)를 구분해야 합니다.
 - 재시도 횟수 제한(예: 최대 3회)을 두거나, 비가역적 실패 시 에러 로깅 후 일반 결과 화면으로 폴백(fallback)하여 esultStore.clear()를 수행하고 빠져나오도록 안전망을 구성해야 합니다.

---

## 4. Recommendation (개선 권고사항)

### 지금 고칠 것 (Worker/UI 연결 전 권장)
1. **completedAtMillis 타임스탬프 일관성 확보**:
 - [MainActivity.java:2190](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L2190):
 OrganizationLinkFromSortResult.create(request, snapshot, System.currentTimeMillis());
 - 스냅샷 객체 내에 Worker가 실제 정리를 완료한 시각인 snapshot.completedAtMillis가 이미 존재합니다.
 - 앱이 백그라운드에 있다가 수 분 뒤 열렸을 때 System.currentTimeMillis()를 쓰면 완료 시각이 왜곡되므로, snapshot.completedAtMillis > 0L ? snapshot.completedAtMillis : System.currentTimeMillis()를 넘기도록 수정하는 것이 바람직합니다.
2. **saveAlbumSummaryHistory 중복 호출 정리**:
 - handleBackgroundSortResultIfAvailable()에서 persistOrganizationResult()가 이미 앨범 히스토리를 저장했음에도, 바로 다음 줄의 m40lambda...에서 saveAlbumSummaryHistory()가 한 번 더 호출됩니다.
 - AlbumSummaryHistoryStore의 containsRequestId 덕분에 중복 저장되지는 않지만, 동일 스레드에서 불필요하게 동일 로직을 연달아 수행하므로 호출 플래그나 조건을 명확히 정리하는 것이 좋습니다.
3. **path: alias 충돌 처리 주석 및 로깅 보강**:
 - [MainActivity.java:2202-2204](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java#L2202-L2204) 분기에 이미 다른 stable ID에 선점된 alias는 덮어쓰지 않고 exact link 저장으로 진행한다는 의도를 주석으로 명시하여 가독성을 높여주세요.

### 후속으로 둘 것 (Follow-ups)
1. **기존 앨범 이력의 OrganizationLink Backfill 정책 수립**:
 - TODO.md에 명시된 대로, 이름만으로 자동 매핑하지 않고 exact identity가 확인되는 경우에 한해 점진적 백필 규칙을 정의합니다.
2. **MemoryRepository와의 연동**:
 - MemoryRepository에서 indLatestUsableLink를 우선 조회하여 발견 기억과 Gallery 앨범을 연결하는 1차 vertical slice 구현.

---

## 5. Test Gaps (테스트 보완 필요 항목)

1. **persistOrganizationResult 단위/통합 테스트**:
 - 현재 MainActivity 내의 private 메서드로 구현되어 있어, saveAlbumSummaryHistory -> egisterAlias -> commit(link)의 3단계 협업 및 실패 시의 롤백/재시도 분기가 독립적으로 단위 테스트되지 못하고 있습니다.
 - (패키지 프라이빗 헬퍼나 별도 Coordinator 클래스로 추출하여 테스트하거나, MainActivity 테스트에서 모의 시나리오 검증 필요).
2. **OrganizationLinkFromSortResult 대소문자/구분자 경로 테스트**:
 - equest.relativePath와 item.targetRelativePath의 끝 슬래시 유무 차이(Pictures/Place vs Pictures/Place/)가 
ormalizePath에 의해 일치 판정되는지 명시적 테스트 필요.
3. **AlbumSummaryHistoryStore의 동일 equestId 2회 연속 호출 멱등성 실증 테스트**:
 - 세션 목록 길이가 늘어나지 않고 1개만 유지되는지 확인하는 테스트.

---

## 6. 최종 판단

### **수정 후 승인 (Conditional Approval)**

- **총평:** 
 정리 Worker 파이프라인 전체를 관통하는 메타데이터 전달(OrganizationRequest), 성공 결과에 대한 엄격한 필터링(OrganizationLinkFromSortResult), 각 영속 저장소의 멱등적 업데이트 흐름은 아키텍처 관점에서 매우 훌륭하고 정교하게 완성되었습니다.
- 다만, **[Blocker 1] 비가역적 오류 발생 시 handleBackgroundSortResultIfAvailable이 2초 간격으로 무한 재시도하는 루프 취약점**은 실제 CTA를 열어 사용자가 기능을 실행하기 전에 반드시 탈출 조건(최대 재시도 횟수 제한 또는 영구 오류 시 폴백 처리)을 보완해야 합니다.
