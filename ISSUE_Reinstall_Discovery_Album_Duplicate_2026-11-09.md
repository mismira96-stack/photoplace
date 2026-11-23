# 재설치 후 발견 기록/복원 앨범 이중 카드 — 조사 기록

작성일: 2026-11-09
상태: 원인 경로 확인 / 구현 전 범위 싱크업 대기 / 앱 코드·사용자 데이터 변경 없음

## 사용자 재현 순서
1. PhotoPlace 삭제.
2. 1.3.39/code75 다시 설치.
3. 설정에서 발견장소/정리기록 다시 만들기 실행(사용자가 설명한 단계).
4. 사진 속 장소 찾기 실행.
5. 발견 기록에 평창1장 + 평창에서127장, 태백46장 + 태백에서2장, 성남도 별도 카드. 반복 재현.

## 실기기 읽기 전용 확인
- R5KL503VHQR 설치: 1.3.39/code75, firstInstall/lastUpdate 2026-11-09 10:56:31.
- MediaStore URI query만 실행. 사진 복사/이동/삭제/EXIF/날짜/앱 DB 변경, 재설치, 스캔 실행은 하지 않음.
- Pictures/평창에서/ 사진127, Pictures/태백에서/ 사진2, Pictures/성남에서/ 사진2312 확인. 화면의 ‘…에서’ 카드 수량과 대응.
- 원본 쿼리 .debug/record-duplication-2026-11-09/album-path-query.txt. 앱은 Play release non-debuggable이므로 내부 snapshot/manual DB를 직접 읽지는 않음. 정확한 기기 내부 국가 필드 값은 미추출.

## 코드에서 확인된 경로
- MainActivity.collectExistingAlbumSummaries: Pictures/%에서/ 경로로 앨범 요약을 재구축. 위치/국가 metadata enrichment는 ‘lightweight’ 주석으로 생략되어 새 AlbumSummary.countryCode/adminArea 등이 비어 있음.
- AlbumSummaryHistoryStore.writeRebuilt: 위 요약을 새 세션으로 저장. 재설치로 이전 OrganizationLink/identity 연결이 없다면 이 새 요약만 존재.
- MainActivity.showMemoryBrowserScreen(true) / MemoryBrowserEntry.build(includeAlbums=true): 발견 기록 화면이 raw discovery + organized album을 함께 노출.
- MemoryRepository.canonicalPlaceName은 이미 ‘에서’를 제거한다. 단순 접미사 제거 누락 문제 아님.
- MemoryRepository.placeIdentityKey: countryCode가 비면 빈 키를 반환. fallback merge 인덱스에 들어가지 못함.
- fromOrganizedAlbum은 국가가 비어 있으면 OverseasMemoryGrouper.countryCodeFor로 보완하지만, 이는 해외 국가 추정용이며 국내 일반 장소의 KR 복원은 못함.
- 이후 GPS discovery: ‘KR|평창’과 복원 앨범의 국가 없는 ‘평창’이 같은 대상으로 연결되지 않아 두 카드가 남을 수 있음. 사용자 경로 및 수량에 부합.
- 수동 위치 연결 projection도 별도 경로가 있지만 이번 재설치→재구축→재탐색 흐름의 가장 직접적인 원인은 위 앨범 복원 identity 정보 누락.
- Story의 ‘성남에서’ 문구 formatter는 caption 표시 전용이며 discovery/앨범 이름을 쓰지 않음. 스토리 변경을 되돌릴 근거 없음.

## 수정 시 반드시 지킬 범위
- 파일을 합치거나 삭제/이동하지 않는다. 사진 원본/GPS/촬영일/메모/stableMemoryId 보존.
- 이름에서 ‘에서’만 떼어 무조건 합치지 않는다. 동명 장소/다른 국가/서로 다른 날짜/수동 기록/사용자 메모의 오연결 방지.
- 재설치·앨범 요약 재구축·discovery 재생성에서도 동일 장소임을 확인할 수 있는 복구 연결 경로를 전용 resolver로 검토. 단순히 카드를 숨겨서 수량/내용을 잃지 않게 할 것.
- 기존 discovery 캐시 및 scan 성능 유지. 매 사진 full hash/재스캔/전체 EXIF 분석을 브라우저 로딩마다 추가하지 않는다.
- 같은 테스트 경로와 평창/태백/성남 케이스, 국가 불명/동명 장소/메모 보존을 포함해 재현 및 검증 후 사용자 테스트. 사용자 확인 전 추가 배포 금지.

## 현재 결론
국가/연결 정보 없이 재구축된 앨범과 새 GPS 발견 기록을 합치지 못하는 경로가 확인됨. 실제 폰 내부 원장의 국가 필드는 직접 확인하지 않았으므로, 최종 수정 검증에서 이 경로를 synthetic fresh-install fixture로 재현해야 함.
사용자와 수정 범위를 먼저 맞춘 뒤 착수. 현재는 진단/문서만, production source 변경 없음.
