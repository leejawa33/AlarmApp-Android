package com.alarmheaven.alarm.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alarmheaven.alarm.R
import com.alarmheaven.alarm.ui.alarmTime
import com.alarmheaven.alarm.ui.components.CtaButton
import com.alarmheaven.alarm.ui.components.Wordmark
import com.alarmheaven.alarm.ui.errorMessage
import com.alarmheaven.alarm.ui.remainingText
import com.alarmheaven.alarm.ui.theme.Bg
import com.alarmheaven.alarm.ui.theme.Cta
import com.alarmheaven.alarm.ui.theme.Line
import com.alarmheaven.alarm.ui.theme.Sun
import com.alarmheaven.alarm.ui.theme.SwitchOff
import com.alarmheaven.alarm.ui.theme.TextMuted
import com.alarmheaven.alarm.ui.theme.TextPrimary
import com.alarmheaven.alarm.ui.theme.appText
import com.alarmheaven.alarm.ui.weekdaySummary

/** 메인: 워드마크 · 알람 리스트 · 알람 추가 CTA · 빈 상태 */
@Composable
fun MainScreen(
    onAdd: () -> Unit,
    onEdit: (alarmId: String) -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            // 시안의 상단 햇살 그라데이션
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Sun.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width / 2, 0f),
                        radius = 360.dp.toPx(),
                    ),
                    size = androidx.compose.ui.geometry.Size(size.width, 360.dp.toPx()),
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            Header()
            when {
                !state.isLoaded -> Unit
                state.items.isEmpty() -> EmptyState(Modifier.weight(1f))
                else -> AlarmList(
                    items = state.items,
                    onClick = { onEdit(it.alarm.id) },
                    onToggle = { item, enabled -> viewModel.setEnabled(item.alarm, enabled) },
                    onDelete = { viewModel.delete(it.alarm) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AddButton(
            isAtLimit = state.isAtLimit,
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomCenter),
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
private fun Header() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(stringResource(R.string.app_display_name))
        // 설정(톱니바퀴)은 설정 화면 단계에서 추가한다.
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⏰", style = appText(56))
        Text(
            stringResource(R.string.main_empty_title),
            style = appText(22, FontWeight.ExtraBold),
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            stringResource(R.string.main_empty_body),
            style = appText(15, FontWeight.SemiBold, TextMuted),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp),
        )
        Box(Modifier.padding(bottom = 96.dp))
    }
}

@Composable
private fun AlarmList(
    items: List<AlarmItem>,
    onClick: (AlarmItem) -> Unit,
    onToggle: (AlarmItem, Boolean) -> Unit,
    onDelete: (AlarmItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = { it.alarm.id }) { item ->
            SwipeToDeleteRow(onDelete = { onDelete(item) }, modifier = Modifier.animateItem()) {
                AlarmRow(item, onClick = { onClick(item) }, onToggle = { onToggle(item, it) }, onDelete = { onDelete(item) })
            }
        }
    }
}

/** 왼쪽으로 밀어서 삭제 (iOS의 swipeActions) */
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    // 끝까지 밀리면 삭제한다. 목록에서 사라지면 이 항목의 상태도 함께 정리된다.
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Cta, RoundedCornerShape(16.dp))
                    .padding(end = 22.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.edit_delete), tint = Color.White)
            }
        },
        content = { content() },
    )
}

/** 리스트 카드: 시각 · 타이틀 · 요일 · 다음 울림까지 / 꺼짐 · 토글. 탭 = 편집, 길게 누르기 = 삭제 메뉴. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmRow(
    item: AlarmItem,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val alarm = item.alarm
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    context.alarmTime(alarm.hour, alarm.minute),
                    style = appText(28, FontWeight.ExtraBold, if (alarm.isEnabled) TextPrimary else TextMuted),
                )
                Text(alarm.title, style = appText(14), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(metaText(item), style = appText(12, FontWeight.SemiBold, TextMuted))
            }
            val toggleLabel = stringResource(R.string.cd_toggle_alarm)
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Sun,
                    checkedBorderColor = Sun,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = SwitchOff,
                    uncheckedBorderColor = SwitchOff,
                ),
                modifier = Modifier.semantics { contentDescription = toggleLabel },
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = Color.White) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_delete), style = appText(15, color = Cta)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Cta) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

/** "월 화 수 목 금 · 8시간 24분 후" / "매일 · 꺼짐". 다음 울림이 없으면 요일만. */
@Composable
private fun metaText(item: AlarmItem): String {
    val context = LocalContext.current
    val days = context.weekdaySummary(item.alarm.weekdays)
    return when {
        !item.alarm.isEnabled -> "$days · ${stringResource(R.string.alarm_off)}"
        item.remaining == null -> days
        else -> "$days · ${context.remainingText(item.remaining)}"
    }
}

@Composable
private fun AddButton(isAtLimit: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isAtLimit) {
            Text(stringResource(R.string.main_max_alarms), style = appText(13, FontWeight.SemiBold, TextMuted))
        }
        CtaButton(
            text = "+  " + stringResource(R.string.main_add_alarm),
            onClick = onClick,
            enabled = !isAtLimit,
        )
    }
}
