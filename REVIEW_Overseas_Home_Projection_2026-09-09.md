# PhotoPlace 해외 기록 및 홈 진입 성능 종합 리뷰

작성일: 2026-09-09  
리뷰 대상: `codex/photoplace-v2-bg-wip`  
현재 상태: 개발 체크포인트, Play 릴리즈 보류

## 1. 대상 변경

- `574fe02` 홈 해외 Memory projection 연결
- `3bff7c6` 홈 projection/live-filter 백그라운드 이동
- `27d19ec` 홈 projection 전용 executor 분리
- `5877a70` 국가 카드에서 discovery/organized 목록 함께 열기
- `830edfa` organized Memory 국가 식별 보강
- `67f7c6b` 해외 카드 source summary 2줄 표시
- `644ebdf` 홈 summary 수집 중 EXIF/동기 Geocoder 차단
- `61fb103`, `1a59115` 해외 카드 높이 및 두 줄 텍스트 정렬 보정

## 2. 현재 제품 흐름

```text
홈
 -> 해외 기록 국가 카드
 -> 국가명 검색이 적용된 Memory 목록
 -> discovery Memory + 기존 위치 앨범 함께 표시
```

전체 `발견 기록` 탭은 여전히 discovery-only다. 국가 카드에서 진입할 때만
`MemoryRepository.memories()`를 사용해 organized source를 함께 보여준다.

홈 카드 mixed-source 표기:

```text
일본
발견 8곳
위치 앨범 6개
```

카드 설명 영역은 고정 높이를 사용해 국가 카드 간 하단 라인을 맞추며, 두 줄 텍스트가 잘리지 않도록 여유 높이를 둔다.

## 3. 홈 진입 지연 원인과 수정

### 확인된 원인

1. 홈 projection이 기존 단일 `worker` 큐에 들어가 backfill/분석 작업 뒤에서 대기할 수 있었다.
2. worker 전환 후 `collectExistingAlbumSummaries()`의 백그라운드 조건이 EXIF 파싱과 동기 Geocoder 보강을 활성화했다.
3. `onResume()` 첫 호출에서 album summary cache를 무조건 무효화했다.
4. Discovery snapshot live-filter는 수천 개 MediaStore ID를 청크 조회한다.

### 적용한 수정

- 홈 projection 전용 `homeProjectionWorker`를 추가해 기존 분석/backfill worker와 분리했다.
- 홈 album summary 수집에서 EXIF/Geocoder 위치 보강을 제거했다.
  - 저장된 메타데이터와 앨범명/경로 휴리스틱은 유지한다.
  - 무거운 위치 보강은 홈 진입 필수 경로가 아니다.
- 첫 `onResume()`에서는 캐시를 무효화하지 않는다.
- 이후 resume에서는 외부 Gallery 변경 반영을 위해 기존 cache invalidation을 유지한다.

### 검증

- `testDebugUnitTest`: 통과
- `assembleDebug`: 통과
- 연결 단말 APK 설치: 성공
- Activity cold start 측정: 약 `0.2~0.5초`
- 앱 시작 크래시/ANR 로그: 재현되지 않음

## 4. 데이터 안정성

- Gallery 파일/MediaStore row 이동 또는 삭제 없음
- Discovery snapshot 변경 없음
- 날짜별 메모 변경 없음
- 기존 위치 앨범 source는 국가 검색 목록에서만 추가 노출
- 기존 전체 발견 탭의 전역 앨범 생성 CTA 범위는 유지
- organized album의 country metadata가 비어도 기존 `OverseasMemoryGrouper` 경로 추론으로 검색 가능

## 5. 현재 의도된 제한

- 홈 일본 카드가 아직 기존 `showOverseasMemoryScreen()` 전용 화면으로 가지 않고 국가 검색 목록으로 진입한다.
- 따라서 화면 맥락은 임시 UX다.
- stable Memory ID와 Gallery organization link 기반 완전 dedupe는 아직 하지 않는다.
- 국가별 `날짜 -> 장소 -> 메모 -> 사진` 통합 상세 화면은 아직 없다.
- cold start fast-path cache/placeholder는 아직 적용하지 않았다.

## 6. Antigravity 리뷰 요청

### Blocker 확인

- 홈 projection 전용 executor 분리가 lifecycle/스레드 안전성 문제를 만들지 않는지
- 홈 summary 수집에서 EXIF/Geocoder를 건너뛰어도 해외 국가 projection과 위치 앨범 목록이 정확한지
- 첫 `onResume()` cache 보류가 외부 Gallery 변경 반영을 놓치지 않는지
- 국가 카드의 discovery/organized 목록 병합이 중복 또는 누락을 만들지 않는지
- 고정 카드 높이에서 작은 화면/긴 국가명/두 줄 source summary가 잘리지 않는지

### 성능 확인

- 최초 화면 첫 프레임 표시 시간
- worker projection 완료 시간
- 썸네일 바인딩 완료 시간
- 앱 재진입 시 체감 지연
- 대형 discovery snapshot에서 ANR/OOM 여부

## 7. 다음 작업

1. `Phase 3-B`: 기존 해외 상세 화면을 국가 통합 상세 화면으로 확장
2. `새로 발견한 장소`와 `정리된 위치 앨범`을 별도 섹션으로 표시
3. 각 항목에서 기존 Memory/앨범 상세로 진입
4. stable Memory ID와 organization link를 연결해 dedupe 및 메모 보존 검증
5. 이후에만 Play 릴리즈 여부 재검토

## 최종 판단

현재 변경은 기능 검증용 개발 체크포인트로는 안정적이다. 다만 홈 국가 카드가 검색 화면으로 우회하는 interim UX이므로,
Antigravity 리뷰와 Phase 3-B 전용 상세 화면 구현 전에는 Play 릴리즈에 포함하지 않는다.

## 8. 보완 리뷰 반영

Antigravity 리뷰에서 확인된 `homeProjectionWorker` lifecycle 누수를 보완했다.

- `MainActivity.onDestroy()`에서 `homeProjectionWorker.shutdownNow()` 호출 추가
- 화면 재생성/종료 시 홈 projection executor가 남지 않도록 정리
- 기존 `worker`, `thumbnailWorker` 종료 정책과 동일한 lifecycle로 통일

검증:

- `testDebugUnitTest` 통과
- `assembleDebug` 통과
