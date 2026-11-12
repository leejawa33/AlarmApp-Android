package com.alarmheaven.alarm.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes as PlatformAudioAttributes
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.alarmheaven.alarm.R
import com.alarmheaven.alarm.service.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 알람이 울리는 동안 살아 있는 포그라운드 서비스: 알람 소리(USAGE_ALARM, 반복)와 진동, 그리고
 * 전체 화면 인텐트 알림(→ [com.alarmheaven.alarm.RingActivity])을 맡는다.
 * 미션을 완료하면 [stop]으로 끈다. 알림을 밀어도 서비스는 계속 울린다.
 */
@AndroidEntryPoint
class RingingService : Service() {
    @Inject lateinit var alarmService: AlarmService

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private var currentId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getStringExtra(EXTRA_ALARM_ID)
        if (alarmId == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        // 이미 같은 알람이 울리는 중이면(알림 재전달 등) 다시 시작하지 않는다.
        if (currentId == alarmId) return START_REDELIVER_INTENT
        currentId = alarmId

        RingNotifications.ensureChannel(this)
        // 5초 안에 startForeground를 불러야 하므로 알람을 읽기 전에 먼저 기본 알림으로 시작한다.
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification(alarmId, title = getString(R.string.app_display_name)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        RingingState.start(alarmId)
        startVibration()

        scope.launch {
            val alarm = alarmService.getAlarm(alarmId)
            alarm?.title?.takeIf { it.isNotBlank() }?.let {
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(alarmId, it))
            }
            // 알람을 못 찾아도(삭제됨) 기본 소리로 울린다. 미션 없이 조용해지면 안 된다.
            val catalog = alarmService.catalog
            val sound = (alarm?.soundId?.let(catalog::sound) ?: catalog.defaultSound)?.file
            sound?.let(::startSound)
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        currentId?.let(RingingState::stop)
        scope.cancel()
        player?.release()
        player = null
        vibrator()?.cancel()
        super.onDestroy()
    }

    private fun startSound(file: String) {
        player?.release()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_ALARM).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri("asset:///$file"))
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                prepare()
            }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        vibrator()?.takeIf { it.hasVibrator() }?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 700, 700), 0),
            PlatformAudioAttributes.Builder().setUsage(PlatformAudioAttributes.USAGE_ALARM).build(),
        )
    }

    private fun notification(alarmId: String, title: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, ringActivityIntent(this, alarmId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, RingNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(getString(R.string.alarm_open_app))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .addAction(0, getString(R.string.alarm_open_app), open)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1

        fun intent(context: Context, alarmId: String): Intent =
            Intent(context, RingingService::class.java).putExtra(EXTRA_ALARM_ID, alarmId)

        /** 지금 울리는 알람이 [alarmId]일 때만 멈춘다(다른 알람이 울리고 있으면 건드리지 않는다). */
        fun stop(context: Context, alarmId: String) {
            if (RingingState.alarmId.value == alarmId) context.stopService(Intent(context, RingingService::class.java))
        }
    }
}
