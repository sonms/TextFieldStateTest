package com.sonms.textfieldstatetest.form.patternb

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.sonms.textfieldstatetest.form.endBeforeStart

/**
 * 패턴 B: 화면 레벨 @Stable 상태 홀더.
 * TextFieldState 는 rememberTextFieldState() 로 컴포저블 스코프에서 생성되며,
 * ViewModel 은 이 홀더를 직접 참조하지 않고 값만 snapshotFlow 로 관찰한다.
 */
@Stable
class FormStateHolderB(
    val title: TextFieldState,
    val description: TextFieldState,
    val price: TextFieldState,
    val startDate: TextFieldState,
    val endDate: TextFieldState,
) {
    fun snapshot(): FormValues = FormValues(
        title = title.text.toString(),
        description = description.text.toString(),
        price = price.text.toString(),
        startDate = startDate.text.toString(),
        endDate = endDate.text.toString(),
    )
}

@Composable
fun rememberFormStateHolderB(): FormStateHolderB {
    val title = rememberTextFieldState()
    val description = rememberTextFieldState()
    val price = rememberTextFieldState()
    val startDate = rememberTextFieldState()
    val endDate = rememberTextFieldState()
    return remember { FormStateHolderB(title, description, price, startDate, endDate) }
}

/**
 * 폼의 값 스냅샷. ViewModel 은 이 형태만 관찰하므로 검증 로직도 이 값 객체에 대한
 * 순수 함수로 작성된다. 단위 테스트에서 TextFieldState 나 ViewModel 을 만들 필요가 없다.
 */
data class FormValues(
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val startDate: String = "",
    val endDate: String = "",
) {
    val dateRangeError: Boolean
        get() = endBeforeStart(startDate, endDate)
}
