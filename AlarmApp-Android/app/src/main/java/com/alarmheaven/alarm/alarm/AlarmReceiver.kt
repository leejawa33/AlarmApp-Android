package com.alarmheaven.alarm.alarm

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.alarmheaven.alarm.service.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 알람 시각에 OS가 호출한다. 울림 서비스를 시작하고, 반복을 위해 다음 울림을 다시 예약한다. */
@AndroidEntryPoint
class AlarmReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var alarmService: AlarmService

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent) // Hilt 주입
        if (intent.action != ACTION_FIRE) return
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return
        // 포그라운드 서비스 시작은 알람 수신 직후 허용되는 시간 안에 바로 한다.
        ContextCompat.startForegroundService(context, RingingService.intent(context, alarmId))

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                alarmService.onFired(alarmId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.alarmheaven.alarm.action.FIRE"
    }
}
