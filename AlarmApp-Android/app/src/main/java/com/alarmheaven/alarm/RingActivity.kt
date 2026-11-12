package com.alarmheaven.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alarmheaven.alarm.ui.ring.RingScreen
import com.alarmheaven.alarm.ui.theme.AlarmTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 알람이 울릴 때 잠금 화면 위로 뜨는 실행 화면. 인텐트의 `alarmId`가 `RingViewModel`의 SavedStateHandle로 전달된다.
 * 뒤로가기는 막혀 있고, 미션을 완료해야 닫힌다.
 */
@AndroidEntryPoint
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AlarmTheme { RingScreen(onClose = ::finish) } }
    }
}
