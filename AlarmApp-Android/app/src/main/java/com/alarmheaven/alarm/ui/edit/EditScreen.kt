package com.alarmheaven.alarm.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import com.alarmheaven.alarm.ui.appLanguage
import com.alarmheaven.alarm.ui.components.WheelPicker
import com.alarmheaven.alarm.ui.components.rememberAskNotificationPermission
import com.alarmheaven.alarm.ui.theme.Line
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alarmheaven.alarm.R
import com.alarmheaven.alarm.domain.AlarmDraft
import com.alarmheaven.alarm.domain.MissionType
import com.alarmheaven.alarm.domain.PromptSelection
import com.alarmheaven.alarm.domain.Weekday
import com.alarmheaven.alarm.ui.components.Block
import com.alarmheaven.alarm.ui.components.Chip
import com.alarmheaven.alarm.ui.components.CtaButton
import com.alarmheaven.alarm.ui.components.GhostButton
import com.alarmheaven.alarm.ui.errorMessage
import com.alarmheaven.alarm.ui.mediaName
import com.alarmheaven.alarm.ui.soundName
import com.alarmheaven.alarm.ui.theme.Bg
import com.alarmheaven.alarm.ui.theme.Cta
import com.alarmheaven.alarm.ui.theme.DayOff
import com.alarmheaven.alarm.ui.theme.Sun
import com.alarmheaven.alarm.ui.theme.TextMuted
import com.alarmheaven.alarm.ui.theme.TextPrimary
import com.alarmheaven.alarm.ui.theme.appText
import com.alarmheaven.alarm.ui.weekdayShort

/** 편집·생성: 시간 · 요일 · 타이틀 · 미션 · 미디어 · 소리 · 저장 · 삭제 */
@Composable
fun EditScreen(
    onClose: () -> Unit,
    onPreview: (AlarmDraft) -> Unit,
    viewModel: EditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPromptPicker by remember { mutableStateOf(false) }
    val askNotification = rememberAskNotificationPermission()

    LaunchedEffect(state.closed) { if (state.closed) onClose() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TopBar(
            title = stringResource(if (state.isNew) R.string.edit_title_new else R.string.edit_title_edit),
            showDelete = !state.isNew && state.isLoaded,
            onCancel = onClose,
            onDelete = viewModel::requestDelete,
        )

        if (state.isLoaded) {
            val focus = LocalFocusManager.current
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimeBlock(state.draft, onChange = viewModel::setTime)
                WeekdayBlock(state.draft, state.showValidation && state.weekdayMissing, viewModel::toggleWeekday)
                TitleBlock(
                    title = state.draft.title,
                    showError = state.showValidation && state.titleMissing,
                    onChange = viewModel::setTitle,
                    onDone = { focus.clearFocus() },
                )
                MissionBlock(viewModel, state.draft, onPickPrompts = { showPromptPicker = true })
                Block(stringResource(R.string.edit_media)) {
                    ChipRow {
                        viewModel.catalog.media.forEach { media ->
                            Chip(
                                context.mediaName(media),
                                isSelected = state.draft.mediaId == media.id,
                                onClick = { viewModel.setMedia(media.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Block(stringResource(R.string.edit_sound)) {
                    ChipRow {
                        viewModel.catalog.sounds.forEach { sound ->
                            Chip(
                                context.soundName(sound),
                                isSelected = state.draft.soundId == sound.id,
                                onClick = { viewModel.setSound(sound.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (!state.isNew) {
                    TextButton(onClick = viewModel::requestDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                        Text(stringResource(R.string.edit_delete), style = appText(15, color = Cta))
                    }
                }
            }
            // 하단: 미리보기(흰색) + 저장(빨강). 미리보기는 저장하지 않고 편집 중인 값으로 실행해 본다.
            Row(
                Modifier.fillMaxWidth().background(Bg).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GhostButton(stringResource(R.string.edit_preview), onClick = { onPreview(state.draft) }, modifier = Modifier.weight(1f))
                CtaButton(
                    stringResource(R.string.edit_save),
                    onClick = { askNotification(); viewModel.save() },
                    enabled = !state.isSaving,
                    height = 48.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Box(Modifier.weight(1f))
        }
    }

    if (showPromptPicker) {
        viewModel.prompts?.let { prompts ->
            PromptPickerDialog(
                prompts = prompts,
                selected = state.draft.promptIds,
                onConfirm = {
                    viewModel.setPromptIds(it)
                    showPromptPicker = false
                },
                onDismiss = { showPromptPicker = false },
            )
        }
    }

    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            text = { Text(stringResource(R.string.edit_delete_confirm), style = appText(16, FontWeight.SemiBold)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.edit_delete), color = Cta) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.edit_cancel), color = TextPrimary) }
            },
            containerColor = Color.White,
        )
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            text = { Text(context.errorMessage(error), style = appText(15, FontWeight.SemiBold)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.common_ok), color = Cta) }
            },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun TopBar(title: String, showDelete: Boolean, onCancel: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 6.dp)) {
        RoundTopButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
            Text(stringResource(R.string.edit_cancel), style = appText(13, FontWeight.ExtraBold, Cta))
        }
        Text(title, style = appText(17, FontWeight.ExtraBold), modifier = Modifier.align(Alignment.Center))
        if (showDelete) {
            RoundTopButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.edit_delete), tint = Cta, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** 흰 바탕 둥근 버튼(한국어 "취소"는 원, 영어 "Cancel"은 알약 모양이 된다) */
@Composable
private fun RoundTopButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .widthIn(min = 44.dp)
            .background(Color.White, CircleShape)
            .border(1.dp, Line, CircleShape)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** 시간: 화면 안 휠 3개(오전/오후 · 시 · 분). 한국어는 "오전 7 10", 영어는 "7 10 AM" 순서. */
@Composable
private fun TimeBlock(draft: AlarmDraft, onChange: (hour: Int, minute: Int) -> Unit) {
    val context = LocalContext.current
    val current by rememberUpdatedState(draft)
    val periods = listOf(stringResource(R.string.am), stringResource(R.string.pm))
    val hours = remember { (1..12).map { it.toString() } }
    val minutes = remember { (0..59).map { String.format(Locale.ROOT, "%02d", it) } }
    val periodFirst = context.appLanguage == "ko"

    fun hour24(hour12: Int, pm: Boolean) = hour12 % 12 + if (pm) 12 else 0

    @Composable
    fun RowScope.Period() = WheelPicker(
        periods, if (draft.hour < 12) 0 else 1,
        onSelected = { onChange(hour24((current.hour % 12).let { if (it == 0) 12 else it }, it == 1), current.minute) },
        modifier = Modifier.weight(1f),
    )

    Block(stringResource(R.string.edit_time)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (periodFirst) Period()
            WheelPicker(
                hours, (if (draft.hour % 12 == 0) 12 else draft.hour % 12) - 1,
                onSelected = { onChange(hour24(it + 1, current.hour >= 12), current.minute) },
                modifier = Modifier.weight(1f),
            )
            WheelPicker(
                minutes, draft.minute,
                onSelected = { onChange(current.hour, it) },
                modifier = Modifier.weight(1f),
            )
            if (!periodFirst) Period()
        }
    }
}

@Composable
private fun WeekdayBlock(draft: AlarmDraft, showError: Boolean, onToggle: (Weekday) -> Unit) {
    val context = LocalContext.current
    Block(stringResource(R.string.edit_weekdays)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // 시안 순서: 일 월 화 수 목 금 토
            Weekday.entries.forEach { day ->
                val isOn = day in draft.weekdays
                Box(
                    Modifier
                        .size(40.dp)
                        .background(if (isOn) Sun else DayOff, CircleShape)
                        .toggleable(value = isOn, role = Role.Checkbox, onValueChange = { onToggle(day) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(context.weekdayShort(day), style = appText(13, FontWeight.ExtraBold, if (isOn) TextPrimary else TextMuted))
                }
            }
        }
        if (showError) ValidationText(R.string.edit_weekday_required)
    }
}

@Composable
private fun TitleBlock(title: String, showError: Boolean, onChange: (String) -> Unit, onDone: () -> Unit) {
    Block(stringResource(R.string.edit_alarm_title)) {
        BasicTextField(
            value = title,
            onValueChange = onChange,
            singleLine = true,
            textStyle = appText(17),
            cursorBrush = SolidColor(Cta),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (title.isEmpty()) {
                        Text(stringResource(R.string.edit_title_placeholder), style = appText(17, color = TextMuted.copy(alpha = 0.6f)))
                    }
                    inner()
                }
            },
        )
        if (showError) ValidationText(R.string.edit_title_required)
    }
}

@Composable
private fun MissionBlock(viewModel: EditViewModel, draft: AlarmDraft, onPickPrompts: () -> Unit) {
    val prompts = viewModel.prompts
    Block(stringResource(R.string.edit_mission)) {
        ChipRow {
            MissionType.entries.forEach { mission ->
                Chip(
                    stringResource(if (mission == MissionType.SHAKE) R.string.mission_shake else R.string.mission_transcription),
                    isSelected = draft.mission == mission,
                    onClick = { viewModel.setMission(mission) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // 따라쓰기면 출제할 문장을 고른다
        if (draft.mission == MissionType.TRANSCRIPTION && prompts != null) {
            val count = PromptSelection.resolve(draft.promptIds, prompts).size
            val summary = if (draft.promptIds.isEmpty() || count == prompts.prompts.size) {
                stringResource(R.string.prompt_selected_all)
            } else {
                stringResource(R.string.prompt_selected_count, count)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button, onClick = onPickPrompts),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.edit_prompts), style = appText(15), modifier = Modifier.weight(1f))
                Text(summary, style = appText(15, FontWeight.SemiBold, TextMuted))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
            }
        }
    }
}

@Composable
private fun ValidationText(resId: Int) {
    Text(stringResource(resId), style = appText(12, FontWeight.SemiBold, Cta))
}
