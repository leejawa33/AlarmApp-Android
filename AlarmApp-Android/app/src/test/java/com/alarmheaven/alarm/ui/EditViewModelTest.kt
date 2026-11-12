package com.alarmheaven.alarm.ui

import androidx.lifecycle.SavedStateHandle
import com.alarmheaven.alarm.FakeAlarmDao
import com.alarmheaven.alarm.FakeScheduler
import com.alarmheaven.alarm.TestAssets
import com.alarmheaven.alarm.catalog.PromptProvider
import com.alarmheaven.alarm.domain.MissionType
import com.alarmheaven.alarm.domain.Weekday
import com.alarmheaven.alarm.service.AlarmService
import com.alarmheaven.alarm.ui.edit.EditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelTest {
    private val dao = FakeAlarmDao()
    private val scheduler = FakeScheduler()
    private val service = AlarmService(dao, TestAssets.catalog(), scheduler)
    private val prompts = PromptProvider { TestAssets.prompts() }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newVm(alarmId: String? = null) = EditViewModel(
        SavedStateHandle(if (alarmId == null) emptyMap() else mapOf("alarmId" to alarmId)),
        service,
        prompts,
    )

    @Test
    fun newAlarmStartsWithDefaults() {
        val state = newVm().state.value
        assertTrue(state.isNew)
        assertTrue(state.isLoaded)
        assertEquals("", state.draft.title)
        assertEquals(7, state.draft.hour)
        assertEquals(Weekday.weekdaysOnly, state.draft.weekdays)
        assertEquals(MissionType.SHAKE, state.draft.mission)
    }

    @Test
    fun saveWithoutTitleShowsValidationAndDoesNotSave() = runTest {
        val vm = newVm()
        vm.save()
        assertTrue(vm.state.value.showValidation)
        assertTrue(vm.state.value.titleMissing)
        assertFalse(vm.state.value.closed)
        assertEquals(0, dao.count())
    }

    @Test
    fun saveWithoutWeekdayIsBlocked() = runTest {
        val vm = newVm()
        vm.setTitle("기상")
        Weekday.weekdaysOnly.forEach(vm::toggleWeekday)
        assertTrue(vm.state.value.weekdayMissing)
        vm.save()
        assertEquals(0, dao.count())
        assertFalse(vm.state.value.closed)
    }

    @Test
    fun saveCreatesAlarmAndCloses() = runTest {
        val vm = newVm()
        vm.setTitle("  출근 ")
        vm.setTime(6, 30)
        vm.save()
        assertTrue(vm.state.value.closed)
        val saved = service.observeAlarmsOnce().single()
        assertEquals("출근", saved.title)
        assertEquals(6, saved.hour)
        assertEquals(30, saved.minute)
        assertNotNull(scheduler.scheduled[saved.id])
    }

    @Test
    fun editLoadsExistingAndUpdates() = runTest {
        val alarm = service.create(service.makeDefaultDraft().copy(title = "원래"))
        val vm = newVm(alarm.id)
        assertFalse(vm.state.value.isNew)
        assertEquals("원래", vm.state.value.draft.title)

        vm.setTitle("바뀜")
        vm.setMission(MissionType.TRANSCRIPTION)
        vm.setPromptIds(listOf("study_01", "study_02"))
        vm.save()

        val saved = service.getAlarm(alarm.id)!!
        assertEquals("바뀜", saved.title)
        assertEquals(MissionType.TRANSCRIPTION, saved.mission)
        assertEquals(listOf("study_01", "study_02"), saved.promptIds)
        assertEquals(alarm.id, saved.id)
        assertEquals(1, dao.count()) // 수정은 새 행을 만들지 않는다
    }

    @Test
    fun unknownAlarmClosesScreen() = runTest {
        assertTrue(newVm("does-not-exist").state.value.closed)
    }

    @Test
    fun deleteNeedsConfirmation() = runTest {
        val alarm = service.create(service.makeDefaultDraft().copy(title = "삭제"))
        val vm = newVm(alarm.id)
        vm.requestDelete()
        assertTrue(vm.state.value.confirmingDelete)
        vm.cancelDelete()
        assertEquals(1, dao.count())

        vm.requestDelete()
        vm.confirmDelete()
        assertTrue(vm.state.value.closed)
        assertNull(service.getAlarm(alarm.id))
    }

    /** 저장은 됐고 예약만 실패: 안내를 보여 주고, 확인하면 닫는다. 데이터는 남는다. */
    @Test
    fun schedulingFailureKeepsDataAndClosesAfterAcknowledge() = runTest {
        scheduler.failWith = IllegalStateException("권한 없음")
        val vm = newVm()
        vm.setTitle("기상")
        vm.save()
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.closeAfterError)
        assertFalse(vm.state.value.closed)
        assertEquals(1, dao.count())

        vm.dismissError()
        assertTrue(vm.state.value.closed)
    }

    @Test
    fun promptsAreAvailableForPicker() {
        assertEquals(12, newVm().prompts?.prompts?.size)
    }

    private suspend fun AlarmService.observeAlarmsOnce() = observeAlarms().first()
}
