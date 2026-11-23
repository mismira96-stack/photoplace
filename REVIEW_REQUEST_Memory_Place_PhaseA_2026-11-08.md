# Antigravity 코드 리뷰 요청 — 수동 장소/미해결 미디어 Phase A

> 최신 R1: 안티 A-1/A-2 후속 수정 완료. 현재 결과는 Debug/Release 각453개 및 API33 프로브 통과. 세부 조치/채택하지 않은 권고/미구현 범위는 `REVIEW_Memory_Place_PhaseA_Codex_Followup_2026-11-08.md`를 먼저 읽을 것. 아래446개/3929ms와 코드 설명 일부는 최초 제출 시점 기록이다. 현재 소스는 갱신된 snapshot을 기준으로 리뷰한다.

- 작성: Codex / 2026-11-08
- 상태: 리뷰 준비. **두 기능 전체 완료/설치용 빌드/출시 승인이 아니다.**
- 범위: 기존 증분 스캔의 미해결 미디어 보존, SQLite 커밋 경계, canonical ID 읽기 합성, revision 무효화, 미구현 연결의 원본 정리 차단.
- 설계: `DESIGN_Memory_Trash_Manual_Place_2026-11-08.md` V1.2, 특히 12절.
- 리뷰 보고서 권장: `REVIEW_Memory_Place_PhaseA_Antigravity_2026-11-08.md`.
- 검토 준비 표식: `REVIEW_READY_Memory_Place_PhaseA_2026-11-08.json`.

## 구현 파일

| 파일 | 이번 변경 |
| --- | --- |
| ManualPlaceStore.java | manual_state(작은 identity/assignment payload+revision)와 manual_inventory(행별 raw 항목)를 한 SQLite 트랜잭션에서 저장. 읽기 경로는 DB 생성/복구/마이그레이션 안 함. |
| ManualPlaceSnapshot.java | 불변 snapshot, canonical alias 합성/충돌 중단, inventory와 identity 분리 읽기 모델. |
| ManualPlaceInventory.java | 선택 폴더 범위 reconciliation의 순수 모델, 기존 assignment 보존. |
| UnresolvedMediaRecord.java | URI+실제 볼륨+MediaStore 버전+타입+ID, 메타데이터 및 UNRESOLVED 상태. GPS 없음이라고 단정하지 않음. |
| UnresolvedMediaCapture.java | 기존 scanner 행 관찰, 볼륨별 버전 캐시, 정상 완료 때만 batch 저장. 보조 저장 오류는 원래 스캔 성공과 분리. |
| IncrementalMediaScanner.java | scanPaths 고정 복사, 기존 쿼리에 volume_name만 추가, 스캔 취소 시 미해결 inventory 커밋 안 함. 별도 전체 스캔 없음. |
| MemoryIdentityRegistryStore.java | Context 기반 ID 읽기는 수동 DB와 읽기 합성. manual: 별칭은 JSON에서 임의 생성/상충 등록 불가. File 기반 테스트/기존 호출은 기존 데이터 동작 유지. |
| HomeRevisionReader.java | 가벼운 단일 revision 조회 반영. inventory 전체 로드 안 함. |
| CleanupMemorySnapshotReader.java | 동일 identity snapshot/revision 포함, ID 충돌 검증. 수동 assignments가 있으면 전체 투영 구현 전까지 명시적 중단. |
| ManualPlaceFoundationTest.java | 신규 13개 단위 테스트. |
| verification/cleanup-execution-probe/.../ManualPlaceFoundationProbe.java 및 Manifest | 독립 테스트 패키지용 실제 SQLite·스캔 통합 검증. 메인 앱 설치/사용자 미디어에 접근하지 않음. |

기존 작업 트리에 많은 미추적 파일/수정이 있으므로 HEAD 전체 diff를 이번 구현으로 취급하지 말 것. 위 파일 중 기존 클래스 변경은 위 설명 범위다. `REVIEW_SNAPSHOT_Memory_Place_PhaseA_2026-11-08.json`은 현재 Java 소스·테스트 해시를 고정한다.

## 실제 검증

- `gradlew.bat testDebugUnitTest testReleaseUnitTest assembleDebug --console=plain`: Debug/Release 각 446개, 실패/오류/스킵 0. Debug APK 빌드 성공. 기존 Java8/deprecated 경고 존재.
- `MainActivity.java` SHA256: `39ec3c85cc6583d8918a92d6363bfa4e737ff46a8c4c1acffcd00d3135ded9a9` — 기존과 동일.
- 에뮬레이터: `PhotoPlaceCleanup_API33`, API33, emulator-5584. 독립 패키지 `com.photoplace.cleanupprobe`만 설치.
- 실제 SQLite에 3,500건(긴 파일명 포함, 합계 CursorWindow 크기 초과)을 행별 저장/조회. 다른 폴더 부분 갱신, 잘못된 범위 롤백, 첫 초기화 트랜잭션 롤백 후 정상 재시도, 손상 payload 읽기 거부, merge-on-read, assignments 보존 통과.
- 테스트 패키지를 force-stop한 다음 별도 instrumentation 실행: 3,500건과 canonical ID/assignment 재조회 통과. 이는 정상 커밋 후 프로세스 재시작 시험이며 전원 차단 시험이 아님.
- 테스트 패키지 소유의 8x8 PNG 한 개만 생성하고 기존 IncrementalMediaScanner로 조회: 미해결 항목 저장, 2회차 metadata cache 재사용, 취소 시 revision 유지, 다른 폴더 inventory 보존 통과.
- 측정값: 긴 이름 3,500건 저장+전체 조회 3,929ms(해당 에뮬레이터 1회). 일반 사용자 기기 성능 보장으로 일반화하지 않음.
- 로그: `.debug/manual-place-2026-11-08/{final-build.log,sqlite-probe.txt,sqlite-resume-probe.txt,scanner-probe.txt}`.
- helper의 첫 빌드는 SDK 경로 환경 누락으로 실패. ANDROID_HOME 지정 후 성공. 코드 오류를 테스트 PASS로 덮지 않으며 최종 결과만 위와 같이 확정.

## 특히 비판적으로 검토할 항목

1. SQLite schema version0의 초기화 롤백과 기존 손상 DB 구분, 동시 프로세스 트랜잭션 및 읽기 도중 revision 변경 감지.
2. 단일 행 대용량 payload 회피가 충분한지, identity 읽기에 inventory 파싱이 유입되는 경로가 없는지.
3. selected paths 범위 교체가 SD카드/제한된 권한/미디어 버전 변경에서 다른 파일의 assignment를 지우지 않는지. raw inventory는 가용 입력이며 파일 삭제 증거가 아님.
4. canonical alias 충돌을 덮어쓰거나 수동 별칭이 legacy JSON에 따로 생기는 우회가 없는지. 기존 alias 읽기/쓰기 동작 회귀 여부.
5. 정상 스캔 성공과 inventory 저장 실패가 분리되는지. 다음 단계에서는 보조 저장 실패/미확보 상태를 사용자에게 표시하는 freshness 계약 필요.
6. cleanup의 임시 차단이 fail-closed인지. 전체 수동 projection이 구현되지 않은 상태에서 assignments를 통과시키면 안 됨.
7. MainActivity 무변경, 기존 MediaCopyEngine/SortJob/원본 정리 실행 싱크 및 활성화 정책 무변경 확인.

## 미구현 / 다음 단계 (PASS 처리 금지)

- 장소 생성·연결 변경·해제의 쓰기 API와 선택 UI, 미해결 항목을 메모리뷰로 노출하는 투영.
- 동명 파일 억제 예외, 실제 파일 출처를 가진 MemoryPhotoItem 및 선택 요약(B2).
- raw 무결성 검사 후 수동 연결 투영 및 copy/move receipt 내구성·승계(B3).
- 동영상 단독 앨범 생성(B4의 남은 부분), LABEL_ONLY의 동명 앨범 경로 정책.
- 사진 0장+노트 존재 기록/날짜 섹션 및 페이지 처리(B5).
- 일반 휴지통 UI·OS 요청·저널·Worker·영속 변경 직렬화 및 API30 게이트.
- API29/36 실제 런타임 검증, 메모리뷰 → 장소 지정 → 앨범 → 일반 휴지통 전체 E2E, 실기기 UX.

판정은 Phase A에 한정해 Blocker / Pre-release / Follow-up으로 구분한다. 코드 수정·설치·출시를 자동 실행하지 말고 보고서를 남겨 다음 구현의 입력으로 사용한다.
