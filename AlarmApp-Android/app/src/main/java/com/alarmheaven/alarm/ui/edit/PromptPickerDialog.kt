package com.alarmheaven.alarm.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alarmheaven.alarm.R
import com.alarmheaven.alarm.domain.PromptCategory
import com.alarmheaven.alarm.domain.PromptSelection
import com.alarmheaven.alarm.domain.TranscriptionPrompts
import com.alarmheaven.alarm.ui.appLanguage
import com.alarmheaven.alarm.ui.theme.Bg
import com.alarmheaven.alarm.ui.theme.ChipOff
import com.alarmheaven.alarm.ui.theme.ChipOn
import com.alarmheaven.alarm.ui.theme.Cta
import com.alarmheaven.alarm.ui.theme.Line
import com.alarmheaven.alarm.ui.theme.Sun
import com.alarmheaven.alarm.ui.theme.SunDeep
import com.alarmheaven.alarm.ui.theme.TextMuted
import com.alarmheaven.alarm.ui.theme.TextPrimary
import com.alarmheaven.alarm.ui.theme.appText

/**
 * 미션 문장 고르기: 카테고리 탭 + 탭별 전체 선택 + 문장별 체크. 하나도 안 고르면 확인 불가.
 * 결과는 [PromptSelection.normalize]로 저장값(전부면 빈 리스트 = 전체)으로 바꿔 돌려준다.
 */
@Composable
fun PromptPickerDialog(
    prompts: TranscriptionPrompts,
    selected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            PromptPickerContent(prompts, selected, onConfirm, onDismiss)
        }
    }
}

@Composable
private fun PromptPickerContent(
    prompts: TranscriptionPrompts,
    selected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onCancel: () -> Unit,
) {
    val language = LocalContext.current.appLanguage
    var checked by remember { mutableStateOf(PromptSelection.checkedIds(selected, prompts)) }
    var categoryId by remember { mutableStateOf(prompts.categories.firstOrNull()?.id.orEmpty()) }
    val category = prompts.categories.firstOrNull { it.id == categoryId }
    val visible = category?.let { prompts.prompts(it) }.orEmpty()

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                Text(stringResource(R.string.edit_cancel), style = appText(15, color = TextPrimary))
            }
            Text(
                stringResource(R.string.prompt_picker_title),
                style = appText(17, FontWeight.ExtraBold),
                modifier = Modifier.align(Alignment.Center),
            )
            TextButton(
                onClick = { onConfirm(PromptSelection.normalize(checked, prompts)) },
                enabled = checked.isNotEmpty(), // 출제할 문장이 하나는 있어야 한다
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(
                    stringResource(R.string.common_ok),
                    style = appText(15, color = if (checked.isNotEmpty()) Cta else TextMuted.copy(alpha = 0.5f)),
                )
            }
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            prompts.categories.forEach { item ->
                CategoryTab(
                    item = item,
                    name = item.name(language),
                    count = prompts.prompts(item).count { it.id in checked },
                    isSelected = item.id == categoryId,
                    onClick = { categoryId = item.id },
                )
            }
        }
        HorizontalDivider(color = Line)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val ids = visible.map { it.id }
            val allOn = ids.isNotEmpty() && ids.all { it in checked }
            CheckRow(stringResource(R.string.prompt_select_all), isOn = allOn, isHeader = true) {
                checked = if (allOn) checked - ids.toSet() else checked + ids
            }
            visible.forEach { prompt ->
                CheckRow(prompt.text(language), isOn = prompt.id in checked) {
                    checked = if (prompt.id in checked) checked - prompt.id else checked + prompt.id
                }
            }
        }
    }
}

@Composable
private fun CategoryTab(item: PromptCategory, name: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.heightIn(min = 48.dp).clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp)) {
            Text(
                name,
                style = appText(16, if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold, if (isSelected) TextPrimary else TextMuted),
            )
            if (count > 0) {
                Text(
                    count.toString(),
                    style = appText(13, FontWeight.ExtraBold),
                    modifier = Modifier
                        .background(if (isSelected) ChipOn else ChipOff, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Box(
            Modifier
                .padding(top = 10.dp)
                .height(3.dp)
                .fillMaxWidth()
                .background(if (isSelected) Sun else Color.Transparent, CircleShape),
        )
    }
}

@Composable
private fun CheckRow(title: String, isOn: Boolean, isHeader: Boolean = false, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isHeader) Color.Transparent else if (isOn) ChipOn else Color.White, shape)
            .border(1.dp, if (isHeader) Color.Transparent else Line, shape)
            .toggleable(value = isOn, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Checkbox(
            checked = isOn,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = SunDeep, checkmarkColor = Color.White, uncheckedColor = TextMuted.copy(alpha = 0.5f)),
        )
        Text(title, style = appText(17, if (isHeader) FontWeight.ExtraBold else FontWeight.Bold))
    }
}
