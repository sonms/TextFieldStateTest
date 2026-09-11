package com.sonms.textfieldstatetest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.sonms.textfieldstatetest.form.AllPatternsScreen
import com.sonms.textfieldstatetest.form.logConfigChange
import com.sonms.textfieldstatetest.navexperiment.NavScopeExperimentApp
import com.sonms.textfieldstatetest.ui.theme.TextFieldStateTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logConfigChange(recreated = savedInstanceState != null)
        enableEdgeToEdge()
        setContent {
            TextFieldStateTestTheme {
                var showNavExperiment by remember { mutableStateOf(false) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    // 5패턴 폼 화면과 nav 스코프 실험 화면을 오가는 전환 버튼.
                    floatingActionButton = {
                        Button(
                            onClick = { showNavExperiment = !showNavExperiment },
                            modifier = Modifier.testTag("toggleNavExperiment"),
                        ) {
                            Text(if (showNavExperiment) "5패턴 폼으로" else "Nav 스코프 실험으로")
                        }
                    },
                ) { innerPadding ->
                    if (showNavExperiment) {
                        NavScopeExperimentApp()
                    } else {
                        AllPatternsScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}
