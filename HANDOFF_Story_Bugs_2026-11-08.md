# Story code71 사용자 반려 — 다음 세션 인수인계

작성일: 2026-11-08 (KST)
상태: **HOLD / 사용자 반려 / 출시 불가 / 작업 중단 요청**

## 오늘 종료 지시
사용자: “오늘은 그만하자, worklog랑 버그만 남기고 다른 채팅창으로 옮겨가야겠다”, “저 화면 뭐가 문제인지 알겠지? 너의 마지막이 저거야”.
이후 작업은 문서 정리만 수행. 코드 수정/빌드/설치/폰 조작/공개 출시 하지 않음.

## 실제 확인된 버그 — 최우선
- 사용자가 마지막 수정본(code71)에서 올린 **스토리 편집 화면**: 동일 사진이 9:16 미리보기 배경에 크게 한 번, 중앙의 흰 폴라로이드 카드 안에 다시 한 번 표시됨.
- 위아래에 검은 띠/영역이 남아 있음. 카드 주변이 깔끔하게 비어 있는 단일 카드 결과가 아님.
- “바깥 배경 제거 완료”로 간주할 수 없음. 빌드/서명/설치 성공은 시각적 정상 동작 증거가 아님.
- 이 캡처에는 문구가 표시되지 않아 오른쪽 정렬의 실제 결과는 검증되지 않음.
- 캡처 경로: `C:/Users/mismi/AppData/Local/Temp/codex-clipboard-61836a2f-4a71-4fdb-94e6-721146a3d126.png`
- 프로젝트 내 영구 보존 복사: `.review/story71-user-rejected-2026-11-08.png`

## 원인: 미확정, 다음 세션에서 조사
code71은 Composer 단일 canvas에 PorterDuff.CLEAR, Exporter에 PNG 및 Instagram interactive_asset_uri를 적용했다. 미리보기는 같은 Composer를 Android View의 Canvas에 직접 호출한다. 투명화/하드웨어 View 합성/이전 프레임 잔상/실제 export 파일을 각각 확인해야 한다. **사진이 두 번 보이는 원인을 아직 입증하지 않았음.** Instagram이 원인이라고 단정하거나 폴라로이드 카드를 삭제해서 가리지 말 것.

## 사용자가 최종 승인한 제품 범위
- 사진 한 장만 Instagram Story로 공유. 사진띠/여러 장 선택 제거.
- 실제 폴라로이드 카드 유지. 카드와 바깥 9:16 배경을 혼동하지 말 것.
- 가로/세로 사진 전체 비율 유지, 사진을 크게, 가로 사진은 가로 카드에 맞는 높이.
- 문구/날짜 카드 내부 오른쪽 정렬.
- 풀스크린 편집, 사진/문구/스타일 탭, 선택 탭 옵션만 표시.
- 상단 ‘스토리 공유’ 텍스트 유지. 사진 drag/pinch, 폰트/크기 옵션 보존.
- 다음 구현 전에 **현재 실패 화면과 최소 수정 범위부터 사용자와 다시 싱크업**. 다른 기능 변경 및 추가 UI 개선 금지.
- 실제 사용자 확인 전 검토 제출/출시 금지.

## 현재 설치/빌드 상태
- 폰 R5KL503VHQR: 1.3.35/code71, install -r Success, lastUpdateTime 2026-11-08 21:38:10 KST.
- APK: `photoplace-1.3.35-code71-play-signed.apk`
- SHA256: `217118fa42440bd136792c40a61841534966b54075627e9d2e3804b9d3819490`
- signer: `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d`
- code71 assembleRelease/bundleRelease 成功; local/Play DEX 일치. 신규 unit/native 시각 검증 없음(사용자 즉시 빌드/설치 지시).
- Play internal71 draft만 저장(동일 서명 APK 확보 목적). 마지막 API 관측 production56 completed / internal completed37 유지. 검토 제출/공개 출시 안 함.
- code71은 사용자 반려 상태. 기존 code69 테스트 PASS를 code70/71 시각 검증으로 재사용하지 말 것.

## 소스/복원 기준
저장소 `C:/work/projects/PhotoPlace` (OneDrive 폴더는 소스 아님).
- code71 변경: MemoryStoryComposer.java (바깥 투명화/오른쪽 정렬), MemoryStoryExporter.java (PNG/스티커 URI) 및 버전.
- 직전 code70 원본: `.review/story70-before-transparent/MemoryStoryComposer.java`, `MemoryStoryExporter.java`.
- code69 원본: `.review/story69-before-single/`.
- ANTI 정상 code66 원본: `.review/story-editor-before-code67/`, `.debug/manual-place-2026-11-08/code66-source-snapshot.json`, `story-anti-restore-proof.json`.
- code68 거절본 `.review/story68-rejected-2026-11-08/` 재적용 금지.
- MainActivity/사진 원본/GPS/촬영일/메모/삭제 기능은 이번 문제 해결 범위 밖.

## 다음 세션 최소 확인 순서 — 실행은 재개 합의 후
1. code71 실제 미리보기와 cache PNG를 분리해서 확인: 사진 이중 표시/검은 띠가 어디서 생성되는지 입증.
2. 사진/문구 포함 모두에서 단일 카드 + 바깥 투명 alpha 확인. 가로/세로 둘 다 실제 렌더 확인.
3. 문구·날짜 오른쪽 정렬과 카드 유지 확인.
4. 이후 APK 업데이트 설치 및 사용자 Instagram 결과 확인. 승인 전 출시 금지.
