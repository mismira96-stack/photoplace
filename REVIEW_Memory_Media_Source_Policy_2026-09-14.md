# PhotoPlace Memory Media Source Policy 코드 리뷰

**일자:** 2026-09-14  
**작성자:** Antigravity  
**검토 대상:**
- `REVIEW_REQUEST_Memory_Media_Source_Policy_2026-09-14.md` (6대 검토 질의)
- `app/src/main/java/com/example/gallerysorter/MemoryMediaSourcePolicy.java`
- `app/src/test/java/com/example/gallerysorter/MemoryMediaSourcePolicyTest.java`
- `TODO.md` 및 `WORKLOG_2026-09-14.md`

**최종 판정:**
- **이번 순수 정책 슬라이스:** **최종 승인 (Final Approved - 무결한 단일 소스 선택 및 안전 차단벽 구축 완료)**
- **Play 프로덕션 정식 출시:** **보류 유지 (Hold - MediaStore 실제 연동 및 내부 사진 뷰어 미구현)**

---

## 1. 개요 및 아키텍처 원칙 준수 확인

이번 작업은 공용 Memory Media Resolver의 첫 단추인 **미디어 소스 선택 정책(Pure-Java Domain Policy)**을 구현한 것입니다.

무엇보다 **`MainActivity`에 단 한 줄의 코드도 추가하지 않고**, 순수 비즈니스 로직 클래스([`MemoryMediaSourcePolicy.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/main/java/com/example/gallerysorter/MemoryMediaSourcePolicy.java))와 독립 단위 테스트([`MemoryMediaSourcePolicyTest.java`](file:///c:/Users/mismi/Documents/Codex/GallerySorter/app/src/test/java/com/example/gallerysorter/MemoryMediaSourcePolicyTest.java))로 100% 격리하여 구현함으로써, **"새 기능 구현 시 MainActivity에 코드를 밀어넣지 않는다"는 P0 아키텍처 원칙을 완벽하게 준수**했습니다.

---

## 2. 6대 리뷰 질문(Review Questions) 심층 검증

### Q1. 의도하지 않은 합집합(Union)이나 위험한 폴백 없는 단일 소스 선택 구현 검증
- **판정: 완벽함 (Pass)**
- `MemoryMediaSourcePolicy.choose()`는 단 하나의 `Source` 열거형(`GALLERY_OUTPUT`, `DISCOVERY`, `EMPTY`, `UNAVAILABLE`)만을 반환합니다.
- usable exact Memory 링크가 있고 Gallery output이 확인(`FOUND`)되면, 라이브 Discovery 미디어의 존재 여부와 무관하게 **오직 `GALLERY_OUTPUT`만을 단독 소스로 확정**합니다.
- 따라서 Gallery 앨범 미디어와 Discovery 미디어가 무분별하게 중복(Union) 노출되는 문제가 원천 차단됩니다.

### Q2. `PARTIAL` 상태의 유효 취급 적합성 및 미디어 누락 리스크 평가
- **판정: 적합함 및 안전 가이드 확인 (Pass)**
- **적합성:** `MemoryOrganizationLinkStore`의 기존 `isUsable(link)` 계약(`SUCCESS` 또는 `PARTIAL`)과 100% 일치합니다.
- **누락 리스크 분석:** 일부 파일 복사가 실패한 `PARTIAL` 상태의 앨범을 `GALLERY_OUTPUT` 단독 소스로 볼 경우, 복사되지 않은 나머지 사진이 뷰어에서 보이지 않을 수 있습니다.
  - 하지만 중복 방지(No Union) 원칙상 이는 의도된 동작이며, 현재 **원본 사진 휴지통 이동이 완전히 차단**되어 있으므로 데이터 유실 위험은 0%입니다.
  - 추후 실제 Resolver 연동 시 `PARTIAL` 링크에 대해서는 UI에 "일부 정리됨" 배지를 표시하거나 재시도 액션을 유도하면 더욱 완벽합니다.

### Q3. `FAILED / UNKNOWN -> UNAVAILABLE` 보수적 방어벽의 적절성
- **판정: 매우 안전한 설계 (Pass)**
- MediaStore 조회 실패(`FAILED`)나 불명(`UNKNOWN`) 상태를 확인된 부재(`MISSING`)와 엄격히 구분했습니다.
- 조회가 일시적으로 실패했다고 해서 섣불리 Discovery로 폴백하면, 실제로는 존재하는 갤러리 앨범을 두고 중복/과거 사진이 노출되거나 사용자를 혼란스럽게 만들 수 있습니다.
- `UNAVAILABLE`로 탈출시킴으로써 호출자(Caller)가 안전하게 에러 뷰나 재시도 UI를 띄울 수 있도록 안전 계약을 확립했습니다.

### Q4. Invalid / Non-Memory / MISSING 링크에 대한 방어 및 테스트
- **판정: 철저함 (Pass)**
- `hasUsableMemoryLink()`에서 4중 필터링을 수행합니다:
  1. `link != null`
  2. `link.isValid()` (ID, 경로, 시각 유효성)
  3. `link.subjectType == SubjectType.MEMORY` (Collection 링크 제외)
  4. `link.status == SUCCESS || status == PARTIAL` (`MISSING` 상태 링크 배제)
- `MemoryMediaSourcePolicyTest.absentOrUnusableMemoryLinkUsesDiscoveryOnly()`에서 `null`, `MISSING`, `COLLECTION`, 비정상 링크를 모두 빠짐없이 테스트하여 검증을 완료했습니다.

### Q5. 단위 테스트의 집중도 및 독립성
- **판정: 100% 순수 JVM 테스트 (Pass)**
- Android 프레임워크(`Context`, `ContentResolver`, `Uri`) 의존성이 전혀 없는 순수 POJO 테스트로 작성되어 수 밀리초 만에 즉시 실행되며, 플랫폼 버전이나 모의 객체(Mock) 없이 결정론적으로 동작합니다.

### Q6. 다음 Resolver 슬라이스를 위한 릴리즈 게이트 및 후속 과제
- **블로커 없음 (No Blocker)**
- **다음 구현 과제:**
  1. `exactMemoryLink.targetRelativePath` 기반으로 실제 MediaStore를 쿼리하여 `GalleryLookup` 상태를 판별하는 `MediaStoreAlbumLookup` 연동.
  2. `GALLERY_OUTPUT`으로 결정된 미디어를 날짜별 `MemoryPhotoSection`으로 구성하고, `MemoryDateNoteStore`의 기존 날짜 메모(`stableMemoryId + "#" + dateKey`)와 완벽히 결합.
  3. 같은 장소/날짜 미디어를 앱 내에서 탐색하는 **인앱 스와이프 사진 뷰어** 구현.

---

## 3. 검증 결과 요약

- **`./gradlew.bat testDebugUnitTest`:** **통과 (20 tasks, 4s)**
- **`./gradlew.bat assembleDebug`:** **통과 (최신 APK 빌드 완료)**
- **`git diff --check`:** **통과 (포맷 결함 없음)**

| 구분 | 판정 | 설명 |
| :--- | :---: | :--- |
| **미디어 소스 선택 정책 슬라이스** | **최종 승인 (Final Approved)** | `MainActivity` 무변경 원칙 준수, 단일 소스 선택 및 안전한 폴백 무결성 확인 |
| **Play 프로덕션 정식 출시** | **보류 유지 (Hold)** | MediaStore 실제 쿼리 연동 및 인앱 사진 뷰어 구현 전까지 릴리즈 게이트 유지 |
