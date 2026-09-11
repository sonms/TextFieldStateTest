package com.sonms.textfieldstatetest.navexperiment

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * 실험: bottom nav 탭을 saveState/restoreState로 전환할 때, viewModel() 기본값(destination
 * entry 스코프)과 navGraphViewModels() 동등 구현(부모 그래프 스코프)이 값을 유지하는지 대조한다.
 */
class NavScopeExperimentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tabSwitch_comparesDefaultScopeAndGraphScopeViewModel() {
        composeTestRule.setContent { NavScopeExperimentApp() }

        composeTestRule.onNodeWithTag("navDefaultTitle").performTextInput("default-value")
        composeTestRule.onNodeWithTag("navGraphTitle").performTextInput("graph-value")

        composeTestRule.onNodeWithTag("navTabB").performClick()
        composeTestRule.onNodeWithTag("navTabA").performClick()

        composeTestRule.onNodeWithTag("navDefaultTitle").assertTextContains("default-value")
        composeTestRule.onNodeWithTag("navGraphTitle").assertTextContains("graph-value")
    }
}
