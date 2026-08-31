package com.sonms.textfieldstatetest.form.patterna

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonms.textfieldstatetest.form.endBeforeStart
import kotlinx.coroutines.launch

/**
 * 패턴 A + SavedStateHandle 직접 연동.
 * FormViewModelA 와 비교했을 때 추가된 코드는 아래 세 가지이다.
 *  1. 생성자에서 SavedStateHandle 을 주입받는다.
 *  2. 각 필드를 field() 헬퍼로 만들어, 저장된 문자열로 초기화한다.
 *  3. field() 안에서 snapshotFlow 로 텍스트 변경을 관찰해 SavedStateHandle 에 다시 기록한다.
 */
class FormViewModelASaved(private val handle: SavedStateHandle) : ViewModel() {
    val title = handle.restoringField("title")
    val description = handle.restoringField("description")
    val price = handle.restoringField("price")
    val startDate = handle.restoringField("startDate")
    val endDate = handle.restoringField("endDate")

    val dateRangeError: Boolean
        get() = endBeforeStart(startDate.text.toString(), endDate.text.toString())

    private fun SavedStateHandle.restoringField(key: String): TextFieldState {
        val state = TextFieldState(get<String>(key).orEmpty())
        viewModelScope.launch {
            snapshotFlow { state.text.toString() }.collect { set(key, it) }
        }
        return state
    }
}
