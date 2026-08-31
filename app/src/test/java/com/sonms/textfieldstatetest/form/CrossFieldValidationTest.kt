package com.sonms.textfieldstatetest.form

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.sonms.textfieldstatetest.form.patterna.FormViewModelA
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
