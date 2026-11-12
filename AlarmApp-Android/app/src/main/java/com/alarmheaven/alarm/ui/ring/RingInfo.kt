package com.alarmheaven.alarm.ui.ring

import com.alarmheaven.alarm.catalog.AssetCatalog
import com.alarmheaven.alarm.domain.MissionType
import java.time.LocalTime
import java.util.Locale

/** 실행 화면이 무엇을 보여 줄지 (알람·초안 → 카탈로그 리소스 해석 결과). iOS `RingInfo`. */
data class RingInfo(
    val title: String,
    val mission: MissionType,
    /** assets 기준 경로 (예: Media/media_puppy_crack.mp4). 없으면 검은 배경 */
    val mediaFile: String?,
    val soundFile: String?,
    val shakeCount: Int,
    /** 알람이 고른 따라쓰기 문장 id (빈 리스트 = 전체) */
    val promptIds: List<String>,
) {
    companion object {
        /**
         * 알 수 없는 미디어·소리 id는 카탈로그 기본값으로 대체한다.
         * 미션 없이 꺼지면 안 되므로, 따라쓰기 문구를 못 읽는 경우의 흔들기 대체는 ViewModel이 맡는다.
         */
        fun resolve(
            catalog: AssetCatalog,
            title: String,
            mission: MissionType,
            mediaId: String?,
            soundId: String?,
            promptIds: List<String>,
        ): RingInfo = RingInfo(
            title = title.trim(),
            mission = mission,
            mediaFile = (mediaId?.let(catalog::media) ?: catalog.defaultMedia)?.file,
            soundFile = (soundId?.let(catalog::sound) ?: catalog.defaultSound)?.file,
            shakeCount = catalog.shakeCount,
            promptIds = promptIds,
        )
    }
}

/** 실행 화면 큰 시각: 레퍼런스처럼 24시간제 "HH:mm" (오전/오후 없이) */
fun formatClock(time: LocalTime): String = String.format(Locale.ROOT, "%02d:%02d", time.hour, time.minute)
