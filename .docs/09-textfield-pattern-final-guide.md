# 09. TextFieldState 다중 필드 폼: 패턴 최종 정리와 선택 가이드

`07-textfield-pattern.md`(패턴 A vs B)와 `08-textfield-container-pattern.md`(패턴 A′)의 실측 결과를
하나로 합치고, 상황별로 어떤 패턴을 쓰면 좋은지 정리한다.

측정 대상 화면: 필드 5개(제목 / 설명 / 가격 / 시작일 / 종료일), 교차 필드 검증은 "종료일이 시작일보다
빠르면 오류". 측정 장비는 RK3566 보드와 SM-G950N, 단위 테스트는 로컬 JVM(JDK 21).

---

## 1. 패턴 목록

| 패턴 | 값을 소유하는 곳 | 저장/복원 연동 | 대표 클래스 |
|---|---|---|---|
| **A** | ViewModel이 `TextFieldState`를 낱개 프로퍼티로 보유 | 없음 | `FormViewModelA` |
| **A+Saved** | 위와 같음 | `SavedStateHandle` 연동을 ViewModel 본문에 작성 | `FormViewModelASaved` |
| **A′** | ViewModel이 `TextFieldState`들을 `@Stable` plain 클래스 컨테이너로 묶어 보유 | 없음 | `FormViewModelA2` + `CourseFormFields` |
| **A′+Saved** | 위와 같음 | `SavedStateHandle` 연동을 컨테이너 클래스 안으로 격리 | `FormViewModelA2Saved` + `SavedCourseFormFields` |
| **B** | 화면 레벨 `@Stable` 상태 홀더가 `rememberTextFieldState()`로 컴포저블 스코프에서 보유. ViewModel은 `snapshotFlow`로 값 스냅샷만 관찰 | `rememberTextFieldState()`가 기본으로 제공 | `FormStateHolderB` + `FormViewModelB` |

패턴 A′는 패턴 A의 가독성 개선 변형이다. 컨테이너는 반드시 재생성/`copy()`를 노출하지 않는 plain
클래스여야 하며(data class로 만들면 안 됨), ViewModel이 컨테이너를 생성자에서 한 번만 만들어 보유한다.

---

## 2. 세 실험 결과 종합

### 실험 1 — 교차 필드 검증 로직의 테스트 용이성

| 패턴 | 협력자 수 | Arrange 줄 수 | 테스트 러너 |
|---|---|---|---|
| A | 2 (ViewModel 1개 + `TextFieldState` 2개 조작) | 3줄 | 순수 JVM |
| A′ | 2 (ViewModel 1개 + `TextFieldState` 2개 조작) | 3줄 | 순수 JVM |
| A′ (컨테이너 단독) | 2 (`CourseFormFields` 1개 + `TextFieldState` 2개 조작) | 3줄 | 순수 JVM |
| B | 1 (`FormValues` 값 객체 1개) | 1줄 | 순수 JVM |

위 표의 각 행은 `CrossFieldValidationTest.kt`의 실제 테스트에서 나왔다(7개 전부 통과).

- 패턴 A: `patternA_endBeforeStart_isError`, `patternA_normalRange_isNotError`
- 패턴 A′: `patternA2_endBeforeStart_isError`, `patternA2_normalRange_isNotError` (둘 다 `FormViewModelA2` 인스턴스화)
- 패턴 A′ (컨테이너 단독): `patternA2_container_isTestableWithoutViewModel` — `CourseFormFields()`를 직접 생성하고 ViewModel을 만들지 않는다
- 패턴 B: `patternB_endBeforeStart_isError`, `patternB_normalRange_isNotError`

관찰:

- `androidx.compose.foundation.text.input.TextFieldState`는 순수 Kotlin 타입이므로, 다섯 패턴 모두
  Robolectric이나 `ComposeTestRule` 없이 순수 JVM 단위 테스트로 끝난다.
- 패턴 A와 패턴 A′는 수치가 완전히 같다. `vm.fields.startDate`처럼 접근 경로가 한 단계 길어질 뿐이다.
- 패턴 B가 1줄인 이유는 값 객체 `FormValues`의 생성자로 값을 바로 주입하기 때문이다. 패턴 A/A′에서도
  검증을 `endBeforeStart` 같은 순수 함수로 분리하면 동일하게 1줄이 된다. 즉 이 차이는 능력의 차이가
  아니라 **기본값의 차이다.** 패턴 B는 ViewModel이 값 스냅샷만 볼 수 있어 순수 함수 검증이 강제되고,
  패턴 A/A′는 "`TextFieldState`를 쥐고 있으니 거기서 읽자"는 선택이 손이 덜 가서 테스트가 ViewModel
  인스턴스화를 요구하게 된다.

### 실험 2 — 프로세스 강제 종료 후 복원

절차: 필드 입력 → `HOME` → `adb shell am kill` (프로세스 소멸 확인) → `savedInstanceState`가 있는
재실행.

| 패턴 | 프로세스 종료 후 | 저장/복원에 드는 추가 코드 |
|---|---|---|
| A (기본) | **소실** — 필드 빈 값, 검증도 "정상"으로 초기화 | 없음 |
| A′ (기본) | **소실** — 동일 (SM-G950N 실측) | 없음 |
| A+Saved | **복원** — 필드·검증 오류 모두 유지 | ViewModel 본문에 1회 고정 약 8줄(`restoringField` 헬퍼) + import 4개 + 필드당 1줄 |
| A′+Saved | **복원** — 동일 (SM-G950N 실측) | ViewModel 본문 1줄. 8줄 헬퍼는 `SavedCourseFormFields` 클래스 안으로 격리 |
| B | **복원** — 필드·검증 오류 모두 유지 | 저장/복원 로직 **0줄** (`rememberTextFieldState()`의 `rememberSaveable`이 처리) |

- 저장/복원 보일러플레이트의 형태는 "1회 고정 약 8줄(헬퍼) + 필드당 1줄"이다. 이 구조상 필드가
  늘어도 헬퍼는 그대로이고 필드당 1줄씩만 추가될 것으로 보인다(예: 20개면 대략 8줄 + 20줄). 다만
  **이번 실험은 필드 5개에서만 측정했다.** 필드 수를 늘려 이 선형 관계를 직접 확인하지는 않았으므로,
  `SavedStateHandle`에 항목이 아주 많아질 때 다른 오버헤드가 생기는지는 검증되지 않은 외삽이다.
- A′+Saved가 바꾸는 것은 그 보일러플레이트의 **위치**뿐이다(ViewModel 본문 → 컨테이너 클래스).
  총 코드량은 오히려 소폭 늘어난다. 컨테이너가 ViewModel이 아니라서 `viewModelScope`를 대신할
  `CoroutineScope`를 생성자로 받아야 하기 때문이다. 얻는 것은 "ViewModel을 읽을 때 저장/복원 세부가
  눈에 띄지 않는다"는 가독성이다.
- 결정적 차이는 분량이 아니라 **기본 동작의 방향이다.** 패턴 B는 코드를 안 써도 복원되고, 패턴 A
  계열은 `SavedStateHandle` 연동을 빠뜨리면 조용히 소실된다.

### 실험 3 — 화면 config change (회전, `uimode`, `font_scale` 등)

RK3566과 SM-G950N 모두 디스플레이 방향이 고정되어 `user_rotation` 변경이 무시되므로, 동일한
메커니즘(Activity 재생성 + ViewModel 생존)을 트리거하기 위해 `uimode night` 또는 `font_scale`을 썼다.

| 패턴 | config change 후 | 값이 유지되는 경로 |
|---|---|---|
| A / A′ | **유지** | ViewModel이 config change에서 생존하고, `TextFieldState`(또는 이를 담은 컨테이너)가 그 ViewModel의 프로퍼티이므로 그대로 참조된다 |
| A+Saved / A′+Saved | **유지** | 위와 동일. `SavedStateHandle` 복원 경로는 이 경우 사용되지 않는다 |
| B | **유지** | ViewModel은 생존하지만 값을 소유하지 않는다. `TextFieldState`는 `rememberTextFieldState()` → `rememberSaveable`이 `SavedStateRegistry`에서 복원한다 |

- 패턴 A′의 컨테이너는 회전 전후로 `System.identityHashCode`가 동일했다. ViewModel이 살아 있으면
  컨테이너도 같은 객체로 그대로 참조된다.
- 결과는 다섯 패턴 모두 같지만 경로가 다르다. 패턴 A 계열은 **ViewModel 생존**, 패턴 B는
  **`rememberSaveable` 복원**이다. 이 차이가 실험 2에서 갈린다. ViewModel은 프로세스 종료에서
  살아남지 못하기 때문이다.
- config change 하나만으로는 패턴 선택의 근거가 되지 못한다.

### 한 줄 요약

| 실험 | A | A′ | A+Saved | A′+Saved | B |
|---|---|---|---|---|---|
| 1. 교차 검증 테스트 | 3줄 / 협력자 2 | 3줄 / 협력자 2 | 3줄 / 협력자 2 | 3줄 / 협력자 2 | **1줄 / 협력자 1** |
| 2. 프로세스 종료 | 소실 | 소실 | 복원(8줄+α) | 복원(8줄+α, 위치만 이동) | **복원(0줄)** |
| 3. config change | 유지 | 유지 | 유지 | 유지 | 유지 |

---

## 3. 상황별 권장 패턴

측정된 판단 변수는 두 가지다. **교차 필드 검증이 있는가**와 **프로세스 종료 복원이 요구사항인가**.
필드 개수는 판단 기준이 아니다.

아래 권장에서 "패턴 B"는 "패턴 B가 우수하다"는 뜻이 아니라, **패턴 B는 해당 이점을 기본값으로
강제하고, 패턴 A 계열은 규율 또는 1회성 보일러플레이트로 같은 수준에 도달한다**는 뜻이다.

### 3-1. 이번 실험이 직접 측정한 상황

아래 네 행은 실험 1·2·3의 실측 결과에 근거한다.

| 상황 | 권장 | 근거 |
|---|---|---|
| 교차 검증 없음 + 복원 요구 없음 | **패턴 A** (필드가 많아 ViewModel 본문이 지저분하면 **패턴 A′**) | 세 실험 모두 유의미한 차이가 없다. 가장 단순한 쪽을 택한다 |
| 교차 검증 있음 | **패턴 B**, 또는 "검증 로직은 항상 순수 함수로 분리한다"는 규율을 지키는 **패턴 A / A′** | 패턴 B는 값 스냅샷 구조라 순수 함수 검증이 강제되어 테스트 Arrange가 1줄이다. 패턴 A/A′도 그 규율을 지키면 동일해지고, 안 지키면 테스트 1개당 2줄·협력자 1개가 더 든다 |
| 프로세스 종료 복원 요구 있음 | **패턴 B**, 또는 `SavedStateHandle` 보일러플레이트를 감수하는 **패턴 A+Saved / A′+Saved** | 패턴 B는 저장/복원 로직이 0줄이다. 패턴 A 계열은 1회 고정 약 8줄 + 필드당 1줄이며, 빠뜨리면 조용히 소실된다 |
| 위 두 요구가 모두 있음 | **패턴 B** | 두 이점이 추가 비용 없이 겹친다. 패턴 A 계열로 하려면 두 규율·보일러플레이트를 모두 감수해야 한다 |

### 3-2. 실험 범위 밖 또는 부분적으로만 실측된 상황

아래 첫 행은 실험 1·2·3이 다루지 않은 항목이라 Compose의 일반 원칙에 근거한 추정으로 남아 있다.
둘째 행은 `10-nav-scope-experiment.md`에서 별도로 실측했다.

| 상황 | 권장 | 근거 |
|---|---|---|
| ViewModel이 서버 제출·비동기 로직의 소유자여야 함 | **패턴 B** (ViewModel은 `snapshotFlow`로 `FormValues`를 관찰해 제출 로직을 가진다), 또는 **패턴 A+Saved / A′+Saved** | 미실측. 패턴 B의 `FormViewModelB`가 `FormValues`를 관찰하는 구조는 코드로 확인되지만, 실제 서버 제출·비동기 시나리오는 이번 실험에서 돌리지 않았다 |
| 폼이 여러 화면·탭에 걸쳐 있고 상태를 화면 밖에서 오래 들고 있어야 함 (bottom nav 탭 전환, 단일 `NavHostController`) | **패턴 A / A′도 안전하다, 이 시나리오에 한해.** `popUpTo(...) { saveState = true }` + `restoreState = true`를 쓰는 표준 탭 전환에서는 `viewModel()` 기본값(destination entry 스코프)도 값을 잃지 않는다 | **실측됨** (`10-nav-scope-experiment.md`). `NavController`가 팝된 엔트리의 `ViewModelStore` 자체를 보존했다가 복원하기 때문에, entry 스코프든 그래프 스코프든 결과가 같았다(`identityHashCode` 동일로 확인). 단, 이 결과는 "`NavController` 자신이 살아있는 동안"에만 성립한다 — 탭이 백그라운드로 밀려난 사이 **프로세스가 종료되면 실험 2의 결론이 그대로 적용되어** `SavedStateHandle` 없는 패턴 A/A′는 소실된다(재실험 불필요, 이미 알려진 메커니즘의 귀결). `saveState` 없는 완전한 pop과 탭마다 별도 `NavHostController`를 쓰는 아키텍처는 진짜 미실측이다(문서 §"이번 실측이 답하지 못한 것" 참고) |

### 패턴 A와 패턴 A′ 사이의 선택

패턴 A로 가기로 했다면, 필드가 많아 `FormViewModelA` 본문이 길어질 때 패턴 A′(컨테이너로 묶기)를
자유롭게 써도 된다. 세 실험 모두 실측치가 동일하다. 다만 다음을 지켜야 한다.

- 컨테이너는 반드시 재생성/`copy()`를 노출하지 않는 plain 클래스로 만든다. **data class 금지.**
- ViewModel이 컨테이너를 생성자에서 한 번만 만들고, 절대 `copy()`하거나 재대입하지 않는다.
- 컨테이너를 생성자 파라미터로 주입받게 만들지 않는다. 같은 인스턴스가 두 ViewModel에 들어가면
  상태가 공유되고, `SavedStateHandle` 연동 시 같은 키에 중복 수집이 돌아간다.
- `SavedStateHandle` 연동을 컨테이너로 옮겨도 코드 총량은 줄지 않는다. 위치만 옮겨가는 캡슐화다.

---

## 4. 선택 순서 (요약)

1. 프로세스 종료 복원이 요구사항인가? 그리고 교차 필드 검증이 있는가?
   - 둘 다 아니오 → **패턴 A**. 필드가 많으면 **패턴 A′**.
   - 하나라도 예 → 2번으로.
2. 팀이 "검증은 순수 함수로 분리" 규율과 `SavedStateHandle` 보일러플레이트를 감수할 수 있는가?
   - 예 → **패턴 A+Saved / A′+Saved** + 순수 함수 검증.
   - 아니오, 또는 두 이점을 추가 비용 없이 기본으로 받고 싶다 → **패턴 B**.
3. 폼 상태를 화면 수명보다 오래, 화면 밖에서(다른 탭으로 이동 등) 들고 있어야 하는가?
   - 단일 `NavHostController` + bottom nav 탭 전환(`saveState`/`restoreState`)이고, **프로세스 종료는
     신경 쓰지 않아도 된다면** → **패턴 A 계열로 충분하다.** `viewModel()` 기본값도 값을 잃지 않는다
     (`10-nav-scope-experiment.md` 실측). 그래프 스코프로 옮길 필요 없다.
   - 위와 같되 **탭이 백그라운드에 있는 동안 프로세스 종료까지 견뎌야 한다면** → 실험 2의 결론이
     그대로 적용된다. `SavedStateHandle` 없는 패턴 A/A′는 소실되므로 **패턴 A+Saved/A′+Saved 또는
     패턴 B**로 가야 한다.
   - `saveState` 없는 완전한 pop, 또는 탭마다 별도 `NavHostController`를 쓰는 구조라면 → 진짜
     미실측이니 적용 전 별도 실측 권장(3-2 참조).

---

## 5. 구현 스케치

### 패턴 A

```kotlin
class FormViewModelA : ViewModel() {
    val title = TextFieldState()
    val description = TextFieldState()
    val price = TextFieldState()
    val startDate = TextFieldState()
    val endDate = TextFieldState()

    val dateRangeError: Boolean
        get() = endBeforeStart(startDate.text.toString(), endDate.text.toString())
}
```

### 패턴 A′

```kotlin
@Stable
class CourseFormFields {                 // data class 아님
    val title = TextFieldState()
    val description = TextFieldState()
    val price = TextFieldState()
    val startDate = TextFieldState()
    val endDate = TextFieldState()

    val dateRangeError: Boolean
        get() = endBeforeStart(startDate.text.toString(), endDate.text.toString())
}

class FormViewModelA2 : ViewModel() {
    val fields = CourseFormFields()       // 생성자에서 한 번만
    val dateRangeError: Boolean get() = fields.dateRangeError
}
```

### 패턴 A+Saved (A′+Saved는 이 헬퍼를 컨테이너 클래스 안으로 옮긴 형태)

```kotlin
class FormViewModelASaved(private val handle: SavedStateHandle) : ViewModel() {
    val title = handle.restoringField("title")
    // ... 필드마다 저장 키 부여

    private fun SavedStateHandle.restoringField(key: String): TextFieldState {
        val state = TextFieldState(get<String>(key).orEmpty())
        viewModelScope.launch {
            snapshotFlow { state.text.toString() }.collect { set(key, it) }
        }
        return state
    }
}
```

### 패턴 B

```kotlin
@Stable
class FormStateHolderB(
    val title: TextFieldState,
    // ... 5개 필드
) {
    fun snapshot(): FormValues = FormValues(title = title.text.toString(), /* ... */)
}

@Composable
fun rememberFormStateHolderB(): FormStateHolderB {
    val title = rememberTextFieldState()
    // ... 5개 필드
    return remember { FormStateHolderB(title, /* ... */) }
}

data class FormValues(val title: String = "", /* ... */) {
    val dateRangeError: Boolean get() = endBeforeStart(startDate, endDate)
}

class FormViewModelB : ViewModel() {
    private val _values = MutableStateFlow(FormValues())
    val values = _values.asStateFlow()
    fun onValuesChanged(v: FormValues) { _values.value = v }
}
```

화면 쪽에서 컴포저블이 값 스냅샷을 관찰해 ViewModel로 전달한다.

```kotlin
LaunchedEffect(holderB) {
    snapshotFlow { holderB.snapshot() }.collect { vmB.onValuesChanged(it) }
}
```

---

## 6. 한계와 미실측 외삽

이 문서의 권장 중 일부는 실측 범위를 벗어난 외삽이다. 아래 표는 어느 항목이 어디까지 실측되었고,
어디서부터 추정으로 전환되는지를 분리해서 보여준다.

| 항목 | 실측 범위 | 미검증 리스크 | 확인 방법 |
|---|---|---|---|
| `SavedStateHandle` 보일러플레이트 선형성 ("1회 고정 8줄 + 필드당 1줄") | 필드 5개 | 코드 줄 수 자체는 `restoringField()` 호출 반복 횟수를 세는 문제라 device 실험 없이 코드만 봐도 선형임이 확정된다(`Bundle` 직렬화 비용은 문자열 20~30개 수준에서 `TransactionTooLargeException` 임계치인 약 1MB에 전혀 못 미치므로 애초에 쟁점이 아니다). 다만 `restoringField()`가 필드마다 `viewModelScope.launch { snapshotFlow{...}.collect{...} }`를 새로 띄우는 구조라서, 필드 20~30개면 ViewModel 생성 시점에 동시에 도는 컬렉터가 20~30개다. 이 컬렉터 수(코루틴 생성 비용, 연속 `launch` 초기화 비용)는 코드 줄 수와 별개의 축이며 미검증이다 | 코드 줄 수는 추가 실험이 불필요하다. 컬렉터 수 축만, 필드 20~30개짜리 ViewModel을 만들어 초기화 시점의 코루틴 launch 오버헤드와 타이핑 지연을 계측(마이크로벤치마크 또는 Compose 계측 테스트)한다 |
| 화면 밖에서 폼 상태를 오래 들고 있는 경우 (단일 `NavHostController` + bottom nav 탭 전환, 프로세스는 살아있음) | **실측됨** — `10-nav-scope-experiment.md` | 없음. `popUpTo(saveState=true)` + `restoreState=true` 조합에서는 `viewModel()` 기본값도 그래프 스코프도 값을 잃지 않는다는 것이 device 테스트로 확인됐다("`NavController` 자신이 살아있는 동안"이라는 전제에 한정) | 해당 없음 |
| 위와 같되 탭이 백그라운드에 있는 동안 **프로세스가 종료되는 경우** | 실험 2의 결론이 그대로 적용됨(재실험 불필요) | `NavController`와 그 `ViewModelStore`는 순수 in-memory 객체라 프로세스 종료로 통째로 사라진다. `rememberNavController()`의 `rememberSaveable`은 백스택 경로만 복원하고 `ViewModelStore`는 복원 대상이 아니다 | 해당 없음. `SavedStateHandle` 연동 여부로 실험 2와 동일하게 판단한다 |
| `saveState` 없는 완전한 pop, 또는 **탭마다 별도 `NavHostController`를 쓰는 아키텍처** | 다루지 않음(진짜 미실측) | 전자는 `ViewModelStore.clear()`가 호출되어 값을 잃을 것으로 보이나 실측 없음. 후자는 `NavController`가 애초에 달라서 이번 실험이 확인한 `ViewModelStore` 보존 메커니즘 자체가 적용되지 않을 수 있다(Fragment 여부와 무관하게 순수 Compose 안에서도 실무에 흔한 변형) | 실제로 쓸 구조(pop 방식, 단일/다중 `NavHostController`)를 정하고 `10-nav-scope-experiment.md`의 테스트를 그 구조로 변형해 재현한다 |
| 서버 제출·비동기 로직의 소유 | 다루지 않음 | 패턴 B에서 ViewModel이 `snapshotFlow`로 값만 관찰하는 구조가 실제 제출 로직(로딩·에러·재시도 상태 관리)과 잘 맞물리는지는 5장의 코드 스케치 수준에 그치며, 이를 뒷받침하는 실험 데이터는 없다 | 실제 제출 API 호출과 로딩·에러 상태를 패턴 B 구조에 연결하고, 제출 도중 화면 이탈·회전·프로세스 종료 같은 실패 시나리오까지 포함한 통합 테스트로 확인한다 |

### 실무 적용 시 신뢰 구간

- **그대로 가져다 써도 되는 결론** (§3-1, §3-2 둘째 행, 실측됨): 폼에 교차 필드 검증이 있거나 프로세스
  강제 종료 복원이 요구사항이면 패턴 B가 그 이점을 기본값으로 제공한다는 판단, 그리고 **프로세스가
  살아있는 동안의** bottom nav 탭 전환(단일 `NavHostController`)에서는 패턴 A 계열도 값을 잃지
  않는다는 판단은 신뢰할 수 있다. 단, 탭이 백그라운드에 있는 사이 프로세스가 종료되는 경우는 이
  결론이 적용되지 않고 실험 2의 결론(`SavedStateHandle` 없는 패턴 A/A′는 소실)로 돌아간다.
- **직접 확인해야 하는 결론** (미실측): `saveState` 없는 pop, 탭마다 별도 `NavHostController`를 쓰는
  아키텍처, 서버 제출·비동기 로직과의 통합은 이 저장소의 권장을 참고 자료로만 삼고, 실제 프로젝트에서
  위 표의 확인 방법대로 별도로 실측해야 한다.

---

## 참고

- 상세 실측 로그와 예상 밖의 동작: `07-textfield-pattern.md`, `08-textfield-container-pattern.md`,
  `10-nav-scope-experiment.md`
- 검증 테스트: `app/src/test/java/com/sonms/textfieldstatetest/form/CrossFieldValidationTest.kt`,
  `app/src/androidTest/java/com/sonms/textfieldstatetest/navexperiment/NavScopeExperimentTest.kt`
- RK3566 보드 실험 제약과 우회법: 저장소 메모리 `rk3566-test-device`
