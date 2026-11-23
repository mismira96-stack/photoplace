# Gemini 성능 개선 5개 제안 — 코드/기기/공식 문서 대조

Codex, 2026-10-19. 사용자가 붙여 준 Gemini 제안을 검토한 문서다. **방향 및 단계적 분리에 동의하되, 예상 수치를 검증된 결과로 받아들이지 않는다.** 이번 검토에서 성능 제안1~5를 추가 구현하지 않았다. 제품 최신 상태는 Back 복원 포함316-test 빌드이며, 제안서의309-test 시점 이후 수량/NEW/레이아웃/Back 변경은 각각 별도 검증 범위다.

## 근거와 계측 한계

현재 폰의 실제 snapshot을 읽기 전용으로 집계:3,664,538bytes /37groups /5,015refs /고유 photo ID4,805 + video ID210. DiscoverySnapshotLiveFilter의 QUERY_CHUNK_SIZE500이므로 MediaStore 조회는 `ceil(4805/500)+ceil(210/500)=11`회다. 사진5천개를 다루는 것과 쿼리5천회를 실행하는 것은 다르다. 다른 앱의 미디어 변경/시스템 성능에 따라 비용은 변한다.

현재 PhotoPlaceBrowser의 sourceMs는 snapshot 읽기/해석·앨범 확인·live filter·repository 및 모음/카드 수량 준비의 합이다. LiveFilter 자체 duration/query count별 시간은 아직 측정하지 않았다. 따라서 “944ms 중600~700ms를 이 부분만으로 줄임”은 **가설**이다. sourceMs/bindMs와 실제 첫 프레임 시간도 다르다. 숫자 표기1618ms/50100개/12만 등은 원문의16~18ms/50~100개/1~2만과 같은 구간 표기일 수 있어 붙은 숫자를 문자 그대로 단정하지 않는다.

## 1. Generation 기반 LiveFilter 재사용

**판정: 다음 성능 업데이트의 우선 후보. 단순 generation 비교 한 줄로 첫 실행 생략은 불가.**

대상: `HomeRevisionReader.java:20`, `DiscoverySnapshotLiveFilter.java:24`, `DiscoverySnapshotController.java:170`, `MemoryDetailLoadSession.java:37`.

이미 HomeRevisionReader는 전체 권한/API30+에서 외부 볼륨별 version+generation과 내부 데이터 revision을 조합한다. 브라우저/상세 화면 재진입은 이 키가 같으면 source 전체를 재사용하고 있다. LiveFilter 전용 재사용이 아직 없는 점은 제안이 맞다.

[Android MediaStore 공식 문서](https://developer.android.com/reference/android/provider/MediaStore#getGeneration(android.content.Context,%20java.lang.String))는 generation 비교 전에 version이 유지되는지 확인하고, version이 바뀌면 전체 동기화를 하도록 요구한다. 동일 generation은 MediaStore 상태 재사용의 근거이지 아직 provider에 반영되지 않은 파일 변경, 앱 권한/선택 범위, 내부 정책까지 모두 같다는 절대 증거로 표현하면 안 된다.

필요한 조건:

- 볼륨 목록/version/generation, 전체 접근 권한과 가시 범위, 원본 snapshot 내용/revision, 제외할 organized relative paths와 분석/필터 정책이 모두 맞을 것.
- 직전 검증이 성공했고 검증 전후 revision이 안정적일 것. 현재 filter는 Provider 예외 시 raw snapshot을 그대로 반환하므로 성공 여부를 따로 전달하지 않으면 실패 결과에 “검증 완료” 키가 생길 수 있다.
- raw snapshot과 검증 후 filtered snapshot을 구분할 것. 현재 삭제/organized-path 항목을 제외한 결과는 메모리 projection이다. 다음 프로세스에서 token만 읽고 raw snapshot을 반환하면 전에 걸러진 항목이 부활할 수 있다.
- 실제 cold process에서도 생략하려면 token에 대응하는 검증 결과/요약을 안전하게 저장·검증해야 한다. 메모리 캐시만 추가하면 같은 프로세스 안의 다른 화면 준비는 빨라져도 완전한 cold start 첫 검증은 남는다.

필수 테스트: version 리셋/볼륨 교체/권한 변경/앨범 경로 변경/snapshot 변경/검증 실패/검증 도중 추가·삭제/프로세스 재시작 때 이전 제외 항목 부활 차단.

## 2. 홈 표시 후 프리페치

**판정: 가능하고 체감 효과 후보. 1번과 별도로 구현·검증할 작은 단계.**

대상: `HomeDashboardController.java:48`, `MemoryBrowserLoadController.java:28`, `MemoryScreenLoadController.java:52`, `MemoryDetailLoadSession.java:32`.

현재 browser loader의 load는 현재 화면 origin/listener/진행 안내/ready 화면 전환과 묶여 있다. 이 함수를 홈에서 그대로 호출하면 “조용히 준비”가 아니라 발견기록 화면으로 전환될 수 있다. 세대 취소 모델상 warm 중 탭 터치를 새로운 요청으로 실행하면 기존 준비를 버리고 같은 무거운 작업을 다시 큐에 넣을 수도 있다.

먼저 UI 없는 prepare API와 같은 key/revision의 in-flight 작업 공유를 분리할 필요가 있다. 홈 첫 프레임을 표시한 뒤 한 번 준비하며, 탭 터치는 같은 작업에 합류하도록 한다. 최신 source/권한 변경과 foreground 이탈을 처리하고, 준비만으로 NEW를 열람 처리하지 않아야 한다. 홈/사진 분석 worker와 CPU·MediaStore 경합도 확인해야 한다.

프리페치는 계산을 없애는 것이 아니라 사용자가 누르기 전에 진행하는 것이다. 준비가 끝나기 전에 바로 누르면 대기는 남는다. “첫 터치에서도 항상2~5ms/0ms” 보장은 불가하다.

필수 테스트: warm+tap source1회, 준비만으로 화면전환/NEW해제 없음, 준비 전·도중 데이터 변경, 홈 첫 프레임/터치 반응성 악화 없음, 실패 후 tap 재시도, 회전/pause/destroy 후 늦은 callback 차단.

## 3. RecyclerView

**판정: 별도 UI 변경 과제. 이번 마감에 묶지 않음.**

대상: `MemoryBrowserGridRenderer.java:53` 및 상세 날짜/더보기 렌더 경로. [RecyclerView 공식 문서](https://developer.android.com/develop/ui/views/layout/recyclerview)는 필요한 View를 만들고 화면 밖 View를 재사용하는 구조를 설명한다. 현재 전체 카드 생성 비용을 줄일 방향은 타당하다. 다만 adapter 데이터/스냅샷/source 쿼리까지 모두 O(1)이 되는 것은 아니고, 보이는 카드 수도 화면 크기/레이아웃/프리페치 풀에 따라 달라진다.

모음 헤더, 검색, NEW 정렬,2/3열 폴드 배치, 썸네일 취소, Back anchor, 날짜 메모/더보기 등을 함께 검증해야 한다. 지금 우세한 source 준비 지연과 View binding 지연은 분리해서 계측해야 한다.

## 4. 요약 색인/장소별 저장 또는 Room

**판정: 중장기 저장 구조 변경. 별도 마이그레이션 작업.**

대상: `DiscoverySnapshotStore.java:27`, `DiscoverySnapshotDecodeCache.java:14`, `DiscoverySnapshotController.java:170`.

이미 동일 실제 파일 내용을 재해석하지 않는 캐시와16K char 읽기가 들어 있다. 전체 내용을 읽는 비용은 여전히 남는다. 카드용 요약/장소별 목록 또는 DB 인덱스로 필요한 장소만 읽는 방향은 유효하다. 하지만 다중 파일의 snapshot version 원자성, 부분 저장/backup 복구, JSON→DB rollback, media URI/alias/stableMemoryId/dateKey 보존을 검증해야 한다. 현재 저장 추정 합계를 단순 색인에 옮기면 기존 수량 문제가 그대로 남으므로 실제 resolved count의 기준 revision도 설계해야 한다.

## 5. Downsampling / RGB_565

**판정: fallback 경로에 이미 적용됨. 일괄 강제 전환은 보류.**

대상: `MainActivity.java:7474~7498`, `MemoryViewerImageLoader.java:129~151`. Provider loadThumbnail이 우선이며 stream fallback에 inSampleSize+inPreferredConfig RGB_565가 있다. 뷰어는 별도 worker/16MiB byte LRU/요청 크기 제한도 적용돼 있다.

[Bitmap.Config 공식 문서](https://developer.android.com/reference/android/graphics/Bitmap.Config#RGB_565)는 RGB_565의 픽셀 저장이2bytes, ARGB_8888이4bytes이며 색 정밀도/투명도 제약과 화질 아티팩트 가능성을 설명한다. 같은 크기로 실제 해당 형식에 디코딩된 픽셀 저장량의 차이이지 앱 전체 메모리·GPU·이미지 캐시·동시 decode peak까지 정확히50% 줄어든다는 보장은 아니다. Provider bitmap은 앱의 fallback 옵션으로 강제되지 않고, 사후 copy 전환은 순간적으로 두 bitmap을 유지할 수 있다. 실제 allocation·peak·색상/HDR·투명도·OOM 근거를 먼저 계측한다.

## 이번 버전과 다음 단계 권고

이번 버전에는 지금까지의 수량·NEW·영상 경로·로딩 배치·Back 위치 복원을 추가 검증하고, 최신 패치 외부 리뷰와 실제 GPS 새 사진→NEW 재등장 확인을 마무리하는 것을 권한다. 테스트316개 통과만으로 이 실기기/릴리스 게이트가 대체되지는 않는다. 공개 배포는 실행하지 않았다.

다음 단계는 **세부 비용 계측→검증된 LiveFilter/준비 결과 재사용→단일 in-flight 프리페치** 순서. RecyclerView와 저장 구조 변경은 독립 과제로 분리한다. 1·2 구현 자체가 불가능하거나 매우 큰 작업이라는 뜻은 아니며, 실패/변경·cold 재사용 검증을 생략한 막판 추가를 권하지 않는 판단이다.

## 발견 사항 분류

- Pre-release: LiveFilter600~700ms/첫진입0ms/앱 메모리 정확히50% 절감은 미검증이므로 출시 문구나 성능 보장에 사용하지 않는다. 근거 위치 및 수정 제안은 위1/2/5에 명시했다.
- Follow-up: LiveFilter 성공 여부·검증 결과 저장/무효화 설계, 프리페치 중복 요청 합류, RecyclerView/저장 마이그레이션/실제 bitmap allocation 계측.
- 본 검토는 제안 평가이며 새로운 제품 코드 승인/5개 항목 구현 완료 보고가 아니다. 기존 리뷰 원문은 덮어쓰지 않는다.
