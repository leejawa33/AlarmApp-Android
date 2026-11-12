package com.alarmheaven.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.alarmheaven.alarm.domain.Alarm
import com.alarmheaven.alarm.domain.MissionType
import com.alarmheaven.alarm.domain.Weekday

/** 알람 테이블(원본). 예약은 여기서 파생된다. 직접 수정하지 말고 `AlarmService`를 통해 변경할 것. */
@Entity(tableName = "alarms")
@TypeConverters(StringListConverter::class)
data class AlarmEntity(
    @PrimaryKey val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    /** [Weekday] 비트마스크 */
    val weekdayMask: Int,
    val isEnabled: Boolean,
    /** [MissionType.id] */
    val mission: String,
    val mediaId: String,
    val soundId: String,
    /** 따라쓰기 문장 id. 빈 리스트 = 전체 */
    val promptIds: List<String>,
    val createdAt: Long,
)

/** 문장 id에는 쉼표가 없다는 전제로 쉼표 구분 문자열로 저장한다. */
class StringListConverter {
    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")
}

fun AlarmEntity.toDomain() = Alarm(
    id = id,
    title = title,
    hour = hour,
    minute = minute,
    weekdays = Weekday.set(weekdayMask),
    isEnabled = isEnabled,
    mission = MissionType.fromId(mission),
    mediaId = mediaId,
    soundId = soundId,
    promptIds = promptIds,
    createdAt = createdAt,
)

fun Alarm.toEntity() = AlarmEntity(
    id = id,
    title = title,
    hour = hour,
    minute = minute,
    weekdayMask = Weekday.mask(weekdays),
    isEnabled = isEnabled,
    mission = mission.id,
    mediaId = mediaId,
    soundId = soundId,
    promptIds = promptIds,
    createdAt = createdAt,
)
