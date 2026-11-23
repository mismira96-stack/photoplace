# 리뷰 요청 — 뷰어 다음 사진 cache miss 깜빡임

저장소 `C:\work\projects\PhotoPlace`. AGENTS.md/GEMINI.md를 읽고 리뷰 결과를 루트 `REVIEW_Viewer_Image_Transition_Antigravity_2026-10-19.md`에 저장해주세요.

신규 ViewerImageTransition.java 및 ViewerImageTransitionTest.java, 변경 MemoryViewerImageLoader.java가 이번 범위. 앞선 Back 복귀 패치는 REVIEW_REQUEST_Viewer_Back_Return_2026-10-19.md 별도 검토이며 기존260-test 승인으로 두 패치 승인을 갈음하지 말아주세요. untracked 파일도 직접 읽어주세요.

변경 이유/제약/269-test 결과/Drive 전달 파일은 REVIEW_Viewer_Image_Transition_2026-10-19.md 참조.

중점: cache miss에서 현재 사진→placeholder→새 사진 flash가 제거되는지, 첫 진입 preview는 남는지, 빠른 넘김·영상 전환·detach 이후 오래된 콜백이 표시되지 않는지, 읽기 실패 시 이전 사진을 새 URI 결과로 영구 표시하지 않는지, cache hit/close 및 메모리 보유량 회귀, MainActivity 판단 추가 없음/미디어 불변을 확인해주세요.

최종269 tests/debug build/diff check PASS. 실기기 시각 확인 및 외부 승인 전. 결과는 Blocker/Pre-release/Follow-up, 파일·줄·재현·영향·수정 제안을 구분해주세요.
