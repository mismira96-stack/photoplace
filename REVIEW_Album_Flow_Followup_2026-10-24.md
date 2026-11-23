# 앨범 흐름 후속 수정 자체 검토

- 2026-10-24 (Asia/Seoul). Phase1 안티 승인 이후 사용자 실기기 제보 및 수량 아이콘 정정을 반영했다.
- 상세 추적: ISSUES_Album_Flow_2026-10-24.md. 원본 정리 Phase2는 시작하지 않았다.

## P0 / P1

- MainActivity 단독 후속 diff는 `.debug/album-flow/activity.patch`에 저장했다. 완료 화면 3개 콜백의 UI routing 위임과 openCompletedAlbums의 UI 상태 초기화만 있다. 데이터 필터·개수·후보 판정 로직은 없다.
- 버튼 표시 판단은 MemoryAlbumAction의 전용 순수 메서드에 격리했고 MemoryBrowserState의 상세 모델이 이를 호출한다.
- Phase1 보고서의 MainActivity 0줄/SHA는 **Phase1 승인 시점**에 대한 사실이다. 그 뒤의 이 UI routing 패치로 파일 해시는 달라진다. 후속 변경을 Phase1 승인 범위로 소급하지 않는다.
- 원장 클래스, MediaCopyEngine, SortJob, 검색/중복 캐시, MediaStoreAlbumLookup, MemoryMediaResolver, 사용자 메모/alias 저장을 이번 UI 수정에서 바꾸지 않았다. 신규 MediaStore 쓰기/복사/휴지통 실행 없음. CleanupFeatureGate=false 유지.

## 수정 검토

1. **추가 버튼 잔류**: 이전에는 live discovery ref 유무만 검사했다. 이미 복사된 원본은 detail projection에서 빠져도 record에는 남으므로 버튼이 살아 있었다. 이제 resolved detail refs에 포함된 원본 사진 URI만 확인한다. 복사본 URI를 원본으로 세거나 영상만으로 켜지 않는다. 기존 resolver의 복사 중복 건너뜀 정책은 변경하지 않는다. 이 URI/파일명 표시는 원본 삭제 자격이 아니다.
2. **완료 후 목록 위치**: createdAtMillis 내림차순인 목록에 맞춰, 완료 화면의 보기에서만 이전 UI 검색·스크롤을 초기화한다. 일반 목록/상세 뒤로가기의 저장 위치 복원은 보존한다. 실제 사용자 폰 확인은 새 빌드 설치 후 남아 있다.
3. **아이콘+단위**: 사용자 최종 요청에 따라 아이콘을 복원하고 숫자 뒤에 장/개를 붙였다. 접근성 설명은 전체 사진/동영상 텍스트를 쓴다. 미구분 예전 합계는 계속 별도 표시한다.
4. **청주·서초동·용인 버튼 없음**: 청주 UI와 청주/서초동 실제 앨범 존재를 읽기 전용으로 확인했다. 그러나 세 장소의 내부 discovery refs/sourceType을 확인하지 못했으므로 정상 정책/버그를 확정하지 않았다. 기존 앨범만 있는 모델에는 만들기 버튼을 켜지 않는 회귀 검증은 추가했다. 이 항목은 열어 둔다.

## 검증

- `testDebugUnitTest testReleaseUnitTest assembleDebug assembleRelease`: 성공, Debug351/Release351, 실패·오류·skip0. lintVitalRelease 통과, 전체 lint 미실행.
- 신규 회귀 5개: 추가 가능 원본·완료 원본·영상·대상 폴더 원본·새 장소/기존 앨범 및 아이콘 단위. 기존 MemoryBrowserStateTest의 동일 IMG.jpg fixture가 이미 복사된 것으로 해석되는 경우는 버튼 숨김 기대값으로 수정했다. 새 원본을 보이는 별도 검증은 유지했다.
- `git diff --check`: 공백 오류 없음, 기존 CRLF 경고만 존재.
- 폰에는 아직 Phase0 1.3.10(40). 이번 코드 설치/프로덕션 배포/사용자 사진 자동 복사는 수행하지 않았다.

## 안티 후속 검토 요청

- MemoryAlbumAction(record, resolvedRefs)의 원본/복사본 구분이 실제 projection과 일치하는지 확인해 달라.
- openCompletedAlbums의 UI 초기화가 완료 진입 3곳에만 적용되는지, 일반 뒤로가기 복원에 영향이 없는지 확인해 달라.
- 수량 표시의 최종 요구는 **아이콘+단위**다. Phase1 승인 보고서의 아이콘 제거는 승인 당시 요구/스냅샷이며 사용자 최신 정정이 우선한다.

## Phase1 승인 참고 및 Phase2 경계

- REVIEW_Original_Cleanup_Phase1_Antigravity_2026-10-24.md의 코드 승인 확인. 실제 설치/사진 복사 처리량 확인은 여전히 별도다.
- Phase1 체크포인트는 복사 작업 스레드 안에서 동기 트랜잭션을 수행하며, UI 밖이라는 뜻이지 별도 writer queue가 있다는 뜻은 아니다.
- Phase2에서 MediaStore content URI는 ContentResolver로 읽어 검증해야 한다. 임의 파일 경로의 exists/length만으로 실존·완전성을 승인하지 않는다. 접근 불명/권한 부족과 부재를 구분하고 전체 바이트 SHA256 및 MemoryDependencyGuard가 필요하다.
- 시스템 휴지통 요청 후 감사는 Phase3~4 실행 검증과 연결된다. Phase2 착수 승인이 휴지통 실행 권한을 자동 부여하지 않는다.
