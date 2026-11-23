# PhotoPlace 퀵뷰티 원본 촬영일·GPS 연결 상세 리뷰 — 2026-11-21

## 판정

**Hold — 아래 Blocker/P0 및 Pre-release 항목 보완 후 재리뷰.** 이번 요청은 리뷰이며 제품 코드·템플릿·APK·원본 파일을 변경하지 않았다.

## 범위 및 변경 근거

- PhotoPlace: `QuickBeautySourceLink.java`, `MediaMetadataReader.java`, `PersonalPlaceGpsReader.java`, `RecoveredAlbumGeoReader.java`, 관련 캐시·테스트·MainActivity의 퀵뷰티 제외 조건.
- 마커 생성 계약: `C:/work/projects/QuickBeauty/app/src/main/java/com/mismira/quickbeauty/BeautyBitmapUtils.kt`와 해당 저장소 WORKLOG의 2026-11-21 연동 기록을 읽기 전용으로 대조했다. 연동 변경 주체·파일 목록은 그 WORKLOG에 기록되어 있다.
- Git 기준점이 오래되어 diff에 기존 변경이 많이 섞여 있다. 이 리뷰는 퀵뷰티 연동 경로로 범위를 제한했으며, 다른 과거 MainActivity 변경을 이번 작업의 문제로 집계하지 않았다.

## 1. [Blocker / P0] MainActivity에 폴더 제외 판단 추가

- 근거: `app/src/main/java/com/example/gallerysorter/MainActivity.java:3138–3140`, `isIgnoredAlbumPath`에 `lowerCase.contains("/quickbeauty/")` 추가. Git diff와 QuickBeauty WORKLOG에서 이번 연동 변경임을 교차 확인했다.
- 프로젝트 AGENTS.md의 “MainActivity에 판단 로직 0줄” 규칙에 해당한다. 폴더를 장소 앨범으로 오인하지 않도록 제외하는 기능 자체는 타당하다.
- 최소 수정: 폴더 제외 정책을 전용 순수 Java 정책 클래스로 이동하고 MainActivity에는 호출만 둔다. QuickBeauty 대소문자·정상 장소 폴더·카메라 등 기존 제외 규칙을 회귀 검증한다.

## 2. [Pre-release / P1] 원본 후보의 동일성을 확인하지 않고 첫 GPS를 채택

- 근거: `QuickBeautySourceLink.java:134–148`. 마커의 `src_id`로 URI를 만들고 해당 사진에서 GPS를 읽으면 이름·촬영일 대조 없이 즉시 반환한다.
- 폴백도 `:154–166`, `:170–190`에서 이름만 또는 촬영시각만 조회하며 `_id ASC` 순서의 최대 3개 후보 중 GPS가 있는 첫 사진을 채택한다. 이름이 중복된 경우의 유일성 검사, 다른 메타데이터와의 교차 확인이 없다. 이름 후보가 존재하면 촬영일 폴백 자체가 생략된다.
- 재현 조건(코드 경로 분석): 보정본을 다른 기기로 복사/복원하여 마커 ID가 그 기기의 다른 사진을 가리키는 경우, 또는 원본이 없어지고 같은 이름의 다른 사진만 존재하는 경우. 단말/사진 제공자가 달라져 ID 의미가 바뀌는 경우도 ID만 믿을 수 없다.
- 영향: 다른 사진의 위치가 정상 복원된 GPS로 반환되어 발견 장소·개인 장소·앨범 위치를 잘못 분류할 수 있다. `MediaMetadataReader.java:38,50`은 얻은 위치를 개인 장소 GPS 캐시에 적재하므로 오연결이 남을 수 있다.
- 최소 수정: ID를 후보 힌트로 취급하고 가능한 이름·촬영일·원본 식별 근거를 대조한다. 불일치 후보는 거절하고 이름/촬영일은 함께 비교한다. 증거가 부족하거나 후보가 여러 개면 미해결로 남긴다. 숫자 ID 단독의 마커를 어느 조건에서 신뢰할지 명시한다.
- 필요한 테스트: ID는 존재하나 이름/날짜가 다른 사진, 동명 사진 여러 개, 같은 촬영시각 여러 개, 원본 이동 후 ID 변경, 원본 없음. 잘못된 후보를 임의로 고르지 않아야 한다.

## 3. [Pre-release / P2] 원본 조회의 일시 실패가 영구적인 GPS 없음 캐시로 바뀜

- 근거: `QuickBeautySourceLink.java:198–237`은 읽기 불가·권한 실패·GPS 없음 등을 모두 null로 합친다. `PersonalPlaceGpsReader.java:183–210`은 보정본 자체의 EXIF는 열렸으나 원본 연결이 null이면 `(0,0)`을 반환한다. `:121–145`는 이를 열기 성공/GPS 없음으로 판정한다.
- `PersonalPlaceGpsPoint.java:54–55`에서 NO_GPS_TAG는 디스크 캐시 가능이고, Reader `:65–73`은 해당 캐시가 있으면 연결 조회를 건너뛴다. 원본 접근이 나중에 가능해져도 보정본 URI/이름/날짜가 같으면 재시도하지 않는다.
- 재현 조건(코드 경로 분석): 위치 권한은 있으나 원본 파일이 잠시 읽히지 않거나 원본에 대한 사진 접근이 제한된 상태에서, 읽을 수 있는 보정본을 개인 장소 GPS 조회로 처리한다. 이후 접근 허용/원본 복원 후 같은 보정본을 다시 조회한다.
- 기존 음수 캐시도 주의: 새 마커가 있는 사진을 이전 PhotoPlace에서 먼저 읽어 NO_GPS_TAG로 저장한 경우, 캐시 schema 1 및 fingerprint에 해석기 버전/참조 원본 상태가 없어 업그레이드 후 새 연결 로직을 건너뛸 수 있다. 일반 위치 스캔에서 양성 좌표를 다시 읽으면 덮어쓸 수는 있으나, 개인 장소 캐시 조회 자체에는 자동 회복이 없다.
- 최소 수정: 연결 결과를 FOUND / CONFIRMED_NO_GPS / DEFERRED_OR_DENIED / AMBIGUOUS 등으로 구분한다. 참조가 있으나 해결되지 않은 경우는 영구 음수 캐시를 하지 않는다. 기존 관련 음수 캐시에 대한 제한적 무효화/해석기 버전 전략을 마련한다.
- 필요한 테스트: 최초 원본 읽기 실패→후속 성공, 사진 접근 허용 전후, 원본 복원 전후, 이전 버전 음수 캐시가 있는 마커 사진.

## 4. [Pre-release / P2] 보정본을 다시 보정하면 원본 연결이 끊김

- 근거: QuickBeauty `BeautyBitmapUtils.kt:310–366`은 현재 선택 URI의 ID/이름으로 sourceMeta를 만들고 `:461–464`에서 새 마커를 기록한다. 기존 사진의 마커를 최종 원본까지 승계하지 않는다.
- PhotoPlace `QuickBeautySourceLink.java:198–237`의 원본 후보 읽기는 직접 GPS만 읽으며 후보 자체의 QuickBeauty 참조를 따라가지 않는다.
- 재현 조건(코드 경로 분석): 원본 O에는 GPS가 있지만 QuickBeauty가 읽은 스트림에는 GPS가 마스킹되어 보정본 A에 마커만 저장된 상태. A를 다시 보정하여 B를 만들면 B는 A를 참조한다. O가 남아 있어도 PhotoPlace는 A의 직접 GPS가 없으면 실패한다. 이름 후보가 A로 존재하므로 현재 날짜 폴백도 실행되지 않는다.
- 최소 수정 선택지: QuickBeauty에서 기존 마커의 최종 원본 연결 정보를 승계하거나, PhotoPlace에서 후보 동일성 검증을 한 뒤 visited 집합과 최대 깊이를 둔 참조 해석을 한다. 무제한 재귀나 날짜가 같다는 이유의 임의 연결은 피한다.
- 필요한 테스트: O→A→B 연속 보정, 참조 순환 A↔B, 중간 보정본 삭제, 최종 원본 없음.

## 유지할 항목 및 정상적인 부분

- 새 원본 연결 로직이 별도 클래스에 모여 있고 파서와 날짜 해석이 분리되어 있다.
- 파일 open은 읽기 전용이며 원본 MediaStore 레코드·파일을 수정하지 않는다. 앱 내부 캐시 저장은 기존 동작이다.
- 사진 자체의 유효한 GPS가 있으면 원본 참조보다 우선한다.
- 참조 후보의 직접 GPS 읽기에도 prepareOriginalUri와 위치 권한 판단을 재사용한다. 위치 권한이 없을 때 Reader가 NO_GPS_TAG를 영구 저장하지 않는 기존 보호도 유지되어 있다. 3번은 위치 권한 이외의 원본 접근 실패를 구분하지 못하는 문제다.
- 파서는 일반 EXIF 설명을 QuickBeauty 마커로 오인하지 않고, 마커가 없으면 기존 EXIF 경로를 사용한다.
- 스토리 템플릿은 이번 리뷰의 수정 범위가 아니다.

## 검증 결과와 한계

- 실행: `.\gradlew.bat :app:testDebugUnitTest --tests '*QuickBeautySourceLinkTest' --tests '*PersonalPlaceTest' --console=plain`
- 결과: 성공. QuickBeauty 파서/날짜 테스트 2개 + 개인 장소 테스트 9개 = 11개, 실패 0.
- 현재 QuickBeauty 테스트 2개는 마커 파싱과 날짜 변환만 검사한다. 실제 ContentResolver 후보 탐색·잘못된 ID/중복 후보·원본 접근 실패 후 재시도·연속 보정은 검증하지 않는다.
- 위 결함은 코드 분기 및 생성자/소비자 계약 대조로 확인했다. 사용자 원본 사진·권한·휴지통·MediaStore를 변경하는 재현은 하지 않았고, 기기 간 오연결/일시 실패의 실기기 재현 테스트는 미실시다.
- 이번 리뷰에서 제품 소스와 테스트를 수정하거나 앱을 재설치/출시하지 않았다.

## 권장 첫 변경 범위

1. MainActivity 폴더 정책 분리와 관련 회귀 테스트.
2. 원본 후보 동일성·모호성 판정 및 실패 상태 구분을 전용 resolver 안에서 보완.
3. 음수 캐시 재시도/무효화와 연속 보정 원본 승계 중 최소 경로를 결정.
4. 4개 결함의 회귀 테스트를 추가하고 사용자 실기기에서 새 보정·재보정·원본 없음 흐름 확인.

기능 의도와 UI·분석 폴더 선택·스토리 템플릿·읽기 전용 원칙은 유지한다. 구현 전 변경 범위를 사용자와 싱크업한다.

---

## 보완 반영 결과 (Antigravity — 2026-11-21)

사용자와 4개 항목 범위를 싱크업한 뒤 아래와 같이 보완 및 테스트를 완료했습니다([작업 기록](WORKLOG_2026-11-21.md)).

1. **[P0] `MainActivity` 폴더 제외 정책 분리 완료**
   - `IgnoredAlbumPathPolicy.java`를 신설해 `DCIM/Camera/`, `/quickbeauty/`, `/screenshots/`, `/kakaotalk/`, `/instagram/`, `/photogrid/`, `/download/`, `Pictures/사진정리` 제외 판단을 모두 이동하고, `MainActivity.isIgnoredAlbumPath`는 `return IgnoredAlbumPathPolicy.isIgnoredAlbumPath(str);` 1줄 위임만 남겼습니다.
   - `IgnoredAlbumPathPolicyTest.java`에서 대소문자 QuickBeauty 경로·기존 제외 경로·정상 장소 앨범 폴더를 회귀 검증했습니다.
2. **[P1] 원본 후보 동일성 대조 및 중복·불일치 거절 완료**
   - `QuickBeautySourceLink.java`에서 `src_id` 조회 후에도 마커의 `src_name`(대소문자 무시 파일명) 및 `src_dt`/`src_taken`(MediaStore `datetaken` 및 원본 EXIF `DateTimeOriginal` ±2초 이내)과 대조하여 불일치 시 거절합니다. 이름·날짜 증거가 없는 단독 숫자 ID 마커는 `AMBIGUOUS`로 거절합니다.
   - 폴백 조회는 파일명과 촬영시각 증거가 모두 있을 때만 수행하며, 검증 통과 후보가 2개 이상이면 첫 후보를 임의 채택하지 않고 `AMBIGUOUS`를 반환합니다.
3. **[P2] 일시적 원본 읽기 실패 시 음수 캐시 방지 및 기존 음수 캐시 우회 완료**
   - `QuickBeautySourceLink.Resolution` 상태(`NOT_LINKED`, `FOUND`, `CONFIRMED_NO_GPS`, `DEFERRED_OR_UNAVAILABLE`, `AMBIGUOUS`)를 분리했습니다.
   - `PersonalPlaceGpsReader.java`는 `DEFERRED_OR_UNAVAILABLE` 또는 `AMBIGUOUS`일 때 디스크 캐시 비대상인 `PersonalPlaceGpsPoint.deferred`(`READ_DEFERRED_OR_DENIED`)를 반환하여 원본이 나중에 접근 가능해지면 재시도합니다. 검증된 원본의 EXIF를 실제로 열었고 GPS와 상위 마커가 모두 없을 때만 `CONFIRMED_NO_GPS` → `noGps`로 처리합니다.
   - `PersonalPlaceGpsPoint.buildFingerprint`에서 `Beauty_` 접두사 파일에 `:qb2` 접미사를 추가해 이전 버전에서 `NO_GPS_TAG`로 캐시된 보정본만 재평가되도록 했습니다.
4. **[P2] 연속 재보정(`O → A → B`) 원본 연결 유지 완료**
   - QuickBeauty(`BeautyBitmapUtils.kt`)에서 입력 사진에 기존 `QuickBeauty|` 마커가 있으면 최초 원본 `O`의 메타데이터를 승계(`parseSourceMarker`, `mergeInheritedSourceMetadata`)하도록 수정했습니다.
   - PhotoPlace(`QuickBeautySourceLink.java`)에서도 `visitedIds`/`visitedMarkers` 집합과 `MAX_CHAIN_DEPTH = 3` 제한으로 순환 참조(`A ↔ B`)를 막으면서 다단계 참조(`B → A → O`)를 추적합니다.
5. **검증**
   - PhotoPlace: `QuickBeautySourceLinkTest`(8건: 파서 2건 + ID 충돌 거절 + 단독 ID 거절 + 원본 이동 ID 변경 및 동명 중복 거절 + 일시 실패 후 재시도 성공/확정 무GPS 구분 + `:qb2` 레거시 음수 캐시 무효화 + `O → A → B` 체인 및 `A ↔ B` 순환 차단) + `IgnoredAlbumPathPolicyTest`(2건) + `PersonalPlaceTest`(9건) = **19건 전체 통과**, `assembleDebug` 성공.
   - QuickBeauty: `BeautyBitmapUtilsMetadataTest`(4건, 재보정 시 원본 마커 승계 포함) 포함 `testReleaseUnitTest` **70건 전체 통과**, `assembleDebug` 성공.
