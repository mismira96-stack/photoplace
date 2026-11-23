# Discovery CTA 설계 리뷰 (커밋 eaae94e)

요청: PhotoPlace의 발견 탭 전역 CTA "발견한 장소 모두 위치 앨범으로 만들기" 설계 리뷰
대상 커밋: `eaae94e` (Add discovery search and live media filtering)

참조 파일
- [DiscoveryAlbumOrganizer.java](app/src/main/java/com/example/gallerysorter/DiscoveryAlbumOrganizer.java)
- [MediaStoreAlbumLookup.java](app/src/main/java/com/example/gallerysorter/MediaStoreAlbumLookup.java)
- [DiscoveryOrganizeConfirmDialog.java](app/src/main/java/com/example/gallerysorter/DiscoveryOrganizeConfirmDialog.java)
- [DiscoveryAlbumOrganizerTest.java](app/src/test/java/com/example/gallerysorter/DiscoveryAlbumOrganizerTest.java)
- [OrganizePlaceService.java](app/src/main/java/com/example/gallerysorter/OrganizePlaceService.java)
- [DiscoverySnapshotController.java](app/src/main/java/com/example/gallerysorter/DiscoverySnapshotController.java)
- [DiscoverySnapshot.java](app/src/main/java/com/example/gallerysorter/DiscoverySnapshot.java)
- [DiscoveryPhotoRef.java](app/src/main/java/com/example/gallerysorter/DiscoveryPhotoRef.java)
- [SortInputStore.java](app/src/main/java/com/example/gallerysorter/SortInputStore.java)
- [SortWorker.java](app/src/main/java/com/example/gallerysorter/SortWorker.java)
- [SortJob.java](app/src/main/java/com/example/gallerysorter/SortJob.java)
- [MainActivity.java](app/src/main/java/com/example/gallerysorter/MainActivity.java)
- [TODO.md](TODO.md)
- [WORKLOG_2026-08-22.md](WORKLOG_2026-08-22.md)

---

## 개요
현재 설계는 discovery snapshot(read-only) 데이터를 기반으로 전역 CTA를 통해 발견한 모든 장소의 사진을 기존 정리 파이프라인(`SortInputStore` → `SortWorker`)로 전달하여 앨범을 생성하는 방식입니다. discovery 데이터는 변경하지 않고, 실제 앨범 생성은 기존 백그라운드 정리 로직을 재사용하는 점은 일관적이며 유지보수성 측면에서 적절합니다.

다음은 코드 검토를 통해 식별한 주요 위험, 블로커, 권장 개선, 그리고 MVP 범위에서 보류할 항목입니다.

## 핵심 발견 및 평결
- 설계 적합성: 전반적으로 목표 UX(전역 CTA → 사용자 확인 → 기존 정리 흐름 재사용)를 충족합니다.
- 안정성 우려: `DiscoveryPhotoRef.sourceUri` 기반의 재구성은 앱 재시작 후에도 동작하지만, URI 접근성(삭제/퍼미션) 문제에 취약합니다.
- 네이밍/경로 일관성: `OrganizePlaceService.targetRelativePath()`는 `Pictures/{장소명}에서/` 규칙을 사용합니다. 기존 앨범 생성 규칙과 완전히 동일하지 않으면 유사 폴더 중복 생성 가능성이 있습니다.
- 중복 판정: `MediaStoreAlbumLookup.fileSignature()`는 파일명에서 `(n)` suffix를 제거해 비교합니다. 실무에서 유용하지만 일부 특수 케이스에서 오탐 가능성이 있습니다.
- 대량 처리: `DiscoveryAlbumOrganizer.prepare()`는 모든 항목을 메모리에 쌓아 처리하므로 10k+ 항목에서 메모리·쿼리 성능 문제가 생길 수 있습니다.

## 블로커 (해결 필요)
1. targetRelativePath 정규화 및 기존 앨범 규칙 일치 검증
   - 문제: 다른 경로 규칙을 쓰는 기존 앨범이 있을 경우 중복 폴더 생성 위험.
   - 필수 조치: 기존 `StoredAlbumSummary.relativePath`를 생성하는 로직과 동일한 정규화/포맷 함수를 재사용하거나 적어도 변환 규칙을 맞추기.

2. 퍼미션 및 URI 접근성 확보 흐름
   - 문제: Worker/Background에서 권한을 요청할 수 없어 동영상 이동 등에서 실패 가능.
   - 필수 조치: CTA 확인 단계에서 동영상 이동 여부를 포함한 권한(또는 사용자 안내)을 확보한 후 `SortInputStore.write()` 및 Worker enqueue.

3. prepare()의 스레딩·메모리 사용 보강
   - 문제: UI 스레드에서 실행되거나 메모리로 대량 데이터를 적재하면 ANR/크래시 위험.
   - 필수 조치: prepare는 반드시 백그라운드(비동기)에서 실행. 가능하면 스트리밍 방식(장소 단위 flush)으로 메모리 사용 최소화.

4. 중복 판정 신뢰성 보완
   - 문제: 파일명 기반 비교만으로는 오탐/과탐 발생 가능.
   - 필수 조치: 가능하면 `mediaStoreId` 또는 파일 크기/촬영시간 등의 보조 기준을 사용해 판정 우선순위 개선.

## 권장 개선(우선순위)
- CTA 실행 직전에 `DiscoverySnapshotLiveFilter` 강제 실행: 삭제/휴지통 항목 제외 및 snapshot 정리. (Worklog에서 해당 컴포넌트가 이미 있음)
- `MediaStoreAlbumLookup`의 캐시 전략 검토: 장소 수가 많으면 쿼리 수 최적화(배치, 비동기) 필요.
- `SortInputStore` 쓰기 → WorkManager enqueue 순서의 원자성 보장 및 오류 회복 시나리오 문서화.
- 중복 판정 개선(옵션): 파일 크기·DATE_TAKEN 비교를 2차 검증으로 추가.
- UX: 권한 부족 시 명확한 권한 안내/가이드 플로우(설정/권한 다이얼로그 유도) 추가.

## MVP에서 보류할 항목
- 콘텐츠 기반 해시/체크섬으로 완벽한 중복 판정(과도한 비용)
- 날짜 그룹 단위 내부 스와이프 뷰어(탐색 완성도 관련)
- 머신러닝 기반 중복 병합 로직

## 안전성/예외 처리 권고
- `DiscoveryPhotoRef.mediaStoreId`를 우선 확인해 MediaStore에 존재하는지 체크하고, 존재하지 않을 경우 `sourceUri` 접근 시도 전 필터링하세요.
- `SortJob`/`MediaCopyEngine`에서 항목별 실패 시 상세 로그/원인 저장(권한, not found, IO)하고 partial 성공은 정상 반영.
- Worker 재시작 정책을 명시: 작업 중 중단되면 `SortInputStore`가 남아 있으므로 재시작 시 재처리되지만, 중복 생성 방지를 위해 `MediaStoreAlbumLookup` 기반 재검증을 수행해야 함.

## 테스트 체크리스트 (필수)
### 단위
- `MediaStoreAlbumLookup.fileSignature()` edge cases
- `OrganizePlaceService.targetRelativePath()` 한글/특수문자/끝말 조사 케이스
- `DiscoveryAlbumOrganizer.prepare()` 중복 제외 동작

### 통합
- CTA 전/후: `SortInputStore.write()` → Worker enqueue → `SortResultStore` 기록 → input clear
- 권한 부재 시: CTA 클릭 후 권한 안내/중단 시나리오
- 삭제된 source URI 항목 처리: 일부 실패, 일부 성공 시 일관성

### 성능
- prepare() 10k/50k 항목 메모리·시간 프로파일링
- 다수 장소에서 `MediaStoreAlbumLookup` 쿼리 부하 테스트

### E2E
- 앱 포그라운드/백그라운드/재시작 후 작업 재진입 및 결과 반영
- 발견 탭과 위치 앨범 탭의 갱신(라이브 필터 재실행 및 요약 업데이트)

## 작업 완료 후 UI 갱신 정책 제안
- Worker 완료 시 `SortResultStore` 쓰기를 트리거로 앱이 포그라운드면 즉시 `DiscoverySnapshotLiveFilter` 재실행 및 `MemoryRepository` 재로딩.
- UI 요약: 성공/실패 건수와 간단한 링크(발견 탭 재보기 / 위치 앨범 보기)를 사용자에게 제공.

## 참고 및 파일 링크
주요 구현/검토 위치는 파일 상단의 "참조 파일" 목록을 확인하세요.

---

원하시면 이 파일을 기반으로 PR 코멘트용 요약(간단 체크리스트)이나, 우선순위 1~3 항목에 대한 구체적 구현 제안 문서를 추가로 생성해 드립니다.