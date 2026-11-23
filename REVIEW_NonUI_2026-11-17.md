# PhotoPlace 비 UI 점검 · 2026-11-17

## 발견 기록 자동 갱신

- **Follow-up · 앱 복귀 직후 다른 작업이 진행 중이면 자동 확인이 생략될 수 있다.** 복귀 시 250ms 뒤 확인을 예약하지만, 실행 시점에 미리보기 확인이나 앨범 작업이 진행 중이면 즉시 반환한다. 카메라 파일 등록을 위한 후속 확인은 스캔 작업이 제출된 뒤 완료 콜백에서만 예약한다. 따라서 처음 예약한 확인이 이 조건에 걸리면 작업이 끝나도 이번 앱 복귀의 자동 확인은 다시 예약되지 않는다. 다음 앱 복귀나 수동 갱신에서 확인될 수 있다. 코드 경로 확인이며 실기기 재현은 하지 않았다. [확인 시작과 조기 반환](app/src/main/java/com/example/gallerysorter/DiscoveryAutoRefresh.java#L53) · [후속 확인 예약](app/src/main/java/com/example/gallerysorter/DiscoveryAutoRefresh.java#L118)
- **수정 후보.** 전용 자동 갱신 클래스에서 앱 복귀 시 작업 중이라 생략된 확인만 짧게 다시 예약한다. 재시도 횟수를 제한하고 화면을 떠나거나 수동 갱신을 시작하면 취소한다. 기존 캐시, 스캔 대상 폴더, 메모·개인 장소 및 원본 사진 처리에는 변경이 필요하지 않다.
- **검증.** 현재 로컬 디버그 클래스에서 자동 갱신 세션, 발견 기록 비교, 화면 캐시, 개인 장소, 백업 저장소 관련 기존 JUnit 56개를 실행해 통과했다. 기존 세션 테스트는 앱 복귀 확인의 예약 여부까지 다루지 않는다. 앱 코드 수정 및 설치는 하지 않았다. [작업 기록](WORKLOG_2026-11-17.md)

## 발견 기록 탭·상세 재진입 로딩 비교

- 로컬 1.3.45 프로덕션 AAB와 1.3.46 출시용 AAB의 DEX에서 발견 기록 목록 컨트롤러와 카드 렌더러의 구현 차이를 확인했다. 상세 로더, 공용 로더, 상세 캐시와 표지 로더의 비교 대상 클래스 구현은 일치했고, 상세 사진 렌더러는 달랐다. 실제 Play 설치본 해시와 사용자 폰 재진입 시간은 확인하지 않았다.
- 목록은 이전 데이터가 있어도 그리드를 다시 생성한다. 2열에서 개인 장소가 있으면 각 카드의 바로가기 바인딩마다 장소 ID와 개인 장소 파일을 다시 읽는다. 표지 캐시는 홈 등과 32개를 공유하며 미스는 비동기 요청을 만든다. 동일한 화면 기준으로 첫 개선 후보는 카드마다 하는 파일 읽기를 목록 로드당 한 번으로 줄이는 것이다. [목록 로더](app/src/main/java/com/example/gallerysorter/MemoryBrowserLoadController.java#L61) · [카드별 개인 장소](app/src/main/java/com/example/gallerysorter/PersonalPlaceDetailPresenter.java#L472) · [상세 캐시](app/src/main/java/com/example/gallerysorter/MemoryDetailLoadController.java#L21)
