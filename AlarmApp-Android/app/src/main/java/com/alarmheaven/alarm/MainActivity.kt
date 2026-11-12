package com.alarmheaven.alarm

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.alarmheaven.alarm.alarm.RingingState
import com.alarmheaven.alarm.alarm.ringActivityIntent
import com.alarmheaven.alarm.ui.navigation.AlarmNavHost
import com.alarmheaven.alarm.ui.theme.AlarmTheme
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity: Android 12 이하에서도 앱별 언어 설정(AppCompatDelegate)이 동작하도록 사용
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    /** 알람이 울리는 중에 앱을 열면 실행 화면으로 보낸다(미션 없이 앱을 둘러볼 수 없게). */
    override fun onStart() {
        super.onStart()
        RingingState.alarmId.value?.let { startActivity(ringActivityIntent(this, it)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AlarmTheme {
                AlarmNavHost()
            }
        }
    }
}
