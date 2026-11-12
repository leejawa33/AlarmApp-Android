package com.alarmheaven.alarm.domain

import java.time.ZonedDateTime
import java.util.UUID

/** 알람 도메인 모델(불변). DB 엔티티는 `data.AlarmEntity`. Spring의 Entity ↔ DTO 분리와 같다. */
data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val hour: Int,
    val minute: Int,
    val weekdays: Set<Weekday>,
    val isEnabled: Boolean = true,
    val mission: MissionType,
    /** `assets_catalog.json`의 media[].id / sounds[].id */
    val mediaId: String,
    val soundId: String,
    /** 따라쓰기 미션 문장 id. 빈 리스트 = 전체 ([PromptSelection]) */
    val promptIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    val schedule: AlarmSchedule get() = AlarmSchedule(hour, minute, weekdays)

    /** 꺼진 알람은 null → 리스트에서 "다음 울림까지"를 숨긴다. */
    fun nextFireTime(now: ZonedDateTime): ZonedDateTime? = if (isEnabled) schedule.nextFireTime(now) else null

    /** 편집 화면 초기값 */
    fun toDraft() = AlarmDraft(title, hour, minute, weekdays, mission, mediaId, soundId, promptIds, isEnabled)
}

/** 알람 생성·수정 시 사용자가 입력하는 값. */
data class AlarmDraft(
    val title: String,
    val hour: Int,
    val minute: Int,
    val weekdays: Set<Weekday>,
    val mission: MissionType,
    val mediaId: String,
    val soundId: String,
    val promptIds: List<String> = emptyList(),
    val isEnabled: Boolean = true,
)

sealed class AlarmValidationError(message: String) : Exception(message) {
    data class LimitReached(val max: Int) : AlarmValidationError("알람은 최대 ${max}개까지 만들 수 있습니다")
    data object EmptyTitle : AlarmValidationError("타이틀이 비어 있습니다")
    data object NoWeekday : AlarmValidationError("요일을 1개 이상 선택해야 합니다")
    data object InvalidTime : AlarmValidationError("시각이 올바르지 않습니다")
    data class UnknownMedia(val id: String) : AlarmValidationError("알 수 없는 미디어: $id")
    data class UnknownSound(val id: String) : AlarmValidationError("알 수 없는 소리: $id")
}
