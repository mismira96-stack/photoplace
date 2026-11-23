# 안티 리뷰 요청 — Original Cleanup Phase 1

2026-10-24, Phase 0 코드 승인 이후 사용자 지시로 증거 원장까지만 구현했다.
검증 전문: `REVIEW_Original_Cleanup_Phase1_2026-10-24.md`.

## 검토 파일

- 신규: CleanupEvidenceRecord.java, CleanupEvidenceSession.java, CleanupEvidenceStore.java, MediaCopyReceipt.java.
- 연결: MediaCopyEngine.java, SortJob.java, SortJobResult.java, SortResultStore.java.
- 테스트: CleanupEvidenceRecordTest.java, CleanupEvidenceSessionTest.java, verification/cleanup-evidence-probe/.
- 별도 사용자 UI 요청: MemoryCardCountPresentation.java, MemoryCardCountRenderer.java, MemoryCardCountPresentationTest.java.
- MainActivity는 Phase 0 결과와 동일하다. HEAD diff는 이전 세션의 누적 변경을 포함하므로 이 단계 변경으로 오인하지 말아 달라.

## 핵심 질문

1. 실제 반환 target URI를 기록하며 실패/건너뜀/영상이 원장에 섞이지 않는가?
2. 50건/항목 간 5초 체크포인트, 취소/예외 finally flush, 미기록 tail 보류, 저장 실패 시 버퍼 상한 및 해당 세션 기록 중단이 적절한가?
3. SQLite 트랜잭션·FULL 동기화·유일 키·페이지 조회·파괴적 업그레이드 금지에 누락이 있는가?
4. 증거 오류가 복사 실패/Worker retry로 연결되는 경로가 남아 있는가? SortJob CopyEngine 인터페이스도 확인해 달라.
5. 모든 행이 `COPY_OBSERVED_UNVERIFIED`이고 content_sha256 NULL임을 확인해 달라. recordId의 SHA256은 내용 검증이 아니다. Phase 2의 전체 해시/MemoryDependencyGuard 없이 정리 자격을 부여하는 경로는 없어야 한다.
6. MainActivity 신규 로직 0줄, 기존 copy filename duplicate 판정/검색 캐시/메모 저장 불변을 확인해 달라.
7. 별도 수량 표시 수정이 알 수 없는 예전 합계를 사진 수로 꾸미지 않고 장·개 단위를 명확히 하는가?

Debug/Release 각 346개 테스트·APK 빌드·lintVitalRelease 통과. 실제 폰에서 격리된 SQLite 검증 및 실제 process kill 후 checkpoint 복구, 실제 SortJob+fake copy engine 검증을 수행했다. 실제 사용자 사진 복사는 하지 않았다.

합성 기록 3000건: 총 297ms / checkpoint 227ms (SM_F971N 한 표본). 실제 복사 throughput·진행 UI 검증은 설치 후 남아 있다. 설치된 사용자 앱은 여전히 Phase 0 1.3.10(40)이다.

이 요청은 Phase 1 코드 리뷰다. 승인 전 Phase 2 진행/원본 정리 활성화/배포 범위를 자동 확대하지 않는다. 리뷰 결과는 프로젝트 루트 REVIEW 파일 및 공유 인덱스에 남겨 달라.
