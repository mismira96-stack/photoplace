# PhotoPlace Memory Grouping Phase 1 & 2 코드 리뷰 (2026-09-05)

- **대상 커밋**:
  - `80dd08f Add memory collection store` (Phase 1)
  - `3de1fa8 Add memory collection resolver` (Phase 2)
- **참조 문서**: `MEMORY_GROUPING_SPIKE_2026-09-05.md`, `WORKLOG_2026-09-05.md`, `TODO.md`
- **작성일**: 2026-09-05
- **리뷰 주체**: Gemini (Antigravity) $\rightarrow$ For Codex
- **상태**: **승인 (Approved - Production Ready)**

---

## 1. 종합 평가

Phase 1(저장소 계층)과 Phase 2(읽기 전용 Resolver 계층)가 스파이크 설계 문서의 핵심 원칙을 **단 한 줄의 타협 없이 완벽하게 구현**했습니다.

1. **불변 불변식 & 1장소 1그룹 제약**: 스토어 레벨에서 `hasMemberConflict`, 최소 2개 고유 멤버 검증(`MIN_MEMBER_COUNT`)을 강제하여 데이터 오염을 사전 차단함.
2. **동일 날짜 내 장소별 메모 보존 (No Flattening)**: `GroupMemoryDateSection` $\rightarrow$ `GroupMemoryPlaceSection` 계층을 도입하여 같은 날짜(`8월 5일`)에 삿포로와 오타루가 함께 있어도 각 장소의 독립된 날짜 메모가 완벽히 보존됨.
3. **사진 URI 중복 제거 (`seenUris`)**: 컬렉션 범위에서 `sourceUri` 기준 deduplicate를 수행하여 다중 장소에 걸친 동일 사진의 중복 노출을 방어함.
4. **결함 방어 렌더링**: 삭제/유실된 장소 멤버가 있어도 크래시 없이 사용 가능한 멤버만으로 안전하게 그룹 뷰를 구성함.

---

## 2. 핵심 구현 상세 검토

| 검토 항목 | 결과 | 상세 분석 내용 |
| :--- | :---: | :--- |
| **① 1장소 1그룹 제약 (`hasMemberConflict`)** | **완벽** | 새 컬렉션 생성 시 기존 활성 그룹들의 모든 `stableMemoryId`와 대조하여, 이미 그룹에 속한 장소는 중복 가입을 엄격히 거부함. |
| **② 그룹 해제(`dissolve`) 격리** | **완벽** | `memory_collections.json`에서 그룹 메타데이터만 제거되며, 원본 장소, 미디어 레퍼런스, 날짜별 메모는 100% 보존됨. |
| **③ 장소별 메모 분리 (`GroupMemoryPlaceSection`)** | **완벽** | 같은 날짜 내에서도 장소별 `noteText`("징기스칸", "운하 산책")가 독립적으로 바인딩되어 메모 키와 소유권이 유지됨. |
| **④ 원자적 저장 및 손상 방어 (`canSafelyWrite`)** | **완벽** | `tmp` $\rightarrow$ `bak` 원자 파일 교체와 손상 파일 시 백업 복원, 백업마저 손상된 경우 덮어쓰기 거부 정책 완비. |
| **⑤ 날짜 최신순 정렬 (`yyyyMMdd` Descending)** | **완벽** | `DateBuilder.dateKey`를 내림차순 정렬하여 가장 최근 날짜의 사진/메모가 상단에 먼저 오도록 보장함. |

---

## 3. 단위 테스트 검증 내역

- **`MemoryCollectionStoreTest` (5개 테스트 통과)**:
  - `createsAndReadsStableMembersWithoutChangingAliases`: 생성 및 조회 검증
  - `onePlaceCanBelongToOnlyOneActiveCollection`: 1장소 1그룹 제약 검증
  - `renameAndDissolveOnlyChangeCollectionMetadata`: 이름 변경 및 안전 해제 검증
  - `invalidTitleOrFewerThanTwoDistinctMembersAreRejected`: 입력 유효성 검증
  - `corruptMainFileRestoresBackupAndUnsafeFileIsNotOverwritten`: 백업 복구 및 쓰기 거부 검증
- **`MemoryCollectionResolverTest` (3개 테스트 통과)**:
  - `keepsEachPlacesDateNoteWhenTwoPlacesShareADate`: 동일 날짜 다중 장소 메모 독립 보존 검증
  - `groupsDatesNewestFirstAndDeduplicatesSourceUrisAcrossPlaces`: 최신 날짜순 정렬 및 URI 중복 제거 검증
  - `ignoresUnavailableMembersWithoutDestroyingTheCollectionProjection`: 유실 장소 방어 렌더링 검증

---

## 4. 다음 단계 권장 사항 (Phase 3 & Phase 4)

```mermaid
graph LR
    subgraph Phase1_2 [Phase 1 & 2 (완료)]
        S["MemoryCollectionStore"]
        R["MemoryCollectionResolver"]
        D["GroupMemoryDetail"]
        S --> R --> D
    end

    subgraph Phase3 [Phase 3 (다음 단계)]
        D --> V["그룹 상세 화면 UI<br/>(Date Header -> Place SubHeader -> Photo Grid)"]
        V --> M["장소별 날짜 메모 편집/수정 연동"]
    end

    subgraph Phase4 [Phase 4 (후속)]
        V --> UI["발견 목록 다중 선택 UI<br/>(2개 이상 장소 선택 -> 그룹 생성 Dialog)"]
    end
```

### 💡 Phase 3 구현 가이드
1. **서브헤더 시각적 계층감**:
   - 날짜 헤더(`8월 5일`)는 기존 스타일 유지.
   - 장소 서브헤더(`삿포로 · 12장`, `오타루 · 8장`)는 저채도 칩(Chip) 또는 서브 타이틀로 계층 분리.
2. **메모 편집 재사용**:
   - 장소 서브헤더 옆의 `+ 메모`/`수정` 클릭 시 기존의 `showSingleLineMemoryEditor`를 그대로 호출 (`stableMemoryId + dateKey` 전달).
3. **페이징 고려**:
   - 대용량 사진 렌더링 시 기존 `MemoryPhotoPage` 48장 페이징 슬라이스 방식 유지.
