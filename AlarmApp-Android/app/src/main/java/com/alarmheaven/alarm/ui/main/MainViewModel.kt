package com.alarmheaven.alarm.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarmheaven.alarm.domain.Alarm
import com.alarmheaven.alarm.domain.AlarmSchedule
import com.alarmheaven.alarm.service.AlarmService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 리스트 한 줄. [remaining]이 null이면 "다음 울림까지"를 숨긴다(꺼짐·요일 없음). */
data class AlarmItem(val alarm: Alarm, val remaining: AlarmSchedule.Remaining?)

data class MainUiState(
    /** 첫 목록이 도착하기 전에는 false (빈 상태가 깜빡이지 않게) */
    val isLoaded: Boolean = false,
    val items: List<AlarmItem> = emptyList(),
    val error: Throwable? = null,
) {
    val isAtLimit: Boolean get() = items.size >= AlarmService.MAX_ALARMS
}

/** Spring의 컨트롤러 + 뷰 모델 역할. 화면 상태를 만들고, 변경은 [AlarmService]에만 위임한다. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val service: AlarmService,
    private val clock: Clock,
) : ViewModel() {

    private val error = MutableStateFlow<Throwable?>(null)

    /** 남은 시간 표시를 위해 매 분 경계마다 현재 시각을 새로 흘려 보낸다. */
    private val minuteTicks = flow {
        while (true) {
            val now = ZonedDateTime.now(clock)
            emit(now)
            val intoMinute = Duration.ofSeconds(now.second.toLong(), now.nano.toLong())
            delay(Duration.ofMinutes(1).minus(intoMinute).toMillis().coerceAtLeast(1))
        }
    }

    val uiState: StateFlow<MainUiState> = combine(service.observeAlarms(), minuteTicks, error) { alarms, now, err ->
        MainUiState(
            isLoaded = true,
            items = alarms.map { alarm ->
                val next = alarm.nextFireTime(now)
                AlarmItem(alarm, next?.let { AlarmSchedule.remaining(now, it) })
            },
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun setEnabled(alarm: Alarm, enabled: Boolean) = run { service.setEnabled(alarm, enabled) }

    fun delete(alarm: Alarm) = run { service.delete(alarm.id) }

    fun dismissError() {
        error.value = null
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = e
            }
        }
    }
}
