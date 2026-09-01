package com.sonms.textfieldstatetest.form.patterna2

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import com.sonms.textfieldstatetest.form.endBeforeStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 패턴 A': ViewModel 에 낱개 프로퍼티로 두던 TextFieldState 5개를 plain 클래스 하나로 묶었다.
 *
 * data class 가 아니다. 이것이 이번 실험의 핵심 전제 조건이다.
 *  - data class 로 만들면 copy() 가 생기고, copy() 는 각 TextFieldState 를 그대로 복사하되
 *    새 컨테이너 인스턴스를 만든다. 어느 쪽이든 "값을 보관하는 그릇은 참조 동일성을 유지해야
 *    한다"는 원칙과 충돌하므로, 컨테이너는 재생성/copy 를 애초에 노출하지 않는 plain 클래스여야 한다.
 *
 * ViewModel 은 이 컨테이너를 딱 한 번 생성해서 보유하고, 절대 copy() 하거나 재대입하지 않는다.
 */
@Stable
class CourseFormFields {
    val title = TextFieldState()
    val description = TextFieldState()
    val price = TextFieldState()
    val startDate = TextFieldState()
    val endDate = TextFieldState()

    val dateRangeError: Boolean
        get() = endBeforeStart(startDate.text.toString(), endDate.text.toString())
}

/**
 * 패턴 A'+Saved: 기존 FormViewModelASaved 의 restoringField 헬퍼를 이 컨테이너 안으로 옮겼다.
 * ViewModel 쪽에는 컨테이너 생성 1줄만 남고, SavedStateHandle 저장/복원 배선은 전부 여기에 격리된다.
 * 코루틴 스코프는 ViewModel 의 viewModelScope 를 그대로 주입받아 사용한다.
 */
@Stable
class SavedCourseFormFields(
    private val handle: SavedStateHandle,
    private val scope: CoroutineScope,
) {
    val title = restoringField("title")
    val description = restoringField("description")
    val price = restoringField("price")
    val startDate = restoringField("startDate")
    val endDate = restoringField("endDate")

    val dateRangeError: Boolean
        get() = endBeforeStart(startDate.text.toString(), endDate.text.toString())

    private fun restoringField(key: String): TextFieldState {
        val state = TextFieldState(handle.get<String>(key).orEmpty())
        scope.launch {
            snapshotFlow { state.text.toString() }.collect { handle[key] = it }
        }
        return state
    }
}
