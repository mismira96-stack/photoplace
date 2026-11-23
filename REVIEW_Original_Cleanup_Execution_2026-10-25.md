# 원본 정리 실행 경로 검증 — 2026-10-25

## 범위와 상태
사용자 요청: 실제 휴지통 기능을 수정하고 실기기에서 검증한 뒤 프로덕션 배포까지 진행한다.
최종 후보는 1.3.15 / versionCode 51. 실제 사용자 사진은 읽기 전용 검토만 수행했다. 휴지통·복구 실험은 별도 패키지 com.photoplace.cleanupprobe가 생성한 전용 합성 미디어만 사용했다.
현재 단계: 최종 code51 실제 사진 검토 및 프로덕션 제출 완료. Google 자동 사전 검사/심사 대기. 공개 배포 완료는 아직 확인되지 않았음.

## P0
이번 세션 MainActivity 변경은 onResume/onPause에서 CleanupExecutionController로 생명주기를 전달하는 2줄뿐이다. 신규 계산·선택·검증·배치 판단은 전용 클래스에 있다. 세션 시작 기준 파일은 .debug/cleanup-review-2026-10-25/MainActivity.before-execution.java에 보관했다. 저장소 전체 누적 diff와 이번 세션 범위를 혼동하지 않는다.

## P1 / 수정한 실행 경로
- CleanupExecutionPreflight: UI의 대표 행만 받지 않고 assessment.evidenceCandidates에서 선택된 원본의 모든 검증된 복사 증거를 확장한다. 전체 바이트 SHA-256을 다시 읽고 검토 당시 해시와 비교한다. 전체 선택 집합의 MemoryDependencyGuard를 재검사한다.
- CleanupMediaAccess: 원본 휴지통 상태는 MATCH_INCLUDE 쿼리로 확인한다. 물리 식별자는 MediaStore 버전 + volume_name + _id로 묶는다. external/external_primary 주소 별칭으로 선택한 원본을 보존 대상처럼 사용하는 순환을 차단한다.
- CleanupExecutionPlan: 영구 삭제는 없다. 300장씩 나누며 선택된 전체 계획을 보관한다. 앞의 100장만 처리하고 나머지를 버리던 경로를 폐쇄했다. 기본 선택은 0장이다.
- CleanupExecutionJournal: 원본/복사본 URI, 실제 크기, 기대 해시, 식별자, 전체 계획, 처리 커서, 요청/감사/복구 상태를 no_backup의 AtomicFile에 fsync한 후 시스템 창을 띄운다. Activity 필드의 pendingRequestId만으로 결과를 판단하지 않는다.
- CleanupExecutionController: 시스템 결과 직후 후속 요청을 잠그고 감사한다. 다음 묶음은 감사 통과 후 사용자가 계속을 눌렀을 때만 재검증한다. 프로세스 재시작은 저장된 계획을 재확인하며 자동으로 새 휴지통 요청을 실행하지 않는다.
- CleanupExecutionAudit: 실제 원본 IS_TRASHED와 모든 보존 대상의 전체 바이트 수·기대 SHA-256·식별자·검사 중 안정성을 확인한다. RESULT_OK나 임의 파일을 읽었다는 사실만으로 성공 처리하지 않는다. 취소도 원본이 실제 그대로인지 확인한다.
- 이상 감지 시 나머지를 중단하고 사유를 보관한다. 복구는 createTrashRequest(..., false)로 300장씩 요청하고 원본의 실제 복구 상태와 기대 바이트를 확인한다. 시스템 영구 삭제 요청 및 직접 원본 삭제는 추가하지 않았다.
- CleanupRecoveryNotifier: 알림 권한이 이미 허용된 환경에서는 백그라운드 복구 안내를 제공한다. 권한이 없더라도 저널과 앱 내 복구 안내는 유지한다. 새로운 권한 요청을 추가하지 않았다.
- 옛 CleanupTrashDispatcher는 후보 목록만 받는 실행을 항상 거절한다. 옛 해시 없는 JournalEntry를 CleanupPostTrashAuditor가 성공으로 인증하지 않게 했다. 이전 요청 조회의 DB 오류도 '요청 없음'으로 처리하지 않고 실행을 막는다.
- 오래된 SortResult 수량으로 홈에 '원본 0개가 남음' 카드를 띄우던 경로를 다시 차단했다. 진입점은 위치 앨범의 원본 정리와 정리 완료 화면이다.

## 실제 0장 원인과 후속 보강
code47에서 실제 원본 3,513장 / 내용 일치 3,513장 / 연결 통과 0장을 관찰했다. 상세 사유는 COLLECTION_DEPENDENCY의 일괄 전파가 아니라 SNAPSHOT_CHANGED였다. 긴 바이트 비교 전에 잡아 둔 snapshot이 종료 시점에는 달라져 전체 보류된 경우다.
code49는 CleanupReviewSnapshotProvider로 연결 검사를 시작하는 순간 snapshot을 캡처하고 before/after/재투영에서 동일 상태를 사용한다. 연결 검사 중 변경 여부 검사는 그대로 유지한다. 이 작업 내의 알려진 앨범 경로와 파싱된 DiscoverySnapshot만 재사용한다. 검색 캐시·복사 루프·중복 복사 방지 엔진은 수정하지 않았다. 기록 연결 단계도 실제 처리 수량과 취소를 전달한다.

## 자동 검증
- testDebugUnitTest / testReleaseUnitTest: 각각 412개, 실패 0 / 오류 0 / skip 0.
- lintVitalRelease / bundleRelease 성공.
- 신규 순수 테스트: 같은 크기의 다른 내용, 잘린/없는 복사본, 검사 중 변경, provider 식별자 재설정, 일부 원본만 휴지통 이동, 빈 감사, 주소 별칭 순환, 불완전 영수증, 3,001장 11개 묶음 보존, snapshot 지연 캡처·동일 세대 재사용·캡처 실패 차단.
- 실제 Android DB에서 옛 저널 테이블 조회가 실패하도록 격리 주입했을 때 실패를 전파함을 확인했다. 오류를 null로 삼지 않는다.

## 실기기 검증 (R5KL503VHQR / SM_F971N)
1. 전용 원본 1장 휴지통 이동 → 복사본 해시·실제 휴지통 상태 확인 → DONE.
2. 시스템 창 거부 → 원본 내용과 원래 상태 확인 → CANCELED.
3. 시스템 창 대기 중 전용 복사본 바이트 변경 → 허용 → NEEDS_RECOVERY 및 후속 중단.
4. 이 상태에서 테스트 앱 강제 종료/재시작 → 복구 안내 재개 → 시스템 복구 허용 → 원본 내용 대조 후 CANCELED.
5. 301쌍 시험: 첫 300장 감사 후 300/301 커서 유지. 강제 종료/재시작해도 다음 1장 계획 유지, 나머지까지 DONE 301/301.
6. 별도로 생성한 정상 PNG 301쌍도 300 + 1로 전부 이동·감사 완료했다. 앞선 시험의 일시 보류는 무시하지 않고 전체 재확인 후 진행했다.
7. 완료된 전용 301장 계획에 복구 상태를 격리 주입하고 실제 300 + 1 복구 확인창을 거쳐 전부 복구했다. 저널 CANCELED / restoreOffset=301 확인. 이 시험은 실제 복사본 손실을 추가로 유발한 사례가 아니라 복구 묶음 경계 검증이다.
8. 실제 MediaStore external/external_primary 대상 주소 두 개를 같은 물리 복사본으로 대조해 의존성 검사 통과를 확인했다. 원본과 대상이 같은 물리 행인 선택은 계획에서 거절한다.

증적: .debug/cleanup-review-2026-10-25/device-execution-{success,canceled,corrupt-copy,restored}.json, device-batch-{completed,valid-paused,valid-completed,restored}.json.
시험 전용 패키지 소유자와 UUID 경로가 확인된 합성 미디어 970건을 정리했고 테스트 앱만 제거했다. 본체 앱을 삭제하지 않았다. 본체 최초 설치일 2026-10-20 00:24:37 유지.

## 의도적으로 남긴 제한
- 로컬 사진 + 앱의 복사/앨범 증거가 있는 대상만 다룬다. 동영상과 임의 기기 전체 중복 삭제를 지원한다고 표시하지 않는다.
- 과거 대표사진/모음/메모의 원본 URI 참조를 임의로 재작성하지 않는다. 증명되지 않는 사진은 보류한다.
- 서로 다른 프로세스가 시스템 창 대기 중 파일을 바꿀 수 있다. 따라서 사후 감사와 복구 안내가 필요하며, OS 확인창을 파일 잠금처럼 취급하지 않는다.
- 대량 사진은 전체 바이트 검증 비용이 든다. 이미 처리한 묶음의 복사본도 후속 묶음 전에 보수적으로 재확인하므로 대량 정리는 시간이 걸릴 수 있다.
- R8 설정 변경은 이번 실행 수정 범위에 포함하지 않았다.

공식 API 근거: https://developer.android.com/reference/android/provider/MediaStore (createTrashRequest, QUERY_ARG_MATCH_TRASHED, API36 요청 URI 제한).

## 대량 선택 성능 회귀 수정 (code50)
code49 실기기 로그에서 snapshot projection before 466ms / after 322ms를 확인했지만 이후 Guard 단계에서 단일 워커 CPU가 오래 점유됐다. 날짜마다 Collections.disjoint(refs, removed)를 원본마다 반복하면 Android Set 최적화 방향에 따라 전체 선택을 계속 순회하는 문제를 분석했다.
MemoryDependencyGuard는 동일한 immutable Memory의 날짜 구성과 선택 원본 잔존 여부를 한 번만 계산한다. 두 Set의 교집합은 작은 쪽을 순회한다. 검사를 생략하거나 개별 PASS를 합쳐 전체 PASS로 바꾸지 않는다. 잘못 남은 원본 URI가 한 개여도 해당 Memory는 여전히 보류된다.
3,000개 원본 + 3,000개 날짜 섹션을 동시에 보존/거절하는 회귀 테스트와 작은 날짜 집합이 전체 10,000개 선택을 순회하지 않는 연산량 테스트를 추가했다. 전체 Debug/Release 각각 412개 통과. 해당 대량 선택 테스트군은 데스크톱 JVM에서 Debug 0.108초 / Release 0.076초였으며, 이것을 폰의 전체 사진 검사 시간으로 주장하지 않는다.
최종 MainActivity 기준 diff는 +2/-0의 resume/pause 위임으로 assert 검사했다. 두 줄 모두 실제 소스에 있는지 확인했다.

## 실제 사진 검토 결과 및 원본 바이트 강제
code50 Play 서명 설치본에서 실제 사용자 사진 3,513장 모두 내용 일치, 정리 가능 3,508장 / 보류 5장을 확인했다. 보류 사유는 개별 앨범 정리 이력의 원본 URI 참조다. 선택은 0장으로 유지했고 실제 사용자 원본의 휴지통 요청은 실행하지 않았다.
마지막 code51에서는 MediaStore.setRequireOriginal로 해시 입력에 원본 EXIF 바이트를 강제했다. 원본을 제공하지 못하면 UNREADABLE로 보류하며 다른 스트림으로 대체하지 않는다. 공식 근거: https://developer.android.com/reference/android/provider/MediaStore#setRequireOriginal(android.net.Uri)
Android disjoint 구현 근거: https://android.googlesource.com/platform/libcore/+/5d9f6038f34163399b22320850317d25d084da06/ojluni/src/main/java/java/util/Collections.java
원본 바이트 제공 불가 시 대체/통과하지 않는 회귀 테스트를 추가한 최종 자동 검증은 Debug 412 / Release 412, 실패·오류·skip 0, lintVitalRelease 및 bundleRelease 통과다.
AAB code51 SHA256: 3D3DEE87717FA4591D9B819150BBAFA802702A4F3AC3DFAC202CBAF5175647CF
code51 Play 서명 설치 완료. 실제 사용자 사진의 원본 바이트 강제 검토와 Play 최종 제출 상태를 확인 중이다.

## code51 최종 실기기 결과
원본 EXIF 바이트 강제 스트림으로 재검토해 원본 3,513장 / 내용 일치 3,513장 / 연결 통과 3,508장 / 개별 앨범 이력 참조 보류 5장 확인. 0장 선택 상태를 유지했고 사용자 원본의 실제 휴지통 요청은 0회다. 관련 캡처는 .debug/cleanup-review-2026-10-25/actual-review51-result.png.
최초 설치일 유지 및 51 / 1.3.15 설치를 확인했다. Production 트랙은 이 AAB의 SHA256을 API 업로드본과 대조한 후 versionCode 51 / completed 구성으로 검증·commit했다. API의 completed를 심사 승인이나 실제 배포 완료로 단정하지 않으며 Console 게시 상태를 별도 확인한다.

## 최종 출시 확인
프로덕션 1.3.15(51) 전체 출시 제출 완료. Console: 검토 중인 변경사항 / 일반적으로 발견되는 문제에 대한 빠른 검사 실행 중. 검사 성공 후 자동으로 심사 전송, 관리형 게시 꺼짐으로 승인 후 자동 배포. 아직 공개 배포 완료라고 주장하지 않음.
증적: .debug/cleanup-review-2026-10-25/play-production51-queued.jpg 및 play-production51-result.json.
프로덕션 변경 목록에는 1.3.15 전체 출시 시작만 있으며 관련 없는 변경을 추가하지 않았다.
최종 설치 APK SHA256: 4F06821D523184291999A5A9EF0FF4F61A2AE0D3AFA48858D4365E9F4542D5D8


## 사용자 테스트 전 출시 중단 (최신 상태)
1.3.15(51) 프로덕션 출시 중단 완료. 신규 사용자에게 1.3.9 제공. 이미 설치된 1.3.15는 유지. 사용자 UI 확인 및 명시적 재출시 요청 전까지 재제출/출시 재개 금지.
제출 회수를 시도했으나 제출 활동에서 이미 출시됨을 확인하여 프로덕션 출시 관리 > 출시 중지로 처리했다. Console의 중단 배너와 이전1.3.9 제공을 직접 확인했다. 증적: .debug/cleanup-review-2026-10-25/play-production51-halted.jpg.
남은 UX: 원본 폴더/유지 복사본 구분 불명확, 보류 항목 우선 노출 및 비활성 버튼 설명 부족, 수천 장의 개별 목록. 코드 안전성 승인은 사용자 UX 승인을 대신하지 않는다. UI 수정은 로컬 테스트 빌드로만 진행한다.
