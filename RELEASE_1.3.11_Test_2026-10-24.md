# PhotoPlace 1.3.11 (41) — 데이터 보존 실기기 테스트 설치

- 날짜: 2026-10-24 (Asia/Seoul).
- 사용자 지시에 따라 Phase1 원장 + 앨범 UI 후속 수정본을 연결폰 SM_F971N에 설치했다.
- 버전: 1.3.11 / code41 / target36 / 앱 이름 `앨범 정리`.
- Play internal **draft**만 갱신했다. 프로덕션 배포·검토 제출은 요청/수행하지 않았다.
- Google generated universal APK의 앱 서명을 확인한 뒤 `adb install -r` 성공. 삭제/재설치 없음.
- firstInstallTime `2026-10-20 00:24:37` 유지, READ_MEDIA_IMAGES/VIDEO 권한 granted 유지.

## 산출물

- AAB: `C:/work/projects/PhotoPlace/photoplace-1.3.11-code41-test.aab`
- SHA256: `EF578D3F2A9089F1FF189C0974AED102C60626C7DA7866A0E062485B5A04CA50`
- 설치 APK: `C:/work/projects/PhotoPlace/photoplace-1.3.11-code41-play-signed.apk`
- SHA256: `F17B1B144F558F3E3CB9273C0C0853C77526021F2C76C46FF054DB1BBFB05123`
- 앱 서명 SHA256: `b9f9d963a583dd74d03b5485b827fea997afeb17100cb0b201a01aa264d6d31d`.
- `aapt2`로 package/version/label 확인, 다운로드 APK의 DEX에서 CleanupEvidenceStore/Session, openCompletedAlbums, photoLabel이 실제 포함됨을 확인했다. 기존 code40에는 이 네 심볼이 없었다.

## 포함한 변경

- Phase1: 복사 실제 source/target 쌍과 바이트 수의 미검증 원장, 유계 트랜잭션 체크포인트, 저장 장애와 복사 결과 분리.
- 상세 화면은 실제 resolved refs에 남은 원본 사진이 있을 때만 앨범 추가 버튼 표시.
- 완료 후 보기 3곳은 이전 검색·스크롤 UI 상태를 초기화해 앨범 목록 맨 위로 이동.
- 사진/영상 아이콘 유지 + 장/개 단위. 미구분 예전 합계는 별도 표시.
- 원본 정리/휴지통 이동은 계속 꺼짐. Phase2 미구현.
- 릴리스 노트: release-notes-1.3.11-test-ko-KR.txt.

## 확인 및 한계

- 앞선 source 검증 Debug351/Release351 전건 통과, Debug/Release APK와 lintVitalRelease 성공. 이번 버전 번호 변경 후 bundleRelease 성공.
- 새 설치 홈 실행 및 기존 장소 카드 표시 확인.
- 발견 기록 UI에서 사진 아이콘 + 950장 / 영상 아이콘 + 23개, 성남 2,266장 / 30개 및 전체 접근성 텍스트 확인.
- 추가 버튼/완료 후 보기의 새 복사 작업 실기기 회귀는 사용자 확인 대기. 개인 사진을 자동 복사·이동·삭제하지 않았다.
- 새 원장의 실제 개인 사진 복사 기록을 Play 앱 private DB에서 직접 추출한 것은 아니다. 원장 검증 근거는 앞선 격리 기기의 실제 SQLite/SortJob 시험이다. 향후 읽기 전용 검토 UI에서 실제 작업 기록을 확인해야 한다.
- 청주/서초동/용인의 버튼 미노출은 계속 조사 중. 이번 목록 탐색 중 사용자가 폰을 브라우저로 전환했고 이를 늦게 확인해 스크롤 입력이 다른 화면에 전달됐다. 이후 폰 조작을 중단했다. 브라우저 상태/내용을 앱 진단 근거로 사용하지 않는다.
- 오래된 로컬 device_discovery_snapshot*.json은 8월 자료여서 현재 장소 상태의 근거로 쓰지 않았다.

## 사용자 확인 순서

1. 발견 기록 아이콘 옆 숫자 뒤에 사진 장/영상 개 단위와 큰 숫자 쉼표가 붙는지.
2. 아직 만들지 않은 작은 장소 한 곳에서 만들기 버튼·진행률·완료 화면 확인.
3. 사전에 위치 앨범 목록을 아래로 내려둔 상태여도 완료 후 보기에서 최상단의 새 앨범을 볼 수 있는지.
4. 그 장소로 돌아왔을 때 실제 추가 원본 사진이 없다면 ‘앨범에 추가’가 숨겨지는지. 추가할 사진이 남아 있다면 표시되는 것이 맞다.

이 테스트는 중복 원본 정리 실행을 포함하지 않는다.

로그: .debug/album-flow/test41-build.log, test41-upload.log, test41-ui.xml, test41-browser.xml.
