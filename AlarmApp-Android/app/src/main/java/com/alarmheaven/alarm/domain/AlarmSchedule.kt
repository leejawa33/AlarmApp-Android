package com.alarmheaven.alarm.domain

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/** 반복 알람의 시각·요일. "다음 울림"과 "남은 시간" 계산을 담당하는 순수 값 타입. */
data class AlarmSchedule(
    val hour: Int,      // 0..23
    val minute: Int,    // 0..59
    val weekdays: Set<Weekday>,
) {
    /** [now] 이후(같은 시각은 제외) 가장 가까운 울림 시각. 요일이 없으면 null. */
    fun nextFireTime(now: ZonedDateTime): ZonedDateTime? {
        if (weekdays.isEmpty()) return null
        val time = LocalTime.of(hour, minute)
        val today = now.toLocalDate()
        // 오늘부터 7일 뒤(같은 요일 다음 주)까지 보면 반드시 찾는다.
        for (offset in 0L..7L) {
            val date = today.plusDays(offset)
            if (Weekday.from(date.dayOfWeek) !in weekdays) continue
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    /** 리스트의 "다음 울림까지 N시간 M분" */
    data class Remaining(val hours: Int, val minutes: Int)

    companion object {
        /** 분 단위 올림이라 울리기 전에는 0분이 되지 않는다(1분 30초 → 2분). */
        fun remaining(from: ZonedDateTime, to: ZonedDateTime): Remaining {
            val millis = Duration.between(from, to).toMillis()
            val totalMinutes = if (millis <= 0) 0 else ((millis + 59_999) / 60_000).toInt()
            return Remaining(totalMinutes / 60, totalMinutes % 60)
        }
    }
}
