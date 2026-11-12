package com.alarmheaven.alarm.alarm

import android.content.Context
import android.content.Intent
import com.alarmheaven.alarm.service.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 재부팅·앱 업데이트·시간대/시각 변경 뒤에 OS 예약을 DB에 맞춰 다시 만든다(Android는 재부팅하면 예약이 사라진다). */
@AndroidEntryPoint
class BootReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var alarmService: AlarmService

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                runCatching { alarmService.reconcile() }
            } finally {
                pending.finish()
            }
        }
    }
}
