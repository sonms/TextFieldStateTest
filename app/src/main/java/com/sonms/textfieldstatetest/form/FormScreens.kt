package com.sonms.textfieldstatetest.form

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonms.textfieldstatetest.form.patterna.FormViewModelA
import com.sonms.textfieldstatetest.form.patterna.FormViewModelASaved
import com.sonms.textfieldstatetest.form.patternb.FormValues
import com.sonms.textfieldstatetest.form.patternb.FormViewModelB
import com.sonms.textfieldstatetest.form.patternb.rememberFormStateHolderB
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

private val FIELD_LABELS = listOf("제목", "설명", "가격", "시작일(yyyy-MM-dd)", "종료일(yyyy-MM-dd)")

@Composable
private fun Field(tag: String, label: String, state: TextFieldState) {
    OutlinedTextField(
        state = state,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

@Composable
private fun SectionHeader(text: String) {
    HorizontalDivider(Modifier.padding(top = 16.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun ErrorLine(tag: String, error: Boolean) {
    Text(
        text = if (error) "종료일이 시작일보다 빠릅니다" else "날짜 범위 정상",
        color = if (error) Color.Red else Color.Gray,
        modifier = Modifier.testTag(tag),
    )
}

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AllPatternsScreen(modifier: Modifier = Modifier) {
    val vmA: FormViewModelA = viewModel()
    val vmASaved: FormViewModelASaved = viewModel()
    val vmB: FormViewModelB = viewModel()
    val holderB = rememberFormStateHolderB()

    // 패턴 B: 컴포저블이 값 스냅샷을 관찰해 ViewModel 로 전달한다.
    LaunchedEffect(holderB) {
        snapshotFlow { holderB.snapshot() }.collect { vmB.onValuesChanged(it) }
    }
    val valuesB: FormValues by vmB.values.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader("패턴 A — ViewModel 프로퍼티, SavedStateHandle 미연동")
        val aStates = listOf(vmA.title, vmA.description, vmA.price, vmA.startDate, vmA.endDate)
        FIELD_LABELS.forEachIndexed { i, l -> Field("a_$i", l, aStates[i]) }
        ErrorLine("a_error", vmA.dateRangeError)

        SectionHeader("패턴 A+Saved — ViewModel 프로퍼티, SavedStateHandle 직접 연동")
        val sStates = listOf(vmASaved.title, vmASaved.description, vmASaved.price, vmASaved.startDate, vmASaved.endDate)
        FIELD_LABELS.forEachIndexed { i, l -> Field("s_$i", l, sStates[i]) }
        ErrorLine("s_error", vmASaved.dateRangeError)

        SectionHeader("패턴 B — 화면 레벨 상태 홀더, ViewModel 은 snapshotFlow 관찰")
        val bStates = listOf(holderB.title, holderB.description, holderB.price, holderB.startDate, holderB.endDate)
        FIELD_LABELS.forEachIndexed { i, l -> Field("b_$i", l, bStates[i]) }
        ErrorLine("b_error", valuesB.dateRangeError)
    }
}

fun logConfigChange(recreated: Boolean) {
    // 실험 3: savedInstanceState 가 null 이 아니면 시스템이 Activity 를 재생성한 것이다.
    Log.i("TFPattern", if (recreated) "Activity 재생성됨 (config change 등)" else "Activity 최초 생성")
}
