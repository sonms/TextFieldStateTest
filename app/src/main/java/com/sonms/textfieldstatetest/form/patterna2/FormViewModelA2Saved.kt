package com.sonms.textfieldstatetest.form.patterna2

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * 패턴 A'+Saved: ViewModel 에는 컨테이너 생성 1줄만 남는다.
 *
 * 기존 FormViewModelASaved 는 restoringField 헬퍼(약 8줄) + import 4개 + 필드당 1줄을
 * ViewModel 본문에 두었다. 이 버전은 그 배선을 전부 SavedCourseFormFields 안으로 옮겨,
 * ViewModel 이 알아야 할 것은 "handle 과 viewModelScope 를 컨테이너에 넘긴다"뿐이다.
 */
class FormViewModelA2Saved(handle: SavedStateHandle) : ViewModel() {
    val fields = SavedCourseFormFields(handle, viewModelScope)

    val dateRangeError: Boolean get() = fields.dateRangeError
}
