package com.alarmheaven.alarm.ui.ring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarmheaven.alarm.alarm.RingingState
import com.alarmheaven.alarm.catalog.PromptProvider
import com.alarmheaven.alarm.domain.MissionType
import com.alarmheaven.alarm.domain.TranscriptionPrompt
import com.alarmheaven.alarm.domain.TranscriptionPrompts
import com.alarmheaven.alarm.service.AlarmService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RingPhase { RING, MISSION, COMPLETE }

data class RingUiState(
    val info: RingInfo,
    val phase: RingPhase = RingPhase.RING,
    /** 따라쓰기 미션에서 출제된 문장. 흔들기 미션이면 null */
    val prompt: TranscriptionPrompt? = null,
    /** true가 되면 화면이 닫힌다. */
    val closed: Boolean = false,
    /** 편집 화면 미리보기인지(false = 실제로 울린 알람) */
    val isPreview: Boolean = true,
    /** 울린 알람을 읽어 오기 전에는 false (기본 영상이 잠깐 보이는 것을 막는다) */
    val isLoaded: Boolean = true,
) {
    /** 실제로 진행할 미션. 문구 파일이 없어 따라쓰기를 못 하면 흔들기로 대체한다(미션 없이 꺼지면 안 됨). */
    val activeMission: MissionType
        get() = if (info.mission == MissionType.TRANSCRIPTION && prompt != null) MissionType.TRANSCRIPTION else MissionType.SHAKE
}

/**
 * 실행 화면(실행 → 미션 → 완료). 두 가지 모드가 있다.
 * - **미리보기**: 편집 중인 값(라우트 인자)으로 열리고, 미션을 끝내도 아무것도 멈추지 않는다.
 * - **실제 울림**: `alarmId`가 있으면 DB의 알람으로 열리고, 미션을 끝내면 그 알람의 울림을 멈춘다.
 *   알람을 못 찾아도(삭제됨) 카탈로그 기본값 + 흔들기로 진행한다 — 미션 없이 꺼지면 안 된다.
 */
@HiltViewModel
class RingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val service: AlarmService,
    promptProvider: PromptProvider,
) : ViewModel() {

    /** 테스트에서 시드를 바꿀 수 있게 생성자가 아닌 프로퍼티로 둔다(Hilt 주입 대상 아님). */
    internal var random: Random = Random.Default

    /** 완료 피드백을 보여 주는 시간 */
    internal var completeDisplayMillis: Long = COMPLETE_DISPLAY_MILLIS

    /** 따라쓰기 문장. 파일을 못 읽으면 null → 흔들기로 대체한다. */
    val prompts: TranscriptionPrompts? = promptProvider.prompts

    /** 있으면 실제 울림 모드 */
    private val alarmId: String? = savedStateHandle["alarmId"]

    private val _state = MutableStateFlow(
        RingUiState(
            isPreview = alarmId == null,
            isLoaded = alarmId == null,
            info = RingInfo.resolve(
                catalog = service.catalog,
                title = savedStateHandle["title"] ?: "",
                mission = MissionType.fromId(savedStateHandle["mission"] ?: ""),
                mediaId = savedStateHandle["mediaId"],
                soundId = savedStateHandle["soundId"],
                promptIds = (savedStateHandle.get<String>("promptIds") ?: "").split(',').filter { it.isNotBlank() },
            ),
        ),
    )
    val state: StateFlow<RingUiState> = _state.asStateFlow()

    init {
        if (alarmId != null) {
            viewModelScope.launch {
                val alarm = service.getAlarm(alarmId)
                _state.update {
                    it.copy(
                        isLoaded = true,
                        info = if (alarm == null) it.info else RingInfo.resolve(
                            service.catalog, alarm.title, alarm.mission, alarm.mediaId, alarm.soundId, alarm.promptIds,
                        ),
                    )
                }
            }
            // 이 알람의 울림이 멈췄으면(다른 경로로 정지) 화면도 닫는다. 미션 완료 중에는 완료 피드백을 끝까지 보여 준다.
            viewModelScope.launch {
                RingingState.alarmId.collect { ringing ->
                    if (ringing != alarmId && _state.value.phase != RingPhase.COMPLETE) _state.update { it.copy(closed = true) }
                }
            }
        }
    }

    fun startMission() {
        val current = _state.value
        if (current.phase != RingPhase.RING) return
        val prompt = if (current.info.mission == MissionType.TRANSCRIPTION) {
            prompts?.randomPrompt(current.info.promptIds, random)
        } else {
            null
        }
        _state.update { it.copy(phase = RingPhase.MISSION, prompt = prompt) }
    }

    /** 미션 완료. 실제 울림이면 그 알람을 멈추고, 미리보기는 아무것도 멈추지 않는다. 피드백을 잠시 보여 준 뒤 닫는다. */
    fun completeMission() {
        if (_state.value.phase == RingPhase.COMPLETE) return
        _state.update { it.copy(phase = RingPhase.COMPLETE) }
        viewModelScope.launch {
            alarmId?.let { service.completeMission(it) }
            delay(completeDisplayMillis)
            _state.update { it.copy(closed = true) }
        }
    }

    /** `미리보기 종료` */
    fun endPreview() = _state.update { it.copy(closed = true) }

    companion object {
        const val COMPLETE_DISPLAY_MILLIS = 1_600L
    }
}
