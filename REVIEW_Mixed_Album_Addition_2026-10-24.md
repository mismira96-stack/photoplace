# 성남 MIXED 장소 앨범 추가 경로 수정

- 날짜: 2026-10-24 (Asia/Seoul).
- 요청: 성남 발견 기록에는 2,200장 이상인데 기존 위치 앨범이 작고, 상세에서 앨범 만들기 버튼이 보이지 않는 문제 디버깅.
- 범위: 기존 working tree 위의 국소 수정. 제품 배포/기기 설치/사진 복사/이동/삭제 및 앱 데이터/캐시 초기화 없음.

## 원인 및 재현

- `MemoryRepository.java:295` 부근 MIXED record는 canOrganize=false, canAddNewItems=true로 구성된다. 이 모델은 신규 앨범 생성과 기존 앨범 추가를 구분하지만 후자가 실행 UI에 연결돼 있지 않았다.
- 기존 `MemoryBrowserState.java:344`는 canOrganize만 상세 액션으로 전달했다. Activity의 단일 장소 진입 및 재조회도 canOrganize만 검사하여, 버튼만 표시해도 실제 준비에서 거부되는 구조였다.
- 기존 앨범 정리 prepare는 장소 이름으로 대상을 재탐색하므로 MIXED 추가에 그대로 적용하면 이미 연결된 경로를 고정하지 못한다.
- `MemoryBrowserStateTest.mixedPlaceWithLiveDiscoveryPhotosOffersAlbumAddition`을 수정 전에 실행: 1 test / 1 failure. MIXED 상세 액션이 false임을 재현했다.
- 성남의 실제 수량 차이 전부가 누락이라는 결론은 내리지 않는다. 이전 실기기 읽기 기록은 `REVIEW_Device_Icon_And_PostSort_Record_2026-10-24.md` 참조.

## 수정

- 신규 `MemoryAlbumAction.java`: 단일 Memory 사진 정리 가능 여부, 추가/생성 표시 문구, MIXED 기존 경로 고정 및 읽기 전용 후보 준비 담당.
- MIXED 상세에는 `앨범에 추가` 표시. 실제 사진 후보 수는 버튼을 누른 뒤 기존 lookup으로 준비하여 확인창에 표시하며, 확인 버튼은 `사진 추가하기`로 안내한다. 버튼 존재 자체가 미복사 사진 존재를 확정하지 않는다. 모두 중복이면 기존의 0-copy 안내 경로를 따른다.
- MIXED는 record가 가리키는 Pictures 하위 기존 경로만 사용하고 이름 기반 다른 폴더 탐색을 호출하지 않는다. 기본/상위이동/비정상 경로는 거부. 기존 경로에서 사진 또는 영상 존재를 확인하지 못하면 새 폴더를 만들지 않고 중단한다.
- live/stale/empty URI 및 사진/영상 구분. 영상만 남은 장소에는 사진 추가 액션을 표시하지 않는다. 후보 준비에 영상 refs가 포함될 수 있으나 actionableCount(false) 및 기존 OrganizationMediaPolicy의 영상 제외를 유지한다.
- Gallery 조회 불명/실패의 unresolved 상세에서는 정리 액션을 비활성화한다.
- MainActivity 변경은 전용 정책/준비 호출과 라벨 바인딩 대체. 새 판단/계산 로직을 Activity에 추가하지 않았다. 기존 가드를 전용 클래스로 이동했으며 전체 Activity에 대한 새 전수 리뷰를 의미하지 않는다.
- 글로벌 발견 일괄 정리, MemoryRepository의 source/identity/집계 규칙, 기존 duplicateInTarget 알고리즘, MediaStore 스캔/분석 및 홈/상세 캐시 코드 수정 없음. 원본 휴지통 제한 유지.

## 검증

- 수정 전 실패한 MIXED 상세 액션 회귀 테스트 통과.
- 신규 MemoryAlbumActionTest 7개: known path 고정/이름 재탐색 금지, 중복+영상 제외 수량, 입력 refs 보존, 전부 중복 시0, 대상 없음/조회 실패 중단, stale/빈 URI/영상 전용 제한, 비정상 경로 거부, 기존 discovery-only 생성 유지.
- `gradlew.bat testDebugUnitTest testReleaseUnitTest assembleRelease --console=plain`: BUILD SUCCESSFUL.
- Debug 327 tests / failures0 / errors0. Release 327 tests / failures0 / errors0.
- 변경된 추적 파일 `git diff --check` 통과. CRLF 정규화 경고 있음. 빌드에는 기존 Java8/deprecated/unchecked 경고 있음. 전체 lint는 이번에 실행하지 않음.
- 로그: `.debug/device-20261024/mixed-regression-before.log`, `mixed-regression-after.log`, `mixed-full-verification.log`.
- 빌드 산출물: `app/build/outputs/apk/release/app-release.apk` (1.3.7/code37, 로컬 검증용, 새 버전으로 게시하지 않음).
- SHA256: `790E3CFC69BB96B6109851E035CCC1988FFC228994BA560FBACF329AE425C5D0`.

## Pre-release / 미완료

1. 폰 설치본은 Play 앱 서명, 로컬 릴리스는 업로드 키 서명으로 달라 직접 덮어쓸 수 없음. 이번 패치를 사용자 폰에서 검증했다고 주장하지 않는다. 삭제/재설치로 앱 데이터를 잃지 않도록 유지한다.
2. 성남의 실제 추가 후보 수와 화면 표시/중복 건너뜀은 Play 서명의 후속 테스트 버전 등으로 검증 필요. 기존 앨범 실제 추가는 사용자의 확인 후에만 수행한다.
3. 기존 파일명 정규화 중복 추정은 그대로다. 내용이 다른 동명 파일을 건너뛸 가능성 및 source 범위 차이는 남는다. 이번 수정은 모든 수량 차이나 중복 동일성 문제의 해결이 아니다.
4. 대상 경로 존재 확인은 준비 시점의 조회 결과다. 확인 이후 외부에서 앨범이 변경되는 TOCTOU 및 Worker 재시도/부분 성공은 기존 동작과 한계 유지.
5. 수원 정리 직후의 일시적 ‘기록 없음’ 안내는 별도 미재현 이슈이며 이번 수정으로 해결됐다고 간주하지 않는다.
6. 검색 성능 회귀는 실기기 미계측. 캐시/스캔 코드는 변경하지 않았고 기존 자동 테스트가 통과했으나 무조건 무회귀를 보장하지 않는다.
