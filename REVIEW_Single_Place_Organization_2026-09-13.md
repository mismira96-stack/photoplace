# PhotoPlace Single-Place Organization 코드 리뷰

**일자:** 2026-09-13  
**대상:**
- Discovery Memory -> Gallery 위치 앨범 단일 장소 정리 구현
- MainActivity.java, DiscoveryAlbumOrganizer.java, DiscoveryOrganizeConfirmDialog.java
- MemoryRepository.java, MemoryIdentityRegistryStore.java, DiscoverySnapshotController.java
- OrganizationResultRetryPolicy.java, OrganizationLinkFromSortResult.java
- 관련 테스트 코드 및 TODO.md / WORKLOG_2026-09-13.md

**리뷰 기준 브랜치:** codex/photoplace-v2-bg-wip  
**최종 판단:** **승인 (Approved - 실기기 검증 진행 권장)**  

---

## 1. 개요 및 구현 범위

이번 구현은 전체 장소 일괄 정리 외에, Memory 상세 화면에서 사용자가 선택한 **특정 한 장소만 위치 앨범으로 만드는 1차 vertical slice**를 완성했습니다.
사전 확인(preflight) 시 해당 장소만 live-filter하고, MediaStore 중복 검사를 거쳐 OrganizationRequest에 stable Memory ID와 타깃 앨범 경로를 바인딩하며, WorkManager 정리가 성공한 경우에만 OrganizationLink를 발행하여 MemoryRepository가 exact link로 결합하도록 파이프라인 전체를 연결했습니다.

---

## 2. 7대 집중 리뷰 항목 검토 결과

### ① 사전 확인(preflight) 시 선택된 장소의 미디어만 Worker에 입력되는가?
- **판정: 완벽함 (Pass)**
- **분석:**
  - prepareSingleMemoryAlbum()에서 loadMemoryRepository().memory(memoryKey)로 현재 시점의 live 레코드를 다시 조회합니다.
  - DiscoveryAlbumOrganizer.prepare(Collections.singletonList(liveRecord), ...)를 통해 오직 해당 1개 장소의 사진/동영상만 Preparation으로 변환됩니다.
  - startPreparedDiscoveryAlbums()에서 준비된 모든 아이템의 	argetRelativePath가 동일한지(sameRelativePath) 전수 검사하여 단일 앨범 경로 무결성을 검증한 뒤 	his.previewItems로 넘겨 Worker에 전달합니다. 타 장소의 미디어가 유입될 여지가 원천 차단되어 있습니다.

### ② 중복 ref 및 동영상 제외 처리가 확인 팝업, PhotoItem, Worker 간 일치하는가?
- **판정: 일치함 (Pass)**
- **분석:**
  - DiscoveryAlbumOrganizer.Preparation.actionableCount(moveVideos):
    !item.duplicateInTarget && (!item.video || moveVideos) 항목만 카운트.
  - SortJob.java:
    동영상 이동 미허용(item.video && !shouldMoveVideos)이거나 이미 존재하는 경우(item.duplicateInTarget) 정확히 skippedCount++ 처리.
  - DiscoveryOrganizeConfirmDialog:
    동영상 정리 옵션이 꺼져 있을 때 제외되는 동영상 수를 분리 표시하고, 이미 존재하는 항목(duplicateCount)과 접근 불가 항목(skippedRefCount)을 명확히 고지합니다.
  - 이미 모든 파일이 앨범에 존재하여 ctionableCount <= 0인 경우, Worker를 실행하지 않고 위치 앨범 탭으로 안내하는 방어 로직(
avigateToTopLevelTab(2))이 매우 훌륭합니다.

### ③ 권한 요청 흐름(동의, 취소, 실패, 액티비티 재생성)에서 상태 유실이나 가짜 진행이 발생하는가?
- **판정: 안전함 (Pass)**
- **분석:**
  - pendingOrganizationRequestForPermission에 요청 객체를 보존한 후 MediaStore.createWriteRequest를 호출합니다.
  - **권한 거부/취소 시:** onActivityResult에서 pendingOrganizationRequestForPermission = null, discoveryOrganizePendingStart = false를 초기화하고 Memory 상세 화면으로 안전하게 복귀합니다.
  - **Intent 발송 실패 시:** SendIntentException 및 일반 Exception을 모두 catch하여 상태를 초기화하고 에러 토스트 후 상세 화면으로 복귀합니다.
  - **액티비티 재생성:** MainActivity는 AndroidManifest.xml에 화면 회전 및 크기 변경에 대한 configChanges가 선언되어 있어 회전 시 액티비티가 파괴되지 않으며, 프로세스 킬 후 복귀 시에도 previewItems.isEmpty() 가드로 인해 비정상 시작이 방어됩니다.

### ④ OrganizationLink가 확인된 성공 결과와 정확한 stable ID/경로로만 생성되는가?
- **판정: 완벽함 (Pass)**
- **분석:**
  - memoryAlias로부터 MemoryIdentityRegistryStore.resolveOrCreate()를 통해 mem_<UUID> stable ID를 확보한 후 OrganizationRequest를 생성합니다.
  - Worker 완료 후 OrganizationLinkFromSortResult.create()에서:
    - esult.copiedCount > 0
    - esult.sortedItems.size() == result.copiedCount
    - 모든 항목의 	argetRelativePath == request.relativePath
    를 엄격히 검증한 뒤에만 링크를 생성합니다.
  - 복사 성공이 0건이거나 경로가 다른 항목이 섞여 있으면 링크를 생성하지 않습니다.

### ⑤ 부분 실패, 재시도, 재생복 시 왜곡된 상태나 중복 기록이 생기는가?
- **판정: 완벽함 (Pass)**
- **분석:**
  - 일부 파일만 복사되고 실패/취소된 경우 Status.PARTIAL 링크를 발행하여 Gallery output의 불완전성을 정확히 반영합니다.
  - 이전 리뷰 피드백이었던 **무한 재시도 루프**가 OrganizationResultRetryPolicy(최대 3회 제한)와 OrganizationPersistenceResult(영구 실패 시 탈출 및 결과 화면 표시)로 완벽히 보완되었습니다.
  - 동일 equestId의 결과가 재수신되어도 AlbumSummaryHistoryStore의 containsRequestId, MemoryOrganizationLinkStore의 ALREADY_COMMITTED로 인해 중복 기록이 일체 발생하지 않습니다.

### ⑥ 정리 후 Memory 정체성, 날짜별 그룹화, 날짜 메모가 온전히 보존되는가?
- **판정: 보존됨 (Pass)**
- **분석:**
  - MemoryRepository.memories()에서 exactLink가 존재하는 경우 정확히 일치하는 StoredAlbumSummary와 merge하여 MemorySourceType.MIXED로 승격합니다.
  - discoveryGroup이 유지되므로 날짜별 미디어 그룹화와 원본 사진 참조가 그대로 유지됩니다.
  - MemoryDateNoteStore는 mem_<UUID> stable ID를 키로 사용하므로, 앨범 정리 후에도 날짜별 메모가 유실되지 않고 동일하게 조회됩니다.
  - 링크된 앨범이 실제 MediaStore에 존재하지 않는 경우, 동명의 다른 앨범으로 잘못 합쳐지지 않도록 휴리스틱 매칭을 차단하는 방어 로직(exactLink != null일 때 fallback 억제)이 탁월합니다.

### ⑦ 코드 레벨의 블로커나 누락 사항이 있는가?
- **판정: 블로커 없음 (No Blockers)**
- **분석:**
  - 모든 단위 테스트(	estDebugUnitTest), 빌드(ssembleDebug), 코드 포맷(git diff --check)이 통과되었습니다.
  - 단위 컴포넌트별 테스트(경로 정규화, 재시도 횟수 경계, exact link 우선 매칭 및 누락 앨범 매칭 차단, 동영상 설정별 카운트 계산)가 촘촘하게 추가되었습니다.

---

## 3. 권고사항 (실기기 테스트 시 체크포인트)

현재 코드 레벨의 블로커는 없으며, 실기기 스모크 테스트 시 다음 사용자 시나리오를 중점적으로 확인하는 것을 권장합니다:

1. **단일 장소 정리 후 화면 갱신 검증:**
   - 정리 완료 후 Home의 최근 발견한 장소 및 해외 상세 화면에서 해당 장소가 정리된 위치 앨범과 올바르게 연결되어 노출되는지.
   - Memory 상세 화면에 진입했을 때 기존에 작성해 둔 날짜별 메모가 그대로 유지되는지.
2. **동영상 이동 권한 취소 시나리오:**
   - 동영상 이동 권한 확인 팝업에서 취소를 눌렀을 때, Memory 상세 화면으로 깔끔하게 복귀하고 프로그레스 바가 남지 않는지.
3. **재정리 방어 안내:**
   - 이미 위치 앨범으로 정리된 장소 상세에서 다시 이 장소만 위치 앨범으로 만들기를 눌렀을 때, 새로 정리할 항목이 없어요 토스트와 함께 위치 앨범 탭으로 자연스럽게 이동하는지.

---

## 4. 최종 결론

- **최종 판단: 승인 (Approved)**
- 코드 설계와 예외 방어, 생명주기 연결이 매우 정교하게 구현되었습니다. 실기기 기능 및 UX 스모크 테스트를 진행하셔도 좋습니다!
