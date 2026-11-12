package com.alarmheaven.alarm.service

import com.alarmheaven.alarm.domain.Alarm

/**
 * OS 알람 예약 추상화 (iOS의 `AlarmScheduling`). 구현체는 `AlarmManager.setAlarmClock()`을 쓴다(다음 단계).
 * DB가 원본이고 이 예약은 파생이다.
 */
interface AlarmScheduler {
    /** 켜진 알람의 다음 울림을 예약한다(같은 id면 덮어씀 = upsert). */
    suspend fun schedule(alarm: Alarm)

    /** 예약을 취소한다. 예약이 없어도 오류가 아니다. */
    suspend fun cancel(alarmId: String)

    /** 지금 울리는 알람을 멈춘다. 반복 예약은 건드리지 않는다. */
    suspend fun stopRinging(alarmId: String)
}

/** 실제 스케줄러를 만들기 전까지 쓰는 자리표시자. 아무것도 예약하지 않는다. */
class NoOpAlarmScheduler : AlarmScheduler {
    override suspend fun schedule(alarm: Alarm) = Unit
    override suspend fun cancel(alarmId: String) = Unit
    override suspend fun stopRinging(alarmId: String) = Unit
}
