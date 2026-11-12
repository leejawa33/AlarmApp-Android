package com.alarmheaven.alarm.ui.ring

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alarmheaven.alarm.R
import com.alarmheaven.alarm.domain.ShakeCounter
import com.alarmheaven.alarm.domain.TranscriptionPrompt
import com.alarmheaven.alarm.domain.TranscriptionPrompts
import com.alarmheaven.alarm.ui.appLanguage
import com.alarmheaven.alarm.ui.theme.Bg
import com.alarmheaven.alarm.ui.theme.Cta
import com.alarmheaven.alarm.ui.theme.ChipOn
import com.alarmheaven.alarm.ui.theme.Line
import com.alarmheaven.alarm.ui.theme.Sun
import com.alarmheaven.alarm.ui.theme.SunDeep
import com.alarmheaven.alarm.ui.theme.TextMuted
import com.alarmheaven.alarm.ui.theme.TextPrimary
import com.alarmheaven.alarm.ui.theme.appText
import kotlinx.coroutines.delay

/** 미션 화면 공통 배경: 밝은 배경 + 위쪽 햇살 */
@Composable
private fun MissionBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .background(
                Brush.radialGradient(
                    colors = listOf(Sun.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(540f, 560f),
                    radius = 900f,
                ),
            ),
    ) { content() }
}

/** 흔들기 미션: 진행 링 + "n/10". 센서가 없는 기기에서는 링을 눌러 센다(미션을 끝낼 수 없게 되면 안 되므로). */
@Composable
fun ShakeMission(target: Int, onComplete: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val counter = remember(target) { ShakeCounter(target) }
    var count by remember(counter) { mutableIntStateOf(0) }
    val hasSensor = remember { context.hasShakeSensor() }

    fun onShake() {
        count = counter.count
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (counter.isComplete) onComplete()
    }

    ShakeSensor(counter, onShake = ::onShake)

    val wiggle by rememberInfiniteTransition(label = "wiggle").animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Reverse),
        label = "wiggleAngle",
    )

    MissionBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📳", style = appText(44), modifier = Modifier.padding(top = 40.dp).rotate(wiggle))
            Text(stringResource(R.string.mission_shake), style = appText(24, FontWeight.ExtraBold), modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.mission_shake_hint), style = appText(15, FontWeight.SemiBold, TextMuted), modifier = Modifier.padding(top = 8.dp))
            ShakeRing(
                count = count,
                target = counter.target,
                modifier = Modifier
                    .padding(top = 36.dp)
                    .then(
                        if (hasSensor) Modifier else Modifier.clickable { if (counter.registerShake(System.nanoTime() / 1e9)) onShake() },
                    ),
            )
            if (count >= maxOf(1, counter.target - 3) && !counter.isComplete) {
                Text(stringResource(R.string.mission_almost), style = appText(17, FontWeight.ExtraBold, Cta), modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
private fun ShakeRing(count: Int, target: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(count) {
        progress.animateTo(count.toFloat() / target, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
    }
    Box(
        modifier
            .size(180.dp)
            .shadow(14.dp, CircleShape, ambientColor = Sun.copy(alpha = 0.25f), spotColor = Sun.copy(alpha = 0.25f))
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(5.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color(0xFFFFE0A3), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(Sun, -90f, 360f * progress.value, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(stringResource(R.string.mission_shake_progress, count), style = appText(46, FontWeight.ExtraBold))
    }
}

/** 따라쓰기 미션: 문장을 그대로 입력하면(앞뒤 공백 무시, 대소문자 구분) 바로 완료 */
@Composable
fun TranscriptionMission(prompts: TranscriptionPrompts, prompt: TranscriptionPrompt, onComplete: () -> Unit) {
    val language = LocalContext.current.appLanguage
    var input by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    MissionBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("✍️", style = appText(40), modifier = Modifier.padding(top = 40.dp))
            Text(stringResource(R.string.mission_transcription), style = appText(24, FontWeight.ExtraBold), modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.mission_transcription_hint), style = appText(15, FontWeight.SemiBold, TextMuted), modifier = Modifier.padding(top = 8.dp))

            val card = RoundedCornerShape(18.dp)
            Text(
                prompt.text(language),
                style = appText(28, FontWeight.ExtraBold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 28.dp)
                    .fillMaxWidth()
                    .background(ChipOn, card)
                    .border(BorderStroke(2.dp, Sun), card)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            )

            val field = RoundedCornerShape(14.dp)
            BasicTextField(
                value = input,
                onValueChange = {
                    input = it
                    if (prompts.isMatch(it, prompt, language)) onComplete()
                },
                singleLine = true,
                textStyle = appText(20),
                cursorBrush = SolidColor(Cta),
                // 영어 문장은 첫 글자만 대문자 → 문장 첫 글자 자동 대문자가 판정과 맞는다
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrectEnabled = false),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .background(Color.White, field)
                    .border(BorderStroke(1.dp, Line), field)
                    .padding(16.dp),
                decorationBox = { inner ->
                    Box {
                        if (input.isEmpty()) {
                            Text(stringResource(R.string.mission_transcription_placeholder), style = appText(20, color = TextMuted.copy(alpha = 0.6f)))
                        }
                        inner()
                    }
                },
            )
        }
    }
}

/** 미션 완료 짧은 피드백 + 진동 */
@Composable
fun MissionComplete() {
    val context = LocalContext.current
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        vibrateSuccess(context)
        scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }
    MissionBackground {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Sun, modifier = Modifier.size(96.dp).scale(scale.value))
            Text(
                stringResource(R.string.mission_complete),
                style = appText(24, FontWeight.ExtraBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

private fun vibrateSuccess(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    vibrator?.takeIf { it.hasVibrator() }?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 60, 140), -1))
}
