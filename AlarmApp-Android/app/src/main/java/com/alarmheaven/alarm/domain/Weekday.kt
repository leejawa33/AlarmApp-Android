package com.alarmheaven.alarm.domain

import java.time.DayOfWeek

/**
 * 요일. [value]는 iOS `Calendar.weekday`와 같다(1 = 일요일 ... 7 = 토요일).
 * DB에는 비트마스크(Int)로 저장한다.
 */
enum class Weekday(val value: Int, val dayOfWeek: DayOfWeek) {
    SUNDAY(1, DayOfWeek.SUNDAY),
    MONDAY(2, DayOfWeek.MONDAY),
    TUESDAY(3, DayOfWeek.TUESDAY),
    WEDNESDAY(4, DayOfWeek.WEDNESDAY),
    THURSDAY(5, DayOfWeek.THURSDAY),
    FRIDAY(6, DayOfWeek.FRIDAY),
    SATURDAY(7, DayOfWeek.SATURDAY);

    val bit: Int get() = 1 shl (value - 1)

    companion object {
        /** 월요일부터 시작하는 표시 순서 */
        val displayOrder: List<Weekday> = listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)

        /** 월~금 (새 알람 기본값) */
        val weekdaysOnly: Set<Weekday> = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

        fun from(dayOfWeek: DayOfWeek): Weekday = entries.first { it.dayOfWeek == dayOfWeek }

        fun mask(days: Set<Weekday>): Int = days.fold(0) { acc, day -> acc or day.bit }

        fun set(mask: Int): Set<Weekday> = entries.filter { mask and it.bit != 0 }.toSet()
    }
}
