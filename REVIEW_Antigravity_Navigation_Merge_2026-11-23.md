# Antigravity 추가 수정 리뷰 — 발견 기록 통합 / 동영상 병합 (2026-11-23)

## 판정

**Pre-release 보완 1건: 통합 상단 개수와 실제 카드 개수 불일치. 조건부 출시의 ‘특이사항 없음’에 해당하지 않으므로 이번 리뷰에서 Play 제출하지 않았다.**

확인한 변경에서 신규 MainActivity 판단 로직이나 파일 이동/삭제 코드는 발견하지 않았다. 동영상 전용 그룹의 GPS 증거를 복구 병합에 포함한 방향은 적절하다. 아래 개수 오류는 파일 손상이나 사진 증발이 아니라 표시/집계 문제다.

## 대상과 기준

- 사용자 요청: 안티 추가 수정 리뷰 후 특이사항 없으면 출시 검토. 이후 최신 테스트 APK로 전체/위치 앨범 전환, 앨범 열기, 사진·동영상 보기, 뒤로가기를 폰에서 확인했고 괜찮다고 답했다.
- 기준 APK: 1.3.47(83) Debug, SHA-256 `182263E48E82849ED7557D9E2D3FA3F525B184EEAB52DDA261C2934BEF1B18D4`. 현재 로컬 Debug APK가 안티 WORKLOG/Drive 기록의 해시와 일치한다.
- 변경 중심: RecoveredAlbumLinkResolver.java, RecoveredAlbumGeoReader.java, MemoryRepository.java, MemoryMediaCounts.java, RecoveredAlbumLinkResolverTest.java 및 기존 3탭·공통 상세 연결.
- MainActivity는 11/22 내비게이션 작업 시작 전 임시 사본과 비교하여 신규 바인딩/위임 변경을 확인했다. 안티의 11/23 추가 수정은 위 GPS·병합·개수 파일에 있고 MainActivity에 새 판단 분기를 추가하지 않았다.
- [설계](DESIGN_Discovery_Centric_Navigation_2026-11-22.md), [기존 구현 리뷰 요청](REVIEW_REQUEST_Discovery_Centric_Navigation_Implementation_2026-11-22.md), [전날 WORKLOG](WORKLOG_2026-11-22.md).

## P0 / P1 우선 확인

- P0: 확인한 내비게이션 변경의 MainActivity 추가분은 필터/클릭 바인딩, 전용 로더 호출과 결과 바인딩, 상태 저장·복원 연결이다. 신규 필터·병합·개수·GPS 판단은 전용 클래스에 있다. 이 변경에 대한 P0 Hold 사유 없음.
- P1: GPS 복구는 후보가 유일하고 국가가 KR이며 기존 stable ID가 충돌하지 않을 때만 GPS 25km 근접 근거로 연결한다. 수동 장소·예약 경로 제외를 유지한다. 동영상도 이 근거 대조에 참여하도록 확장했다.
- 경로 복구는 읽기 전용 프로젝션이며 파일/MediaStore 수정이나 영구 ID 합치기를 추가하지 않는다. UI 복구 결과를 원본 정리 권한으로 승격하지 않는다.
- 위치 앨범 필터의 PhysicalAlbumDetail은 discoveryGroup을 제거하고 해당 폴더만 읽는다. 공통 뷰어를 쓰더라도 전체의 신규 사진을 앨범 상세에 합치지 않는 구조를 유지한다.
- 기존 openAlbumInSamsungGallery의 bucket ID 기반 특정 앨범 진입 시도와 일반 갤러리 폴백을 재사용한다. 신규 대표 사진 ACTION_VIEW로 대체하지 않았다.

## [Pre-release / P2] 사진·동영상 구분이 없는 기존 앨범을 합치면 상단 수량이 부풀려진다

### 코드 근거

- `MemoryRepository.java:301–304`: mediaSplitKnown=false인 기존 앨범은 itemCount 전체를 photoCount로 임시 간주한다.
- `MemoryRepository.java:350–352`: 이번 변경은 photoCount/videoCount 각각의 최댓값을 구하고 itemCount에 photoCount+videoCount를 추가로 최댓값 비교한다. 서로 다른 출처의 수치가 같은 파일을 포함하는지 확인하지 않는다.
- `MemoryMediaCounts.java:49–50` 부근: MIXED에 대해 위의 임시 photoCount/videoCount를 실제 구분 개수처럼 표시한다.
- `MemoryBrowserSummary.java:29–30`: 상단 요약은 record.itemCount를 그대로 합한다.
- 반면 `MemoryBrowserMediaProjection.build`는 실제 폴더와 발견 사진의 중복 제거 결과를 계산해 카드 수량만 보정한다. 상단 records의 itemCount는 그대로 남는다.

### 재현 및 결과

1. 구분 정보 없는 기존 앨범 이력: 총 100개(mediaSplitKnown=false). 실제 폴더는 사진 90장 + 동영상 10개.
2. 같은 장소 발견 그룹에 그 동영상 10개가 함께 있는 통합 사례를 구성한다.
3. 현재 production 코드의 MemoryRepository → MemoryBrowserEntry.build → MemoryBrowserSummary.from과 실제 미디어 reader 결과를 실행한다.
4. 결과:

| 항목 | 현재 결과 | 실제 |
|---|---:|---:|
| 병합 record.itemCount | 110 | 100 |
| 상단 장소 요약의 합계 | 110 | 100 |
| 실제 미디어를 읽은 카드 | 사진 90장 · 동영상 10개 | 100 |
| 추정 record 라벨 | 사진 100장 · 동영상 10개 | 사진 90장 · 동영상 10개 |

진단 실행 로그: `actualFiles=100, mergedItemCount=110, summaryCount=110`. 진단 소스는 `C:/Users/mismi/AppData/Local/Temp/photoplace-navigation-20261122/AntiCountReviewProbe.java`이며 제품 소스를 수정하지 않고 현재 compiled Debug 클래스를 호출했다. 실기기에서 이 정확한 fixture를 주입한 검증은 아니다.

### 최소 보완 제안

- 분류를 모르는 과거 앨범의 ‘총합’을 ‘사진 수 확정값’으로 취급해 동영상 수와 더하지 않는다.
- 이미 MemoryBrowserMediaProjection에서 구한 중복 제거 후의 실제 개수를 상단 요약에도 재사용한다. 요약 때문에 추가 전체 MediaStore 스캔을 넣지 않는다.
- 실제 조회 실패/권한 제한 시 추정/알 수 없음과 확정 수량을 구분한다. 원본/경로/ID/정렬/뷰어를 변경하지 않는다.
- 회귀 테스트: 구분 없는 mixed 앨범의 중복 동영상, 사진만 있는 앨범 + 신규 동영상(정상 합산), 조회 실패, 일반 순수 사진 앨범. 상단·카드·상세 합계 일치와 캐시 재사용을 확인한다.

## 통과·확인한 부분

- `testDebugUnitTest testReleaseUnitTest` 재실행 성공(기존 결과 UP-TO-DATE). XML Debug 596 / Release 596, 각각 실패 0·오류 0. 기존 테스트 통과와 위 누락된 경계 사례는 구분한다.
- 안티 신규 회귀 테스트는 GPS reader를 stub으로 넣어 ‘동영상 그룹도 GPS 비교 대상이 된다’는 resolver 동작과 사진127+신규동영상1의 알려진 구분 수량 사례를 검증한다. 실제 MP4 메타데이터/캐시/권한 경로 자체를 검증하는 테스트는 아니다.
- Android 공식 문서는 동영상 위치 조회에 MediaMetadataRetriever + METADATA_KEY_LOCATION 사용을 안내한다. 새 fallback에서 일반 video URI를 사용하는 것 자체를 결함으로 판단하지 않았다. [공식 동영상 위치 메타데이터 안내](https://developer.android.com/training/data-storage/shared/media#location-info).
- 사용자의 최신 APK 실기기 기본 흐름 확인 완료. 정확히 어떤 안동/청주 카드가 합쳐졌는지나 위 숫자 경계 사례 확인까지 포함했다고 확장 해석하지 않았다.
- git diff --check에서 오류 없음(LF→CRLF 안내만 존재).

## 검증 한계와 후속 항목

- 안티가 기록한 `.debug/discovery_filter_screen.png`는 FF FE로 시작하는 UTF-16 변환 데이터여서 정상 PNG로 열리지 않았다. 해당 파일을 실제 화면 시각 검수의 근거로 사용하지 않았다. 다음 캡처는 adb shell screencap 파일 생성 후 adb pull로 가져오는 방식 권장.
- 11/22 Codex가 직접 확인한 전체/앨범 필터 및 공통 사진 뷰어 렌더와 이번 사용자의 최신 실기기 확인은 별도 근거다. 이번 안티 변경은 레이아웃 변경이 아니다.
- GPS cache miss의 실제 MP4 및 복구 성능은 별도 실제 파일 검증이 있으면 좋다. 모든 원본 메타데이터 재조회에 예외를 삼키는 구조이므로 stub 단위 테스트만으로 실제 기기 병합 성공을 보장하지 않는다.
- 출력 선명도 추가 개선은 기존 사용자 결정대로 이월. 이번 리뷰에서 해당 기능을 해결 판정하지 않았다.
- 코드/버전 변경, 새 APK/AAB 생성, Drive 교체, Play 제출 없음. 원래 조건부 출시 요청의 조건이 충족되지 않아 제출을 보류했다.
