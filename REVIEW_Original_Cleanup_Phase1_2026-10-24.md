# Original Cleanup Phase 1 — 증거 원장 구현 및 자체 검증

- 날짜: 2026-10-24 (Asia/Seoul)
- 상태: 로컬 구현·자동/격리 실기기 검사 완료, 안티 코드 리뷰 대기.
- Phase 0 안티 승인 이후 사용자의 다음 단계 지시로 진행했다. Phase 2·휴지통 실행·프로덕션 배포는 포함하지 않는다.

## 변경과 안전 경계

- `CleanupEvidenceRecord`: 임의 작업 ID, 실제 source/target URI 쌍, 요청한 target relative path, 원본 촬영 시각, 복사 스트림 바이트 수, 관찰 시각을 보존한다. 작업·쌍의 길이 프레이밍 SHA256은 **recordId용**이며 미디어 내용 해시가 아니다.
- 로컬 MediaStore 사진 URI만 수용하고 동일 URI·영상·클라우드 등은 기록하지 않는다. URI가 같은 것이 불변 파일 신원을 보장하지 않는다.
- `MediaCopyEngine.copy()`는 기존 출력 스트림에서 센 바이트 수와 insert가 반환한 **실제 대상 URI**를 `MediaCopyReceipt`로 반환한다. 파일명 검색으로 대상을 추정하지 않는다. 추가 파일 읽기/전체 파일 해시 계산/대상 재조회는 없다.
- 복사 후 metadata update 반환값, 촬영 날짜 보정, 외부 변경 등을 새로 검증한 것은 아니다. 따라서 모든 행은 `COPY_OBSERVED_UNVERIFIED`, `content_sha256=NULL`이다. 원장에 있다는 이유로 정리 후보가 되지 않는다.
- `CleanupEvidenceSession`: SortJob 실행마다 별도 세션. 최대 50건 또는 5초 경과를 **항목 처리 사이**에 검사하여 SQLite 단일 트랜잭션으로 체크포인트한다. 긴 단일 복사 중 5초마다 저장하는 타이머는 아니다. 정상 종료·취소·예외 종료의 finally에서 남은 기록을 flush하고 자원을 닫는다.
- 첫 저장 실패 시 해당 세션의 기록만 중단하고 버퍼를 비운다. 이후 관찰은 미기록 건수로 세며 메모리 증가/자동 재시도를 막는다. 이전 커밋은 남긴다. 프로세스 종료로 소실된 tail은 추정 복구하지 않는다.
- `CleanupEvidenceStore`: 별도 private SQLite DB, schema 1, `synchronous=FULL`, 트랜잭션 및 작업/source/target 유일성. 같은 작업의 동일 쌍 재저장은 첫 관찰을 보존한다. 다른 작업·복수 대상은 유지한다. 오류를 빈 성공으로 바꾸지 않는다. 향후 읽기에는 최대 100행의 sequence 기반 페이지와 다음 sequence를 반환한다.
- DB 버전이 달라지면 파괴적 재생성 대신 실패한다. 기록 보존 기간/정리 정책은 추가하지 않았다.
- `SortJob`: 복사 성공 처리 후 **복사 try/catch 바깥**에서 증거를 관찰한다. 실제 반환 대상만 사용하며 실패·duplicateInTarget·위치 없음·건너뛴 영상·이동 영상은 증거 대상에서 제외한다. 기존 Activity 경로와 Worker 경로 모두 같은 SortJob을 사용한다. 테스트용 CopyEngine 인터페이스로 격리했다.
- `SortJobResult`/`SortResultStore`: evidence jobId, 커밋/미기록/오류 건수, 체크포인트 시간만 부가 진단 필드로 저장한다. 기존 결과 소비 로직·Worker 성공/재시도 기준은 바꾸지 않았다. foreground 직접 실행 결과에도 메모리 내 통계는 있지만 별도 사용자 진단 UI는 없다.
- `CleanupFeatureGate.isEnabled()`는 계속 false. 삭제/휴지통 요청 API를 추가하지 않았다. 사용자 메모·alias·stableMemoryId 저장소는 변경하지 않았다.

## P0 및 기존 성능 경로

- Phase 0 이전 파일에 저장된 scoped patch를 적용해 Phase 0 완료 상태를 재구성하고 현재 MainActivity와 완전히 일치함을 확인했다. **Phase 1 Activity 변경 0줄**.
- MainActivity SHA256: `72BA13D89DA96748BE7CFE170F4C344BAE82F2FF204767834D93D939BC7036C9`.
- `duplicateInTarget`, 검색/중복 캐시, MediaStoreAlbumLookup, IncrementalMediaScanner, MemoryMediaResolver의 알고리즘은 이번 단계에서 바꾸지 않았다.
- 원장은 기존 복사 작업 스레드에서만 기록한다. 홈/발견/검색 진입 때 원장을 읽거나 파일 해시를 계산하지 않는다.

## 검증

### 자동 테스트 및 빌드

- `gradlew.bat testDebugUnitTest testReleaseUnitTest assembleDebug assembleRelease --console=plain`: 최종 성공.
- Debug **346개**, Release **346개**, 실패/오류/skip 0. Phase 0의 336개에서 원장 9개 및 표시 회귀 1개 추가.
- CleanupEvidenceRecordTest: 동일 작업·쌍의 ID 안정성, 다른 작업/복수 대상 분리, 부적합 미디어 및 같은 URI 차단.
- CleanupEvidenceSessionTest: 50건 버퍼/최종 tail, 시간 체크포인트, 1만 건 중 디스크 실패 시 유계 버퍼·무재시도, 중지/예외 flush, 프로세스 소실 모델, 잘못된 관찰 제외, 마지막 flush/close 오류 격리.
- 기존 CleanupFeatureGateTest 및 LegacyCleanupIsolationTest 통과. `createTrashRequest`/`createDeleteRequest` production 검색 0건.
- lintVitalRelease 통과. 전체 lint는 실행하지 않았다. 기존 Java 8/폐기 API/unchecked 및 CRLF 관련 경고는 남는다.
- 최종 로그: `.debug/cleanup-phase1-final-build.log`.

### 연결된 Samsung SM_F971N에서 격리 검사

`verification/cleanup-evidence-probe`는 **production Java 소스를 직접 컴파일**하는 별도 패키지 `com.photoplace.evidenceprobe`이다. 미디어 권한·런처가 없고 자기 DB에 합성 URI 문자열만 쓴다. 사용자의 사진을 열거나 com.photoplace.app 데이터에 접근하지 않았다. 실행 후 테스트 도구는 제거했다.

1. 실제 Android SQLite에서 동일 쌍 replay, 복수 출력/작업, 제한 페이지, 트랜잭션 중 예외 시 전체 배치 rollback, close/reopen 검사 통과.
2. SQLite query_only로 실제 쓰기를 거부시켜 기록 실패 격리 및 잘못된 행 생성 없음 확인.
3. **실제 SortJob + 가짜 복사 엔진**으로 반환 target URI(9001/9006) 기록, 실패·중복·위치 없음·영상 제외, 저장 오류 시 copy/failed/skipped 건수 보존, 취소 tail flush 확인. 실제 MediaStore 파일 복사 자체를 실행한 검사는 아니다.
4. 50건 커밋 + 17건 메모리 버퍼 + 미커밋 트랜잭션 1건 상태에서 테스트 프로세스를 실제 종료. 새 프로세스에서 **커밋된 50건만** 복구 확인. crash 모드의 `Process crashed / INSTRUMENTATION_CODE: 0`은 의도한 종료이며 후속 audit PASS가 판정 근거다.
5. 합성 관찰 3,000건 / 60회 체크포인트: 최근 실행 총 **297ms**, 체크포인트 **227ms**. 실제 사진 복사/검색 성능이나 다른 기기 수치를 의미하지 않는다.

로그: `.debug/cleanup-evidence-probe-device.log`, `cleanup-evidence-probe-crash.log`, `cleanup-evidence-probe-recovery.log`.

## 별도 요청: 발견 기록 수량 표시

- `MemoryCardCountRenderer`의 사진/재생 ImageSpan을 제거했다. `사진 996장 · 동영상 12개`처럼 명칭·단위를 글자로 표시한다. 숫자 쉼표, 자동 글자 크기, 접근성 텍스트를 유지한다.
- `전체 121개`는 계산 버그를 입증한 것이 아니라 **종류 구분 없는 예전 앨범 합계의 표시 fallback**이었다. 이를 `사진·동영상 121개 (이전 기록)`으로 명확히 했다.
- 이 요청으로 예전 합계를 모두 사진으로 바꾸거나 새로운 전체 앨범 스캔을 넣지 않았다. 종류를 실제로 확인하지 못한 이력의 사진/영상 분리 복구는 이번 수정에 없다. 기존 분리 가능한 장소는 원래 resolved counts를 그대로 쓴다.
- 별도 회귀 테스트: `MemoryCardCountPresentationTest.visibleTextIncludesPhotoAndVideoUnitsWithoutIcons`.

## 남은 검증과 다음 게이트

- **Pre-release**: 새 production 경로를 설치한 후 사용자가 선택한 복사 작업으로 실제 MediaStore URI/원장 저장과 처리량·진행 UI를 확인해야 한다. 이번에는 installed PhotoPlace **1.3.10(40) Phase 0**을 교체하지 않았다. 로컬 APK는 같은 버전 번호의 개발 산출물이며 Play 설치본에 직접 덮어쓸 서명이 아니다.
- **Pre-release**: 원장 스키마/복사 결과 분리/성능 계측에 대한 안티 코드 리뷰 대기. Phase 1 승인 완료로 임의 표시하지 않는다.
- **Follow-up / Phase 2**: 선택 파일 전체 해시 및 재검증, exact Memory 연결과 날짜/메모 의존성 확인, 과거 사진의 읽기 전용 대조. 원장의 source/target/path는 이 검증을 대신하지 않는다. 백업 복원이나 MediaStore ID 재사용도 원장만으로 승인할 수 없다.
- **Follow-up / Phase 3~4**: durable request journal, 시스템 확인, 사후 감사, 복원 안내는 아직 미구현. 휴지통 실행은 닫힌 상태다.

현재까지 자체 검증에서 새 Blocker는 발견하지 않았다. 이는 안티 승인이나 사용자 원본 정리 활성화 승인이 아니다.
