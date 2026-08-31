# 07. TextFieldState 다중 필드 폼: 패턴 A vs 패턴 B 실측 검증

## 검증 대상

같은 화면(필드 5개: 제목 / 설명 / 가격 / 시작일 / 종료일)을 두 가지 패턴으로 구현하고 세 가지 변수를 독립적으로 측정했다.

- **패턴 A**: ViewModel이 `TextFieldState`를 직접 프로퍼티로 보유한다. (`FormViewModelA`)
  - **A+Saved**: 여기에 `SavedStateHandle`을 직접 연동한 변형. (`FormViewModelASaved`)
- **패턴 B**: 화면 레벨 `@Stable` 상태 홀더가 `rememberTextFieldState()`로 값을 컴포저블 스코프에서 관리하고, ViewModel은 `snapshotFlow`로 값만 관찰한다. (`FormStateHolderB` + `FormViewModelB`)

측정 장비: RK3566 보드(Android 11, API 30, 1920×1024 고정 가로 화면). 단위 테스트는 로컬 JVM(JDK 21).

> 참고: 지시에 언급된 `01-unit-test-ease.md`는 현재 저장소에 존재하지 않아 파일을 직접 대조하지 못했다. 실험 1의 표는 지시에 기술된 "협력자 수 / Arrange 줄 수" 형식을 그대로 따랐다.

---

## 실험 1: 교차 필드 검증 로직의 테스트 용이성

검증 로직: "종료일이 시작일보다 빠르면 오류". ISO(`yyyy-MM-dd`) 문자열은 사전순 비교가 곧 날짜순 비교이므로 파싱 없이 판정한다(`endBeforeStart()`).

- 패턴 A: 검증을 `FormViewModelA.dateRangeError`가 두 `TextFieldState`의 텍스트를 직접 읽어서 계산한다.
- 패턴 B: 검증을 값 객체 `FormValues`에 대한 순수 함수 `FormValues.dateRangeError`로 작성한다.

### (a) 실측 결과 표

| 구분 | 협력자 수 | Arrange 줄 수 | 테스트 러너 | 실행 결과 |
|---|---|---|---|---|
| 패턴 A | 2 (`FormViewModelA` 1개 + `TextFieldState` 2개 조작) | 3줄 | 순수 JVM (JUnit4) | 통과 (`patternA_endBeforeStart_isError`, `patternA_normalRange_isNotError`) |
| 패턴 B | 1 (`FormValues` 값 객체 1개) | 1줄 | 순수 JVM (JUnit4) | 통과 (`patternB_endBeforeStart_isError`, `patternB_normalRange_isNotError`) |

패턴 A Arrange(3줄):

```kotlin
val vm = FormViewModelA()
vm.startDate.setTextAndPlaceCursorAtEnd("2026-05-10")
vm.endDate.setTextAndPlaceCursorAtEnd("2026-05-01")
```

패턴 B Arrange(1줄):

```kotlin
val values = FormValues(startDate = "2026-05-10", endDate = "2026-05-01")
```

전체 실행 결과: `./gradlew :app:testDebugUnitTest` → `CrossFieldValidationTest` 4개 테스트 모두 통과(BUILD SUCCESSFUL). 테스트 소스는 `app/src/test/java/com/sonms/textfieldstatetest/form/CrossFieldValidationTest.kt`.

### (b) 예상 밖의 동작

- **패턴 A도 Compose 테스트 러너가 필요하지 않았다.** `androidx.compose.foundation.text.input.TextFieldState`는 순수 Kotlin 타입이라, JVM 단위 테스트에서 생성과 `setTextAndPlaceCursorAtEnd` 호출이 그대로 동작했다. Robolectric이나 `ComposeTestRule` 없이 통과했다. 두 패턴의 차이는 "JVM 대 Compose 러너"가 아니라 Arrange 분량과 협력자 수였다.
- 패턴 A에서도 검증을 `endBeforeStart` 같은 순수 함수로 분리하면 패턴 B와 동일한 1줄 Arrange가 된다. 즉 패턴 A가 구조적으로 불리한 것이 아니라, "ViewModel이 `TextFieldState`를 들고 있으니 검증도 거기서 읽자"는 자연스러운 선택이 테스트에서 ViewModel 인스턴스화와 상태 주입을 요구하게 만든다.

### (c) 결론

교차 필드 검증이 **여러 개** 필요하고 그 로직을 자주 테스트한다면, 검증을 값 스냅샷에 대한 순수 함수로 밀어내는 **패턴 B가 유리하다**(Arrange 1줄, 협력자 1개). 패턴 A는 검증 순수 함수를 별도로 분리하는 규율을 지켜야 같은 수준이 된다.

---

## 실험 2: 프로세스 강제 종료 후 복원

절차: 세 구획의 5개 필드를 모두 채운다 → `HOME` 키로 백그라운드 전환(→ `onSaveInstanceState` 실행) → `adb shell am kill com.sonms.textfieldstatetest`(프로세스 소멸 확인: "PROCESS GONE") → `am start`로 재실행. 재실행 시 `savedInstanceState != null`이 로그로 확인됨("Activity 재생성됨").

### (a) 실측 결과 표

| 구분 | 프로세스 종료 후 재실행 | 복원을 위한 추가 코드 | 검증 로직 상태 |
|---|---|---|---|
| 패턴 A (기본) | **소실** — 5개 필드 모두 빈 값 | 없음(복원 안 됨) | `a_error`가 "정상"으로 초기화 |
| 패턴 A+Saved | **복원** — 5개 필드 모두 입력값 유지 | `SavedStateHandle` 연동 코드 추가 (아래) | `s_error` "종료일이 시작일보다 빠릅니다" 유지 |
| 패턴 B | **복원** — 5개 필드 모두 입력값 유지 | **0줄** (`rememberTextFieldState()`가 이미 처리) | `b_error` "종료일이 시작일보다 빠릅니다" 유지 |

패턴 A+Saved가 `FormViewModelA`에 추가한 코드: 실질 코드 약 8줄 + import 4개.

```kotlin
class FormViewModelASaved(private val handle: SavedStateHandle) : ViewModel() {   // 생성자 파라미터 추가
    val title = handle.restoringField("title")   // 필드마다 저장 키 문자열 부여
    // ... (5개 필드 동일)

    private fun SavedStateHandle.restoringField(key: String): TextFieldState {     // 헬퍼 7줄
        val state = TextFieldState(get<String>(key).orEmpty())
        viewModelScope.launch {
            snapshotFlow { state.text.toString() }.collect { set(key, it) }
        }
        return state
    }
}
```

추가 import: `androidx.compose.runtime.snapshotFlow`, `androidx.lifecycle.SavedStateHandle`, `androidx.lifecycle.viewModelScope`, `kotlinx.coroutines.launch`.

### (b) 예상 밖의 동작

- `adb shell am kill`은 "백그라운드 프로세스만" 종료하므로, 반드시 `HOME`으로 먼저 백그라운드 전환해야 한다. 포그라운드 상태에서 `am kill`을 호출하면 아무 일도 일어나지 않는다.
- 패턴 B에서 화면 하단의 `시작일` / `종료일`은 소프트 키보드에 가려 스크롤해야 접근되는데, 프로세스 종료 후에도 스크롤 위치와 무관하게 5개 값이 모두 복원되었다. `rememberTextFieldState()` 내부의 `rememberSaveable`은 컴포지션에서 잠깐 벗어난 항목도 `SavedStateRegistry`에 남긴다.
- RK3566 보드에서 다른 앱(`com.mobis.vai.agent`)이 포그라운드를 가로채면서 테스트 앱이 예기치 않게 백그라운드로 밀리는 경우가 있었다. 측정 전 `am start`로 포그라운드를 재확인해야 했다.

### (c) 결론

프로세스 종료 복원이 요구사항이면 **패턴 B가 유리하다** — `rememberTextFieldState()`만으로 추가 코드 없이 복원된다. 패턴 A는 `SavedStateHandle` 연동 코드(필드당 저장 키 + `snapshotFlow` 관찰 헬퍼)를 직접 작성해야 하고, 이를 빠뜨리면 조용히 소실된다.

---

## 실험 3: 화면 회전(config change)

RK3566 보드는 화면이 고정 가로(`ROTATION_0`)라 `settings put system user_rotation 1`로는 Activity 재생성이 일어나지 않았다. 동일한 메커니즘(Activity 소멸 후 재생성, ViewModel 생존, `rememberSaveable` 복원)을 트리거하기 위해 `adb shell cmd uimode night yes`로 실제 config change를 발생시켰고, 로그로 재생성을 확인했다.

### (a) 실측 결과 표

| 구분 | config change 후 | 값이 유지되는 이유 |
|---|---|---|
| 패턴 A (기본) | **유지** — 5개 필드 입력값 유지 | ViewModel이 config change에서 생존하고, `TextFieldState`가 그 ViewModel의 프로퍼티이므로 그대로 참조된다. |
| 패턴 A+Saved | **유지** | ViewModel 생존(위와 동일). `SavedStateHandle` 복원은 이 경우 사용되지 않는다. |
| 패턴 B | **유지** — 5개 필드 입력값 유지 | ViewModel(`FormViewModelB`)은 생존하지만 값을 소유하지 않는다. `TextFieldState`는 `rememberTextFieldState()` → `rememberSaveable`이 `SavedStateRegistry`에서 복원한다. |

세 구획 모두 재생성 직후 교차 검증 오류 표시("종료일이 시작일보다 빠릅니다")까지 그대로 유지되었다.

### (b) 예상 밖의 동작

- 실기기라고 해서 회전이 항상 config change를 일으키는 것은 아니다. RK3566처럼 디스플레이 방향이 고정된 장비에서는 `user_rotation` 변경이 무시된다. `uimode`, `font_scale` 등 다른 config 축을 써야 재생성을 관측할 수 있다.
- 패턴 A와 패턴 B는 "값이 유지된다"는 결과는 같지만 경로가 완전히 다르다. 패턴 A는 **ViewModel 생존**, 패턴 B는 **`rememberSaveable` 복원**이다. 이 차이가 실험 2에서 갈린다(ViewModel은 프로세스 종료에서 살아남지 못한다).

### (c) 결론

config change만 놓고 보면 두 패턴이 동등하다. 이 변수 하나만으로는 패턴 선택 근거가 되지 못한다 — 유지되는 메커니즘이 다를 뿐 결과가 같다.

---

## 패턴 선택 가이드

측정된 변수는 두 가지다: **교차 필드 검증의 유무**와 **프로세스 종료 복원 요구사항의 유무**. (필드 개수는 기준이 아니다.)

| 상황 | 권장 패턴 | 근거 |
|---|---|---|
| 교차 필드 검증 없음 + 복원 요구 없음 | 아무거나 (패턴 A가 더 단순) | 세 실험 모두 유의미한 차이 없음. config change는 두 패턴 모두 통과. |
| 교차 필드 검증 있음 (자주 테스트) | **패턴 B** | 검증을 값 스냅샷 순수 함수로 두면 Arrange 1줄, 협력자 1개. 패턴 A는 순수 함수 분리 규율을 별도로 지켜야 동급. |
| 프로세스 종료 복원 요구 있음 | **패턴 B** | `rememberTextFieldState()`가 추가 코드 0줄로 복원. 패턴 A는 필드당 저장 키 + `snapshotFlow` 관찰 헬퍼(약 8줄 + import 4개)를 직접 작성해야 하고, 누락 시 조용히 소실. |
| 위 두 요구가 모두 있음 | **패턴 B** | 두 이점이 겹친다. |
| ViewModel이 서버 제출·비동기 로직의 소유자여야 함 | 패턴 B (ViewModel은 `snapshotFlow`로 값만 관찰) 또는 패턴 A+Saved | 패턴 B에서도 ViewModel은 `FormValues`를 관찰해 제출 로직을 가질 수 있다. |

한 줄 요약: **교차 검증이 있거나 프로세스 종료 복원이 필요하면 패턴 B. 둘 다 없으면 패턴 A로도 충분하다.**
