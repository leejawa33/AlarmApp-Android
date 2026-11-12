package com.alarmheaven.alarm.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 다음 울림 시각·남은 시간 계산. 기준일 2026-11-11은 수요일. */
class AlarmScheduleTest {
    private val zone = ZoneId.of("Asia/Seoul")

    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 11, day, hour, minute, second, 0, zone)

    @Test
    fun laterTodayFiresToday() {
        val schedule = AlarmSchedule(7, 0, setOf(Weekday.WEDNESDAY))
        assertEquals(at(11, 7, 0), schedule.nextFireTime(at(11, 6, 0)))
    }

    @Test
    fun exactlyNowFiresNextWeek() {
        val schedule = AlarmSchedule(7, 0, setOf(Weekday.WEDNESDAY))
        assertEquals(at(18, 7, 0), schedule.nextFireTime(at(11, 7, 0)))
    }

    @Test
    fun passedTodayFiresNextSelectedDay() {
        val schedule = AlarmSchedule(7, 0, setOf(Weekday.MONDAY, Weekday.FRIDAY))
        assertEquals(at(13, 7, 0), schedule.nextFireTime(at(11, 8, 0)))
    }

    @Test
    fun crossesWeekBoundary() {
        // 토요일 밤 → 일요일 새벽
        val schedule = AlarmSchedule(0, 10, setOf(Weekday.SUNDAY))
        assertEquals(at(15, 0, 10), schedule.nextFireTime(at(14, 23, 30)))
    }

    @Test
    fun noWeekdayHasNoNextFire() {
        assertNull(AlarmSchedule(7, 0, emptySet()).nextFireTime(at(11, 6, 0)))
    }

    @Test
    fun disabledAlarmHidesNextFire() {
        val alarm = Alarm(
            title = "t", hour = 7, minute = 0, weekdays = setOf(Weekday.WEDNESDAY),
            isEnabled = false, mission = MissionType.SHAKE, mediaId = "m", soundId = "s",
        )
        assertNull(alarm.nextFireTime(at(11, 6, 0)))
    }

    private fun assertRemaining(seconds: Long, hours: Int, minutes: Int) {
        val now = at(11, 6, 0)
        val result = AlarmSchedule.remaining(now, now.plusSeconds(seconds))
        assertEquals("$seconds 초 → 시간", hours, result.hours)
        assertEquals("$seconds 초 → 분", minutes, result.minutes)
    }

    @Test
    fun remainingRoundsUpToMinute() {
        assertRemaining(90, 0, 2)                  // 1분 30초 → 2분 (올림)
        assertRemaining(5400, 1, 30)               // 정확히 1시간 30분
        assertRemaining(60L * 60 * 23 + 1, 23, 1)
        assertRemaining(0, 0, 0)
    }

    @Test
    fun remainingNeverNegative() {
        val now = at(11, 6, 0)
        assertEquals(AlarmSchedule.Remaining(0, 0), AlarmSchedule.remaining(now, now.minusMinutes(5)))
    }

    @Test
    fun weekdayMaskRoundTrips() {
        val days = setOf(Weekday.MONDAY, Weekday.WEDNESDAY, Weekday.SUNDAY)
        assertEquals(days, Weekday.set(Weekday.mask(days)))
    }

    @Test
    fun weekdayMaskMatchesIosBits() {
        assertEquals(1, Weekday.SUNDAY.bit)
        assertEquals(0b0111110, Weekday.mask(Weekday.weekdaysOnly))
    }
}
