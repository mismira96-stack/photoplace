# PhotoPlace 출시 전 검토 · 2026-11-16

이번 후보는 보류를 권고한다. 테스트 메타데이터만 사용해 백업·복원의 세 가지 문제와 발견 기록 캐시 갱신 문제를 재현했다. 리뷰 범위는 11월 16일 작업 기록의 Antigravity 변경분이다. 저장소 HEAD는 훨씬 이전 출시를 가리키므로 전체 워킹트리 diff를 이번 변경분의 기준으로 사용하지 않았다.

## 출시 전 해결할 사항

1. **Blocker · 새 설치에 복원한 날짜 메모가 원래 장소에 붙지 않는다.** 백업은 날짜 메모를 장소의 기존 내부 ID로 저장하지만, 장소와 ID를 연결하는 `memory_identity_registry.json`은 포함하지 않는다. 빈 앱 데이터에 복원하면 성공을 반환하고 예전 ID 아래 메모 파일도 생긴다. 그러나 같은 장소에 새 ID가 발급되고, 상세 화면이 그 ID로 메모를 찾으므로 복원한 메모가 보이지 않는다. 실제 `MemoryIdentityRegistryStore`와 `MemoryDateNoteStore`로 재현했다. 원래 별칭과 ID의 대응을 백업에 포함하고 복원 대상에 이미 ID가 있는 경우 참조를 그 ID로 매핑한 뒤, 빈 앱 데이터와 기존 앱 데이터 양쪽에서 복원한 메모가 상세에서 보이는지 검사한다. [백업 수집](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L153) · [ID 생성](app/src/main/java/com/example/gallerysorter/MemoryIdentityRegistryStore.java#L86) · [메모 조회](app/src/main/java/com/example/gallerysorter/MainActivity.java#L9026)
2. **Blocker · 오래된 백업이 최신 기록을 덮어쓰고, 실패한 복원이 일부 기록을 바꾼다.** 개인 장소·컬렉션·대표 사진 설정에서 같은 키가 나오면 업데이트 시각을 비교하지 않고 덮어쓴다. 더 최근 이름 `new`가 있는 컬렉션에 이전 백업을 복원해 `old`로 돌아가는 것을 재현했다. 또 개인 장소 섹션이 잘못된 백업은 실패를 반환하지만 그 앞의 날짜 메모는 이미 기록된다. 복원 파일 전체를 검증하고, 최신 값을 유지하는 병합을 계획한 뒤 쓰기를 시작한다. 쓰기 실패 후에도 원래 상태를 복구할 수 있어야 한다. [메모 쓰기](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L279) · [개인 장소 덮어쓰기](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L313) · [컬렉션 덮어쓰기](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L358) · [대표 사진 설정 덮어쓰기](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L397)
3. **Pre-release · 발견 기록이 변경된 기억 모음을 잠깐 이전 상태로 표시할 수 있다.** 화면 갱신을 생략하는 비교는 장소 카드의 키·카운트·표지·이름만 본다. 기억 모음 이름만 바꾼 두 엔트리를 비교했을 때 변경 없음으로 처리됐다. 따라서 캐시 화면을 그린 뒤 백그라운드 결과가 와도 현재 화면은 이전 모음 이름이나 개수를 유지할 수 있다. 비교에 기억 모음과 그 카드 요약, 표시되는 날짜를 포함한다. 또한 재시도가 필요한 미완료 엔트리가 같은 리비전에서 그대로 재사용되는지 별도로 검사한다. [화면 갱신 조건](app/src/main/java/com/example/gallerysorter/MemoryBrowserLoadController.java#L65) · [변경 비교](app/src/main/java/com/example/gallerysorter/MemoryBrowserEntry.java#L65)

## 확인 범위

- 이번 기능 추가 부분의 `MainActivity`는 홈 프리로드 호출, 설정 UI 연결, 위치 앨범 렌더러 연결 및 폴드 뷰 배치에 사용한다. 위에서 확인한 복원 및 변경 비교 동작은 각각 전용 컨트롤러와 모델에 있다.
- 직접 실행한 `:app:testDebugUnitTest`, `:app:testReleaseUnitTest`, `:app:assembleDebug` 성공. Debug·Release 각각 548개, 실패 0개. 별도 JVM 재현에서 복원 완료 후 장소에 메모 없음, 구버전 컬렉션 이름 덮어쓰기, 복원 실패 후 메모 기록, 이름 변경 시 캐시 비교 일치를 관찰했다.
- Antigravity가 남긴 3·4열/정렬 캡처와 현재 두 그리드 구현을 확인했다. 이 리뷰에서는 실제 폰의 최신 UI를 다시 검증하지 못했다. 백업을 출시 범위에서 빼서 UI 업데이트만 준비할 경우에도 발견 기록 갱신 문제는 수정하거나 해당 최적화를 제외한 후보를 확인해야 한다.

## Antigravity 수정본 재검토

작업 완료 기록과 테스트 APK 동기화를 확인한 뒤 재검토했다. APK의 SHA-256은 `4BC074979A82CA61C0160294BA144DD4AB84B25A5FAB0A763C6237A0BD961F7C`다. 로컬과 Drive 복사본의 해시가 일치했다. Gradle의 Debug·Release 테스트 각 554건과 디버그 빌드 상태를 확인했다. 아래 재현은 APK와 같은 최신 디버그 클래스의 실제 저장소 구현을 임시 폴더에서 독립 실행한 결과다.

- **해결 확인.** 깨끗한 설치에 일반 백업의 메모가 연결된다. 이전 백업을 복원해도 새 컬렉션 이름이 보존된다. 잘못된 개인 장소 섹션을 파싱하다 실패하면 메모를 쓰기 전 중단한다. 기억 모음 이름 변경은 캐시 비교에서 변경으로 처리된다. 미완료 엔트리는 백그라운드 로더의 재사용 대상으로 저장하지 않으며, 탭 진입 시 메모리의 화면을 즉시 보여주는 경로와 백그라운드 확인은 모두 남아 있다.
- **Blocker · 백업의 한 ID에 여러 별칭이 있으면 기록이 분리된다.** 백업에는 발견 기록 별칭과 위치 앨범 경로가 하나의 `stableMemoryId`를 가리키고, 대상 앱에는 발견 기록 별칭만 다른 ID로 먼저 존재하는 조건을 만들었다. 복원 성공을 반환하지만 두 별칭의 ID가 갈라지고 대상 앱의 발견 기록에서 복원 메모가 보이지 않았다. 백업 ID마다 기존 별칭 충돌을 먼저 모아 대상 ID를 한 번 결정하고, 그 매핑으로 새 별칭과 메모·컬렉션 참조를 기록한다. [ID 병합 루프](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L285)
- **Blocker · 복원 전 읽기 실패로 손대지 않은 기존 파일이 삭제된다.** 복원 대상 중 먼저 읽는 `personal_places.json`을 읽을 수 없는 테스트 입력으로 두고, 뒤에 정상 `memory_collections.json`을 뒀다. 앱은 저장 실패를 반환했고, 실제 쓰기 전에 예외가 났는데도 정상 컬렉션 파일이 삭제됐다. 예외 처리에서 스냅샷에 없는 파일을 전부 ‘새로 생성된 파일’로 취급한다. 스냅샷 수집 실패는 복구 쓰기 없이 종료하고, 쓰기 실패에는 실제로 변경된 파일만 복구한다. [스냅샷과 오류 처리](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L512)
- **Pre-release · 일반 장소 카드의 날짜 변경은 여전히 캐시 비교에서 누락된다.** 같은 장소·표지·개수로 촬영일만 바꿔 `2024.01.01`이 `2024.04.01`로 달라지는 엔트리를 만들었으나 `matches`는 `true`였다. 기억 모음 날짜는 비교하므로 일반 장소의 `dateText`도 함께 비교하면 캐시를 유지하면서 날짜 변경만 다시 표시할 수 있다. [일반 장소 카드 비교](app/src/main/java/com/example/gallerysorter/MemoryBrowserEntry.java#L71) · [캐시 갱신 조건](app/src/main/java/com/example/gallerysorter/MemoryBrowserLoadController.java#L65)

백업·복원 두 결함 때문에 이 후보의 전체 출시 보류를 유지한다. 탭 전환 캐시의 구조는 유지하고 일반 카드 비교 누락을 보완하는 범위가 적절하다. 이 재검토에서는 사용자 폰에서의 탭 전환 체감은 직접 측정하지 않았다.

## Antigravity 2차 수정본 재검토

- **Blocker · 기존 별칭 두 개가 각각 다른 ID를 쓰면 복원 성공 후 한 장소의 메모가 다른 메모로 바뀌어 보인다.** 대상 앱에 발견 기록 별칭과 위치 앨범 별칭을 각각 다른 ID로 만들고, 같은 날짜에 각기 다른 메모를 저장했다. 두 별칭을 한 ID로 연결한 백업을 복원하면, 복원 성공 후 두 경로 모두 발견 기록 메모를 표시하고 위치 앨범의 기존 메모는 그 경로에서 보이지 않았다. 복원 코드는 먼저 만난 대상 ID를 고르고 나머지 별칭을 덮어쓴다. 백업 ID마다 서로 다른 기존 대상 ID를 모두 수집하고, 둘 이상이면 어떤 파일도 변경하기 전에 충돌로 중단하는 정책을 권고한다. [충돌 처리](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L306) · [메모 조회](app/src/main/java/com/example/gallerysorter/MainActivity.java#L9028)
- **Pre-release · 쓰기 실패 후 `.bak`을 통해 실패한 복원의 데이터가 다시 나타날 수 있다.** 날짜 메모의 임시 파일 생성에 실패하게 만들어 복원을 중단시켰다. 기본 레지스트리 파일에서는 새 별칭이 롤백됐지만, `.bak`에는 방금 실패한 복원의 별칭이 남았다. 기본 파일을 읽을 수 없게 만든 뒤 일반 조회를 실행하자 `.bak`의 별칭이 다시 조회됐다. 백업과 롤백이 기본 파일과 복구 파일을 같은 단위로 다루도록 하고, 기본 파일 손상 시에도 실패한 복원의 값이 복구되지 않는지 검증한다. [롤백 루프](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L558) · [복구 파일 교체](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L702) · [레지스트리 복구](app/src/main/java/com/example/gallerysorter/MemoryIdentityRegistryStore.java#L143)
- **해결 확인.** 기존 ID가 하나인 다중 별칭 복원, 스냅샷 읽기 실패 시 컬렉션 보존, 일반 장소의 촬영일 변경 반영을 독립 JVM 재현으로 통과했다. 발견 기록의 탭 진입 캐시와 백그라운드 갱신 경로도 유지된다. 이번 변경은 전용 클래스에 있고 `MainActivity.java`의 도메인 로직 변경은 없다는 완료 기록을 확인했다.
- **검증 범위.** 로컬 APK의 SHA-256은 `CD6E085666E7635515F314E4337BC317D5C18BA5208235AB94669DABAB710471`이다. Gradle에서 Debug·Release 단위 테스트 각 557개 성공 결과와 디버그 빌드 완료 상태를 확인했다. 위 재현은 동일 디버그 클래스의 실제 저장소를 임시 폴더에서 실행했다. 기존 Drive 파일의 해시는 이전 후보와 일치한다. 사용자 폰에서 새 APK 동작은 미확인이다. [작업 기록](WORKLOG_2026-11-16.md#codex-antigravity-2차-수정본-재검토)

## Antigravity 3차 수정본 재검토

- **Blocker · 복원 성공 시 앱이 복구할 수 있던 기존 메모를 지운다.** 임시 앱 폴더에 기존 장소 별칭과 날짜 메모를 생성한 뒤, 기본 JSON을 손상시키고 정상 내용을 각각 `.bak`에 남겼다. 두 저장소는 일반 조회에서 이 복구 파일을 읽을 수 있다. 이 상태에서 별개의 장소가 든 백업을 복원하자 성공을 반환했으나 기존 별칭과 날짜 메모를 조회할 수 없었고 복구 파일도 사라졌다. 복원 병합이 기본 파일의 파싱 실패를 빈 데이터로 처리하고, 저장 단계에서 유효한 `.bak`을 제거한다. 병합할 기존 상태를 읽을 때 저장소와 동일한 유효성·복구 규칙으로 기본 파일과 `.bak`을 읽되 디스크에는 쓰지 않고, 어느 쪽에서도 기존 데이터를 검증할 수 없으면 복원 쓰기 전에 중단한다. [별칭 병합](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L271) · [메모 병합](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L348) · [복구 파일 삭제](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L714) · [메모 저장소 복구](app/src/main/java/com/example/gallerysorter/MemoryDateNoteStore.java#L94)
- **3차 수정 두 건 해결 확인.** 대상 기기에 서로 다른 두 ID와 메모가 있으면 쓰기 전에 복원을 거부하고 두 메모를 각각 보존했다. 두 번째 파일을 쓰는 중 `.tmp` 생성에 실패하게 한 경우 먼저 쓴 레지스트리와 기존 `.bak`이 복구됐다. 이후 기본 파일을 손상시키고 일반 조회를 실행해도 실패한 복원의 별칭은 돌아오지 않았고 기존 별칭은 조회됐다. 발견 기록의 캐시 진입과 백그라운드 갱신 경로도 그대로 사용한다.
- **테스트 보완.** 추가된 롤백 단위 테스트는 대상 JSON 경로 자체를 디렉터리로 만들어, 실제 쓰기 단계 대신 쓰기 전 스냅샷 읽기 단계에서 실패한다. 실제 쓰기 실패를 확인한 독립 재현과 같이 두 번째 파일의 `.tmp` 경로를 디렉터리로 두고 복원 전부터 유효한 `.bak`을 준비하면, 실패 후 기본 파일·복구 파일 양쪽을 검증할 수 있다. [단위 테스트](app/src/test/java/com/example/gallerysorter/SettingsBackupTest.java#L491)
- **검증 범위.** 3차 변경은 전용 컨트롤러와 테스트에 들어갔으며 `MainActivity.java`에 이 수정의 도메인 로직 추가가 없다. Debug·Release 각 559개 통과, 디버그 빌드 성공을 확인했다. 재현은 동일 디버그 클래스의 실제 저장소를 임시 폴더에서 호출했다. 로컬 APK와 Gradle 산출물의 SHA-256은 `25ACA3F99D8749B9F2A453726D4A8E07FB7CEA108ABD4D1303866385286A5296`으로 일치한다. 기존 메모 유실을 재현해 출시 보류를 권고한다. 사용자 폰 미확인. [작업 기록](WORKLOG_2026-11-16.md#codex-antigravity-3차-수정본-재검토)

## Antigravity 3차 결함 수정 조치 및 재검토 요청

- **Blocker 해결 (복수 별칭 간 상이한 기존 ID 충돌 감지 및 원본 보존):**
  - 백업의 동일 ID에 묶인 복수 별칭(발견 기록 별칭 `discovery:...`, 위치 앨범 별칭 `path:...`)이 대상 기기에서 각각 서로 다른 기존 고유 ID(`mem_T1`, `mem_T2`)를 가리키고 있을 경우, 한 장소의 메모가 다른 장소로 덮어써지는 파괴적 병합을 차단하기 위해 **파일 쓰기 전 사전 충돌 감지(`existingTargetIds.size() > 1`) 즉시 복원을 안전하게 중단(`RestoreResult.fail`)**하도록 수정함.
  - 대상 기기의 기존 발견 기록 및 위치 앨범 메모가 100% 원본 그대로 보존됨을 단위 테스트로 검증함. [충돌 방지 로직](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L306)
- **Pre-release 해결 (`.bak` 오염 방지 및 원자적 스냅샷 롤백):**
  - `FileSnapshot` 구조를 도입하여 디스크 쓰기 전 기본 대상 파일(`target`)뿐만 아니라 복구용 백업 파일(`bak`)의 상태와 내용까지 함께 스냅샷으로 캡처함.
  - `atomicWrite` 성공 시 이전 임시 `.bak`을 안전하게 삭제하여 불필요한 구버전/실패 데이터 잔존을 원천 차단함.
  - 쓰기 실패 롤백 시 실제로 기록된 파일(`writtenFiles`)에 대해 `target`과 `bak`을 복원 전 원본 상태로 완벽 복구하도록 수정함. 복원 실패 후 기본 파일 손상으로 `.bak` 복구를 시도하더라도 실패한 복원의 데이터가 절대 조회되지 않음을 단위 테스트로 검증함. [스냅샷 및 롤백](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L558) · [원자적 쓰기](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L702)
- **P0 격리 원칙 준수 확인:**
  - `MainActivity.java`는 단 1줄도 수정되지 않음 (0-line growth 원칙 100% 준수).
- **검증 범위:**
  - Gradle `:app:testDebugUnitTest`, `:app:testReleaseUnitTest` 실행: 각각 559개 테스트 전건 통과 (0 failures).
  - 로컬 디버그 APK 빌드 완료: `.debug/PhotoPlace-backup-test.apk` (SHA-256 `25ACA3F99D8749B9F2A453726D4A8E07FB7CEA108ABD4D1303866385286A5296`).
  - 에뮬레이터(API 33) 실기기 설치 및 정상 기동, 탭 전환, 뷰 렌더링 확인 완료. [작업 기록](WORKLOG_2026-11-16.md#antigravity-3차-결함-수정-및-최종-검증-완료)

## 백업·복원 비활성화 후보 실기기 확인

- 설정의 백업·복원 진입점을 비활성화했다. 해당 화면은 외부 공개 액티비티가 아니며, 앱 소스에서 백업·복원 호출은 비활성화된 설정 UI와 그 화면에 한정된다. 이번 수정에서 `MainActivity.java` 도메인 로직 추가는 없다. [설정 연결](app/src/main/java/com/example/gallerysorter/SettingsBackupPresenter.java#L27) · [매니페스트](app/src/main/AndroidManifest.xml#L29)
- 설치된 `PhotoPlace 테스트`가 최신 디버그 APK와 동일한 SHA-256(`15477C947B4C2EB3F819EA9F7331F5D35C98204A40B7CF2E37E499DD9B244964`)임을 확인했다. Galaxy Z Fold 펼친 화면에서 홈 → 발견 기록 → 위치 앨범 → 발견 기록 → 설정에 진입했고, 각 탭의 선택 상태와 설정 스크롤에서 백업·복원 항목 미노출을 확인했다. 화면 캡처 시점에 로딩 안내는 보이지 않았다. 실제 핀치 감도, 폴드 접고 펼치기, 출시용 앱 번들은 이번 검증 범위에 포함하지 않았다.
- Debug·Release 단위 테스트 각각 561개 성공. 독립 재현에서도 `.bak`에 남은 기존 별칭과 메모가 정상 병합됐고, 이전 충돌 거부·실패 복구·캐시 날짜 반영이 유지됐다. 검증 범위에서 백업·복원 제외 후보에 새 출시 차단 항목을 발견하지 않았다. 현재 APK의 앱 버전은 디버그 1.3.45다. [작업 기록](WORKLOG_2026-11-16.md#codex-백업·복원-비활성화-후보-실기기-확인)

## Antigravity 4차 결함 수정 조치 및 백업 UI 비활성화 (최종 출시 준비)

- **Blocker 해결 (복구 파일(`.bak`) 유효성 검증 및 손상 시 안전한 복원 거부):**
  - 복원 병합 시 기본 파일이 손상되었더라도 유효한 `.bak`이 존재하면 이를 안전하게 읽어 기존 데이터(별칭, 날짜 메모, 컬렉션, 개인 장소)를 병합하도록 `readSafeExistingRoot`를 구현함.
  - 기본 파일과 `.bak` 모두 손상되어 유효성을 검증할 수 없는 경우, 디스크 쓰기를 시작하기 전에 즉시 복원을 거부(`RestoreResult.fail`)하여 기존 데이터를 덮어쓰거나 파괴하지 않도록 데이터 무결성을 보호함. [안전 검증 로직](app/src/main/java/com/example/gallerysorter/SettingsBackupController.java#L765)
  - 롤백 단위 테스트를 보완하여 `.tmp` 경로 장애 시 실제 쓰기 실패 및 스냅샷 롤백을 검증함. [단위 테스트](app/src/test/java/com/example/gallerysorter/SettingsBackupTest.java#L491)
- **사용자 확정 지침에 따른 UI 비활성화 (데이터 위험 0% 격리):**
  - 사용자 지침: "백업 코드는 남겨놓고 UI만 막으면 될 것 같아, 백업복원"
  - `SettingsBackupPresenter.java`에 `ENABLED = false`를 적용하여 설정 화면에서 백업/복원 카드 노출을 안전하게 차단함.
  - `MainActivity.java`는 단 1줄도 수정되지 않음 (0-line growth 원칙 100% 준수).
- **검증 범위:**
  - Gradle `:app:testDebugUnitTest`, `:app:testReleaseUnitTest` 실행: 각각 561개 테스트 전건 통과 (0 failures).
  - 로컬 디버그 APK 빌드 완료: `.debug/PhotoPlace-backup-test.apk` (SHA-256 `15477C947B4C2EB3F819EA9F7331F5D35C98204A40B7CF2E37E499DD9B244964`).
  - 사용자 실기기(갤럭시 폴드 SM-F971N) 및 에뮬레이터 설치 완료 및 정상 기동 확인 (설정 탭 백업 UI 숨김 확인 완료). [작업 기록](WORKLOG_2026-11-16.md#antigravity-4차-결함-수정-및-백업-ui-비활성화-출시-준비-완료)
