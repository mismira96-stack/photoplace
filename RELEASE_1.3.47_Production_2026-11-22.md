# PhotoPlace 1.3.47 (83) 프로덕션 출시 — 2026-11-22

## 승인 및 범위

- 사용자 최신 테스트 피드백: 기존 스토리 동작은 정상, 가로2·3장 하단 여백 축소안은 변화가 크지는 않지만 나쁘지 않다고 확인. “오늘꺼까지만 마무리하고 1차 출시” 요청으로 이번 범위 프로덕션 제출 진행.
- 포함: 공유·저장 고화질, 나란히/스크랩북 2·3장 가로 한 줄·4장2×2, 사진띠 세로 스택, 편집 도구 고정, 가로2·3장 하단 비례 축소, 퀵뷰티 촬영일/GPS 연동 보완.
- 제외: 자유 배치/투명 배경 신규 템플릿은 다음 개선.
- 퀵뷰티 실제 연동은 사용자가 아직 직접 확인하지 못했다고 알린 상태에서 출시를 요청했다. 해당 부분의 검증 근거는 후보 동일성·모호성·일시 실패/캐시·체인 테스트 및 코드 리뷰이며, 실사진 연동 검증 완료로 기록하지 않는다.

## 검증 및 산출물

- 코드 기능 변경 없이 app/build.gradle 버전만 1.3.47(83)로 변경했다.
- Gradle :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintVitalRelease :app:assembleRelease :app:bundleRelease 성공. 최신 테스트 결과 Debug·Release 각586개/실패0, 변경 없는 테스트는 UP-TO-DATE 재사용.
- AAB: photoplace-1.3.47-code83-production.aab
- AAB SHA-256: DD33C03B7CC40BC3849B8921221521B89D3FE7C02C0F298ABC911A31F7E452F1
- Release APK: photoplace-1.3.47-code83-production.apk
- APK SHA-256: 21F1129E0542158C766A72C67C6D90B8D57547BDB0CB21C75E17F45FCCCADC91
- apksigner 및 jarsigner 서명 검증 성공. APK는 업로드 키 서명이며 Google Play 설치용 서명과는 별개이므로 사용자 Play 앱 위에 이 APK를 임의로 설치하지 않았다.
- 한국어 출시 노트: release-notes-1.3.47-ko-KR.txt

## Play 진행 증거

- 제출 전 API: production code82 completed, 업로드 번들 최대82. code83 미사용 확인.
- 게시 개요: 최근 게시11월17일, 관리형 게시 해제, 다른 대기 변경 표시 없음 확인.
- Google Play API에서83번들 업로드 및 로컬 SHA-256 일치 확인. production 1.3.47/code83 completed(전체 출시 대상)로 편집 검증·커밋 성공.
- API의 completed는 트랙 요청 상태이며 공개 완료 증거로 간주하지 않는다. Console 실제 심사/게시 상태는 아래 최종 확인을 따른다.

## 최종 Console 확인

- 게시 개요의 “검토 중인 변경사항”에 **프로덕션 / 1.3.47 / 전체 출시 시작** 항목 표시 확인.
- 현재 “일반적으로 발견되는 문제에 대한 빠른 검사 실행 중”. 검사 성공 즉시 변경사항이 검토를 위해 전송된다는 Console 안내 확인.
- 관리형 게시가 사용 중지되어 있어 심사 승인 후 자동 공개 대상이다. 현재 공개/심사 승인 완료는 아니다.
- 다른 스토어 등록정보·정책 변경은 제출 목록에 보이지 않았고 이번 프로덕션 릴리스만 표시됐다.
- 게시 개요: https://play.google.com/console/u/0/developers/7774153056624295744/app/4974837404467270049/publishing
- 추가 자동 감시/일정은 생성하지 않았다.
