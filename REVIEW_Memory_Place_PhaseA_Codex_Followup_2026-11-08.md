# Phase A 안티 리뷰 검토 및 후속 수정 R1

- 작성: Codex / 2026-11-08
- 검토 원문: REVIEW_Memory_Place_PhaseA_Antigravity_2026-11-08.md
- 판정: A-1/A-2의 결함을 확인하여 수정·회귀 검증 완료. 독립 재리뷰는 아직 받지 않음.
- 대상 범위: Phase A 기반. 두 기능 전체 구현/출시 승인이 아님.

## P0 / P1 먼저 확인

- MainActivity.java SHA256는 `39ec3c85cc6583d8918a92d6363bfa4e737ff46a8c4c1acffcd00d3135ded9a9`로 기존 동일.
- 실제 미디어 변경 코드·기존 원본 정리 실행 정책·copy engine·SortJob 수정 없음. 수동 assignment 미완성 상태의 cleanup fail-closed 유지.

## 검토 및 조치

| 심각도 / 항목 | 코드 근거와 재현 | 조치 / 현재 상태 |
| --- | --- | --- |
| Blocker A-1 음수 부가 메타데이터로 전체 캡처 폐기 | 기존 UnresolvedMediaRecord 생성자가 음수 size/modified/takenAt을 거부하고 Capture가 버퍼 전체를 폐기 | UnresolvedMediaRecord.java:27에서 0(미상)으로 정규화. 볼륨/버전/타입/ID/URI의 엄격한 신원 검사는 유지. 0을 파일 내용 보존 증거로 사용하지 않음. 단위/실제 SQLite/실제 scanner 프로브 통과. |
| Blocker A-2 대형 JSON 반복 파싱 | 기존 snapshot이 전체 인벤토리 문자열을 생성·재파싱, 조회마다 다시 record 생성 | ManualPlaceSnapshot.java:14에서 작은 identity payload·파싱된 alias·assignment 여부·불변 record 목록 분리. unresolved()/mergedAliases()/hasAssignments()는 전체 JSON을 파싱하지 않음. DB 행은 읽을 때 한 번만 파싱. |
| Blocker A-2 DB/pure 모델 불일치 | writer가 identity-only before를 reconciliation 모델에 넘겨 scope 밖 동일 key 충돌을 누락 | ManualPlaceInventory.Update가 incoming/scope/revision을 검증. ManualPlaceStore.java:80은 기존 DB의 key/path를 읽고, scope 밖 충돌 후보에 한해 payload/URI/경로를 확인. 검사가 끝나기 전 DELETE/INSERT 없음. 충돌 시 트랜잭션과 revision 롤백. pure reconcile도 동일 Update 계약 사용. |
| A-1 추가 경로 비교 쟁점 | SQL과 Java의 경로 비교가 다르면 정상 캡처를 폐기하거나 다른 폴더를 지울 수 있음 | ManualPlacePaths에서 슬래시·후행 슬래시를 통일. 대소문자는 합치지 않음. scanner는 정규화한 같은 경로를 COLLATE BINARY 조건으로 조회. case-distinct 경로 보존/역슬래시/경로 탈출 거부 테스트 추가. |
| Pre-release A-1 Freshness | 미캡처/실패와 실제 사진0개 구분 필요 | Phase B 필수 조건으로 유지. 아직 UI·캡처 실패 상태 영속 기록을 추가하지 않았으므로 해결 완료 처리 안 함. revision==0만으로 향후 장소 생성 후의 캡처 여부까지 판단하면 안 됨. |
| Pre-release A-2 정리된 복사본 재유입 | 무GPS 복사본도 raw inventory에 보존될 수 있음 | raw 관찰은 유지. Phase B에서 확정된 organized path/receipt/assignment를 사용해 effective 위치 없음 투영에서 제외. 폴더 이름만으로 전체 제외하지 않음. 아직 투영 구현 없음. |
| Follow-up 읽기 트랜잭션 제안 | OPEN_READONLY DB에서 beginTransactionNonExclusive() 권고 | API33 독립 프로브에서 SQLiteReadOnlyDatabaseException으로 실제 거부됨. 이 제안을 그대로 적용하지 않고 읽기 전후 revision 비교 유지. 다른 API에서 항상 같다고 단정하지 않음. |

## 검증 결과

- `gradlew.bat testDebugUnitTest testReleaseUnitTest assembleDebug --console=plain`: Debug/Release 각 **453개**, 실패0/오류0/스킵0. 기존446 대비 신규 회귀 테스트7개 추가. Debug 빌드 성공.
- 새 테스트: 음수 metadata+정상 peer 보존, 3500개 불변 목록/identity payload 분리, scope 밖 key/URI 충돌, 실제 동일 URI의 scope 간 이동 반영, incoming 중복 거부, 슬래시 정규화/대소문자 구별, 경로 탈출 거부.
- API33 / emulator-5584 / 테스트 전용 패키지 `com.photoplace.cleanupprobe`:
  - 긴 이름 3500건 중 음수 size/modified/takenAt 행을 섞어 실제 SQLite 저장·조회 통과. 기존 동료 행 손실 없음.
  - scope 밖 동일 asset_key/다른 URI 충돌 실제 DB에 구성 → 변경 및 revision 없이 롤백, 기존 행 보존.
  - 첫 초기화 롤백/손상 payload 거부/identity 읽기 합성/assignments 보존 통과.
  - force-stop 후 별도 프로세스에서3500건과 canonical ID/assignment 재조회 통과.
  - 소유 PNG만 사용한 기존 IncrementalMediaScanner의 음수 날짜 입력·metadata cache 재사용·취소 시 미커밋·타 폴더 보존 통과. 명시적 BINARY 경로 조건을 MediaProvider가 수용함을 확인.
  - 읽기 전용 연결의 nonexclusive transaction은 실제 거부됨.
- 측정: 같은 형태의 긴 파일명3500건 저장+조회는 이전 **3929ms**, 수정 후 **370ms**(수정 후 쓰기165ms 포함). 각각 단일 실행이고 워밍업/CPU 상태를 통제한 벤치마크가 아니다. JSON만이 지연의 유일한 원인이라는 주장이나 일반적인 N배 향상 보장은 하지 않음.
- 로그: `.debug/manual-place-2026-11-08/followup-build.log`, `followup-probe-build.log`, `followup-sqlite-probe.txt`, `followup-resume-probe.txt`, `followup-scanner-probe.txt`.
- 테스트용 에뮬레이터는 종료했다. 폰/메인 설치본/Play 상태 변경 없음.

## 다음 리뷰/구현 경계

현재 수정 R1을 독립 재검토할 수 있도록 snapshot 및 REVIEW_READY 표식을 갱신한다. 기존 원문 보고서는 수정하지 않는다.

Phase B에는 freshness(미캡처·마지막 정상 scope·실패/부분 접근), organized-copy 재유입 방지와 기존 B2~B5를 포함한다. 수동 장소 UI/receipt 승계/노트 전용 기록/일반 휴지통 실행은 아직 구현되지 않았다. 이 보고서를 전체 기능 완료로 해석하지 않는다.
