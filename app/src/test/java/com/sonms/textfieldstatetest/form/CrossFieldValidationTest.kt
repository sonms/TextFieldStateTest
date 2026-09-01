package com.sonms.textfieldstatetest.form

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.sonms.textfieldstatetest.form.patterna.FormViewModelA
import com.sonms.textfieldstatetest.form.patterna2.CourseFormFields
import com.sonms.textfieldstatetest.form.patterna2.FormViewModelA2
import com.sonms.textfieldstatetest.form.patternb.FormValues
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 실험 1: 교차 필드 검증("종료일 < 시작일 이면 오류") 단위 테스트.
 * 두 패턴 모두 순수 JVM 테스트로 끝나는지, Arrange 코드가 몇 줄인지 비교한다.
 */
class CrossFieldValidationTest {

    // ---- 패턴 A: ViewModel + TextFieldState 를 직접 세팅해야 한다 ----
    @Test
    fun patternA_endBeforeStart_isError() {
        // Arrange (3줄): ViewModel 생성 + TextFieldState 2개에 값 주입
        val vm = FormViewModelA()
        vm.startDate.setTextAndPlaceCursorAtEnd("2026-05-10")
        vm.endDate.setTextAndPlaceCursorAtEnd("2026-05-01")

        assertTrue(vm.dateRangeError)
    }

    @Test
    fun patternA_normalRange_isNotError() {
        val vm = FormViewModelA()
        vm.startDate.setTextAndPlaceCursorAtEnd("2026-05-01")
        vm.endDate.setTextAndPlaceCursorAtEnd("2026-05-10")

        assertFalse(vm.dateRangeError)
    }

    // ---- 패턴 A': 컨테이너를 거쳐 필드에 접근한다 ----
    @Test
    fun patternA2_endBeforeStart_isError() {
        // Arrange (3줄): ViewModel 생성 + 컨테이너 경유 TextFieldState 2개에 값 주입
        val vm = FormViewModelA2()
        vm.fields.startDate.setTextAndPlaceCursorAtEnd("2026-05-10")
        vm.fields.endDate.setTextAndPlaceCursorAtEnd("2026-05-01")

        assertTrue(vm.dateRangeError)
    }

    @Test
    fun patternA2_normalRange_isNotError() {
        val vm = FormViewModelA2()
        vm.fields.startDate.setTextAndPlaceCursorAtEnd("2026-05-01")
        vm.fields.endDate.setTextAndPlaceCursorAtEnd("2026-05-10")

        assertFalse(vm.dateRangeError)
    }

    @Test
    fun patternA2_container_isTestableWithoutViewModel() {
        // 컨테이너가 독립 객체이므로 ViewModel 없이도 검증할 수 있다. 그래도 Arrange 는 여전히 3줄이다
        // (컨테이너 생성자가 값을 받지 않아, 필드마다 setTextAndPlaceCursorAtEnd 를 호출해야 하기 때문).
        val fields = CourseFormFields()
        fields.startDate.setTextAndPlaceCursorAtEnd("2026-05-10")
        fields.endDate.setTextAndPlaceCursorAtEnd("2026-05-01")

        assertTrue(fields.dateRangeError)
    }

    // ---- 패턴 B: 값 객체에 대한 순수 함수 ----
    @Test
    fun patternB_endBeforeStart_isError() {
        // Arrange (1줄): 값 스냅샷만 만든다
        val values = FormValues(startDate = "2026-05-10", endDate = "2026-05-01")

        assertTrue(values.dateRangeError)
    }

    @Test
    fun patternB_normalRange_isNotError() {
        val values = FormValues(startDate = "2026-05-01", endDate = "2026-05-10")

        assertFalse(values.dateRangeError)
    }
}
