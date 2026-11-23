# PhotoPlace Single-Place Organization & 완료 UX 종합 코드 리뷰

**일자:** 2026-09-13  
**대상:**
- REVIEW_REQUEST_Single_Place_Organization_2026-09-13.md
- 단일 장소 정리(Single-Place Organization) 완료 UX 및 미디어 소스 수명주기
- MainActivity.java, MemoryRepository.java, DiscoverySnapshotLiveFilter.java, MemoryBrowserState.java
- 관련 커밋 및 WORKLOG_2026-09-13.md

**최종 판단:** **조건부 승인 (Conditional Approval - 미디어 리졸버 및 원본 휴지통 Gating 필수)**

---

## 1. 개요 및 핵심 배경

실기기에서 단일 장소 정리(39개 복사, 0개 중복)가 성공적으로 동작함을 확인했습니다. 그러나 정리 완료 후 노출되는 전역 결과 화면(showResultScreen)에서 **원본 사진 휴지통으로 이동을 실행할 경우, 메모리 상세 화면의 사진이 0장으로 증발하는 생명주기 결함(Media Source Gap)**이 확인되었습니다.

또한, 단일 장소라는 맥락에서 진입했음에도 대규모 전역 정리 결과 화면으로 강제 이동되어 사용자 경험이 지나치게 무겁고 복잡해지는 문제가 제기되었습니다. 이에 대해 제품 방향과 12대 집중 검토 항목을 심층 분석했습니다.

---

## 2. 12대 집중 검토 항목 분석 및 답변

### [1~6] 파이프라인 무결성 및 수명주기 검증 (Pass)
1. **사전 확인(preflight) 스코핑:** prepareSingleMemoryAlbum이 Collections.singletonList(liveRecord)만 조회하고, startPreparedDiscoveryAlbums가 sameRelativePath로 모든 아이템의 대상 앨범 경로 일치성을 전수 검증하므로 타 장소 미디어가 유입되지 않습니다.
2. **중복/동영상 카운트 일치성:** Preparation.actionableCount(moveVideos)와 SortJob의 실제 건너뜀 조건이 100% 일치하며, 신규 대상이 없을 때 위치 앨범 탭으로 이동시키는 방어 안내도 적절합니다.
3. **권한 확인 안전성:** 동영상 이동 권한 취소나 Intent 발송 실패 시 pendingOrganizationRequestForPermission과 진행 플래그를 깔끔히 초기화하고 상세 화면으로 안전 복귀합니다.
4. **OrganizationLink 엄격 발행:** 복사 성공이 0건이거나 경로 불일치 시 링크를 생성하지 않으며, mem_<UUID> stable ID와 타깃 앨범 경로를 정확히 보존합니다.
5. **멱등성 및 재처리:** 동일 equestId 재진입 시 세션 및 링크 중복이 차단되며, 영구 실패 시 무한 루프 없이 안전하게 탈출합니다.
6. **Memory 정체성 및 메모 보존:** exactLink를 통해 StoredAlbumSummary와 merge되어 MIXED 타입으로 승격되며, mem_<UUID> 키를 유지하므로 날짜별 메모와 날짜 그룹핑이 보존됩니다.

---

### [7 & 8] Memory Media Resolver와 원본 휴지통 이동 Gating (★ Release Blocker)
- **Q7. Memory media resolver가 원본 정리를 허용하기 전의 릴리즈 블로커인가?**
  - **답변: 네, 100% Release Blocker입니다.**
  - **원인:** 현재 MemoryBrowserDetail은 discoveryGroup.photoRefs만으로 사진 섹션을 만듭니다. 원본을 휴지통으로 보내면 DiscoverySnapshotLiveFilter가 IS_TRASHED=1을 감지해 이들을 라이브 목록에서 제거하므로, **위치 앨범에 사진이 엄연히 살아있음에도 메모리 상세에서는 사진 0장으로 증발**합니다.
  - **원칙:**
    1. 위치 앨범이 연결된 장소: Pictures/<장소>/의 MediaStore 미디어를 우선 읽어와 날짜별 섹션 구성.
    2. 위치 앨범이 없는 발견 장소: 기존대로 발견 원본(DiscoverySnapshot) 사용.
    3. 두 소스를 합집합(Union)하지 않고 **단일 활성 소스만 선택**하여 중복 표시를 방지.
- **Q8. 원본 휴지통 이동을 엄격히 Gating해야 하는가?**
  - **답변: 네, 단순 경고 문구로는 부족하며 코드 레벨에서 차단되어야 합니다.**
  - 원본 휴지통 이동은 다음 두 가지 조건이 **모두 충족될 때만** 활성화되어야 합니다:
    1. 공용 Memory media resolver가 구현되어 위치 앨범 사진으로 상세 뷰가 정상 렌더링됨을 보장할 것.
    2. 해당 정리 작업의 OrganizationLink가 완전한 성공(Status.SUCCESS)으로 저장되었을 것 (부분 성공인 PARTIAL이거나 영속화 실패 시에는 원본 삭제 절대 금지).

---

### [9 & 10] 단일 장소 완료 UX 및 시안 평가
- **Q9. 단일 장소 액션을 전역 결과 화면으로 보내는 것이 적절한가? 최소 안전 변경은?**
  - **답변: 부적절합니다. 가장 안전하고 직관적인 최소 변경은 Memory 상세 화면으로의 즉시 복귀입니다.**
  - 사용자는 특정 장소 상세를 보다가 앨범을 만든 것이므로, 완료 후 다시 그 장소 상세로 돌아가는 것이 가장 자연스럽습니다.
  - **최소 안전 변경 (5줄 수준):**
    MainActivity.handleBackgroundSortResultIfAvailable()에서 snapshot.organizationRequest != null인 경우:
    1. 전역 showResultScreen()을 호출하지 않습니다.
    2. 토스트 안내: <장소> 앨범을 만들었어요 · 사진 N장
    3. 원래 보던 상세 화면으로 직행: showMemoryBrowserDetailScreen(this.activeMemoryKey);
    4. **부수 효과:** 전역 결과 화면에 있는 위험한 원본 사진 휴지통으로 이동 버튼에 사용자가 아예 노출되지 않으므로, 미디어 리졸버 완성 전까지 완벽한 안전 차단벽이 됩니다!
- **Q10. 완료 화면 시안 평가:**
  - **Hero 영역 축소:** 단일 장소에 대형 축하 카드는 과도합니다. 콤팩트한 확인 카드 형태로 축소해야 합니다.
  - **단수형 문구:** 성남 앨범을 만들었어요 · 사진 39장처럼 명확한 단수형 장소/사진 수 표기가 필수적입니다.
  - **버튼 위계:**
    - **Primary:** 확인 또는 장소 상세로 돌아가기 (Memory 정체성 유지)
    - **Secondary:** 위치 앨범 열기 (갤러리) (보조적 물리 앨범 확인)

---

### [11 & 12] 실패/부분 성공 대응 및 릴리즈 전 체크리스트
- **Q11. 영속화 실패 또는 PARTIAL 결과 시의 최소 안전 조치:**
  - OrganizationLink 저장이 실패했거나 결과가 PARTIAL인 경우:
    - 원본 휴지통 이동 액션을 **원천 비활성화/숨김**.
    - 안내 문구: 일부 사진만 정리되었거나 장소 연결을 완료하지 못했습니다. 원본 사진은 안전하게 보존됩니다.
- **Q12. 릴리즈 전 필수 과제:**
  1. **[Blocker]** 단일 장소 정리 후 전역 결과 화면 대신 Memory 상세 화면으로 복귀하도록 변경 (위험한 원본 휴지통 버튼 노출 차단).
  2. **[P1 핵심]** 공용 Memory media resolver 구현 (hasGalleryAlbum 시 Pictures/<장소>/의 MediaStore 쿼리 연결).
  3. **[검증]** 리졸버 구현 후 실제 실기기에서 원본 휴지통 이동 시에도 Memory 상세 사진과 날짜 메모가 온전히 유지되는지 실증.

---

## 3. 권장 실행 순서 (Action Plan)

1. **Step 1 (즉시 안전 조치 - 5분):**
   MainActivity에서 단일 장소 정리 완료 시 전역 결과 화면을 건너뛰고 토스트와 함께 Memory 상세 화면(showMemoryBrowserDetailScreen)으로 바로 복귀하도록 라우팅 분기.
2. **Step 2 (핵심 resolver 구현):**
   TODO.md의 공용 Memory media resolver 작성: 위치 앨범 경로의 MediaStore 사진/동영상을 날짜순으로 쿼리하여 MemoryPhotoSection 생성.
3. **Step 3 (실기기 최종 검증):**
   - 단일 장소 정리 후 Memory 상세 진입 ➡️ 위치 앨범 사진 노출 확인.
   - 원본 휴지통 이동 실행 ➡️ Memory 상세 사진 및 날짜 메모 유지 확인.

---

## 4. Follow-up: 전용 완료 화면 반영

2026-09-13 후속 구현에서 Step 1은 즉시 상세로 점프하는 대신, 사용자가 최종 화면으로 기대한 첫 번째 시안 구조를 축소해 반영했다.

- 새 `MemoryOrganizationCompletionRenderer`를 분리해 크기를 줄인 완료 요약, 장소명/정리 수, 썸네일이 있는 앨범 행을 표시한다.
- Primary는 `위치 앨범 보기`, secondary는 `기억으로 돌아가기`다.
- 한 장소 정리 Worker 실행 중에는 전체 정리 결과 화면을 거치지 않고 Memory 상세에 머문 뒤, 완료 시 전용 화면을 연다.
- 성공, 새 항목 0건, 취소, 일부 실패, OrganizationLink 저장 실패 문구를 구분한다.
- 단일 Memory 결과에서는 복사한 원본 URI를 휴지통 대기 목록에 넣지 않으며, 전용 화면에도 원본 삭제 액션을 표시하지 않는다.
- bulk 정리의 기존 일반 결과 UI는 변경하지 않았다.
- 단위 테스트 및 debug 빌드는 통과했다. 전용 화면의 실기기 표시/복귀/탭 이동 확인은 아직 필요하다.
- 공용 Memory media resolver는 미구현이므로 원본 삭제 테스트와 릴리즈 승인은 여전히 보류한다.
