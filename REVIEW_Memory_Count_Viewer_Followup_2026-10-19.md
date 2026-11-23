# Antigravity 수량·뷰어 리뷰 수신 및 근거 대조

- 작성: Codex, 2026-10-19.
- 원문: REVIEW_Memory_Count_Viewer_2026-10-19.md (원문 보존).
- 수신 판정: Antigravity는 **코드 반영 승인 / 전체 앱 조건부 출시 승인**. 실제 merge/commit/release가 실행된 것은 아님.
- 수행 범위: 원문 전체 읽기 및 아래 코드 근거 대조. 이번 확인에서 테스트/lint를 새로 실행하거나 제품 코드를 변경하지 않음. 이전 최종 자동 검증은 260 tests/debug build PASS.

## P0 / P1

MainActivity diff는 현재 +81/-382로 리뷰 기준과 일치. 신규 수량/이미지 로딩 정책은 전용 클래스에 있으며 이번 리뷰 수신 과정에서 추가 코드를 넣지 않음. 미디어/메모/identity 데이터 쓰기 없음.

## Pre-release — 유지할 확인

1. 최신 debug 설치 후 많은 사진의 장소에서 확대·넘김·사진→영상·Back 확인. 아직 미설치이며 실기기 속도 검증 전.
2. 같은 source 범위의 홈 카드/상세 수량 비교. 발견-only 목록과 앨범 포함 화면이 모두 같아졌다고 해석하지 않음.
3. 사용자는 첫 사진의 GPS 부재를 삼성 갤러리에서도 확인했고 두 번째 촬영은 정상으로 나왔다고 알려줌. 새 사진 발견 성공과 별개로 성남 NEW/추가 수량 및 동일 소스 반복 보고0 전체 절차의 명시적 결과는 아직 확인 필요.

## Follow-up — 리뷰 문구의 한계 및 정정

- **수량 전체 해결로 해석 금지:** 원문의 “핵심 화면의 수량 불일치 완전히 해결”, “홈과 상세 완벽히 일치”는 동일 source 결과 및 안정된 미디어 상태라는 범위에서만 유효. MemoryRepository.java:113/133 및 DiscoverySnapshotController.java:163은 여전히 발견-only/앨범 포함 범위를 달리함. MIXED 집계 Math.max는 MemoryRepository.java:274~276에 남아 있음. 원문의 Non-blocking 판단은 리뷰어의 판단으로 보존하며 사용자 제보 전체 해결로 표시하지 않음.
- **목록 합집합 구현 방법:** 원문은 완전한 목록 집계에는 모든 앨범의 동기 조회가 필요하다고 서술하지만 필수 제약은 아님. 백그라운드 계산/리비전 캐시/필요한 카드만 갱신하는 방법도 있음. UI 스레드 전수 조회를 전제로 후속 작업을 포기하지 말 것.
- **2048px는 요청 크기:** MemoryViewerImageLoader.java:55는 provider 요청 상한2048. stream fallback의 샘플링은 :141에서 size*2를 사용하므로 fallback 결과 장변이2048 이하라는 보장이 아님. 16MiB는 캐시 보유 한도이며 실행 중 decode+화면 bitmap을 포함한 총 RAM 한도가 아님.
- **RGB_565는 fallback 경로에만 지정:** MemoryViewerImageLoader.java:144. provider loadThumbnail 결과에도 동일 포맷/50% 절감이 적용된다고 일반화하지 않음.
- **lint:** 이전 오류179건의 source 문장이 HEAD에 있음을 확인한 근거는 있음. 새 패치 이후 lint를 재실행했다는 별도 근거 없이 “이번 패치 신규 오류0”을 추가 실행 결과로 단정하지 않음.
- **링크 경로:** 원문 일부 링크가 옛 `C:/Users/mismi/Documents/Codex/GallerySorter`를 가리킴. 현재 실제 작업/리뷰 대상은 `C:/work/projects/PhotoPlace`. 링크가 오래됐다는 사실만으로 잘못된 checkout을 검토했다고 단정하지 않음.

## 최종 상태

외부 코드 반영 승인은 수신 완료. 현재 설치 전 및 실기기 확인 전이고 목록 수량/source/교차 중복 정책의 OPEN 항목은 남음. 따라서 전체 수량 문제 해결 또는 출시 완료로 기록하지 않는다. 후속 판단은 본 파일과 원문/수량 조사 보고서를 함께 참조한다.
