# Commit 6020569 리뷰 결과

## 리뷰 대상

- Commit: `6020569 Add display-first discovery memory models`
- 목적: PhotoPlace V2 Display First / Organize Optional 모델 뼈대 추가
- 리뷰 범위:
  - `DiscoverySnapshot`
  - `DiscoveryMemoryGroup`
  - `DiscoveryPhotoRef`
  - `MemoryRecord`
  - `OrganizedAlbumRef`
  - `DiscoverySnapshotJson`
  - 관련 테스트
  - 기존 Organizer/History 구조와의 분리
  - 다음 단계 `DiscoverySnapshotStore` 구현 준비도

## 결론

현재 커밋에서 즉시 수정이 필요한 High 수준 기능 결함은 확인되지 않았다.

이번 커밋은 다음 방향을 잘 지키고 있다.

```text
V1 Gallery organizer
  -> 기존 MainActivity / SortJob / SortWorker / AlbumSummaryHistoryStore 유지

V2 discovery layer
  -> 별도 모델 추가
  -> 기존 AlbumSummaryHistoryStore와 분리
  -> 다음 단계에서 DiscoverySnapshotStore 연결
```

특히 `MainActivity`와 기존 Gallery 정리 흐름을 건드리지 않은 점은 안전하다.

다만 다음 `DiscoverySnapshotStore` 구현 전에 count 불일치, JSON validation, source state 기본값, stale 식별 필드에 대한 정책을 확정하는 것이 좋다.

---

## 1. Medium: snapshot JSON이 photo/video count 불일치를 그대로 복원할 수 있음

파일:

- `app/src/main/java/com/example/gallerysorter/DiscoveryMemoryGroup.java`
- `app/src/main/java/com/example/gallerysorter/DiscoverySnapshotJson.java`

`DiscoveryMemoryGroup`은 다음 count를 생성자 외부에서 받는다.

```text
itemCount
photoCount
videoCount
staleCount
```

JSON 복원도 저장된 값을 그대로 사용한다.

```java
json.optInt("itemCount", refs.size()),
json.optInt("photoCount", 0),
json.optInt("videoCount", 0)
```

따라서 JSON의 `photoRefs`와 count가 서로 다르면 불일치가 그대로 유지된다.

예:

```json
{
  "photoRefs": [
    { "mediaKind": "PHOTO" },
    { "mediaKind": "VIDEO" }
  ],
  "itemCount": 2,
  "photoCount": 0,
  "videoCount": 0
}
```

복원 결과:

```text
itemCount = 2
photoCount = 0
videoCount = 0
```

실제 ref와 summary count가 맞지 않는다.

현재 모델 단계에서는 허용할 수 있지만, 다음 `DiscoverySnapshotStore`에서 count의 신뢰 기준을 정해야 한다.

### 권장 정책

```text
photoRefs가 있으면:
  itemCount = ref 기반 계산값
  photoCount = PHOTO ref 수
  videoCount = VIDEO ref 수

저장된 count는 summary cache로만 사용
```

또는 최소한 다음 테스트를 추가해야 한다.

```text
저장된 count와 photoRefs가 불일치할 때:
  ref 기반으로 재계산한다
  또는 저장 count를 신뢰한다
```

현재 테스트는 round-trip만 검증하고 count 불일치 정책은 검증하지 않는다.

파일:

```text
app/src/test/java/com/example/gallerysorter/DiscoverySnapshotJsonTest.java
```

---

## 2. Medium: 설계 문서의 source signature 필드가 실제 모델에 없음

설계 문서에서는 stale 및 원본 재식별을 위해 다음 필드를 검토했다.

```text
sourceBucketId/signature
```

하지만 현재 `DiscoveryPhotoRef`에는 다음 필드만 있다.

```text
sourceUri
mediaStoreId
sourceRelativePath
displayName
takenAtMillis
```

파일:

```text
app/src/main/java/com/example/gallerysorter/DiscoveryPhotoRef.java
```

현재 단계에서는 구현하지 않아도 된다. 복잡한 stale 상태를 후순위로 미룬 MVP 방향과도 맞는다.

다만 다음 단계 전에 명확히 결정하는 것이 좋다.

### 권장 결정

```text
MVP:
  sourceUri
  mediaStoreId
  displayName
  takenAtMillis
  sourceRelativePath
  만 사용

후속 stale/incremental scan 단계:
  source signature 추가 검토
```

문서에는 다음과 같이 명시하면 혼동이 줄어든다.

```text
sourceBucketId/signature는 MVP 모델에서 제외한다.
복잡한 stale 식별과 incremental scan을 도입할 때 schemaVersion을 올려 추가한다.
```

---

## 3. Low: 잘못된 모델 조합을 현재 생성자가 막지 않음

파일:

- `app/src/main/java/com/example/gallerysorter/DiscoveryMemoryGroup.java`
- `app/src/main/java/com/example/gallerysorter/DiscoveryPhotoRef.java`

현재 다음과 같은 잘못된 상태가 생성 가능하다.

```text
memoryKey = ""
placeKey = ""
photoRefs = []
itemCount = 10
photoCount = 0
videoCount = 0
```

또는:

```text
sourceUri = ""
mediaKind = PHOTO
```

현재 모델은 package-private이고 아직 UI에 연결되지 않았기 때문에 즉시 발생하는 버그는 아니다.

하지만 `DiscoverySnapshotStore`가 외부 JSON을 읽기 시작하면 잘못된 snapshot이 내부로 들어올 수 있다.

### 권장 validation 정책

`DiscoveryPhotoRef`:

```text
sourceUri가 비어 있으면 해당 ref를 제외
```

`DiscoveryMemoryGroup`:

```text
memoryKey 또는 placeKey가 없으면 group 제외
photoRefs가 없고 summary count도 0이면 group 제외
```

생성자에서 예외를 던지기보다는 `DiscoverySnapshotJson.fromJson()` 또는 별도 validator에서 처리하는 편이 기존 사용자 파일에 안전하다.

---

## 4. Low: MemoryRecord의 sourceType 기본값이 누락을 숨길 수 있음

파일:

```text
app/src/main/java/com/example/gallerysorter/MemoryRecord.java
```

현재 `sourceType`이 null이면 자동으로 다음 값으로 대체된다.

```java
MemorySourceType.DISCOVERED_ONLY
```

다음 adapter에서 source type 전달을 빠뜨리면 실제로 organized album이 있어도 Discovery-only로 표시될 수 있다.

예상 가능한 문제:

```text
organizedAlbum이 있음
sourceType 전달 누락
-> DISCOVERED_ONLY
-> Gallery 열기 action이 잘못 숨겨질 수 있음
```

현재는 `MemoryRepository`와 adapter가 아직 구현되지 않았기 때문에 즉시 결함은 아니다.

### 권장 정책

다음 중 하나를 선택하는 것이 좋다.

```text
1. adapter가 sourceType을 반드시 계산한다.
2. sourceType null을 허용하지 않고 validation 실패로 처리한다.
3. MemoryRecord 생성자를 직접 호출하지 않고 factory를 사용한다.
```

MVP에서는 `MemoryRecordFactory` 또는 `MemoryRecordMapper` 한 곳에서 source state를 계산하는 방식을 권장한다.

```text
discoveryGroup 있음 + organizedAlbum 없음
  -> DISCOVERED_ONLY

discoveryGroup 없음 + organizedAlbum 있음
  -> ORGANIZED_ALBUM

discoveryGroup 있음 + organizedAlbum 있음
  -> MIXED
```

---

## 정상적으로 반영된 부분

### 1. 기존 Organizer 경로와 MainActivity를 건드리지 않음

현재 커밋은 모델과 JSON helper, 테스트만 추가하고 기존 Gallery 정리 흐름을 변경하지 않는다.

영향을 받지 않은 핵심 경로:

```text
MainActivity
SortInputStore
SortWorker
SortJob
AlbumSummaryHistoryStore
StoredAlbumSummarySearch
```

따라서 이번 커밋이 기존 V1 organizer 동작을 깨뜨릴 위험은 낮다.

### 2. 국가 identity 정규화가 새 모델에도 일관되게 적용됨

다음 모델 모두 `countryCode`를 우선하고 `countryName`을 canonical 표시명으로 정규화한다.

```text
DiscoveryPhotoRef
DiscoveryMemoryGroup
MemoryRecord
OrganizedAlbumRef
```

기대 동작:

```text
countryCode = TR
countryName = Turkey

-> countryCode = TR
-> countryName = 튀르키예
```

이 방향은 기존 해외 기록 국가 identity 설계와 일관된다.

### 3. JSON round-trip 기본 테스트가 있음

파일:

```text
app/src/test/java/com/example/gallerysorter/DiscoverySnapshotJsonTest.java
```

현재 다음 항목이 검증된다.

- snapshot 복원
- group 복원
- countryCode canonicalization
- countryName canonical display
- photo ref URI
- MediaKind
- sourceRelativePath
- stale flag
- null snapshot이 empty snapshot으로 복원되는지

모델만 추가한 첫 커밋의 테스트 범위로는 적절하다.

### 4. 기존 저장소와의 분리 방향이 문서화됨

파일:

```text
AGENT_SHARED_TODO.md
WORKLOG_2026-08-10.md
DISPLAY_FIRST_ORGANIZE_OPTIONAL_DESIGN_2026-08-10.md
```

다음 경계가 명확히 기록되어 있다.

```text
AlbumSummaryHistoryStore
  = 실제 Gallery album history

DiscoverySnapshotStore
  = 별도 추가 예정인 analysis result store

MemoryRepository
  = 두 source를 통합할 adapter/repository
```

또한 다음 구현을 하지 않았다는 점도 명확하다.

```text
- MainActivity 연결
- Home 연결
- Search 연결
- Detail 연결
- SortWorker 연결
```

이것은 이번 커밋의 안전한 범위와 일치한다.

---

## 검증 기록 확인

사용자가 보고한 검증:

```text
./gradlew.bat testDebugUnitTest 성공
./gradlew.bat assembleDebug 성공
```

`testDebugUnitTest` 성공은 `WORKLOG_2026-08-10.md`의 기록과 일치한다.

하지만 `assembleDebug`에 대해서는 저장소 문서에 기록이 서로 다르다.

사용자 보고:

```text
assembleDebug 성공
```

반면 `WORKLOG_2026-08-10.md`에는 다음처럼 적혀 있다.

```text
Not run yet:

./gradlew.bat assembleDebug
```

따라서 현재 repository evidence만 기준으로 하면 `assembleDebug` 성공은 확인할 수 없다.

가능성:

```text
- assembleDebug 실행 후 worklog를 갱신하지 않았음
- 커밋 시점과 worklog 기록 시점이 다름
- 사용자 검증은 최신 상태지만 문서가 뒤처짐
```

다음 작업 전에 worklog의 검증 상태를 정정하면 좋다.

---

## 다음 DiscoverySnapshotStore 구현 전에 확정할 것

우선순위:

```text
1. corrupt/empty JSON 처리 정책
2. unknown schemaVersion 처리
3. photoRefs와 count 불일치 시 재계산 정책
4. 빈 URI / 빈 group validation
5. atomic write와 tmp/bak 복구 정책
6. 기존 snapshot이 새 저장 실패로 덮어써지지 않는지
7. main thread에서 JSON read/write를 하지 않는지
8. source signature를 MVP에서 제외한다는 결정
9. MemoryRecord sourceType을 factory/repository에서만 계산
```

### 반드시 추가할 Store 테스트

```text
- 파일이 없으면 empty snapshot
- 빈 JSON이면 empty snapshot
- corrupt JSON이면 기존 snapshot 보존
- unknown schemaVersion 처리
- tmp 파일이 남아 있어도 정상 파일 우선
- write 성공 후 tmp 정리
- write 실패 시 기존 snapshot 유지
- snapshot round-trip
- countryCode round-trip
- photoRefs/count mismatch 정책
- 빈 sourceUri ref 제거
- 잘못된 group skip
```

---

## 최종 평가

이번 커밋은 V2의 첫 구조 checkpoint로 적절하다.

```text
V1 Gallery organizer
  -> 기존 코드 유지

V2 discovery layer
  -> 별도 model 추가
  -> 기존 history와 분리
  -> 다음 단계에서 Store 연결
```

현재 반드시 수정해야 하는 기능 결함은 확인되지 않았다.

다음 단계의 핵심 위험은 모델 자체보다 `DiscoverySnapshotStore`의 파일 안정성과 JSON validation이다.

따라서 다음 작은 패치로 제안한 방향이 적절하다.

```text
DiscoverySnapshotStore 추가
JSON 저장/복구 테스트 추가
atomic write 테스트 추가
corrupt JSON 보호 테스트 추가
```

단, 다음 세 가지는 Store 작업에 반드시 포함하는 것이 좋다.

```text
- count 불일치 정책
- 빈 ref/group validation
- sourceType 계산 책임
```

전체적으로는 기존 V1 organizer를 건드리지 않고 V2 discovery layer를 별도로 세운 안전한 foundation commit이다.
