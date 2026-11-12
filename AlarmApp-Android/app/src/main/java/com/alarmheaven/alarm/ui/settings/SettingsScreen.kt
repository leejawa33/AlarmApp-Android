package com.alarmheaven.alarm.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alarmheaven.alarm.R
import com.alarmheaven.alarm.domain.AppLanguage
import com.alarmheaven.alarm.platform.openAlarmPermissionSettings
import com.alarmheaven.alarm.service.AlarmPermissionStatus
import com.alarmheaven.alarm.ui.components.Block
import com.alarmheaven.alarm.ui.components.GhostButton
import com.alarmheaven.alarm.ui.components.RoundButton
import com.alarmheaven.alarm.ui.theme.Bg
import com.alarmheaven.alarm.ui.theme.Cta
import com.alarmheaven.alarm.ui.theme.SunDeep
import com.alarmheaven.alarm.ui.theme.TextMuted
import com.alarmheaven.alarm.ui.theme.appText

fun AppLanguage.nameRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.KOREAN -> R.string.settings_language_ko
    AppLanguage.ENGLISH -> R.string.settings_language_en
}

/** 설정: 언어(한 줄 → 언어 선택) · 권한 안내(알람이 울릴 수 없는 상태일 때만) */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLanguage: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 언어 화면·설정 앱에서 돌아올 때마다 다시 읽는다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME, onEvent = viewModel::refresh)

    SettingsScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        Block(stringResource(R.string.settings_general)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onLanguage),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_language), style = appText(16), modifier = Modifier.weight(1f))
                Text(stringResource(state.language.nameRes()), style = appText(15, FontWeight.SemiBold, TextMuted))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
            }
        }
        // 권한이 정상이면 안내가 필요 없으므로 숨긴다.
        if (!state.permission.isOk) {
            Block(stringResource(R.string.settings_permissions)) {
                Text(stringResource(R.string.settings_permission_alarm_body), style = appText(15, FontWeight.SemiBold))
                PermissionProblem(state.permission)
                GhostButton(
                    stringResource(R.string.permission_open_settings),
                    onClick = { context.openAlarmPermissionSettings(state.permission) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 언어 선택: 고르면 바로 적용하고 설정 화면으로 돌아간다. */
@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScaffold(title = stringResource(R.string.settings_language), onBack = onBack) {
        Block(stringResource(R.string.settings_language)) {
            AppLanguage.entries.forEachIndexed { index, option ->
                if (index > 0) HorizontalDivider(color = com.alarmheaven.alarm.ui.theme.Line)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(role = Role.RadioButton) {
                            viewModel.setLanguage(option)
                            onBack()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(option.nameRes()), style = appText(16), modifier = Modifier.weight(1f))
                    if (state.language == option) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SunDeep)
                    }
                }
            }
        }
    }
}

/** 문제가 되는 권한을 한 줄씩 적는다(알림이 꺼졌으면 그것부터). */
@Composable
fun PermissionProblem(status: AlarmPermissionStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!status.notificationsEnabled) {
            Text(stringResource(R.string.permission_alarm_denied), style = appText(13, FontWeight.Bold, Cta))
            Text(stringResource(R.string.permission_settings_path), style = appText(12, FontWeight.SemiBold, TextMuted))
        } else if (!status.fullScreenAllowed) {
            Text(stringResource(R.string.permission_fullscreen_denied), style = appText(13, FontWeight.Bold, Cta))
        }
    }
}

@Composable
private fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 6.dp)) {
            RoundButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), modifier = Modifier.padding(0.dp))
            }
            Text(title, style = appText(17, FontWeight.ExtraBold), modifier = Modifier.align(Alignment.Center))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}
