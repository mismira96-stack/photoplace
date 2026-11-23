# 코드 리뷰 비판적 정밀 검토 보고서: WorkManager 감사 및 복귀 실행 경로의 엣지 케이스 분석

- **작성 일자:** 2026-10-25
- **검토자:** Antigravity (Google DeepMind Advanced Agentic Coding)
- **리뷰 대상:** `CleanupAuditWorker`, `CleanupExecutionController`, `CleanupSessionAudit`, `CleanupExecutionScreen`, `CleanupWorkNotification`
- **검토 관점:** 무비판적 수용(Rubber-stamping)을 배제하고, Android OS 런타임 제약, 백그라운드 수명주기, 멀티스레딩 동시성, MediaProvider 지연 인덱싱 등 **실제 프로덕션 장애를 유발할 수 있는 잠재적 취약점**을 비판적으로 분석.

---

## 1. 종합 평가 요약

전체적인 구조(원자적 저널링, 300장 단위 분할, 멱등적 상태 전이)는 우수하지만, **Android 12~15 백그라운드 FGS 제약, 단일 스레드 폴링 부하, MediaProvider 갱신 지연에 따른 복구 오판 위험** 등 실제 사용자의 실기기 환경에서 크래시나 무한 재시도를 일으킬 수 있는 치명적인 엣지 케이스들이 존재합니다.

---

## 2. 비판적 정밀 검토 발견 사항 (Critical Findings)

### 🔴 [Critical 1] Android 12+ 백그라운드 시작 시 `ForegroundServiceStartNotAllowedException` 크래시 및 재시도 루프 위험
- **발생 위치:** [`CleanupAuditWorker.java#L30-L32`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupAuditWorker.java#L30-L32)
  ```java
  setForegroundAsync(new ForegroundInfo(7202,
          CleanupWorkNotification.progress(c, "복사본 보존을 확인하고 있어요", 0, 0),
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)).get();
  ```
- **문제점:**
  1. 사용자가 OS 휴지통 승인 팝업에서 "허용"을 누른 직후 홈 버튼을 누르거나 전원 버튼을 눌러 화면을 끄면, 앱 프로세스는 즉시 **백그라운드 상태**가 됩니다.
  2. Android 12+ (API 31+) 정책상 백그라운드 상태에서 Foreground Service를 시작하려고 하면 `ForegroundServiceStartNotAllowedException`이 발생합니다.
  3. 현재 코드는 L66-69에서 `catch (Exception failed) { return Result.retry(); }`로 처리하고 있어, **백그라운드에서 예외가 발생하면 감사가 전혀 진행되지 못하고 무한 재시도 큐(Retry Loop)에 갇히게 됩니다.**
  4. 또한 Android 14+에서는 `dataSync` 타입의 FGS에 24시간당 6시간의 런타임 쿼터가 적용되며 Google Play 정책상 엄격한 사유 소명을 요구합니다.
- **수정 제안:**
  - 300장 감사는 수 초 내에 끝나므로 굳이 FGS로 강제 승격할 필요가 없습니다. WorkManager는 일반 백그라운드 상태에서도 최대 10분간의 실행을 기본 보장합니다.
  - 만약 FGS를 유지하더라도 `setForegroundAsync(...).get()`을 `try-catch`로 감싸서, 백그라운드 제약으로 승격이 실패하더라도 감사 자체는 일반 백그라운드 Worker로 계속 완수되도록 방어해야 합니다:
    ```java
    try {
        setForegroundAsync(...).get();
    } catch (Exception fgsError) {
        // 백그라운드 상태에서 FGS 승격이 거부되어도 감사는 일반 Worker로 계속 진행
    }
    ```

---

### 🟡 [Major 2] 단일 스레드 Executor 상의 750ms Polling (`watchAudit`) 병목 및 IPC 낭비
- **발생 위치:** [`CleanupExecutionController.java#L152-L166`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupExecutionController.java#L152-L166)
  ```java
  private void watchAudit(Activity a, Consumer<String> notice, String name, int generation) {
      ...
      worker.execute(() -> {
          List<WorkInfo> rows = WorkManager.getInstance(...).getWorkInfosForUniqueWork(name).get();
          ...
          main.postDelayed(() -> watchAudit(...), 750);
      });
  }
  ```
- **문제점:**
  1. `worker`는 `Executors.newSingleThreadExecutor()`로 선언된 **단 1개의 작업 스레드**입니다.
  2. 이 단일 스레드가 사전 검증(`start`), 배치 준비(`prepare`), 사후 감사(`audit`)를 모두 처리하는데, 포그라운드에서는 750ms마다 `getWorkInfosForUniqueWork().get()` 동기 Room SQLite 조회를 계속해서 큐에 밀어 넣습니다.
  3. 만약 단일 스레드가 대용량 파일 해시 검사 등으로 2~3초간 바쁘면, 큐에 쌓인 `watchAudit` 작업들이 지연되어 UI 진행률 업데이트가 멈칫거리게 됩니다.
  4. WorkManager는 이미 관찰용 `getWorkInfosForUniqueWorkLiveData(name)`를 제공하고 있는데, 구식 폴링 루프를 사용하는 것은 비효율적입니다.
- **수정 제안:**
  - WorkManager의 `LiveData`를 관찰하거나, 단일 비즈니스 스레드(`worker`)를 폴링용으로 점유하지 않도록 분리해야 합니다.

---

### 🟡 [Major 3] 300장 이동 후 MediaProvider 인덱싱 지연에 따른 `MEMORY_PROJECTION_CHANGED` 오판 위험
- **발생 위치:** [`CleanupAuditWorker.java#L40-L45`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupAuditWorker.java#L40-L45), [`CleanupSessionAudit.java#L40-L41`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupSessionAudit.java#L40-L41)
  ```java
  for (int attempt = 0; attempt < 3; attempt++) {
      if (isStopped()) throw new InterruptedException();
      try { CleanupExecutionPreflight.verifyDependencies(c, proofs); return; }
      catch (Exception changed) { last = changed; }
      if (attempt < 2) Thread.sleep(250);
  }
  ```
- **문제점:**
  1. 300장의 사진이 휴지통으로 이동되면 Android OS의 `MediaProvider`는 백그라운드에서 SQLite 트리거를 돌리며 썸네일과 인덱스를 갱신합니다. 이 동안 `generation_modified`나 전역 MediaStore 버전이 수 초간 계속 바뀝니다.
  2. 현재 재시도 로직은 `250ms * 3회 = 최대 750ms`만 대기합니다.
  3. 저사양 폰이나 사진이 수만 장인 환경에서 MediaProvider의 동기화가 1초 이상 걸리면, 실제로는 복사본과 원본이 완벽하게 안전함에도 `verifyDependencies`가 `reader.isCurrent()` 실패를 던져 **`NEEDS_RECOVERY` (복구 필요 경고)로 오판**될 수 있습니다.
- **수정 제안:**
  - 재시도 타임아웃을 최대 3~4초(예: 500ms * 6회 등)로 여유 있게 늘려, 대용량 I/O 직후의 MediaProvider 인덱싱 안정화 시간을 충분히 확보해야 합니다.

---

### 🟢 [Moderate 4] `progress()` 호출 시마다 일어나는 다이얼로그 파괴 및 재성성 (Flicker & Token Risk)
- **발생 위치:** [`CleanupExecutionController.java#L249-L258`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupExecutionController.java#L249-L258)
  ```java
  private CleanupExecutionScreen progress(Activity a, String message) {
      ...
      dismiss(currentScreen);
      CleanupExecutionScreen p = new CleanupExecutionScreen(a, ...);
      p.setMessage(message);
      p.show();
      currentScreen = p;
  }
  ```
- **문제점:**
  1. `progress()`가 호출될 때마다 기존 `currentScreen`을 `dismiss()`하고 새로운 `CleanupExecutionScreen` 인스턴스를 `new`하여 띄웁니다.
  2. 이미 다이얼로그가 화면에 떠 있는 상태에서 메시지만 바뀔 때도 전체 윈도우를 닫고 다시 띄우므로, 화면 깜빡임(Flicker)이 발생하고 화면 전환 타이밍에 따라 `WindowManager$BadTokenException` 위험이 증가합니다.
- **수정 제안:**
  - `if (currentScreen != null && currentScreen.isShowing()) { currentScreen.setMessage(message); return currentScreen; }` 형태로 기존 다이얼로그의 텍스트만 업데이트하도록 리팩토링하는 것이 훨씬 부드럽고 안전합니다.

---

### 🟢 [Minor 5] `CleanupWorkNotification.progress()`의 반복적 NotificationChannel 생성 IPC
- **발생 위치:** [`CleanupWorkNotification.java#L13-L14`](file:///C:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/CleanupWorkNotification.java#L13-L14)
  ```java
  manager.createNotificationChannel(new NotificationChannel("cleanup_progress", "원본 정리 진행", NotificationManager.IMPORTANCE_LOW));
  ```
- **문제점:**
  - 진행률이 업데이트될 때마다(매 750ms) 시스템 서버(`NotificationManagerService`)로 `createNotificationChannel` IPC 바인더 호출이 일어납니다.
- **수정 제안:**
  - `private static volatile boolean channelCreated;` 플래그를 두어 프로세스당 최초 1회만 채널을 생성하도록 최적화해야 합니다.

---

## 3. 결론 및 제안 요약

이번 변경의 방향성(WorkManager 분리, 멱등 세션, 풀스크린 UX)은 맞지만, **실제 안드로이드 OS의 가혹한 런타임 환경(백그라운드 FGS 금지, 단일 스레드 병목, MediaProvider I/O 지연)**에 대한 방어벽이 얇습니다.

실기기(Galaxy Fold / Android 16) 검증 전에 위 **1번(FGS 백그라운드 예외 방어)** 및 **3번(MediaProvider 동기화 타임아웃 완화)**은 반드시 보강해야 실기기 테스트 중 억울한 실패(`NEEDS_RECOVERY` 또는 크래시)를 피할 수 있습니다.
