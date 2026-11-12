package com.alarmheaven.alarm.ui

import android.content.Context
import com.alarmheaven.alarm.R
import com.alarmheaven.alarm.catalog.AssetCatalog
import com.alarmheaven.alarm.domain.AlarmSchedule
import com.alarmheaven.alarm.domain.AlarmValidationError
import com.alarmheaven.alarm.domain.Weekday
import com.alarmheaven.alarm.service.SchedulingFailedException
import java.util.Locale

/** 화면 표시용 문구 조합. 문구 원본은 strings.xml(한/영). iOS의 `Strings` 헬퍼에 해당한다. */

/** 앱 언어("ko" 또는 "en"). 한·영이 아니면 한국어. 따라쓰기 문장 선택에도 쓴다. */
val Context.appLanguage: String
    get() = resources.configuration.locales[0].language.takeIf { it == "en" } ?: "ko"

/** ko: "오전 6:30", en: "6:30 AM" (순서는 `alarm_time_format`이 정한다) */
fun Context.alarmTime(hour: Int, minute: Int): String {
    val period = getString(if (hour < 12) R.string.am else R.string.pm)
    val clock = String.format(Locale.ROOT, "%d:%02d", if (hour % 12 == 0) 12 else hour % 12, minute)
    return getString(R.string.alarm_time_format, clock, period)
}

fun Context.weekdayShort(day: Weekday): String = getString(
    when (day) {
        Weekday.SUNDAY -> R.string.weekday_sun
        Weekday.MONDAY -> R.string.weekday_mon
        Weekday.TUESDAY -> R.string.weekday_tue
        Weekday.WEDNESDAY -> R.string.weekday_wed
        Weekday.THURSDAY -> R.string.weekday_thu
        Weekday.FRIDAY -> R.string.weekday_fri
        Weekday.SATURDAY -> R.string.weekday_sat
    },
)

/** "월 화 수 목 금" / 7일 모두면 "매일" */
fun Context.weekdaySummary(days: Set<Weekday>): String =
    if (days.size == Weekday.entries.size) {
        getString(R.string.weekday_everyday)
    } else {
        Weekday.displayOrder.filter { it in days }.joinToString(" ") { weekdayShort(it) }
    }

/** "8시간 24분 후" / "24분 후" */
fun Context.remainingText(remaining: AlarmSchedule.Remaining): String =
    if (remaining.hours > 0) {
        getString(R.string.relative_later, remaining.hours, remaining.minutes)
    } else {
        getString(R.string.relative_minutes_only, remaining.minutes)
    }

/** 카탈로그 id(`media_...`)를 이름으로 문자열을 찾고, 없으면 카탈로그 `displayName`(한국어). */
private fun Context.catalogName(id: String, fallback: String): String {
    val resId = resources.getIdentifier(id, "string", packageName)
    return if (resId != 0) getString(resId) else fallback
}

fun Context.mediaName(media: AssetCatalog.Media) = catalogName(media.id, media.displayName)
fun Context.soundName(sound: AssetCatalog.Sound) = catalogName(sound.id, sound.displayName)

/** 서비스 에러 → 사용자 문구 */
fun Context.errorMessage(error: Throwable): String = getString(
    when (error) {
        is AlarmValidationError.EmptyTitle -> R.string.edit_title_required
        is AlarmValidationError.NoWeekday -> R.string.edit_weekday_required
        is AlarmValidationError.LimitReached -> R.string.main_max_alarms
        is SchedulingFailedException -> R.string.error_scheduling_failed
        else -> R.string.error_save_failed
    },
)
