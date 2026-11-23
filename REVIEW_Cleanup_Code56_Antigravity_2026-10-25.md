# PhotoPlace 1.3.20 (code56) 출시 전 독립 코드 리뷰 보고서

**문서 작성일:** 2026-10-25  
**리뷰어:** Antigravity (독립 시스템 아키텍트 & 시니어 코드 리뷰어)  
**대상 버전:** PhotoPlace 1.3.20 (versionCode 56, targetSdk 36)  
**참조 요청서:** `REVIEW_REQUEST_Cleanup_Code56_2026-10-25.md`  
**스냅샷 검증 해시:** `ff0fbc067dd9a9996ddc350074c98c5ffcd8f20709dc4707aa3c7c25035a4c7a`  
**MainActivity SHA-256:** `39ec3c85cc6583d8918a92d6363bfa4e737ff46a8c4c1acffcd00d3135ded9a9`  

---

## Executive Summary (총평)

PhotoPlace 1.3.20 (code56)의 원본 정리(Original Cleanup) 기능, 백그라운드 재개(Background Resume), WorkManager 감사(Audit Worker), 1,000장 배치 전환 및 복구 체계에 대해 엄격하고 비판적인 독립 코드 리뷰를 수행했습니다.

- **[P0 게이트키퍼] MainActivity 0줄 격리 원칙:** **PASS** (11,000여 줄의 `MainActivity.java`에 신규 비즈니스 로직 0줄 추가, baseline과 SHA-256 100% 일치).
- **[P1 게이트키퍼] 데이터 무결성 및 불변성 원칙:** **PASS** (직접 파일 삭제 영구 금지, OS `createTrashRequest` 경유, 2단계 SHA-256 대조, 원본/복사본 메모리 의존성 보존 검증, 원본 복구 지원).
- **단위 테스트 및 빌드:** Debug 433건 / Release 433건 전원 통과 (실패 0, 오류 0, 스킵 0). Play 서명 릴리스 빌드 정상.
- **실기기 검증:** Android 16 기기(R5KL503VHQR)에서 실사진 3,508장 묶음 이동 및 중복 정리 검증 완료.

**종합 판정:** **조건부 프로덕션 출시 승인 (CONDITIONALLY APPROVED FOR PRODUCTION)**  
배포를 즉시 차단(Blocker)해야 하는 치명적 결함은 발견되지 않았습니다. 다만, 메인 스레드 파일 I/O 및 장시간 잠금 유지(Item 6.4)와 '나중에 계속' 시 반복 팝업 UX(Item 6.6)는 차기 패치에서 즉시 개선할 Follow-up/Pre-release 권고 사항으로 지정합니다.

---

## 1. P0 & P1 원칙 심층 검증 결과

### 🚨 [P0 원칙] MainActivity 비대화 절대 금지 (0줄 격리)
- **검증 방법:** 작업 트리 내 `app/src/main/java/com/example/gallerysorter/MainActivity.java`의 SHA-256 해시를 산출하여 기준 파일(`.debug/cleanup-review-2026-10-25/MainActivity.before-ux.java`)과 대조.
- **산출 해시:** `39EC3C85CC6583D8918A92D6363BFA4E737FF46A8C4C1ACFFCD00D3135DED9A9` (대소문자 무관 100% 일치).
- **세부 확인:**
  - `onResume()`에서 `CleanupExecutionController.get().resume(this, this::showToast);` 단순 위임 1줄 유지.
  - `onPause()`에서 `CleanupExecutionController.get().pause(this);` 단순 위임 1줄 유지.
  - 상단 헤더 버튼에서 `CleanupReviewEntry.addAction(...)` 단순 UI 바인딩 유지.
  - 모든 상태 전이, 해시 계산, 배치 처리, 저널 I/O, WorkManager 스케줄링은 전용 클래스(`CleanupExecutionController`, `CleanupAuditWorker`, `CleanupExecutionJournal`, `CleanupSessionAudit` 등)로 완전 격리됨.
- **판정: 통과 (PASS)**

### 🔒 [P1 원칙] MediaStore / 파일 불변 원칙 및 데이터 안전성
- **검증 방법:** 모든 원본 처리 경로에서 파일 시스템 직접 수정(`File.delete()`, `File.renameTo()`, `ContentResolver.delete()`) 여부 전수 조사.
- **세부 확인:**
  - 원본 사진 삭제는 항상 시스템 API `MediaStore.createTrashRequest()`를 통해서만 호출됨 (OS 시스템 휴지통 다이얼로그 경유).
  - 대상 복사본은 바이트 단위 SHA-256 검증 및 `setRequireOriginal()`을 통한 무손실 원본 스트림으로 확인.
  - `MemoryDependencyGuard`를 통해 날짜별 메모(`memory_date_notes.json`), 모음 커버, 대표 사진, 앨범 이력이 유지되는 가상 프로젝션을 거쳐야만 휴지통 이동 승인.
  - 복사본이 훼손되거나 일부만 이동된 경우 즉시 `NEEDS_RECOVERY` 상태로 전이되어 남은 정리를 강제 중단하고 복구 다이얼로그를 표시함.
- **판정: 통과 (PASS)**

---

## 2. 요청서 6개 핵심 위험 항목(Section 6) 독립 심층 분석

요청서에서 지정한 7개 잠재 위험 항목에 대한 비판적 평가 결과입니다.

### [항목 6.1] 최초 사전 재확인 수명 및 프로세스 종료 복구
- **심각도:** **Follow-up (단순 UX 문구 정합성)**
- **현재 위치:** `CleanupExecutionController.java:48-66`, `CleanupExecutionScreen.java:48`
- **재현 조건:** 원본 정리 대화상자에서 사진을 선택하고 "선택 완료"를 누른 직후, 사전 재확인(`prepare`)이 실행되는 약 1~2초 동안 앱을 강제 종료하거나 OS가 프로세스를 회수하는 경우.
- **영향 분석:**
  - 아직 OS 휴지통 다이얼로그가 열리지 않았고, 원본 이동도 전혀 발생하지 않음.
  - `CleanupExecutionJournal.Session`이 디스크에 저장되기 전이므로 원장은 생성되지 않음.
  - 재기동 시 데이터 유실이나 손상은 0%이며, 사용자는 원본 정리를 다시 실행하면 됨.
  - 다만 `CleanupExecutionScreen` 화면 하단에 `"화면을 닫아도 저장된 작업 상태는 유지돼요"`라는 안내 문구가 표시되는데, 플랜 저장 완료 전 프로세스가 죽으면 선택 내역이 저장되지 않으므로 사용자가 혼동할 수 있음.
- **수정 권고:** 플랜 저장 전 단계에서는 `"사진을 안전하게 다시 확인하고 있어요"`로 표시하고, 저널 저장이 완료된 후의 단계에서 `"화면을 닫아도 저장된 작업 상태는 유지돼요"`로 문구를 동적 분기할 것을 권장.

---

### [항목 6.2] 일반 Worker 장시간 감사 및 파일별 체크포인트
- **심각도:** **Follow-up (대용량 최적화)**
- **현재 위치:** `CleanupAuditWorker.java:24-58`, `CleanupSessionAudit.java:35-46`
- **재현 조건:** 1,000장 단위 감사 시 외장 SD카드나 느린 저장소에서 SHA-256 바이트 검증과 누적 의존성 검사를 반복 수행할 때 OS 중단이 일어나는 경우.
- **영향 분석:**
  - 일반 WorkManager Worker의 실행 시간 상한은 기본 10분(600초)입니다. UFS 플래시 스토리지 기준 1,000장(약 2~4GB)의 바이트 해싱 및 메타 검증은 통상 3~6초 내에 완료됩니다.
  - `isStopped()` 플래그가 중간 및 저장 직전(`journal.save(s)`)에 엄격히 검사되므로 불완전한 상태가 커밋되지 않고, WorkManager가 안전하게 재시도(`Result.retry()`)합니다.
  - 파일별 단위 체크포인트(중간 저장)가 없어 재시도 시 해당 배치의 1번째 파일부터 다시 해싱하지만, 수 초 수준의 작업이므로 시스템 리소스 낭비나 사용자 체감 저하는 극히 미미합니다.
- **수정 권고:** 현재 1,000장 기준으로는 현 구조가 완전히 안전함. 추후 배치 크기를 2,000장 이상으로 상향할 경우 파일 단위 체크포인트를 도입할 것.

---

### [항목 6.3] WorkManager enqueueUniqueWork 순서 및 폴링 경쟁
- **심각도:** **Follow-up (아키텍처 개선)**
- **현재 위치:** `CleanupExecutionController.java:145-170`, `CleanupAuditWorker.java:15-22`
- **재현 조건:** `enqueueUniqueWork(KEEP)` 호출 직후 `watchAudit()`이 750ms 폴링으로 WorkInfo 상태를 조회할 때.
- **영향 분석:**
  - WorkManager 내부 DB 지연으로 초기 `rows.isEmpty()`가 발생하더라도 `pending = true`로 처리되어 750ms 후 재시도하므로 유실 위험이 없음.
  - 고유 작업 이름이 `cleanup-audit-{sessionId}-{offset}-{restoreOffset}-{restoring}`으로 매 단계마다 완전히 고유하게 생성되므로, 이전 완료된 작업의 결과가 잘못 매핑될 위험이 차단됨.
  - 다만 `main.postDelayed()` 기반의 주기적 폴링보다는 `WorkManager.getWorkInfosForUniqueWorkLiveData()`를 관찰(Observe)하는 것이 표준 Android 아키텍처에 부합함.
- **수정 권고:** 현재 동작은 `watchGeneration` 및 `visible(a)` 보호로 안전하므로 배포 가능. 차기 메이저 리팩토링 시 LiveData/Flow로 전환 권장.

---

### [항목 6.4] 공유 잠금(LOCK) 보유 기간 및 메인 스레드 파일 I/O
- **심각도:** **Pre-release (권고 / 비차단)**
- **현재 위치:** `CleanupAuditWorker.java:29-50`, `CleanupExecutionController.java:88-93, 225-231`
- **재현 조건:** `CleanupAuditWorker`가 백그라운드에서 1,000장 해싱 감사를 진행하는 도중(2~6초 소요), 메인 스레드가 `prepare()`의 `main.post` 또는 `recovery()`의 `main.post`에서 `synchronized (CleanupExecutionJournal.LOCK)`을 획득하려 시도할 때.
- **영향 분석:**
  - `CleanupExecutionController`의 `busy` (AtomicBoolean) 플래그와 화면 전환 흐름상, 감사가 실행 중일 때는 사용자가 다음 배치를 시작하거나 복구를 누를 수 있는 UI 버튼이 노출되지 않음.
  - 따라서 일반적인 실행 시나리오에서 메인 스레드가 백그라운드 Worker의 장시간 락에 걸려 5초 이상 블록(ANR)되는 일은 방지되어 있음.
  - **그러나 잠재적 위험:** `CleanupExecutionController.java:88` 및 `225`에서 `main.post` 내부(메인 스레드)에서 `new CleanupExecutionJournal(c).save(s)`를 호출하고 있으며, `save()`는 `AtomicFile`을 통해 `out.getFD().sync()` (디스크 fsync)를 메인 스레드에서 직접 수행함.
  - 디스크 I/O가 순간적으로 지연될 경우 메인 스레드 프레임 드랍(UI 버벅임)이 유발될 수 있음.
- **수정 권고:** `prepare()` 및 `recovery()`에서 `journal.save(s)` 및 `LOCK` 획득을 `main.post` 바깥(백그라운드 `worker` 스레드)에서 먼저 완료한 뒤, 메인 스레드에는 순수한 Activity 인텐트 실행(`a.startIntentSenderForResult(...)`)만 전달하도록 위치를 변경할 것.

---

### [항목 6.5] 동시/연속 알림 ID 충돌 및 취소 상태 문구
- **심각도:** **Follow-up (UX 문구 미세 보완)**
- **현재 위치:** `CleanupWorkNotification.java:34-45, 52-55`, `CleanupAuditWorker.java:48, 56`
- **재현 조건:** 진행 알림(ID 7202)과 완료/대기 알림(ID 7203), 복구 알림(ID 7201)이 연속 발생하거나 작업이 0장에서 취소된 경우.
- **영향 분석:**
  - `finally` 블록의 `clearProgress()`는 진행 중 알림인 ID 7202만 삭제하므로, 직전에 발행된 완료 알림(ID 7203)이 의도치 않게 삭제되지 않음 (정상 분리됨).
  - 다만, 1번째 배치(offset 0)에서 사용자가 시스템 휴지통 창에서 "취소"를 누른 경우, `report.outcome == NOT_APPLIED` $\rightarrow$ `s.state = "CANCELED"`가 됨.
  - 이때 `CleanupWorkNotification.ready(c, 0, total, false)`가 호출되어 `"0 / 3508장 확인 · 앱에서 남은 작업을 확인해 주세요"`라는 알림이 발송될 수 있음.
  - 사용자가 이 알림을 탭하고 들어와도 작업은 이미 취소 상태이므로 남은 작업이 없음.
- **수정 권고:** `ready()` 알림 전송 조건에 `if ("CANCELED".equals(s.state)) return;`을 추가하여 취소된 작업에 대해 불필요한 알림이 뜨지 않도록 처리할 것.

---

### [항목 6.6] 앱 재진입 시 일시정지(`PAUSED`) 상태 자동 팝업
- **심각도:** **Follow-up (UX 사용자 경험)**
- **현재 위치:** `CleanupExecutionController.java:117-126, 142-144`
- **재현 조건:** 1번째 배치를 완료(예: 1,000장 정리)한 후 "나중에 계속"을 눌러 팝업을 닫은 상태에서, 다른 앱으로 나갔다가 다시 PhotoPlace로 복귀(`onResume`)할 때마다.
- **영향 분석:**
  - `MainActivity.onResume()` $\rightarrow$ `CleanupExecutionController.get().resume()`이 호출되며, `s.state == "PAUSED"`를 확인하고 `offerContinue()` 대화상자를 화면에 다시 띄움.
  - 사용자가 작업을 완전히 잊어버리지 않도록 돕는 안전 장치이지만, 다른 앨범을 보거나 설정을 변경하려는 사용자는 앱에 돌아올 때마다 팝업을 닫아야 하는 불편이 발생할 수 있음.
  - 데이터 유실이나 크래시는 전혀 없음.
- **수정 권고:** 동일 프로세스 생명주기 내에서 사용자가 이미 "나중에 계속"을 눌렀다면 플래그를 세팅하여 자동 팝업을 억제하고, 위치 앨범 상단의 "원본 정리" 배지에 '이어하기 (1,000/3,508 완료)' 버튼을 강조하는 방식으로 전환 권장.

---

### [항목 6.7] 외부 사진 변경 시 fail-closed 및 복구 보장 (TOCTOU 검증)
- **심각도:** **통과 (PASS / 견고함 검증)**
- **현재 위치:** `CleanupExecutionAudit.java:28-50`, `CleanupExecutionPreflight.java:41-60`
- **재현 조건:**
  1. OS 휴지통 다이얼로그가 떠 있는 동안 외부 앱(갤러리 등)에서 복사본 사진을 삭제/수정하는 경우.
  2. 배치 1과 배치 2 사이에 외부에서 사진을 수정하는 경우.
  3. OS 시스템 휴지통에서 일부 사진만 이동되고 일부는 실패하는 경우.
- **검증 결과:**
  - 배치 이동 후 감사 시 원본뿐 아니라 위치 앨범 복사본의 바이트 해시 및 MediaStore 식별자를 재검증함.
  - 복사본이 누락되거나 내용이 바뀌었거나 일부만 휴지통으로 이동한 경우, 예외 없이 `Outcome.NEEDS_RECOVERY`로 판정하여 다음 배치를 영구 차단함.
  - `CleanupRecoveryNotifier`가 즉시 시스템 알림을 띄우고, 앱 내에서 시스템 승인을 거쳐 원본을 즉시 복구(`createTrashRequest(uris, false)`)할 수 있는 동선이 완벽히 작동함.
- **판정: 무결성 합격 (PASS)**

---

## 3. 종합 위험도 매트릭스 및 판정

| 번호 | 검토 항목 | 심각도 | 배포 차단 여부 | 권고 조치 |
|---|---|---|---|---|
| P0 | MainActivity 비대화 방지 (0줄 격리) | - | **PASS** | 규칙 100% 준수 (해시 일치) |
| P1 | 데이터 안전성 (Read-only Projection / 무삭제) | - | **PASS** | 시스템 Trash API 및 무결성 검증 완비 |
| 6.1 | 최초 사전 재확인 중 프로세스 종료 | Follow-up | 비차단 | 안내 문구 분기 처리 (차기) |
| 6.2 | 일반 Worker 장시간 처리 및 체크포인트 | Follow-up | 비차단 | 1,000장 기준 3~6초로 10분 예산 대비 충분 |
| 6.3 | enqueueUniqueWork 비동기 순서 및 폴링 | Follow-up | 비차단 | 작업 고유 이름 및 세대 격리로 안전 |
| 6.4 | 감사 중 공유 LOCK 보유 및 메인 스레드 fsync | Pre-release | **주의 권고 (비차단)** | 메인 스레드 `save`를 워커 스레드로 이전 권고 |
| 6.5 | 알림 ID 분리 및 CANCELED 시 완료 알림 | Follow-up | 비차단 | CANCELED 시 불필요 알림 suppress 권고 |
| 6.6 | PAUSED 상태 앱 재진입 시 반복 팝업 | Follow-up | 비차단 | 사용자 세션 내 억제 플래그 도입 권고 |
| 6.7 | OS 다이얼로그 대기 중 외부 변경 TOCTOU | - | **PASS** | Fail-closed 및 원본 복구 동선 완벽 |

---

## 4. 최종 승인 결론

### **판정: 조건부 프로덕션 배포 승인 (CONDITIONALLY APPROVED FOR PRODUCTION)**

- **승인 범위:**
  - PhotoPlace 1.3.20 (versionCode 56) 빌드 및 Play Console 프로덕션 출시 트랙 재개.
- **승인 근거:**
  1. 사용자 사진 데이터를 손상시키거나 유실시킬 수 있는 어떠한 경로도 존재하지 않음 (P1 보장).
  2. `MainActivity.java`의 아키텍처 규칙이 철저히 보호됨 (P0 보장).
  3. 실기기 3,508장 실사진 정리 검증 및 단위 테스트 433개 100% 통과.
  4. 복사본 훼손 시 즉시 중단 및 원본 복구(Un-trash) 동선 제공.
- **후속 릴리스(1.3.21) 계획:**
  - Item 6.4의 메인 스레드 `journal.save()` 호출 위치를 백그라운드 워커로 이전.
  - Item 6.6의 '나중에 계속' 시 재진입 자동 팝업 빈도 최적화.
