package com.alarmheaven.alarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 디자인 토큰 (CLAUDE.md 참고)
val Bg = Color(0xFFFFF8F0)
val BgDeep = Color(0xFFFFE8D6)
val Sun = Color(0xFFFFC107)
val SunDeep = Color(0xFFFFB300)
val Cta = Color(0xFFFF5A5F)
val CtaPress = Color(0xFFE8454A)
val TextPrimary = Color(0xFF1A1A1A)
val TextMuted = Color(0xFF6B6560)
val Line = Color(0x141A1A1A)      // text 8%
val ChipOn = Color(0xFFFFF3CD)
val ChipOff = Color(0xFFF3EEE8)
val DayOff = Color(0xFFEEE7DF)
val SwitchOff = Color(0xFFE5DDD4)

/**
 * 타이포: 둥근 산세리프 볼드 계열이 목표지만 Android에는 SF Rounded가 없다.
 * 지금은 시스템 산세리프의 굵은 두께로 대체한다(둥근 폰트 번들은 이후 다듬기 단계에서 결정).
 */
fun appText(size: Int, weight: FontWeight = FontWeight.Bold, color: Color = TextPrimary) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
)

// 라이트 테마 고정 (다크는 이후 단계)
private val LightColors = lightColorScheme(
    primary = Cta,
    onPrimary = Color.White,
    secondary = Sun,
    onSecondary = TextPrimary,
    background = Bg,
    onBackground = TextPrimary,
    surface = Bg,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
)

@Composable
fun AlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
