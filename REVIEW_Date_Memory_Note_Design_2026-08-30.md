# PhotoPlace 날짜별 Memory Note 설계 리뷰 (2026-08-30)

- **대상 커밋**: `dab14a3 Plan date-based memory notes`
- **참조 문서**: `TODO.md` (`P1 - 날짜별 Memory Note`)
- **검토 코드**: `MemoryRepository`, `MemoryRecord`, `MemoryPhotoSection`, `MemoryPersonalizationKey`
- **작성일**: 2026-08-30
- **상태**: **승인 (Approved with Identity Registry Refinement)**

---

## 1. 종합 결론 및 아키텍처 합의

Gemini의 `독립 MemoryDateNoteStore + tmp/bak 원자 저장`과 Codex의 `Memory Identity Registry (내부 Stable ID + Alias)` 구조를 결합하여 최종 아키텍처를 확정합니다.

장소명이나 파일 경로를 직접 메모 저장 키로 쓰지 않고, **내부 `stable-id`를 발급하여 메모의 영구 키로 사용**하고 `discovery:<placeKey>`와 `path:<relativePath>`는 **관찰 Alias(별칭)**로 매핑합니다.

```
[UI / Discovery Detail]                [Location Album (추후)]
        │                                        │
        ▼ (alias: discovery:삿포로)               ▼ (alias: path:Pictures/삿포로에서)
┌───────────────────────────────────────────────────────────┐
│              Memory Identity Registry                     │
│   discovery:삿포로  ────────┐                              │
│   path:Pictures/삿포로에서 ─┼─► stable-id: "mem_2026_0802_a1" │
└─────────────────────────────┼─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                 MemoryDateNoteStore                       │
│   Key  : "memory-id:mem_2026_0802_a1#20260802"           │
│   Value: { text: "...", createdAt: ..., updatedAt: ... }  │
└───────────────────────────────────────────────────────────┘
```

---

## 2. 핵심 설계 보완 사항 (Identity Registry & Stable ID)

### ① 메모 저장 키 포맷
- **포맷**: `memory-id:<stable-id>#yyyyMMdd`
  - 예: `memory-id:mem_01J6X9#20260802`
- **장점**:
  - 장소명 변경, Geocoder 재분류, 물리 앨범 이동/통합이 발생해도 **메모 저장소(`memory_date_notes.json`)를 다시 뜯거나 마이그레이션할 필요가 완전히 사라짐.**
  - 문자열 파싱이나 지명 변경에 영향을 받지 않는 영구적인 불변 키 보장.

### ② Memory Identity Registry & Alias 정책 (Phase 0)
1. **최초 발급**: 발견 기록에서 새로운 논리 장소가 처음 감지될 때 내부 `stable-id`를 생성하고 `discovery:<placeKey>`를 alias로 등록.
2. **앨범 생성/이동 연계**: 발견 기록 $\rightarrow$ 위치 앨범 생성, 또는 위치 앨범 물리 이동은 **앱이 직접 실행하는 명시적 이벤트**이므로, 이때 새 `path:<relativePath>`를 동일한 `stable-id`의 alias로 안전하게 추가/갱신.
3. **자동 병합 억제**: Geocoder 재분류처럼 시스템이 "동일 장소"라고 100% 확신하기 어려운 경우는 임의로 자동 병합하지 않고 독립 identity를 유지.

### ③ 독립 `MemoryDateNoteStore` 영속화 (Phase 1)
- 파일명: `memory_date_notes.json`
- `MediaAnalysisStore`와 동일한 `tmp` $\rightarrow$ `bak` 원자적(Atomic) 파일 쓰기 및 백업 복원 패턴 적용.
- 손상 파일 발생 시 빈 값으로 덮어쓰지 않고 쓰기를 안전하게 억제(`canSafelyWrite()`).

---

## 3. UI 및 MVP 범위 확정 (MVP Scope)

### ✅ Phase 1~2: MVP 구현 범위 (현재 목표)
1. **Phase 0**: Memory Identity Registry (`stable-id` 발급 및 alias 맵) 기반 마련
2. **Phase 1**: `MemoryDateNoteStore` (`memory_date_notes.json` + 단위 테스트)
3. **Phase 2**: **발견 상세 화면**의 날짜 헤더 아래 1줄 메모 뷰 & 작성/수정/삭제 다이얼로그

### ⏸️ Phase 3 이후 후속 과제 (보류)
- **위치 앨범 상세 뷰어 메모 연결**: 데이터 모델과 Alias는 이번에 준비해 두되, UI 연결은 발견 상세 MVP 안정화 후 별도 단계로 진행.
- **홈 화면 `기억을 꺼내보기`**: 상세 메모 작성/조회 실기기 검증 후 진행.
- **사진/동영상 첨부 메모**: 1줄 텍스트 안정화 후 검토.

---

## 4. 라이프사이클 검증 시나리오

1. **재분석 보존**: 동일 폴더 재분석 시 `discovery:<placeKey>` $\rightarrow$ `stable-id` 매핑이 유지되어 메모 정상 노출.
2. **새 미디어 추가**: 동일 날짜에 새 사진이 추가되어도 `stable-id#yyyyMMdd` 메모 유지.
3. **라이브 필터 제외**: 일부 사진이 갤러리에서 삭제되어도 메모는 스토어에 보존.
4. **앨범 생성 시 Alias 갱신**: 발견에서 위치 앨범 생성 시 `path:...` alias가 등록되어 차후 앨범 뷰어 연결 준비 완료.

---

## 5. 최종 승인 상태

> 🟢 **설계 리뷰 최종 승인 (Approved)**  
> 위 Identity Registry + Stable ID 구조로 Phase 0 (Key/Registry 계약) $\rightarrow$ Phase 1 (Store) $\rightarrow$ Phase 2 (발견 상세 UI) 순서로 구현을 시작합니다.
