# 발견 장소 수량 문구·NEW 열람 확인·빠른 조회 안내 수정

Codex, 2026-10-19. 상태: **309 tests / debug build PASS**, 연결폰 일부 검증 완료, 외부 리뷰 전.

## 사용자 요청 / 동작

1. 송파구 약300개도 로딩 안내가 뜬다. 실제 대기가 짧을 때까지 항상 안내를 띄우던 동작을 개선한다.
2. 성남은 “사진·동영상2107개”, 수원은 사진/영상 분리 표기로 다르며 이 문제가 미수정이었다. 발견한 장소 카드도 실제 상세와 같은 소스로 사진/영상을 센다.
3. NEW가 열람 후 남는 이유를 설명했고, 사용자 선택으로 **해당 장소 상세가 정상적으로 열리면 NEW/이번에+N 해제, 새 분석 버전의 새 사진이 생기면 재표시**하도록 변경했다. 로딩 취소/실패/원본목록 미해석 시에는 해제하지 않는다.

## P0 / P1

- MainActivity는 두 렌더 콜백에 준비된 MemoryBrowserState 인수를 전달하고, 검색 결과에 준비된 item을 선택하도록 바인딩만 변경했다. 신규 수량/정렬/열람 판정 로직은 별도 클래스에 있다.
- 원본 미디어/MediaStore/발견 스냅샷/OrganizationLink/날짜 메모/identity 원본 수정 없음. 기존 메모 stableMemoryId+dateKey 체계를 변경하지 않는다.
- 열람 상태만 신규 SharedPreferences `memory_seen_versions.xml`에 저장한다. 사용자 승인한 NEW 해제 기능에 필요한 별도 데이터이며 기존 미디어 캐시의 revision에 포함하지 않는다.
- 기기 보호JSON7개(album_summary_history/discovery_snapshot/memory_collections/memory_date_notes/memory_identity_registry/memory_organization_links/memory_personalization)는 이번 테스트 시작/종료 해시 동일. 새로운 열람 preferences가 변경되는 것은 의도된 동작이다.

## 구현 근거

### 실제 수량 / 사진·영상 문구

- `MemoryBrowserMediaProjection.java:17`: discoveryGroup이 있는 장소 카드에 MemoryMediaResolver의 실제 refs를 적용하고 MemoryMediaCounts.fromRefs로 센다. 기존 resolver의 exact link 우선·legacy fallback·URI/파일명 교차 중복 정책은 바꾸지 않는다. 조회 실패는 “불러오기 필요”, 재사용 기준점 금지다.
- `MemoryBrowserMediaProjection.java:25`: discoveryGroup이 없는 순수 legacy album 행은 기존 추정 집계를 유지한다. 이 행들은 사진/영상 구분 정보가 없어 generic 합계가 남을 수 있다. 관련 없는 해외 앨범 전체를 장소 카드 수량 때문에 매번 조회하지 않는다. 모음 멤버의 실제 조회는 기존 모음 요약 정책대로 수행한다.
- `MemoryBrowserMediaProjection.java:38`: 한 browser entry 준비 안에서 path+memory key별 Gallery 결과를 장소 카드와 모음이 공유해 중복 쿼리를 피한다. 이 memo는 화면 준비 동안만 사용한다.
- `MemoryBrowserEntry.java:44`: worker에서 수량 projection과 모음 요약을 함께 준비. MemoryBrowserState.withCounts로 실제 표시 모델에 전달.
- `MemoryBrowserState.java:53`, `MainActivity.java:9150`: 검색/모음 멤버 제외 후 예전 records에서 item을 다시 만들던 경로를 제거하고 prepared.retainingRecords를 사용한다. 수량뿐 아니라 NEW 해제 상태도 검색 결과에서 유지한다. 장소 소속/검색 조건 자체는 바꾸지 않는다.

### 열람한 NEW 해제

- `MemorySeenPolicy.java:10`: key는 discoveryGroup.memoryKey. 같은 발견 그룹이 MIXED/path형 메모리와 discovery형 메모리로 나타나도 동일한 열람 상태다. 앨범 경로 대신 발견 그룹 alias에 연결하며 새 stable identity를 만들거나 기존 alias를 수정하지 않는다.
- `MemorySeenPolicy.java:14`: group.snapshotVersion이 확인한 버전 이하이면 최근 추가0. 더 새 버전이면 해당 분석에서 최초 등장한 유효 고유 URI 수를 표시한다. stale/빈 URI/중복을 제외한다. 기존의 “최근 분석 배치” 범위는 유지하며 모든 미열람 옛 배치의 누적 개수를 새로 정의하지 않는다.
- `MemorySeenPolicy.java:27`, `MemoryDetailLoadController.java:31`: 정상 상세 바인딩 콜백이 끝난 뒤 mediaResolved+canOpenPhotos인 경우만 acknowledge. 취소/이탈/조회 실패는 기존 loader guard 때문에 callback에 도달하지 않는다. null/불완전/빈 목록은 policy에서 제외한다.
- `MemorySeenStore.java:16`: 별도 prefs에 확인한 버전의 최댓값을 apply. 앱 재실행 후에도 유지. 분석 소스 캐시와 독립적이다.
- `MemoryBrowserEntry.java:24`, `MemoryBrowserLoadController.java:32`: source cache가 재사용되더라도 화면 전달 시 최신 열람 버전만 덧씌운다. 본 장소의 NEW와 “이번에+…”는0으로 되고 기존 NEW 우선 정렬에 따라 위치가 내려갈 수 있다. 미디어 전체를 재조회하여 표시를 지우지 않는다.

### 로딩 안내 유예

- `DelayedLoadingNotice.java`: 취소/새 요청/빠른 완료가 이미 큐에 들어간 늦은 안내 callback을 무효화한다.
- `MemoryScreenLoadController.java:32`: 최초에 예정했던180ms보다 여유를 두어 최종 **250ms** 이후에도 같은 요청/원래 화면이 유효할 때만 안내를 붙인다. 완료/오류/취소/닫기 때 callback과 안내를 제거한다.
- 빠른 warm 조회의 불필요한 안내를 줄이는 UI 개선이다. 실제 첫 조회의 source 시간을 없애거나 사진 수300개 이하면 무조건 안내가 안 뜬다는 뜻은 아니다. 큰 장소든 작은 장소든 실제250ms를 넘기면 안내가 뜬다.

## 자동 검증

`gradlew.bat testDebugUnitTest assembleDebug`: 최종 **309 tests / 0 failures / 0 errors**, build PASS. `git diff --check` PASS. 기존 lint 전체 통과를 새로 주장하지 않는다.

이번 신규9건:

- DelayedLoadingNoticeTest2: 빠른 완료/취소 후 큐 callback도 표시 금지, 지연 요청 표시·새 요청이 이전 표시 무효화.
- MemoryBrowserEntryTest3: MIXED/발견 전용 문구 및 카드/상세 일치(중복 URI 제외, 저장 합계는 보존), 실패한 MIXED 조회의 unknown 표시/캐시 금지, 모음과 실제 Gallery 결과 공유. 기존 scope 테스트를 확장해 무관한 순수 legacy album을 카드 때문에 추가 조회하지 않음을 확인.
- MemoryBrowserEntryTest4: 캐시된 entry에서 한 장소 NEW만 해제/총수량 보존, 새 버전 새 사진으로 NEW 재등장+stale/빈 URI/중복 제외, MIXED/발견 전용 동일 열람key, 정상/실패/빈 상세의 acknowledge 자격. 검색/가시 행 선택에서도 준비된 수량·NEW 유지 검증을 포함한다.

## 실기기 확인

SM-F971N, 펼친 화면(2448px 폭)에서도 확인. 화면이 사용자의 조작으로 바뀌는 중에는 자동 조작을 중단하고 사용자의 “지금부터 잠깐 테스트해줘” 응답 후 계속했다.

- 카드: 성남 **사진2252장·동영상31개**, 수원 **사진854장·동영상11개**, 송파구 **사진339장·동영상7개**. 전까지 성남에 남았던 generic2107 표시는 발견 카드에서 제거됨.
- 송파구 상세: 사진339/영상7로 카드와 일치. 정상 진입 후 prefs에 해당 발견 그룹 버전 저장 확인.
- 프로세스 재시작→발견기록→2017 검색에서 송파구 카드의 NEW/이번에+288이 없고, 용인/Tsushima 등 아직 안 본 장소의 NEW/추가 수가 남음을 UI hierarchy로 확인. 검색 필터를 닫아 원복했다.
- 열람 후 탭 sourceMs5 및4ms/reused=true. NEW 해제를 위해 source cache 전체를 무효화하지 않음. 첫 탭은 최종922ms, 재시작1093ms로 여전히 비용이 있다.
- 이번 송파구 첫 상세는506ms였으므로 실제 대기가 남고250ms 안내가 뜨는 것이 정상이다. earlier221ms 샘플과 달리 속도 편차가 존재한다. 시간은 source/bind 로그이며 실제 화면 첫 프레임 지연과 동일하지 않다.
- 실기기의 새 GPS 사진 촬영→분석으로 NEW 재등장하는 과정과 의도적 Provider 실패/권한 취소는 이번에 수행하지 않았다. 해당 정책은 단위 테스트로 검증했다. 미디어를 삭제/손상시켜 시험하지 않았다.

## 잔여 사항

### Pre-release

새 GPS 사진 추가 후 이미 읽은 장소에 NEW가 다시 붙고, 다시 정상 열람하면 사라지는 실제 검증 필요. API29/부분 권한/Provider 실패/화면 회전·pause 중 source 진행 시 표시 및 acknowledge 시점 외부 리뷰 필요. 열람prefs key는 현재 discoveryGroup alias에 따르므로 향후 canonical discovery key migration 시 열람 상태 이전도 함께 설계해야 한다.

### Follow-up

발견 카드/상세 수량 표시가 같은 resolver 결과로 통일됐지만, 성남 추가176개의 실제 source/파일 동일성과 기존 파일명 기반 중복 정책의 정확성까지 검증한 것은 아니다. 단순 Math.max→합산으로 해결하지 않는다. 순수 legacy album의 사진/영상 미분리 표시는 별도 남는다. 첫 탭 전체 준비와 첫 상세 source 시간은 남아 있으며 추가 count query가 필요한 MIXED 장소가 많으면 비용이 늘 수 있다. 이번에 모든 legacy 앨범을 카드용으로 조회하던 시도는 첫 탭2410ms를 관측해 최종 코드에서 제외했다.

## APK / 외부 리뷰 요청

폰에는 최종 빌드를 데이터 보존 설치했다. `G:\내 드라이브\Codex APK\PhotoPlace-1.3.6-counts-seen-feedback-20261019-debug.apk`,9,323,443bytes, 빌드22:25:50 KST,version1.3.6/code36/debug. SHA256 `894FAD6975C1C62019F78F116C5A59F4AFC7280019277F647F0F4FE17D95BA65`, 로컬/Drive/기기에서 pull한 설치본 일치. 기존 APK 유지, 커밋/스토어 배포 없음. 기기 증거 `.debug/device-20261019/seen-persist-ui.xml`,counts-seen-timings.log,counts-before/after.sha256는 Git 제외.

이 보고서와 위 변경 클래스/테스트를 검토하고, 전체 결과는 루트 `REVIEW_Discovery_Counts_Seen_Feedback_Antigravity_2026-10-19.md`에 저장해 주세요. P0/P1을 먼저 검사하고, 각 발견 사항에 Blocker / Pre-release / Follow-up, 파일·줄, 재현, 영향, 수정 제안을 포함해 주세요. 이전300-test 승인 범위와 이번309-test 패치를 구분해 주세요.
