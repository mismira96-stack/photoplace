# PhotoPlace Memory Collection UI 슬라이스 코드 리뷰

**일자:** 2026-09-13  
**대상:**
- `MemoryCollectionMembership.java` (컬렉션 멤버십 투영 헬퍼)
- `MemoryCollectionMembershipTest.java` (멤버십 필터링 및 누락 alias 방어 테스트)
- `MainActivity.java` (기억 모으기 선택 화면, 내 기억 모음 카드, 모음 상세 화면, 검색 투영, 이름 변경/해제)
- `TODO.md` 및 `WORKLOG_2026-09-13.md`

**최종 판단:** **승인 (Approved - P1 Collection UI 슬라이스 체크포인트 완성, 실기기 스모크 권장)**

---

## 1. 개요 및 구현 범위

이번 작업은 P1 마일스톤 중 **Memory Collection(기억 모음) UI의 첫 번째 수직 슬라이스(Vertical Slice)**를 구현했습니다. 기존에 이미 완성되어 있던 `MemoryCollectionStore` 및 `MemoryCollectionResolver`의 도메인 로직을 실제 UI와 유기적으로 연결했습니다:

1. **기억 모으기 선택 화면 (`showMemoryCollectionSelectionScreen`):**
   - 발견된 장소 중 2곳 이상을 체크박스로 선택하여 하나의 모음으로 생성.
   - 이미 다른 활성 모음에 속한 장소는 선택이 비활성화되며 `"다른 모음에 포함됨"` 안내 표시.
   - 제목 입력 팝업을 거쳐 `mem_<UUID>` stable ID 매핑과 함께 `group_<UUID>` 컬렉션으로 저장.
2. **목록 내 기억 모음 카드 & 장소 접기 (`addMemoryCollectionCards`, `hideCollectedMemoryRecords`):**
   - 기본 발견 목록 상단에 `내 기억 모음` 카드 렌더링 (대표 커버 사진, 고유 사진/동영상 수, 포함된 장소명, 전체 날짜 범위).
   - 모음에 묶인 장소들은 기본 목록 그리드에서 깔끔하게 접힘(숨김).
3. **검색 시 개별 장소 접근성 보존:**
   - 검색창에 검색어 입력 시 `hideCollectedMemoryRecords`를 건너뛰어, 모음에 포함된 원래 장소도 검색을 통해 언제든 개별로 열 수 있도록 보장.
4. **모음 상세 화면 (`showMemoryCollectionDetailScreen`):**
   - 계층 구조: `날짜 -> 장소 -> 기존 날짜 메모 -> 사진` 순서로 깔끔하게 렌더링.
   - 48장 단위 점진적 사진 확장(`addGroupMemoryPhotos`).
   - 컬렉션 이름 변경(`rename`) 및 해제(`dissolve`) 지원.
5. **파일 및 갤러리 100% 안전 보존:**
   - 갤러리 앨범이나 원본 미디어 파일, 날짜 메모는 일체 변경/삭제되지 않는 순수 메타데이터 관리 작업.

---

## 2. 세부 검토 및 분석

### ① 선택 흐름 및 불변식 검증 (Pass - 완벽함)
- **최소 인원 가드:** `selectableRecords.size() < MemoryCollection.MIN_MEMBER_COUNT(2)` 및 선택 수량 `< 2`일 때 버튼 비활성화와 안내 문구(`"2곳 이상 선택해 주세요"`)가 정확히 동작합니다.
- **단일 소속(One Active Collection) 제약:** `MemoryCollectionMembership.stableIds()`를 조회하여 이미 묶인 장소는 체크박스를 `setEnabled(false)` 처리하여 중복 소속을 원천 방어합니다.
- **Stable ID 바인딩:** 모음 저장 시 `memoryIdentityRegistryStore().resolveOrCreate(record.memoryKey)`를 호출하여 발견 별칭(`discovery:<place>`)을 영구 불변 식별자(`mem_<UUID>`)로 변환한 뒤 `MemoryCollection.Member`로 저장하므로, 장소명이 변경되더라도 소속 관계가 유지됩니다.

### ② 메인 목록과 검색 투영 분리 (Pass - 우수함)
- `MemoryCollectionMembership.ungroupedRecords()`를 순수 함수형 유틸리티로 깔끔히 분리하고, 단위 테스트(`MemoryCollectionMembershipTest`)를 통해 묶인 멤버만 정확히 걸러내고 매핑 정보가 없거나 모음에 없는 장소는 보존함을 검증했습니다.
- 검색 모드(`searching == true`)일 때는 필터링을 건너뛰어 사용자가 특정 장소를 찾고 싶을 때 모음 소속 여부와 무관하게 접근할 수 있도록 한 UX 판단이 매우 훌륭합니다.

### ③ 모음 상세 및 메모 보존 (Pass - 우수함)
- `MemoryCollectionResolver`를 재사용하여 여러 장소의 미디어를 날짜순으로 재그룹화하면서도, 각 장소 밑에 사용자가 작성해 둔 고유 날짜 메모(`place.noteText`)를 유실 없이 온전히 노출합니다.
- 해제(`dissolve`) 시 `AlertDialog`로 사용자 확인을 거치며, "원래 장소와 사진, 날짜 메모는 삭제되지 않아요"라는 명확한 안내 후 메타데이터만 삭제하므로 데이터 안정성이 뛰어납니다.

### ④ 개선 권고 사항 반영 완료 (화면 회전 복원 분기 완벽 적용)
- 리뷰에서 지적되었던 `refreshCurrentScreenForLayoutChange()` 내 모음 상세(`memoryCollectionDetailMode`) 및 선택 화면(`memoryCollectionSelectionMode`) 복원 분기가 완벽히 추가되었습니다.
- **세부 보완점:**
  1. `beginMemoryCollectionSelection()`을 분리하여 메인에서 새로 진입할 때는 선택 상태를 초기화하고, 화면 회전(`showMemoryCollectionSelectionScreen()`) 시에는 체크 상태와 CTA 버튼 카운트를 온전히 유지합니다.
  2. 회전 후 재조회 시 더 이상 사용할 수 없는 장소는 `this.selectedMemoryCollectionKeys.retainAll(currentKeys)`로 안전하게 필터링합니다.
  3. 모음 상세 화면 또한 회전 시 보고 있던 `activeMemoryCollectionId`를 그대로 유지하여 다시 로드합니다.

---

## 3. 검증 결과

- **`./gradlew.bat testDebugUnitTest`:** **통과 (BUILD SUCCESSFUL in 3s, 20 tasks)**
- **`./gradlew.bat assembleDebug`:** **통과 (최신 디버그 APK 빌드 완료)**
- **`git diff --check`:** **통과 (공백 및 포맷 결함 없음)**

---

## 4. 최종 결론

- **최종 판정:** **승인 (Approved)**
- `MemoryCollection`의 핵심 가치인 '여러 장소를 하나의 여행/기억으로 묶어 날짜순으로 감상'하는 사용자 흐름이 군더더기 없이 구현되었습니다.
- 원본 파일이나 기존 갤러리를 전혀 건드리지 않는 순수 메타데이터 기능이므로 안전성이 매우 높습니다.
- 워크로그에 명시된 대로 공용 Memory media resolver 이전의 훌륭한 체크포인트이며, 실기기 화면 스모크 테스트를 진행하셔도 좋습니다!
