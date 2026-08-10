# Commit 9e51a2c 리뷰 결과

## 리뷰 대상

- Commit: `9e51a2c Harden discovery snapshot model validation`
- 이전 기준 커밋: `6020569 Add display-first discovery memory models`
- 목적: Discovery snapshot 모델과 JSON 복원 validation 보강

## 결론

이번 `9e51a2c` 커밋은 이전 리뷰에서 지적한 항목을 적절히 반영했다.

현재 새로운 High 또는 Medium 수준의 기능 결함은 확인되지 않았다.

변경 방향:

```text
6020569
  -> Display First 모델 뼈대 추가

9e51a2c
  -> 빈 ref/group validation
  -> photo/video/stale count 재계산
  -> MemoryRecord sourceType 추론 보강
  -> 관련 테스트 추가
  -> worklog 검증 기록 정정
```

기존 Gallery Organizer 흐름과 `MainActivity`를 건드리지 않은 상태도 유지되고 있다.

---

## 이전 리뷰 항목 반영 확인

### 1. 빈 `sourceUri` ref 제외

파일:

- `app/src/main/java/com/example/gallerysorter/DiscoverySnapshotJson.java`
- `app/src/test/java/com/example/gallerysorter/DiscoverySnapshotJsonTest.java`

`photoRefFromJson()`은 `sourceUri`가 비어 있으면 `null`을 반환한다.

```text
sourceUri가 비어 있음
  -> DiscoveryPhotoRef 생성하지 않음
  -> group의 photoRefs에 추가하지 않음
```

이후 group 복원 과정에서도 null ref는 목록에 들어가지 않는다.

관련 테스트:

```text
fromJsonSkipsInvalidRefsAndGroups
```

---

### 2. `memoryKey` / `placeKey` 없는 group 제외

파일:

- `app/src/main/java/com/example/gallerysorter/DiscoverySnapshotJson.java`
- `app/src/test/java/com/example/gallerysorter/DiscoverySnapshotJsonTest.java`

group 복원 시 다음 validation이 적용된다.

```text
memoryKey가 비어 있음
  -> group 제외

placeKey가 비어 있음
  -> group 제외
```

또한 다음 group도 제외된다.

```text
photoRefs가 비어 있음
itemCount <= 0
```

이렇게 하면 외부 JSON이나 손상된 snapshot으로부터 식별할 수 없는 group이 내부 Memory 모델에 들어오는 것을 막을 수 있다.

---

### 3. photo/video/stale count 재계산

파일:

```text
app/src/main/java/com/example/gallerysorter/DiscoverySnapshotJson.java
```

`photoRefs`가 하나라도 있으면 저장된 count를 신뢰하지 않고 ref 기준으로 재계산한다.

```text
itemCount  = photoRefs.size()
photoCount = PHOTO ref 수
videoCount = VIDEO ref 수
staleCount = stale ref 수
```

예를 들어 JSON이 다음과 같아도:

```text
itemCount = 99
photoCount = 0
videoCount = 0
staleCount = 0
photoRefs = PHOTO 1개 + VIDEO 1개
```

복원 결과는 다음과 같다.

```text
itemCount = 2
photoCount = 1
videoCount = 1
staleCount = 실제 stale ref 수
```

관련 테스트:

```text
fromJsonRecalculatesCountsWhenPhotoRefsExist
```

이전 리뷰에서 지적했던 count 불일치 문제가 해결되었다.

---

### 4. summary-only group 처리

`photoRefs`가 비어 있어도 `itemCount > 0`이면 group을 유지한다.

```text
photoRefs = []
itemCount = 3
  -> group 유지
```

반대로:

```text
photoRefs = []
itemCount = 0
  -> group 제외
```

이 정책은 향후 legacy summary나 상세 ref를 lazy하게 로드하는 구조를 허용한다.

다만 이 동작은 다음 Store 단계에서 명시적인 테스트로 고정하는 것이 좋다.

---

### 5. `MemoryRecord.sourceType` 추론

파일:

```text
app/src/main/java/com/example/gallerysorter/MemoryRecord.java
```

이전에는 `sourceType == null`이면 무조건 `DISCOVERED_ONLY`로 처리되었다.

현재는 discovery/organized 객체의 존재 여부를 이용해 추론한다.

```text
discoveryGroup 있음 + organizedAlbum 있음
  -> MIXED

discoveryGroup 없음 + organizedAlbum 있음
  -> ORGANIZED_ALBUM

discoveryGroup 있음 + organizedAlbum 없음
  -> DISCOVERED_ONLY
```

따라서 adapter가 sourceType을 전달하지 않아도 organized album이 있는 경우 `DISCOVERED_ONLY`로 잘못 숨겨지지 않는다.

현재 모델 단계에서의 null 방어로 적절한 구현이다.

---

### 6. source signature MVP 제외 결정 문서화

파일:

```text
WORKLOG_2026-08-10.md
```

`sourceBucketId/source signature`는 MVP에서 제외하고, 향후 stale detection 또는 incremental sync가 필요할 때 schema version을 올려 추가한다는 결정이 기록되었다.

현재 MVP 범위와 일치한다.

```text
MVP:
  sourceUri
  mediaStoreId
  displayName
  takenAtMillis
  sourceRelativePath

후속:
  source signature
  incremental sync
  복잡한 stale 상태
```

---

### 7. 검증 기록 정정

파일:

```text
WORKLOG_2026-08-10.md
```

다음 검증 기록이 현재 저장소 문서에 반영되었다.

```text
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebug
```

이전의 `assembleDebug Not run yet` 기록은 정정되었다.

사용자 보고와 worklog가 일치한다.

---

## 테스트 확인

현재 추가된 테스트는 다음을 검증한다.

### Count 재계산

```text
fromJsonRecalculatesCountsWhenPhotoRefsExist
```

검증 항목:

- itemCount
- photoCount
- videoCount
- staleCount

### 잘못된 ref/group 제외

```text
fromJsonSkipsInvalidRefsAndGroups
```

검증 항목:

- 빈 sourceUri ref 제외
- memoryKey 없는 group 제외
- photoRefs와 count가 모두 없는 group 제외
- summary-only group 유지

### 기존 round-trip

기존 `DiscoverySnapshotJsonTest`도 유지된다.

검증 항목:

- snapshot 복원
- group 복원
- 국가 code 정규화
- canonical country display name
- photo ref URI
- media kind
- source relative path
- stale flag
- null snapshot의 empty snapshot 변환

---

## 남은 테스트 공백

현재 기능 결함이라기보다는 다음 단계에서 보강할 테스트다.

### 1. `MemoryRecord.sourceType` 직접 테스트

다음 조합을 직접 검증하면 좋다.

```text
sourceType = null

discoveryGroup 있음 + organizedAlbum 없음
  -> DISCOVERED_ONLY

discoveryGroup 없음 + organizedAlbum 있음
  -> ORGANIZED_ALBUM

discoveryGroup 있음 + organizedAlbum 있음
  -> MIXED
```

현재 로직은 코드에 반영되어 있지만, 전용 테스트가 있으면 이후 `MemoryRecordMapper`나 `MemoryRepository` 작업에서 회귀를 잡기 쉽다.

### 2. summary-only group 테스트 명시

다음 두 케이스를 별도 테스트로 고정하면 정책이 명확해진다.

```text
photoRefs = []
itemCount = 3
  -> group 유지
```

```text
photoRefs = []
itemCount = 0
  -> group 제외
```

현재 `fromJsonSkipsInvalidRefsAndGroups`가 일부 검증하지만, summary-only 유지 정책을 독립적으로 표현하면 더 읽기 쉽다.

### 3. unknown schemaVersion 처리

현재 `DiscoverySnapshotJson.fromJson()`은 `schemaVersion` 값을 읽는다.

다음 단계 `DiscoverySnapshotStore`에서 미래 schema version을 어떻게 처리할지 결정해야 한다.

권장 정책 예시:

```text
schemaVersion <= CURRENT_SCHEMA_VERSION
  -> 읽기 허용

schemaVersion > CURRENT_SCHEMA_VERSION
  -> 안전한 empty 처리 또는 읽기 거부
  -> 기존 snapshot 덮어쓰기 금지
```

미래 버전 JSON을 현재 앱이 조용히 잘못 해석하는 것을 피해야 한다.

---

## 기존 V1 Organizer 영향

이번 커밋은 Discovery 모델과 JSON 복원 validation 범위에 머문다.

다음 기존 경로에는 UI 또는 파일 작업 연결이 없다.

```text
MainActivity
SortInputStore
SortWorker
SortJob
AlbumSummaryHistoryStore
StoredAlbumSummarySearch
```

따라서 기존 Gallery Organizer 기능을 깨뜨릴 위험은 낮다.

Discovery-only 데이터를 다음 기존 저장소에 섞지 않는 경계도 유지된다.

```text
AlbumSummaryHistoryStore
StoredAlbumSummary
```

---

## 다음 `DiscoverySnapshotStore` 단계에서 포함할 최소 범위

다음 작업은 예정대로 작게 유지한다.

### 저장/복구

```text
- 파일이 없으면 empty snapshot
- JSON 저장/복구
- snapshot round-trip
```

### 파일 안정성

```text
- atomic write
- 임시 파일 처리
- backup 파일 처리
- write 실패 시 기존 snapshot 보존
- corrupt JSON이 정상 snapshot을 덮어쓰지 않음
```

### schema 정책

```text
- 현재 schema version 확인
- unknown/future schema version 처리
- schema version이 낮은 데이터의 호환 읽기
```

### validation 보존

```text
- 빈 sourceUri ref 제외
- memoryKey/placeKey 없는 group 제외
- photoRefs가 있으면 count 재계산
- summary-only group 정책 유지
```

### 실행 위치

```text
- JSON read/write를 MainActivity main thread에서 직접 실행하지 않음
- Store는 UI와 분리
- 다음 단계에서도 MainActivity에는 snapshot JSON 로직을 넣지 않음
```

---

## 최종 판정

```text
Review status: Pass
```

`9e51a2c`는 이전 리뷰의 핵심 지적을 모두 반영했다.

현재 반드시 수정해야 하는 추가 finding은 없다.

이번 커밋의 역할은 다음과 같이 명확하다.

```text
6020569
  -> V2 Display First 모델 뼈대

9e51a2c
  -> 모델 입력 validation
  -> JSON count 정합성 보강
  -> sourceType 추론 보강

다음 커밋
  -> DiscoverySnapshotStore
  -> atomic JSON persistence
  -> corrupt/backup/schema 처리
```

전체적으로 기존 V1 Gallery Organizer를 건드리지 않고 V2 discovery layer의 모델 안정성을 높인 안전한 follow-up 커밋이다.
