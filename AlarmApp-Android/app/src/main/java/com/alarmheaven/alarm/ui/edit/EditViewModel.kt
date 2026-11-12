package com.alarmheaven.alarm.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarmheaven.alarm.catalog.AssetCatalog
import com.alarmheaven.alarm.catalog.PromptProvider
import com.alarmheaven.alarm.domain.Alarm
import com.alarmheaven.alarm.domain.AlarmDraft
import com.alarmheaven.alarm.domain.MissionType
import com.alarmheaven.alarm.domain.TranscriptionPrompts
import com.alarmheaven.alarm.domain.Weekday
import com.alarmheaven.alarm.service.AlarmService
import com.alarmheaven.alarm.service.SchedulingFailedException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditUiState(
    val isNew: Boolean,
    /** 기존 알람을 읽어 오기 전에는 false */
    val isLoaded: Boolean,
    val draft: AlarmDraft,
    val showValidation: Boolean = false,
    val isSaving: Boolean = false,
    val confirmingDelete: Boolean = false,
    val error: Throwable? = null,
    /** 저장은 됐지만 예약만 실패한 경우: 안내 확인 후 닫는다. */
    val closeAfterError: Boolean = false,
    /** true가 되면 화면이 닫힌다. */
    val closed: Boolean = false,
) {
    val titleMissing: Boolean get() = draft.title.isBlank()
    val weekdayMissing: Boolean get() = draft.weekdays.isEmpty()
}

/** 알람 생성·편집. 라우트 인자 `alarmId`가 없으면 새 알람이다. */
@HiltViewModel
class EditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val service: AlarmService,
    promptProvider: PromptProvider,
) : ViewModel() {

    private val alarmId: String? = savedStateHandle["alarmId"]
    private var original: Alarm? = null

    val catalog: AssetCatalog get() = service.catalog

    /** null이면(문구 파일 없음) 따라쓰기 문장 선택을 숨긴다. */
    val prompts: TranscriptionPrompts? = promptProvider.prompts

    private val _state = MutableStateFlow(
        EditUiState(isNew = alarmId == null, isLoaded = alarmId == null, draft = service.makeDefaultDraft()),
    )
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        if (alarmId != null) {
            viewModelScope.launch {
                val alarm = service.getAlarm(alarmId)
                if (alarm == null) {
                    _state.update { it.copy(closed = true) } // 이미 삭제된 알람
                } else {
                    original = alarm
                    _state.update { it.copy(isLoaded = true, draft = alarm.toDraft()) }
                }
            }
        }
    }

    // MARK: 입력

    private fun edit(block: (AlarmDraft) -> AlarmDraft) = _state.update { it.copy(draft = block(it.draft)) }

    fun setTitle(title: String) = edit { it.copy(title = title) }
    fun setTime(hour: Int, minute: Int) = edit { it.copy(hour = hour, minute = minute) }
    fun setMission(mission: MissionType) = edit { it.copy(mission = mission) }
    fun setMedia(id: String) = edit { it.copy(mediaId = id) }
    fun setSound(id: String) = edit { it.copy(soundId = id) }
    fun setPromptIds(ids: List<String>) = edit { it.copy(promptIds = ids) }

    fun toggleWeekday(day: Weekday) = edit {
        it.copy(weekdays = if (day in it.weekdays) it.weekdays - day else it.weekdays + day)
    }

    // MARK: 동작

    fun save() {
        val current = _state.value
        _state.update { it.copy(showValidation = true) }
        if (current.titleMissing || current.weekdayMissing || current.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val existing = original
                if (existing != null) service.update(existing, current.draft) else service.create(current.draft)
                _state.update { it.copy(isSaving = false, closed = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun requestDelete() = _state.update { it.copy(confirmingDelete = true) }

    fun cancelDelete() = _state.update { it.copy(confirmingDelete = false) }

    fun confirmDelete() {
        val id = original?.id ?: return
        _state.update { it.copy(confirmingDelete = false) }
        viewModelScope.launch {
            try {
                service.delete(id)
                _state.update { it.copy(closed = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun dismissError() = _state.update {
        // 저장·삭제는 됐고 예약만 실패했다면 안내 후 닫는다.
        it.copy(error = null, closed = it.closed || it.closeAfterError, closeAfterError = false)
    }

    private fun fail(error: Exception) = _state.update {
        it.copy(isSaving = false, error = error, closeAfterError = error is SchedulingFailedException)
    }
}
