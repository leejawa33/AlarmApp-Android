package com.alarmheaven.alarm.ui

import com.alarmheaven.alarm.FakeLanguageStore
import com.alarmheaven.alarm.FakePermissions
import com.alarmheaven.alarm.domain.AppLanguage
import com.alarmheaven.alarm.service.AlarmPermissionStatus
import com.alarmheaven.alarm.ui.settings.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    private val languages = FakeLanguageStore()
    private val permissions = FakePermissions()
    private val vm = SettingsViewModel(languages, permissions)

    @Test
    fun startsWithStoredLanguageAndPermission() {
        val saved = SettingsViewModel(FakeLanguageStore(AppLanguage.ENGLISH), FakePermissions())
        assertEquals(AppLanguage.ENGLISH, saved.state.value.language)
        assertTrue(saved.state.value.permission.isOk)
    }

    @Test
    fun setLanguageAppliesAndKeeps() {
        vm.setLanguage(AppLanguage.KOREAN)
        assertEquals(AppLanguage.KOREAN, languages.language)
        assertEquals(AppLanguage.KOREAN, vm.state.value.language)
        vm.setLanguage(AppLanguage.SYSTEM)
        assertEquals(AppLanguage.SYSTEM, languages.language)
    }

    @Test
    fun refreshRereadsPermissionAfterReturningFromSystemSettings() {
        permissions.status = AlarmPermissionStatus(notificationsEnabled = false, fullScreenAllowed = true)
        assertTrue(vm.state.value.permission.isOk) // 아직 읽기 전
        vm.refresh()
        assertFalse(vm.state.value.permission.isOk)
        permissions.status = AlarmPermissionStatus.Ok
        vm.refresh()
        assertTrue(vm.state.value.permission.isOk)
    }

    @Test
    fun languageTagsMapToChoices() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromTag("ko"))
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromTag("ko-KR"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("ja"))
    }

    @Test
    fun permissionStatusIsOkOnlyWhenBothAreOn() {
        assertFalse(AlarmPermissionStatus(true, false).isOk)
        assertFalse(AlarmPermissionStatus(false, true).isOk)
        assertTrue(AlarmPermissionStatus(true, true).isOk)
    }
}
