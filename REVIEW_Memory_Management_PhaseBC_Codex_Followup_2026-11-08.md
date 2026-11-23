# Phase B/C 안티 리뷰 후속 수정 — 기기 테스트용 code57

독립 리뷰 `REVIEW_Memory_Management_PhaseBC_Antigravity_2026-11-08.md`의 실제 코드를 대조한 결과 B-1/B-2/B-3은 유효했다. 최초 에뮬레이터 backend 테스트가 확인창 경로를 우회해 B-1을 놓친 것도 확인했다. 세 항목을 수정했으며 **독립 재승인은 아직 없고 프로덕션 준비 상태는 false**다. 사용자 요청으로 연결된 폰에 테스트 설치를 준비한다.

## B-1 단일 기록 동영상 확인/실행 불일치

- MainActivity의 기존 단일 확인 분기 전체를 MemoryAlbumConfirmationController로 추출. MainActivity에는 UI/탐색/실행 callback 위임만 남겼다.
- 확인창과 실행 모두 OrganizationMediaPolicy의 동일한 수동 동영상·사용자 설정 조건을 사용한다. hardcoded false 제거.
- DiscoveryOrganizeConfirmDialog는 사진과 동영상의 수량/단위를 구분하고 실제 이동하는 동영상은 '위치 앨범으로 이동'으로 표시한다.
- API33 실제 Dialog를 열고 '위치 앨범으로 이동' 노출, '이 장소만 만들기' 버튼 활성/클릭, 같은 mem_ ID를 실행 callback에 전달하는 경로를 검증했다. 이후 실제 MP4 이동도 통과했다.

## B-2 메인 스레드 영속 I/O 및 충돌 시 레거시 우회

- SortStartController가 준비 예약·SortInputStore 저장·결과 초기화·WorkManager enqueue를 executor에서 수행한다. UI에는 시작/실패 callback만 전달한다.
- runCopy의 direct-copy fallback과 미사용 m41/m39 레거시 실행 본문을 물리적으로 제거했다. 예약 거절 시 어떤 경우에도 다른 복사 엔진으로 우회하지 않는다.
- 실패 시 사용자에게 원래 한국어 충돌 사유를 전달하고 working 상태를 해제한다.
- SortInputStore 쓰기는 AtomicFile + fsync로 변경했다. durable input이 저장된 뒤에만 WorkManager KEEP enqueue한다.
- API33에서 활성 일반 휴지통 journal을 만든 뒤 실제 UI launcher를 호출: '휴지통' 충돌 사유 callback, input 미저장, 복사 우회 없음 확인.

## B-3 복사본 장소 변경/해제 시 원본 재등장

- ManualAssignmentSelection이 확인된 동일 장소 source→copy receipt 연결을 양방향으로 확장한다. 장소 메타데이터 변경에만 적용하며 **일반 휴지통 대상은 여전히 실제 선택 파일만**이다.
- 변경 시 같은 SQLite 트랜잭션에서 원본/복사본을 함께 새 장소로 연결하고 논리 날짜, copyTargetKey, galleryCopy 플래그를 보존한다.
- 해제 시 연결된 원본/복사본의 앱 내부 지정을 함께 해제한다. 실제 파일·물리 갤러리 앨범·날짜 메모는 삭제/이동하지 않는다.
- 확인 UI에 연결된 원본/복사본의 장소 지정도 함께 바뀐다는 설명을 추가했다.
- 이미 다른 장소로 분리된 legacy pair를 임의 합치지 않는다. 선택과 같은 장소에서 확인된 연결만 따라간다.
- API33 SQLite 실제 테스트: C를 새 장소로 지정→S도 같은 장소, C.galleryCopy=true/S.copyTargetKey 보존, 화면은 C 한 장만 표시; 기존 장소로 복귀; C 해제→S/C assignment 모두 제거. 원본/복사본 파일은 보존된다.

## 재검증

- Debug474 / Release474 tests, 실패0 오류0 스킵0. `followup-BC-tests.log`: testDebugUnitTest + testReleaseUnitTest + bundleRelease PASS.
- 신규 정책 테스트4개: pair 양방향 확장·기선택 중복 제거·분리된 장소 미전파·수동 동영상 확인/실행 설정 일치.
- `followup-BC-flow.log`: 기존 known/unknown 날짜·원본 시각·SHA256·날짜 메모/불변 ID·cleanup PRESERVED·동영상 이동·일반 휴지통 감사 회귀 검사와 위 세 항목 통합 검증 PASS.
- Dialog 최초 접근성 검사에서 animation/layout 전의 root를 읽어 테스트가 실패했다. 실제 text가 뜨는 bounded wait로 테스트를 수정하고 재실행 PASS. 제품 코드의 실패를 숨긴 것이 아니라 실제 OS 접근성 텍스트와 클릭 callback까지 확인했다.
- 사용자 폰의 앱은 Play 서명 code56. code57 서명 APK를 내부 draft에서 생성해 update install 예정. 공개 출시/검토 제출 권한으로 확대하지 않는다.

재리뷰 요청: 위 세 수정의 경계, 특히 비동기 sort 시작 실패·프로세스 재생성·paired metadata 변경을 반증할 것. 최초 independent report는 그대로 보존. 현재 source snapshot/READY는 후속 수정본을 가리키도록 갱신한다. 실제 폰 검증 및 API29/부분 권한/신규 Android/SD/재부팅 게이트는 계속 남아 있다.
