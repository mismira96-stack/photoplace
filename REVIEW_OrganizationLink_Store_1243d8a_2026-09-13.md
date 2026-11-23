# PhotoPlace OrganizationLink Store 코드 리뷰

**일자:** 2026-09-13  
**대상 커밋:** 8cd8192 (Add organization link persistence) -> 1243d8a (Harden organization link replay semantics)  
**최종 판단:** **승인 (Approved)**  

---

## 1. 개요 및 리뷰 범위

PhotoPlace에서 발견한 기억(Memory, mem_<UUID>) 및 기억 모음(Collection, group_<UUID>)의 stable identity와 실제 Gallery 위치 앨범(Pictures/<앨범명>/) 출력을 generic하게 연결하고, 멱등적인 저장 및 손상 복구를 제공하는 OrganizationLink 영속 저장 계층을 검토했습니다.

### 검토 파일
- pp/src/main/java/com/example/gallerysorter/OrganizationLink.java
- pp/src/main/java/com/example/gallerysorter/MemoryOrganizationLinkStore.java
- pp/src/test/java/com/example/gallerysorter/OrganizationLinkTest.java
- pp/src/test/java/com/example/gallerysorter/MemoryOrganizationLinkStoreTest.java
- TODO.md
- WORKLOG_2026-09-13.md

---

## 2. 1차 리뷰 Blocker 및 보완 사항 반영 결과 (1243d8a)

### ① [Blocker 1 해결] writeRoot() 백업 삭제 실패 시 거짓 FAILED 반환 방지
- **위치:** MemoryOrganizationLinkStore.java:263-265
- **반영 내용:**
  `java
  if (backup.exists()) {
      backup.delete();
  }
  `
- **검토:** 	emp.renameTo(target)이 성공하여 물리적 원자 교체가 완료된 시점에서 일시적 파일 락 등으로 인한 ackup.delete() 실패가 더 이상 예외를 던지지 않습니다. 성공 트랜잭션이 허위로 CommitResult.FAILED를 반환하던 문제가 완벽히 해소되었습니다.

### ② [Blocker 2 해결] sameRequest() 타임스탬프 엄격 비교 제거
- **위치:** MemoryOrganizationLinkStore.java:286-295
- **반영 내용:**
  - 비교 대상에서 organizedAtMillis를 제외하고 순수 비즈니스 페이로드(subjectType, subjectId, equestId, lbumName, elativePath, copiedCount, skippedCount, ailedCount)만으로 동일성을 판정합니다.
- **검토:** Worker 재시도, 비동기 브로드캐스트 재수신, 앱 재시작 후 결과 재확인 시 candidate 객체에 새 시각이 부여되더라도 가짜 CONFLICT가 발생하지 않고 CommitResult.ALREADY_COMMITTED로 안전하게 멱등 처리됩니다.

### ③ [API 추가] indLatestUsableLink() 추가 및 hasUsableOutput() 일원화
- **위치:** MemoryOrganizationLinkStore.java:75-91
- **반영 내용:**
  - SUCCESS와 PARTIAL만을 usable로 필터링하고 MISSING은 배제합니다.
  - 동일 장소가 재정리되어 여러 usable 링크가 존재하는 경우 타임스탬프가 가장 최신(organizedAtMillis >= latest.organizedAtMillis)인 유효 링크를 단일 객체로 반환합니다.
  - hasUsableOutput()도 indLatestUsableLink() != null에 위임하여 판정 기준이 단일화되었습니다.

### ④ [경계 검증 보강] 
ormalizeRelativePath() 및 hasSubjectPrefix() 보강
- **위치:** OrganizationLink.java:110-123
- **반영 내용:**
  - hasSubjectPrefix: mem_, group_ 단독 빈 ID를 id.length() > prefix.length() 검사로 차단.
  - 
ormalizeRelativePath: Windows 역슬래시(\)를 슬래시(/)로 치환하고, 끝 슬래시 중복을 제거한 뒤 표준 단일 슬래시(path + /)로 통일.
- **검토:** Pictures\Place, Pictures/Place/, Pictures/Place/// 등 다양한 입력 형태가 모두 Pictures/Place/로 정규화되어 경로 비교 시 오작동을 차단합니다.

### ⑤ [테스트 보강]
- eplayIgnoresCreatedAtAndNormalizesRelativePath: 시각 차이 허용 및 역슬래시 정규화 멱등성 검증.
- indsLatestUsableOutputAndIgnoresMissingHistory: 최신 usable 링크 선택, MISSING 무시, 미존재 ID null 반환 검증.
- ejectsInvalidCandidatesAndLinkIdConflicts: 
ull 입력 거부(INVALID) 및 동일 linkId 충돌(CONFLICT) 검증.
- invalidIdentityStatusAndCountsAreRejected: 빈 ID prefix(mem_, group_) 거부 검증.
- elativePathUsesCanonicalSlashAndTrailingSeparator: 정규화 단위 테스트.

---

## 3. 핵심 안전성 분석 체크리스트

| 검증 항목 | 판정 | 설명 |
| :--- | :---: | :--- |
| **MISSING 링크 동일 equestId 재진입** | **안전** | existing.status == MISSING 분기에 의해 ALREADY_COMMITTED 반환, 디스크의 MISSING 상태가 SUCCESS로 덮어써지지 않고 유지됨 |
| **백업 복구 실패 시 상태** | **안전** | 파일과 백업이 모두 손상되었을 때 eadWritableRoot()가 
ull을 반환하여 쓰기 전면 거부(빈 데이터로 덮어쓰는 0-data overwrite 방지) |
| **잘못된/중복된 링크 데이터** | **안전** | 파일 로드 시 linkIds.add()와 equestIds.add()로 고유성 이중 검증, 위반 시 파일 전체를 손상 상태로 격리 |
| **원자 교체 중단/크래시** | **안전** | .tmp 생성 중단 시 원본 보존, 	arget -> backup 후 중단 시 다음 실행 시 백업 자동 복구, .corrupt 백업 보존 롤백 메커니즘 구비 |
| **다중 이력 보존** | **안전** | 한 subject의 과거 앨범 출력 이력을 모두 보존하며 최신 usable 출력만 선별 조회 가능 |

---

## 4. 후속 관리 권고사항 (Worker 연동 이후)

1. **OrganizationLink equals() / hashCode() 구현**: 값 객체로서의 기본 규약 보강.
2. **상세 로깅 (Log.w/Log.e)**: catch (Exception ignored) 구문에 로깅을 추가하여 운영 중 디스크 I/O 실패 추적성 확보.
3. **스키마 버전 마이그레이션 (SCHEMA_VERSION = 1)**: 스키마 2 이상 확장 시 구버전 데이터 호환 마이그레이션 훅 준비.

---

## 5. 최종 검증 및 결론

- **빌드/테스트:** 	estDebugUnitTest 통과, ssembleDebug 통과, git diff --check 통과.
- **최종 판단:** **승인 (Approved)**  
- **다음 단계:** 로컬 커밋 8cd8192, 1243d8a를 원격에 push한 후, SortWorker의 persisted request/result에 equestId와 subject identity를 연결하는 작업으로 안전하게 진입할 수 있습니다.
