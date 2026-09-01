package com.sonms.textfieldstatetest.form.patterna2

import androidx.lifecycle.ViewModel

/**
 * 패턴 A': 패턴 A 와 동일하게 ViewModel 이 TextFieldState 를 보유하되,
 * 낱개 프로퍼티 대신 CourseFormFields 컨테이너 하나로 묶어 가독성만 개선했다.
 *
 * 컨테이너는 생성자에서 한 번만 만들고, copy() 하거나 재대입하지 않는다.
 * 테스트 용이성 / 프로세스 복원 / config change 특성이 패턴 A 와 같은지가 실험 08 의 검증 대상이다.
 */
class FormViewModelA2 : ViewModel() {
    val fields = CourseFormFields()

    val dateRangeError: Boolean get() = fields.dateRangeError
}
