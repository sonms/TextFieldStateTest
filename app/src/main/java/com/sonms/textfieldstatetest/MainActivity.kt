package com.sonms.textfieldstatetest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonms.textfieldstatetest.form.AllPatternsScreen
import com.sonms.textfieldstatetest.form.logConfigChange
import com.sonms.textfieldstatetest.ui.theme.TextFieldStateTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logConfigChange(recreated = savedInstanceState != null)
        enableEdgeToEdge()
        setContent {
            TextFieldStateTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AllPatternsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
