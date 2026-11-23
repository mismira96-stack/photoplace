# PhotoPlace 발견 기록 중심 3탭 전환 및 갤러리 연동 설계 검토 요청 — 2026-11-22

> **검토 대상 문서**: [DESIGN_Discovery_Centric_Navigation_2026-11-22.md](DESIGN_Discovery_Centric_Navigation_2026-11-22.md)  
> **요청자**: Antigravity (Gemini)  
> **수신자**: Codex 및 사용자  
> **상태**: 구현 전 아키텍처 및 UX 설계 합의 단계 (코드 변경 없음)

---

## 1. 개요 및 배경

- **방향성 합의**: 사용자께서 '위치 앨범' 탭을 서서히 정리하고, **`발견 기록`을 앱의 단일 메인 피드로 격상**하는 방향을 제안하셨습니다.
- **원래 핵심 니즈(남편분의 갤러리 앨범 생성)**:
  - 남편("우리아저씨")의 필수 사용 시나리오인 **물리 폴더(`Pictures/<장소명>에서/`) 생성**은 발견 상세 화면의 `[📁+ 앨범 만들기 / 추가]` 액션을 통해 100% 보존합니다.
  - 추가로, 이미 생성된 물리 앨범은 **삼성 갤러리 앱 원탭 점프(`[갤러리에서 보기 ↗]`)**를 제공하여 갤러리 앱과 매끄럽게 연결합니다.
- **하단 3탭 슬림화**:
  - 기존: `[ 홈 ]` · `[ 발견 기록 ]` · `[ 위치 앨범 ]` · `[ 설정 ]` (4탭)
  - 개선: `[ 홈 ]` · `[ 발견 기록 ]` · `[ 설정 ]` (3탭)
  - 발견 기록 상단에 `[전체 N]` / `[📁 갤러리 앨범만 M]` 가벼운 서브 필터를 제공하여 기존 위치 앨범 탭의 탐색 경험을 100% 흡수.

---

## 2. 주요 설계 검토 포인트 (Codex 피어 리뷰 요청 사항)

1. **`BottomNavigationRenderer.java` 3탭 전환 호환성**:
   - `ICONS = {"home", "photoLibrary", "settings"}`, `LABELS = {"홈", "발견 기록", "설정"}` 축소 시 인덱스 매핑(`0: 홈, 1: 발견 기록, 2: 설정`)과 기존 탭 전환 콜백 간섭 여부.
2. **`MainActivity.java` 0줄 수정 원칙 (P0 Gatekeeper)**:
   - 3탭 변경 및 갤러리 점프 인텐트 연동 시 `MainActivity` 수정 없이 `BottomNavigationRenderer`, `DiscoverySnapshotController`, `GalleryJumpHelper` 등 전용 컴포넌트로 격리 구현 가능한지 검토.
3. **삼성 갤러리 연동 인텐트 규격 (`GalleryJumpHelper`)**:
   - `Pictures/<장소명>에서/` 폴더 또는 대표 미디어 URI 기반 `Intent.ACTION_VIEW` (`com.sec.android.gallery3d` 우선, 일반 갤러리 폴백) 호출 시 Scoped Storage / Android 11+ `queries` 패키지 가시성 확인.
4. **`MemoryRepository` 내 중복 레코드(예: `청주` vs `청주에서`) 병합 정책**:
   - 발견 기록 중심으로 일원화될 때, 기존에 물리 앨범이 존재하는 장소는 `MIXED` 타입 단일 카드로 깔끔하게 합쳐져 표출되는지 검토.

---

## 3. 관련 파일 링크

- [설계 명세서](DESIGN_Discovery_Centric_Navigation_2026-11-22.md)
- [작업 기록](WORKLOG_2026-11-22.md)
- [Codex 지침](AGENTS.md) · [Antigravity 지침](GEMINI.md) · [UI 가이드](UI_GUIDE.md)
