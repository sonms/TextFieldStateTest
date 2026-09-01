# TextFieldState 다중 필드 폼: 상태 소유 위치 실측 비교

Jetpack Compose의 `TextFieldState` API로 다중 필드 폼을 만들 때, **상태를 어디에 두어야
하는가**를 실측으로 비교한 기술 검증 저장소이다.

- **패턴 A** — ViewModel이 `TextFieldState`를 직접 프로퍼티로 보유
- **패턴 A′** — 패턴 A의 `TextFieldState`들을 `@Stable` plain 클래스 컨테이너로 묶은 변형
- **패턴 B** — 화면 레벨 상태 홀더가 `rememberTextFieldState()`로 보유, ViewModel은 `snapshotFlow`로 값 스냅샷만 관찰

각 패턴을 `SavedStateHandle` 연동 여부까지 나눠 총 5가지 구현을 같은 화면(필드 5개: 제목 /
설명 / 가격 / 시작일 / 종료일, 교차 필드 검증 포함)에 올리고, 세 가지 축으로 측정했다.

## 측정 축과 결과

| 축 | 측정 방법 | A / A′ | A+Saved / A′+Saved | B |
|---|---|---|---|---|
| **1. 교차 검증 로직의 테스트 용이성** | 동일한 검증 테스트의 협력자 수·Arrange 줄 수·테스트 러너 | 협력자 2, Arrange 3줄, 순수 JVM | 동일 | **협력자 1, Arrange 1줄, 순수 JVM** |
| **2. 프로세스 강제 종료 후 복원** | `adb shell am kill`로 프로세스 소멸 → `savedInstanceState`가 있는 재실행 (실기기) | **소실** | **복원** (1회 고정 약 8줄 + 필드당 1줄) | **복원** (저장/복원 코드 0줄) |
| **3. 화면 config change** | `uimode night` / `font_scale` 로 Activity 재생성 트리거, `identityHashCode` 로그로 인스턴스 동일성 확인 (실기기) | 유지 (ViewModel 생존) | 유지 | 유지 (`rememberSaveable` 복원) |

### 핵심 결론

- **패턴 A와 패턴 B의 차이는 "능력"이 아니라 "기본값"이다.** 패턴 B는 값 스냅샷 구조라
  순수 함수 검증과 프로세스 복원이 기본으로 강제된다. 패턴 A도 "검증은 순수 함수로 분리"
  규율과 `SavedStateHandle` 보일러플레이트(1회성)를 더하면 동급이 된다.
- **config change 하나만으로는 패턴 선택의 근거가 되지 못한다.** 다섯 패턴 모두 값을
  유지하되, 유지되는 경로(ViewModel 생존 vs `rememberSaveable` 복원)가 다르다. 이 차이가
  프로세스 종료에서 갈린다.
- **패턴 A′는 세 축 모두에서 패턴 A와 실측치가 동일하다.** 가독성만 개선한 변형이며,
  `SavedStateHandle` 배선을 컨테이너로 옮겨도 코드 총량은 줄지 않는다(위치만 이동).
- `androidx.compose.foundation.text.input.TextFieldState`는 순수 Kotlin 타입이라, 다섯 패턴
  모두 Robolectric이나 `ComposeTestRule` 없이 순수 JVM 단위 테스트로 끝난다.

전체 판단 변수는 **교차 필드 검증의 유무**와 **프로세스 종료 복원 요구의 유무** 두 가지이며,
필드 개수는 기준이 아니다.

## 문서

| 문서 | 내용 |
|---|---|
| [`.docs/07-textfield-pattern.md`](.docs/07-textfield-pattern.md) | 패턴 A vs B 원본 실험. 실측 표와 예상 밖의 동작 기록 |
| [`.docs/08-textfield-container-pattern.md`](.docs/08-textfield-container-pattern.md) | 패턴 A′ 실험. 세 축 모두 재실측 |
| [`.docs/09-textfield-pattern-final-guide.md`](.docs/09-textfield-pattern-final-guide.md) | 07 + 08 통합. **상황별 권장 패턴 표**와 선택 순서. 실측 항목과 미실측 추론을 표에서 분리 |

## 소스 구조

```
app/src/main/java/com/sonms/textfieldstatetest/form/
├── DateLogic.kt                  # endBeforeStart(): ISO 문자열 사전순 비교로 날짜 판정
├── FormScreens.kt                # 다섯 패턴을 한 화면에 올린 AllPatternsScreen
├── patterna/
│   ├── FormViewModelA.kt
│   └── FormViewModelASaved.kt
├── patterna2/
│   ├── CourseFormFields.kt       # @Stable plain 컨테이너 (data class 아님) + SavedCourseFormFields
│   ├── FormViewModelA2.kt
│   └── FormViewModelA2Saved.kt
└── patternb/
    ├── FormStateHolderB.kt       # 상태 홀더 + FormValues 값 객체
    └── FormViewModelB.kt

app/src/test/java/com/sonms/textfieldstatetest/form/
└── CrossFieldValidationTest.kt   # 교차 검증 테스트 7개 (JVM)
```

## 빌드와 실행

```bash
# 단위 테스트 (실험 1)
./gradlew :app:testDebugUnitTest

# 실기기에 설치 (실험 2·3용)
./gradlew :app:installDebug
```

- Kotlin 2.2, AGP 9.2, Compose BOM 2026.02, `minSdk 24` / `targetSdk 36`
- 실험 2·3은 실기기에서 `adb` 명령으로 진행했다. 절차는 각 문서에 기록되어 있다.

## 환경

측정 장비는 RK3566 보드(Android 11, API 30)와 SM-G950N(Android 9, API 28)이다. 두 기기 모두
디스플레이 방향이 고정되어 회전으로 config change를 만들 수 없었고, `uimode` / `font_scale`
같은 다른 config 축으로 재생성을 트리거했다.
