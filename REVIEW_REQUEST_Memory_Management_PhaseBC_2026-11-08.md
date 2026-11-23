# 메모리뷰 장소 지정·일반 휴지통 Phase B/C 리뷰 요청

상태: 구현 및 API33 에뮬레이터 통합 검증 후 독립 리뷰 요청. **프로덕션 승인 요청이 아니며 실제 폰 검증·출시는 별도**다. Phase A R1 승인으로 이번 변경을 승인한 것으로 간주하지 않는다.

## 제품 범위와 실행 동선

- 메모리 목록의 **위치 없는 사진 정리** → 사진/동영상 선택 → 기존 위치기록 또는 이름을 입력한 새 기록에 연결. GPS/EXIF와 실제 파일 위치는 변경하지 않는다.
- 메모리 상세 **사진 선택 · 정리**, 단건 뷰어 **이 사진 정리** → 선택한 실제 파일을 시스템 확인 후 휴지통으로 이동. 복사본 보존을 전제로 하는 기존 **원본 정리**와 다른 기능이다.
- 장소 변경·해제는 앱 내부 연결만 변경. 이미 생성한 물리 앨범은 이동하거나 삭제하지 않는다.
- 위치기록에서 사용자가 기존 앨범 만들기를 명시 실행해야 실제 사진 복사/동영상 이동이 발생한다. 외부 지도/장소 검색 API는 후속 B-2 범위다.
- 선택은 기본 미선택. 단건 뷰어 진입은 해당 파일에 대한 명시적 동작이다. 목록은 ListView 재활용 구조다.

## P0 우선 검토

MainActivity 해시는 Phase A와 달라졌다. '0줄 변경'이라고 주장하지 않는다. 추가분은 라우팅/뷰 연결/생명주기 위임이며 새 계산·선택·실행 판단은 전용 클래스에 둔다.

- `MainActivity.java:369,374`: managedMemoryKey 라우팅 위임.
- `:381`: 미완료 수동 복사 receipt metadata 복구 Worker enqueue.
- `:8302,8970`: 관리 화면 진입 버튼 바인딩.
- `:8971,9004`: 빈 날짜 메모 표시와 문구 위임.
- `:1908,8611,8762,8779`: 기존 정책 호출에 현재 항목/준비 결과 전달. 수동 지정 동영상만 기존 사용자 이동 설정을 따르는 판단은 OrganizationMediaPolicy에 있음.
- MemoryManagementActivity는 lifecycle/result를 Controller에 위임한다.

Git HEAD diff에는 이전 출시 변경이 대량 섞여 있다. `REVIEW_SNAPSHOT_Memory_Management_PhaseBC_2026-11-08.json`과 Phase A snapshot의 SHA 비교 목록을 먼저 사용해 이번 변경 범위를 확인할 것.

## 저장·투영 계약

1. ManualPlaceStore SQLite v2: 장소/alias singleton + 행별 manual_assignments + manual_copy_receipts + 기존 raw inventory. SQLite FULL transaction 하나에서 장소/불변 ID/연결/revision 커밋. 조회는 JSON registry와 merge-on-read. 거대한 assignment JSON 재직렬화 없음.
2. 기존 inventory의 날짜는 파일 size/modified가 같은 경우 보존한다. 장소 재지정은 기존 논리 날짜를 보존한다. 복사본은 source의 논리 날짜를 승계한다. **0(알 수 없음)을 오늘 날짜로 대체하지 않는다.**
3. EffectiveMediaPlaceResolver는 수동 연결을 적용하고, 실제 존재·같은 장소·명시적 receipt가 있는 복사본만 원본 대신 표시한다. Cleanup 검사에서는 원본·복사본을 모두 포함해 세트 투영을 검사한다.
4. ManualGalleryPolicy는 물리 앨범에 있더라도 다른 수동 기록으로 연결된 파일을 옛 기록에서 제외한다. 수동 미복사 항목은 파일명 일치만으로 억제하지 않는다. Gallery fallback 날짜 대신 저장된 수동 논리 날짜를 적용한다.
5. stableMemoryId/dateKey는 변경하지 않는다. 날짜 메모가 있으면 사진 0장이어도 기록 카드와 날짜 섹션을 유지한다.
6. 위치 없는 raw 소스는 기존 IncrementalMediaScanner에서 관찰한다. 별도 전체 기기 재검색을 추가하지 않았다. 검색한 경로·캡처 성공/중단 상태를 표시하며 전체 기기 완전성으로 표현하지 않는다. 실제 live 접근 가능 항목, 미지정 항목, 알려진 outputPath 제외 조건을 적용한다.
7. ManualAlbumTargetPolicy는 동명 타인 폴더를 이름만으로 채택하지 않는다. 알려진 연결 경로를 우선하고 새 수동 기록의 충돌은 별도 경로로 분리한다.
8. copy/move receipt는 복사 전/target URI 생성 직후/복사 완료를 영속화한다. 원장 실패로 성공 복사를 재시도하지 않는다. 완성된 DB outputPath alias가 JSON link 반영 전에도 권위다. ManualReceiptWorker는 metadata만 복구하며 새 파일 복사/삭제를 실행하지 않는다.

## 일반 휴지통 계약

- API30+ MediaStore.createTrashRequest만 실행. API29 UI와 실행 양쪽에서 비활성화, 영구 삭제 fallback 없음.
- 실제 선택 파일의 volume + MediaStore epoch + kind + ID를 고정하고 실행 직전 size/modified/date/name/path까지 재검증한다. 물리 위치를 사용자 확인창에 표시한다.
- no_backup immutable plan + SHA-256 + fsync된 작은 progress journal. 1회 최대1000개, 배치 크기를 plan에 저장한다. 다음 묶음은 OS 승인이 필요하다.
- 승인/취소 결과코드만 믿지 않는다. ordinary MemoryTrashAuditWorker가 실제 is_trashed 상태를 50개씩 체크포인트한다. FGS 시작에 의존하지 않는다.
- 전부 미적용이면 offset을 진행시키지 않고 재시도/중단. 일부 적용이면 이동/미이동을 각각 집계. 상태가 불명확하면 NEEDS_ATTENTION으로 다음 배치를 막는다.
- 진입 시 활성 journal을 먼저 복구한다. 정확한 WorkId LiveData 관찰, pause시 observer 해제. 홈/강제종료 후 처음부터 새 검사로 진입하지 않는다.
- MediaMutationCoordinator: 준비 구간 FileLock+영속 예약, 이후 cleanup/trash journal, sort input/progress/WorkManager 상태를 교차 확인한다. 장소 변경·앨범 만들기·두 휴지통 작업의 충돌을 차단한다.

## 실제 발견한 회귀와 수정

통합 테스트에서 SHA-256 MATCH인 복사본도 기존 원본 정리 검사에 DATE_SECTION_CHANGED로 보류되는 결함을 발견했다. DATE_TAKEN이 없는 PNG에 Gallery reader가 복사본의 새 파일 수정시각을 fallback으로 사용하면서 원본의 unknown 날짜 섹션이 사라졌다.

안전 검사를 느슨하게 하지 않고 source 논리 날짜를 receipt/assignment로 승계하고 수동 Gallery 투영에도 적용했다. 수정 후 known 촬영일(고정 EXIF JPEG)과 unknown PNG가 각각 같은 날짜 섹션에 유지되며, whole-set MemoryDependencyGuard가 PRESERVED를 반환한다. 원본 촬영일·수정시각 불변, 전체 파일 SHA-256 동일, 복사본 DATE_TAKEN 보존을 API33에서 검사했다. EXIF 수정은 테스트 fixture 생성에만 있으며 production 신규 코드에는 없다.

## 검증 증빙

실행 로그는 `.debug/manual-place-2026-11-08/`에 있다. 프로브 소스는 `verification/memory-management-probe/`, 패키지 **com.photoplace.memoryprobe**이며 실제 앱/폰 데이터와 분리했다.

- `phaseBC-final.log`: Debug/Release 단위 테스트 각470개, 실패/오류/스킵0. assembleDebug/assembleRelease 통과. 최종 XML 합계도 확인.
- `probe-dates-flow.log`: 새 기록, 기존 기록 재지정, 실제 사진 복사와 동명 폴더 격리, 사진 byte SHA-256, known/unknown 날짜·메모 보존, 기존 cleanup dependency guard, 실제 MP4 이동, note-only, 연결 해제, 영속 충돌 차단, 취소/감사 통합 경로.
- `probe-restart.log`: force-stop 뒤 새 프로세스에서 수동 기록·노트·불변 ID·terminal journal 재조회.
- `probe-boundary.log`: 실제1001개 fixture, 1000+1 분할, 중간 정지, 최종 수량1001, 일부 적용 moved1/skipped1, unknown 상태 중단.
- `ui-selection.xml`, `ui-trash-confirm.xml`, `ui-os-trash.xml`, `ui-cancel.xml`, `ui-resume.xml`, `ui-done.xml/png`: 실제 화면 기본 미선택 → OS Deny → 강제종료/재진입 동일 작업 → OS Allow 직후 Home → 완료5개 확인.
- `ui-place-selection.xml`, `ui-picker.xml`, `ui-name-filled.xml`, `ui-place-confirm.xml`, `ui-place-success.xml`: 실제 관리 화면 → 새 기록 이름 → GPS/파일 위치 불변 확인 → 1개 연결 성공.
- git diff --check 오류0 (기존 LF/CRLF 안내만 있음).

1001장 테스트의 실제 OS 대화상자는 자동 승인하지 않았다. 배치 Worker 테스트는 **helper가 만든 자체 fixture만** 테스트 코드에서 is_trashed를 설정하여 사후 상태를 재현했다. 실제 OS Allow/Deny는 별도5개 UI 테스트로 확인했다. 이를 '1001개 OS 시스템창 승인 실험'이라고 표현하지 말 것.

## 안티에게 요청하는 반증 중심 검토

1. P0 신규 MainActivity 줄에 숨은 판단이 없는지 먼저 검사.
2. manual assignment/receipt/JSON link 사이 크래시, logical date 승계, alias 충돌, MediaStore epoch 변경, SD/internal ID 중복을 반증할 것.
3. whole-selection cleanup projection이 unknown 날짜/수동 동영상/기존 collection·note/cover 보호를 우회하지 않는지 검사.
4. REQUESTED 상태의 OS 창 취소·Home·재생성·중복 result, Worker 실패/재시도와 journal 체크포인트가 잘못 누적되거나 다음 배치를 열지 않는지 검사.
5. 의도적 일반 휴지통이 '위치앨범은 보존'이라고 오인하게 만들지 않는지 UX 검토. 여기서는 선택한 파일 자체가 대상이다.
6. API29, Android14+ 제한 권한/Android16, 실제 제조사 기기, 장기 백그라운드/재부팅은 아직 런타임 검증하지 않았다. 실기기 검증 전 프로덕션 승인 금지.

추가 관찰: SortInputStore.write는 기존 호출부에서 메인 스레드일 수 있어 coordinator의 파일 동기화/WorkManager 상태 조회 지연을 검토해야 한다. 미해결 receipt가 불명확한 경우 자동 삭제·재복사를 하지 않고 보류하는 정책도 사용자 재시도 안내가 충분한지 확인할 것. zero-media/zero-note 수동 빈 기록 정리는 후속 UX 후보다.

결과 파일 권장: `REVIEW_Memory_Management_PhaseBC_Antigravity_2026-11-08.md`. 코드 수정 시 snapshot 일치 여부와 새 검증 결과를 명시할 것. 독립 판정 전 승인으로 기록하지 말 것.
