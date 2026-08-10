# PhotoPlace V2 Display First / Organize Optional 설계

작성일: 2026-08-10
상태: 설계 전용, 아직 구현하지 않음

## 0. 결론 요약

PhotoPlace V2의 핵심 원칙은 다음과 같다.

```text
Display First, Organize Optional

분석
  -> PhotoPlace 안에서 장소별 Memory View 제공
  -> 사용자가 사진과 장소를 확인
  -> 필요할 때만 Gallery 앨범 생성
```

추천 구조:

```text
MediaStore / EXIF 분석
  -> PhotoItem 목록
  -> DiscoverySnapshotMapper
  -> DiscoverySnapshotStore
  -> MemoryRepository
       - Discovery-only snapshot
       - 기존 organized album history
       - live album 상태
  -> 통합 MemoryRecord
  -> Home / Search / Memory Detail
```

중요한 경계:

```text
AlbumSummaryHistoryStore
  = 실제 Gallery 앨범 기록 전용

DiscoverySnapshotStore
  = Gallery 앨범 생성 여부와 무관한 분석 결과 전용

MemoryRepository
  = 두 데이터를 UX용 MemoryRecord로 통합하는 adapter/repository
```

`AlbumSummaryHistoryStore`에 Discovery-only 데이터를 넣지 않는다. `relativePath` 없는 메모리를 기존 `StoredAlbumSummary`로 억지 변환하지 않는다. `MainActivity`는 화면 orchestration만 담당하고 분석·snapshot·검색·상태 병합은 별도 계층으로 이동한다.

---

## 1. 현재 코드 기준 분석

### 1.1 현재 전체 흐름

현재 앱의 실질적인 흐름은 다음과 같다.

```text
MediaStore scan
  -> EXIF / MediaStore / video metadata에서 날짜·GPS 조회
  -> Android Geocoder reverse lookup
  -> 장소명·국가·주소 계산
  -> PhotoItem 생성
  -> MainActivity.previewItems에 보관
  -> 사용자가 정리 실행
  -> SortJob / SortWorker
  -> MediaCopyEngine
  -> Gallery 사진 복사·동영상 이동
  -> AlbumSummary 생성
  -> AlbumSummaryHistoryStore 저장
  -> StoredAlbumSummary 로드
  -> 최근 장소 / 해외 기록 / 상세 화면
```

주요 분석·Preview 경로는 `MainActivity`가 소유한다.

- Preview 실행과 `previewItems` 보관: [MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java#L1180)
- `PhotoItem` 생성: [MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java#L2680)
- 좌표와 Geocoder 처리: [MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java#L2760)
- Geocoder 국가 identity: [MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java#L3025)

### 1.2 PhotoItem의 현재 역할

[PhotoItem.java](app/src/main/java/com/example/gallerysorter/PhotoItem.java#L1)은 분석 결과와 Gallery 정리 상태를 함께 가진다.

분석/Memory 데이터:

```text
uri
name
mimeType
 takenAt
locationKey
noLocation
countryCode
countryName
adminArea
addressLine
```

Gallery 정리 전용 데이터:

```text
targetExists
duplicateInTarget
targetRelativePath
video 정리 설정과 결합되는 상태
```

`PhotoItem`은 정리 작업에는 적합하지만 장기 Discovery snapshot의 public 저장 모델로는 너무 많은 책임을 가진다.

### 1.3 SortInputStore와 SortWorker

[SortInputStore.java](app/src/main/java/com/example/gallerysorter/SortInputStore.java#L1)은 `PhotoItem` 전체를 JSON으로 저장해 WorkManager가 이어서 정리할 수 있게 한다.

[SortWorker.java](app/src/main/java/com/example/gallerysorter/SortWorker.java#L20)은 저장된 `PhotoItem`을 읽어 `SortJob`을 실행한다.

[SortJob.java](app/src/main/java/com/example/gallerysorter/SortJob.java#L20)은 다음 파일 작업만 담당한다.

- 위치 없는 항목 skip
- 동영상 이동 설정 적용
- 이미 target에 있는 항목 skip
- 사진 복사
- 동영상 이동
- 결과 URI와 sorted item 기록

따라서 Discovery-only 경로에서는 `SortInputStore`, `SortWorker`, `SortJob`, `MediaCopyEngine`을 호출하지 않는다. 분석 결과를 snapshot으로 저장하고, Gallery 파일을 변경하지 않는 별도 흐름이 필요하다.

### 1.4 AlbumSummaryHistoryStore의 전제

[AlbumSummaryHistoryStore.java](app/src/main/java/com/example/gallerysorter/AlbumSummaryHistoryStore.java#L1)은 실제 Gallery 앨범을 전제로 한다.

주요 필드:

```text
albumName
relativePath
itemCount
startDate
endDate
thumbnailUri
countryCode
countryName
adminArea
addressLine
createdAt
```

이 구조는 다음을 암묵적으로 가정한다.

```text
relativePath가 존재한다.
MediaStore에서 해당 앨범 폴더를 조회할 수 있다.
앨범이 삭제되면 live check에서 제거할 수 있다.
상세 화면은 Gallery 폴더를 통해 사진을 찾는다.
```

따라서 Discovery-only 결과를 이 저장소에 넣으면 다음 문제가 발생한다.

- `relativePath`가 비어 있음
- `filterLiveStoredAlbumSummaries()`에서 제거될 수 있음
- `hasLiveMediaInAlbum()`이 실패함
- `loadLatestAlbumPreviewUris(relativePath, ...)`가 사진을 찾지 못함
- 실제 생성 앨범 수와 발견한 Memory 수가 섞임

### 1.5 기존 MemoryGroup 구조

[MemoryItem.java](app/src/main/java/com/example/gallerysorter/MemoryItem.java#L1)과 [MemoryGroup.java](app/src/main/java/com/example/gallerysorter/MemoryGroup.java#L1)은 현재 주로 해외 국가 카드의 표시용 grouping 모델이다.

`OverseasMemoryGrouper`는 `StoredAlbumSummary`를 국가별로 묶는다.

이 모델은 다음에는 적합하지 않다.

- 국가가 아닌 장소 단위 Memory
- 원본 URI 목록
- Discovery-only와 organized 상태의 결합
- stale/missing 개별 사진 상태
- 사용자 personalization의 stable key

따라서 기존 `MemoryGroup`을 확장해 V2의 전체 메모리 모델로 만들기보다 새로운 `MemoryRecord` 계층을 둔다.

### 1.6 Personalization 현재 상태

[MemoryPersonalizationStore.java](app/src/main/java/com/example/gallerysorter/MemoryPersonalizationStore.java#L1)은 displayName, memo, userCoverUri를 JSON으로 저장할 기반이 있다.

[MemoryPersonalizationKey.java](app/src/main/java/com/example/gallerysorter/MemoryPersonalizationKey.java#L1)은 현재 다음 순서를 사용한다.

```text
relativePath가 있으면 path:{relativePath}
그렇지 않으면 album:{albumName}
```

이 정책은 Discovery-only에서 다음 문제가 있다.

- `relativePath`가 없으면 장소명만 key가 됨
- 같은 장소가 여러 날짜·여행·snapshot으로 나뉠 수 있음
- organized 상태로 전환되면 `relativePath` key와 discovery key가 달라질 수 있음
- 사용자의 displayName/memo/cover가 새 source type에서 이어지지 않을 수 있음

V2에서는 personalization key를 source-independent `memoryKey`로 승격해야 한다.

---

## 2. 제품 원칙과 범위

### 2.1 Display First

Preview가 끝나면 먼저 다음을 제공한다.

```text
발견한 장소 둘러보기
```

사용자는 Gallery 폴더를 만들지 않아도 다음을 할 수 있어야 한다.

- 장소별 사진 보기
- 대표 이미지 보기
- 날짜 범위 확인
- 국가·도시·주소 확인
- 검색으로 장소 찾기
- memo/displayName/cover 편집
- 나중에 Gallery 앨범 생성

### 2.2 Organize Optional

Gallery 앨범 생성은 제거하지 않는다.

```text
전체 장소 앨범 만들기
이 장소만 앨범으로 만들기
선택한 장소 앨범 만들기
새 사진만 추가하기
```

다만 Preview 완료 직후 기본 화면의 중심은 Gallery 생성 완료가 아니라 발견한 Memory View다.

### 2.3 단일 모드로 표현

다음과 같은 mode switch는 도입하지 않는다.

```text
발견 모드 / 정리 모드
```

대신 하나의 Memory 화면에서 상태와 다음 행동을 보여준다.

```text
사진을 발견함
앨범으로 정리할 수 있음
이미 Gallery 앨범이 있음
새 사진이 추가됨
앨범은 삭제되었지만 앱 안에서 볼 수 있음
```

---

## 3. 추천 아키텍처

### 3.1 계층

```text
UI layer
  - MainActivity 또는 이후 Memory/Home 화면 controller
  - Preview complete action renderer
  - Memory detail renderer

Application/service layer
  - DiscoveryAnalysisService
  - DiscoverySnapshotService
  - MemoryRepository
  - MemorySearchService
  - OrganizePlaceService

Data/model layer
  - DiscoverySnapshot
  - DiscoveryMemoryGroup
  - DiscoveryPhotoRef
  - OrganizedAlbumRef
  - MemoryRecord
  - MemoryPersonalization

Persistence layer
  - DiscoverySnapshotStore
  - AlbumSummaryHistoryStore
  - MemoryPersonalizationStore
  - later MemoryIndexStore / Room database
```

### 3.2 각 책임

#### DiscoveryAnalysisService

책임:

- MediaStore/EXIF/GPS 분석 orchestration
- 현재 `PhotoItem` 생성 로직을 재사용 가능한 분석 결과로 변환
- 장소 grouping에 필요한 canonical field 생성
- Gallery에 파일을 복사하거나 이동하지 않음

초기에는 기존 분석 코드를 한 번에 옮기지 않는다. 먼저 `MainActivity`가 만든 `PhotoItem` 목록을 입력받아 snapshot으로 변환하는 `DiscoverySnapshotService`부터 둔다.

이후 분석 자체를 옮길 때는 다음 단계로 분리한다.

```text
MediaStorePhotoReader
  -> LocationAnalysisService
  -> DiscoveryGrouper
  -> DiscoverySnapshotService
```

#### DiscoverySnapshotService

책임:

- `List<PhotoItem>`을 `DiscoverySnapshot`으로 변환
- place grouping
- cover와 date range 계산
- source URI reference 저장
- snapshot version과 생성 시각 기록
- 같은 snapshot의 중복 URI 제거

#### DiscoverySnapshotStore

책임:

- snapshot JSON 읽기/쓰기
- atomic write
- schemaVersion 관리
- 최신 snapshot 조회
- snapshot 삭제/보존 정책

#### MemoryRepository

책임:

- discovery snapshot 로드
- organized album history 로드
- MediaStore live 상태 확인 결과 반영
- 같은 place의 discovery/organized source 병합
- `MemoryRecord` 반환
- UI가 두 저장소를 직접 알지 않게 함

#### MemorySearchService

책임:

- `MemoryRecord` 통합 검색
- country alias normalization 재사용
- displayName/memo 검색
- date query
- source state filter
- in-memory filtering 또는 index 사용

#### OrganizePlaceService

책임:

- 하나의 MemoryRecord 또는 선택된 place group을 Gallery 정리 대상으로 변환
- 기존 `PhotoItem` 또는 새 `OrganizeInput` 생성
- `SortJob`/`SortWorker`와 연결
- discovery snapshot은 조직 작업과 별도로 유지
- 성공 후 organized ref와 snapshot 상태를 갱신

### 3.3 MainActivity 역할

V2에서 `MainActivity`는 다음만 담당하도록 줄인다.

```text
현재 화면 상태
버튼/탭 이벤트 전달
서비스 호출
결과를 renderer에 전달
navigation/back stack
```

다음은 새 클래스가 소유해야 한다.

```text
MediaStore scan
snapshot grouping
snapshot JSON
MemoryRecord merge
search index
stale check
organize selection
```

기존 `MainActivity`에 다음을 새로 추가하지 않는다.

- Discovery JSON 포맷
- 국가 alias 테이블
- Memory merge 정책
- URI stale 검사 loop
- 장소별 앨범 생성 selection 계산

---

## 4. 최소 데이터 모델

### 4.1 MemorySourceType

```java
enum MemorySourceType {
    DISCOVERED_ONLY,
    ORGANIZED_ALBUM,
    MIXED
}
```

JSON 저장 시 enum name을 그대로 사용해도 되지만, 향후 이름 변경을 고려하면 versioned string mapping이 안전하다.

### 4.2 DiscoveryPhotoRef

Discovery-only 상세 화면에서 실제 사진을 열기 위한 원본 reference다.

```text
DiscoveryPhotoRef
  - sourceUri: String
  - mediaStoreId: long, optional
  - mediaKind: PHOTO / VIDEO
  - mimeType: String
  - displayName: String
  - takenAtMillis: long, optional
  - locationKey: String
  - placeName: String
  - countryCode: String
  - countryName: String
  - adminArea: String
  - addressLine: String
  - sourceRelativePath: String, optional
  - sourceBucketId/signature: String, optional
  - firstSeenSnapshotVersion: long
  - lastSeenSnapshotVersion: long
  - stale: boolean
```

필드 우선순위:

```text
필수:
  sourceUri
  mediaKind
  placeKey 또는 group 연결값

권장:
  mediaStoreId
  mimeType
  takenAtMillis
  locationKey
  countryCode
  adminArea
  addressLine

보조:
  sourceRelativePath
  displayName
  stale
```

원본 URI만 저장하는 것은 부족하다. URI가 바뀌거나 권한이 사라질 수 있으므로 `mediaStoreId`, 이름, takenAt, source path를 함께 저장해 stale 진단에 사용한다.

### 4.3 DiscoveryMemoryGroup

```text
DiscoveryMemoryGroup
  - memoryKey: String
  - placeKey: String
  - placeName: String
  - displayName: String, optional overlay
  - countryCode: String
  - countryName: String
  - adminArea: String
  - addressLine: String
  - itemCount: int
  - photoCount: int
  - videoCount: int
  - startDateMillis: long, optional
  - endDateMillis: long, optional
  - coverUri: String
  - photoRefs: List<DiscoveryPhotoRef>
  - organizedAlbumRef: OrganizedAlbumRef, optional
  - sourceState: DISCOVERED_ONLY / ORGANIZED_ALBUM / MIXED
  - staleCount: int
  - snapshotVersion: long
```

`countryCode`를 내부 grouping key로 사용한다. `countryName`은 표시 cache다.

`placeKey`는 장소명 표시 문자열과 분리한다.

```text
placeKey = canonical location identity
placeName = 현재 정책으로 보여주는 이름
```

### 4.4 OrganizedAlbumRef

```text
OrganizedAlbumRef
  - relativePath: String
  - albumName: String
  - itemCount: int
  - thumbnailUri: String
  - liveState: PRESENT / EMPTY / MISSING / UNKNOWN
  - lastCheckedAtMillis: long
  - countryCode: String
  - startDateMillis: long, optional
  - endDateMillis: long, optional
```

기존 `StoredAlbumSummary`를 그대로 넣지 않고 adapter로 변환한다.

### 4.5 DiscoverySnapshot

```text
DiscoverySnapshot
  - schemaVersion: int
  - snapshotVersion: long
  - createdAtMillis: long
  - sourceSignature: String
  - sourceItemCount: int
  - groupCount: int
  - groups: List<DiscoveryMemoryGroup>
  - analysisPolicyVersion: String
  - countryIdentityPolicyVersion: String
```

`sourceSignature`는 전체 MediaStore를 매번 재검증하지 않기 위한 coarse signature다. 예를 들어 분석 대상 source folder, visible item count, max modified time, permission state를 조합한다.

### 4.6 MemoryRecord

UX에서 사용하는 통합 모델이다.

```text
MemoryRecord
  - memoryKey: String
  - placeKey: String
  - title: String
  - canonicalPlaceName: String
  - displayName: String
  - countryCode: String
  - countryName: String
  - adminArea: String
  - addressLine: String
  - itemCount: int
  - photoCount: int
  - videoCount: int
  - startDateMillis: long, optional
  - endDateMillis: long, optional
  - coverUri: String
  - sourceType: DISCOVERED_ONLY / ORGANIZED_ALBUM / MIXED
  - discoveryGroup: optional
  - organizedAlbum: optional
  - staleCount: int
  - availableCount: int
  - canOpenPhotos: boolean
  - canOpenGalleryAlbum: boolean
  - canOrganize: boolean
  - canAddNewItems: boolean
```

이 모델은 UI가 `relativePath` 존재 여부를 직접 판단하지 않게 한다.

---

## 5. Stable key 및 personalization 설계

### 5.1 key를 장소명만으로 만들지 않기

다음은 금지한다.

```text
memoryKey = albumName
memoryKey = placeName
```

표시명은 정책이나 locale에 따라 바뀔 수 있고, 같은 도시가 여러 session에 반복될 수 있다.

### 5.2 MVP key 정책

초기 MVP에서는 `placeKey`와 날짜 범위를 조합한다.

```text
placeKey = countryCode + "|" + normalized admin/locality/place identity
memoryKey = placeKey + "|" + startDateBucket + "|" + endDateBucket
```

예:

```text
JP|Hokkaido|Sapporo|2026-08-01|2026-08-06
TR|Istanbul|Fatih|2026-05-01|2026-05-03
```

다만 exact date range가 매번 조금씩 바뀌면 key가 분리될 수 있다. 그래서 MVP에서는 다음 규칙을 둔다.

- 같은 분석 대상에서 snapshot이 갱신되면 기존 memoryKey를 재사용
- 새 snapshot group과 기존 group은 placeKey와 날짜 overlap으로 matching
- 날짜가 완전히 달라지면 새 Memory로 분리할 수 있음
- 사용자가 장기적으로 같은 장소로 합치고 싶어 하는 기능은 Personal Place 후속 범위

### 5.3 장기 key 방향

장기적으로는 source-independent `memoryKey`와 `placeKey`를 분리한다.

```text
placeKey = canonical geographic place
memoryKey = placeKey + memory session/trip identity
```

여행/session 모델이 생기면 날짜 범위는 key의 보조 조건이 되고, user merge/split 기능을 제공할 수 있다.

### 5.4 Personalization 연결

`MemoryPersonalizationStore`는 `StoredAlbumSummary`를 직접 key로 삼지 않고 다음 API를 제공하는 방향이 좋다.

```text
get(memoryKey)
saveDisplayName(memoryKey, value)
saveMemo(memoryKey, value)
saveUserCoverUri(memoryKey, uri)
clear(memoryKey)
```

기존 API는 migration 기간에 유지한다.

```text
get(StoredAlbumSummary summary)
  -> MemoryPersonalizationKey.forSummary(summary)
  -> legacy adapter
```

기존 organized album에는 다음 alias를 제공한다.

```text
old path:{relativePath}
new memory:{memoryKey}
```

읽기 순서:

```text
1. new memoryKey record
2. 기존 relativePath key
3. legacy SharedPreferences memo
```

새 displayName/memo/cover가 저장되면 new memoryKey record에 기록하고, 성공 후에만 legacy memo migration을 완료한다.

### 5.5 Discovery-only의 cover

cover는 다음 우선순위로 결정한다.

```text
1. userCoverUri가 현재 열리는 경우
2. snapshot group coverUri가 열리는 경우
3. 첫 번째 live photoRef thumbnail
4. organized album thumbnailUri
5. placeholder
```

User cover가 stale이면 삭제하지 말고 stale 상태로 남기며, 다음 화면에서 snapshot cover로 fallback한다.

---

## 6. DiscoverySnapshotStore 설계

### 6.1 저장 단위

초기 MVP는 하나의 최신 snapshot을 저장한다.

```text
filesDir/discovery_snapshot.json
```

필요하면 직전 snapshot을 backup으로 둔다.

```text
discovery_snapshot.json
 discovery_snapshot.json.bak
 discovery_snapshot.json.tmp
```

저장 시 atomic write를 사용한다.

### 6.2 Snapshot 보존 정책

초기에는 최신 1개 + 직전 1개면 충분하다.

이유:

- 사용자는 오래된 분석 결과보다 현재 사진 상태를 원함
- 여러 snapshot을 모두 저장하면 JSON 용량과 merge 복잡도가 커짐
- 과거 여행 기록은 group 내부의 source photo refs와 dateRange로 유지 가능

향후 분석 diff나 undo가 필요할 때만 다중 snapshot을 도입한다.

### 6.3 JSON 예시

```json
{
  "schemaVersion": 1,
  "snapshotVersion": 12,
  "createdAtMillis": 1780000000000,
  "sourceSignature": "images:12000:modified:...",
  "analysisPolicyVersion": "place-v2-1",
  "countryIdentityPolicyVersion": "country-v1",
  "groups": [
    {
      "memoryKey": "JP|Hokkaido|Sapporo|2026-08-01|2026-08-06",
      "placeKey": "JP|Hokkaido|Sapporo",
      "placeName": "삿포로",
      "countryCode": "JP",
      "countryName": "일본",
      "itemCount": 24,
      "photoCount": 22,
      "videoCount": 2,
      "startDateMillis": 1780000000000,
      "endDateMillis": 1780400000000,
      "coverUri": "content://media/external/images/media/123",
      "photoRefs": [
        {
          "sourceUri": "content://media/external/images/media/123",
          "mediaStoreId": 123,
          "mediaKind": "PHOTO",
          "mimeType": "image/jpeg",
          "takenAtMillis": 1780000000000,
          "placeKey": "JP|Hokkaido|Sapporo",
          "countryCode": "JP",
          "lastSeenSnapshotVersion": 12,
          "stale": false
        }
      ],
      "sourceState": "DISCOVERED_ONLY"
    }
  ]
}
```

### 6.4 JSON vs Room/SQLite

초기 MVP에서는 JSON이 적절하다.

JSON이 충분한 조건:

- 최신 snapshot 하나만 저장
- Preview 완료 때 한 번 쓰고, 화면에서는 메모리 로드
- 1만~3만장의 모든 사진 reference를 한 번에 저장하는 것까지는 허용
- 검색은 이미 로드한 group/record를 대상으로 수행
- snapshot write는 background thread에서 수행

3만장 기준 위험:

- raw photo refs JSON이 수 MB 이상이 될 수 있음
- `JSONObject` 전체 parse가 큰 메모리 spike를 만들 수 있음
- snapshot 전체 재작성 시간이 길어질 수 있음
- URI와 metadata 중복으로 저장 용량이 늘어남
- Activity main thread에서 parse하면 첫 화면이 느려짐

권장 MVP 제한:

```text
최신 snapshot 1개
모든 photoRef 저장
parse/write는 executor 또는 Worker
UI에는 group summary를 먼저 제공
상세 photoRefs는 필요 시 lazy load
```

Room/SQLite로 전환할 시점:

- snapshot이 여러 개 쌓임
- 3만장 이상에서 검색·stale update가 반복적으로 느림
- photoRef 단위 부분 업데이트가 필요함
- source folder incremental scan이 필요함
- memory merge와 user personalization query가 복잡해짐
- 앱 시작 시 전체 JSON parse가 체감 지연을 만듦

Room 전환 전까지는 JSON을 직접 UI에서 읽지 말고 Store interface 뒤에 둔다.

---

## 7. MemoryRepository와 기존 history 연결

### 7.1 Adapter 구조

```text
AlbumSummaryHistoryStore
  -> StoredAlbumSummary
  -> OrganizedAlbumMapper
  -> OrganizedAlbumRef

DiscoverySnapshotStore
  -> DiscoverySnapshot
  -> DiscoveryMemoryGroup

MemoryRepository
  -> match by memoryKey/placeKey
  -> live album state check
  -> MemoryRecord
```

`StoredAlbumSummary`를 `MemoryRecord`로 바로 노출하지 않고 `OrganizedAlbumMapper`를 거친다.

### 7.2 matching 우선순위

같은 장소인지 판단할 때 다음 순서를 사용한다.

```text
1. persisted memoryKey가 있으면 memoryKey
2. canonical placeKey + date overlap
3. countryCode + normalized place identity + date overlap
4. 기존 relativePath/albumName은 legacy fallback
```

앨범명 문자열만으로 merge하지 않는다.

### 7.3 source state

```text
DISCOVERED_ONLY:
  discovery snapshot 있음
  live organized album 없음

ORGANIZED_ALBUM:
  live organized album 있음
  discovery snapshot 없음 또는 photo refs 없음

MIXED:
  discovery snapshot 있음
  organized album도 있음
```

정책:

- 같은 MemoryRecord로 보여준다.
- 대표 title/displayName은 personalization overlay를 먼저 사용한다.
- photo count는 discovery source와 live album source를 중복 합산하지 않는다.
- organized album이 있으면 Gallery open action을 보여준다.
- discovery refs가 있으면 PhotoPlace 내부 사진 보기 action을 보여준다.
- 새 원본 사진이 snapshot에만 있으면 `새 사진 추가 가능`을 보여준다.

### 7.4 중복 합산 방지

MIXED 상태에서 discovery photo와 organized album photo를 단순 합산하면 count가 부풀려질 수 있다.

MVP에서는 다음을 사용한다.

```text
대표 count = discovery snapshot의 live photoRef 수
organized count = StoredAlbumSummary.itemCount
```

UI에는 다음을 분리 표시한다.

```text
앱에서 발견한 사진 24개
Gallery 앨범 20개
```

중복 제거가 필요할 때는 URI와 MediaStore ID를 기준으로 비교한다. 동영상 이동 후 URI가 바뀌는 경우에는 name + takenAt + size 또는 source signature를 보조 키로 사용한다.

---

## 8. Migration 전략

### 8.1 공통 원칙

- 기존 `AlbumSummaryHistoryStore` 파일은 덮어쓰지 않는다.
- 기존 organized album record는 `StoredAlbumSummary`로 계속 읽는다.
- Discovery snapshot은 별도 파일로 시작한다.
- 기존 사용자에게 자동으로 Gallery 구조를 변경하지 않는다.
- migration은 read adapter 우선, write는 새 schema에만 한다.
- migration 실패가 기존 기록을 삭제하거나 가리지 않게 한다.

### 8.2 Case A: 기존 앨범이 많은 사용자

현재 상태:

```text
AlbumSummaryHistoryStore 있음
relativePath 있음
Gallery 폴더 있음
DiscoverySnapshot 없음
```

처리:

1. 기존 history를 `ORGANIZED_ALBUM` MemoryRecord로 변환
2. 기존 `StoredAlbumSummary`를 보존
3. snapshot이 없어도 홈과 검색에 기존 Memory를 표시
4. 사용자가 새 분석을 실행하면 discovery snapshot을 별도로 생성
5. 같은 장소로 matching되면 `MIXED`, 아니면 별도 Memory로 유지

기존 record에 `memoryKey`를 즉시 삽입하는 것은 위험하므로, 첫 단계에서는 adapter가 임시 key를 계산한다.

### 8.3 Case B: 발견 장소 다시 만들기

현재 버튼 이름과 기능이 Gallery history rebuild와 discovery analysis를 혼동할 수 있다.

기능을 분리한다.

```text
발견한 장소 새로 분석
  = MediaStore/EXIF를 다시 읽고 DiscoverySnapshot 갱신
  = Gallery 변경 없음

정리 기록 새로 만들기
  = 기존 Gallery 폴더를 다시 조회하고 AlbumSummaryHistoryStore 재생성
  = 원본 분석을 다시 하지 않을 수 있음

이 장소를 앨범으로 만들기
  = 선택 Memory의 원본 photoRefs를 OrganizePlaceService에 전달
```

기존 “발견장소 다시 만들기”가 history 재생성 기능이었다면 다음처럼 명확히 분리한다.

```text
발견한 장소 새로 고침
정리 기록 새로 고침
```

Preview 완료 화면의 action:

```text
발견한 장소 둘러보기
전체 앨범 만들기
```

### 8.4 Case C: Discovery-only 사용자

상태:

```text
DiscoverySnapshot 있음
relativePath 없음
Gallery 앨범 없음
```

UI:

```text
사진 보기
앨범으로 만들기
메모 / 이름 / 커버 편집
```

숨김:

```text
Gallery에서 보기
앨범 열기
```

원본 URI가 열리지 않는 항목은 stale로 표시한다.

```text
일부 사진을 불러올 수 없음
사용 가능한 사진 18개
```

### 8.5 Case D: 일부 장소만 앨범 생성

같은 place에 discovery와 organized가 둘 다 있으면 중복 카드를 만들지 않는다.

```text
DiscoverySnapshot group + StoredAlbumSummary
  -> 하나의 MemoryRecord
  -> sourceType = MIXED
```

상태 표시:

```text
앨범 있음
새 사진 4개
사진 보기
Gallery에서 보기
```

조직 action:

```text
새 사진만 추가
```

기존 `SortJob`은 `duplicateInTarget` 기준으로 이미 복사된 항목을 skip하므로 재사용할 수 있지만, “새 사진만 추가”의 입력은 snapshot group의 photoRefs에서 만들어야 한다.

### 8.6 Case E: Gallery 앨범 삭제

상태:

```text
StoredAlbumSummary 있음
live Gallery album 없음
DiscoverySnapshot source photo는 여전히 live
```

처리:

- 기존 organized-only record는 live check 결과에 따라 숨길 수 있음
- discovery group이 살아 있으면 같은 MemoryRecord를 계속 표시
- sourceType은 `DISCOVERED_ONLY`로 전환
- UI:
  - 앱에서 사진 보기
  - 앨범 없음
  - 다시 앨범 만들기

중요: 기존 `filterLiveStoredAlbumSummaries()`의 “live album 없으면 제거” 정책을 Discovery Memory 전체에 적용하지 않는다. live check는 `OrganizedAlbumRef`에만 적용한다.

### 8.7 Case F: 원본 사진 삭제 또는 권한 손실

stale check는 앱 시작마다 전체 MediaStore를 다시 확인하지 않는다.

MVP 전략:

```text
snapshot load
  -> 전체 URI를 즉시 open하지 않음
  -> group summary는 우선 표시
  -> cover/detail을 열 때 해당 group photoRefs만 확인
  -> 실패한 ref만 stale=true
```

보조 전략:

- MediaStore 변경 감지 또는 다음 분석의 source signature 비교
- detail 진입 시 `ContentResolver.openAssetFileDescriptor` 또는 thumbnail load 실패 확인
- 권한 변화 시 stale를 일괄 확정하지 말고 `UNKNOWN` 상태를 둠
- 다음 Preview/refresh에서 다시 평가

상태는 최소 다음 세 가지가 안전하다.

```text
AVAILABLE
STALE
UNKNOWN
```

boolean `stale`만으로 권한 없음과 실제 삭제를 구분하지 않는다.

---

## 9. 검색 통합 설계

### 9.1 기존 검색의 한계

[StoredAlbumSummarySearch.java](app/src/main/java/com/example/gallerysorter/StoredAlbumSummarySearch.java#L1)은 `StoredAlbumSummary` 목록만 검색한다.

현재 검색 대상:

```text
albumName
countryCode
countryName
adminArea
addressLine
startDate/endDate
relativePath
```

Discovery-only에는 다음이 추가되어야 한다.

```text
placeName
memoryKey
displayName
memo
sourceType
photo count/date fields
```

### 9.2 추천: StoredAlbumSummarySearch를 확장하지 않기

기존 helper의 반환 타입이 `List<StoredAlbumSummary>`이므로 Discovery-only를 넣으려면 model 의미가 깨진다.

새 계층을 둔다.

```text
MemorySearchService.search(List<MemoryRecord> records, String query)
```

기존 검색은 adapter로 감싼다.

```text
StoredAlbumSummary
  -> MemoryRecord
  -> MemorySearchService
```

### 9.3 검색 index

MVP:

```text
MemoryRecord 목록을 메모리에 로드
검색어 입력마다 local filter
```

대상 field:

```text
memoryKey
placeKey
placeName
canonicalPlaceName
displayName
memo
countryCode
countryName canonical display name
country aliases
adminArea
addressLine
start/end date
sourceType
```

국가 alias는 기존 `CountryIdentityNormalizer`에서 재사용한다. `turkey`, `Türkiye`, `터키`, `튀르키예`, `TR`은 모두 같은 `TR` record를 찾아야 한다.

### 9.4 날짜 검색

기존 날짜 검색을 유지하되 V2에서 다음을 추가한다.

```text
2026-08
2026.08
2026년 8월
8월
2026년 8월 2일
```

날짜 parser를 `MainActivity`에 넣지 않고 `MemoryDateQueryMatcher` 같은 순수 helper로 둔다.

### 9.5 source state 검색

초기에는 UI filter로 충분하다.

```text
앨범 있음
앱에서만 발견
새 사진 있음
앨범 없음
```

문자열 query로 `MIXED`를 직접 검색하는 것은 후순위다.

---

## 10. Gallery 앨범 생성 옵션화

### 10.1 Preview 완료 flow

```text
Preview 완료
  -> 발견한 장소 둘러보기
  -> 전체 앨범 만들기
```

기존 Gallery 생성 CTA는 유지한다.

### 10.2 Memory detail flow

Discovery-only:

```text
사진 보기
이 장소 앨범 만들기
```

Organized:

```text
사진 보기
Gallery에서 보기
```

Mixed:

```text
사진 보기
Gallery에서 보기
새 사진만 추가
```

### 10.3 선택 장소 생성

초기 MVP:

```text
MemoryRecord 단위로 한 장소 선택
```

후속:

```text
여러 장소 선택
선택한 장소만 앨범 만들기
```

### 10.4 기존 SortJob/SortWorker 재사용

재사용 가능 범위:

- `SortJob`의 순차 처리 loop
- `MediaCopyEngine`의 photo copy/video move
- `SortWorker`의 foreground/background 실행
- `SortInputStore`의 persisted input

그대로 재사용하면 안 되는 부분:

- `previewItems`를 MainActivity에서 직접 꺼내는 의존성
- 전체 Preview list를 전제로 하는 입력 생성
- `targetRelativePath` 계산을 UI가 담당하는 구조
- snapshot 저장과 Gallery 파일 작업을 한 transaction으로 처리하는 것

추천:

```text
OrganizePlaceService
  -> MemoryRecord에서 OrganizeInput 생성
  -> SortInputStore 또는 새 OrganizeInputStore 저장
  -> WorkManager SortWorker 실행
  -> 성공 결과 수신
  -> AlbumSummaryHistoryStore append
  -> DiscoverySnapshot은 별도 유지 및 source state 재계산
```

### 10.5 성공/실패 계약

Gallery 파일 작업 성공 여부와 snapshot 저장 성공 여부를 하나의 transaction으로 묶지 않는다.

```text
Gallery organize 성공
  -> organized album ref 갱신

snapshot write 실패
  -> 기존 snapshot 보존
  -> 다음 refresh에서 live album을 다시 발견
```

```text
snapshot write 성공
  -> Gallery organize 실패
  -> sourceType은 DISCOVERED_ONLY 유지
  -> 실패 상태와 retry 가능 표시
```

---

## 11. 저장 및 성능 전략

### 11.1 JSON MVP

초기에는 JSON으로 시작한다.

조건:

- 최신 snapshot 1개
- atomic write
- background read/write
- UI에는 summary 우선
- detail photoRefs lazy load
- snapshot 전체를 앱 시작 main thread에서 parse하지 않음

### 11.2 1만~3만장 성능 리스크

위험:

- 모든 photoRef를 JSONObject로 parse할 때 memory spike
- snapshot 전체 저장 시 긴 write
- Home 진입 시 전체 MediaStore live check를 하면 지연
- 모든 URI를 thumbnail decode하면 I/O 폭증
- History와 snapshot을 각각 MediaStore scan하면 중복 비용

완화:

```text
Home:
  summary-only snapshot read

Detail:
  selected group refs lazy read

Search:
  MemoryRecord summary index only

Stale:
  cover/detail on-demand

Refresh:
  explicit user action 또는 background constrained work
```

### 11.3 Room/SQLite 전환 기준

다음 중 2개 이상이면 Room을 검토한다.

- 3만장 이상에서 snapshot parse/write가 체감 지연
- group/photoRef 부분 업데이트가 빈번함
- 여러 snapshot/history를 동시에 검색해야 함
- MediaStore incremental sync 필요
- stale 상태를 photoRef 단위로 자주 갱신함
- personalization과 memory matching query가 복잡해짐

Room으로 전환하더라도 `DiscoverySnapshotStore` interface와 `MemoryRepository` interface는 유지해 storage 구현만 교체한다.

---

## 12. 단계별 구현 TODO

### Phase 0: 설계 고정 및 현재 동작 보존

- [ ] `AlbumSummaryHistoryStore`를 organized-only로 명시
- [ ] Discovery-only가 기존 history에 들어가지 않는다는 테스트/문서 추가
- [ ] `MemoryRecord` field와 source state 계약 확정
- [ ] `memoryKey`/`placeKey` MVP 정책 확정
- [ ] 기존 `MemoryPersonalizationKey` migration alias 정책 확정
- [ ] 현재 main/home/detail/search regression 기준선 고정

### Phase 1: 모델과 Store만 추가

- [ ] `DiscoveryPhotoRef` 추가
- [ ] `DiscoveryMemoryGroup` 추가
- [ ] `DiscoverySnapshot` 추가
- [ ] `OrganizedAlbumRef` 추가
- [ ] `MemoryRecord` 추가
- [ ] `DiscoverySnapshotStore` 추가
- [ ] JSON schemaVersion 및 atomic write 구현
- [ ] 저장/복원 unit test 추가
- [ ] corrupt JSON이 기존 snapshot을 덮어쓰지 않는지 테스트

### Phase 2: Preview 결과 snapshot 저장

- [ ] `DiscoverySnapshotMapper` 추가
- [ ] `PhotoItem -> DiscoveryPhotoRef` 변환
- [ ] Gallery 전용 field를 snapshot model에 넣지 않기
- [ ] 장소 grouping을 service 계층에서 수행
- [ ] Preview 완료 후 사용자가 선택한 경우 snapshot 저장
- [ ] 저장 실패 시 기존 Gallery 정리 흐름 유지
- [ ] 1만~3만장 sample JSON 크기/parse 시간 측정

### Phase 3: Discovery Memory View

- [ ] `MemoryRepository` 추가
- [ ] Discovery snapshot adapter 추가
- [ ] `StoredAlbumSummary` organized adapter 추가
- [ ] same place matching 및 MIXED merge 추가
- [ ] `MemoryDetailService` 또는 URI 기반 detail loader 추가
- [ ] Preview 완료 후 `발견한 장소 둘러보기` CTA 추가
- [ ] Discovery-only detail에서 원본 URI로 사진 열기
- [ ] Gallery album action은 organized state에 따라 조건부 표시
- [ ] MainActivity는 repository/service 호출만 담당

### Phase 4: Search 통합

- [ ] `MemorySearchService` 추가
- [ ] `StoredAlbumSummarySearch`를 adapter 경로로 유지
- [ ] displayName/memo 검색 추가
- [ ] CountryIdentityNormalizer alias 통합
- [ ] 한국어 날짜 query parser 추가
- [ ] source state filter 추가
- [ ] 검색 결과에서 Memory detail로 직접 이동

### Phase 5: 선택적 Gallery 정리

- [ ] `OrganizePlaceService` 추가
- [ ] MemoryRecord -> organize input 변환
- [ ] 선택한 place만 정리
- [ ] `SortInputStore`/`SortWorker` 재사용 또는 input adapter 추가
- [ ] background 작업과 process death 결과 복구
- [ ] 성공 후 organized album ref 갱신
- [ ] 기존 AlbumSummaryHistoryStore 기록 유지
- [ ] 새 사진만 추가 정책 구현

### Phase 6: migration 및 stale check

- [ ] 기존 history를 organized MemoryRecord로 adapter 변환
- [ ] old personalization key -> memoryKey lazy migration
- [ ] Gallery 삭제 후 Discovery fallback 확인
- [ ] source URI stale 상태 확인
- [ ] permission revoked/returned 시 UNKNOWN -> AVAILABLE 재평가
- [ ] explicit 발견 장소 새로 고침과 정리 기록 새로 고침 분리
- [ ] 필요 시 WorkManager constrained refresh 추가

### Phase 7: Room 검토

- [ ] 실제 1만~3만장 benchmark
- [ ] JSON parse/write 및 home latency 측정
- [ ] stale partial update 빈도 측정
- [ ] incremental MediaStore sync 필요성 판단
- [ ] 기준 충족 시 Room migration 설계

---

## 13. Migration compatibility 표

| 사용자 상태 | 읽을 데이터 | MemoryRecord sourceType | 기본 표시 | 주요 action |
|---|---|---|---|---|
| 기존 앨범만 있음 | AlbumSummaryHistoryStore | ORGANIZED_ALBUM | 기존 앨범/장소 | 사진 보기, Gallery에서 보기 |
| Discovery-only | DiscoverySnapshotStore | DISCOVERED_ONLY | 발견 장소 | 사진 보기, 앨범으로 만들기 |
| 둘 다 있음 | 두 store | MIXED | 하나의 Memory | 사진 보기, Gallery에서 보기, 새 사진 추가 |
| 앨범 삭제, 원본 유지 | history + snapshot | DISCOVERED_ONLY | 앱 안에서 보기 | 다시 앨범 만들기 |
| 원본 일부 stale | snapshot | DISCOVERED_ONLY 또는 MIXED | 사용 가능 수 표시 | stale 안내, 재분석 |
| 권한 일시 상실 | snapshot | 기존 state + UNKNOWN | 마지막 summary 유지 | 권한 복구 후 재확인 |

---

## 14. 테스트 전략

### 14.1 Model/Store

- 빈 snapshot 저장/복원
- 1개 group과 여러 photoRef 저장/복원
- `countryCode` canonicalization
- schemaVersion 미지 값 처리
- corrupt JSON read가 기존 파일을 덮어쓰지 않음
- atomic write 실패 시 기존 파일 보존
- snapshot version 증가

### 14.2 Grouping

- 같은 placeKey/date overlap이 하나의 group으로 merge
- 서로 다른 날짜 여행은 별도 memory로 분리
- countryCode가 같은 장소 표시명의 변경을 흡수
- countryName alias가 달라도 같은 countryCode로 merge
- photo count/date range/cover 계산
- 중복 URI 제거

### 14.3 Source state

- discovery only -> DISCOVERED_ONLY
- live organized only -> ORGANIZED_ALBUM
- 둘 다 -> MIXED
- live album 삭제 -> discovery fallback
- discovery snapshot 없음 -> organized record 유지
- discovery photo 일부 stale -> available/stale count

### 14.4 Personalization

- discovery-only memoryKey에 displayName/memo/cover 저장
- organized로 전환해도 같은 memoryKey면 personalization 유지
- 기존 `path:{relativePath}` memo를 new memoryKey로 lazy migration
- old alias와 new displayName이 동시에 있을 때 new record 우선
- stale user cover가 snapshot cover로 fallback

### 14.5 Search

- 일본 / Japan / JP
- 터키 / Turkey / Türkiye / 튀르키예 / TR
- 장소명 / romanized place alias
- displayName
- memo
- `2026-08`, `2026.08`, `2026년 8월`, `8월`
- source state filter
- mixed record가 중복 검색 결과를 만들지 않음

### 14.6 Organize

- discovery-only 한 장소 정리
- 이미 일부 organized인 장소의 새 사진만 정리
- duplicateInTarget skip
- 사진은 copy, 동영상은 설정에 따라 move
- Worker process death 후 결과 복구
- organize 실패 시 discovery snapshot 보존
- history append 실패 시 기존 history와 snapshot 보존

### 14.7 UI/device

- Preview 완료 후 Discovery View 진입
- Gallery를 만들지 않은 상태에서 사진 열기
- 일부 앨범만 생성한 상태에서 중복 카드 없음
- Gallery 앨범 외부 삭제 후 앱 Memory 유지
- 원본 삭제/권한 변경 후 stale UI
- Fold/wide layout
- 1만~3만장 Home 진입 latency
- back navigation: Home -> Memory -> Photo -> Home

---

## 15. 절대 피해야 할 shortcut

### 15.1 AlbumSummaryHistoryStore에 Discovery-only를 섞기

`relativePath` 전제가 깨지고 기존 live check/detail/search가 불안정해진다.

### 15.2 relativePath 없는 Memory를 StoredAlbumSummary로 변환하기

저장 모델의 의미가 달라진다. 별도 `DiscoveryMemoryGroup` 또는 `MemoryRecord`를 사용한다.

### 15.3 MainActivity에 로직을 계속 추가하기

다음 코드를 Activity에 넣지 않는다.

- snapshot schema
- URI stale loop
- Memory merge
- search alias/date parser
- organize selection

### 15.4 앱 시작마다 MediaStore 전체 scan

Home 첫 draw와 navigation latency를 악화시킨다. summary-first와 explicit refresh를 사용한다.

### 15.5 원본 URI만 저장하고 stale 처리 생략

URI 권한·삭제·MediaStore ID 변화가 있다. metadata와 상태를 함께 저장하고 on-demand 검증한다.

### 15.6 placeName 문자열만 key로 사용

locale·표시 정책·동명이인·날짜 session을 구분하지 못한다.

### 15.7 기존 Gallery history를 덮어쓰기

기존 사용자의 organized record와 Gallery 폴더 관계를 잃을 수 있다.

### 15.8 discovery 저장과 파일 이동을 같은 transaction으로 묶기

MediaStore 파일 작업과 JSON 저장은 같은 transaction이 아니다. 어느 쪽이 실패해도 다른 쪽을 잃지 않도록 보상/재동기화 전략을 둔다.

### 15.9 모든 photoRef를 Home에서 즉시 thumbnail decode

대량 사진에서 I/O와 메모리 사용이 급증한다. cover 한 장과 lazy detail을 사용한다.

### 15.10 unknown 국가·장소를 문자열 alias로 억측

국가 identity 도입 취지와 충돌한다. canonical data가 없으면 unknown으로 유지한다.

---

## 16. 최종 추천

### 지금 바로 고정할 결정

```text
1. AlbumSummaryHistoryStore는 organized-only로 유지
2. DiscoverySnapshotStore는 원본 URI 기반 별도 저장소로 추가
3. MemoryRepository가 두 source를 통합
4. MemoryRecord는 sourceType과 action capability를 노출
5. Personalization key는 source-independent memoryKey로 이동
6. Search는 StoredAlbumSummarySearch 확장이 아니라 MemorySearchService로 분리
7. Preview 완료 후 Discovery View를 먼저 열 수 있게 설계
8. Gallery 생성은 OrganizePlaceService를 통해 선택적으로 실행
9. 초기 저장은 JSON, 대용량 병목 시 Room 검토
10. MainActivity는 orchestration만 담당
```

### MVP 범위

가장 작은 MVP는 다음이다.

```text
Preview 완료
  -> 발견한 장소 둘러보기
  -> 최신 DiscoverySnapshot 저장
  -> 장소별 MemoryRecord 목록 생성
  -> 장소별 원본 URI detail
  -> 기존 organized album과 같은 장소인지 merge
  -> 기존 Gallery 앨범 만들기 CTA 유지
```

첫 MVP에서 반드시 구현할 범위:

```text
1. Preview/분석 결과를 DiscoverySnapshot JSON으로 저장
2. DiscoverySnapshot에서 장소별 MemoryRecord 목록 생성
3. Gallery 앨범을 만들지 않고 PhotoPlace 안에서 원본 사진 보기
4. 기존 AlbumSummaryHistoryStore 기록과 중복 카드 없이 merge
5. 기존 Gallery 앨범 만들기 CTA를 선택 action으로 유지
```

이 범위에서는 기존 organizer 흐름을 바꾸지 않는다. 새 discovery layer가 먼저 결과를 제공하고, 기존 Gallery 정리는 사용자가 선택했을 때만 실행된다.

첫 MVP에서 구현하지 않을 것:

- Room/SQLite 도입
- 복잡한 stale 상태와 background crawler
- 여행 session 자동 묶기
- 여러 snapshot history와 diff/undo
- 사용자가 장소를 merge/split하는 기능
- 전체 MediaStore incremental sync
- full migration/backfill
- 자동 Gallery 구조 변경

초기 stale 처리는 복잡한 상태 모델 대신 최소 동작으로 제한한다.

```text
사진 URI를 열 수 있음 -> 사진 보기
사진 URI를 열 수 없음 -> 해당 항목을 detail에서 제외하거나 간단한 불러오기 실패 표시
다음 Preview/분석에서 다시 snapshot 생성
```

즉 MVP에서는 `AVAILABLE / STALE / UNKNOWN`을 완전한 저장 상태로 도입하지 않는다. 이 상태 모델은 snapshot이 실제 사용자에게 유용하다는 것이 확인된 뒤 추가한다.

### MVP 구현 순서

```text
Phase 1. DiscoverySnapshot 모델/JSON Store 추가
  - 기존 AlbumSummaryHistoryStore와 별도 파일
  - atomic write/read
  - PhotoItem -> DiscoveryPhotoRef 변환

Phase 2. MemoryRecord adapter 추가
  - DiscoverySnapshot -> MemoryRecord
  - StoredAlbumSummary -> MemoryRecord
  - placeKey/date overlap 기반의 최소 merge
  - duplicate card 방지

Phase 3. 앱 내부 Memory Detail 추가
  - 원본 photo URI 기반 사진 보기
  - Gallery relativePath에 의존하지 않는 detail 경로
  - cover와 기본 count 표시

Phase 4. Preview 완료 action 연결
  - 발견한 장소 둘러보기
  - 기존 전체 앨범 만들기 CTA 유지
  - discovery 저장 실패 시 기존 organizer 흐름 유지
```

Phase 1~4가 완료되기 전에는 검색 통합, personalization key 전환, 선택 장소 background organize를 필수 범위로 확장하지 않는다.

### V1/V2 책임 경계

PhotoPlace V1은 Gallery organizer다.

```text
분석 -> PhotoItem -> SortJob/SortWorker -> Gallery album -> AlbumSummaryHistoryStore
```

PhotoPlace V2는 V1 위에 discovery layer를 별도로 얹는다.

```text
분석 -> PhotoItem -> DiscoverySnapshotStore -> MemoryRecord -> PhotoPlace 내부 보기
                                      \
                                       -> 사용자가 선택할 때 기존 organizer 실행
```

따라서 첫 V2 구현에서 기존 모델과 흐름을 뜯어엎지 않는다. 새 저장소와 새 adapter를 추가해 두 source를 `MemoryRecord`로 통합한다. `AlbumSummaryHistoryStore`는 계속 실제 Gallery 앨범 기록의 소유자로 남긴다.

이 분리는 다음 위험을 줄인다.

- Discovery-only record가 기존 live album check에서 사라지는 문제
- `relativePath` 없는 record가 기존 detail renderer와 충돌하는 문제
- 기존 사용자 history가 새 snapshot 저장 때문에 덮어써지는 문제
- Gallery 파일 작업과 discovery JSON 저장을 하나의 transaction처럼 취급하는 문제

### 성공 기준

- Gallery 앨범을 만들지 않아도 Preview 후 장소를 다시 열 수 있음
- 기존 Gallery 사용자 기록이 사라지지 않음
- Discovery-only와 organized record가 중복 카드로 나타나지 않음
- 원본 URI가 열리지 않는 항목을 stale로 표시함
- `일본`, `japan`, `turkey`, `튀르키예` 검색이 MemoryRecord 기준으로 동작함
- 사용자의 displayName/memo/cover가 source state 변경 후 유지됨
- 앱 시작과 Home 첫 화면에서 전체 MediaStore scan을 강제하지 않음
- 기존 SortJob/SortWorker Gallery 정리 기능이 그대로 동작함

이 설계는 기존 Organizer 기능을 보존하면서 PhotoPlace를 장소 기반 Memory View로 확장하는 가장 낮은 위험의 경로다.
