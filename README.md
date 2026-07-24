# PhotoPlace / 앨범정리

사진과 동영상을 위치별 앨범으로 정리하고, 정리된 장소를 앱 안에서 다시 볼 수 있게 하는 Android 앱입니다.

- 패키지: `com.photoplace.app`
- 현재 방향: V1의 위치별 앨범 정리에서 V2의 앱 내부 Memory View / Filtered View로 확장
- 사진: 위치별 앨범으로 복사
- 동영상: 위치별 앨범으로 이동
- 갤러리 앱은 fallback이며, V2 탐색은 앱 내부 뷰 중심

## 동작 방식

1. `앨범 정리 시작`을 눌러 선택한 사진/동영상 폴더를 분석합니다.
2. 앱이 GPS/EXIF/동영상 메타데이터에서 위치와 날짜를 읽습니다.
3. 기존 위치 앨범이 있으면 그 앨범을 사용합니다.
4. 기존 앨범이 없으면 `{지역명}에서` 폴더를 만들고 정리합니다.
5. 위치 정보가 없는 항목은 정리하지 않고 `위치 없음`으로 표시합니다.
6. 정리 기록은 앱 내부 `album_summary_history.json`에 저장됩니다.
7. 홈과 정리 기록에서 장소/국가별 Memory View를 제공합니다.

## 생성되는 폴더

```text
Pictures/
  안동에서/
    IMG_0001.jpg
  청주에서/
    IMG_0003.jpg
```

날짜별 하위 폴더를 만들지 않습니다.
파일명도 바꾸지 않습니다.

## V2 Memory View

- 홈에 `해외 기록` 섹션을 표시합니다.
- 국가별 카드에는 대표 썸네일, 사진 수, 날짜 범위를 표시합니다.
- 카드를 누르면 해당 국가의 장소들을 앱 내부 filtered memory view로 보여줍니다.
- 기존 사용자는 앱 업데이트 후 자동 1회 백필 또는 설정의 `발견 장소 다시 만들기`로 기존 정리 앨범을 다시 읽어 해외 기록을 만들 수 있습니다.

## 위치 없음 캐시

위치 정보가 없던 항목은 앱 내부 캐시에 저장해 다음 스캔에서 불필요한 EXIF/비디오 메타데이터 재검사를 줄입니다.

캐시 signature:

```text
uri + 파일명 + date_modified + date_added + datetaken + 사진/동영상 여부
```

파일이 수정되거나 메타데이터가 바뀌면 signature가 달라져 다시 검사됩니다.

## 중복 기준

현재 버전은 대상 폴더에 같은 파일명이 있으면 건너뜁니다.

## 빌드

Android Studio에서 이 폴더를 열고 `Run`을 누르면 됩니다.

터미널에서는 아래 명령으로 디버그 APK를 만들 수 있습니다.

```powershell
.\gradlew.bat assembleDebug
```

빌드 결과:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release Notes

- 1.2.0 release candidate keeps `NoLocationCache` disabled even though the class exists in code.
- Re-enable no-location caching only after real-device cache invalidation tests.
