package com.alarmheaven.alarm

import android.app.Application
import com.alarmheaven.alarm.alarm.RingNotifications
import com.alarmheaven.alarm.service.AlarmService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 앱 진입점. Hilt DI 컨테이너(Spring ApplicationContext에 해당)를 초기화하고, 시작할 때 알람 예약을 DB에 맞춘다. */
@HiltAndroidApp
class AlarmApplication : Application() {
    @Inject lateinit var alarmService: AlarmService

    override fun onCreate() {
        super.onCreate()
        RingNotifications.ensureChannel(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { runCatching { alarmService.reconcile() } }
    }
}
