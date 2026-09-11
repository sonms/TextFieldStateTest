# 10. 멀티스크린(bottom nav) ViewModel 스코프 실측

`09-textfield-pattern-final-guide.md` §3-2/§6이 "미실측 외삽"으로 남겨뒀던 항목 중 하나를 실측했다.
질문은 하나다: bottom nav 탭을 전환할 때(`popUpTo(...) { saveState = true }` +
`restoreState = true`), 기본값 `viewModel()`(destination entry 스코프)이 값을 잃는가, 아니면
`navGraphViewModels()`처럼 부모 그래프에 스코프해야만 안전한가.

## 검증 대상

- 새 화면 `NavScopeExperimentApp`(`app/src/main/java/com/sonms/textfieldstatetest/navexperiment/NavScopeExperimentScreen.kt`).
  Navigation-Compose(`androidx.navigation:navigation-compose:2.9.7`)로 bottom nav 탭 2개(Tab A /
  Tab B)를 구성했다. 각 탭은 `navigation(startDestination=..., route=...)`로 감싼 중첩 그래프이고,
  탭 전환은 공식 샘플과 동일한 패턴을 쓴다.

  ```kotlin
  private fun NavController.navigateToTab(route: String) {
      navigate(route) {
          popUpTo(graph.findStartDestination().id) { saveState = true }
          launchSingleTop = true
          restoreState = true
      }
  }
  ```

- Tab A 화면에 기존 패턴 A의 `FormViewModelA`(`SavedStateHandle` 미연동, 순수 `ViewModel`)를 두
  가지 스코프로 동시에 생성해 대조했다.
  - **default**: `viewModel()` 기본값. `LocalViewModelStoreOwner`가 현재 destination의
    `NavBackStackEntry`이므로 그 엔트리에 스코프된다.
  - **graph**: `navGraphViewModels()`를 직접 구현한 `sharedViewModel()` 확장 함수로, 이 destination을
    감싼 nested navigation 그래프(`tabAGraph`)의 엔트리에 스코프된다.
- 측정: `NavScopeExperimentTest`(`app/src/androidTest/java/.../navexperiment/NavScopeExperimentTest.kt`)가
  두 필드에 서로 다른 문자열을 입력하고, Tab B로 이동했다가 Tab A로 되돌아온 뒤 값이 남아있는지
  확인한다. 장비는 SM-G950N(Android 9, API 28), `./gradlew :app:connectedDebugAndroidTest`로 실행.

## (a) 실측 결과

| 스코프 | 탭 전환(B→A) 후 | 근거 |
|---|---|---|
| default (`viewModel()`, destination entry 스코프) | **유지** — 입력값 그대로 | `identityHashCode`가 전환 전후 동일. `LaunchedEffect(Unit)`는 두 번 실행됐으므로 컴포지션 자체는 새로 만들어졌지만, 같은 `ViewModelStore`가 재사용됐다 |
| graph (`navGraphViewModels()` 동등 구현, `tabAGraph` 스코프) | **유지** — 입력값 그대로 | 위와 동일한 근거 |

테스트 통과 로그(`TFNavScope` 태그, logcat):

```
17:11:12.510 tabA composed: default=80241297 graph=75519478 defaultTitle='' graphTitle=''
(텍스트 입력 → Tab B로 이동 → Tab A로 복귀)
17:11:14.897 tabA composed: default=80241297 graph=75519478 defaultTitle='default-value' graphTitle='graph-value'
```

`NavScopeExperimentTest.tabSwitch_comparesDefaultScopeAndGraphScopeViewModel` 통과
(`./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...`,
`BUILD SUCCESSFUL`).

## (b) 예상 밖의 동작

- **기본값 `viewModel()`도 이 패턴에서는 값을 잃지 않는다.** 09 문서 §3-2가 "일반 원칙상 A 계열이
  유리할 것"이라고 추정만 했던 것과 달리, 실제로는 스코프 위치(entry vs graph)가 승부를 가르지
  않았다. `popUpTo(...) { saveState = true }` + `restoreState = true` 조합이 핵심이다. 이 조합은
  단순히 컴포지션의 `rememberSaveable` UI 상태만 저장하는 게 아니라, `NavController`가 팝된
  엔트리의 `ViewModelStore` 자체를 자신의 내부 상태(`NavControllerViewModel`)에 보관했다가
  `restoreState = true`로 돌아올 때 같은 엔트리(와 그 `ViewModelStore`)를 복원한다. 그래서 destination
  entry에 스코프된 `viewModel()`이라도 살아남는다.
- 첫 실행은 코드 문제가 아니라 **디바이스가 화면 잠금 상태**라서 실패했다. `ActivityScenario`가
  액티비티를 RESUMED로 올린 직후 26ms 만에 PAUSED/STOPPED로 떨어졌는데, 원인은 화면 타임아웃으로
  잠금 화면이 위로 덮인 것이었다(`adb shell screencap`으로 잠금 화면을 직접 확인). RK3566에서 겪은
  "다른 앱이 포그라운드를 가로챔"과 같은 종류의 디바이스 플레이키니스가 SM-G950N에도 있다. 해결:
  `adb shell wm dismiss-keyguard` + `adb shell settings put system screen_off_timeout 1800000`로
  테스트 전에 잠금을 풀고 타임아웃을 늘린다.

## (c) 결론과 09 문서 갱신

09 문서 §3-2의 "폼이 여러 화면·탭에 걸쳐 있고 상태를 화면 밖에서 오래 들고 있어야 함" 행은 더 이상
순수 추정이 아니다. 다만 결론은 아래처럼 범위를 명확히 좁혀서 적어야 정확하다.

> 표준 Nav Compose 백스택 + `popUpTo(saveState=true)`/`restoreState=true` 조합에서는 ViewModel
> 스코프(기본값/그래프 스코프 모두)가 탭 전환 시 파괴되지 않고 보존되므로, **이 시나리오에 한해**
> 패턴 A 계열과 패턴 B 사이에 실측상 차이가 없다(`identityHashCode` 동일로 확인). 단, 탭이
> 백그라운드에 있는 동안 프로세스가 종료되는 경우는 실험 2의 결론(A 계열은 `SavedStateHandle`
> 없이는 소실)이 그대로 적용되며, 별도 `NavHostController`를 쓰는 아키텍처는 검증 범위 밖이다.

"이 시나리오에 한해"가 핵심이다. 아래에서 그 경계를 구체적으로 나눈다.

### 실험 2의 결론이 그대로 적용되는 것 (재실험 불필요)

- **탭이 백그라운드(detach)로 밀려나 있는 동안 프로세스가 종료되는 경우.** 이번 실험이 확인한 건
  "`NavController` 자신이 살아있는 동안" `ViewModelStore`가 보존된다는 것뿐이다. `NavController`와
  그 안의 모든 `ViewModelStore`는 순수 in-memory 객체라 프로세스가 죽으면 통째로 사라진다.
  `rememberNavController()`의 `rememberSaveable`은 백스택의 **경로(route 문자열)**만 복원 대상이고
  `ViewModelStore`는 복원 대상이 아니다. 따라서 이 경우 `SavedStateHandle` 없는 패턴 A/A′는 실험
  2에서 이미 확인한 대로 소실되고, 패턴 B는 `rememberTextFieldState()`의 `rememberSaveable`이
  독립적으로 복원한다. 이건 새로 잴 필요가 없는, 이미 알려진 메커니즘의 직접적인 귀결이다.

### 이번 실측이 답하지 못한 것 (진짜 미실측)

- **`saveState`를 안 쓰는 완전한 pop** (예: 위저드 플로우에서 이전 단계로 영구히 돌아가지 않는 경우).
  이 경우 `ViewModelStore.clear()`가 실제로 호출되어 값을 잃을 것으로 보이나, 이번 실험은 그 경로를
  다루지 않았다.
- **탭마다 완전히 별도의 `NavHost`/`NavController`를 두는 아키텍처.** 이번 실험은 `NavHostController`
  하나로 중첩 그래프를 구성하는 표준 패턴만 확인했다. 탭별로 `NavController`가 아예 다르면
  `NavControllerViewModel`이 `ViewModelStore`를 보존하는 메커니즘 자체가 그 구조에 적용되지
  않을 수 있고, 이건 Fragment 여부와 무관하게 순수 Compose 안에서도 실무에 흔한 변형이다.
- **`FormViewModelASaved`(SavedStateHandle 연동)와의 조합**. 이번 실험은 `SavedStateHandle`이 없는
  `FormViewModelA`만 썼다. `SavedStateHandle` 연동 ViewModel이 같은 탭 전환에서 어떻게 동작하는지는
  확인하지 않았다(다만 `ViewModelStore` 자체가 보존된다는 이번 결과로 미루어보면 동일하게 유지될
  가능성이 높다).
- **Fragment 기반 Navigation**은 이번 저장소 스코프에서 의도적으로 제외했다(README 스택이 순수
  Compose 기반이라 Fragment 하이브리드는 무관한 변수가 너무 많이 섞인다).

---

## 참고

- 실험 코드: `app/src/main/java/com/sonms/textfieldstatetest/navexperiment/NavScopeExperimentScreen.kt`
- 실험 테스트: `app/src/androidTest/java/com/sonms/textfieldstatetest/navexperiment/NavScopeExperimentTest.kt`
- 종합 가이드: `09-textfield-pattern-final-guide.md`
