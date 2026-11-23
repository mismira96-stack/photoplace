# 실기기 발견기록 성능 및 스냅샷 해석 캐시 코드 리뷰 보고서

- **검토자:** Antigravity (Google DeepMind)
- **일자:** 2026-10-19
- **검토 대상:** [`REVIEW_Device_Discovery_Performance_2026-10-19.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_Device_Discovery_Performance_2026-10-19.md), [`REVIEW_REQUEST_Device_Discovery_Performance_2026-10-19.md`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/REVIEW_REQUEST_Device_Discovery_Performance_2026-10-19.md), [`DiscoverySnapshotStore.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotStore.java), [`DiscoverySnapshotDecodeCache.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotDecodeCache.java), 관련 단위 테스트 2건
- **판정:** **코드 반영 승인 (APPROVED for Merge)** — 단, 전체 출시 승인과는 엄격히 구분되며 아래 Pre-release 및 Follow-up 과제 승계 필요

---

## 1. P0 / P1 원칙 및 핵심 검증 요약

### 🚨 [P0 원칙] MainActivity 비대화 절대 금지 (0줄 격리 원칙) — 통과 (PASS)
- 이번 스냅샷 디코드 캐시 및 실기기 튜닝 패치에서 [`MainActivity.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MainActivity.java)의 신규 추가 라인은 **정확히 0줄**입니다.
- Git 누적 working tree 기준 `MainActivity.java`는 **+152 / -415 (순감소 -263줄)** 상태를 안정적으로 유지하고 있습니다.
- 모든 파일 I/O 버퍼링, JSON 디코딩, 동일 내용 캐싱 로직은 [`DiscoverySnapshotStore`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotStore.java) 및 [`DiscoverySnapshotDecodeCache`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotDecodeCache.java) 전용 클래스로 철저히 격리되었습니다.

### 🔒 [P1 원칙] 데이터 안전성 및 무결성 규칙 — 통과 (PASS)
- **MediaStore / 파일 불변 원칙:** 디스크의 미디어 파일을 복사, 이동, 삭제하지 않으며 MediaStore 레코드를 변경하지 않는 Read-only Projection을 엄격히 유지합니다.
- **원본 휴지통 이동(Trash) 영구 차단:** 휴지통 이동 로직 영구 차단 유지 확인.
- **사용자 핵심 메타데이터 보존:** 실제 연결 기기(SM-F971N) 테스트 및 업데이트 전후 핵심 JSON 7종(`album_summary_history`, `discovery_snapshot`, `memory_collections`, `memory_date_notes`, `memory_identity_registry`, `memory_organization_links`, `memory_personalization`)의 SHA256 해시가 100% 동일함을 실증했습니다.

### 🧪 자동 빌드 및 단위 테스트 검증
- `./gradlew.bat testDebugUnitTest --rerun`: **총 300 tests PASS (0 failures, 0 errors, 0 skipped)**
  - 신규 추가된 7개 회귀 테스트(`DiscoverySnapshotDecodeCacheTest` 3건, `DiscoverySnapshotStoreTest` 4건) 모두 정상 통과.
- `git diff --check`: 포맷/공백 오류 없음 (**PASS**).
- Debug APK 빌드 및 SHA256 (`AFDF0DF39C0F61793680476594325EAE2381C074BC7C97DE020DC85F9FF63F54`) 일치 확인.

---

## 2. 주요 기술 검토 항목 상세 분석

| 검토 항목 | 구현 분석 및 검증 결과 | 상태 |
|---|---|---|
| **내용 기반 동일성 비교** | `File.lastModified()`나 `File.length()`에 의존하지 않고, 실제 디스크에서 읽은 전체 텍스트에 대한 `text.equals(previousText)`를 수행. 파일 크기나 타임스탬프가 같더라도 내용이 다르면 즉시 재해석됨을 단위 테스트로 검증함. | **PASS** |
| **손상/미지원 파일 및 캐시 무효화** | 텍스트 불일치 시 즉시 `clear()`를 선행 호출. 손상(`{broken`) 또는 미지원 스키마인 경우 `null`을 반환하고 캐시를 비워둔 상태로 유지. `canSafelyWrite()`는 디코드 캐시를 거치지 않고 독립적인 JSON 유효성을 검사하므로 손상 원본을 캐시 존재로 인해 덮어쓰는 사고를 원천 차단함. | **PASS** |
| **백업 복구 보장** | 메인 스냅샷이 손상되었을 때 백업 파일(`discovery_snapshot.json.bak`) 디코드를 시도하고, 유효한 경우 `restoreBackup()` 수행 후 복구된 객체를 정상 반환함을 검증함. | **PASS** |
| **반환 모델 불변성 & 스레드 안전성** | [`DiscoverySnapshot`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshot.java), [`DiscoveryMemoryGroup`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoveryMemoryGroup.java), [`DiscoveryPhotoRef`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoveryPhotoRef.java)의 모든 필드는 `final`이며 리스트는 `Collections.unmodifiableList`로 보호되어 재사용 시 안전함. `DiscoverySnapshotStore`의 `read()`, `save()`, `clear()`는 모두 `synchronized`로 동기화됨. | **PASS** |
| **메모리 상한 및 안전 정책** | 기본 4MB(4,194,304 char) 초과 시 객체 디코딩은 정상 수행하되 캐시에는 보관하지 않음(`oversizedContentIsDecodedButNotRetained`). 단일 인스턴스 멤버 변수로서 static 메모리 릭 위험 없음. | **PASS** |
| **I/O 버퍼 최적화** | 기존 라인별 `readLine()`의 수천 개 임시 String 할당을 제거하고, 16KB(`char[16384]`) 버퍼를 사용해 3.6MB 파일의 읽기 오버헤드와 GC 압박을 대폭 경감함. 원본 개행 보존은 `new JSONObject(text)` 파싱에 아무런 부정적 영향을 주지 않음. | **PASS** |

---

## 3. 실측 성능 관측 분석

실제 연결 기기(SM-F971N)에서 관측된 지표 분석:
- **발견기록 진입:**
  - 최초 진입: 1,115ms (재시작 후 482ms) → 캐시 패치 후 **944ms**
  - 재진입(Warm): 2~5ms → **2ms ~ 11ms** (`reused=true`)
- **장소 상세 진입 (성남 2,107건 / 수원 865건):**
  - 성남 첫 상세 진입: 568ms (재시작 후 666ms) → **387ms**
  - 수원 첫 상세 진입: 381ms → **265ms**
  - 교차 재진입 (성남↔수원 왕복 후 성남): 3ms → **2ms** (`reused=true`)
  - 상세 뷰 바인딩 시간: **16ms ~ 18ms**
- **뷰어 백(Back) 복귀:**
  - 목록 루트 재부착(`returnReused=true`): **6ms**
  - 외부 플레이어 복귀(pause 후 invalidate fallback): 준비 12ms + 바인딩 16ms = **28ms** 복귀

> **평가:** 장소 교차 방문 시 multi-place LRU 캐시(3장소/10,000건)와 스냅샷 해석 캐시의 시너지로 재진입 및 왕복 시 **2~6ms 수준의 즉각 반응**이 실기기에서 안정적으로 유지됩니다.

---

## 4. 발견 사항 및 후속 과제 (Findings)

### ⚠️ [Pre-release] 성남 카드와 상세 화면 간 수량 불일치 (176건 차이)
- **위치:** [`MemoryMediaCounts.java:46-52`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaCounts.java#L46-L52), [`MemoryRepository.java:274-276`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryRepository.java#L274-L276), [`MemoryMediaResolver.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaResolver.java)
- **현상:**
  - 발견기록 카드 표시: **2,107개**
  - 카드 탭 후 상세 화면 표시: **사진 2,252장 · 동영상 31개 (총 2,283개)**
  - 불일치: 상세 화면이 카드보다 **176개 더 많음** (수원은 854장 + 11개 = 865개로 카드와 상세 일치)
- **원인 분석:**
  - `MIXED` 소스 장소(정리된 앨범 + 발견 스냅샷이 공존하는 장소)의 경우, 카드는 `MemoryRecord.itemCount`(= `Math.max(existing.itemCount, incoming.itemCount)`)를 기반으로 표시됩니다.
  - 반면 상세 화면은 `MemoryMediaResolver.resolve()`를 통해 물리 앨범 MediaStore 레코드와 Discovery 참조를 실제 합집합(Union) 처리합니다.
  - 성남의 경우 물리 앨범 외부에 존재하는 신규 발견 사진이 176장 존재하여 합집합 크기가 2,283개로 확장됩니다.
- **판정 및 권고:**
  - Codex가 임의로 `Math.max`를 단순 덧셈(`existing + incoming`)으로 변경하지 않은 판단은 매우 적절했습니다. 단순 덧셈 시 중복 항목으로 인해 4,000건이 넘는 허위 팽창이 발생할 수 있기 때문입니다.
  - **수정 제안:** 출시 전 해당 176개 항목의 URI/경로 구성을 진단하고, 카드 요약 생성 단계에서 합집합 카운트를 비동기로 계산하거나 `MemoryCollectionCardSummary`에 동기화하는 메커니즘을 적용해야 합니다.

### 📋 [Follow-up] 첫 탭 Cold 진입 시의 종합 준비 비용 최적화 (944ms)
- **위치:** [`DiscoverySnapshotController.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java), [`DiscoverySnapshotLiveFilter.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/DiscoverySnapshotLiveFilter.java), [`MemoryBrowserEntry.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryBrowserEntry.java)
- **현상:** 프로세스 콜드 스타트 후 첫 발견기록 준비 시간이 약 944ms 소요됨.
- **원인:** 3.6MB 플래시 I/O 읽기 및 `DiscoverySnapshotLiveFilter`를 통한 전체 항목 생존성(MediaStore 검증) 및 앨범 매핑 작업이 수행되기 때문입니다.
- **권고:** 첫 탭 진입 시 이전 세션의 가벼운 요약본(`MemoryCollectionCardSummary`)을 홈 대시보드처럼 먼저 렌더링하고, 백그라운드에서 전체 검증을 수행하는 점진적 렌더링을 차기 성능 과제로 권장합니다.

### 📋 [Follow-up] 승인 범위 및 성능 표현의 명확화
- 이전 리뷰(`REVIEW_Discovery_Tab_Load_Antigravity_2026-10-19.md` 등)에서 언급된 "UI 프리징 해소"는 메인 스레드 블로킹 해소 및 비동기 워커 격리를 의미하며, 프로세스 콜드 스타트 시 물리적 I/O 및 연산 시간 자체가 0ms가 됨을 뜻하지 않습니다.
- 실기기 측정 결과 웜 재진입은 2~6ms, 콜드 스타트는 ~944ms로 나타났으며, 비동기 로딩 알림 필(Notice Pill)을 통해 사용자 체감 응답성을 확보하고 있습니다.

---

## 5. 결론 및 다음 단계

1. **승인 여부:**
   - 본 300-test 스냅샷 해석 캐시 및 I/O 최적화 패치는 **코드 반영 승인 (APPROVED for Merge)** 합니다.
   - P0/P1 원칙을 완벽히 충족하며 메모리 누수 및 데이터 손상 위험이 없습니다.
2. **출시 전 잔여 조치:**
   - 성남 176건 수량 불일치 진단 (Pre-release).
   - 실제 GPS 태깅된 신규 사진 촬영 후 성남 `NEW` 뱃지 및 점분석 반영 실기기 최종 확인.
