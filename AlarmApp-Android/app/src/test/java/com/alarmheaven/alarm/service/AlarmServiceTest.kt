package com.alarmheaven.alarm.service

import com.alarmheaven.alarm.FakeAlarmDao
import com.alarmheaven.alarm.FakeScheduler
import com.alarmheaven.alarm.TestAssets
import com.alarmheaven.alarm.domain.Alarm
import com.alarmheaven.alarm.domain.AlarmDraft
import com.alarmheaven.alarm.domain.AlarmValidationError
import com.alarmheaven.alarm.domain.MissionType
import com.alarmheaven.alarm.domain.Weekday
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AlarmServiceTest {
    private val dao = FakeAlarmDao()
    private val scheduler = FakeScheduler()
    private val service = AlarmService(dao, TestAssets.catalog(), scheduler)

    private fun draft(title: String = "기상") = service.makeDefaultDraft().copy(title = title)

    @Test
    fun defaultDraftFollowsSpec() {
        val d = service.makeDefaultDraft()
        assertEquals("", d.title)
        assertEquals(7, d.hour)
        assertEquals(0, d.minute)
        assertEquals(Weekday.weekdaysOnly, d.weekdays)
        assertEquals(MissionType.SHAKE, d.mission)
        assertEquals("media_puppy_crack", d.mediaId)
        assertEquals("sound_morning_arrived", d.soundId)
        assertTrue(d.promptIds.isEmpty()) // 새 알람 기본값 = 전체
        assertTrue(d.isEnabled)
    }

    @Test
    fun createSavesAndSchedules() = runTest {
        val alarm = service.create(draft("  출근  "))
        assertEquals("출근", alarm.title) // trim
        assertEquals(alarm, service.getAlarm(alarm.id))
        assertEquals(alarm, scheduler.scheduled[alarm.id])
        assertEquals(listOf(alarm), service.observeAlarms().first())
    }

    @Test
    fun createRejectsInvalidDrafts() = runTest {
        suspend fun expect(error: AlarmValidationError, d: AlarmDraft) {
            try {
                service.create(d)
                fail("예외가 나야 함: $error")
            } catch (e: AlarmValidationError) {
                assertEquals(error, e)
            }
        }
        expect(AlarmValidationError.EmptyTitle, draft("   "))
        expect(AlarmValidationError.NoWeekday, draft().copy(weekdays = emptySet()))
        expect(AlarmValidationError.InvalidTime, draft().copy(hour = 24))
        expect(AlarmValidationError.InvalidTime, draft().copy(minute = 60))
        expect(AlarmValidationError.UnknownMedia("nope"), draft().copy(mediaId = "nope"))
        expect(AlarmValidationError.UnknownSound("nope"), draft().copy(soundId = "nope"))
        assertEquals(0, dao.count()) // 검증 실패는 저장하지 않는다
    }

    @Test
    fun blocksTheEleventhAlarm() = runTest {
        repeat(AlarmService.MAX_ALARMS) { service.create(draft("알람 $it")) }
        assertFalse(service.canAddAlarm())
        try {
            service.create(draft("초과"))
            fail("한도 초과 예외가 나야 함")
        } catch (e: AlarmValidationError.LimitReached) {
            assertEquals(10, e.max)
        }
        assertEquals(10, dao.count())
    }

    @Test
    fun updateChangesFieldsAndKeepsIdentity() = runTest {
        val alarm = service.create(draft())
        val updated = service.update(
            alarm,
            draft("새 이름").copy(hour = 6, minute = 30, mission = MissionType.TRANSCRIPTION, promptIds = listOf("study_01")),
        )
        assertEquals(alarm.id, updated.id)
        assertEquals(alarm.createdAt, updated.createdAt)
        assertEquals("새 이름", updated.title)
        assertEquals(MissionType.TRANSCRIPTION, service.getAlarm(alarm.id)?.mission)
        assertEquals(listOf("study_01"), service.getAlarm(alarm.id)?.promptIds)
        assertEquals(updated, scheduler.scheduled[alarm.id])
    }

    @Test
    fun disablingCancelsAndEnablingSchedulesAgain() = runTest {
        val alarm = service.create(draft())
        val off = service.setEnabled(alarm, false)
        assertNull(scheduler.scheduled[alarm.id])
        assertFalse(service.getAlarm(alarm.id)!!.isEnabled)
        service.setEnabled(off, true)
        assertNotNull(scheduler.scheduled[alarm.id])
    }

    @Test
    fun deleteRemovesRowAndSchedule() = runTest {
        val alarm = service.create(draft())
        service.delete(alarm.id)
        assertNull(service.getAlarm(alarm.id))
        assertNull(scheduler.scheduled[alarm.id])
    }

    @Test
    fun schedulingFailureKeepsDataAndThrows() = runTest {
        scheduler.failWith = IllegalStateException("권한 없음")
        try {
            service.create(draft())
            fail("SchedulingFailedException이 나야 함")
        } catch (e: SchedulingFailedException) {
            assertTrue(e.cause is IllegalStateException)
        }
        assertEquals(1, dao.count()) // 데이터는 남는다 → reconcile이 나중에 맞춘다
    }

    @Test
    fun completingMissionStopsAlarmButKeepsSchedule() = runTest {
        val alarm = service.create(draft())
        service.completeMission(alarm.id)
        assertEquals(listOf(alarm.id), scheduler.stopped)
        assertNotNull(scheduler.scheduled[alarm.id]) // 반복 알람은 다음 요일에 또 울려야 한다
    }

    @Test
    fun completeMissionSwallowsStopFailure() = runTest {
        val alarm = service.create(draft())
        scheduler.failWith = IllegalStateException("정지 실패")
        service.completeMission(alarm.id) // 예외 없이 끝나야 한다
    }

    @Test
    fun reconcileSchedulesEnabledAndCancelsDisabled() = runTest {
        val on = service.create(draft("켜짐"))
        val off = service.create(draft("꺼짐").copy(isEnabled = false))
        scheduler.scheduled.clear() // 재부팅으로 예약이 사라진 상태
        scheduler.scheduled[off.id] = off // 꺼진 알람의 잔여 예약

        service.reconcile()

        assertEquals(setOf(on.id), scheduler.scheduled.keys)
    }
}
