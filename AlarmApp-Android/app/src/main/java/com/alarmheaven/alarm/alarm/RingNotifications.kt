package com.alarmheaven.alarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.alarmheaven.alarm.R

/** 알람 울림 알림 채널. 앱 시작 때도 만들어 두어야 사용자가 설정에서 끈 상태를 미리 알아챌 수 있다. */
object RingNotifications {
    const val CHANNEL_ID = "alarm_ringing"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null) // 소리는 서비스가 직접 낸다
            enableVibration(false)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
