# PhotoPlace Memory Photo Viewer Review Request

## 대상

- 브랜치: `codex/photoplace-v2-bg-wip`
- 주요 커밋:
  - `25de144 Add in-app memory photo viewer`
  - `c40b037 Preserve memory detail position after viewer`
  - `00ecf6e Polish memory viewer navigation`
  - `07937f6 Refine memory viewer navigation controls`
- 대상 기능:
  - Memory 상세 사진 썸네일 -> PhotoPlace 내부 viewer
  - 동일 place + date section 전체 미디어 표시
  - 사진 좌우 swipe
  - 이전/다음 보조 컨트롤
  - 기억 모음 상세에서도 동일 viewer 사용
  - Back 후 Memory 상세 스크롤 위치 복원

## 확인된 검증 상태

- `./gradlew.bat testDebugUnitTest`: PASS
- `./gradlew.bat assembleDebug`: PASS
- `git diff --check`: PASS
- 실기기 `R5KL503VHQR`에 debug APK 설치 및 프로세스 재실행 완료
- 원본 이동/휴지통/앨범 생성 테스트는 수행하지 않음

## 리뷰 요청 사항

### 1. 내부 viewer 진입 경로

- 일반 Memory 상세의 사진 썸네일이 외부 chooser가 아니라 내부 viewer로 연결되는지 확인한다.
- 기억 모음 상세의 날짜 -> 장소 -> 사진도 동일한 내부 viewer를 사용하는지 확인한다.
- 동영상 썸네일은 기존 외부 player 흐름을 유지하고, viewer 내부의 동영상 항목은 `Gallery에서 열기` 보조 액션으로 처리되는지 확인한다.

### 2. 미디어 source / identity

- `MemoryMediaResolver`가 선택한 단일 source를 viewer가 그대로 사용하는지 확인한다.
- Gallery output과 Discovery refs를 viewer에서 임의로 union하지 않는지 확인한다.
- same place + date section 범위가 유지되는지 확인한다.
- 날짜 메모의 `stableMemoryId + dateKey` ownership이 viewer 진입/복귀 후 유지되는지 확인한다.

### 3. navigation / state

- 선택한 thumbnail 위치에서 viewer가 시작하는지 확인한다.
- swipe 및 이전/다음 컨트롤의 경계 동작을 확인한다.
- 첫 장 이전, 마지막 장 다음이 비활성화되는지 확인한다.
- Back 또는 상단 닫기로 원래 Memory 상세에 복귀하는지 확인한다.
- 상세 화면의 스크롤 위치가 복원되는지 확인한다.
- 화면 회전/재생성 시 viewer 또는 상세 상태가 안전하게 유지되는지 확인한다.

### 4. 실기기에서 발견한 UX 이슈: 전환 번쩍임

이전/다음 버튼을 누를 때 새 이미지가 로드되는 순간 화면이 잠깐 번쩍이는 현상이 있다.

현재 의심 경로:

```text
next/previous tap
-> same ImageView 재사용
-> loadThumbnailInto()
-> placeholder 먼저 설정
-> thumbnailWorker 비동기 decode
-> 새 bitmap 적용
```

다음 사항을 확인한다.

- 실제 원인이 placeholder 재설정인지, ImageView 재측정/layout 변화인지, worker 지연인지
- 이미 thumbnail cache에 있는 사진도 동일하게 번쩍이는지
- 새 사진이 준비될 때까지 기존 bitmap을 유지하는 편이 안전한지
- `thumbnailCache` hit 시 placeholder를 생략할 수 있는지
- 필요하면 짧은 crossfade 또는 adjacent item preload가 적절한지
- 이번 MVP에서 가장 작은 수정으로 flicker를 줄일 수 있는지

권장 범위:

- viewer 전체 재설계는 하지 않는다.
- Gallery/MediaStore mutation은 추가하지 않는다.
- 원본 휴지통 이동은 계속 차단한다.
- MainActivity 비대화 없이 `MemoryPhotoViewer` 또는 thumbnail loading 경계에서 해결한다.

### 5. 구조 및 회귀

- 신규 viewer UI가 MainActivity에 과도하게 몰리지 않았는지 확인한다.
- 기존 Memory detail, Collection detail, 날짜 메모 편집, single-place organize 흐름에 회귀가 없는지 확인한다.
- resolver가 아직 실기기 Gallery-only output에서 검증되지 않은 경우 release gate를 유지해야 하는지 판단한다.

## 최종 판정 형식

1. 승인/조건부 승인/보류
2. Blocker 및 P1/P2 finding
3. 번쩍임 원인 분석
4. 최소 수정 권장안
5. 실기기 smoke test 결과
6. Play release gate 판단
