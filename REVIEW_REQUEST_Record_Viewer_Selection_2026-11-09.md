# Record recovery / viewer quality / selection Story — code76 review request

2026-11-09. Status: DEVICE_VALIDATION_PENDING, user_acceptance=false, production_submission_allowed=false.

## Approved scope
- Reinstall -> rebuild organized album history -> rescan discovery produces separate same-place cards. Recover a read-only presentation connection, preserving media, capture times, notes and stable IDs.
- Fullscreen photo viewer must load original bytes first; grid thumbnails unchanged.
- Add Instagram share action to existing record selection. Reuse its selected asset keys, trash/place flows. 1=single card, 2–4=existing strip; >4 exact notice. No editor repicker. Cream retained, dark UI option removed, card-only transparent export for both modes.

## Implementation
- RecoveredAlbumLinkResolver + RecoveredAlbumGeoReader: only generated root Pictures/<name>에서 albums without country; exact canonical name, unique domestic discovery candidate, real read-only EXIF GPS proximity <=25 km. Existing OrganizationLink ownership takes precedence. Missing GPS/permission, ambiguous names, overseas unknown country or conflicting immutable IDs fail closed. Per-repository resolution/URI metadata caching.
- MemoryRepository / DiscoverySnapshotController: merge presentation while retaining both source sets, route original aliases to combined key. No registry remap, fake OrganizationLink, cleanup permission or writes.
- MemoryViewerImageLoader: ImageDecoder original-first, EXIF orientation, longest side <=2048, cancellation/OOM handling. Existing latest-request queue, cache budgets and thumbnail preview unchanged. Provider thumbnail/video fallback only if original decode unavailable.
- MemoryManagementController: Instagram secondary footer action uses existing selection(); no new selection state. Trash and place methods unchanged.
- MemoryStorySelection immutable input snapshot enforces count/type and cream/place/date configuration. Existing editor preserves chosen layout and does not offer repicking. Composer renderCard now supports tightly cropped strip with transparent rounded corners and right-aligned footer; full-canvas background is preview only. PhotoPreview uses strip bounds for gestures/fit.
- MainActivity.java byte-identical to code75 baseline.

## Validation completed
- Debug and Release each 517 unit tests, 0 failures/errors/skips.
- API33 real MediaStore/EXIF fresh-state fixture: old merge ->6 cards, new ->3 MIXED records with both photos; old routes retained, date note stable ID unchanged, protected JSON and all six JPEGs byte-identical.
- API33 viewer: original4096x3072 with EXIF90 ->1536x2048; cancellation respected, source byte-identical.
- API33 1/2/3/4 selection editor + PNG dimensions/transparency/white paper/no outer colored rectangle; no black option; cancel preserves selection. Actual MemoryManagementController footer opens existing editor, selected asset keys identical after cancel, trash action still exists.
- Existing six single-card native cases pass: portrait/landscape/square, context on/off, fonts/text size, drag/pinch/reset, explicit save, share Intent and alpha, fixed header.
- assembleRelease, bundleRelease, lintVitalRelease successful. No production publication authorized.

## Evidence
- .debug/recovered-album-native.log
- .debug/viewer-quality-native.log
- .debug/story-selection-native.log
- .debug/story-single-regression76.log
- .debug/record-viewer-selection-tests.log
- .debug/record-viewer-selection76/selection-strip-editor.png and selection-sticker-{1,2,4}.png
- .review/code75-before-record-recovery/source-snapshot.json

## Review focus / residual limits
- Conservative identity conflict behavior intentionally keeps two separately identified records separate. Do not merge/overwrite their notes. Domestic recovery requires readable GPS in both representative album photo and a discovery photo, no name-only guesses.
- Emulator uses synthetic media only; it is not user's device acceptance. Actual Samsung reinstall dataset and Instagram app return must be checked on device before release.
- 2048 viewer bound preserves memory budgets; this is not arbitrary full-resolution zoom/tiling support.
- Existing selection activity recreation persistence is not expanded; ordinary editor cancel/Instagram pause/resume retains the existing selected set (reload retains live keys). Process death is a separate follow-up.


## code76 설치 및 사용자 확인
- Play 동일 signer b9f9d963... 검증, 로컬 release APK와 공식 APK DEX 동일 확인.
- photoplace-1.3.40-code76-play-signed.apk SHA256: 1aa11bb151634d9907cb911d52588a3bdd418e15bfe16918809855a94a0e3ece.
- R5KL503VHQR install -r 성공. version1.3.40/code76, firstInstallTime2026-11-09 10:56:31 유지, lastUpdate22:50:27. 앱 삭제/데이터 초기화 없음.
- 사용자가 설치 후 “합쳐졌네”로 기존 실기기 재현 데이터의 카드 통합 확인. 기록 연결 수정 실기기 확인 완료.
- 뷰어 화질 및 Instagram1~4장 실제 앱 왕복은 사용자 확인 대기. 공개 출시 승인 없음.
- 공식 동일 서명 확보용 internal76 DRAFT ONLY. production75 completed 유지. 검토 제출/프로덕션 변경 없음.
- G:/내 드라이브/Codex APK/ 에 동일 APK 복사 및 SHA256 확인. 웹 동기화 상태는 별도 확인하지 않음.


## code77 / 1.3.41 — 공유 진입을 휴지통 밖으로 분리 (사용자 정정)
- 사용자: 휴지통 안에서 Instagram을 공유하는 동선은 부적절. 기존 선택 UI는 재사용하되 진입은 휴지통 밖으로 분리.
- MemoryManagementEntry.detailStoryAction: 기존 상세 요약 휴지통 옆에 동일48dp 공유 아이콘, 접근성 라벨 Instagram 스토리 공유. RECORD 경로 그대로, story_selection intent purpose만 전달. 새 selected set/선택 컨트롤러/사진 재선택 UI 없음.
- MemoryDetailSummaryRenderer: 공유 아이콘 뷰 바인딩만 추가. MainActivity 변경 없음.
- MemoryManagementController: 동일 rows/selected/selection()을 공유 목적으로 재사용. 공유 진입엔 닫기+Instagram 공유만, 기존 휴지통 진입엔 닫기+휴지통 이동만. 공유 진입에서 휴지통 복구저널 resume 및 장소 변경 메뉴를 호출하지 않음. 기존 휴지통/장소변경 함수 본문은 그대로.
- 뷰어 1장 공유, 카드 renderer, 사진띠/폰트/크림/투명 PNG 등 결과물은 code76 그대로. 사용자는 현재 code76에서 Instagram 사진띠 여백 실제 결과 확인 중. 여백 임의 변경 금지, 테스트 중 폰 설치로 방해하지 않음.
- API33 native 1~4장 공유 + 실제 컨트롤러: 공유 진입에 휴지통 없음, 취소 후 selected asset keys 동일, 기존 휴지통 진입에 Instagram 없음 PASS. 증빙 .debug/story-entry-native.log.
- 기준 백업 .review/code76-before-share-entry. 빌드/최종 APK 기록은 이어서 갱신. 사용자 실기기 설치 및 수용 전 프로덕션 제출 금지.


## code77 추가 합의 — compact 카드 및 inline 캡션
- 사용자 참고 1/3/4장 이미지와 명시적 승인: 1장도 여백 수정. 2/3장 세로, 4장2×2. 문구 대신 캡션. 카드에서 직접 입력하고 문구 탭 제거/사진·스타일 유지.
- Composer: 단일 사진 내부 padding32→24, caption gap24→16/linegap18→12. 원본 비율/전체 표시와 card-only PNG 유지. 다중 사진은 actual dimensions로 각 행을 비율 배분하여 fixed-slot letterboxing 제거. 4장은2행×2장, 2/3장은세로. 총카드는1760높이 안에서 fit, 사진+padding+caption만.
- PhotoPreview는 동일 geometry를 hit-test/gesture/fit에 사용. 캡션 영역 클릭은 사진 drag/pinch와 분리. 편집 중 빈 캡션 geometry는 표시용으로만 reserve, 실제 저장/공유 PNG에 힌트/커서 없음.
- ShareSheet: 캡션 overlay EditText를 카드 하단에만 표시, 입력 중 사진/스타일 도구 숨김, 헤더 완료/IME완료로 복귀. 기존 폰트/크기/표시 토글은 스타일 유지, date 원본 변경 없음. 입력 text 변경은 decoder worker 재실행 없이 config만 업데이트(키입력시 spinner/flicker 없음).
- MemoryStoryEditorInsets 전용 helper: edge-to-edge API30+ ADJUST_NOTHING + 실제 IME inset padding. 키보드가 editor 전체를 pan하여 헤더가 사라지는 문제를 native screenshot으로 발견/수정. API29는 기존 resize + compat inset.
- Debug/Release 각519 tests PASS. API33 6종 single(가로/세로/정사각, context on/off),1~4장 공유, 독립 entry/선택 보존, 투명 PNG·save·share, fonts/slider/drag/pinch PASS. 실제 Gboard 표시+cursor+header위치+IME위 caption bounds+Done 종료 native검사 PASS.
- 캡처: .debug/compact-caption77/inline-final-insets-fixed.png, selection-strip-editor.png. 합성 테스트 사진만 사용. 실기기 사용자 수용은 아직 대기.
- 로그 .debug/compact-caption-single-native.log / compact-caption-selection-native.log / compact-caption-tests.log / release77-final-build.log.
- MainActivity는 code75 source snapshot과 동일. 다른 장소/삭제/날짜 도메인 코드 변경 없음.


## code77 공식 APK 및 설치 완료
- 1.3.41/code77 Play-signed APK: photoplace-1.3.41-code77-play-signed.apk.
- SHA256 15c145b45564b357ccb949e87ea841aa23e33fd7515403278eca7d6c4c7a7e77. signer b9f9d963... 기존 설치본 동일, 최종 로컬/Play DEX 동일.
- R5KL503VHQR install -r 성공. firstInstall2026-11-09 10:56:31 유지, lastUpdate23:13:13. 원본/메모/앱데이터 삭제 없음.
- G:/내 드라이브/Codex APK/ 동일 APK 복사 및 SHA256 일치. 클라우드 웹 sync 별도 확인 없음.
- internal77 draft only. production75 유지, 검토 제출/공개 출시 없음. 사용자 실기기77 수용/Instagram 최종 결과 확인 대기.


## code78 / 1.3.42 — 최종 두 문제 수정 (사용자 캡처 재현)
- 승인 범위: 공유 선택 화면에 남은 사진정리 문구, 캡션 편집 시 카드 폭 불일치/위치 캡션 중복만 수정. 카드 구성/여백/내보내기 로직 변경 금지.
- 실제 폰77 캡처에서 편집 overlay는 이전 미리보기 크기의 위치/폭을 유지하고 Canvas 캡션이 별도로 남아 중복되는 것을 확인. 키보드 표시/숨김과 도구 패널 숨김이 preview 변환을 바꿀 때 sibling overlay 레이아웃이 갱신되지 않는 경계였음.
- MemoryStoryPreviewStage 전용 FrameLayout: preview sibling의 최종 onLayout 뒤에 동일 card transform으로 EditText overlay를 EXACTLY measure/layout. 이전 LayoutParams 좌표나 초기 크기에 의존하지 않음. 입력 폰트도 실제 카드 축척을 사용해 좁은3장 사진띠 밖으로 텍스트가 튀거나 잘리지 않게 함.
- MemoryStoryPhotoPreview: 편집 중 Canvas footer의 기존 캡션/날짜를 흰 종이로 가림. EditText만 표시. 완료하면 Canvas 캡션/날짜가 한 번만 복귀. PNG export의 Composer/Exporter는 변경 없음.
- MemoryManagementController: 공유 loading은 “공유할 사진을 불러오고 있어요.”, 공유 empty/닫기/옵션도 공유 문맥으로 분기. loading 중 정리할 사진 없음 문구를 동시에 표시하지 않음. 기존 휴지통 문구 및 동작 유지.
- 신규 native MemoryStoryCaptionBoundsProbe: 세로1/2/3/4장 각각 입력 전 최초 상태, 실제 IME 표시/숨김, text 변경/완료까지 exact bounds(±2px), 기존 Canvas 텍스트 0픽셀/Done 후 복귀 검증. 기존 검증은 text 변경 후만 확인해 초기 어긋남을 놓쳤다는 한계를 이번 테스트로 보완.
- 실제 loader I/O를 latch로 보류하여 공유 최초 frame에 공유용 로딩 문구/정리 empty 부재/휴지통 부재 확인. 선택/단일 공유 회귀 native PASS.
- 로그 .debug/caption-alignment78/native.log / selection-regression.log / single-regression.log. 캡처 portrait1-input-fixed.png 및 portrait3-input-fixed.png(폰트 축척 최종 수정 후 재캡처 예정). 개인 폰 캡처는 같은 폴더 local-only, APK/Drive에 첨부하지 않음.
- 테스트/빌드 및 APK 설치 최종 상태는 아래 후속 항목 참고. 사용자 수정본 수용 전 production 제출 보류. 사용자 '잘마무리하고 출시하자' 요청은 최종 검증 후 출시 의도이며, 기존 실기기 확인 계약 유지.


## code78 설치 기록 / code79 추가 정렬 요청
- code78 APK SHA256 202bfe653603321b138c320644d00d379d3900a5b3794f4b50f7e98689ac0849, signer 기존 Play 동일 및 local/Play DEX 동일. R5KL503VHQR install-r 성공(lastUpdate23:33:13), firstInstall10:56:31 유지.
- native1~4장 초기 입력 전/IME표시·숨김/글자변경/Done exact bounds와 비중복 PASS. 최종 입력 폰트는 실제 card scale에 맞춰3장 좁은 strip의 초기 장소명도 잘리지 않음. PNG1/4장 code77과 byte-identical.
- code78 internal draft only, production75 유지. 이후 사용자가 공유 목록 최신 파일이 위에 오지 않는 문제 추가 발견 → code79로 후속 작업.
- MemoryMediaAccess.observeUris()는400개 chunk별 MediaStore 조회 순서를 그대로 반환했음. 공유 모드에서만 MemoryStoryPhotoOrder.newestFirst() 적용. 촬영일 millis 우선/없으면 수정일 seconds*1000 fallback, 동일시각은 ID/assetKey로 안정화. 결과 복사본 정렬, 원본 입력/metadata/selected set 변경 없음. 휴지통 및 장소 변경 순서 불변.
- 순수3개 unit tests: provider/chunk/ID 순서가 아닌 전체 최신순, 날짜 fallback 단위, 새로고침 안정성. Debug/Release 예상각522(최종 결과 로그 확인).
- 사용자78 캡션 수용/79 최신순 수용을 독립 기록할 것. 사용자 최종 확인 전 production 제출 없음.


## code79 최종 합의 — 캡션 편집 가독성 및 최신순
- 사용자가 code78에서3장 strip이 키보드 위에 전체 fit 되면서 캡션이 콩알만해지는 문제를 지적. 참고처럼 사진 상단이 위로 넘어가도 캡션은 입력 가능한 크기를 유지하도록 명시 요청.
- PhotoPreview의 caption-edit 전용 camera: 편집 시작시 축척을 유지하고 최소16sp 가독성에 필요한 축척을 확보. 키보드가 올라와도 높이 fit으로 재축소하지 않으며, 카드 하단/캡션을 키보드 위에 고정하고 긴 사진 상단은 preview clip 밖으로 올림. Done에서는 정상 전체 카드 fit으로 복귀.
- 입력 폰트는 최소16sp. 별도 preview-only 좌표/글자 크기이며 Composer/PNG 결과물은 변경하지 않음. 단일/다중 card 원본/padding/export는code77동일.
- API33 native1~4장: IME 상태 caption>=16sp, 캡션 전체IME위, 최초 캡션 가로잘림 없음, exact input/card bounds, 편집중 Canvas중복없음, 키보드숨김/typing/Done 복귀 PASS. 증빙 .debug/caption-alignment78/readable79-native.log, three-portrait-readable79.png.
- 이 변경과 공유 목록 최신 촬영일순 정렬을 code79/1.3.43에 통합. unit/native회귀 및 최종 빌드는 release79-final-build.log에 기록.


## code79 설치/공식 artifact 완료
- APK photoplace-1.3.43-code79-play-signed.apk SHA256 9e7f5431f9ac9f04a8d0a4b5e0277cb670b51495fe732bde50c9cfcc31888ed9.
- signer b9f9d963... 기존 Play signer 동일, 최종 local vs Play DEX 동일 확인.
- R5KL503VHQR install-r 성공. code79/1.3.43, firstInstall2026-11-09 10:56:31 유지, lastUpdate23:41:05. 사용자 데이터 삭제 없음.
- G:/내 드라이브/Codex APK/ 동일 APK 복사/SHA256 확인. Drive 웹 동기화 별도 미확인.
- Debug/Release 각522/0fail, native1~4장 가독성·캡션bounds·중복·Done 및 단일6종/선택공유 회귀 PASS. MainActivity hash동일.
- 공식 서명 확보용 internal79 draft only. production75 유지, production 검토 제출하지 않음. 사용자에게 code79 최신순/입력가독성 최종 실기기 확인 요청 완료.
- 출시용 변경 설명 release-notes-1.3.43-ko-KR.txt 준비. 사용자 수용 시 검증된 동일79bundle만 승격하고 콘솔 게시개요 관련변경만 최종 제출할 것.

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
