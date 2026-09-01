# 08. TextFieldState 컨테이너 클래스(패턴 A′): 패턴 A와 동일한가

## 검증 대상

`07-textfield-pattern.md`에서 비교한 패턴 A와 패턴 B 사이에 있는 제3의 변형을 추가로 측정했다.

- **패턴 A′**: 패턴 A와 똑같이 ViewModel이 `TextFieldState`를 보유하되, 낱개 프로퍼티 대신
  plain 클래스 컨테이너 `CourseFormFields` 하나로 묶어 가독성만 개선한 것이다. (`FormViewModelA2`)
  - **A′+Saved**: `SavedStateHandle` 연동을 컨테이너(`SavedCourseFormFields`) 안으로 옮긴 변형이다.
    (`FormViewModelA2Saved`)

컨테이너는 **data class가 아니다.** `copy()`가 생기면 값을 담는 그릇이 재생성되어 참조 동일성이
깨질 수 있으므로, 재생성/복사를 노출하지 않는 plain 클래스로 선언했다. 코드 작성 후 다시 확인했으며,
`CourseFormFields`와 `SavedCourseFormFields` 모두 `@Stable class`(data class 아님)이다.

새로 추가한 파일:

- `form/patterna2/CourseFormFields.kt` — `CourseFormFields`(plain), `SavedCourseFormFields`(plain, 생성자에서 `SavedStateHandle`과 `CoroutineScope`를 받음)
- `form/patterna2/FormViewModelA2.kt` — `val fields = CourseFormFields()` 한 번만 생성
- `form/patterna2/FormViewModelA2Saved.kt` — `val fields = SavedCourseFormFields(handle, viewModelScope)` 한 줄

측정 장비: 이번에는 RK3566 보드가 연결되어 있지 않아, 두 번째 기기 SM-G950N(Android 9, API 28,
1080×2220)에서 측정했다. 단위 테스트는 로컬 JVM(JDK 21). 기기가 달라졌으나 이번 실험의 비교는
"같은 기기·같은 실행 안에서 패턴 A와 패턴 A′가 갈리는가"이므로, 화면에 다섯 구획(A / A+Saved /
A′ / A′+Saved / B)을 모두 띄우고 한 번에 대조했다.

---

## 실험 1: 교차 필드 검증 로직의 테스트 용이성

패턴 A′에도 동일한 검증(`endBeforeStart()`로 "종료일 < 시작일이면 오류")을 붙였다. 검증 프로퍼티는
컨테이너(`CourseFormFields.dateRangeError`)에 두고, `FormViewModelA2.dateRangeError`가 이를 위임한다.

### (a) 실측 결과 표 (07 문서 표에 이어붙일 수 있는 형식)

| 구분 | 협력자 수 | Arrange 줄 수 | 테스트 러너 | 실행 결과 |
|---|---|---|---|---|
| 패턴 A | 2 (`FormViewModelA` 1개 + `TextFieldState` 2개 조작) | 3줄 | 순수 JVM (JUnit4) | 통과 |
| **패턴 A′** | **2 (`FormViewModelA2` 1개 + `TextFieldState` 2개 조작)** | **3줄** | **순수 JVM (JUnit4)** | **통과** (`patternA2_endBeforeStart_isError`, `patternA2_normalRange_isNotError`) |
| **패턴 A′ (컨테이너 단독)** | **2 (`CourseFormFields` 1개 + `TextFieldState` 2개 조작)** | **3줄** | **순수 JVM (JUnit4)** | **통과** (`patternA2_container_isTestableWithoutViewModel`) |
| 패턴 B | 1 (`FormValues` 값 객체 1개) | 1줄 | 순수 JVM (JUnit4) | 통과 |

패턴 A′ Arrange(3줄):

```kotlin
val vm = FormViewModelA2()
vm.fields.startDate.setTextAndPlaceCursorAtEnd("2026-05-10")
vm.fields.endDate.setTextAndPlaceCursorAtEnd("2026-05-01")
```

전체 실행 결과: `./gradlew :app:testDebugUnitTest` → `CrossFieldValidationTest` 7개 테스트 모두 통과
(BUILD SUCCESSFUL, `tests="7" failures="0" errors="0"`). 패턴 A′ 테스트 3개가 추가되었다.

### (b) 확인 사항 — 컨테이너 경유가 수치에 영향을 주는가

**주지 않는다.** `vm.fields.startDate`는 `vm.startDate`보다 접근 경로가 한 단계 길 뿐이고,
줄 수와 협력자 수는 그대로다.

- 컨테이너 생성자가 값을 받지 않으므로, 테스트에서 각 필드에 값을 넣으려면 여전히
  `setTextAndPlaceCursorAtEnd`를 필드마다 호출해야 한다. 패턴 B의 1줄(`FormValues(startDate = …, endDate = …)`)에
  도달하지 못한다.
- 컨테이너가 독립 객체라 `FormViewModelA2` 없이 `CourseFormFields()`만으로도 검증을 테스트할 수 있지만
  (`patternA2_container_isTestableWithoutViewModel`), Arrange는 여전히 3줄이다. ViewModel 인스턴스화
  1줄이 컨테이너 인스턴스화 1줄로 바뀔 뿐이다.

### (c) 결론

실험 1에서 패턴 A′는 패턴 A와 **수치가 완전히 동일하다** (협력자 2, Arrange 3줄, 순수 JVM 러너).
07 문서 실험 1의 결론("능력 차이가 아니라 기본값 차이")이 패턴 A′에도 그대로 적용된다. 검증을
컨테이너에 두든 ViewModel에 두든, "`TextFieldState`를 쥐고 있으니 거기서 읽자"는 선택이 테스트에서
인스턴스화와 상태 주입을 요구한다는 점은 바뀌지 않는다. 패턴 A′에서도 검증을 `endBeforeStart` 같은
순수 함수로 분리하면 패턴 B와 같은 1줄 Arrange가 된다.

---

## 실험 2: 프로세스 강제 종료 후 복원

### (a) 실측 결과 표

| 구분 | 프로세스 종료 후 재실행 | 복원을 위한 추가 코드 | 검증 로직 상태 |
|---|---|---|---|
| 패턴 A (기본) | 소실 (07 문서 실측) | 없음 | "정상"으로 초기화 |
| 패턴 A+Saved | 복원 (07 문서 실측) | ViewModel 본문에 1회 고정 8줄(헬퍼) + import 4개 + 필드당 1줄 | 오류 표시 유지 |
| **패턴 A′ (기본)** | **소실** (SM-G950N 실측) — 시작일·종료일 빈 값, "날짜 범위 정상" | 없음 | "정상"으로 초기화 |
| **패턴 A′+Saved** | **복원** (SM-G950N 실측) — 시작일 `2026-05-10`, 종료일 `2026-05-01`, 오류 표시 유지 | **ViewModel 본문 1줄**(`val fields = SavedCourseFormFields(handle, viewModelScope)`). 8줄 헬퍼·`snapshotFlow`·해당 import는 `SavedCourseFormFields` 안으로 격리 | 오류 표시 유지 |
| 패턴 B | 복원 (07 문서 실측) | 저장/복원 로직 0줄 | 오류 표시 유지 |

### (b) 실측 절차와 결과

이번 세션에는 07 문서가 사용한 RK3566 보드가 연결되어 있지 않아 두 번째 기기 SM-G950N에서 측정했다.
07 문서와 동일한 메커니즘(프로세스 소멸 → `savedInstanceState`가 있는 재실행)을 재현했다.

1. 패턴 A′와 패턴 A′+Saved의 시작일(`2026-05-10`)·종료일(`2026-05-01`) 필드를 채우고, 두 구획 모두
   교차 검증 오류("종료일이 시작일보다 빠릅니다")가 표시되는 것을 화면으로 확인했다.
2. `input keyevent HOME` → `adb shell am kill com.sonms.textfieldstatetest` → `pidof`가 빈 값을
   반환하는 것으로 프로세스 소멸을 확인했다(PID 7099 → 없음 → 8657).
3. 재실행은 `adb shell monkey -p … -c android.intent.category.LAUNCHER 1`로 했다. `am start -n
   …/.MainActivity`는 저장된 태스크를 재개하지 않고 새 태스크(`icicle=null`, "Activity 최초 생성")를
   만들지만, `am kill`(force-stop 아님) 뒤 monkey의 LAUNCHER 인텐트는 기존 태스크를 그대로 재개한다.
   재실행 로그에 **"Activity 재생성됨 (config change 등)"**이 찍혔다 — `savedInstanceState != null`이다.
4. 재실행 후 화면 확인:
   - **패턴 A′(기본)**: 시작일·종료일 모두 빈 값, 오류 없이 "날짜 범위 정상". **소실**.
   - **패턴 A′+Saved**: 시작일 `2026-05-10`, 종료일 `2026-05-01`, "종료일이 시작일보다 빠릅니다" 유지. **복원**.

결과는 패턴 A(소실) / 패턴 A+Saved(복원)와 정확히 일치한다. 코드 구성으로도 예측되는 결과다.

- **패턴 A′(기본)**: `CourseFormFields`는 `TextFieldState()`를 그냥 들고 있고 `FormViewModelA2`에는
  `SavedStateHandle`이 없다. 어떤 번들에도 값을 쓰지 않으므로 프로세스가 죽으면 반드시 소실된다.
- **패턴 A′+Saved**: `SavedCourseFormFields.restoringField(key)`는 `FormViewModelASaved.restoringField(key)`와
  줄 단위로 같은 코드다 — 같은 `SavedStateHandle`, 같은 문자열 키("title" … "endDate"), 같은
  `viewModelScope`, 같은 `snapshotFlow` 쓰기. 담는 클래스만 다르다.

### (c) 코드량 가설 검증 — 헬퍼가 컨테이너 안으로 격리되는가

가설: "패턴 A′+Saved는 ViewModel에는 컨테이너 생성 1줄만 남고, 8줄 헬퍼는 컨테이너 클래스 안으로
격리된다."

**확인됨 (코드 대조).**

| | `FormViewModelASaved` (패턴 A+Saved) | `FormViewModelA2Saved` (패턴 A′+Saved) |
|---|---|---|
| ViewModel 본문 코드 | 필드 5줄 + `dateRangeError` 2줄 + `restoringField` 헬퍼 7줄 | `val fields = …` 1줄 + `dateRangeError` 위임 1줄 |
| ViewModel 파일 import | 7개 (`TextFieldState`, `snapshotFlow`, `SavedStateHandle`, `ViewModel`, `viewModelScope`, `endBeforeStart`, `launch`) | 3개 (`SavedStateHandle`, `ViewModel`, `viewModelScope`) |
| 저장/복원 배선 위치 | ViewModel 본문 | `SavedCourseFormFields` 클래스 안 |

단, 이것은 **격리(캡슐화)이지 총량 감소가 아니다.** 헬퍼 7줄은 사라지지 않고 `SavedCourseFormFields`로
이동하며, 컨테이너가 ViewModel이 아니라서 `viewModelScope`를 대신할 `CoroutineScope`를 생성자로 명시적으로
받아야 하는 배선(생성자 파라미터 1개 + 전달 1줄)이 오히려 늘어난다. 두 파일을 합친 코드 총량은
패턴 A+Saved보다 약간 많다. 얻는 것은 "ViewModel을 읽을 때 저장/복원 세부가 눈에 안 띈다"는 가독성이다.

### (d) 결론

프로세스 종료 복원 관점에서 패턴 A′는 패턴 A와 **동작이 동일하다.** 기본형은 소실되고, `SavedStateHandle`
배선을 더한 형은 복원된다. 패턴 A′+Saved가 바꾸는 것은 그 배선의 **위치**(ViewModel 본문 → 컨테이너
클래스)뿐이며, 복원 여부·타이밍·키는 그대로다. "안 쓰면 소실"이라는 기본 동작의 방향도 패턴 A와 같다.

---

## 실험 3: 화면 config change

RK3566처럼 SM-G950N도 adb로 `user_rotation`을 바꿔도 Activity 재생성이 일어나지 않았다. 07 문서와
동일하게 `adb shell settings put system font_scale 1.30`으로 실제 config change를 발생시켰고, 로그로
재생성을 확인했다.

컨테이너가 회전 후에도 같은 객체인지 직접 확인하려고, 화면 진입 시
`System.identityHashCode(vmA2.fields)`와 `vmA2Saved.fields`를 로그로 남겼다. Activity가 재생성되면 컴포지션이
새로 만들어져 `LaunchedEffect(Unit)`가 다시 실행되므로, ViewModel(과 그 안의 `fields`)이 생존했다면
identityHashCode가 이전과 같아야 한다.

### (a) 실측 결과 표

| 구분 | config change 후 | 값이 유지되는 이유 |
|---|---|---|
| 패턴 A (기본) | 유지 (07 문서 실측) | ViewModel 생존, `TextFieldState`가 그 프로퍼티 |
| **패턴 A′ (기본)** | **유지** | ViewModel(`FormViewModelA2`) 생존 → `fields` 컨테이너 생존 → 같은 `TextFieldState` 참조 |
| **패턴 A′+Saved** | **유지** | 위와 동일. `SavedCourseFormFields` 복원 경로는 이 경우 쓰이지 않음 |
| 패턴 B | 유지 (07 문서 실측) | `rememberTextFieldState()` → `rememberSaveable` 복원 |

### (b) identityHashCode 로그 (SM-G950N, 같은 PID)

```
# config change 전
I TFPattern: A2 fields identity=222455264, A2Saved fields identity=193974425
# font_scale 1.30 적용
I TFPattern: Activity 재생성됨 (config change 등)
# config change 후
I TFPattern: A2 fields identity=222455264, A2Saved fields identity=193974425
```

재생성 전후로 `fields` 인스턴스의 identityHashCode가 동일하다. 컨테이너를 생성자에서 한 번만 만들고
`copy()`하지 않는다는 전제가 실제로 지켜지며, 회전(= config change) 후에도 같은 객체가 그대로
참조된다. 다른 실행에서도 동일하게 재현되었다(예: `177103745 / 129489958`가 재생성 전후 동일).

### (c) 결론

실험 3에서 패턴 A′는 패턴 A와 **경로도 결과도 동일하다.** 둘 다 ViewModel 생존에 의존하며,
컨테이너로 한 겹 감싼 것이 이 경로에 아무 영향을 주지 않는다. 07 문서 실험 3의 결론(config change
하나만으로는 패턴 선택 근거가 못 된다)이 패턴 A′에도 그대로 적용된다.

---

## 추가 확인 — 이 변형에서만 생길 수 있는 리스크

### `@Stable` 어노테이션과 recomposition 횟수

**런타임 카운터로는 측정하지 않았다.** 이 화면 구성에서는 측정 대상이 성립하지 않기 때문이다.

`@Stable`/`@Immutable`은 Compose가 "이 파라미터가 안 바뀌었으니 이 컴포저블 재구성을 건너뛰어도 된다"고
판단하게 하는 표시다. 그런데 이 화면(`FormScreens.kt`)에서 컨테이너 인스턴스(`vm.fields`)는 **어떤
컴포저블에도 파라미터로 전달되지 않는다.** 개별 `TextFieldState` 필드만 `Field(state = …)`로 넘어가고,
`TextFieldState` 자체가 이미 Compose용 `@Stable` 타입이다. 따라서 컨테이너 클래스의 `@Stable` 유무는
이 화면의 recomposition에 영향을 주지 않는다.

`@Stable`이 의미를 갖는 경우는 컨테이너를 통째로 컴포저블 파라미터로 넘길 때다(`MyForm(fields: CourseFormFields)`).
그때는 어노테이션이 없으면 Compose가 plain 클래스를 unstable로 보아 재구성을 건너뛰지 못한다. 이번
컨테이너에 `@Stable`을 붙여 둔 것은 그런 사용법에 대비한 것이며, 현재 화면에서는 중립이다.

### 컨테이너를 여러 ViewModel에서 재사용하는 실수 (코드 리뷰 관점, 재현하지 않음)

**막는 장치가 코드에 없다.** `CourseFormFields()`는 public 무인자 생성자다. `private` 생성자도
팩토리 함수도 없으므로, `val shared = CourseFormFields()`를 만들어 두 ViewModel에 주입하는 것을
문법적으로 막지 못한다. 그렇게 하면 두 ViewModel이 같은 `TextFieldState` 인스턴스들을 공유하여
한 화면의 입력이 다른 화면에 반영되고, `SavedCourseFormFields`라면 두 `viewModelScope`에서 같은 키에
중복으로 `snapshotFlow` 수집이 돌아간다.

현재 코드에서 이 실수가 실제로 일어나긴 어렵다. `FormViewModelA2`는 컨테이너를 **주입받지 않고
내부에서 직접** `CourseFormFields()`로 생성하며(`FormViewModelA2Saved`도 `viewModelScope`가 필요해
자기 스코프로 인라인 생성), 매 인스턴스가 새 컨테이너를 갖는다. 따라서 "같은 인스턴스를 두 곳에"는
`FormViewModelA2`의 시그니처를 능동적으로 바꿔 컨테이너를 생성자 파라미터로 받게 고쳐야만 가능하다.

권장:

- 컨테이너 생성은 지금처럼 ViewModel 내부에 두고, 주입을 유도하는 생성자를 공개 API로 노출하지 않는다
  (현재 상태가 이미 가장 안전하다).
- 향후 주입이 꼭 필요하면, 호출마다 새 인스턴스를 반환하는 팩토리 함수로 넘기거나 소유권을 주석으로
  명시한다. 공유 가능한 싱글턴처럼 보이는 형태(top-level `val`, `object`)로 만들지 않는다.

---

## 종합 결론: 패턴 A와 패턴 A′는 실측 상 동일한가

**세 실험 모두에서 패턴 A′는 패턴 A와 수치·동작이 동일하다.** 새로 갈리는 지표는 없다.

| 실험 | 패턴 A′ 대 패턴 A | 차이 |
|---|---|---|
| 1. 교차 검증 테스트 용이성 | 협력자 2, Arrange 3줄, 순수 JVM — **동일** | 접근 경로만 `vm.fields.x`로 한 단계 길어짐. 수치 차이 없음 |
| 2. 프로세스 종료 복원 | 기본형 소실 / Saved형 복원 — **동일** (SM-G950N에서 `am kill` 후 `savedInstanceState != null` 재실행으로 실측) | `SavedStateHandle` 배선의 **위치**만 ViewModel 본문 → 컨테이너 클래스. 총 코드량은 오히려 소폭 증가 |
| 3. config change | ViewModel 생존으로 값 유지, 컨테이너 인스턴스 동일성 유지 — **동일** | 없음 |

패턴 A′는 **패턴 A와 동작상 동일하고, 다섯 필드를 한 이름으로 묶어 ViewModel 본문의 가독성만
개선한 것**이다. 테스트 특성·복원 특성·config change 특성 중 어느 것도 바뀌지 않는다.

### 07 문서 선택 가이드에 추가할 각주 제안 (07은 직접 수정하지 않음)

07 문서 "패턴 선택 가이드" 표 아래, 또는 첫 행("교차 필드 검증 없음 + 복원 요구 없음") 근처에
다음 각주를 붙이는 것을 제안한다.

> †  **패턴 A′ (컨테이너 변형).** 패턴 A에서 `TextFieldState` 5개를 `@Stable` plain 클래스(=data class
> 아님) 하나로 묶어 ViewModel 본문을 정리한 변형은, 세 실험(교차 검증 테스트 용이성 / 프로세스 종료
> 복원 / config change) 모두에서 패턴 A와 실측치가 동일하다(`08-textfield-container-pattern.md`).
> 순수하게 가독성 개선이므로, 패턴 A를 고른 상황이라면 필드가 많을 때 이 컨테이너 변형을 자유롭게
> 써도 된다. 단 컨테이너는 반드시 재생성/`copy()`를 노출하지 않는 plain 클래스여야 하고
> (data class 금지), ViewModel이 컨테이너를 한 번만 생성해 보유해야 하며, `SavedStateHandle` 연동을
> 컨테이너로 옮기면 코드가 줄지는 않고 위치만 옮겨간다(캡슐화이지 총량 감소가 아님).
