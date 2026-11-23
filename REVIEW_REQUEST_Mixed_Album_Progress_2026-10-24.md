# Codex 리뷰 요청 — MIXED 앨범 추가 및 상세 진행 표시

최신 작업 맥락과 근거는 WORKLOG_2026-10-24.md를 먼저 읽어주세요. 기존 October19 미커밋 변경과 이번 패치를 구분하세요. 이 요청서는 독립 리뷰가 완료됐다는 뜻이 아닙니다.

## 우선 확인

- P0: 오늘 MainActivity에 새 business/판단/계산 로직을 추가했는지 먼저 확인. 오늘 변경은 정책/준비/배너 전용 클래스 위임 및 표시 연결이어야 함. 단순 전체 줄수 감소만으로 PASS 처리하지 말 것.
- P1: 원본 휴지통 노출/실행 확대, 사진 삭제/이동, stable Memory ID/날짜 메모/모음 유실 경로가 생겼는지 확인. 기기 테스트를 위해 임의로 사용자 사진을 바꾸지 말 것.

## 패치 A — MIXED 기존 앨범 추가

- MemoryAlbumAction.java 및 MemoryAlbumActionTest.java.
- MemoryBrowserState.java의 상세 액션/불명 상태 처리, MemoryBrowserStateTest의 신규 MIXED 회귀.
- DiscoveryOrganizeConfirmDialog.java의 기존 대상 추가 문구.
- MainActivity의 showSingleMemoryOrganizeConfirmation/prepareSingleMemoryAlbum/createMemoryAlbumHeaderAction 호출 대체.
- 질문: canOrganize/canAddNewItems 경계를 보존하는지, 기존 경로가 고정되는지, 대상 없음/조회 불명 시 다른 폴더 생성이 없는지, 영상 제외/중복 건너뜀/모두중복 경로/alias 안정성이 유지되는지.
- 기존 파일명 동일성 한계, 후보 준비 이후 외부 변경 경쟁은 문서에 남겼으며 해결했다고 주장하지 않음.

## 패치 B — 진행 표시

- WorkingProgressBanner.java / WorkingProgressPresentation.java / WorkingProgressPresentationTest.java.
- MainActivity의 addWorkingBanner/updateProgressBanner/setWorking/renderProgress 연결.
- 질문: 미작업 상태로 열린 상세에서 작업을 시작하면 배너가 나타나는지, 기존 Store/Worker 진행 값을 사용하는지, 완료/취소/화면 전환/캐시된 상세 재부착 시 오래된 뷰 갱신이나 잔여 배너가 없는지.
- 기존 홈 progress 뷰가 없어도 배너가 갱신되는지, 준비 중 total0 처리와 수량 클램프, 접힘/펼침 및 UI 트리 탐색 비용 확인.
- 별도 진행 폴링/스레드/스캔을 추가하지 않았는지 확인.

## 근거 및 한계

- 변경 전 MIXED 버튼 회귀 실패, 변경 후 Debug327/Release327 통과. 진행 표시 패치 후 Debug331/Release331 및 assembleRelease 통과.
- 38 Play 서명 업데이트 설치와 사용자 성남 작업 완료는 확인. 39 표시 패치의 실제 수량 증가/백그라운드 복귀는 WORKLOG 최종 상태와 대조.
- 수원 간헐 ‘기록 없음’은 미재현 이슈로 남음. 이번 변경과 별도로 다룰 것.
- 전체 lint/앱 전수 무결성/독립 외부 리뷰 완료로 확대 해석하지 말 것.
- 최종 리뷰는 프로젝트 규칙에 따라 별도 REVIEW_주제_2026-10-24.md 파일로 저장하고, Blocker/Pre-release/Follow-up 및 파일·줄·조건·제안을 구분해주세요.
