# 정리 기록 검색 MVP 설계

작성일: 2026-08-09

## 목표

정리 기록과 최근 발견한 장소가 많아졌을 때 드래그로 원하는 장소를 찾는 불편을 줄인다.

MVP 범위는 다음과 같다.

- 최근 발견한 장소 화면에 검색 입력창 추가
- 이미 로드된 `StoredAlbumSummary` 목록을 메모리에서 필터링
- 장소명, 국가, 행정구역, 주소, 날짜 검색
- 검색 결과를 누르면 기존 장소 상세 화면으로 이동
- 검색 입력마다 MediaStore를 다시 조회하지 않음
- `MainActivity`에 검색 알고리즘을 직접 넣지 않음

## 설계 판단

현재 최근 장소 화면은 다음 흐름으로 동작한다.

```text
showRecentPlacesScreen()
  -> loadRecentAlbumSummariesForUi()
  -> filterLiveStoredAlbumSummaries()
  -> Grid 카드 렌더링
  -> showRecentPlaceDetailScreen(summary)
```

`StoredAlbumSummary`에는 MVP 검색에 필요한 데이터가 이미 존재한다.

- `albumName`
- `countryName`
- `adminArea`
- `addressLine`
- `startDate`
- `endDate`
- `relativePath`
- `thumbnailUri`

따라서 새 저장소나 새 검색 모델을 만들 필요는 없다. 검색 규칙만 순수 helper로 분리하고, `MainActivity`에는 입력 상태와 결과 영역 갱신만 남기는 것이 최소 변경이다.

## 영향 파일

### 필수 변경

- [MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java#L5381)
  - `showRecentPlacesScreen()`에 검색 입력 UI 추가
  - 검색어 상태 보관
  - 초기 목록과 검색 결과 목록 분리
  - 검색 결과 영역만 갱신
  - 기존 `showRecentPlaceDetailScreen(summary)` 재사용

- 새 파일: `app/src/main/java/com/example/gallerysorter/StoredAlbumSummarySearch.java`
  - 검색어 정규화
  - `StoredAlbumSummary` 필드 검색
  - 목록 필터링
  - Android UI 및 MediaStore 비의존

- 새 파일: `app/src/test/java/com/example/gallerysorter/StoredAlbumSummarySearchTest.java`
  - 검색 규칙 단위 테스트

### 변경하지 않을 파일

- [StoredAlbumSummary.java](app/src/main/java/com/example/gallerysorter/StoredAlbumSummary.java#L5)
  - 현재 검색 대상 필드가 모두 있으므로 모델 변경 불필요

- `AlbumSummaryHistoryStore.java`
  - 검색은 저장 포맷이나 기록 로딩 정책의 문제가 아니므로 변경 불필요

## 새 클래스 후보

### `StoredAlbumSummarySearch`

첫 패치에서 추가할 유일한 새 클래스 후보다.

권장 API 형태:

```java
static List<StoredAlbumSummary> filter(
        List<StoredAlbumSummary> summaries,
        String query)
```

책임:

- 검색어의 앞뒤 공백 제거
- 대소문자 무시
- null 검색어를 빈 검색어로 처리
- 장소명, 국가, 행정구역, 주소, 시작일, 종료일 검색
- 원본 목록을 변경하지 않고 기존 객체를 반환
- 기존 정렬 순서 유지

검색 대상 문자열은 다음 필드를 결합해 만든다.

```text
albumName
countryName
adminArea
addressLine
startDate
endDate
```

날짜는 우선 raw 값인 `yyyy-MM-dd`를 검색 대상으로 삼는다. 필요하면 `-`를 `.`으로 바꾼 변형도 함께 검색한다.

### `RecentPlacesSearchState`

첫 MVP에서는 만들지 않는다.

`MainActivity`에 다음 상태 하나만 두는 것으로 충분하다.

```text
String recentPlacesSearchQuery
```

검색어 복원, 정렬 옵션, 필터 종류, 최근 검색어 저장이 추가될 때 별도 상태 클래스를 검토한다.

### `RecentPlacesListRenderer`

장기적인 추출 후보지만 검색 MVP에서는 만들지 않는다.

현재 `showRecentPlacesScreen()`은 화면 상태, ScrollView, 빈 상태, Grid, 하단 탭, 스크롤 복원을 함께 처리한다. 이 전체를 한 번에 옮기면 변경 범위와 회귀 위험이 커진다.

## UI 흐름

```text
최근 발견한 장소 진입
  -> loadRecentAlbumSummariesForUi()
  -> filterLiveStoredAlbumSummaries()
  -> 초기 목록을 메모리에 보관
  -> 검색 입력창 표시
  -> 입력 시 StoredAlbumSummarySearch.filter()
  -> 결과 영역만 갱신
  -> 카드 클릭
  -> showRecentPlaceDetailScreen(summary)
```

### 화면 구성

헤더 아래, 기존 요약 카드 위에 검색 입력창을 둔다.

```text
최근 발견한 장소

[ 장소, 국가, 주소 검색 ]

전체 장소 요약
[ 장소 카드 ]
[ 장소 카드 ]
```

검색 결과 상태:

- 빈 검색어: 전체 기록 표시
- 결과 있음: 필터링된 카드와 결과 개수 표시
- 결과 없음: `검색 결과가 없어요` 표시

### 상세 화면 이동

검색 결과를 새 모델로 변환하지 않는다. 원본 `StoredAlbumSummary` 객체를 그대로 카드에 전달한다.

기존 흐름을 재사용한다.

```text
카드 클릭
  -> showRecentPlaceDetailScreen(summary)
```

이렇게 해야 `relativePath`, `thumbnailUri`, 장소 메타데이터가 유지되고, 기존 뒤로 가기 흐름도 보존된다.

## 성능 경계

화면 진입 시에는 기존 로딩 흐름을 한 번 수행한다.

```text
filterLiveStoredAlbumSummaries(loadRecentAlbumSummariesForUi())
```

검색 입력 중에는 다음 메서드를 다시 호출하지 않는다.

```text
loadRecentAlbumSummaries()
collectExistingAlbumSummaries()
filterLiveStoredAlbumSummaries()
```

검색 입력에서는 오직 다음 작업만 수행한다.

```text
StoredAlbumSummarySearch.filter(loadedList, query)
```

검색 helper는 다음에 의존하지 않아야 한다.

- `Context`
- `ContentResolver`
- `MediaStore`
- 파일 IO
- UI 상태

## 위험 요소

### 전체 화면 재생성

`TextWatcher`에서 `showRecentPlacesScreen()` 전체를 다시 호출하면 다음 문제가 발생할 수 있다.

- 검색 입력창 포커스 손실
- 키보드 닫힘
- 커서 위치 초기화
- ScrollView 위치 변경
- 하단 탭과 화면 상태 반복 설정

검색 입력창과 결과 영역을 분리하고, 검색 시 결과 컨테이너만 갱신해야 한다.

### MediaStore 재조회

검색어 입력마다 live 앨범 검증을 반복하면 대용량 사진 라이브러리에서 입력이 지연될 수 있다. 초기 화면 진입 시 검증한 목록을 검색 생명주기 동안 재사용한다.

### 날짜 표시 형식

저장 날짜는 `yyyy-MM-dd`, 화면 표시 날짜는 `yyyy.MM` 또는 `최근 yyyy.MM.dd` 형식일 수 있다. MVP에서는 raw 날짜 검색을 우선 지원하고, 점 구분자 검색은 정규화 변형으로 보완한다.

### 앨범 식별 정보 손실

상세 화면 갱신은 `relativePath` 또는 `albumName`을 사용한다. 검색 결과를 별도 객체로 복사하면서 이 값을 빠뜨리면 상세 화면 갱신이 깨질 수 있다. 필터는 기존 객체를 그대로 반환해야 한다.

### 외부 삭제 반영 시점

다른 갤러리 앱에서 앨범이 삭제된 경우 검색 목록에 잠시 남을 수 있다. MVP에서는 기존 정책을 유지한다.

- 목록 진입 시 live 앨범 검증
- 상세 진입 후 기존 `refreshActivePlaceDetailAfterExternalChange()`로 재검증
- 검색 입력마다 실시간 검증하지 않음

### 한글 IME 조합

한글 입력 중간 이벤트마다 무거운 작업을 하지 않는다. helper가 순수 메모리 연산이고 결과 영역만 갱신되면 우선 debounce 없이 시작할 수 있다. 실제 입력 지연이 확인될 때만 짧은 debounce를 추가한다.

## 테스트 케이스

테스트 파일은 기존 JVM 단위 테스트 패턴에 맞춰 작성한다.

### 검색 필드

- 장소명 검색
- 국가명 검색
- 행정구역 검색
- 주소 검색
- 시작일 검색
- 종료일 검색

예시:

```text
"삿포로" -> albumName이 "삿포로 여행"인 항목 반환
"일본" -> countryName이 "일본"인 항목 반환
"札幌市" -> addressLine에 포함된 항목 반환
"2026-08" -> startDate 또는 endDate에 포함된 항목 반환
```

### 정규화

- 대소문자 무시
- 앞뒤 공백 무시
- null 검색어는 전체 목록 반환
- 빈 검색어는 전체 목록 반환
- null 메타데이터에서도 예외 없음
- 필요 시 `-`와 `.` 날짜 검색 동등 처리

### 결과 보존

- 결과 순서가 입력 순서와 동일함
- 입력 목록이 변경되지 않음
- 검색 결과가 원본 `StoredAlbumSummary` 객체를 그대로 가리킴
- `relativePath`, `thumbnailUri`, `createdAtMillis`가 보존됨

### 기기 회귀 테스트

- 최근 발견한 장소 진입
- 검색어 입력과 삭제
- 장소명, 국가, 주소, 날짜 검색
- 결과 0개 상태
- 검색 결과에서 상세 화면 진입
- 상세 화면에서 뒤로 가기
- 검색 목록 복귀 시 검색어와 스크롤 상태
- Home/History 탭 전환
- 화면 회전 또는 Fold 열림/닫힘
- 기록 재생성 직후 검색 목록 갱신
- 검색 입력 중 MediaStore 재조회나 noticeable 지연이 없는지 확인

## 작은 패치 순서

### 1. 순수 검색 helper 추가

`StoredAlbumSummarySearch`를 추가한다.

범위:

- 검색어 정규화
- 검색 필드 결합
- 목록 필터링
- Android 의존성 없음

### 2. helper 단위 테스트 추가

UI 연결 전에 장소명, 국가, 주소, 날짜, 빈 검색어, null 필드, 결과 순서와 원본 불변을 검증한다.

### 3. 최근 장소 화면에 입력창 추가

`showRecentPlacesScreen()`에 검색 입력 UI와 검색어 상태를 연결한다. 이 단계에서 기존 상세 화면과 카드 생성 코드는 재사용한다.

### 4. 초기 목록과 검색 목록 분리

화면 진입 시 live 검증을 거친 목록을 보관한다. 입력 이벤트에서는 보관된 목록만 helper로 필터링한다.

### 5. 결과 영역만 갱신

`showRecentPlacesScreen()` 전체를 다시 호출하지 않고 검색 결과 컨테이너만 비운 뒤 카드 또는 빈 상태를 다시 그린다.

### 6. 기기 회귀 테스트

검색, 상세 진입, 뒤로 가기, 탭 전환, Fold 레이아웃, 기록 재생성 후 갱신을 확인한다.

## 최종 구조

```text
MainActivity
  - 최근 장소 화면 상태와 UI
  - 초기 summary 목록 로드
  - 검색 입력 이벤트 연결
  - 결과 영역 갱신
  - 기존 상세 화면으로 이동

StoredAlbumSummary
  - 기존 데이터 모델 유지

StoredAlbumSummarySearch
  - 검색어 정규화
  - 순수 목록 필터링
  - UI/MediaStore 비의존

AlbumSummaryHistoryStore
  - 기존 기록 저장 및 복원 책임 유지
```

## 결론

정리 기록 검색 MVP는 새 화면, 새 저장소, 새 데이터 모델 없이 구현할 수 있다.

첫 변경 범위는 다음 세 가지로 제한하는 것이 좋다.

1. `StoredAlbumSummarySearch` 순수 helper 추가
2. `MainActivity` 최근 장소 화면에 검색 입력과 결과 영역 연결
3. helper 단위 테스트 추가

가장 중요한 구현 원칙은 검색 입력마다 MediaStore를 다시 조회하지 않고, 초기 화면 로딩 결과를 메모리에서 빠르게 필터링하는 것이다.
