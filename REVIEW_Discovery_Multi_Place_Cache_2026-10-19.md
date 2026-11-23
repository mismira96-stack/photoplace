# 발견기록 다중 장소 반복 조회 — 수정 및 자체 검증

작성: Codex, 2026-10-19. 외부 리뷰 승인 전. 앞선 발견기록 탭/상세 비동기화에 대한 후속 변경이다.

## 사용자 확인과 재현

- 사용자는 확대 뷰어 Back은 개선됐다고 확인했다. 성남과 수원은 상세 진입 시 여전히 로딩이 보이며, 기존 앨범 사진을 메모리 쪽으로 모두 변경했다고 설명했다. 실제 이동/복사/앨범 삭제 절차와 현재 파일 상태는 미확인이다.
- 캡처: 성남 합계 2,107개/이번에 +1,960장, 수원 사진854장+영상11개/이번에 +800장. 이 숫자가 실제 미디어 전체와 일치한다고 검증한 것은 아니다. 이번에 추가된 개수는 스냅샷에 새로 편입된 기존 사진을 포함할 수 있어 새 촬영 수와 동일시하지 않는다.
- 발견기록 성능 개선을 우선한다는 사용자 요청에 따라 확인. 기존 MemoryDetailLoadSession은 한 결과만 보관하여 성남→수원→성남에서 첫 성남의 캐시가 소실된다. 동일 revision이어도 다시 source를 읽는다.

## P0 / P1

- 이번 후속 패치의 MainActivity 추가 변경은 **0줄**. 기존 working tree 변경은 별개이며 이전 리뷰 범위다.
- 변경 파일은 MemoryDetailLoadSession.java, MemoryScreenLoadController.java, MemoryDetailLoadController.java 및 해당 세션 테스트다.
- 원본 미디어, MediaStore, discovery snapshot, OrganizationLink, 사용자 메모를 쓰거나 삭제하지 않는다. 메모 identity, 수량/NEW/source 선택 정책도 이번 변경에서 바꾸지 않는다.

## 구현

- `app/src/main/java/com/example/gallerysorter/MemoryDetailLoadSession.java:37`: key+revision 기반 access-order LRU. key에는 기존 albums/discovery 범위를 유지한다. revision 변경/미확인은 요청한 장소 외의 캐시도 폐기한다. 조회 전후 revision 일치, reusable predicate, 요청 세대 검사는 유지한다.
- `app/src/main/java/com/example/gallerysorter/MemoryDetailLoadSession.java:35`: clear와 cache 접근/게시를 동기화하여 close/clear 후 이전 요청의 늦은 캐시 게시를 막는다. 오래 걸리는 source 조회는 잠금 밖에서 수행한다.
- `app/src/main/java/com/example/gallerysorter/MemoryDetailLoadController.java:21`: 상세는 최대3곳, 전체 sourceUris 합계10,000개 제한. 큰 결과는 표시하되 캐시하지 않는다. 덜 사용한 장소부터 제거한다. 비트맵/View를 추가 보관하지 않는다. URI 개수는 메모리 사용량의 대략적인 제한이며 정확한 byte/RSS 상한은 아니다.
- `app/src/main/java/com/example/gallerysorter/MemoryScreenLoadController.java:36`: 기본 생성자는 기존 한 결과 캐시 유지. 발견기록 탭 캐시는 그대로 한 결과이며 상세 전용 생성자에서만 다중 결과 정책을 공급한다.

## 검증

- `gradlew.bat testDebugUnitTest assembleDebug`: PASS. **290 tests / 0 failures / 0 errors**.
- 신규6건: 성남2,107/수원865 상당 가중치로 번갈아3회 방문 시 source는 최초2회만 호출, LRU 방문 순서, 엔트리 제한보다 먼저 가중치 제한 도달, 초과 결과 정상 전달/캐시 금지, 변경/unknown revision에서 모든 옛 장소 폐기, clear 도중 결과 게시 방지.
- 기존 source scope/조회 실패/불완전 결과/취소/탭 데이터 테스트 통과. `git diff --check` PASS.
- adb 연결 기기0대. 실제 기기의 첫 프레임/소요 시간과 캐시 적중 로그는 아직 확인하지 못했다. 기존 lint 부채는 별도이며 lint 전체 통과를 주장하지 않는다.

## 잔여 발견 사항

### Pre-release — 실제 기기 및 데이터 변경 시 캐시 무효화 확인

대상: MemoryDetailLoadSession.java:37, MemoryScreenLoadController.java:40. 성남→Back→수원→Back→성남 순서에서 PhotoPlaceDetail의 reused/sourceMs/bindMs 확인. 사진 추가/삭제/이동, 권한 변경, 앱 재시작 후에는 fresh source와 최신 목록이 보여야 한다. API29/부분 권한/unknown revision은 캐시를 보장하지 않는다. UI root·그리드는 재생성하므로 warm에서도 로딩 안내 또는 binding 비용이 남을 수 있다.

### Follow-up — 첫 상세 진입이 전체 발견 자료를 준비하는 비용

대상: DiscoverySnapshotController.java:159 및 :170, DiscoverySnapshotLiveFilter.java:24. loadBrowserDetail은 repository 전체 스냅샷 read→전체 그룹 live filter→전체 records의 MemoryBrowserState 구성 후 한 장소를 선택한다. Live filter는 ID500개 단위 batch 조회이며 사진별 단건 조회는 아니다. 사진 수가 많을 때 첫 진입의 전체 준비 비용이 남는다. 다음 개선은 장소 범위 조회 또는 revision이 확인된 공유 repository 준비 결과를 재사용하는 방향을 검토하되, 삭제/권한/앨범 source 제외 정책을 보존해야 한다. 아직 구현/실기기 계측하지 않았다.

### Follow-up — 발견 탭 UI 생성과 전체 수량 일관성

탭의 장소 카드 View 생성, 썸네일, 모음 상세 로딩 비용은 이번 캐시 개선으로 해결되지 않는다. 화면에 나타난 성남 generic 합계와 수원 사진/영상 분리 표기는 MemoryMediaCounts의 source 타입 분기와 관련될 수 있으나 실제 현재 데이터의 타입은 기기 확인 전이다. MIXED 저장 집계를 단순 더하기로 바꾸지 않는다. 기존 수량/중복 식별 이슈는 OPEN 유지한다.

## 전달

- `G:\내 드라이브\Codex APK\PhotoPlace-1.3.6-multi-place-cache-20261019-debug.apk`
- 빌드 파일 시간21:53:45 KST, 9,312,723bytes. version1.3.6/code36/debug. 이전 APK는 유지.
- APK 원본/Drive 사본 SHA256은 WORKLOG_2026-10-19.md에 기록한다. 폰 설치/데이터 삭제/커밋/공개 배포 없음.
- Drive 동기화 로그에서 파일명과 매칭된 ID 확인: [확인용 APK](https://drive.google.com/file/d/1s3yNegLImA3TFyhxJ7x7Tmu7LyNLyCU0/view). 공유 권한은 변경하지 않았다.
