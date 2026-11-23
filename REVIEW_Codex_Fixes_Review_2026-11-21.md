# PhotoPlace 코덱스 수정 전수 코드 리뷰 보고서 (2026-11-21)

## 1. 종합 판정: 승인 (Approved)

코덱스(Codex)가 최근 수정한 **[1] 퀵뷰티 원본 촬영일·GPS 연동 결함 보완 및 MainActivity 격리**, **[2] 스토리 템플릿 확장 및 2160px 고화질 내보내기/OOM 방어 메모리 예산**, **[3] 발견 기록 2열 카드 및 상세 화면 로딩 최적화** 전반에 걸친 코드 리뷰를 실시했습니다.

앞선 리뷰에서 지적되었던 **Blocker 1건(MainActivity 폴더 제외 로직 잔존)**과 **Pre-release 3건(후보 동일성 미검증, 영구 음수 캐시, 연속 보정 단절)**이 완벽하고 견고하게 해결되었으며, 프로젝트 핵심 원칙(P0/P1)을 100% 준수하고 있음을 확인했습니다.

---

## 2. P0 / P1 게이트키퍼 검증 결과

### 🚨 [P0 원칙] MainActivity 비대화 절대 금지 (0줄 격리 원칙) — 통과 (PASS)
- **점검 내용**: 폴더 제외 판단 로직(`isIgnoredAlbumPath`)이 `MainActivity.java`에 직접 구현되어 있던 문제를 검사.
- **결과**: `IgnoredAlbumPathPolicy.java` 순수 Java 도메인 정책 클래스로 완전 분리 완료.
- **현재 `MainActivity.java:3138-3140`**:
  ```java
  private boolean isIgnoredAlbumPath(String str) {
      return IgnoredAlbumPathPolicy.isIgnoredAlbumPath(str);
  }
  ```
- 비즈니스/판단 로직 0줄 유지 (단순 위임 호출만 존재). 신규 단위 테스트 `IgnoredAlbumPathPolicyTest` 2개 추가 및 통과.

### 🔒 [P1 원칙] 데이터 안전성 및 무결성 규칙 — 통과 (PASS)
- **MediaStore / 파일 불변 원칙**: 원본 사진 파일의 수정, 이동, 복사, 삭제가 전혀 없으며 모두 읽기 전용(`r`) 스트림으로만 접근.
- **불변 식별자 및 메모 보존**: `buildFingerprint`에 `:qb2`를 도입하여 기존 레거시 캐시와의 충돌을 안전하게 무효화하면서도 사용자 메모와 식별자 체계는 그대로 보존.

---

## 3. 핵심 수정 사항 상세 검토

### ① 퀵뷰티 원본 연동 4대 결함 해결 ([`QuickBeautySourceLink.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/QuickBeautySourceLink.java))
1. **후보 동일성 다중 교차 검증 (Identity Verification)**:
   - 기존의 `src_id` 단순 맹신을 제거하고, `matchesMediaRowPreliminarily` 및 `matchesFullIdentity`를 통해 **파일명 + 촬영일(2초 오차 범위 허용) + 원본 EXIF 일시**를 다중 대조.
   - 단말 이동/복원으로 ID가 바뀌었거나 다른 사진을 가리키는 경우, 동명 파일이 여러 개일 때 모호성(`Status.AMBIGUOUS`)으로 판정하여 오연결 차단.
2. **일시적 실패와 영구 실패 분리 (Transient vs Terminal)**:
   - 상태를 `NOT_LINKED`, `FOUND`, `CONFIRMED_NO_GPS`, `DEFERRED_OR_UNAVAILABLE`, `AMBIGUOUS`로 세분화.
   - 일시적으로 원본 스트림을 열지 못하거나 권한이 없는 경우 `DEFERRED_OR_UNAVAILABLE`을 반환하며, [`PersonalPlaceGpsPoint.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/PersonalPlaceGpsPoint.java)의 `isCacheableOnDisk()`에서 디스크 캐시를 차단하여 차후 정상 재시도 보장.
3. **연속 보정 체인 추적 (Multi-hop Chain Resolution)**:
   - 원본 O ➡️ 1차 보정 A ➡️ 2차 보정 B와 같은 연속 보정본에서도 `parentLink`를 최대 3단계(`MAX_CHAIN_DEPTH = 3`)까지 추적하여 최종 원본 O의 GPS를 정상 추출.
   - `visitedIds` 및 `visitedMarkers` 세트로 순환 참조(A ↔ B) 발생 시 무한 루프 원천 차단.

### ② 스토리 템플릿 확장 및 2160px 초고화질 메모리 예산 ([`MemoryStoryExportQuality.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryStoryExportQuality.java), [`MemoryStoryRowLayout.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryStoryRowLayout.java))
1. **해상도 2배 확대 & OOM 방어 버퍼**:
   - 카드 출력 캔버스 너비를 2160px로 2배 확대.
   - `SOURCE_PIXEL_BUDGET = 12_000_000L`을 설정하여 4장 선택 시에도 디코딩 총 픽셀을 약 48MB ARGB_8888 이내로 캡핑, 고화질 렌더링과 저메모리 기기 안정성을 동시 확보.
2. **실기기 확정 템플릿 레이아웃**:
   - 2·3장: 가로 1열 나란히 / 4장: 2×2 그리드 / 사진띠: 동일 폭 세로 스택.
   - 회전 각도 삼각함수 보정으로 카드 밖 픽셀 이탈 방지 및 하단 편집 도구 Y좌표(`[549,1596][1032,1728]`) 고정.
   - 스크랩북 개별 사진 흰 테두리 제거(사용자 확정 피드백 반영).

### ③ 로딩 최적화 및 안정성 보강 ([`PersonalPlaceDetailPresenter.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/PersonalPlaceDetailPresenter.java))
- 발견 2열 그리드 렌더링 시 개인 장소 I/O를 1회만 조회하여 모든 카드에 공유.
- 상세 화면 진입 시 이미 로딩된 목록의 메모리 버전을 재사용하여 첫 진입/재진입 속도 대폭 개선.
- `PersonalPlaceDetailPresenter:488`의 `cardPlaces == null || cardPlaces.isEmpty()` 방어 코드로 NPE 가능성 원천 배제.

---

## 4. 검증 결과

- **단위 테스트 실행**:
  - `.\gradlew.bat testDebugUnitTest`: **584개 테스트 전체 통과 (0 failures, 0 errors)**
  - `.\gradlew.bat testReleaseUnitTest`: **584개 테스트 전체 통과 (0 failures, 0 errors)**
- **신규 테스트 커버리지**:
  - `IgnoredAlbumPathPolicyTest`: 카메라, 퀵뷰티, 시스템 SNS 폴더 제외 및 정상 장소 앨범 통과 검증 (2개 테스트)
  - `QuickBeautySourceLinkTest`: 마커 파싱, ID 충돌 거부, 일시적 실패 지연 처리, 이동된 사진 해석, 레거시 캐시 무효화, 다단계 보정 체인 및 순환 방지 검증 (6개 테스트)

---

## 5. 결론 및 후속 안내

- **최종 판정**: **승인 (Approved)**
- 코덱스의 이번 수정은 P0 격리 원칙, P1 데이터 불변 원칙, 성능 및 메모리 안정성을 모두 완벽하게 충족합니다.
- 추가 코드 수정 없이 실기기 테스트 및 후속 사용자 시나리오 확인을 진행할 수 있습니다.
