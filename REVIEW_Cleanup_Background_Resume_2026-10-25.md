# 원본 정리 백그라운드·복귀 후속 검토

## 현재 판정
**HOLD FOR DEVICE VALIDATION. 프로덕션 재개 승인 아님.**

사용자는 원본 이동 자체는 모두 완료됐다고 확인했다. 중간 오류 발생 시점은 정확하지 않으므로 OS 승인 직후 장애로 단정하지 않는다. 제공된 스크린샷으로 확인된 문제는 활성 저널의 정상 복귀가 오류 토스트로 표시되는 점, 실행 단계에 원형 ProgressDialog/구형 안내창이 남은 점이다.

## 변경과 책임
- CleanupController: 활성 작업이 있으면 오류를 던지지 않고 기존 작업 확인에 위임.
- CleanupExecutionController: 화면 복귀/이탈 라우팅, 전경에서만 시스템 승인 요청. 승인 후 감사는 WorkManager에 위임. 화면 이탈 시 검사 UI를 닫고 다음 복귀 시 원장에서 재구성.
- CleanupAuditWorker: session ID + 완료 offset + restore offset + restoring을 입력으로 가지는 고유 작업. KEEP으로 같은 묶음의 중복 실행을 합침. 이전 작업 입력은 새 세션/다음 묶음을 변경할 수 없음.
- CleanupSessionAudit: 복사본 SHA-256/식별자, 실제 원본 IS_TRASHED, 이전 묶음, Memory 의존성 검사 후에만 offset 증가. READY/PAUSED/DONE/CANCELED 재검증은 offset을 증가시키지 않는 멱등 전이.
- CleanupExecutionJournal: 기존 version 1과 호환되는 sessionId 추가, 모든 인스턴스가 공유하는 잠금으로 AtomicFile 읽기/쓰기를 직렬화. 쓰기 fsync 유지.
- CleanupExecutionScreen: 실행 재검증/복사본 감사/계속/복구/완료를 전체 높이 화면 및 가로 진행률로 통일. 사진 검사 화면(CleanupProgressScreen)은 유지.
- CleanupReviewEntry: 진입 시 검색 포커스 및 키보드 해제.

## 안전 경계
- MainActivity는 이번 후속 작업 직전/이전 UI baseline과 동일. 기존 HEAD 누적 diff와 구분해야 한다.
- 휴지통/복구는 여전히 사용자가 고른 원본 URI에 대해서만 MediaStore 시스템 승인 UI를 요청한다. Worker는 어떤 미디어도 이동/복구/삭제하지 않는다.
- 배치 크기 300 유지. 이미 저장된 저널의 cursor와 묶음 경계를 바꾸지 않음. 2000장 증대는 별도 기기 검증 전 보류.
- 화면을 떠난 동안 검증을 마쳐도 자동으로 OS 승인창을 열지 않음. READY/PAUSED 원장을 남기고 앱에서 다시 확인한 후 진행.
- '나중에 계속'은 CANCELED로 바꾸지 않고 완료 수량/남은 계획을 유지.
- 최초 선택 해시 재검증은 아직 Executor 기반. Activity 파괴/이탈만으로 취소하지 않지만, 계획 저장 전 프로세스 종료까지 보장하는 WorkManager 작업은 아님. 그 경우 사진 변경 없이 재검토해야 함.
- WorkManager도 시스템 스케줄/할당량/강제 종료에 영향을 받는다. 무조건 즉시 또는 무제한 백그라운드 실행을 보장하지 않는다. 지연 시 저널을 보존해 다음 실행에 재검증한다.

## 검증 기록
- Debug/Release 각 425 tests, 실패 0 (신규 상태 전이 테스트 8건 포함).
- 신규 테스트: 301장 첫 배치 중복 감사/최종 1장 재개, 다음 묶음 취소 시 이전 완료 수량 유지, 부분 이동 차단, 이전 복사본 소실, Memory 가드 실패, 300+1 복구 재개, 외부에서 원본 복구 후 해소.
- app assembleDebug 성공. 기본 Java 8 deprecated 경고는 기존과 동일.
- 별도 테스트 앱 CleanupBackgroundProbe 추가: 스스로 만든 합성 원본/복사본만 사용하여 WorkManager 실제 실행·중복 요청·복사본 보존을 검증하도록 준비.
- 물리 폰: R5KL503VHQR 연결 및 com.photoplace.app code52/1.3.16 확인. CleanupExecution 오류 로그는 확보되지 않음. 조회 결과의 다른 앱 과거 크래시는 이 이슈의 증거에서 제외.
- 사용자 사진 복원/이동, 폰 앱 재설치, Google Drive/Play 새 업로드는 수행하지 않음. Drive code52는 이번 후속 변경 전 파일이다.

## 남은 게이트
1. Android WorkManager 실제 런타임/알림 및 전체 진행 화면 시각 검증.
2. 합성 301장으로 홈/화면 잠금/프로세스 종료/복귀, 승인 취소, 보존 감사 실패, 복구 재개 확인.
3. 사용자 실기기 소량 확인 후 새 테스트 APK와 생산 배포를 별도로 판단.

## 공식 참고
- https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running
- https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/observe
- https://developer.android.com/reference/android/provider/MediaStore#createTrashRequest(android.content.ContentResolver,%20java.util.Collection%3Candroid.net.Uri%3E,%20boolean)


## 런타임 추가 결과 (Android 13 전용 에뮬레이터)
- READ_MEDIA_VIDEO 미부여 첫 실행은 MEMORY_PROJECTION_CHANGED / NEEDS_RECOVERY로 차단됨. 복사본/원본 바이트 감사는 TRASHED였으나 Memory 권한 가드가 통과하지 않아 cursor 0 유지. 테스트 권한을 정상 부여한 뒤 전체 경로 재시험.
- CleanupBackgroundProbe PASS: 합성 원본/복사본 1쌍만 사용. WorkManager가 사후 검증 후 DONE / offset 1을 원자적으로 기록. 복사본 실존/해시 보존, 같은 작업 반복 enqueue 시 offset 1 유지 확인.
- stage-resume PASS: OS 승인 후 enqueue 전 종료에 해당하는 AUDIT_PENDING 원장을 스테이징. 테스트 앱 force-stop -> Activity 재실행 -> 즉시 HOME 전환 후 Worker가 DONE / offset 1을 저장. 재시작된 세션 ID 유지 확인.
- 전체 진행 화면 360dp, font_scale 1.3 XML 확인: 원본 사진 정리, 300 / 808장, 다음 묶음 안내, 보존 안내, 남은 사진 계속 정리, 나중에 계속이 화면 안의 bounds에 존재. 실제 미디어를 이동하지 않는 UI 프리뷰임.
- API36의 새 전용 에뮬레이터 부팅이 완료되지 않아 API33 새 AVD에서 런타임 검증. Android16 실기기 결과로 확대 해석하지 않음.
- 스크린샷 캡처는 별도 에뮬레이터에서 지연되어 픽셀 단위 시각 확인을 완료했다고 주장하지 않음. API36/실기기/301장 OS 승인 흐름과 최종 시각 검증 게이트 유지.

- 임시 AVD 종료 완료. 생성한 PhotoPlaceCleanup_API33/BG2/20261025의 폴더 정리는 자동 승인 검토가 파일 삭제 명령을 차단하여 미실행(구체 사유 미제공). 경로는 C:\Users\mismi\.android\avd 아래이며 기존 Medium_Phone AVD는 변경하지 않음.
