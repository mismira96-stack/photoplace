# PhotoPlace 해외 기록 국가 Identity 구조 설계

작성일: 2026-08-10

## 목적

PhotoPlace에서 `KarlovyVary`, `Fatih` 같은 장소명은 최근 발견 장소에 표시되지만 해외 기록의 국가 그룹에는 들어가지 않는 문제를 코드 기준으로 분석한다.

목표는 도시별 alias를 계속 추가하는 것이 아니라, 국가를 안정적인 canonical identity로 저장하고 표시명·검색명·그룹핑을 분리하는 것이다.

핵심 방향:

```text
국가 식별: countryCode 또는 canonical country identity
국가 표시: countryCode -> 제품 표시명 mapping
국가 검색: 표시명 + legacy alias + countryCode
국가 그룹핑: countryCode
```

이번 문서는 설계 문서이며 코드 수정 내용은 포함하지 않는다.

---

## 1. 현재 국가명이 만들어지고 저장되는 흐름

현재 위치 정보 흐름은 다음과 같다.

```text
사진 URI
  -> EXIF / MediaStore / 동영상 메타데이터에서 좌표 추출
  -> Android Geocoder.getFromLocation()
  -> Address 정보 추출
      - countryName
      - adminArea
      - addressLine
  -> preferredLocationName()
  -> locationKey / 앨범명 생성
  -> PhotoItem
  -> AlbumSummary
  -> StoredAlbumSummary
  -> album_summary_history.json
```

국가 정보의 원천은 `MainActivity`의 `Geocoder` 처리다.

```java
new LocationLookupResult(
    strNormalizeLocationKey,
    address.getCountryName(),
    address.getAdminArea(),
    address.getAddressLine(0)
);
```

현재 `Address.getCountryName()`만 사용하고 `Address.getCountryCode()`는 사용하지 않는다.

국가 문자열은 다음 모델을 통과한다.

- `LocationLookupResult`
- `LocationResult`
- `PhotoItem`
- `AlbumSummary`
- `StoredAlbumSummary`
- `MemoryItem`

모든 계층에서 국가를 `countryName` 문자열 하나로만 전달한다.

정리 기록 JSON은 다음처럼 저장된다.

```json
{
  "countryName": "...",
  "adminArea": "...",
  "addressLine": "..."
}
```

`PhotoItem`의 sort input/output JSON도 `countryName`만 저장한다.

반면 앨범명은 별도 경로로 생성된다.

```text
Geocoder Address
  -> preferredLocationName()
  -> PlaceNamePolicy / knownTravelPlaceName()
  -> locationKey
  -> Pictures/{locationKey}에서/
```

따라서 `KarlovyVary`, `Fatih`가 최근 발견 장소에 보인다는 것은 장소명 생성에는 성공했다는 뜻이다. 국가 정보가 저장됐다는 뜻은 아니다.

---

## 2. 최근 발견 장소에는 나오는데 해외 기록에는 빠지는 이유

최근 발견 장소는 앨범명, 상대 경로, 실제 MediaStore 앨범 존재 여부를 중심으로 표시한다.

해외 기록은 `OverseasMemoryGrouper.overseasCountryName()`이 국가명을 찾지 못하면 해당 summary를 제외한다.

현재 국가 판별 순서는 다음과 같다.

```text
1. albumName + relativePath에 등록된 도시별 country hint
2. summary.countryName
3. addressLine + adminArea + albumName + relativePath의 국가 alias 검색
4. 등록된 도시별 country hint
5. 그래도 없으면 제외
```

예를 들어 다음 summary가 있다고 하자.

```text
albumName  = KarlovyVary에서
countryName = ""
adminArea   = ""
addressLine = ""
```

현재 `KarlovyVary`는 도시별 fallback 목록에 없으므로 해외 기록에서 빠진다.

`Fatih`도 같은 유형의 문제가 발생할 수 있다.

### Geocoder 반환값의 불안정성

- `countryName`은 locale에 따라 한국어·영어·다른 번역명으로 반환될 수 있다.
- `countryName`이 비어 있을 수 있다.
- `getCountryCode()`를 저장하지 않으므로 countryName이 비면 국가를 복원할 내부 identity가 없다.
- 여러 Geocoder 후보 중 첫 번째 usable location을 사용한다.
- 한국어 결과와 기본 locale 결과가 달라질 수 있지만 국가 identity는 문자열에 의존한다.

### 앨범 단위 metadata 집계의 한계

`AlbumSummary.includeLocationMetadata()`는 각 필드가 비어 있을 때만 값을 넣는다.

따라서 위치 메타가 불완전한 사진이 먼저 반영되거나, 국가 정보 없는 데이터를 기준으로 history가 생성되면 앨범 summary의 국가 정보가 비어 있을 수 있다.

### 기존 history에 복구 근거가 없음

현재 `StoredAlbumSummary`와 history JSON에는 다음이 없다.

- latitude
- longitude
- countryCode
- Geocoder 원본 후보
- 국가 판별 confidence

따라서 history에 국가명이 비어 있고 MediaStore 사진의 EXIF 좌표도 다시 읽을 수 없다면, `KarlovyVary`나 `Fatih`만 보고 안전하게 국가를 복구할 수 없다.

---

## 3. `countryName`만 사용하는 구조의 한계

### 표시 문자열과 식별자를 동시에 맡음

같은 국가가 다음처럼 저장될 수 있다.

```text
터키
튀르키예
튀르키에
Turkey
Türkiye
TR
```

문자열을 그룹 key로 사용하면 같은 국가가 여러 그룹으로 분리될 수 있다.

### locale과 Geocoder 데이터에 의존함

동일한 좌표도 다음처럼 반환될 수 있다.

```text
한국 locale -> 체코
영어 locale -> Czechia
기기 locale -> Czech Republic
```

앱 업데이트, 기기 제조사, Geocoder 데이터 변경으로 결과가 달라질 수 있다.

### alias 테이블이 계속 커짐

현재 `OverseasMemoryGrouper`에는 국가 alias와 도시별 국가 hint가 함께 있다.

도시 alias를 계속 추가하면 다음 문제가 생긴다.

- 새 도시가 발생할 때마다 patch 필요
- 동명이인 도시를 구분하지 못함
- 국가 판별과 장소명 정책이 결합됨
- 새로운 국가 추가 시 동작을 예측하기 어려움
- 검색과 history 재생성에서 재사용할 안정적인 identity가 없음

### 검색도 저장 문자열에 종속됨

현재 검색은 `countryName` 문자열을 그대로 비교한다.

저장값이 `Türkiye`이면 사용자가 `turkey`를 검색해도 매칭되지 않을 수 있다.

### 표시명 정책 변경이 어려움

제품 표시 정책을 `터키 -> 튀르키예`로 바꾸려면 저장된 문자열을 전체 변환해야 한다. 국가 identity가 분리되어 있으면 표시 mapping만 바꾸면 된다.

---

## 4. `countryCode` 도입 최소 설계

가장 작은 변경은 복잡한 국가 도메인 모델을 처음부터 도입하는 것이 아니라 기존 구조에 `countryCode`를 병렬 추가하는 방식이다.

### canonical identity

ISO 3166-1 alpha-2를 기본 내부 key로 사용한다.

```text
JP = 일본
AU = 호주
GU = 괌
CZ = 체코
TR = 튀르키예
US = 미국
KR = 대한민국
```

`countryCode`는 내부 식별자이고, `countryName`은 표시용 또는 legacy 호환 필드로 취급한다.

### 모델 변경 범위

다음 모델에 `countryCode`를 추가한다.

```text
LocationLookupResult
LocationResult
PhotoItem
AlbumSummary
StoredAlbumSummary
MemoryItem
```

기존 생성자를 사용하는 코드가 많으므로 기존 생성자는 유지하고 새 생성자를 추가하는 방식이 안전하다.

### 국가 정규화 계층

작은 단일 책임의 정규화 계층을 둔다.

```text
CountryIdentityNormalizer
```

책임:

```text
fromGeocoder(countryCode, countryName)
  -> canonical country

fromLegacyCountryName(countryName)
  -> canonical country 또는 unknown

displayName(countryCode)
  -> 현재 제품 표시명

searchAliases(countryCode)
  -> 표시명·영문명·legacy alias 목록
```

초기 반환 모델은 다음 정도면 충분하다.

```text
countryCode: "TR"
displayName: "튀르키예"
```

### 국가 판별 우선순위

```text
1. Geocoder countryCode
2. legacy countryName alias
3. 명확한 주소 국가 alias
4. unknown
```

도시명은 기본 국가 판별 근거로 사용하지 않는다.

`KarlovyVary -> CZ`, `Fatih -> TR` 같은 직접 매핑은 migration 보조 외에는 새로 추가하지 않는다.

### 표시명 결정

화면에서는 저장된 `countryName`을 직접 출력하지 않고 canonical mapping을 통해 표시한다.

```text
TR -> 튀르키예
CZ -> 체코
JP -> 일본
AU -> 호주
GU -> 괌
```

이렇게 하면 내부 그룹 key와 표시 언어를 독립적으로 변경할 수 있다.

---

## 5. 기존 JSON 호환 및 migration 전략

기존 필드는 유지하고 `countryCode`만 추가한다.

```json
{
  "albumName": "KarlovyVary에서",
  "relativePath": "Pictures/KarlovyVary에서/",
  "countryCode": "CZ",
  "countryName": "체코",
  "adminArea": "...",
  "addressLine": "..."
}
```

### 읽기 전략

```text
countryCode = json.optString("countryCode", "")
countryName = json.optString("countryName", "")

countryCode가 비어 있으면:
  countryName alias로 countryCode 추론

countryCode가 있으면:
  표시명은 canonical mapping으로 결정
```

기존 JSON의 다음 값은 모두 동일한 내부 국가로 읽는다.

```json
{ "countryName": "Turkey" }
{ "countryName": "Türkiye" }
{ "countryName": "터키" }
{ "countryName": "튀르키예" }
```

결과:

```text
countryCode = TR
displayName = 튀르키예
```

### 기존 앨범 metadata backfill

기존 국가 정보가 비어 있으면 다음 순서로 보충한다.

```text
1. 기존 countryName alias 정규화
2. MediaStore 앨범의 대표 사진에서 좌표 재조회
3. Geocoder countryCode 재조회
4. 성공하면 countryCode 저장
5. 실패하면 unknown 유지
```

현재 기존 앨범 backfill이 대표 미디어의 EXIF 좌표를 재조회하는 경로를 갖고 있으므로, 여기에 `countryCode`를 추가하는 것이 최소 변경이다.

### 저장 시점

앱 시작마다 전체 history를 재작성하지 않는다.

권장 방식:

```text
앱 실행 시에는 memory상 legacy fallback
명시적인 기록 새로 고침/재생성 또는 backfill 때 저장
```

또는 `countryCode`가 새로 채워진 summary만 dirty 처리하고 모든 처리가 끝난 뒤 atomic write한다.

### migration 실패

좌표도 없고 국가명도 불명확한 기록은 임의로 보정하지 않는다.

```text
countryCode = ""
countryName = ""
```

상태를 유지하거나, 향후 UI에서 `국가 미확인`으로 별도 표시한다.

---

## 6. `OverseasMemoryGrouper` 변경 방향

`OverseasMemoryGrouper`는 국가를 추측하는 역할보다 canonical country를 그룹핑하는 역할에 집중해야 한다.

### 권장 grouping key

```text
summary.countryCode
```

예:

```text
JP -> 일본 그룹
CZ -> 체코 그룹
TR -> 튀르키예 그룹
```

### legacy fallback

migration 기간에는 다음 fallback을 허용한다.

```text
if countryCode가 비어 있음:
    legacy countryName alias로 countryCode 추론
```

기존 도시 hint는 구 JSON을 읽는 한시적 fallback으로만 유지한다.

```text
countryCode 있음
  -> code grouping

countryCode 없음
  -> legacy countryName normalization

그래도 없음
  -> 기존 fallback 또는 unknown 정책
```

새 도시 alias는 추가하지 않는다.

### 한국 판단

현재 문자열 기반 `KOREA_HINTS`보다 다음 우선순위를 사용한다.

```text
countryCode == "KR" -> 국내
countryCode != "KR" -> 해외 후보
countryCode == "" -> legacy fallback 또는 unknown
```

`GU`는 제품 정책상 `US`와 별도 표시하는 현재 동작을 유지할 수 있다.

---

## 7. 검색 설계

검색은 canonical identity와 표시 alias를 모두 대상으로 한다.

### 국가별 검색 alias 예시

```text
JP:
  일본
  japan
  日本
  JP
```

```text
TR:
  튀르키예
  튀르키에
  터키
  turkey
  türkiye
  TR
```

```text
CZ:
  체코
  czech
  czechia
  czech republic
  CZ
```

### 검색 대상

각 summary의 검색 대상은 다음과 같다.

```text
albumName
countryCode
canonical displayName
country aliases
adminArea
addressLine
relativePath
startDate
endDate
```

country alias 목록은 `MainActivity`에 직접 넣지 않고 `CountryIdentityNormalizer`가 제공한다.

### 검색과 해외 그룹의 일관성

두 기능 모두 같은 `countryCode`를 사용해야 한다.

```text
OverseasMemoryGrouper:
  countryCode = TR -> 튀르키예 그룹

StoredAlbumSummarySearch:
  turkey / Türkiye / 튀르키예 / 터키 -> TR summary
```

이렇게 해야 검색 결과와 화면 그룹이 서로 다른 국가 기준을 사용하지 않는다.

---

## 8. 작은 patch 순서

### Patch 1: 국가 정규화 계층 추가

`CountryIdentityNormalizer`를 추가한다.

- ISO alpha-2 code 정규화
- legacy countryName alias 변환
- canonical displayName 반환
- 검색 alias 반환

이 단계에서는 기존 동작을 유지하고 단위 테스트를 먼저 추가한다.

### Patch 2: Geocoder 결과에 countryCode 추가

`Address.getCountryCode()`와 `Address.getCountryName()`을 함께 읽는다.

`LocationLookupResult`와 `LocationResult`에 전달한다.

### Patch 3: PhotoItem JSON 확장

`PhotoItem`과 `PhotoItemJson`에 `countryCode`를 추가한다.

기존 JSON에 필드가 없으면 빈 문자열로 읽는다.

### Patch 4: AlbumSummary/history 확장

다음에 `countryCode`를 추가한다.

```text
AlbumSummary
StoredAlbumSummary
AlbumSummaryHistoryStore
```

기존 `countryName`은 호환성을 위해 유지한다.

### Patch 5: 앨범 metadata 집계 보강

국가 code는 countryName과 독립적으로 채워져야 한다.

```text
countryCode가 비어 있을 때만 채움
countryName은 code에서 canonical displayName으로 파생
adminArea/addressLine은 기존 first-non-empty 정책 유지
```

### Patch 6: OverseasMemoryGrouper 전환

```text
countryCode
-> legacy countryName normalization
-> 기존 fallback
```

그룹 key는 countryCode, 표시명은 canonical displayName으로 변경한다.

### Patch 7: 검색 alias 적용

`StoredAlbumSummarySearch`가 canonical country aliases를 검색하도록 변경한다.

### Patch 8: 기존 history backfill

초기에는 전체 자동 재작성보다 기존 rebuild/backfill 경로에 연결한다.

대상:

- 기존 album summary
- EXIF 좌표가 남아 있는 실제 미디어
- countryCode가 비어 있는 summary

---

## 9. 반드시 추가할 테스트 케이스

### 국가 정규화

```text
JP -> JP / 일본
Japan -> JP / 일본
日本 -> JP / 일본
```

```text
TR -> TR / 튀르키예
Turkey -> TR / 튀르키예
Türkiye -> TR / 튀르키예
터키 -> TR / 튀르키예
튀르키에 -> TR / 튀르키예
튀르키예 -> TR / 튀르키예
```

```text
Czechia -> CZ / 체코
Czech Republic -> CZ / 체코
체코 -> CZ / 체코
```

### Geocoder 우선순위

```text
countryCode = TR
countryName = Turkey
-> TR / 튀르키예
```

```text
countryCode = ""
countryName = Türkiye
-> TR / 튀르키예
```

```text
countryCode = ""
countryName = ""
albumName = Fatih에서
-> 도시명만으로 국가를 추측하지 않음
```

### OverseasMemoryGrouper

- CZ summary 여러 개가 체코 그룹 하나로 합쳐지는지 확인
- TR summary 여러 개가 튀르키예 그룹 하나로 합쳐지는지 확인
- JP와 legacy Japan summary가 일본 그룹 하나로 합쳐지는지 확인
- `countryCode = TR`, `countryName = Turkey`가 튀르키예로 표시되는지 확인
- `countryCode = KR`이 해외 그룹에서 제외되는지 확인
- 국가 code와 국가명이 모두 없는 `KarlovyVary`가 도시 hint 없이 그룹에 들어가지 않는지 확인

### JSON 호환

기존 JSON에 `countryCode`가 없어도 예외 없이 읽혀야 한다.

```json
{
  "countryName": "Turkey"
}
```

결과:

```text
countryCode = TR
displayName = 튀르키예
```

새 JSON의 `countryCode`가 있으면 국가명보다 code를 우선해야 한다.

### 검색

```text
일본 / japan / JP -> JP summary
터키 / 튀르키예 / Türkiye / turkey / TR -> TR summary
체코 / czech / czechia / CZ -> CZ summary
```

### AlbumSummary aggregation

다음 경우 국가 code가 정상적으로 보충되어야 한다.

```text
첫 번째 item: countryCode = ""
두 번째 item: countryCode = CZ
최종 summary: countryCode = CZ
```

서로 다른 countryCode가 같은 앨범 summary에 들어오는 비정상 상황도 진단해야 한다.

---

## 10. 구현하면 안 되는 위험한 shortcut

### 도시별 국가 patch를 계속 추가하지 않기

```text
KarlovyVary -> 체코
Fatih -> 튀르키예
```

이 방식은 새 도시가 발생할 때마다 계속 유지보수가 필요하고, 국가 identity 자체를 저장하지 못한다.

### countryName 문자열만 강제 치환하지 않기

`Turkey`를 `튀르키예`로 바꾸는 것만으로는 내부 key 문제가 해결되지 않는다.

### 앨범명에 국가명을 붙이지 않기

```text
KarlovyVary에서 -> 체코 KarlovyVary에서
```

앨범명은 장소 표시용이며 국가 identity 저장소가 아니다. 폴더와 기존 history에 불필요한 영향을 준다.

### unknown 도시를 억측하지 않기

좌표와 `countryCode`가 없으면 unknown으로 남겨야 한다. 문자열만으로 모든 도시의 국가를 자동 추정하면 오분류 위험이 높다.

### 앱 시작마다 history 전체 재작성하지 않기

Geocoder는 느리고 실패할 수 있다. 앱 시작 지연과 잘못된 결과의 영구 저장을 피하기 위해 명시적 rebuild/backfill 또는 atomic migration을 사용한다.

### countryName을 즉시 삭제하지 않기

기존 JSON 호환을 위해 당분간 병렬 유지한다.

```text
countryCode: 내부 canonical identity
countryName: legacy 호환 및 cache
```

새 코드가 표시를 결정할 때는 `countryCode`를 우선한다.

### 장소명 정책과 국가 정책을 합치지 않기

`PlaceNamePolicy`는 장소명 생성 정책이고, 국가 identity는 그룹핑·검색·정규화용이다.

```text
장소명: 사용자에게 보이는 album/place name
국가 identity: canonical country key
```

두 책임은 별도 계층으로 유지한다.

---

## 결론

현재 문제의 직접 원인은 `countryName`이 비어 있거나 지역화된 문자열로 저장되고, `OverseasMemoryGrouper`가 country code 없이 문자열과 일부 도시 alias만으로 국가를 추정하는 구조다.

권장 구조는 다음과 같다.

```text
Geocoder.Address
  -> countryCode 우선
  -> CountryIdentityNormalizer
  -> canonical countryCode
  -> canonical displayName
  -> PhotoItem / AlbumSummary / StoredAlbumSummary에 저장
  -> 해외 그룹은 countryCode로 grouping
  -> 검색은 displayName + legacy aliases + countryCode 사용
```

가장 작은 구현 순서는 다음 세 단계다.

```text
1. countryCode와 CountryIdentityNormalizer 추가
2. JSON 및 모델에 countryCode 병렬 저장
3. OverseasMemoryGrouper와 검색을 countryCode 기준으로 전환
```

기존 history에 국가 정보가 전혀 없고 좌표도 없으면 안전한 자동 복구는 불가능하다. 실제 MediaStore 좌표를 재조회할 수 있는 경우에만 backfill하고, 그 외에는 도시명 patch로 억지 보정하지 않는 것이 적절하다.
