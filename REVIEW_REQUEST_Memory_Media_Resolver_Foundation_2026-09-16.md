# PhotoPlace Memory Media Resolver Foundation Review Request

## 대상

- 브랜치: `codex/photoplace-v2-bg-wip`
- 커밋: `87178eb Add memory media resolver foundation`
- 관련 작업 로그: `WORKLOG_2026-09-16.md`

## 리뷰 범위

이번 커밋은 UI 연결 전의 작은 foundation slice다. 다음 파일을 실제 코드 기준으로 검토해 주세요.

- `MemoryMediaStoreReader.java`
- `MemoryMediaResolver.java`
- `MemoryMediaResolution.java`
- `MemoryMediaResolutionTest.java`

## 반드시 확인할 항목

1. **단일 source 원칙**
   - exact usable Memory `OrganizationLink`가 있고 Gallery 조회가 `FOUND`이면 Gallery만 반환하는가?
   - Gallery와 Discovery를 무조건 union하지 않는가?
   - 확인된 `MISSING`만 Discovery fallback으로 처리하고, `FAILED`/`UNKNOWN`을 안전하게 `UNAVAILABLE`로 남기는가?

2. **MediaStore 조회 안전성**
   - 정확한 `relative_path`만 조회하는가?
   - pending/trashed 항목을 제외하는가?
   - 사진과 동영상 URI, MIME type, 촬영 시각을 올바르게 구성하는가?
   - null cursor, 잘못된 경로, 예외 발생 시 오판하지 않는가?

3. **Memory identity와 메모리 보존 전제**
   - 조회 결과가 Gallery path를 Memory identity로 사용하지 않는가?
   - 장소/국가 메타데이터를 보조 표시값으로만 붙이는가?
   - 기존 `stableMemoryId + date` 메모리와 Collection 계층을 다음 단계에서 유지할 수 있는 반환 형태인가?

4. **정합성 및 테스트**
   - 결과 목록이 immutable하게 보존되는가?
   - 경로 정규화가 OrganizationLink의 Windows 경로/끝 슬래시와 일치하는가?
   - 테스트에 추가해야 할 blocker 또는 high-risk case가 있는가?

5. **Release gate**
   - 이번 커밋만으로 UI나 Memory detail이 Gallery output을 실제로 읽는 상태인지 과장 없이 판단해 주세요.
   - 내부 Photo Viewer 연결 전까지 원본 휴지통 이동을 계속 막아야 하는지 확인해 주세요.
   - 이번 foundation을 승인하더라도 Play release는 resolver UI 연결과 실기기 회귀 검증 전까지 보류해야 하는지 판단해 주세요.

## 검증 결과

- `./gradlew.bat testDebugUnitTest`: PASS
- `./gradlew.bat assembleDebug`: PASS
- `git diff --check`: PASS

## 리뷰 결과물

심각도 순으로 findings를 먼저 정리하고, 문제가 없으면 남은 테스트 공백과 release blocker를 명시해 주세요. 리뷰 문서는 다음 파일에 남겨 주세요.

`REVIEW_Memory_Media_Resolver_Foundation_2026-09-16.md`

이번 리뷰에서는 application code 수정 없이 검토만 진행해 주세요.
