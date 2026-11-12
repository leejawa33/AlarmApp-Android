package com.alarmheaven.alarm.ui

import androidx.lifecycle.SavedStateHandle
import com.alarmheaven.alarm.FakeAlarmDao
import com.alarmheaven.alarm.FakeScheduler
import com.alarmheaven.alarm.TestAssets
import com.alarmheaven.alarm.catalog.PromptProvider
import com.alarmheaven.alarm.alarm.RingingState
import com.alarmheaven.alarm.domain.MissionType
import com.alarmheaven.alarm.service.AlarmService
import com.alarmheaven.alarm.ui.ring.RingInfo
import com.alarmheaven.alarm.ui.ring.RingPhase
import com.alarmheaven.alarm.ui.ring.RingViewModel
import com.alarmheaven.alarm.ui.ring.formatClock
import java.time.LocalTime
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RingTest {
    private val scheduler = FakeScheduler()
    private val catalog = TestAssets.catalog()
    private val service = AlarmService(FakeAlarmDao(), catalog, scheduler)

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() {
        // 전역 상태를 먼저 지우고(수집 중인 코루틴이 Main에서 깨어날 수 있으므로) 그다음 Main을 되돌린다.
        RingingState.alarmId.value?.let(RingingState::stop)
        Dispatchers.resetMain()
    }

    private fun liveVm(alarmId: String) = RingViewModel(
        SavedStateHandle(mapOf("alarmId" to alarmId)),
        service,
        PromptProvider { TestAssets.prompts() },
    )

    private fun vm(
        title: String = "기상",
        mission: String = "shake",
        mediaId: String = "media_puppy_crack",
        soundId: String = "sound_morning_arrived",
        promptIds: String = "",
        prompts: Boolean = true,
    ) = RingViewModel(
        SavedStateHandle(mapOf("title" to title, "mission" to mission, "mediaId" to mediaId, "soundId" to soundId, "promptIds" to promptIds)),
        service,
        PromptProvider { if (prompts) TestAssets.prompts() else null },
    )

    @Test
    fun clockIs24HourZeroPadded() {
        assertEquals("06:05", formatClock(LocalTime.of(6, 5)))
        assertEquals("18:30", formatClock(LocalTime.of(18, 30)))
        assertEquals("00:00", formatClock(LocalTime.MIDNIGHT))
    }

    @Test
    fun resolveUsesChosenMediaAndSound() {
        val info = RingInfo.resolve(catalog, "  제목 ", MissionType.SHAKE, "media_puppy_crack", "sound_morning_arrived", emptyList())
        assertEquals("제목", info.title)
        assertEquals(catalog.media("media_puppy_crack")!!.file, info.mediaFile)
        assertEquals(catalog.sound("sound_morning_arrived")!!.file, info.soundFile)
        assertEquals(10, info.shakeCount)
    }

    @Test
    fun resolveFallsBackToDefaultsForUnknownIds() {
        val info = RingInfo.resolve(catalog, "", MissionType.SHAKE, "nope", null, emptyList())
        assertEquals(catalog.defaultMedia!!.file, info.mediaFile)
        assertEquals(catalog.defaultSound!!.file, info.soundFile)
    }

    @Test
    fun viewModelReadsPreviewArguments() {
        val state = vm(title = "출근", mission = "transcription", promptIds = "study_01,study_02").state.value
        assertEquals("출근", state.info.title)
        assertEquals(MissionType.TRANSCRIPTION, state.info.mission)
        assertEquals(listOf("study_01", "study_02"), state.info.promptIds)
        assertEquals(RingPhase.RING, state.phase)
    }

    @Test
    fun emptyPromptIdsMeansAll() {
        assertTrue(vm(promptIds = "").state.value.info.promptIds.isEmpty())
    }

    @Test
    fun shakeMissionHasNoPrompt() {
        val vm = vm(mission = "shake")
        vm.startMission()
        assertEquals(RingPhase.MISSION, vm.state.value.phase)
        assertNull(vm.state.value.prompt)
        assertEquals(MissionType.SHAKE, vm.state.value.activeMission)
    }

    @Test
    fun transcriptionPicksOnlyFromSelectedPrompts() {
        repeat(20) { seed ->
            val vm = vm(mission = "transcription", promptIds = "study_01,study_02")
            vm.random = Random(seed)
            vm.startMission()
            assertTrue(vm.state.value.prompt!!.id in setOf("study_01", "study_02"))
            assertEquals(MissionType.TRANSCRIPTION, vm.state.value.activeMission)
        }
    }

    /** 문구 파일을 못 읽어도 미션 없이 끝나면 안 된다 → 흔들기로 대체 */
    @Test
    fun missingPromptsFallBackToShake() {
        val vm = vm(mission = "transcription", prompts = false)
        vm.startMission()
        assertNull(vm.state.value.prompt)
        assertEquals(MissionType.SHAKE, vm.state.value.activeMission)
    }

    @Test
    fun startMissionOnlyWorksFromRingPhase() {
        val vm = vm()
        vm.startMission()
        vm.completeMission()
        vm.startMission()
        assertEquals(RingPhase.COMPLETE, vm.state.value.phase)
    }

    /** 미리보기는 미션을 끝내도 알람을 멈추지 않는다 */
    @Test
    fun previewCompletionStopsNothingAndClosesAfterFeedback() = runTest(StandardTestDispatcher()) {
        val vm = vm()
        vm.completeDisplayMillis = 1_000
        vm.startMission()
        vm.completeMission()
        assertEquals(RingPhase.COMPLETE, vm.state.value.phase)
        assertFalse(vm.state.value.closed)
        testScheduler.advanceTimeBy(1_001)
        testScheduler.runCurrent()
        assertTrue(vm.state.value.closed)
        assertTrue(scheduler.stopped.isEmpty())
    }

    @Test
    fun endPreviewClosesImmediately() {
        val vm = vm()
        vm.endPreview()
        assertTrue(vm.state.value.closed)
        assertNotNull(vm.state.value.info)
    }

    // MARK: 실제 울림 모드

    @Test
    fun liveModeLoadsAlarmAndStopsItOnCompletion() = runTest(StandardTestDispatcher()) {
        val alarm = service.create(
            service.makeDefaultDraft().copy(title = "출근", mission = MissionType.TRANSCRIPTION, promptIds = listOf("study_01")),
        )
        RingingState.start(alarm.id)
        val vm = liveVm(alarm.id)
        assertFalse(vm.state.value.isLoaded)
        testScheduler.runCurrent()
        assertTrue(vm.state.value.isLoaded)
        assertFalse(vm.state.value.isPreview)
        assertEquals("출근", vm.state.value.info.title)
        assertEquals(MissionType.TRANSCRIPTION, vm.state.value.info.mission)

        vm.completeDisplayMillis = 500
        vm.startMission()
        vm.completeMission()
        testScheduler.runCurrent()
        assertEquals(listOf(alarm.id), scheduler.stopped) // 울리는 알람을 멈춘다
        assertFalse(vm.state.value.closed)
        testScheduler.advanceTimeBy(501)
        testScheduler.runCurrent()
        assertTrue(vm.state.value.closed)
    }

    /** 알람이 삭제돼도 미션은 해야 한다 → 기본 미디어·소리 + 흔들기 */
    @Test
    fun liveModeWithMissingAlarmFallsBackToDefaults() = runTest(StandardTestDispatcher()) {
        RingingState.start("deleted")
        val vm = liveVm("deleted")
        testScheduler.runCurrent()
        assertTrue(vm.state.value.isLoaded)
        assertEquals(MissionType.SHAKE, vm.state.value.info.mission)
        assertEquals(catalog.defaultMedia!!.file, vm.state.value.info.mediaFile)
    }

    @Test
    fun liveModeClosesWhenRingingStopsElsewhere() = runTest(StandardTestDispatcher()) {
        RingingState.start("a")
        val vm = liveVm("a")
        testScheduler.runCurrent()
        assertFalse(vm.state.value.closed)
        RingingState.stop("a")
        testScheduler.runCurrent()
        assertTrue(vm.state.value.closed)
    }

    @Test
    fun previewNeverTouchesRingingState() {
        assertNull(RingingState.alarmId.value)
        assertTrue(vm().state.value.isPreview)
        assertNull(RingingState.alarmId.value)
    }
}
