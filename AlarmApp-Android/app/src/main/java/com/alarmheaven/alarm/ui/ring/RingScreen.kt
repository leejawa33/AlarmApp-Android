package com.alarmheaven.alarm.ui.ring

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alarmheaven.alarm.R
import com.alarmheaven.alarm.domain.MissionType
import com.alarmheaven.alarm.ui.appLanguage
import com.alarmheaven.alarm.ui.components.CtaButton
import com.alarmheaven.alarm.ui.theme.TextMuted
import com.alarmheaven.alarm.ui.theme.appText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * 알람 실행 화면 → 미션 → 완료. 뒤로가기로 닫을 수 없고, 하단 `미리보기 종료`로만 나갈 수 있다(미리보기).
 * 영상·소리는 이 화면이 떠 있는 동안만 재생하고, 화면이 꺼지지 않게 유지한다.
 */
@Composable
fun RingScreen(
    onClose: () -> Unit,
    viewModel: RingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) onClose() }

    BackHandler(enabled = true) {} // 미션 완료 전에는 뒤로가기로 끌 수 없다
    KeepScreenOn()
    // 완료되면 소리를 멈춘다. 영상은 완료 화면으로 바뀌며 사라진다.
    RingSound(state.info.soundFile, playing = state.phase != RingPhase.COMPLETE)

    Column(Modifier.fillMaxSize().background(Color.Black).imePadding()) { // 키보드가 올라와도 종료 바가 보이게
        Box(Modifier.weight(1f)) {
            Crossfade(state.phase, label = "ringPhase") { phase ->
                when (phase) {
                    RingPhase.RING -> RingContent(state.info, onStartMission = viewModel::startMission)
                    RingPhase.MISSION -> {
                        val prompts = viewModel.prompts
                        val prompt = state.prompt
                        if (state.activeMission == MissionType.TRANSCRIPTION && prompts != null && prompt != null) {
                            TranscriptionMission(prompts, prompt, onComplete = viewModel::completeMission)
                        } else {
                            ShakeMission(state.info.shakeCount, onComplete = viewModel::completeMission)
                        }
                    }
                    RingPhase.COMPLETE -> MissionComplete()
                }
            }
        }
        PreviewBar(onEnd = viewModel::endPreview)
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun RingContent(info: RingInfo, onStartMission: () -> Unit) {
    val context = LocalContext.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            // 다음 분이 시작될 때 갱신
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val shadowed = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.25f), blurRadius = 16f))

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        info.mediaFile?.let { LoopingVideo(it, Modifier.fillMaxSize()) }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(top = 48.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val date = DateTimeFormatter
                .ofPattern(stringResource(R.string.ring_date_format), Locale.forLanguageTag(context.appLanguage))
                .format(now)
            Text(date, style = appText(18, color = Color.White).merge(shadowed))
            Text(
                formatClock(now.toLocalTime()),
                style = appText(84, FontWeight.Black, Color.White).merge(shadowed).copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(top = 4.dp),
            )
            if (info.title.isNotEmpty()) {
                Text(
                    info.title,
                    style = appText(15, color = TextMuted),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
            Box(Modifier.weight(1f))
            CtaButton(
                stringResource(R.string.mission_start),
                onClick = onStartMission,
                height = 56.dp,
                modifier = Modifier.padding(horizontal = 20.dp).scale(pulse),
            )
        }
    }
}

/** 미리보기에서만 보이는 하단 바 */
@Composable
private fun PreviewBar(onEnd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF232325))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            stringResource(R.string.preview_end),
            style = appText(15, color = Color.White),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(Color(0xFF3A3A3C), RoundedCornerShape(percent = 50))
                .clickable(onClick = onEnd)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}
