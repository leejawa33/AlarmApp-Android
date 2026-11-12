# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 따르는 지침입니다.

## 현재 상태

기준일: 2026-11-12

**프로젝트 골격(`AlarmApp-Android/`)을 만들었고 `assembleDebug`·단위 테스트가 통과합니다. 도메인·저장 계층, 메인·편집 화면(미션 문장 고르기 포함), 실행 화면·미션·미리보기까지 구현했습니다**알람 예약·울림 서비스까지 구현했습니다(단위 테스트 77개 통과, 실기기에서 잠금 화면 울림 확인). iOS 앱은 Phase 1 구현을 마쳤고(테스트 통과, 실기기에는 설치해 봤지만 확인 목록의 결과는 아직 정리 전), 이 프로젝트는 그 기능을 **Android 네이티브로 따로 재구현**합니다. iOS와 코드는 공유하지 않습니다.

이 폴더에는 인수 자료만 있습니다.
- `docs/iOS_알람앱_기획서.md`: 제품 기획(단일 진실 원천). 2026-11-12 시점의 복사본이라 이후 iOS 쪽 원본과 달라질 수 있습니다. iOS 기준으로 쓰였으니 AlarmKit·SwiftData 같은 iOS 용어는 아래 "플랫폼 대응표"로 바꿔 읽습니다.
- `docs/design-preview.html`: 디자인 시안(HTML 목업, CSS 토큰). `docs/알람실행화면.png`: 실행 화면 레퍼런스(잠금화면이 아니라 **앱 안** 화면).
- `shared-resources/`: iOS 앱에서 가져온 리소스(카탈로그, 한/영 문구, 따라쓰기 문장, 영상 3개, 소리 mp3 2개). Android 프로젝트에는 `app/src/main/assets/`에 **같은 폴더 구조로** 복사해서 씁니다.
- iOS 구현 참고용 저장소(읽기 전용): `/Users/leehyun/toy-project/AlarmApp-iOS-handoff` (GitHub `leejawa33/AlarmApp-iOS`). 도메인 로직은 그대로 옮길 수 있습니다: `AlarmApp-iOS/AlarmApp/Domain/`의 `AlarmSchedule`(다음 울림·남은 시간), `Weekday`(비트마스크), `ShakeCounter`, `TranscriptionPrompts`(+`PromptSelection`)와 `Services/AlarmService`. 구현 규칙은 그 저장소의 `CLAUDE.md`에 정리되어 있습니다. 세션을 `claude --add-dir /Users/leehyun/toy-project/AlarmApp-iOS-handoff`로 시작하면 바로 읽을 수 있습니다.

**다음 할 일 (순서대로):**
1. ~~개발 환경 준비~~ **완료**(2026-11-12): Android Studio 2026.2, SDK platform 37·build-tools 36.0.0, 실기기 삼성 Galaxy Z Flip4(SM-F721N, Android 16/API 36) USB 디버깅 연결됨. 알람·화면 잠금 동작은 에뮬레이터로 검증하기 어려우니 이 실기기를 씁니다. 빌드는 터미널에서 `JAVA_HOME`을 Android Studio 번들 JBR(`/Applications/Android Studio.app/Contents/jbr/Contents/Home`)로 두고 `./gradlew`를 씁니다.
2. ~~프로젝트 골격 만들기~~ **완료**(2026-11-12): 단일 모듈 `app`, AGP 9.5.1 + Gradle 9.6.0 + Kotlin 2.4.21 + KSP, Compose(BOM 2026.09.00), Hilt, Room, Navigation Compose, Media3. `minSdk 28`, `compileSdk 37`, `targetSdk 36`, 정확한 알람 권한은 `USE_EXACT_ALARM`. 버전은 `AlarmApp-Android/gradle/libs.versions.toml`. 아직 폰에 설치해 실행해 보지는 않았습니다.
3. ~~도메인·저장~~ **완료**(2026-11-12): `domain/`(Weekday·AlarmSchedule·ShakeCounter·TranscriptionPrompts·Alarm), `catalog/AssetCatalog`, Room(`data/`), `service/AlarmService`(변경의 단일 진입점) + `AlarmScheduler` 인터페이스. **`AlarmScheduler`는 자리표시자 `NoOpAlarmScheduler`가 바인딩되어 있어 실제로 예약되지 않습니다**(알람 예약 단계에서 `AlarmManager` 구현으로 교체, `di/AppModule.kt`). Room DB는 JVM 단위 테스트로는 검증하지 않았고(가짜 DAO 사용) 실기기에서 실제로 열어 본 적도 없습니다. 흔들기 임계값 14.7 m/s²는 실기기 조정 전입니다.
   **메인/편집 화면 구현됨**(2026-11-12): Compose + Hilt ViewModel(`ui/main`, `ui/edit`), 문구는 `shared-resources/Copy/ui_strings_*.json`을 `strings.xml`로 옮겼고(이후 `strings.xml`이 Android 기준) 두 언어 키·서식 일치 테스트가 있습니다. **아직 없는 것**: 설정 톱니바퀴·권한 경고 배너(설정·권한 단계). 타이포는 시스템 산세리프 굵게로 대체(둥근 폰트 번들은 미정). 편집 화면은 iOS처럼 시간 휠(화면 안 3열)·둥근 취소/휴지통 버튼으로 다듬었습니다(실기기 확인). 실행 화면 하단 시스템 내비게이션 영역 색은 아직 다듬지 않았습니다. 
   **실행 화면·미션·미리보기 구현됨**(2026-11-12, `ui/ring/`): 실행(음소거 반복 영상 + 날짜·`HH:mm`·타이틀·미션 시작) → 미션(흔들기 `TYPE_LINEAR_ACCELERATION`, 따라쓰기) → 완료(진동). 뒤로가기 차단·화면 켜짐 유지·소리는 `USAGE_ALARM`로 반복. 두 모드: 미리보기(라우트 `Preview`가 편집 중인 초안을 받고, `미리보기 종료`, 알람을 멈추지 않음)와 실제 울림(`RingActivity`가 `alarmId`로 열고 미션 완료 시 `completeMission`으로 정지, 소리는 서비스가 재생). 흔들기 센서가 없는 기기는 링을 눌러 셉니다. **실기기에서 영상·소리·흔들기 감도·한글 입력은 아직 확인 전입니다.**
   **알람 예약·울림 구현됨**(2026-11-12, `alarm/`): `AndroidAlarmScheduler`(`AlarmManager.setAlarmClock`, 다음 울림 한 번만 예약), `AlarmReceiver`(울림 서비스 시작 + 다음 울림 재예약), `RingingService`(포그라운드 서비스: `USAGE_ALARM` 반복 소리·진동·전체 화면 인텐트 알림), `RingActivity`(잠금 화면 위 실행 화면), `BootReceiver`(부팅·업데이트·시간 변경 후 `reconcile`), 앱 시작 때도 `reconcile`. 울리는 중 앱을 열면 실행 화면으로 보냅니다. 알림 권한은 저장·켜기 때 요청. **실기기(Galaxy Z Flip4, Android 16)에서 화면 꺼진 상태로 지정 시각에 울리고 잠금 화면 위에 실행 화면이 뜨는 것까지 확인.** 아직 확인 전: 흔들기 감도, 미션 완료 후 소리 정지, 재부팅 후 재예약, 강제 종료 후 울림, 무음·방해 금지, 배터리 최적화. 전체 화면 알림 권한이 꺼진 기기의 안내는 설정·권한 단계에서 다룹니다. 앱 아이콘은 iOS 아이콘으로 교체했습니다.
   다음: **설정·언어**(톱니바퀴, 권한 경고 배너, 언어 선택) → 실기기 확인 목록.
4. **실기기 검증** — 아래 "확인 목록".

## 사용자와 작업 방식

- 사용자는 15년차 백엔드 엔지니어입니다. Android 개념은 Spring 비유(Service, Repository, Entity, DTO 등)로 설명하면 잘 통합니다. **설명은 항상 한국어**로 합니다(진행 중 짧은 안내 문장까지).
- **진행 상태는 `CLAUDE.md`의 "현재 상태·다음 할 일"에만 적습니다.** 기획서(`docs/`)는 요구사항·규칙·화면만 담고, "구현 완료", "검증 전", 다음 단계 체크리스트 같은 작업 상태는 쓰지 않습니다(바뀔 때 어긋나기 쉬움).
- **새로 추가하는 모든 기능은 한국어와 영어를 함께 지원합니다.** 화면 문구, 버튼, 에러·권한 안내, 알림 문구, 접근성 라벨, 소리·미디어·미션 문장 이름까지 전부 대상입니다. 한쪽만 넣고 끝내지 않고, 두 언어의 키 일치를 검사하는 테스트를 둡니다.
- 커밋·푸시는 사용자가 요청할 때만 합니다. 되돌리기 어려운 작업(삭제, 강제 푸시 등)은 먼저 확인합니다.
- 코드 주석과 문서는 한국어로 씁니다.

## 확정된 사항

- 표시명 **알람천국**, 프로젝트 `AlarmApp-Android`, **applicationId `com.alarmheaven.alarm`**(iOS 번들 ID와 동일)
- Kotlin 네이티브. 크로스플랫폼 프레임워크(Flutter, RN, KMP 등)는 쓰지 않습니다.
- **라이트 테마 고정**(다크는 이후 단계). 휴대폰만(태블릿 비범위)
- 한국어/영어 지원. 시스템 언어가 한·영이 아니면 한국어. 설정에서 수동 선택 가능
- 기능 범위와 도메인 규칙은 기획서와 같습니다. 요약은 아래.

## 도메인 규칙 (기획서 요약)

- 강제 기상 유틸리티: **미션을 완료하기 전에는 앱 안에서 알람을 끌 수 없습니다.**
- 알람은 최대 10개(초과 시 추가 차단). 요일 1개 이상, 타이틀 필수. **반복 알람만** 지원(단발 없음).
- 알람 필드: 시각, 요일, 타이틀, 켜짐, 미션(shake | transcription), 미디어 id, 소리 id, 따라쓰기 문장 id 목록(**빈 배열 = 전체**).
- 새 알람 기본값: 미션 = 흔들기, 미디어·소리 = 카탈로그에서 `default: true`인 항목, 시각 07:00, 요일 월~금, 타이틀 빈칸.
- 미션은 알람당 정확히 1개. **흔들기 10회**(`defaultCount`), **따라쓰기**는 trim 후 **대소문자 구분 완전 일치**이며 일치하는 즉시 완료. 문장은 알람이 고른 것 중 무작위, **앱 언어의 문장**으로 출제·판정합니다.
- 미션 완료 시 **지금 울리는 알람만** 멈추고 반복 예약은 유지합니다. 알람을 찾을 수 없거나 문구 파일이 없어도 **미션 없이 꺼지지 않습니다**(기본 미디어·소리 + 흔들기로 대체).
- 리스트: 시각, 요일, 타이틀, on/off, "다음 울림까지 N시간 M분"(분 단위 올림, 해당 없으면 숨김). 삭제는 리스트 왼쪽으로 밀기, 카드 길게 누르기, 편집 화면 휴지통(확인창).
- 실행 화면: **전체 화면 배경 동영상**(음소거·반복) 위에 날짜·큰 시각(24시간제 `HH:mm`)·타이틀·미션 시작. 날짜·시각은 상단 1/3, 미션 시작은 하단. 뒤로가기로 끄는 경로가 없고, 울리는 동안 화면이 꺼지지 않습니다. 미션 완료 시 "미션 완료! 좋은 아침이에요"와 진동 후 닫힙니다.
- 미리보기는 **편집 화면에서만** 열 수 있고 `미리보기 종료` 버튼이 있습니다. 미션을 해 볼 수 있지만 알람을 멈추지 않습니다.
- 화면: 메인 / 편집·생성 / 미션 문장 고르기 / 설정(언어, 권한 안내) / 언어 선택 / 알람 실행 / 미션 / 미리보기.
- **비범위:** 다크 테마, 스누즈, 통계, 결제, 클라우드, 태블릿, 단발 알람.

## 플랫폼 대응표 (iOS → Android)

아래는 **출발점 제안**입니다. 실제 API 제약은 구현 전에 공식 문서로 확인하세요(Android는 버전마다 알람·전체 화면 알림 정책이 자주 바뀝니다).

| iOS | Android 후보 | 메모 |
|---|---|---|
| AlarmKit(시스템 알람) | `AlarmManager.setAlarmClock()` + `BroadcastReceiver` | Doze에서도 정확히 울리도록 알람 시계 API 사용. 알람 앱용 정확한 알람 권한(`USE_EXACT_ALARM` 등) 정책을 확인 |
| 시스템 울림 → 앱 열기 | 수신 → **포그라운드 서비스**로 소리 재생 + 전체 화면 인텐트 알림 → 실행 Activity | 잠금화면 위로 띄우려면 `showWhenLocked`/`turnScreenOn`과 `USE_FULL_SCREEN_INTENT`(Android 14+ 정책) 확인 |
| 앱이 소리를 직접 재생(시스템 소리가 멈춘 경우) | Android는 **앱이 항상 직접 재생** | 알람용 오디오 속성(`USAGE_ALARM`)으로 무음 모드에서도 울리게. `.caf`는 필요 없음 |
| SwiftData | **Room**(SQLite) | 저장이 원본, 예약은 파생. 저장 후 예약, 실패해도 데이터는 유지, 앱 시작 때 다시 맞추기(reconcile) |
| `AlarmService`(변경의 단일 진입점) | Repository/UseCase 한 곳 | 화면에서 DAO를 직접 수정하지 않습니다 |
| 재부팅 후 예약 유지(OS가 처리) | `RECEIVE_BOOT_COMPLETED` 수신 후 **재예약 필수** | Android는 재부팅하면 알람이 사라집니다 |
| Core Motion `userAcceleration` 1.5g | `SensorManager` `TYPE_LINEAR_ACCELERATION` (m/s², 1.5g ≈ 14.7) | 0.35초 안의 피크 무시, 시작 0.5초 무시(iOS `ShakeCounter` 규칙). **임계값은 실기기 조정 필요** |
| `LoopingVideoView`(음소거 반복 영상) | Media3 ExoPlayer(`repeatMode = ALL`, 볼륨 0) 또는 `VideoView` | 영상이 알람 소리(오디오 포커스)를 끊지 않게 |
| 앱 언어 설정 | `AppCompatDelegate.setApplicationLocales` / 앱별 언어 + `values`/`values-en` `strings.xml` | 문구 원본은 `shared-resources/Copy/ui_strings_{ko,en}.json`. Android에서는 `strings.xml`로 옮깁니다 |
| 알람 권한 창 | 알림(`POST_NOTIFICATIONS`, Android 13+), 정확한 알람, 전체 화면 알림 | 권한 요청은 사용자 동작(저장·켜기·`설정 열기`)에서만 합니다 |
| `OpenSettingsButton` | `Settings.ACTION_APP_NOTIFICATION_SETTINGS` 등 앱 설정 화면 인텐트 | iOS와 달리 알람 관련 설정 화면으로 더 직접 보낼 수 있음 |

## 리소스 규칙

- `shared-resources/assets_catalog.json`이 미디어·소리·미션 목록의 기준입니다. 코드에 하드코딩하지 말고 카탈로그를 읽어 쓰세요. `file` 경로(`Media/...`, `Sounds/...`)는 `assets/` 아래 같은 구조에 두고 `AssetManager`로 엽니다(iOS와 달리 평탄화하지 않으므로 경로 그대로 사용 가능).
- 카탈로그의 `alarmFile`(`.caf`)은 iOS 전용이라 Android에서는 무시합니다. 소리는 mp3만 씁니다. `아침이 왔다잖아`(33.7초)는 iOS 알림 소리의 30초 제한 때문에 iOS에서 실기기 확인 대상이지만, Android는 앱이 직접 반복 재생하므로 같은 제한이 없을 것으로 봅니다(구현 후 확인).
- **소리·미디어 표시명은 앱 언어를 따라갑니다.** 카탈로그 id(`media_...`, `sound_...`)를 키로 한/영 문구에서 찾고, 없으면 카탈로그 `displayName`(한국어)으로 대체합니다. 영어 이름: `Morning Has Come`, `Yar Song`, `Puppy Smash`, `Cat vs. Phone`, `Shaky Alarm`.
- 따라쓰기 문장은 `Copy/transcription_prompts.json`(`categories` 한/영 표시명 + `prompts[].category` + `prompts[].text`의 같은 id에 `ko`/`en` 문장)입니다. 문장을 추가할 땐 두 언어를 함께 넣고, 띄어쓰기가 헷갈리지 않는 짧은 문장으로 씁니다. 영어는 첫 글자만 대문자, 중간 대문자·아포스트로피 금지(키보드가 ’로 바꿔 불일치할 수 있음).
- 미디어 영상은 이미 렌더링되어 있습니다(1080×1920, 7초, 무음 반복). 생성 스크립트는 macOS 전용 Swift라 이 프로젝트에는 없습니다. 새 영상이 필요하면 iOS 저장소의 `AlarmApp-iOS/Tools/`를 참고하세요. 영상에는 날짜·시각(상단 1/3)과 미션 버튼(하단)을 가리는 연출을 넣지 않고, **화면 전체 번쩍임은 금지**(광과민성)입니다.

## 디자인 토큰

| 토큰 | 값 | 용도 |
|---|---|---|
| bg / bg-deep | `#FFF8F0` / `#FFE8D6` | 배경 |
| sun / sun-deep | `#FFC107` / `#FFB300` | 포인트, 토글 on |
| cta / cta-press | `#FF5A5F` / `#E8454A` | 미션 시작 등 CTA |
| text / text-muted | `#1A1A1A` / `#6B6560` | 텍스트(실행 화면 시각은 화이트 + 섀도) |

- 타이포는 둥근 산세리프 볼드입니다(iOS는 SF Rounded). Android 대체 폰트는 구현할 때 정합니다.
- 모션: CTA 스프링, 토글 점등, 미션 완료 짧은 피드백. 다크 기본 배경, 퍼플 네온, 무거운 세리프는 쓰지 않습니다. 카피는 밝고 짧게.

## 열린 결정 (사용자와 먼저 정할 것)

- (확정) `minSdk 28`/`targetSdk 36`, 단일 모듈 `app`, Hilt + Navigation Compose, `USE_EXACT_ALARM`.
- Play 스토어 배포 시 알람 앱의 정확한 알람·전체 화면 알림 권한 선언 정책 확인(열림).

## 확인 목록 (실기기에서 확인할 것)

① 알림 권한 창과 정확한 알람 허용 흐름 ② 지정 시각에 소리와 함께 정확히 울림(Doze, 절전 모드) ③ 잠금 상태에서 화면이 켜지고 실행 화면이 뜨는지 ④ 앱이 꺼진/백그라운드/포그라운드 상태 각각에서 울릴 때 ⑤ 알림에서 앱 열기, 알림 스와이프로 알람이 꺼지지 않는지 ⑥ 무음·방해 금지 모드에서 소리 ⑦ 흔들기 임계값(너무 쉽거나 어려운지) ⑧ 미션 완료 시 소리 정지와 다음 요일 예약 유지 ⑨ **재부팅 후 알람 재예약** ⑩ 앱 강제 종료 후에도 울리는지 ⑪ 영어 따라쓰기에서 키보드 자동 대문자, 한글 조합 중 판정이 자연스러운지 ⑫ 제조사별 배터리 최적화(삼성·샤오미 등)에서 알람이 죽지 않는지

## 리스크

- Android는 제조사·버전별로 알람, 백그라운드 서비스, 전체 화면 알림 정책이 다릅니다. 실기기(가능하면 2종 이상)로 확인해야 합니다.
- 사용자가 알림을 스와이프하거나 앱을 강제 종료하면 미션 강제가 약해집니다. 포그라운드 서비스 + 지속 알림으로 줄이되 100% 차단은 어렵다고 보고, 한계는 리스크로 수용합니다.
- 흔들기 기준값과 따라쓰기의 완전 일치 규칙(띄어쓰기 실수로 실패 가능)은 실기기에서 보정합니다.
