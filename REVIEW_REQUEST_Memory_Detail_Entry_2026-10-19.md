# 리뷰 요청 — 많은 사진의 메모리 날짜별 목록 진입

저장소 `C:\work\projects\PhotoPlace`. AGENTS.md/GEMINI.md를 읽고 수정 없이 리뷰, 결과는 **REVIEW_Memory_Detail_Entry_Antigravity_2026-10-19.md**에 저장해주세요.

사용자가 새 APK의 성남 지연을 카드→목록 구간으로 확정했고2,107개라고 알려줬습니다. 기존 viewer image/Back 수정과 별개이며 이번 source loading 패치를 이전260-test 승인으로 처리하지 말아주세요.

주요 파일: MemoryDetailLoadController.java / MemoryDetailLoadSession.java / MemoryBrowserState.java(mediaResolved) / MemoryViewerReturnController.java(revision 전달) / MemoryPhotoSection.java(formatter) / MainActivity의 async load/bind 및 lifecycle 연결. 테스트 MemoryDetailLoadSessionTest / MemoryMediaCountsTest / MemoryBrowserStateTest. 신규untracked 파일 본문도 확인하세요.

REVIEW_Memory_Detail_Entry_2026-10-19.md의 변경/한계를 참고하고 다음을 검토해주세요.

1. P0: MainActivity는 load/bind와 lifecycle만 연결하고 새 정책은 외부에 있는가.
2. MediaStore/live filter/repository/Gallery/date section 해석이 UI 스레드에서 제거됐는가. source 공급자의 공유 단기 캐시 및 home worker와의 동시 접근도 검토.
3. 다른 화면으로 이동/다른 카드/Back/회전/외부 앱 이탈 후 늦은 결과·오류가 화면을 덮지 않는가. 진행 안내 cleanup과 상태 flag 타이밍, viewer Back fallback의 비동기 전환도 검토.
4. 같은 key/scope/stable revision만 재사용하고, 권한/unknown/동시 source변경/UNAVAILABLE 결과는 안전하게 재조회하는가. 캐시1건의 객체/Activity 보유량 및 lifecycle 정리 확인.
5. source revision을 기존 viewer return guard로 연결할 때 캐시된 오래된 목록을 정상 기준점으로 만들지 않는가.
6. 날짜 formatter의 지역/시간대/날짜메모 ownership/정렬/중복/페이지 처리 보존.
7. 원본 MediaStore/파일/identity/메모/링크에 쓰기가 없는가.

최종276 tests/debug APK/diff check PASS, 폰 실기기 계측·시각 확인 전. 이 패치는 cold 첫 source 해석 시간0을 보장하지 않으며 UI응답/warm 재진입 개선이다. 모음 상세 source, 메모 헤더 I/O, 수량 source 범위는 별도 한계로 유지.

결과는 Blocker/Pre-release/Follow-up, 파일·줄·재현·영향·수정 제안을 구분하고 독립 실행 검증과 코드 추론/실기기 미검증을 구별해주세요.
