package com.alarmheaven.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.alarmheaven.alarm.MainActivity
import com.alarmheaven.alarm.domain.Alarm
import com.alarmheaven.alarm.service.AlarmScheduler
import java.time.Clock
import java.time.ZonedDateTime

/**
 * `AlarmManager.setAlarmClock()`으로 **다음 울림 한 번**을 예약한다(Doze에서도 정확하고, 알람 시계 API라 별도 권한이 필요 없다).
 * 반복은 울린 직후 [AlarmReceiver]가 다음 울림을 다시 예약해서 만든다.
 * 알람마다 PendingIntent가 하나이고, 같은 id를 다시 예약하면 교체된다(upsert).
 */
class AndroidAlarmScheduler(
    private val context: Context,
    private val clock: Clock,
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun schedule(alarm: Alarm) {
        val next = alarm.nextFireTime(ZonedDateTime.now(clock)) ?: return cancel(alarm.id)
        // 상태바·잠금 화면의 알람 아이콘을 누르면 앱이 열린다.
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(next.toInstant().toEpochMilli(), show),
            firePendingIntent(alarm.id, PendingIntent.FLAG_UPDATE_CURRENT)!!,
        )
    }

    /** 예약만 취소한다. 지금 울리고 있는 소리는 멈추지 않는다(미션 없이 꺼지면 안 된다). */
    override suspend fun cancel(alarmId: String) {
        firePendingIntent(alarmId, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    override suspend fun stopRinging(alarmId: String) = RingingService.stop(context, alarmId)

    /** 알람 id를 data URI로 구분한다(같은 id → 같은 PendingIntent). requestCode 해시 충돌을 피한다. */
    private fun firePendingIntent(alarmId: String, flag: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java)
                .setAction(AlarmReceiver.ACTION_FIRE)
                .setData(Uri.parse("alarm://$alarmId"))
                .putExtra(EXTRA_ALARM_ID, alarmId),
            flag or PendingIntent.FLAG_IMMUTABLE,
        )
}
