# PhotoPlace 1.3.20(code56) 출시 전 독립 코드 리뷰 요청

## 요청과 현재 상태
사용자는 실기기 정리 완료 및 UI 원복 확인 후 '코드 리뷰하고 출시하자, 일단 리뷰할 수 있게 남겨 달라'고 요청했다. **이번 산출물은 리뷰 준비이며 출시 승인 보고서가 아니다.** 프로덕션 code51 중지 상태를 유지한다. 리뷰 결과에서 차단 이슈가 해소된 뒤 출시 절차를 이어간다.

소스 경로는 `C:\work\projects\PhotoPlace`이다. OneDrive의 예전 Photo Place 폴더가 아니다. `AGENTS.md`와 `GEMINI.md`를 먼저 읽는다.

코드는 수정하지 말고 우선 독립 리뷰 결과를 `REVIEW_Cleanup_Code56_Antigravity_2026-10-25.md`에 기록해 달라. 문제마다 심각도(Blocker/Pre-release/Follow-up), 현재 파일/줄, 재현 조건, 영향, 수정 권고와 필요한 검증을 제시한다. 결함을 못 찾았으면 못 찾았다고 쓰되 시험하지 않은 경로를 통과로 표시하지 않는다.

## 1. 정확한 검토 기준
- versionName **1.3.20**, versionCode **56**, targetSdk **36**, package `com.photoplace.app`.
- Git HEAD `1c8d6832b5e43278cef83012ab6b8753f59624ab`와 **현재 작업 트리 전체**가 검토 대상이다. 많은 Cleanup 파일이 untracked이므로 `git diff`만 보면 핵심 구현을 놓친다. HEAD만 검토해서는 안 된다.
- 파일별 SHA256/검증 결과: [REVIEW_SNAPSHOT_Cleanup_Code56_2026-10-25.json](REVIEW_SNAPSHOT_Cleanup_Code56_2026-10-25.json).
- 텍스트 소스 고정본(282파일): [.review/cleanup-code56-2026-10-25/source.zip](.review/cleanup-code56-2026-10-25/source.zip).
- 스냅샷 SHA256: `ff0fbc067dd9a9996ddc350074c98c5ffcd8f20709dc4707aa3c7c25035a4c7a`.
- ZIP에는 Java 소스/테스트, 리소스 XML, Manifest, app Gradle, AGENTS/GEMINI와 추적 파일의 작업트리 diff, MainActivity 이전 UI baseline이 있다. 기기 사진/데이터·서명키·자격증명·local.properties는 포함하지 않는다. 이미지 리소스/비밀 서명 설정을 포함한 독립 빌드 프로젝트가 아니라 **리뷰용 텍스트 스냅샷**이다. 빌드는 원래 로컬 프로젝트에서 한다.
- 라이브 파일의 해시가 manifest와 달라지면 이후 변경을 구분해서 검토한다.

## 2. P0를 먼저 확인
- MainActivity.java는 UI/백그라운드 후속 작업 전 `.debug/cleanup-review-2026-10-25/MainActivity.before-ux.java`와 바이트 동일.
- SHA256 `39ec3c85cc6583d8918a92d6363bfa4e737ff46a8c4c1acffcd00d3135ded9a9`.
- HEAD 대비 누적 diff에는 이전 작업이 있으므로 '현재 UI 후속 변경 0줄'과 혼동하지 않는다. 이전 Cleanup 수명주기/결과/진입 위임도 함께 확인한다.
- 신규 판단/배치/해시/복구 로직이 Activity에 들어가지 않았는지 독립 확인한다.

## 3. 최종 제품 흐름
1. 위치앨범의 '원본 정리'로 진입, 원본/대상 후보 수집 -> SHA256 내용 비교 -> 발견 기록/날짜 메모/모음 참조 보존 검사.
2. 원본 폴더와 보존할 위치앨범을 구분해 표시. 기본 미선택, 폴더/전체 선택 지원. 보류 항목은 선택/실행 불가.
3. 선택 세트 전체 사전 재검증 및 이번 묶음 최종 검증 후에만 시스템 `createTrashRequest` 승인 요청.
4. 신규 묶음은 최대 **1,000개 원본 URI**. OS 승인만으로 성공 처리하지 않고 실제 원본 휴지통 상태/복사본 내용·식별자/Memory를 검증한다.
5. 승인 후 감사는 일반 WorkManager Worker. FGS 승격을 하지 않는다. 알림은 선택적이며 알림 부재가 완료/실패를 뜻하지 않는다.
6. 일부 완료 후 다음 묶음 미적용/취소는 PAUSED로 남기며 명시적인 계속 및 새 OS 승인 전까지 추가 이동하지 않는다.
7. 과거 300장 원장의 pending 범위는 변경하지 않는다. READY/PAUSED에서 계속할 때만 남은 범위를 1,000장으로 전환하고 sessionId를 회전한다.
8. 복사본 보존/연결 검증 실패는 다음 묶음을 막고 복구 요청 동선으로 진입. 영구 삭제 기능은 제공하지 않는다.

`CleanupFeatureGate=false`는 레거시 차단 게이트다. 실제 신규 실행은 `CleanupExecutionPolicy=true`이며 폰 테스트에 사용했다. 'FeatureGate가 false이므로 모든 실행이 꺼져 있다'고 판단하면 안 된다.

## 4. 우선 검토 지도
| 대상 | 현재 주요 진입점/책임 | 리뷰할 실패 경로 |
|---|---|---|
| CleanupExecutionController.java | start:42, prepare:68, OS 요청:109, resume:117, audit:127, watchAudit:156, 계속:189, 복구:209 | 화면 숨김/복귀/OS 결과 순서, stale callback, busy 잠금, 실제 전경 여부 |
| CleanupExecutionPreflight.java | prepare:12, verifyBatch:44, verifyDependencies:68 | 선택 전체/묶음 TOCTOU, 현재 revision, source/target 별칭, 메모/모음 보존 |
| CleanupAuditWorker.java | enqueue:15, doWork:24, 공유 잠금:29, 의존성 확인:35, 실패 재시도:54 | 프로세스 종료/시간 제한, 중복 enqueue, 중단 뒤 commit, 실패 후 재진입 |
| CleanupExecutionJournal.java | forContinuation:26, 읽기:43, batchSize 해석:69, 저장:80 | v1/300 호환, 손상 원장, fsync, ID 회전, 전체 원장 교체/동시성 |
| CleanupExecutionPlan.java / CleanupSessionAudit.java | 원본/보호 대상 키 및 immutable receipts, 배치/cursor, 실제 감사 상태 전이 | 300->1000, 600 완료 후 전환, 마지막 나머지, 중복 콜백, 혼합 휴지통 상태, 복구 경계 |
| CleanupMediaAccess.java / CleanupExecutionAudit.java / CleanupPairVerifier.java | MediaStore, requireOriginal, 바이트 해시·크기·식별자 | 원본 EXIF 접근/권한, MediaStore reset/alias, 동일 크기 변조, 대상 소실 |
| CleanupMemorySnapshotReader.java / MemoryDependencyGuard.java | 선택 전체 제외 가상 projection | stableMemoryId/dateKey/사진 섹션/모음 커버·기록 참조, 직접/간접·순환 참조 |
| CleanupController.java / CleanupHistoricalScanner.java / CleanupReviewCoordinator.java | 후보 수집/검토 세션/일괄 안전 후보 | 취소/프로세스 복귀, 과거 사진 대조, 보류/가능 분리, 대량 성능 |
| CleanupReviewModel.java / CleanupReviewSourceLoader.java / CleanupReviewDialog.java | 원본 메타데이터, 그룹/전체 선택 | 같은 경로 다른 volume, 알 수 없는 원본 위치, 선택 URI가 보호 대상이 되는 경우 |
| CleanupProgressScreen.java / CleanupExecutionScreen.java / CleanupUiStyle.java | 작은 진행 카드/하단 검토 패널/보라색 | 큰 글꼴/접힘/회전, 버튼 잘림, 닫기/중지 의미, 숫자 단위·단계 안내 |
| CleanupDependencyRetry.java / CleanupWorkNotification.java / CleanupRecoveryNotifier.java | revision만 제한 재검증, 알림 채널/진행/복구 | 실제 권한 오류 은폐 여부, 알림 금지, 오래된 작업이 새 알림을 지우는 경쟁 |

## 5. 기존 안티 리뷰와 반영 구분
- `REVIEW_Cleanup_Critical_Audit_2026-10-25.md` 원문과 `REVIEW_Cleanup_Critical_Audit_Response_2026-10-25.md` 대조.
- FGS 승격 의존 제거, 명시적 worker retry 상한 추가.
- 'revision 불안정'만 6회 검사/500ms*5 대기. 실제 권한/의존성 오류는 성공으로 완화하지 않는다.
- progress 표시 창 재사용, NotificationChannel 프로세스당 1회 생성.
- **폴링->관찰 전환은 미반영.** 현재는 자기 조회 완료 후 다음 조회를 예약하므로 스스로 무제한 큐를 쌓지는 않지만, 동기 DB 조회/동일 Executor 지연은 남아 있다.
- 기존 보고서의 '백그라운드면 무조건 FGS 크래시', '10분 실행 보장', '3번 sleep=750ms'는 사실 그대로 인용하지 말고 공식 계약/실제 코드로 검증한다.

## 6. 반드시 차단 여부를 판단할 남은 항목
다음은 아직 모두 해결됐다고 주장하지 않는 **Pre-release 판단 항목**이다. 단위 테스트 통과나 사용자 1회 성공으로 자동 통과시키지 않는다.

- **최초 사전 재확인:** Executor에서 실행하며 전체 plan 저장 전 프로세스 종료 시 선택/진행을 복구하지 못한다. 이동 전이라 파일은 그대로지만 UI의 작업 보관 문구와 제품 기대가 맞는지 검토.
- **일반 Worker 장시간 처리:** 현재 묶음뿐 아니라 이전 완료 복사본/전체 의존성도 읽는다. 큰 파일·느린 저장소에서 실행 제한/OS 중단이 반복될 때 완료하지 못하는지, 감사 체크포인트가 필요한지 검토. 실패/중단 후 원장은 보존되지만 파일별 검증 체크포인트는 없다.
- **enqueue/watch 순서:** enqueueUniqueWork 비동기 완료와 조회의 순서, 같은 이름 재사용 시 이전 finished WorkInfo를 먼저 읽는 가능성 및 UI/원장 영향 확인.
- **공유 잠금:** 감사 동안 LOCK 보유, 컨트롤러 읽기·메인 스레드의 요청 직전 원장 save가 같은 잠금을 얻는 경로에 ANR/교착이 가능한지 검토.
- **동시/연속 알림:** 서로 다른 작업이 공용 id7202/7203을 쓴다. 오래된 Worker의 finally가 현재 진행 알림을 지울 가능성 및 알림 문구와 저널 상태(CANCELED/PAUSED)의 일치 검토.
- **앱 재진입 자동 안내:** '나중에 계속' 후 보관된 동일 완료 수량의 화면이 재노출된다. 데이터 손실은 아니지만 반복 팝업 UX가 출시 차단인지 후속인지 구분.
- **대상 외부 변경:** OS 승인창 대기·감사 뒤·다음 승인 전에 갤러리에서 복사본/원본을 바꿨을 때 fail-closed 및 복구가 유지되는지 확인.

## 7. 검증 근거와 한계
- 현재 code56 소스로 `testDebugUnitTest testReleaseUnitTest assembleRelease` 재실행: **Debug 433 / Release 433, 실패0·오류0·스킵0**, Release 빌드 및 lintVitalRelease 통과.
- Java8 source/target deprecated 경고는 남아 있다. 경고0 빌드라고 하지 않는다.
- 300/301/3001 경계, 1,000+1,000+8, 600장 완료된 과거 plan의 명시적 전환/ID 회전, 중복 감사, 일부만 이동, 대상 변조·소실, 복구 경계, revision 제한 재시도, 소스 폴더/선택 모델 테스트 포함.
- API33 합성 미디어: Worker 완료·중복 enqueue·AUDIT_PENDING 저장 뒤 프로세스 강제 종료/재진입/HOME 전환 검증. 이는 당시 구현 결과이므로 최신 1,000장 실기기 전체 시험을 대체하지 않는다.
- Galaxy Fold(Android16) 사용자 관측: code53에서 1,500/3,508 처리 뒤 새 검사2,008 표시되는 복귀 이슈 확인. code54에서 PAUSED 유지 수정. code55(1,000장) 사용 후 사용자가 정리를 마치고 앱에 중복 사진이 없다고 보고. code56은 보라색 원복 설치 완료.
- 사용자 사진 전체를 사람이 열어본 결과, 모든 날짜 메모/모음의 수동 확인, 현재 버전의 모든 OS/프로세스 종료 조합을 전수 검증했다고 주장하지 않는다.
- '보류'는 정리 가능0과 별개다. 266장 보류 관측 기록이 있으므로 '모든 중복이 삭제됐다'로 일반화하지 않는다.
- 에이전트는 사용자 휴지통 비우기/영구 삭제를 수행하지 않았다.

## 8. 설치/출시 파일
- `photoplace-1.3.20-code56-purple-test.aab`, SHA256 `b570ad39aec9053a579c28d3c2192c0747c37643818c9da3069c2b60de03b6db`.
- `photoplace-1.3.20-code56-purple-test-play-signed.apk`, SHA256 `32e7fc86268b09f592dfb31a413c6ddd64fe217ed4956b52316106762f932fd0`.
- Play signer `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d`.
- 폰 R5KL503VHQR에 code56 업데이트 설치 확인, 기존 최초 설치 시각 유지. 로컬 app-release.apk는 upload key 서명이므로 폰 Play 설치본 업데이트용이 아니다.
- internal `1.3.20 Purple UI Test` code56 **draft**. production code51 **halted**, code39 **completed**. 이번 리뷰 준비에서 업로드/제출/출시 재개 안 함.

## 결과물 요구
1. P0/P1 우선 판정과 재현 가능한 발견 사항을 먼저 적는다.
2. 위 남은 항목별 Blocker / Pre-release / Follow-up 분류와 이유를 적는다.
3. 승인 범위를 코드/기기 시험/출시 중 무엇인지 명시한다. 이전 code51의 APPROVED FOR PRODUCTION을 현재 code56의 승인으로 재사용하지 않는다.
4. 차단 결함이 있으면 HOLD, 근거 없는 '완벽', '모든 기기 통과'는 쓰지 않는다.
