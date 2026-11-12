package com.alarmheaven.alarm.service

import com.alarmheaven.alarm.domain.AppLanguage

/**
 * 알람이 제대로 울리기 위한 권한 상태. 둘 다 켜져 있어야 정상이다.
 * - 알림: 꺼져 있으면 울림 서비스의 알림·전체 화면 알림이 모두 보이지 않는다.
 * - 전체 화면 알림(Android 14+): 꺼져 있으면 잠금 화면에서 실행 화면이 자동으로 뜨지 않고 알림만 뜬다.
 * 정확한 알람 권한은 필요 없다(`setAlarmClock`, `USE_EXACT_ALARM`).
 */
data class AlarmPermissionStatus(
    val notificationsEnabled: Boolean,
    val fullScreenAllowed: Boolean,
) {
    val isOk: Boolean get() = notificationsEnabled && fullScreenAllowed

    companion object {
        val Ok = AlarmPermissionStatus(notificationsEnabled = true, fullScreenAllowed = true)
    }
}

/** 현재 권한 상태를 읽는다(화면이 다시 보일 때마다 새로 읽는다). 구현은 `platform/`, 테스트는 가짜. */
fun interface PermissionStatusSource {
    fun current(): AlarmPermissionStatus
}

/** 앱 언어 저장소. 구현은 AppCompat 앱별 언어(`platform/`), 테스트는 가짜. */
interface AppLanguageStore {
    fun get(): AppLanguage
    fun set(language: AppLanguage)
}
