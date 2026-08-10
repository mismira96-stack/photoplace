# Photoplace V2 Product Hypothesis Review

## Discovery-First Experience

작성일: 2026-08-09

## 1. Recommendation

### 결론

- **Discovery First 가설:** Recommend
- **기존 기본 흐름 즉시 교체:x`** Do Not Recommend
- **기존 Organizer 흐름을 유지한 Discovery-only 실험:** Strongly Recommend

Photoplace의 제품 비전은 Gallery 정리보다 장소 발견과 기억 탐색에 가깝다. 또한 실제 사용자 중 일부는 장소 발견에는 관심이 있지만 Gallery에 새 앨범이나 폴더가 생성되는 것은 부담스러워한다.

따라서 다음 방향은 제품적으로 검증할 가치가 있다.

```text
분석
  -> Photoplace 안에서 장소 발견/탐색
  -> 필요할 때만 Gallery 앨범 생성
```

다만 현재 구현은 Gallery 앨범 생성을 명확한 결과로 제공하고 있고, Discovery-only 결과를 장기간 유지하고 탐색하는 기반은 아직 충분하지 않다. 따라서 첫 실행 경험을 즉시 바꾸지 말고, 기존 Preview 완료 후 선택 가능한 Discovery 경로로 먼저 검증하는 것이 안전하다.

## 2. Current Architecture

현재 실제 흐름은 다음과 같다.

```text
MediaStore scan
  -> 위치/날짜 분석
  -> PhotoItem 생성
  -> targetRelativePath 계산
  -> previewItems 보관
  -> 사용자 확인
  -> SortJob 또는 SortWorker
  -> MediaCopyEngine
  -> Gallery 앨범 생성/이동
  -> AlbumSummaryHistoryStore 저장
  -> 실제 생성 앨범 기반 history/detail 표시
```

### Analysis

핵심 분석 흐름은 [MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java#L1180)의 Preview 실행 경로가 소유한다.

분석 시 수행하는 작업:

- 기존 Gallery 폴더 조회
- 선택된 source folder의 MediaStore 항목 조회
- 촬영 날짜 읽기
- 위치 정보와 주소 분석
- 국가/행정구역/주소 계산
- 장소명 정책 적용
- `PhotoItem` 생성
- `previewItems`에 저장

`PhotoItem`은 다음 데이터를 이미 갖고 있다.

```text
uri
name
mimeType
takenAt
locationKey
noLocation
targetExists
duplicateInTarget
targetRelativePath
video
countryName
adminArea
addressLine
```

따라서 분석 결과 자체는 Discovery 기능에 재사용할 수 있다.

### Grouping

현재 별도의 `DiscoveryGroup` 또는 `PlaceGroup` 모델은 없다. 장소 grouping의 실질적인 기준은 다음 값들에 분산되어 있다.

```text
locationKey
targetRelativePath
AlbumSummary
AlbumFolder
albumCandidateGroupKey(...)
```

현재 구조는 사실상 다음과 같다.

```text
locationKey
  -> targetRelativePath 계산
  -> Gallery album 후보
```

이 때문에 grouping과 Gallery organization이 완전히 분리되어 있지는 않다.

### Preview

Preview는 Gallery를 실제로 생성하지 않고도 완료된다.

```text
runPreview()
  -> PhotoItem 목록 생성
  -> 장소/날짜/위치 통계 계산
  -> previewItems 보관
  -> Preview 결과 렌더링
```

이 지점이 Discovery-only 실험을 삽입하기 가장 좋다.

현재 Preview 완료 후에는 주로 다음 행동이 노출된다.

```text
Preview 완료
  -> Gallery 앨범 정리
```

실험에서는 다음 행동을 추가할 수 있다.

```text
Preview 완료
  -> 발견한 장소 보기
  -> 필요하면 Gallery 앨범 정리
```

### Album Creation

앨범 생성은 [SortJob.java](app/src/main/java/com/example/gallerysorter/SortJob.java#L22), `SortWorker`, `MediaCopyEngine`가 담당한다.

주요 동작:

- 위치 정보 없는 항목 skip
- 동영상 이동 설정 적용
- 기존 target 중복 확인
- 사진 복사
- 동영상 이동
- `targetRelativePath`에 따른 MediaStore 저장
- 정리 결과와 진행 상태 저장

이 단계는 Discovery와 분리할 수 있다. Discovery-only 경로에서 `SortJob`과 `MediaCopyEngine`를 호출하지 않으면 원본 Gallery를 변경하지 않을 수 있다.

### History

[AlbumSummaryHistoryStore.java](app/src/main/java/com/example/gallerysorter/AlbumSummaryHistoryStore.java#L1)은 정리 세션과 생성된 앨범의 요약을 저장한다.

저장 시점:

```text
SortJob 완료
  -> saveAlbumSummaryHistory()
  -> AlbumSummaryHistoryStore.appendSession()
```

주요 저장 필드:

```text
albumName
relativePath
itemCount
startDate
endDate
thumbnailUri
countryName
adminArea
addressLine
createdAt
```

현재 history는 순수한 분석 결과라기보다 다음 의미에 가깝다.

> 생성된 Gallery 앨범 또는 기존 Gallery 앨범의 요약 기록

따라서 Discovery-only 결과를 기존 history에 그대로 넣으면 의미와 전제조건이 충돌한다.

### Detail

최근 장소와 상세 화면은 현재 실제 Gallery 앨범에 의존한다.

```text
showRecentPlaceDetailScreen(summary)
  -> summary.relativePath 사용
  -> loadLatestAlbumPreviewUris(relativePath, ...)
  -> MediaStore에서 해당 폴더의 사진 조회
```

관련 의존 지점:

- `filterLiveStoredAlbumSummaries()`
- `hasLiveMediaInAlbum()`
- `loadLatestAlbumPreviewUris()`
- `loadStoredAlbumThumbnailInto()`
- Gallery 앨범 열기 동작

`StoredAlbumSummary`에 `relativePath`가 없고 원본 URI만 있는 Discovery record는 기존 detail을 그대로 사용할 수 없다.

## 3. Coupling Analysis

### Analysis와 Album Creation

분석 알고리즘 자체는 `PhotoItem`을 만들기 때문에 어느 정도 재사용 가능하다.

하지만 `PhotoItem` 생성 시 분석 데이터와 Gallery organization 데이터가 함께 계산된다.

```text
장소 발견 정보
+
Gallery targetExists
Gallery duplicateInTarget
Gallery targetRelativePath
```

결합도: **Medium**

분석을 새로 만들 필요는 없지만, Discovery-only에서는 Gallery 전용 필드를 무시하거나 별도 모델로 분리해야 한다.

### Grouping과 Album Creation

현재 grouping이 독립 모델이 아니라 `locationKey`와 `targetRelativePath`에 걸쳐 있다.

결합도: **Medium to High**

특히 다음은 실제 Gallery 폴더를 전제로 한다.

- `findMatchingAlbum(...)`
- `hasDisplayNameInPath(...)`
- `duplicateInTarget`
- `targetExists`
- `collectExistingAlbumSummaries(...)`
- `AlbumFolder`

### History와 Album Creation

결합도: **High**

현재 history는 생성된 앨범의 `relativePath`와 MediaStore 존재 여부를 중요하게 사용한다. Discovery-only 결과를 이곳에 넣으면 다음 문제가 발생할 수 있다.

- `relativePath`가 비어 있음
- live album 필터에서 제거됨
- detail이 MediaStore 폴더 조회에 실패함
- 실제 앨범 수와 발견 장소 수가 섞임

### Detail과 Gallery

결합도: **High**

현재 detail은 “장소별 원본 사진 탐색기”가 아니라 “생성된 Gallery 앨범 상세”에 가깝다.

따라서 Discovery-only를 위해서는 별도의 원본 URI 기반 detail renderer가 필요하다.

### MediaStore 역할 분리

MediaStore는 현재 두 가지 역할을 한다.

```text
1. 원본 사진 분석 대상 조회
2. 생성된 Gallery 앨범 존재/사진 조회
```

Discovery-only에서는 1번은 유지하고 2번은 별도 경로로 처리해야 한다.

## 4. Estimated Complexity

### 전체 기본 흐름 전환: High

다음 변경은 High다.

```text
Install
  -> Discover
  -> Explore
  -> Optional Organization
```

이유:

- `MainActivity`가 Preview, Result, Copy, History, Detail을 함께 소유
- history가 생성 앨범 중심
- detail이 `relativePath` 기반
- WorkManager가 sorting 전용
- Discovery result 영속 모델이 없음
- 원본 URI 기반 detail/복원 흐름이 없음

### 작은 Discovery-only 실험: Medium

기존 Preview 결과를 재사용하고 별도 저장/상세 화면만 추가하면 Medium 범위다.

필수 추가 요소:

- Discovery result 모델
- Discovery result 저장소
- 원본 URI 기반 detail
- Preview 완료 후 진입 버튼

## 5. Minimum Viable Experiment

### 추천 실험

기존 Organizer 흐름을 유지하면서 Preview 완료 후 Discovery를 선택 가능하게 한다.

```text
기존 설치
  -> 권한
  -> 기존 Preview/분석
  -> 발견 결과 저장
  -> Photoplace 내부 장소 탐색
  -> 필요하면 Gallery 앨범 생성
```

첫 실험에서는 다음을 하지 않는다.

- 첫 실행 onboarding 전체 교체
- WorkManager를 Discovery용으로 재작성
- 기존 history 포맷 변경
- Gallery 생성 흐름 제거
- Home 전체 재설계
- displayName/userCover/search를 한 번에 추가

### 최소 모델

```text
DiscoverySession
  sessionId
  createdAtMillis
  totalPhotoCount
  placeCount
  startDate
  endDate
  places
```

```text
DiscoveryPlace
  placeId
  groupKey
  placeName
  countryName
  adminArea
  addressLine
  photoCount
  startDate
  endDate
  coverUri
  photoUris
```

Preview 완료 시 `previewItems`를 `locationKey` 기준으로 묶어 다음을 계산할 수 있다.

```text
PhotoItem
  -> group by locationKey
  -> count
  -> date range
  -> representative URI
  -> source URI list
```

`targetRelativePath`는 Discovery record에 저장하지 않는다.

```text
Discovery result
  = locationKey + source photo URI + metadata

Organization result
  = targetRelativePath + copy/move status
```

### Discovery 저장소

기존 `AlbumSummaryHistoryStore`에 억지로 넣지 말고 별도 저장소를 둔다.

```text
DiscoveryResultStore
  -> discovery_history.json
```

이유:

- Organizer history와 의미가 다름
- `relativePath` 없는 record를 기존 live filter가 처리하지 못함
- 기존 detail 전제조건을 보존할 수 있음
- 향후 공통 Memory 모델로 migration하기 쉬움

### Discovery Detail

기존 `showRecentPlaceDetailScreen(StoredAlbumSummary)`를 바로 재사용하지 않는다.

별도 흐름 후보:

```text
showDiscoveryPlaceDetailScreen(DiscoveryPlace place)
```

이 화면은:

- `coverUri`로 대표 사진 표시
- `photoUris` 중 유효한 URI만 thumbnail 로딩
- `ContentResolver` 또는 thumbnail API로 원본 사진 접근
- `loadLatestAlbumPreviewUris(relativePath, ...)`를 호출하지 않음
- Gallery 폴더 열기 버튼은 숨기거나 별도 Optional action으로 제공

### Preview 버튼 흐름

```text
분석 완료

[ 발견한 장소 보기 ]
[ Gallery 앨범으로 정리 ]
```

기존 정리 버튼을 제거하거나 의미를 바꾸지 않고, 첫 실험에서는 별도 action으로 추가한다.

### 실험 성공 기준

기능보다 사용자 행동을 측정한다.

- 발견한 장소 보기 클릭률
- 장소 detail 진입률
- 장소당 사진 열람 수
- Gallery 앨범 생성 전환율
- 7일 재방문율
- Organizer 사용자의 기존 정리 흐름 유지율
- Explorer 사용자의 Gallery 생성 없이 만족도

추천 이벤트:

```text
preview_complete
discovery_opened
discovery_place_opened
photos_viewed
gallery_organization_started
gallery_organization_completed
```

## 6. Risks

### 기술적 위험

#### 원본 URI invalidation

Discovery-only는 생성 앨범 URI가 아니라 원본 사진 URI를 저장한다.

문제:

- 사진 삭제
- 휴지통 이동
- 다른 앱에서 이동
- MediaStore ID 변경
- URI 접근 권한 변화

대응:

- URI를 영구 파일 경로로 취급하지 않음
- 표시 시 유효성 확인
- 무효 URI는 목록에서 제외
- cover가 무효면 다른 URI로 fallback

#### 대용량 저장

수천~수만 장의 URI를 하나의 JSON에 저장하면 다음 문제가 생긴다.

- 파일 크기 증가
- JSON parse 비용 증가
- 앱 시작 지연
- 부분 업데이트 어려움
- 저장 중 손상 위험

첫 실험에서는 저장 사진 수를 제한하거나, 장기적으로 Room/SQLite 또는 MediaStore 재조회 기반 모델을 검토한다.

#### MediaStore 재조회

장소 카드 수가 많을 때 모든 URI의 thumbnail을 동시에 읽으면 성능이 떨어질 수 있다.

대응:

- 초기에는 cover만 로드
- detail 진입 후 lazy thumbnail
- 기존 LruCache 활용
- 화면에 보이는 사진만 로드
- URI 전수 유효성 확인은 백그라운드 처리

#### 위치 분석 재현성

`locationKey`는 Geocoder와 `PlaceNamePolicy` 결과에 의존한다.

- locale에 따라 달라질 수 있음
- 해외 주소 문자열이 바뀔 수 있음
- ward/subLocality가 도시명처럼 사용될 수 있음
- 같은 장소가 여러 문자열로 나뉠 수 있음

Discovery record에는 최소한 다음을 함께 저장한다.

```text
raw/normalized group key
country
admin area
address
```

장기적으로는 `stablePlaceKey`와 displayName을 분리한다.

#### 분석 결과와 현재 MediaStore 불일치

분석 후 사진이 삭제되거나 이동되면 저장된 count와 현재 유효 count가 달라질 수 있다.

```text
저장 당시 270개
현재 유효 264개
```

오류로 처리하기보다 현재 유효 개수와 일부 누락 상태를 보여주는 것이 적절하다.

### 제품 위험

#### Gallery/Google Photos와 차별화 부족

장소별 Grid만 제공하면 Google Photos의 장소 탐색과 겹친다.

Photoplace의 차별화는 다음에 있어야 한다.

- 사용자 개인화 이름
- 메모와 기억
- 정리 기록과 시간 흐름
- 여행/장소 연결
- 재방문과 잊힌 장소 resurfacing
- 설명 가능한 장소 발견

#### Product Identity Dilution

Gallery 앨범 생성이 사라지면 제품의 즉시 효용이 약해질 수 있다. Discovery 결과에서 바로 다음 행동이 가능해야 한다.

- 사진 보기
- 메모 남기기
- 표시 이름 정하기
- 다시 찾기
- 필요하면 Gallery 앨범 만들기

#### Organizer 경험 악화

기존 Organizer 사용자는 분석 후 Gallery 앨범 생성을 기대한다. 기본 흐름을 바꾸면 기존 사용자의 목적이 한 단계 더 멀어질 수 있다.

### 데이터 위험

- Organizer history와 Discovery result를 같은 포맷에 섞을 위험
- 상대 경로가 없는 Discovery record가 기존 live filter에서 사라질 위험
- URI가 무효화된 뒤 오래된 record가 남을 위험
- locationKey 변경으로 동일 장소가 중복될 위험
- displayName/memo personalization key와 Discovery key가 충돌할 위험

### UX 위험

- 첫 실행에서 정리/발견 mode를 고르게 하면 선택 피로 발생
- 분석 완료까지 기다린 뒤 결과가 너무 늦게 나타날 수 있음
- 장소당 대표 사진만 보여주면 “전체 장소 사진” 기대를 충족하지 못함
- Discovery와 Organizer가 별도 제품처럼 보일 수 있음

## 7. Migration Strategy

### 1차: 저장 포맷 분리

기존 포맷을 유지한다.

```text
album_summary_history.json
  = Organizer 정리 기록

discovery_history.json
  = Gallery를 만들지 않은 발견 결과
```

Discovery 결과를 기존 `loadRecentAlbumSummaries()`에 넣지 않는다.

현재 로더는 다음을 전제로 한다.

- albumName 존재
- relativePath 또는 live album 매칭 가능
- MediaStore에 해당 앨범 존재
- thumbnailUri 또는 relativePath로 사진 조회 가능

### 2차: 공통 Memory 모델

Discovery와 Organizer를 한 화면에서 통합해야 할 때 별도 abstraction을 도입한다.

```text
MemoryRecord
  memoryId
  sourceType
  displayName
  memo
  coverUri
  place metadata
  photoCount
  startDate
  endDate
  relativePath(optional)
```

```text
sourceType:
  ORGANIZED_ALBUM
  DISCOVERED_ONLY
```

기존 모델은 adapter로 유지한다.

```text
StoredAlbumSummary -> MemoryRecord(ORGANIZED_ALBUM)
DiscoveryPlace -> MemoryRecord(DISCOVERED_ONLY)
```

### 3차: MemoryPersonalization 연계

현재 개인화 key는 `relativePath` 우선이다. Discovery-only에는 relativePath가 없으므로 별도 key namespace가 필요하다.

후속 방향:

```text
organized:path:<relativePath>
discovered:<stablePlaceKey>
```

단, 이번 Discovery 실험에서 기존 `MemoryPersonalizationKey`를 즉시 바꾸지는 않는다. 먼저 Discovery key와 기존 Organizer key를 분리해 충돌을 피한다.

## 8. Alternative

### Alternative A: Preview 이후 Discovery 경로만 추가

가장 추천하는 저비용 대안이다.

```text
분석 -> 발견 결과 보기
     -> 필요하면 Gallery 정리
```

기존 기본 흐름과 Organizer 사용자를 보존하면서 Explorer 사용자 반응을 확인할 수 있다.

### Alternative B: 기존 Gallery 결과를 Memory 경험으로 강화

Gallery 생성은 유지하고, 생성 후 Photoplace에서 장소 기록/메모/검색을 강화한다.

장점:

- 기술 비용이 가장 낮음
- 현재 history/detail 재사용 가능

단점:

- Explorer 사용자의 Gallery 변경 부담을 해결하지 못함

### Alternative C: “사진을 바꾸지 않고 발견” 버튼

기존 Preview를 재사용하고 `runCopy()`를 호출하지 않는 별도 action을 추가한다.

단, 결과를 재실행 후에도 사용하려면 Discovery store와 원본 URI detail이 필요하다.

### Alternative D: 샘플 장소만 보여주는 Concierge Experiment

전체 결과를 영속화하지 않고 대표 장소 몇 개만 보여준다.

```text
최근 발견한 장소 3개
  -> 대표 사진
  -> 장소명
  -> 사진 수
```

제품 반응을 가장 낮은 비용으로 확인할 수 있지만 실제 Discovery-only의 장기 가치를 완전히 검증하지는 못한다.

## 9. Counterargument

이 아이디어를 지금 기본 경험으로 구현하면 안 되는 가장 강한 이유는 다음과 같다.

> 현재 Photoplace의 가장 명확하고 검증된 결과는 Gallery에 실제 앨범을 만들어주는 것이지만, Discovery-only는 아직 그 대체 가치가 충분히 구현되지 않았다.

현재 Discovery-only에는 다음이 부족하다.

- 독립 영속 모델
- 원본 URI 기반 전체 사진 탐색
- 삭제/이동된 사진 처리
- Discovery detail
- 개인화 이름/메모의 일관된 적용
- 재방문/기억 resurfacing
- Google Photos와 다른 명확한 핵심 경험

즉시 기본 흐름을 바꾸면 다음 교체가 발생한다.

```text
명확한 기존 가치
  -> Gallery 앨범 생성

아직 검증되지 않은 가치
  -> 장소별 탐색
```

따라서 먼저 다음 행동이 실제로 발생하는지 확인해야 한다.

```text
분석 완료
  -> 발견 결과 보기
  -> 장소 detail 탐색
  -> 사진 열기
  -> 메모/표시 이름 저장
  -> 며칠 후 재방문
```

## Final Decision

```text
Discovery First 가설: Recommend
즉시 기본 Flow 교체: Do Not Recommend
Preview 이후 Discovery 실험: Strongly Recommend
```

현재 코드 적합성:

```text
분석 재사용: 가능
grouping 재사용: 부분 가능
Gallery 생성 재사용: 분리 가능
기존 history 직접 재사용: 어려움
기존 detail 직접 재사용: 어려움
Discovery 영속화: 새 모델/Store 필요
```

가장 작은 다음 단계:

```text
1. 기존 Preview 완료 시점 유지
2. `발견한 장소 둘러보기` 버튼 추가
3. previewItems를 임시 DiscoveryPlace로 그룹화
4. 별도 DiscoveryResultStore에 제한적으로 저장
5. 원본 URI 기반 Discovery detail 추가
6. 기존 `Gallery 앨범으로 정리` 버튼 유지
7. 사용자 행동과 재방문 관찰
```

권장 구현 순서:

```text
Preview
  -> DiscoveryPlace 그룹화
  -> DiscoveryResultStore
  -> Discovery detail
  -> 메모/displayName 적용
  -> Discovery 검색
  -> Optional Gallery organization
  -> 첫 실행 Flow 재검토
```

결론적으로 Discovery First는 검증할 가치가 있는 제품 가설이지만, 현재 Photoplace의 기본 흐름을 즉시 대체할 만큼 구현·검증된 상태는 아니다. 기존 Organizer 흐름을 보존한 채 Preview 이후 Discovery-only 경로를 추가하고, 실제 Explorer 사용자의 탐색과 재방문 행동을 측정하는 것이 가장 합리적이다.
