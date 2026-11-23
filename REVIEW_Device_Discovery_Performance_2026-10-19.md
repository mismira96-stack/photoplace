# 연결 기기 발견기록 검증 및 스냅샷 해석 캐시

Codex, 2026-10-19 22:06~22:13 KST. SM-F971N / R5KL503VHQR. 상태: 자동 테스트·실기기 탐색 확인 완료, 이번 스냅샷 캐시의 외부 리뷰 전. 공개 출시 승인을 뜻하지 않는다.

## 범위와 P0 / P1

사용자 요청: 연결한 폰에서 테스트하며 개선하고 기록을 남긴다. 설치 APK는 21:59 viewer-video-tap와 SHA256이 같음을 먼저 확인했다. 발견기록 첫/재진입, 성남·수원 첫/교차 재진입, 사진 뷰어 Back, 영상 직접 선택과 뷰어 도착 후 탭, 로딩 취소를 확인했다.

이번 제품 코드 추가 변경은 DiscoverySnapshotStore.java와 신규 DiscoverySnapshotDecodeCache.java이다. MainActivity 신규 변경0줄. 데이터 필터/카운트/source 우선순위/메모 identity는 바꾸지 않는다. 미디어 파일 이동/복사/삭제나 MediaStore 갱신을 실행하지 않았다. 앱 데이터 삭제 없이 `adb install -r`로 업데이트했다.

## 실측

시간은 앱 로그 sourceMs/uiWaitMs/bindMs 및 viewer reattachMs다. 실제 디스플레이의 첫 프레임 시간이나 전체 손가락 터치 지연과 동일하지 않다. OS 파일 캐시·설치 직후 최적화 등의 조건이 일정한 통계 벤치마크가 아니다.

| 구간 | 변경 전 21:59 APK | 최종 추가 패치 APK |
|---|---|---|
| 발견기록 첫 준비 | 1,115ms; 프로세스 재시작 후482ms | 944ms |
| 발견기록 재진입 준비 | 2~5ms (reused=true) | 2ms, 이후11ms (reused=true) |
| 성남 첫 상세 준비 | 568ms; 재시작 후666ms | 387ms |
| 수원 첫 상세 준비 | 381ms | 265ms |
| 성남↔수원 뒤 성남 준비 | 3ms (reused=true) | 2ms (reused=true) |
| 최종 상세 화면 binding | 비교 대상18/13ms 등 | 성남18ms, 수원16ms, warm16ms |
| 사진 뷰어 Back | 별도 중간 빌드에서5ms | returnReused=true / 6ms |

첫 성남/수원 진입은 상세 결과 캐시를 재사용하지 않은 reused=false다. 발견기록을 준비하며 읽었던 같은 스냅샷의 JSON 해석 결과는 재사용 가능하다. 중간 해석 캐시 빌드에서는 상세438/282ms 및466ms, 탭836/406ms도 관측됐다. **첫 탭 진입의 안정적인 속도 개선을 입증하지 못했다.** 일부 상세 수치가 낮아졌으나 이를 확정적인 개선율로 환산하지 않는다. 전체 파일 읽기·live filter·앨범 source 확인은 남아 있다.

## 재현과 동작 확인

- 성남→Back→수원→Back→성남에서 마지막 성남의 reused=true 확인. 다중 장소 캐시가 다른 장소 방문 때문에 즉시 밀리던 문제는 실기기에서도 개선 확인.
- 사진 확대 후 Back은 원래 목록 root 재부착(returnReused=true), 최종6ms. 외부 플레이어에 다녀와 pause된 경우 returnReused=false fallback 로그도 관측했고, 상세 데이터 캐시를 써서 준비12ms+binding16ms로 복귀했다. 이것은 기존 pause 시 root 무효화 정책에 따른 동작이다.
- 수원 10월16일 목록의 영상 직접 선택은 삼성 MoviePlayer로 열림. 같은 날짜의 5/6 사진에서 다음을 눌러6/6 영상에 도착하면 “눌러서 재생” 표시, 경계 스와이프만으로 앱이 전환되지 않음, 중앙 탭하면 삼성 MoviePlayer로 열림을 window focus/UI hierarchy로 확인. 모든 코덱/재생 종료/오디오 동기화를 검증한 것은 아니다.
- 프로세스 재시작 뒤 발견기록 로딩 도중 Back을 눌러 홈에 남고, 이후 늦은 결과가 발견 화면을 덮지 않는 것을 UI dump로 확인. 다시 탭해 정상 진입.
- 원본 기록 관련 JSON7개(album_summary_history, discovery_snapshot, memory_collections, memory_date_notes, memory_identity_registry, memory_organization_links, memory_personalization) SHA256이 테스트 시작/영상 확인 후/최종 업데이트 후 모두 동일. 모든 사진 파일을 byte 단위 해시한 검증은 아니다. home dashboard 등 가변 캐시 파일은 이7개 보존 비교에 포함하지 않는다.

## 추가 수정: 동일 내용의 스냅샷 JSON 반복 해석 제거

기기 discovery_snapshot.json은3,664,538bytes였다. 기존 Store.read는 repository를 만들 때마다 파일을 읽고 JSON 객체/모델을 전부 다시 생성한다. 화면 데이터 캐시가 없는 장소 첫 진입에서 이 비용이 반복된다.

- `app/src/main/java/com/example/gallerysorter/DiscoverySnapshotDecodeCache.java:14`: 실제 읽은 문자열이 이전 정상 파일 내용과 같을 때만 불변 DiscoverySnapshot을 재사용. timestamp/파일 길이만 비교하지 않는다. 내용이 바뀌거나 읽기 실패/지원하지 않는 schema/손상 내용이면 이전 해석 결과를 폐기한다.
- `app/src/main/java/com/example/gallerysorter/DiscoverySnapshotDecodeCache.java:22`: 최대4,194,304 Java char의 파일 내용 한 건+해석 결과를 유지한다. 초과 내용도 해석해서 반환하되 보관하지 않는다. 이는 모델 그래프를 포함한 정확한 RAM byte 상한은 아니다. 캐시는 Store 인스턴스 수명 범위이며 static 공유가 아니다.
- `app/src/main/java/com/example/gallerysorter/DiscoverySnapshotStore.java:27`: read는 실제 파일 읽기를 계속하고, 손상/미지원일 때 기존 backup 복구를 유지한다. 변경된 캐시가 live-filter 결과를 저장하는 것은 아니므로 매 repository의 생존 확인은 그대로 수행한다.
- `app/src/main/java/com/example/gallerysorter/DiscoverySnapshotStore.java:38`: save/clear는 해석 캐시를 먼저 무효화. save의 canSafelyWrite는 여전히 실제 JSON 유효성을 검사해 손상 원본을 캐시 존재 때문에 덮어쓰지 않는다.
- `app/src/main/java/com/example/gallerysorter/DiscoverySnapshotStore.java:85`:16K char 단위 파일 읽기로 줄별 문자열 할당을 줄이고 원래 개행을 유지한다. 파일 내용 비교는 읽힌 전체 문자열에 대한 정확한 equals다. 모든 접근은 기존 Store synchronized 안에서 이뤄진다.

## 자동 검증

`gradlew.bat testDebugUnitTest assembleDebug`: **300 tests / 0 failures / 0 errors**, 최종 debug build PASS. `git diff --check` PASS. 기존 lint 부채는 별도이며 lint 전체 통과를 주장하지 않는다.

추가7건: 동일 내용 재사용/clear, missing·손상·미지원 schema 후 이전 결과 폐기, 보관 제한 초과 정상 읽기/캐시 제외, 반복 store read 후 길이+mtime 같은 다른 내용 재해석, 캐시 상태에서 손상 원본 저장 차단, 캐시 상태에서 backup 복구/clear, save 후 새 version 반영. 기존 backup/unsupported schema/저장 보호 테스트도 통과했다. 기기 파일에 손상·삭제를 주입하지 않았고 해당 실패 실험은 임시 테스트 디렉터리에서만 수행했다.

## 잔여 발견 사항

### Pre-release — 성남 카드와 상세의 수량 불일치 재현

대상: MemoryMediaCounts.forRecord, MemoryRepository.merge, MemoryMediaResolver.resolve/withNewDiscovery. 카드2,107개 대 상세 사진2,252장+영상31개=2,283개. 수원은 카드/상세 모두854장+11개였다. MIXED 저장 집계와 실제 gallery+discovery 해석 범위의 차이가 여전히 있다. 실제 union에서 어떤176개가 추가되는지 파일/URI 단위 진단을 후속으로 해야 한다. 이번 작업에서 Math.max를 단순 합산으로 바꾸지 않았다. 사용자가 기존 앨범을 메모리로 전환한 정확한 절차와 물리 앨범 상태는 아직 별도 확인 전이다.

### Follow-up — 첫 탭의 전체 준비 비용

대상: DiscoverySnapshotController.repository / DiscoverySnapshotLiveFilter.filter / MemoryBrowserEntry.build. 첫 발견기록 source 준비는 이번 최종 기기 관측944ms로 남아 있다. 전체 live filter, 앨범 확인, 모음 집계를 피하는 저장된 요약 우선 표시/검증된 revision 기반 공용 준비 결과를 다음 성능 과제로 둔다. 먼저 상세 source의 세부 비용을 나누어 계측하는 것이 필요하다. 이번 해석 캐시만으로 전체 첫 진입 해결로 처리하지 않는다.

### Follow-up — 승인 문구의 적용 범위

Antigravity의 REVIEW_Discovery_Tab_Load_Antigravity_2026-10-19.md 및 REVIEW_Memory_Detail_Entry_Antigravity_2026-10-19.md는290-test 시점 코드 반영·조건부 출시 승인을 남겼다. “0ms 즉시” 및 “UI 프리징 완전 해소” 표현은 이번 기기 로그로 입증되지 않는다. worker source를 재사용해도 revision 확인/UI 대기/binding이 남는다. 이번300-test 스냅샷 변경과 영상 탭 변경은 별도 리뷰 범위이며 과거 승인으로 대체하지 않는다. 원문은 수정하지 않는다.

## 빌드 / 기기 상태

최종 `PhotoPlace-1.3.6-device-tested-snapshot-cache-20261019-debug.apk`를 Drive Codex APK에 복사.9,315,455bytes, 빌드 파일시간22:11:22 KST. SHA256 `AFDF0DF39C0F61793680476594325EAE2381C074BC7C97DE020DC85F9FF63F54`. 로컬 빌드/Drive 사본/기기에서 다시 pull한 설치 APK 모두 동일. version1.3.6/code36/debug 유지. 기기에는 최종 APK를 데이터 보존 업데이트하고 PhotoPlace 홈으로 복귀해 뒀다. 이전 Drive APK 보존. 커밋/푸시/스토어 출시 없음.

증거는 Git 제외 `.debug/device-20261019/`의 baseline-timings.log, final-timings.log, video-launches.log 및 data-before/data-final.sha256에 있다. 파일에는 기기 정보가 포함될 수 있어 리뷰 문서에 원본 전체를 복제하지 않는다.
