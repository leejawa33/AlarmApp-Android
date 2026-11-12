package com.alarmheaven.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper

/**
 * Hilt는 `super.onReceive()` 안에서 필드를 주입하는데, `BroadcastReceiver.onReceive`가 추상이라
 * Kotlin이 `super` 호출을 막는다. 빈 구현을 가진 중간 클래스를 두어 우회한다(Hilt 공식 권장 방식).
 */
abstract class HiltBroadcastReceiver : BroadcastReceiver() {
    @CallSuper
    override fun onReceive(context: Context, intent: Intent) = Unit
}
