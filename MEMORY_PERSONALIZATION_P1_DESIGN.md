# Memory Personalization P1 설계

작성일: 2026-08-09

## 이 작업은 리팩토링인가?

순수한 의미의 리팩토링만은 아니다.

이번 작업은 다음 두 성격이 함께 있다.

```text
기존 메모 저장 책임 분리
  = 구조 리팩토링

displayName / userCoverUri 지원을 위한 모델과 저장 기능 추가
  = 기능 기반 추가
```

따라서 정확히는 다음과 같이 부르는 것이 적절하다.

> 기존 메모 동작을 보존하면서 개인화 저장 경계를 분리하고, 이후 이름/대표 사진 기능을 위한 기반을 추가하는 호환성 중심 리팩토링

## 리팩토링에 해당하는 부분

현재 `MainActivity`는 다음 책임을 함께 가지고 있다.

- 화면 렌더링
- 장소 상세 화면 상태
- `SharedPreferences` 키 생성
- 메모 읽기/쓰기
- 기존 메모 fallback 처리

현재 메모 호출 흐름:

```text
UI
  -> MainActivity.albumMemory(summary)
  -> SharedPreferences
```

이를 다음처럼 바꾸는 것은 구조 리팩토링이다.

```text
UI
  -> MainActivity의 얇은 위임
  -> MemoryPersonalizationStore
  -> JSON / legacy SharedPreferences
```

화면 동작과 기존 메모 데이터를 유지한다면, 이 부분은 외부 동작을 보존하는 리팩토링으로 볼 수 있다.

## 기능 추가에 해당하는 부분

다음은 기존에 없던 사용자 기능이다.

- 앱 내부 표시 이름 `displayName`
- 사용자 대표 사진 override `userCoverUri`
- 개인화 데이터 JSON 저장
- displayName 검색
- 향후 기억해 둔 장소 영역

특히 `displayName`을 저장하고 UI에 표시하는 순간부터는 새 기능이다. `userCoverUri` 역시 자동 썸네일을 override하는 사용자 기능이므로 단순 리팩토링이 아니다.

## 제품 경계

### Gallery 폴더명

변경하지 않는다.

```text
relativePath = 실제 Gallery 폴더 식별자
albumName    = 자동으로 만든 원래 장소명
```

### displayName

앱 내부에서만 사용하는 표시 이름이다.

예:

```text
자동 앨범명: 삿포로에서
displayName: 2026 삿포로 여행
```

변경하지 않는 것:

- 실제 Gallery 폴더명
- `relativePath`
- MediaStore 경로
- 사진 파일 이름
- 사진 파일 자체

표시 우선순위:

```text
displayName이 비어 있지 않음 -> displayName
그 외 -> StoredAlbumSummary.albumName
```

### userCover

사진 편집 기능이 아니다.

```text
userCoverUri = 사용자가 고른 기존 사진의 대표 URI
thumbnailUri = 자동으로 고른 대표 URI
```

표시 우선순위:

```text
유효한 userCoverUri
  -> thumbnailUri
  -> 기존 fallback preview
```

사진을 crop, filter, rename, move하지 않는다.

## 현재 코드 기준

### 자동 데이터 모델

`StoredAlbumSummary`는 자동 발견/history 데이터로 유지한다.

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
```

여기에 사용자 값을 직접 추가해 자동 history 데이터와 개인화 상태를 섞는 것은 첫 단계에서 피한다.

### 현재 메모 키

현재 `MainActivity`는 `SharedPreferences` 파일 `album_sorter`에 다음 prefix를 사용한다.

```text
album_memory_
album_alias_
```

실제 key identity:

```java
Uri.encode(firstNonEmpty(relativePath, albumName, ""))
```

읽기 순서:

```text
album_memory_<encoded identity>
  -> 값이 비어 있으면
album_alias_<encoded identity>
```

쓰기 동작:

```text
album_alias_<key> 삭제
album_memory_<key>에 메모 저장
```

중요한 점:

> `album_alias_`라는 이름만 보고 기존 값을 displayName으로 해석하면 안 된다. 현재 동작에서는 `albumMemory()`의 legacy memo fallback이다.

## 새 모델 제안

새 파일 후보:

```text
app/src/main/java/com/example/gallerysorter/MemoryPersonalization.java
```

권장 모델:

```java
final class MemoryPersonalization {
    final String memoryKey;
    final String displayName;
    final String memo;
    final String userCoverUri;
    final long updatedAtMillis;
}
```

필드 의미:

- `memoryKey`: 개인화 저장용 식별자
- `displayName`: 앱 내부 표시 이름 override
- `memo`: 기존 `albumMemory()`에서 이전되는 사용자 메모
- `userCoverUri`: 사용자 대표 사진 URI
- `updatedAtMillis`: 마지막 변경 시각

메모 삭제와 legacy 재부활을 명확히 처리해야 할 경우 다음 필드를 추가 검토한다.

```java
final boolean memoOverride;
```

## stable key 제안

첫 단계에서는 `relativePath`를 우선한다.

```text
relativePath가 있음:
  path:<relativePath>

relativePath가 없음:
  album:<albumName>
```

예:

```text
path:Pictures/삿포로에서/
album:삿포로에서
```

key 생성은 별도 클래스에 격리한다.

```text
MemoryPersonalizationKey
```

규칙:

- relativePath 우선
- trailing slash를 임의로 제거하지 않음
- 대소문자 변경 금지
- URL decode/encode 중복 금지
- MainActivity가 key를 직접 만들지 않음

`albumName` fallback은 충돌 가능성이 있으므로 임시 fallback으로만 취급한다. 장기적으로는 stable memory key를 별도 도입해야 한다.

## 저장 구조 제안

새 파일:

```text
memory_personalization.json
```

예시:

```json
{
  "schemaVersion": 1,
  "updatedAtMillis": 1750000000000,
  "memories": {
    "path:Pictures/삿포로에서/": {
      "displayName": "2026 삿포로 여행",
      "memo": "친구와 여름 휴가",
      "userCoverUri": "content://media/external/images/media/123",
      "updatedAtMillis": 1750000000000
    }
  }
}
```

`memories`는 배열보다 map이 적절하다.

- key로 바로 조회 가능
- 중복 방지
- 부분 업데이트 쉬움
- 특정 앨범 삭제 쉬움

## `MemoryPersonalizationStore` 책임

새 파일 후보:

```text
app/src/main/java/com/example/gallerysorter/MemoryPersonalizationStore.java
```

책임:

- JSON 읽기/쓰기
- schemaVersion 관리
- memoryKey 생성
- 개인화 값 조회/저장/삭제
- legacy SharedPreferences fallback
- legacy memo migration
- 부분 필드 업데이트
- 저장 실패 시 원본 보존

권장 API:

```java
MemoryPersonalization get(StoredAlbumSummary summary)

String displayNameFor(StoredAlbumSummary summary)
String memoFor(StoredAlbumSummary summary)
String coverUriFor(StoredAlbumSummary summary)

void saveDisplayName(StoredAlbumSummary summary, String displayName)
void saveMemo(StoredAlbumSummary summary, String memo)
void saveUserCoverUri(StoredAlbumSummary summary, String uri)
void clear(StoredAlbumSummary summary)
```

MainActivity에는 다음 정도만 남긴다.

```text
store 초기화
store 조회
store 저장 호출
effective 값으로 렌더링
```

개인화 정책과 key 생성은 MainActivity에 두지 않는다.

## 기존 메모 마이그레이션

### 권장 방식

lazy read-through migration을 사용한다.

```text
1. 새 JSON store에서 memoryKey 조회
2. 값이 있으면 새 값 반환
3. 없으면 legacy SharedPreferences 조회
4. album_memory_ 우선 확인
5. 비어 있으면 album_alias_ 확인
6. legacy 값을 새 memo로 변환
7. 새 JSON 저장 성공
8. 성공 후 legacy key 삭제 또는 migration marker 처리
```

### 데이터 보호 원칙

다음 순서를 반드시 지킨다.

```text
새 JSON 저장 성공
  -> legacy 삭제
```

다음은 금지한다.

```text
새 JSON 저장 실패
  -> legacy 삭제
```

기존 메모를 잃지 않기 위해 JSON 저장 실패 시 legacy 값을 그대로 둔다.

### `album_alias_` 처리

기존 값은 displayName으로 승격하지 않는다.

```text
album_memory_ -> memo
album_alias_  -> memo fallback
새 displayName  -> 빈 값으로 시작
```

### 메모 삭제 문제

새 store에서 메모를 삭제했는데 legacy 키에 과거 값이 남아 있으면, 다음 조회에서 과거 메모가 다시 살아날 수 있다.

해결책은 둘 중 하나다.

#### 권장안 A: 성공 후 legacy key 삭제

```text
legacy read
  -> 새 JSON 저장 성공
  -> legacy key 삭제
```

구현이 단순하고 이후 새 store를 단일 진실 공급원으로 만들 수 있다.

#### 대안 B: migration marker/tombstone

```json
{
  "memo": "",
  "memoOverride": true
}
```

`memoOverride=true`이면 legacy를 다시 읽지 않는다. downgrade 호환성을 더 오래 유지해야 할 때 사용한다.

첫 P1 패치에서는 성공 후 legacy 삭제 방식이 더 작은 변경이다. 단, JSON 저장 성공을 확인한 뒤에만 삭제해야 한다.

## displayName 검색 연동

현재 `StoredAlbumSummarySearch`는 summary의 자동 필드만 검색한다.

```text
albumName
countryName
adminArea
addressLine
startDate
endDate
relativePath
```

displayName은 summary에 직접 넣지 않는 방향이므로, 검색 시 개인화 overlay가 필요하다.

### 권장 흐름

```text
최근 장소 목록 로드
  -> personalization을 한 번 읽음
  -> summary별 displayName overlay 준비
  -> 메모리에서 검색
```

검색 입력마다 다음을 호출하지 않는다.

```text
MemoryPersonalizationStore 파일 읽기
MediaStore 조회
```

### 1차 최소 연동

기존 카드 API와 `StoredAlbumSummary`를 유지하려면 displayName map을 전달하는 방식이 가장 작다.

```java
StoredAlbumSummarySearch.filter(
        summaries,
        query,
        displayNameByMemoryKey
)
```

다만 key 생성 규칙이 helper에 새어 나오지 않도록, 장기적으로는 다음 중 하나로 발전시킨다.

```text
옵션 A: PersonalizedMemory 목록을 검색
옵션 B: 검색용 resolver가 summary와 personalization을 결합
```

첫 구현에서는 옵션 B 또는 displayName lookup 함수가 적절하다.

첫 검색 연동 범위는 다음으로 제한한다.

```text
displayName + 기존 검색 대상
```

memo 검색은 별도 제품 결정을 거친 뒤 추가한다.

## 표시 계층 제안

UI 여러 곳에서 `storedAlbumSummary.albumName`을 직접 쓰고 있다.

- 상세 헤더
- 상세 카드 제목
- Grid 카드
- compact 카드
- row 카드
- 메모 편집 화면

이 값을 화면마다 따로 바꾸면 불일치가 생긴다.

### 장기 후보: `PersonalizedMemory`

```text
PersonalizedMemory
  summary
  personalization
  effectiveDisplayName
  effectiveMemo
  effectiveCoverUri
```

첫 패치에서는 `MemoryPersonalization`과 store를 먼저 만들고, 이후 presentation 계층을 도입하는 것이 안전하다.

최종 UI 원칙:

```text
모든 memory UI
  -> effectiveDisplayName()
  -> effectiveCoverUri()
  -> effectiveMemo()
```

## userCover 적용 원칙

모델과 JSON 필드는 첫 단계에 포함할 수 있지만, 선택 UI는 별도 패치로 미룬다.

대표 사진 로딩:

```text
userCoverUri가 있고 유효함
  -> userCoverUri 사용
그 외
  -> summary.thumbnailUri 사용
```

URI가 삭제되거나 접근 불가여도 앱이 죽지 않아야 한다.

- 로딩 실패 시 자동 thumbnail fallback
- 실패 즉시 저장 record를 삭제하지 않음
- 원본 사진 수정/이동/crop/filter 없음

## 위험 요소

### 1. Gallery 폴더명 변경 오해

displayName 저장 시 다음 동작은 금지한다.

- MediaStore `relative_path` 변경
- 파일 rename
- 폴더 이동
- 새 Gallery 앨범 생성

### 2. 기존 메모 손실

특히 다음을 피해야 한다.

- JSON 저장 전 legacy 삭제
- `album_alias_`를 displayName으로 오해
- 빈 새 record가 legacy memo를 덮어씀
- legacy key 생성 규칙 변경
- 삭제한 메모가 legacy에서 부활

### 3. key 변경

relativePath 표기 변화는 새 memory로 인식될 수 있다.

- trailing slash 차이
- 실제 폴더 이동
- 외부 앱 변경
- path 표기 변화

첫 MVP에서 자동 alias matching을 넣지 않는다. identity migration은 별도 작업으로 둔다.

### 4. 동시 JSON 쓰기

빠른 연속 저장이나 Activity 생명주기 전환 중 read-modify-write가 겹칠 수 있다.

최소 보호:

- `synchronized` store 메서드
- 임시 파일 저장 후 교체
- 손상 파일 발생 시 backup/fallback 고려

### 5. userCoverUri 권한

사진 삭제, 휴지통 이동, URI 접근 권한 변경으로 userCover가 무효가 될 수 있다. 자동 thumbnail로 fallback하고 저장값은 즉시 지우지 않는다.

### 6. 화면별 표시 이름 불일치

일부 화면만 displayName을 적용하면 자동 이름과 사용자 이름이 섞인다. 공통 표시 helper 또는 presentation 모델을 사용한다.

### 7. 검색 성능

검색 입력마다 JSON store나 MediaStore를 읽지 않는다. 목록 진입 때 한 번 로드하고 이후 메모리 필터링만 한다.

### 8. 자동 history rebuild

개인화 값을 history JSON에 섞지 않는다. history rebuild 후에도 별도 personalization store에서 overlay한다.

## 작은 패치 순서

### 1단계: 모델과 key policy

추가 후보:

```text
MemoryPersonalization.java
MemoryPersonalizationKey.java
```

이 단계에서는 UI를 수정하지 않는다.

검증:

- relativePath 우선 key
- albumName fallback
- legacy key 계산이 기존 코드와 동일함

### 2단계: store와 JSON 단위 테스트

추가 후보:

```text
MemoryPersonalizationStore.java
MemoryPersonalizationStoreTest.java
```

검증:

- JSON 저장/복원
- 부분 필드 업데이트
- 여러 앨범 독립 저장
- 손상 JSON 처리
- 저장 실패 시 기존 파일 보존

### 3단계: 기존 메모 위임

기존 메서드는 즉시 삭제하지 않는다.

```text
albumMemory(summary)
  -> memoryPersonalizationStore.memoFor(summary)

saveAlbumMemory(summary, value)
  -> memoryPersonalizationStore.saveMemo(summary, value)
```

UI는 우선 그대로 둔다. 이 단계의 목적은 저장 책임만 MainActivity 밖으로 이동하는 것이다.

### 4단계: legacy 메모 migration 검증

테스트 기기에서 확인:

1. 구버전에서 기존 메모 저장
2. 새 버전 설치
3. 동일 앨범 상세 진입
4. 기존 메모 표시
5. 새 JSON 저장 확인
6. 메모 수정
7. 메모 삭제
8. 재시작 후 삭제한 메모가 부활하지 않는지 확인

### 5단계: displayName 표시 helper

우선 상세 화면부터 적용한다.

```text
1. 상세 header/title
2. 상세 내부 제목
3. 최근 장소 Grid 카드
4. compact/row 카드
5. 메모 편집 화면의 자동 앨범명/표시 이름 구분
```

메모 편집 화면에서는 다음처럼 구분한다.

```text
표시 이름: 2026 삿포로 여행
자동 앨범명: 삿포로에서
```

### 6단계: displayName 편집 UI

최소 UI:

```text
표시 이름
[ 2026 삿포로 여행 ]

자동 앨범명: 삿포로에서
```

빈 값을 저장하면 자동 `albumName`으로 돌아간다.

### 7단계: displayName 검색 연동

```text
화면 진입 시:
  summaries 로드
  personalization overlay 한 번 로드
  displayName lookup 준비

입력 중:
  메모리에서 displayName + 기존 필드 검색
```

검색 helper가 JSON store를 직접 읽지 않도록 한다.

### 8단계: userCover 별도 패치

`userCoverUri` 필드는 모델/JSON에 미리 포함할 수 있다. 사진 선택 UI와 전체 화면 적용은 displayName/memo가 안정화된 뒤 별도 변경으로 진행한다.

## 테스트 케이스

### Key

- relativePath가 있으면 `path:` key
- 없으면 `album:` fallback
- 같은 path, 다른 albumName이어도 같은 key
- null summary 처리
- encode/decode 중복 없음

### Store

- 빈 store에서 기본값 반환
- 하나/여러 앨범 저장과 복원
- displayName만 저장해도 memo/cover 보존
- memo만 저장해도 displayName/cover 보존
- cover만 저장해도 나머지 보존
- schemaVersion
- 알 수 없는 필드 무시
- 손상 JSON에서 앱이 죽지 않음
- 임시 파일 실패 시 기존 파일 보존
- 빈 record 정리

### Legacy migration

- `album_memory_` 값 이전
- `album_alias_` fallback 이전
- 둘 다 있으면 `album_memory_` 우선
- relativePath 기준 legacy key 읽기
- relativePath 없을 때 albumName fallback
- 저장 실패 시 legacy key 유지
- 저장 성공 후 legacy 삭제 또는 marker 처리
- 새 store에서 메모 삭제 후 legacy 부활 없음
- legacy 메모가 displayName으로 잘못 승격되지 않음

### 표시

- displayName이 있으면 모든 memory UI에서 표시
- 없으면 albumName 표시
- Gallery 폴더명/relativePath 불변
- 자동 앨범명과 표시 이름이 편집 UI에서 구분
- 유효한 userCover가 thumbnail보다 우선
- 무효한 userCover가 thumbnail로 fallback
- userCover 설정이 사진 파일을 수정하지 않음

### 검색

- displayName 검색
- displayName과 albumName 동시 검색
- 검색 결과 순서 유지
- summary 식별 정보 보존
- 입력마다 store/MediaStore 재조회 없음
- displayName 삭제 후 albumName 검색 복귀
- memo 검색은 정책에 따라 제외 또는 별도 테스트

### 기기

- 기존 메모가 있는 설치본 업데이트
- 메모 저장/수정/삭제
- displayName이 상세/Grid/row에서 일관됨
- displayName을 비우면 자동 이름 복귀
- displayName 검색과 상세 이동
- 검색 후 뒤로 가기
- Gallery 폴더명 유지
- userCover 선택/무효 URI fallback
- Fold/회전
- 대용량 기록 입력 지연 없음

## 권장 최종 구조

```text
StoredAlbumSummary
  = 자동 발견/history 데이터

MemoryPersonalization
  = 사용자 개인화 overlay

MemoryPersonalizationKey
  = stable key 및 legacy key 정책

MemoryPersonalizationStore
  = JSON 저장/복원 및 기존 메모 migration

PersonalizedMemory 또는 MemoryPresentation
  = effectiveDisplayName/effectiveMemo/effectiveCoverUri

StoredAlbumSummarySearch
  = effective displayName과 자동 필드 검색

MainActivity
  = store 초기화, 목록 overlay 로드, 편집 위임, 렌더링
```

## 결론

이번 작업은 순수 리팩토링이 아니라 다음을 합친 작업이다.

```text
기존 메모 저장 책임을 MainActivity 밖으로 이동
  + 기존 메모 호환 및 migration
  + displayName 개인화 기능 추가
  + 향후 userCover를 위한 저장 기반 마련
```

가장 안전한 첫 구현 범위는 다음과 같다.

1. `MemoryPersonalization` 모델 추가
2. stable/legacy key 정책 분리
3. `MemoryPersonalizationStore` 추가
4. 기존 `albumMemory()`/`saveAlbumMemory()`를 store 위임으로 변경
5. lazy read-through로 기존 메모 보존
6. displayName 저장/표시 추가
7. displayName 검색 연동
8. userCover 선택 UI는 다음 패치로 분리

이 순서라면 Gallery 폴더명을 건드리지 않으면서 기존 메모를 보존하고, `MainActivity`에는 조정 코드만 남길 수 있다.
