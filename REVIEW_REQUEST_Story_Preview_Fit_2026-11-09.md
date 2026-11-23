# code75 Story 미리보기 확대 최종 리뷰 요청

- WORKLOG_2026-11-09.md의 code75 항목 참고.
- code74 대비 기존 production 변경은 MemoryStoryPhotoPreview 및 ShareSheet 미리보기 layout params만. geometry/Composer/export/저장/Viewer/MainActivity 모두 불변.
- 실제 카드 기준 fit 85% 가로/94% 세로, 중앙/비율 유지. 기존 사진 gestures 역변환 및 clipRect 확인.
- API33 native6종 PASS, header 침범 검사6회 PASS. 실제 가로/세로 PNG byte-identical 증빙.
- 검증/캡처: .debug/preview-fit-native.log 및 .debug/preview-fit75-2026-11-09/verification.json, portrait-final75.png/landscape-final75.png.
- 사용자 최종 미리보기 확인 뒤 출시 요청. 아직 사용자 확인/독립 리뷰 미완료이므로 제출 금지.
