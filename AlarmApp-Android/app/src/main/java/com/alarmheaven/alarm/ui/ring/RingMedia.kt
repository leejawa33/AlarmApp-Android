package com.alarmheaven.alarm.ui.ring

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.alarmheaven.alarm.domain.ShakeCounter

private fun assetItem(file: String) = MediaItem.fromUri("asset:///$file")

/** 음소거 반복 영상. 오디오 포커스를 요청하지 않아서 알람 소리를 끊지 않는다. */
@OptIn(UnstableApi::class)
@Composable
fun LoopingVideo(file: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(assetItem(file))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) { onDispose(player::release) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // 화면을 꽉 채운다(1080×1920)
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player = player
            }
        },
        update = { it.player = player },
    )
}

/**
 * 알람 소리 반복 재생. 알람용 오디오 속성(USAGE_ALARM)이라 무음·진동 모드에서도 울린다.
 * 컴포지션을 떠나면 멈추고 해제한다. (실제 울림 단계에서는 포그라운드 서비스가 이 역할을 맡는다.)
 */
@Composable
fun RingSound(file: String?, playing: Boolean) {
    val context = LocalContext.current
    DisposableEffect(file, playing) {
        if (file == null || !playing) return@DisposableEffect onDispose {}
        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_ALARM)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .apply {
                setMediaItem(assetItem(file))
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                prepare()
            }
        onDispose(player::release)
    }
}

/** 선형 가속도 센서(중력 제외)로 흔들기를 센다. 센서가 없으면 false를 돌려주고 아무것도 등록하지 않는다. */
fun Context.hasShakeSensor(): Boolean =
    (getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null

@Composable
fun ShakeSensor(counter: ShakeCounter, onShake: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(counter) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble())
                if (counter.feed(magnitude, event.timestamp / 1e9)) onShake()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
}
