# 안티 critical audit 재검토 및 실제 폰 복귀 이슈

## 판정
배포 보류 유지. 리뷰의 위험은 유효하지만, 모든 설명을 사실로 승인하지 않는다. 아래 소스 수정은 폰 code53에 아직 반영하지 않았다.

## 실기기 증거
- 현재 폰 화면: 정리 가능 2,008장 / 보류 266장 / 선택 0장. 추가 실행 중이 아닌 검토 대기 화면.
- PhotoPlace 알림 7203: `1500 / 3508장 확인 · 앱에서 남은 작업을 확인해 주세요`.
- 3,508 - 1,500 = 2,008. 이미 처리된 사진이 후보에서 제외된 수량과 일치한다. 이 수량만으로 파일 전체 무결성을 증명하지는 않는다.
- CleanupAuditWorker 여러 묶음 SUCCESS 및 FGS 정상 시작/종료 확인. 이 폰에서 FGS 예외가 관측된 것은 아니다. JobScheduler에는 HAS_FOREGROUND_EXEMPTION이 표시되어 있었다.
- 실제 실행 원장 파일은 Play 서명 앱의 private storage이므로 직접 읽지 못했다. 정확한 종료 상태는 미확정.

## 새로 확인한 우선 수정
CleanupSessionAudit는 일부 묶음을 완료한 뒤 다음 묶음이 NOT_APPLIED면 CANCELED로 종료했다. CleanupController는 inactive 저널을 재개하지 않으므로 새 검토 화면으로 들어간다. 이 코드 경로가 관측된 동작을 설명한다. OS 확인 취소/실행되지 않음의 정확한 트리거는 아직 확정하지 않는다.

수정: 이전 완료 offset > 0이고 다음 묶음이 적용되지 않았으면 PAUSED로 유지. 다음 이동은 사용자의 명시적인 계속 + 새 전체 안전 검증 + OS 승인이 있어야 한다. 첫 묶음 취소(offset 0)는 CANCELED 유지. 과거 취소 저널을 무조건 되살리는 마이그레이션은 하지 않는다. 이미 열린 2,008장 새 검토는 그대로 사용할 수 있으며 재복원 불필요.

## 리뷰 5건 판정
1. **FGS 제한 / 재시도: 수용, 설명 일부 정정.** 백그라운드면 무조건 금지는 아니며 예외가 있다. 코드의 get() 예외는 잡히므로 직접적인 앱 크래시가 필연적이지 않다. 그러나 승격 실패 시 감사 없이 retry만 반복하는 구조는 취약하다. 사후 감사에서 FGS 의존을 제거하고 일반 Worker + 선택적 진행 알림으로 변경. 실행 실패의 명시적 retry는 최대 3회 재시도 뒤 failure로 종료하며 원장은 보존. 일반 Worker도 10분 실행을 보장하는 것은 아니며 OS 중단/할당량을 받는다. 장시간 대용량 감사의 체크포인트는 후속 검증 과제.
2. **750ms polling: 개선 권고 수용, 치명적 누적 설명은 정정.** 기존 루프는 조회가 끝난 뒤 다음 조회를 예약하므로 스스로 무제한 큐를 쌓는 구조가 아니다. 다만 동기 조회 및 주기적 DB 읽기는 불필요하고 같은 Executor에서 지연될 수 있다. LiveData/리스너 전환은 별도 follow-up으로 남김. Pure Activity이므로 observeForever를 쓰면 pause/취소/종료에서 observer 제거를 반드시 구현해야 한다.
3. **인덱싱 지연: 부분 수용.** 기존 sleep은 250ms * 2 = 500ms이며 '750ms'는 계산 오류. OS trash 작업은 결과 콜백 이전 완료가 공식 계약이다. 부가 revision 변경까지 항상 즉시 안정화되는 것은 아니므로 불안정한 snapshot revision에 한해 최대 6회 검사 / 500ms * 5 대기. 실제 메모 의존성 손상/권한 문제를 무조건 지연 재시도하지 않음. 매 시도마다 새로운 전체 projection을 만들고 실패를 성공으로 취급하지 않는다.
4. **창 재생성: 수용, 빈도 설명 정정.** 기존 해시 진행률 갱신은 이미 setMessage로 처리했다. progress()는 단계 전환/재진입 때 창을 재생성하므로 기존 표시 창을 재사용하도록 수정.
5. **채널 IPC: 수용.** 프로세스당 한 번만 생성, 성공한 뒤에만 volatile 플래그 설정. 일반 Worker의 선택적 진행 알림 갱신/제거를 별도로 제공.

## UI 수정
- CleanupUiStyle로 기존 WorkingProgressBanner/앨범 카드의 글자, 보라색, border, radius18, 버튼 스타일 공유.
- 검토 화면의 중복된 거대 수량 블록 제거. 수량은 탭에 유지. 보류 탭의 거대한 실행 불가 버튼 제거.
- 검사/실행은 전체 화면 대신 내용 높이의 작은 진행 카드. 긴 본문은 최대 화면 45% 높이까지 스크롤.
- 선택 목록은 하단 88% 패널. 실제 원본 위치와 보존할 위치앨범 구분 유지. 보류 원본에는 '휴지통으로' 대신 '원본 위치' 표시.
- 반복된 제목/숫자 제거, 검증 수량은 건으로 표시. 시스템 MediaStore 승인창은 OS 기본 UI 유지.

## 검증과 남은 일
- compileDebugJavaWithJavac 및 testDebugUnitTest/testReleaseUnitTest/assembleDebug 성공.
- revision 재시도/영구 오류/중단/재시도 상한 신규 4개 테스트, 일부 완료 후 취소 상태 유지 테스트 보강.
- MainActivity는 이번 작업 전 baseline과 SHA256 동일. 사진 복사/검색 캐시/실제 사진에 쓰기 없음.
- 최신 스타일 픽셀 검증 및 새 일반 Worker의 실기기/대량/프로세스 종료 검증 필요. code53 실사용 테스트 결과와 최신 소스의 테스트 결과를 혼동하지 않는다.
- 새 APK 설치, Play 제출/출시, 사용자 사진 복원·휴지통 이동 수행 안 함.

## 공식 근거
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/reference/androidx/work/ListenableWorker
- https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running
- https://developer.android.com/reference/android/provider/MediaStore#createTrashRequest(android.content.ContentResolver,%20java.util.Collection%3Candroid.net.Uri%3E,%20boolean)
