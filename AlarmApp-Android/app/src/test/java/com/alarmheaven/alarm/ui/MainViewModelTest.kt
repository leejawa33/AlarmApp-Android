package com.alarmheaven.alarm.ui

import com.alarmheaven.alarm.FakeAlarmDao
import com.alarmheaven.alarm.FakePermissions
import com.alarmheaven.alarm.FakeScheduler
import com.alarmheaven.alarm.TestAssets
import com.alarmheaven.alarm.domain.AlarmSchedule
import com.alarmheaven.alarm.service.AlarmPermissionStatus
import com.alarmheaven.alarm.service.AlarmService
import com.alarmheaven.alarm.service.SchedulingFailedException
import com.alarmheaven.alarm.ui.main.MainViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    // 2026-11-11(수) 06:00 KST
    private val clock = Clock.fixed(Instant.parse("2026-11-10T21:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val scheduler = FakeScheduler()
    private val service = AlarmService(FakeAlarmDao(), TestAssets.catalog(), scheduler)
    private val permissions = FakePermissions()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newViewModel(): MainViewModel {
        val vm = MainViewModel(service, clock, permissions)
        // WhileSubscribed 구독 유지. 즉시 시작해야 첫 값을 바로 읽을 수 있다.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private suspend fun create(title: String, hour: Int = 7, enabled: Boolean = true) =
        service.create(service.makeDefaultDraft().copy(title = title, hour = hour, isEnabled = enabled))

    @Test
    fun emptyListIsLoadedAndEmpty() = runTest {
        val state = newViewModel().uiState.value
        assertTrue(state.isLoaded)
        assertTrue(state.items.isEmpty())
        assertFalse(state.isAtLimit)
    }

    @Test
    fun enabledAlarmShowsRemainingTimeAndDisabledHidesIt() = runTest {
        create("켜짐", hour = 7)            // 수요일 07:00, 지금 06:00 → 1시간 0분
        create("꺼짐", hour = 8, enabled = false)
        val items = newViewModel().uiState.value.items
        assertEquals(listOf("켜짐", "꺼짐"), items.map { it.alarm.title })
        assertEquals(AlarmSchedule.Remaining(1, 0), items[0].remaining)
        assertNull(items[1].remaining)
    }

    @Test
    fun togglingUpdatesStateAndSchedule() = runTest {
        val alarm = create("토글")
        val vm = newViewModel()
        vm.setEnabled(alarm, false)
        assertFalse(vm.uiState.value.items.single().alarm.isEnabled)
        assertNull(vm.uiState.value.items.single().remaining)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun deleteRemovesAlarm() = runTest {
        val alarm = create("삭제")
        val vm = newViewModel()
        vm.delete(alarm)
        assertTrue(vm.uiState.value.items.isEmpty())
    }

    @Test
    fun reportsAtLimitAfterTenAlarms() = runTest {
        repeat(AlarmService.MAX_ALARMS) { create("알람 $it") }
        assertTrue(newViewModel().uiState.value.isAtLimit)
    }

    @Test
    fun schedulingFailureSurfacesErrorUntilDismissed() = runTest {
        val alarm = create("실패")
        val vm = newViewModel()
        scheduler.failWith = IllegalStateException("예약 실패")
        vm.setEnabled(alarm, false)
        assertTrue(vm.uiState.value.error is SchedulingFailedException)
        vm.dismissError()
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun permissionBannerShowsOnlyWhenEnabledAlarmCannotRing() = runTest {
        val vm = newViewModel()
        permissions.status = AlarmPermissionStatus(notificationsEnabled = false, fullScreenAllowed = true)
        vm.refreshPermission()
        assertFalse(vm.uiState.value.showPermissionBanner) // 알람이 없으면 경고하지 않는다

        create("꺼짐", enabled = false)
        assertFalse(vm.uiState.value.showPermissionBanner) // 켜진 알람이 없어도 마찬가지

        create("켜짐")
        assertTrue(vm.uiState.value.showPermissionBanner)

        permissions.status = AlarmPermissionStatus.Ok // 설정 앱에서 켜고 돌아옴
        vm.refreshPermission()
        assertFalse(vm.uiState.value.showPermissionBanner)
    }

    @Test
    fun fullScreenOffAlsoWarns() = runTest {
        create("켜짐")
        permissions.status = AlarmPermissionStatus(notificationsEnabled = true, fullScreenAllowed = false)
        val vm = newViewModel()
        assertTrue(vm.uiState.value.showPermissionBanner)
    }
}
