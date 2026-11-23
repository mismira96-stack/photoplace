# 리뷰 요청 — 확대 뷰어 Back 복귀 화면 재사용

저장소 `C:\work\projects\PhotoPlace`, HEAD `1c8d683` 대비 working tree. AGENTS.md/GEMINI.md를 먼저 읽고 수정 없이 리뷰해주세요. 결과는 **REVIEW_Viewer_Back_Return_Antigravity_2026-10-19.md**에 저장해주세요.

직전 반영 승인은260-test 수량/이미지 로딩 패치입니다. 이번 Back 복귀 패치는 그 승인 이후 변경이며 별도 검토가 필요합니다. 상세 변경/실험/전달 APK는 REVIEW_Viewer_Back_Return_2026-10-19.md를 참고해주세요.

대상: 신규 MemoryViewerReturnController.java / ViewerReturnState.java / ViewerReturnStateTest.java, MainActivity의 해당 컨트롤러 연결과 closeMemoryPhotoViewer/rebuildMemoryPhotoViewerReturn. 신규 파일은 untracked일 수 있으므로 git diff 외에 본문도 확인해주세요.

검토 사항:

1. P0: MainActivity에 새 수량/소스/재사용 판단이 들어갔는지 우선 검사. 콜백의 UI mode flag 바인딩과 컨트롤러 정책을 구분.
2. 실제 상세 build 전후 token이 안정된 경우에만 원래 root를 재사용하는지. 뷰어 진입 전 source 변경도 놓치지 않는지.
3. Back 재사용 경로에 전체 MediaStore/live filter/Gallery resolver/그리드 재생성이 남아 있는지.
4. 스크롤/더보기/메모/하단 탭/모음 상세 상태 및 다음 Back 목적지 보존.
5. onPause/config/destroy 무효화, 외부 Gallery 변경, 권한/unknown/API29 fallback, 기존 parent가 남은 View 처리.
6. View/Activity 메모리 보유·재진입·configuration 시 보관된 viewer/원래 상세의 혼동 가능성. revision 검사 비용이 UI에 남는 한계도 평가.
7. 미디어/메모/alias/링크에 쓰기가 없는지.

최종265 tests 및 assembleDebug PASS, diff check PASS. 폰 미연결로 새 APK 실기기 검증 전. 기존 lint179 errors는 별도 보고서의 기존 부채이며 이번 lint 통과를 주장하지 않습니다.

발견 사항은 Blocker/Pre-release/Follow-up으로 파일·현재 줄·재현 조건·영향·수정 제안을 기록하고, 코드 반영 가능 여부와 실기기 미검증 한계를 분리해주세요.
