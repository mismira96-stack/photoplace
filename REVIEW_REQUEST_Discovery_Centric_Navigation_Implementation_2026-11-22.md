# 안티 리뷰 요청 — 발견 기록 통합 및 앨범 공통 뷰어 (2026-11-22)

## 승인 범위와 현재 상태

사용자가 홈/발견 기록/설정 3탭, 발견 기록 안의 전체/위치 앨범 필터를 승인했다. 이어 ‘이번에 발견 기록 상세·뷰어도 함께 사용’을 선택했다. 구현·Debug 단위 테스트·APK 빌드 완료, 출시하지 않았다. 실기기 확인은 사용자 대기다.

이전 [설계 리뷰](REVIEW_Discovery_Centric_Navigation_2026-11-22.md)와 [확정 범위를 추가한 설계](DESIGN_Discovery_Centric_Navigation_2026-11-22.md)를 함께 읽어주세요. 기존 작업 트리에 다수 변경이 있으므로 전체 Git diff를 이번 변경으로 오인하지 말아주세요.

## 구현

- DiscoveryTabDestinations / BottomNavigationRenderer: 표시 순서 [홈0, 발견1, 설정3], 기존 위치 앨범 목적지2는 발견 기록 탭으로 선택 표시. 기존 목적지 ID와 back stack은 재번호화하지 않았다.
- DiscoveryFilterRenderer: 전체/위치 앨범 48dp 터치 영역, 선택 배경·접근성 상태. 기존 전체 목록과 실제 앨범 목록을 각각 호출한다. 필터 수량 조회나 새 스캔을 추가하지 않았다.
- PhysicalAlbumDetail: 정확한 relativePath만 조회, discoveryGroup을 제거해 신규 사진 합류 차단. 가능한 경우 기존 통합 목록의 해당 경로 memoryKey를 재사용해 메모·개인 장소 식별 관계를 유지한다. 캐시가 없으면 기존 앨범 path 식별자를 사용하며 새 영구 별칭을 쓰지 않는다.
- DiscoverySnapshotController: PhysicalAlbumDetail 결과에 기존 날짜 메모를 투영한다. 앨범 하나를 열기 위해 전체 repository를 다시 만들지 않고 이미 로드된 MemoryBrowserEntry.records만 재사용한다.
- MemoryDetailLoadController: physical-album|경로 별 기존 revision 기반 3개/10000 URI 캐시, 앨범 범위 재열기, 목록 복귀와 선택 탭 목적지, 보조 액션 관리. 앨범만 열었다고 전체 발견 사진을 읽음 처리하지 않는다.
- PhysicalAlbumActionsRenderer: 갤러리에서 보기 / 앨범 관리. 갤러리는 기존 bucket ID 기반 openAlbumInSamsungGallery를 재사용한다. 앨범 관리는 기존 상세 관리 화면을 유지한다.
- MemoryDetailSummaryRenderer: 수행할 앨범 추가 동작이 없으면 비어 있는 더보기 버튼을 숨긴다.
- MainActivity: 필터·카드 클릭 바인딩, 공통 상세 로딩/복귀 위임, scroll 상태 저장·복원 연결만 추가. 신규 필터/파일/병합/소스 판단 if문은 추가하지 않았다. 첫 MainActivity 원본 사본은 C:/Users/mismi/AppData/Local/Temp/photoplace-navigation-20261122/MainActivity.java에 있다.
- UI_GUIDE에 필터·공통 상세 패턴 기록. 버전83/1.3.47, 원본/폴더/별칭/메모 저장 구조는 유지한다.

## 검증 결과

- 최종 testDebugUnitTest 595개, 실패/오류 0. assembleDebug 성공.
- 추가 테스트 6개: 설정 목적지 유지, 위치 앨범의 발견 탭 선택 표시, 통합 목록2장/물리 앨범1장 범위 분리와 identity 유지, 읽기 실패 시 발견 사진 fallback 금지, 동영상 전용 앨범, 동명 다른 폴더 및 발견 캐시 없는 앨범.
- 최초 에뮬레이터 렌더: 3탭, 전체25곳/앨범24개(합성 fixture), 두 필터 선택 상태, 앨범에서 공통 상세 진입(사진1장), 공통 사진 뷰어1/1 및 스토리 동작 진입 아이콘 확인. 실제 사용자 사진 검증이 아니다.
- 재설치 후 에뮬레이터 시스템 자체의 지연/설정 앱 시작 지연이 발생해 재부팅 복구 중이다. 최종 재설치 후 전체 스모크 결과는 WORKLOG의 후속 기록을 우선한다. 모든 실기기 검증이 완료됐다고 간주하지 않는다.
- APK: C:/work/outputs/PhotoPlace/navigation-2026-11-22/PhotoPlace-1.3.47-navigation-test.apk
- SHA-256: DA0B906EE019CC40DC831694B984866071585E5A8B4084D33EE99CDF52DC499D.
- 이 APK에는 오늘 앞서 로컬에 있던 미출시 입력 디코딩 보강 패치도 포함돼 있다. 이번 내비게이션 작업에서 스토리 화질을 추가 수정하거나 해결 판정하지 않았다.

## 리뷰 우선순위

1. P0: 신규 MainActivity 코드가 바인딩/위임 범위를 넘지 않는지 먼저 확인.
2. P1: 탐색만으로 파일/MediaStore/ID를 바꾸지 않는지, 앨범 선택·개인 장소 편집 이후 scope와 stableMemoryId/dateKey 메모가 유지되는지.
3. 통합 목록의 동일 장소 병합은 기존 MemoryRepository 정책이다. 국가/이름/복수 폴더 모호한 항목을 억지로 합치지 않는지.
4. PhysicalAlbumDetail의 앨범-only 범위, 권한 실패/폴더 삭제, 동영상-only, cache 없는 경우와 alias 연결을 확인.
5. 비동기 로딩 중 뒤로/탭 전환, 앨범→뷰어→복귀, 메모 편집 후 재로딩, 갤러리 복귀 시 범위 유지, 앨범 관리→뒤로, 설정 목적지와 선택 표시 확인.
6. 캐시/핀치/기억 모으기/우리집·회사/선택 공유 회귀와 3·4열 좁은 화면·폴드 렌더 검증.

## 범위와 남은 확인

- 위치 앨범 필터의 카드 클릭을 공통 상세로 바꿨다. 기존 홈/해외 앨범 바로가기와 관리 화면의 모든 내부 동선을 전면 교체하지 않았다.
- 위치 앨범 목록의 최초 조회·검증 방식은 기존 동작을 유지했다. 전 목록에 대한 새로운 영속 캐시나 성능 개선 프로젝트로 확대하지 않았다.
- 삼성 갤러리 해당 앨범 진입·복귀, 실제 사진/동영상 공유·저장, 실제 개인 장소·기억 모으기·날짜 메모, 폴드 핀치, 대량 앨범은 사용자 실기기 확인이 필요하다.
- 실제 수정 제안/수정 후에는 WORKLOG와 리뷰 문서에 남겨주세요. 사용자 확인 전 Play 제출 금지.
