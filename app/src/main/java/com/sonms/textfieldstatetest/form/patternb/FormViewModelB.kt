package com.sonms.textfieldstatetest.form.patternb

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 패턴 B: ViewModel 은 TextFieldState 를 소유하지 않는다.
 * 컴포저블이 snapshotFlow 로 수집한 FormValues 를 onValuesChanged() 로 밀어넣고,
 * ViewModel 은 그 값만 보관하며 관찰한다.
 */
class FormViewModelB : ViewModel() {
    private val _values = MutableStateFlow(FormValues())
    val values = _values.asStateFlow()

    fun onValuesChanged(v: FormValues) {
        _values.value = v
    }
}
