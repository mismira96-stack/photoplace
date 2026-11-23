# PhotoPlace 공유·저장 화질 점검 · 2026-11-21

## 확인된 처리 경로

- 인스타 공유와 카드 저장은 모두 MemoryStoryShareSheet.renderCardForUris를 사용한다. 원본의 긴 변을 1440px 이하로 디코딩하고, 1080px 기준 좌표의 카드만 잘라 PNG로 출력한다. 최종 파일은 항상 1080×1920이 아니며 카드와 프레임·문구 높이에 따라 달라진다. [공통 내보내기](app/src/main/java/com/example/gallerysorter/MemoryStoryShareSheet.java#L581) · [디코딩](app/src/main/java/com/example/gallerysorter/MemoryStoryExporter.java#L66) · [카드 렌더링](app/src/main/java/com/example/gallerysorter/MemoryStoryComposer.java#L200)
- **Follow-up · 원본 축소 후 확대 손실.** 예를 들어 3000×4000 사진은 1080×1440으로 먼저 줄어든다. 편집에서는 최대 4배 확대를 허용한다. 이때 일부 픽셀을 크게 그리므로 원본에서 필요한 부분을 충분한 해상도로 읽는 방식보다 세부가 흐려질 수 있다. PNG 저장은 이 단계에서 없어진 디테일을 복구하지 않는다. 단일 세로 사진도 구성에 따라 사진 영역의 긴 변이 1440px를 넘을 수 있다.
- **Follow-up · 저장 결과도 스토리 크기 기준.** 고해상도 저장 경로가 별도로 없어 갤러리에 저장한 결과를 원본과 확대 비교하면 디테일 차이가 생길 수 있다. 공유와 저장은 PNG여서 현재 코드에 JPEG 저품질 재압축은 없다. 인스타 수신 이후의 처리와 실사진 전후 결과는 아직 측정하지 않았다.
- 최소 개선 후보는 편집 확대율과 출력 사진 영역에 맞춰 원본 디코딩 크기를 정하고 메모리 상한을 두는 것이다. 저장 해상도를 더 높이는 선택은 별도 범위로 합의가 필요하다. 현재 프레임·문구·사진 순서·원본 보존을 기준으로 한다. 앱 코드 수정 없음.

## 자동 갱신 폴더 확인

- 앱 복귀 자동 확인과 수동 분석은 동일한 getSelectedSourcePaths를 사용한다. 설정의 분석할 폴더를 정확한 relative_path 일치 조건으로 조회하므로 하위 폴더는 별도 선택이 필요하다. 유효한 선택이 없으면 카메라 폴더로 되돌아간다. [설정 읽기](app/src/main/java/com/example/gallerysorter/MainActivity.java#L2191) · [자동 스캔](app/src/main/java/com/example/gallerysorter/DiscoveryAutoRefresh.java#L116) · [경로 조건](app/src/main/java/com/example/gallerysorter/IncrementalMediaScanner.java#L76)

## 공유·저장 화질 개선 구현

- 사용자 승인으로 공유·저장의 카드 출력 기준을 2160px로 높였다. 카드의 실제 크기는 기존처럼 사진 비율·프레임·문구에 따라 정해지며, 이전 대비 가로·세로 픽셀 수가 각각 두 배가 된다.
- 먼저 작은 이미지로 사진 비율을 확인하고, 출력 슬롯과 편집 확대율에 필요한 크기로 원본을 다시 읽는다. 원본이 작은 경우 디코더가 불필요하게 확대하지 않는다. 사진마다 긴 변 최대 4096px, 전체 소스 디코딩 약 1200만 픽셀 예산을 적용했다. 최대 확대에서는 이 상한에 따라 선명도가 제한될 수 있다.
- PNG 공유·저장 및 기존 프레임·순서·원본 사진 보존 경로를 사용한다. 앱 내부 미리보기의 디코딩 상한은 기존대로다. 인스타 수신 이후 처리와 사용자 실사진의 시각적 비교는 미확인이다.
