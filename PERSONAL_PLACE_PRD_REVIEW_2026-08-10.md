# PhotoPlace V2 Personal Place PRD 리뷰 결과

## 리뷰 대상

- 문서: `Photoplace_V2_Personal_Place_PRD.md`
- 기능명: 나만의 장소
- 내부 작업명: Personal Place
- 리뷰 기준:
  - 현재 `DiscoverySnapshot` / `MemoryRecord` 설계
  - 기존 Gallery Organizer 구조
  - `AlbumSummaryHistoryStore`
  - `SortJob` / `SortWorker` / `SortInputStore`
  - 기존 장소명 및 country identity 구조

## 최종 판정

```text
PRD status: Strong direction, needs data-contract decisions before implementation
```

제품 방향은 명확하다. 특히 다음 원칙은 현재 Display First / Organize Optional 설계와 잘 맞는다.

```text
후보 발견
  -> 사진 확인
  -> 사용자가 같은 장소인지 확인
  -> 사용자가 장소명 입력
  -> PhotoPlace 내부 장소로 저장
  -> 필요할 때만 Gallery 정리
```

다만 구현 전에 다음 네 가지 계약을 확정해야 한다.

```text
1. PersonalPlace와 MemoryRecord의 관계
2. 기존 사진 membership과 미래 GPS rule의 분리
3. 후보 dismiss identity
4. 이미 정리된 사진 재이동과 Undo의 범위
```

---

## 주요 Finding

### 1. High: 개인 장소의 저장과 향후 사진 자동 적용 계약이 불명확함

PRD는 개인 장소 저장 후 다음 사진에도 적용되는 동작을 요구한다.

```text
장소 저장 즉시:
- 기존에 일치하는 사진에 적용
- 이후 새로 발견되는 사진에도 적용
- 장소 카드 및 검색에 적용
```

동시에 데이터 모델은 다음을 분리한다.

```text
PersonalPlace
  - 중심 좌표
  - 반경
  - 사용자 지정 이름

PhotoPlaceMembership
  - 사진과 개인 장소의 연결
```

문제는 기존 사용자가 확인한 사진과 미래 GPS 매칭 사진의 신뢰 수준이 다르다는 점이다.

GPS는 다음 장소를 정확히 구분하지 못할 수 있다.

- 같은 건물 안의 카페·식당·학원
- 인접한 건물
- 도로·주차장·상가 밀집 지역
- GPS 오차가 큰 사진

기존 사진 membership을 단순히 중심 좌표와 반경으로 다시 계산하면, 다음 분석에서 다른 사진이 개인 장소에 잘못 포함될 수 있다.

### 권장 데이터 책임

```text
PersonalPlace:
  앞으로 적용할 공간 규칙과 사용자 이름

PhotoPlaceMembership:
  사용자가 확인한 기존 사진의 확정 소속

DiscoverySnapshot:
  특정 분석 시점의 발견 결과
```

새 사진은 처음부터 확정 membership으로 저장하기보다 다음 상태를 거치는 것이 안전하다.

```text
새 GPS 매칭
  -> 개인 장소 후보 또는 provisional match
  -> 사용자가 확인
  -> 확정 membership
```

초기 MVP에서는 “앞으로 모든 사진에 자동 적용”을 강하게 약속하지 않는 편이 안전하다.

```text
다음 분석에서 같은 장소 후보로 우선 표시
```

정도로 표현하면 GPS 오차와 후보 규칙의 한계를 정직하게 반영할 수 있다.

---

### 2. High: 이미 정리된 사진 재이동과 Undo는 현재 Organizer 계약보다 범위가 큼

PRD는 이미 `송파구` 앨범으로 이동된 사진을 개인 장소 앨범으로 다시 옮기는 시나리오를 P1으로 정의한다.

```text
송파구 앨범의 사진 186장을
라비에벨 발레 앨범으로 옮길까요?
```

현재 Gallery 정리 파이프라인은 분석 시점에 계산된 `PhotoItem.targetRelativePath`를 사용해 `SortJob`이 복사·이동하는 구조다.

관련 파일:

- `app/src/main/java/com/example/gallerysorter/PhotoItem.java`
- `app/src/main/java/com/example/gallerysorter/SortInputStore.java`
- `app/src/main/java/com/example/gallerysorter/SortJob.java`
- `BACKGROUND_PROCESSING_PLAN.md`

재이동과 Undo를 제공하려면 PRD에 다음 정보가 추가로 필요하다.

```text
- 이동 전 source relativePath
- 이동 전 URI
- 이동 후 URI
- 사진별 undo source
- 이미 이동된 사진과 원본 사진의 중복 판정
- 동영상 이동의 undo
- 부분 성공 시 복구 대상
- 원본이 외부에서 삭제된 경우의 처리
```

따라서 `Undo 지원`은 단순한 버튼이 아니라 별도 이동 transaction/history 계약이 필요하다.

### 권장 범위 조정

Personal Place P0:

```text
개인 장소명 저장
  -> PhotoPlace 내부 표시/검색 우선순위 변경
```

P1:

```text
이미 정리된 사진 재이동
Gallery 앨범 병합
부분 성공 복구
Undo
```

---

### 3. High: Candidate Quality Validation 결과를 저장할 모델과 재현 계약이 없음

PRD는 추천 UI 전에 Candidate Quality Validation을 요구한다.

```text
상위 20개 후보 출력
사진/날짜/좌표 분포 확인
사람이 의미 있음/애매함/오탐 판정
```

하지만 후보 품질 검증 결과를 어떻게 저장하고 재현할지 충분히 정의되어 있지 않다.

후보 생성 알고리즘이 바뀌면 같은 사진에서도 다음 결과가 달라질 수 있다.

- 후보 중심 좌표
- 포함 사진 수
- 반경
- score
- 날짜 범위

따라서 후보 결과에는 최소한 다음 정보가 필요하다.

```text
candidateId
sourceSnapshotVersion
candidatePolicyVersion
candidateSignature
score
cluster member identity
photoCount
distinctDateCount
radius
reviewState
reviewReason
```

### 권장 모델

```text
PlaceCandidate
  - candidateId
  - sourceSnapshotVersion
  - candidatePolicyVersion
  - candidateSignature
  - state: NEW / VIEWED / ACCEPTED / DISMISSED
  - dismissalReason
```

Candidate Quality Validation이 내부 도구 단계라면 사용자 저장소가 아니어도 된다. 하지만 최소한 다음 중 하나는 필요하다.

```text
- debug report 파일
- 테스트 fixture
- versioned candidate result JSON
```

---

### 4. Medium: 추천 숨김 조건이 동일 후보를 판별할 수 없음

PRD는 사용자가 숨긴 후보를 같은 조건으로 다시 제안하지 않도록 요구한다.

```text
숨긴 후보는 같은 조건으로 다시 제안하지 않는다.
```

하지만 후보는 분석을 다시 실행할 때 조금씩 변할 수 있다.

- 중심 좌표가 달라짐
- 포함 사진이 달라짐
- snapshot version이 달라짐
- score가 달라짐
- 날짜 범위가 달라짐

단순한 장소명이나 좌표 하나만 비교하면 dismiss가 무력화될 수 있다.

### 권장 후보 identity

MVP에서는 다음과 같은 signature를 검토할 수 있다.

```text
candidateSignature
  = policyVersion
  + sorted photo identity set
```

또는:

```text
stableCandidateKey
  = normalized placeKey
  + coarse date bucket
  + cluster signature
```

전체 photo set hash는 단순하지만 snapshot이 갱신될 때 사진 한두 장의 변화로 hash가 달라질 수 있다. 따라서 다음 단계에서는 근사 matching 정책도 필요하다.

---

### 5. Medium: `PersonalPlace`와 현재 `MemoryRecord`의 관계가 명확하지 않음

현재 V2 설계는 다음 흐름을 기준으로 한다.

```text
DiscoverySnapshot
  -> MemoryRecord
  -> DISCOVERED_ONLY / ORGANIZED_ALBUM / MIXED
```

Personal Place PRD는 별도로 다음 모델을 제안한다.

```text
PersonalPlace
PlaceCandidate
PhotoPlaceMembership
```

하지만 개인 장소 저장 후 어떤 모델 변화가 일어나는지 명확하지 않다.

가능한 방향:

```text
1. MemoryRecord의 displayName overlay
2. MemoryRecord의 placeKey를 개인 장소 key로 대체
3. 새로운 MemoryRecord 생성
4. 기존 DiscoveryMemoryGroup을 분할
5. 행정구역 MemoryRecord의 child memory
```

PRD의 장소 우선순위는 다음과 같다.

```text
1. 사용자 지정 개인 장소
2. 확실한 대형 POI
3. 기본 행정구역
```

따라서 단순히 표시 문자열만 바꾸는 overlay로는 부족할 수 있다. Gallery 정리 시 실제 grouping 기준도 개인 장소에 의해 바뀌어야 하기 때문이다.

### 권장 MVP 정의

첫 단계에서는 개인 장소를 기존 Memory를 대체하는 새로운 장소 identity로 만들지 않는다.

```text
PersonalPlace
  = MemoryRecord에 붙는 사용자 확인 overlay

PhotoPlaceMembership
  = 특정 photoRef가 개인 장소에 속하는지 기록

기존 placeKey/locationKey
  = 원래 분석 결과로 유지
```

이렇게 하면 기존 DiscoverySnapshot과 Gallery Organizer를 깨뜨리지 않고 개인 장소 우선 표시를 추가할 수 있다.

---

### 6. Medium: 개인 장소 이름 충돌 정책이 결정되지 않음

PRD는 기존 장소명 및 Gallery 앨범명과 충돌하면 저장 전에 알리도록 한다.

하지만 다음 상황의 처리 방식이 없다.

```text
이미 "회사"라는 PersonalPlace가 있음
새 후보도 "회사"로 입력

기존 Gallery에 "회사" 앨범이 있음
개인 장소 이름도 "회사"

서로 다른 위치에 같은 이름의 개인 장소가 있음
```

필요한 정책:

```text
- 같은 이름 허용 여부
- 같은 이름 + 다른 placeKey 허용 여부
- Gallery 앨범과 합칠지
- 새 앨범명 suffix를 붙일지
- PhotoPlace 내부 이름과 Gallery 폴더 이름을 분리할지
```

### 권장 MVP

```text
PhotoPlace 내부에서는 같은 displayName을 허용
Gallery 생성 시에만 충돌을 확인
기존 Gallery 앨범을 자동으로 합치지 않음
사용자에게 대상 앨범을 명시적으로 선택하게 함
```

---

### 7. Medium: 민감 장소 개인정보 정책과 이벤트 설계가 더 구체적이어야 함

PRD는 개인 장소명 원문을 analytics에 수집하지 않는다고 명시한다. 방향은 적절하다.

다만 다음 데이터도 민감할 수 있다.

```text
candidate centerLat / centerLng
photo membership
집·회사로 반복되는 위치 패턴
candidateSignature
```

로컬 JSON에 저장하더라도 Android backup이나 기기 데이터 이전 경로를 고려해야 한다.

### 추가 결정 필요

```text
- snapshot과 PersonalPlace JSON에 좌표를 그대로 저장할지
- 사용자 설정에서 개인 장소 데이터 export/delete를 지원할지
- 앱 삭제/데이터 초기화 시 어떤 데이터가 제거되는지
- analytics에 좌표나 후보 signature를 절대 보내지 않는지
```

PRD의 “기기 내 처리” 원칙을 저장·백업·삭제 정책까지 확장해야 한다.

---

### 8. Medium: 후보 생성 threshold와 증분 갱신 전략이 없음

PRD는 다음 값을 확정 스펙이 아닌 실험 파라미터로 취급한다.

```text
20장
3일
2주
100m
```

이 판단은 적절하다. 다만 대량 사진에서 후보 생성 비용과 갱신 조건이 충분히 정의되어 있지 않다.

현재 코드에는 다음 흐름이 이미 있다.

```text
MediaStore scan
  -> EXIF/GPS 분석
  -> Geocoder 조회
  -> PhotoItem 생성
  -> SortWorker/SortJob
```

Personal Place 후보 생성이 이 흐름을 다시 실행하면 불필요한 비용이 발생한다.

### 권장 처리

```text
DiscoverySnapshot
  -> 이미 저장된 좌표/날짜/photoRef 사용
  -> 후보 생성
```

후보 생성 단계에서 다음을 다시 실행하지 않는다.

```text
- MediaStore 전체 scan
- EXIF 전체 재조회
- Geocoder 전체 재조회
```

source signature 또는 snapshot version이 바뀐 경우에만 후보 결과를 재계산하는 방향이 적절하다.

---

## 정상적으로 잘 잡힌 부분

### 1. Candidate Quality Validation Gate

추천 UI보다 먼저 실제 후보 품질을 검증하도록 한 것은 매우 적절하다.

특히 다음 판단이 좋다.

```text
threshold는 제품 확정값이 아니라 실험 파라미터
```

### 2. 사용자 확인을 자동 분류보다 우선함

다음 흐름은 신뢰성과 개인정보 측면에서 적절하다.

```text
후보 발견
  -> 사진 확인
  -> 같은 장소인지 사용자 확인
  -> 사용자 이름 입력
  -> 내부 장소 저장
```

### 3. 장소 저장과 Gallery 이동을 분리함

```text
장소명 저장
  !=
파일 이동
```

이는 현재 V2 `DiscoverySnapshot` / `MemoryRecord` 방향과 잘 맞는다.

### 4. 기존 POI 정책을 무리하게 확장하지 않음

모든 상점·건물을 자동으로 분류하지 않고, 사용자 확인 기반의 개인 장소로 제한한 점은 현재 `PlaceNamePolicy`의 보수적 방향과 일치한다.

관련 파일:

```text
app/src/main/java/com/example/gallerysorter/PlaceNamePolicy.java
```

### 5. 기존 V1 Organizer 유지

Personal Place가 Gallery 기능을 대체하지 않고, 나중에 선택 action으로 연결되도록 한 점은 안전하다.

---

## 현재 V2 설계와의 권장 정합성

현재까지의 안전한 순서는 다음과 같다.

```text
Phase 1
  DiscoverySnapshotStore
  DiscoverySnapshotMapper

Phase 2
  MemoryRecord adapter
  Discovery-only 사진 보기
  기존 organized history와 merge

Phase 3
  Candidate generator
  Candidate Quality Validation
  상위 후보 내부 report

Phase 4
  추천 UI
  후보 사진 확인
  사용자 장소명 저장

Phase 5
  PersonalPlace membership 적용
  MemoryRecord 표시/검색 우선순위 적용

Phase 6
  사용자가 선택한 경우 Gallery 정리
  기존 앨범 사진 재이동
  Undo
```

특히 다음 순서를 지켜야 한다.

```text
DiscoverySnapshot을 먼저 안정화
  -> 후보 품질 검증
  -> 개인 장소 저장
  -> Gallery 이동
```

---

## 권장 TODO

```text
- [ ] DiscoverySnapshotStore 구현
- [ ] DiscoverySnapshotMapper 구현
- [ ] MemoryRecord adapter 구현
- [ ] Discovery-only 사진 보기
- [ ] 기존 organized album과 중복 merge
- [ ] Candidate generator 설계
- [ ] candidateSignature / dismiss identity 정의
- [ ] Candidate Quality Validation report 포맷 정의
- [ ] 후보 생성 시 DiscoverySnapshot 재사용
- [ ] PersonalPlace / PlaceCandidate / PhotoPlaceMembership 모델 분리
- [ ] 개인 장소와 MemoryRecord의 관계 확정
- [ ] 개인 장소명 충돌 정책 확정
- [ ] 기존 사진 membership과 미래 GPS rule 분리
- [ ] 장소 저장과 Gallery 이동 분리
- [ ] 기존 앨범 재이동/Undo를 P1으로 분리
- [ ] 개인 장소 좌표·membership의 local storage/backup/delete 정책 정의
- [ ] threshold 및 후보 품질 benchmark
```

## 최종 판정

```text
PRD status: Strong direction, needs data-contract decisions before implementation
```

제품 방향은 명확하고 현재 V2 설계와도 잘 맞는다.

다만 구현 전에 다음 네 가지를 확정해야 한다.

```text
1. PersonalPlace와 MemoryRecord의 관계
2. 기존 사진 membership과 미래 GPS rule의 분리
3. 후보 dismiss identity
4. 이미 정리된 사진 재이동과 Undo의 범위
```

이 네 가지를 정하면 `DiscoverySnapshotStore` 이후 Personal Place를 안전하게 얹을 수 있다.
