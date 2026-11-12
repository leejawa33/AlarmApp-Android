package com.alarmheaven.alarm.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.alarmheaven.alarm.ui.theme.ChipOff
import com.alarmheaven.alarm.ui.theme.ChipOn
import com.alarmheaven.alarm.ui.theme.Cta
import com.alarmheaven.alarm.ui.theme.CtaPress
import com.alarmheaven.alarm.ui.theme.Line
import com.alarmheaven.alarm.ui.theme.Sun
import com.alarmheaven.alarm.ui.theme.SunDeep
import com.alarmheaven.alarm.ui.theme.TextMuted
import com.alarmheaven.alarm.ui.theme.TextPrimary
import com.alarmheaven.alarm.ui.theme.appText
import androidx.compose.ui.graphics.Color

/** 시안의 흰 카드 블록 (라벨 + 내용) */
@Composable
fun Block(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, style = appText(12, color = TextMuted))
        content()
    }
}

/** 선택 칩 (미션·미디어·소리). 한 줄에 균등 분할할 수 있게 호출하는 쪽에서 weight를 준다. */
@Composable
fun Chip(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(if (isSelected) ChipOn else ChipOff, shape)
            .border(if (isSelected) BorderStroke(2.dp, Sun) else BorderStroke(0.dp, Color.Transparent), shape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            style = appText(13, color = if (isSelected) TextPrimary else TextMuted),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 눌렀을 때 스프링으로 살짝 줄어드는 효과 */
@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return scale
}

/** 미션 시작·저장·알람 추가 등 CTA */
@Composable
fun CtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 52.dp,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale = pressScale(source)
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) CtaPress else Cta,
            contentColor = Color.White,
            disabledContainerColor = Cta.copy(alpha = 0.45f),
            disabledContentColor = Color.White,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (enabled) Modifier.shadow(10.dp, shape, ambientColor = Cta.copy(alpha = 0.35f), spotColor = Cta.copy(alpha = 0.35f))
                else Modifier,
            ),
    ) {
        Text(text, style = appText(16, FontWeight.ExtraBold, Color.White))
    }
}

/** 흰 배경 보조 버튼 (미리보기 등) */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    val shape = RoundedCornerShape(12.dp)
    Button(
        onClick = onClick,
        interactionSource = source,
        shape = shape,
        border = BorderStroke(1.dp, Line),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = TextPrimary),
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Text(text, style = appText(15, FontWeight.ExtraBold))
    }
}

/** "알람" + "천국"(sun-deep). 공백이 있으면 마지막 단어, 없으면 뒤쪽 절반을 강조한다. */
@Composable
fun Wordmark(name: String, modifier: Modifier = Modifier) {
    val split = name.lastIndexOf(' ').let { if (it >= 0) it + 1 else name.length / 2 }
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = TextPrimary)) { append(name.substring(0, split)) }
            withStyle(SpanStyle(color = SunDeep)) { append(name.substring(split)) }
        },
        style = appText(26, FontWeight.ExtraBold),
        modifier = modifier,
    )
}

/** 흰 바탕 둥근 버튼(상단 취소·휴지통·설정·뒤로). 내용이 한 글자 폭이면 원, 길면 알약 모양이 된다. */
@Composable
fun RoundButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
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
