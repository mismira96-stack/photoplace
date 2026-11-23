# 기억 모으기 왕복 후 썸네일 빈칸 — code80 수정

2026-11-10. 상태: 구현/API33 native 검증 완료, API36 및 사용자 실기기 수용 확인 중. 공개 출시 보류.

## 신고의 정확한 의미 (사용자 추가 캡처로 정정)
- 처음 ‘화면이 전체 하얗다’를 화면 복귀 실패로 해석했으나, 추가 캡처에서 제목/장소/모음/수량/탭은 정상이고 대표 사진만 모두 빈 placeholder인 것을 확인.
- 모음을 저장하지 않아도 기억 모으기 선택 화면을 갔다 나오면 발생한다고 사용자 확인. 이전 Back 네비게이션 정상 검사는 이 썸네일 문제를 검증한 것이 아니었음.

## 재현된 실패 경로
- 기존 MainActivity.loadMemoryBrowserThumbnailInto가 모든 대표사진 요청을 legacy thumbnailWorker(2개 스레드)에 넣음.
- 모으기 선택 목록의 작은 cover 요청과 돌아온 발견 기록의 큰 cover 요청이 공유 큐를 사용. 서로 다른 size cache key,32entry LRU eviction 후 새 화면의 cache miss는 과거 화면 작업 뒤에서 대기함.
- 테스트에서 과거 공유 작업2개를 latch로 대기시키고 cache를 비운 뒤 기억 모으기->Back: 화면/제목/수량은 돌아오지만 새 cover는4초 이상 placeholder. 수정 전 native assertion 실패로 동일 형태를 재현.
- 실제 폰은 USB 미연결이어서 큐 상태 직접 로그는 미수집. 사용자 screenshot 증상과 제어된 backlog 재현을 근거로 이 실패 경로를 수정했으며, 실기기 수용은 별도 필요.

## 수정
- MemoryBrowserThumbnailLoader: 대표사진 전용2-worker 큐. 닫힌 ImageView의 요청은 기존 LatestImageLoadQueue generation/cancellation signal로 무효화, 대기한 구 화면 decode는 실행 전에 skip. 재바인딩 소유권 검사.
- provider cancellation Binder 처리는 별도 단일 executor에서 수행하여 화면 detach/main thread를 막지 않음. Activity 종료시 owned workers 및 pending listener 정리.
- 기존 MainActivity.thumbnailCache를 그대로 공유. cache capacity/key/기존 검색·미디어 조회 로직 변경 없음. URI와 요청 크기 그대로 사용, 원본 fallback 읽기도 이미지 크기를 제한함.
- MainActivity 변경은 field/초기화/전용 loader 위임/종료 연결뿐. 기존 메서드 안의 null/URI/catch 판단은 helper로 이동. 신규 비즈니스 판단0줄(P0).
- 원본 파일/EXIF/촬영일/Memory/모음/OrganizationLink/메모 저장 경로 수정 없음(P1).

## 검증
- 수정 전 .debug/collection-back80/thumbnail-backlog-before.log: 과거 큐 blocked시 복귀 cover 로딩 assertion FAIL.
- 수정 후 .debug/collection-back80/thumbnail-backlog-after.log: 동일 blocked 조건에서 두 실제 MediaStore synthetic photo cover BitmapDrawable 로딩 PASS. 같은 shared cache Bitmap 즉시 재사용 PASS. worker close PASS.
- 시스템 Back/상단 Back/검색IME 복귀 반복 및 실제 UI/픽셀 확인 PASS.
- Story 단일6종 및1~4장 선택 공유 회귀 PASS. .debug/collection-back80/story-single-regression.log / story-selection-regression.log.
- Debug/Release 각522 tests, failures/errors/skips0. assembleRelease/bundleRelease/lintVitalRelease 성공.
- API36 결과 및 공식 APK/Drive 검증은 WORKLOG_2026-11-10.md 후속 기록.

## 다음 단계
- code80/1.3.44 공식 서명 APK를 Drive에 전달 -> 사용자 기기에서 기억 모으기 갔다 Back 후 대표 사진이 나타나는지 확인 -> 같은 검증 bundle로 출시.
- production75 completed 유지. 사용자80 실기기 수용 전 제출 금지.

## code80 전달 최종 기록 (2026-11-10)
- Google Drive 웹 실제 파일 행 확인 완료. 다운로드: https://drive.google.com/open?id=19nHWGZN0GH_CQsVY5Q6tGWMRFtd6TDno
- 파일: photoplace-1.3.44-code80-play-signed.apk (5,856,668 bytes)
- SHA-256: 0a69912d3f6d25279f6232cc0f2658c1b2d8da0b0d7e0d8e354884ad7677f6b9
- Play signer SHA-256: b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d
- 로컬 최종 release와 Play universal APK DEX 일치, mounted Drive 사본 SHA 일치. 서명용 internal draft80만 생성. production75 유지, 공개 제출하지 않음.
- 폰 USB 미연결로 code80 실기기 설치/수용은 미완료. 삭제 없이 update 설치 후 기억 모으기 진입->아무것도 만들지 않고 Back->발견 기록/모음 대표사진 복귀 확인 필요.
- 자동 검증: API33 Back/IME/backlog/cache/lifecycle PASS. Debug/Release 각522 PASS, 기존 Story 단일6종/1~4장 공유 PASS.
- API36 최초 테스트는 일반 Back/상단 Back PASS 후 검색 키보드 시스템 Back 2연타 구간 timeout. 그 뒤 backlog 검사는 도달하지 못했으므로 API36 전체 통과로 보고하지 않음. 입력 애니메이션 대기 보강한 test-only 재확인 진행. 제품 APK에는 추가 변경 없음.
- 웹 업로드 증거: .debug/collection-back80/drive-upload.png. 실제 폰 큐 로그는 미수집이므로 수정 원인은 제어된 backlog 재현에 근거하며 최종 수용 대기.

### API36 최종 재확인
- .debug/collection-back80/thumbnail-backlog-api36-recheck.log: INSTRUMENTATION_CODE -1 / 전체 PASS.
- 키보드 dismiss/Back 주입 간격 및 소비 여부를 test harness에서 기다린 뒤 재확인. 제품 소스/APK 변경 없음. 최초 timeout 로그도 삭제하지 않고 보존함.
- API36 일반/헤더/검색IME Back, old shared queue blocked 상태 실제 cover 로딩, shared cache 동일 bitmap 재사용, worker 종료 모두 PASS. 캡처 thumbnail-return-api36-after.png.
- API33/API36 자동 검증 완료. 사용자 실제 폰 수용은 계속 미완료, 출시 보류.
