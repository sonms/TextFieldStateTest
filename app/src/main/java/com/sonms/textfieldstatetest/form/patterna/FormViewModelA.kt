package com.sonms.textfieldstatetest.form.patterna

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import com.sonms.textfieldstatetest.form.endBeforeStart

/**
 * 패턴 A: ViewModel이 TextFieldState 5개를 직접 프로퍼티로 보유한다.
 * 교차 필드 검증은 두 TextFieldState의 현재 텍스트를 직접 읽어서 계산한다.
 * SavedStateHandle을 연동하지 않았기 때문에 프로세스가 종료되면 입력값이 사라진다.
 */
class FormViewModelA : ViewModel() {
    val title = TextFieldState()
    val description = TextFieldState()
    val price = TextFieldState()
    val startDate = TextFieldState()
    val endDate = TextFieldState()

    val dateRangeError: Boolean
        get() = endBeforeStart(startDate.text.toString(), endDate.text.toString())
}
