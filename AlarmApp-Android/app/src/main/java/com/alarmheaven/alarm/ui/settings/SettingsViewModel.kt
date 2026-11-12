package com.alarmheaven.alarm.ui.settings

import androidx.lifecycle.ViewModel
import com.alarmheaven.alarm.domain.AppLanguage
import com.alarmheaven.alarm.service.AlarmPermissionStatus
import com.alarmheaven.alarm.service.AppLanguageStore
import com.alarmheaven.alarm.service.PermissionStatusSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val language: AppLanguage,
    val permission: AlarmPermissionStatus,
)

/** 설정(언어·권한 안내)과 언어 선택 화면이 함께 쓴다. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languages: AppLanguageStore,
    private val permissions: PermissionStatusSource,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(languages.get(), permissions.current()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** 화면이 다시 보일 때마다 부른다(설정 앱이나 언어 화면에서 돌아온 직후). */
    fun refresh() = _state.update { SettingsUiState(languages.get(), permissions.current()) }

    /** 고르면 바로 적용된다(열려 있는 화면이 새 언어로 다시 그려진다). */
    fun setLanguage(language: AppLanguage) {
        languages.set(language)
        _state.update { it.copy(language = language) }
    }
}
