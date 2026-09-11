package com.sonms.textfieldstatetest.navexperiment

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.OutlinedTextField
import com.sonms.textfieldstatetest.form.patterna.FormViewModelA

/**
 * 실험: bottom nav 탭 전환(saveState/restoreState) 시 ViewModel이 어느 스코프에 묶여 있는지에 따라
 * 값이 유지되는지 확인한다. 같은 화면에 두 ViewModel을 나란히 두어 대조한다.
 * - default: viewModel() 기본값 — destination(탭 자신의) 엔트리 스코프
 * - graph:   navGraphViewModels() 동등 구현 — 탭을 감싼 nested navigation 그래프 스코프
 */
private const val ROUTE_TAB_A_GRAPH = "tabAGraph"
private const val ROUTE_TAB_A = "tabAScreen"
private const val ROUTE_TAB_B_GRAPH = "tabBGraph"
private const val ROUTE_TAB_B = "tabBScreen"

@Composable
fun NavScopeExperimentApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == ROUTE_TAB_A } == true,
                    onClick = { navController.navigateToTab(ROUTE_TAB_A_GRAPH) },
                    icon = {},
                    label = { Text("Tab A") },
                    modifier = Modifier.testTag("navTabA"),
                )
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == ROUTE_TAB_B } == true,
                    onClick = { navController.navigateToTab(ROUTE_TAB_B_GRAPH) },
                    icon = {},
                    label = { Text("Tab B") },
                    modifier = Modifier.testTag("navTabB"),
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_TAB_A_GRAPH,
            modifier = Modifier.padding(padding),
        ) {
            navigation(startDestination = ROUTE_TAB_A, route = ROUTE_TAB_A_GRAPH) {
                composable(ROUTE_TAB_A) { entry ->
                    ScopeTestScreen(tag = "tabA", entry = entry, navController = navController)
                }
            }
            navigation(startDestination = ROUTE_TAB_B, route = ROUTE_TAB_B_GRAPH) {
                composable(ROUTE_TAB_B) {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Tab B — Tab A를 백스택에서 saveState로 밀어내는 용도")
                    }
                }
            }
        }
    }
}

private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ScopeTestScreen(tag: String, entry: NavBackStackEntry, navController: NavController) {
    val vmDefault: FormViewModelA = viewModel()
    val vmGraphScoped: FormViewModelA = entry.sharedViewModel(navController)

    LaunchedEffect(Unit) {
        Log.i(
            "TFNavScope",
            "$tag composed: default=${System.identityHashCode(vmDefault)} " +
                "graph=${System.identityHashCode(vmGraphScoped)} " +
                "defaultTitle='${vmDefault.title.text}' graphTitle='${vmGraphScoped.title.text}'",
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("기본 viewModel() — destination entry 스코프 (탭이 saveState로 빠지면 파괴될 수 있음)", style = MaterialTheme.typography.bodySmall)
        TitleField("navDefaultTitle", vmDefault.title)

        Text("navGraphViewModels() 동등 — 부모 nav 그래프 스코프", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
        TitleField("navGraphTitle", vmGraphScoped.title)
    }
}

@Composable
private fun TitleField(tag: String, state: TextFieldState) {
    OutlinedTextField(
        state = state,
        label = { Text("제목") },
        modifier = Modifier.testTag(tag),
    )
}

/** navGraphViewModels()의 최소 동등 구현: 이 destination을 감싼 nav 그래프에 스코프된 ViewModel을 얻는다. */
@Composable
private inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(navController: NavController): T {
    val parentRoute = destination.parent?.route ?: return viewModel()
    val parentEntry = remember(this) { navController.getBackStackEntry(parentRoute) }
    return viewModel(parentEntry)
}
