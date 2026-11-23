# ANTI Story 복원 및 최소 수정 검토 요청 — 2026-11-08

## 현재 상태
- 대상 1.3.33/code69. 사용자 폰 설치 완료. 프로덕션 제출 금지, 사용자 확인 대기.
- code68 UI는 사용자가 거절했다. code67/68의 탭 UI·카드 제거·문구 드래그·기본값 변경을 이번 검토 승인 근거로 사용하지 말 것.
- 최초 Story 4개 파일을 ANTI code66 SHA256과 동일하게 복원하고 원래499개 테스트/기존 카드·2/4장·전체 메뉴·폰트/크기·공유 연결을 먼저 검증한 뒤 수정했다.
- 복원 증빙 `.debug/manual-place-2026-11-08/story-anti-restore-proof.json`, `story-anti-restored-ui.log`.

## 사용자가 싱크업한 범위
1. 완성 카드의 여백이 아니라 **편집 화면의 미리보기 높이를 조절하여 아래 설정을 더 보이게** 한다.
2. 원래 카드/1장/사진띠2~4장/기존 전체 메뉴/폰트·글자 크기·배경 옵션을 보존한다. 탭으로 숨기지 않는다.
3. 상단은 아이콘 단독이 아닌 ‘스토리 공유’ 텍스트.
4. 사진별 크기 및 위치 조절. 원본은 그대로 읽고 스토리 렌더링만 변경한다.

## 변경 파일/계약
- MemoryStoryShareSheet: 기존 ScrollView와 모든 설정을 그대로 보존. 기존 헤더/공유 버튼만 상단 행에 연결. 미리보기 ImageView만 제스처용 View로 교체. 기존 토글·폰트·크기·배경/공유 로직 유지.
- MemoryStoryPreviewFit: 실제 측정한 나머지 설정 높이로 미리보기 높이만 조절. 1장 최소180dp, 사진띠 최소240dp; 작은 화면/4장에서는 원래 스크롤 허용. 메뉴 숨김 없음.
- MemoryStoryPhotoPreview: 원래 Composer의 카드/메타를 그대로 그린다. 1손가락 pan/2포인터 pinch만 제공. 0.5~4배, UI 프레임마다 decode/Bitmap 생성 안 함.
- MemoryStoryPhotoEdits/Config: URI별 불변 편집 값. 선택 순서가 바뀌어도 다른 사진으로 편집 값이 넘어가지 않음. 폰트/글자 크기/모드 변경 시 편집 값 보존.
- MemoryStoryComposer: 카드 박스/그림자/테두리/글자 배치 원본 유지. 사진을 그리는 rect에만 fit/zoom/pan. 기본 fit으로 전체 사진을 보이며 사용자 제스처에 따라서만 crop.
- MemoryStoryExporter: 원본 이미지의 비율/EXIF 방향을 지키는 sampled ImageDecoder를 먼저 사용. 기존 비디오/실패 fallback과 FileProvider/Instagram Intent는 유지.
- MainActivity 및 다른 뷰어/장소/삭제 production 파일은 code66 snapshot과 동일. ‘에서’/우측 정렬/문구 드래그/새 글자 기본 크기 등 거절된 확장 변경은 이번 범위에 포함하지 않음.

## 검증
- Debug/Release 각각501개, 실패/오류/스킵0. release APK/AAB 및 lintVitalRelease PASS.
- API33 별도 패키지: portrait1/landscape1/strip2/strip4, 모든 기존 옵션, 실제 font 선택/크기 SeekBar, 별도 사진 pinch/pan, 다른 사진 값 보존, 원래 종이 카드 픽셀 존재 PASS.
- 로그: `.debug/manual-place-2026-11-08/story69-final-ui.log`, `story69-final-build.log`.
- 검증용 fixture는 com.photoplace.memoryprobe 전용. 외부 공유/개인 사진 이동·삭제 없음.
- 실제 폰69에서 카드·모드·전체 설정/폰트가 돌아온 상태와 landscape/context 설정을 캡처 확인. 사용자 폰이 채팅 화면으로 바뀌어 나머지 물리7개 캡처는 중단. 에뮬레이터 결과를 물리7개 완료로 과장하지 않음.
- 공유 버튼은 wiring/Intent 기존 경로 보존까지 확인, 이번 테스트에서 Instagram으로 실제 사진을 전송하지 않음.

## 리뷰 우선 질문
- 카드/메뉴/모드가 원본과 동일하게 살아 있는가?
- PreviewFit이 특정 기기/폰트/IME 조건에서 메뉴를 숨기거나 리레이아웃 루프를 만들지 않는가?
- Photo edit가 URI별로 유지되고 소스 로딩 실패 시 잘못된 사진을 공유하지 않는가?
- 사용자 승인 전 공개 출시하지 않는 상태가 유지되는가?
