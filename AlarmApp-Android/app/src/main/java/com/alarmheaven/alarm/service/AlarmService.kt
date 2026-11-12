package com.alarmheaven.alarm.service

import com.alarmheaven.alarm.catalog.AssetCatalog
import com.alarmheaven.alarm.data.AlarmDao
import com.alarmheaven.alarm.data.toDomain
import com.alarmheaven.alarm.data.toEntity
import com.alarmheaven.alarm.domain.Alarm
import com.alarmheaven.alarm.domain.AlarmDraft
import com.alarmheaven.alarm.domain.AlarmValidationError
import com.alarmheaven.alarm.domain.MissionType
import com.alarmheaven.alarm.domain.Weekday
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DB에는 저장됐지만 OS 예약에 실패함. 다음 [AlarmService.reconcile]에서 다시 맞춘다. */
class SchedulingFailedException(cause: Throwable) : Exception("알람 예약에 실패했습니다", cause)

/**
 * 알람 변경의 단일 진입점(화면에서 DAO를 직접 수정하지 않는다). DB(원본) → OS 예약(파생) 순서로 갱신한다.
 *
 * DB와 OS 스케줄러를 하나의 트랜잭션으로 묶을 수 없으므로:
 * 1. DB에 먼저 저장(커밋)하고
 * 2. 스케줄러에 반영한다. 실패하면 [SchedulingFailedException]을 던지되 데이터는 남기고,
 * 3. 앱 시작·재부팅 때 [reconcile]이 둘을 다시 맞춘다.
 */
@Singleton
class AlarmService @Inject constructor(
    private val dao: AlarmDao,
    val catalog: AssetCatalog,
    private val scheduler: AlarmScheduler,
) {
    companion object {
        const val MAX_ALARMS = 10
    }

    // MARK: 조회

    fun observeAlarms(): Flow<List<Alarm>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAlarm(id: String): Alarm? = dao.getById(id)?.toDomain()

    suspend fun canAddAlarm(): Boolean = dao.count() < MAX_ALARMS

    /** 새 알람 기본값: 미션 = 흔들기, 미디어·소리 = 카탈로그 default. 타이틀은 비워 두어 입력을 받는다. */
    fun makeDefaultDraft() = AlarmDraft(
        title = "",
        hour = 7,
        minute = 0,
        weekdays = Weekday.weekdaysOnly,
        mission = MissionType.SHAKE,
        mediaId = catalog.defaultMedia?.id ?: catalog.media.firstOrNull()?.id.orEmpty(),
        soundId = catalog.defaultSound?.id ?: catalog.sounds.firstOrNull()?.id.orEmpty(),
    )

    // MARK: 변경

    suspend fun create(draft: AlarmDraft): Alarm {
        if (!canAddAlarm()) throw AlarmValidationError.LimitReached(MAX_ALARMS)
        val valid = validate(draft)
        val alarm = Alarm(
            title = valid.title, hour = valid.hour, minute = valid.minute, weekdays = valid.weekdays,
            isEnabled = valid.isEnabled, mission = valid.mission, mediaId = valid.mediaId,
            soundId = valid.soundId, promptIds = valid.promptIds,
        )
        dao.upsert(alarm.toEntity())
        sync(alarm)
        return alarm
    }

    suspend fun update(alarm: Alarm, draft: AlarmDraft): Alarm {
        val valid = validate(draft)
        val updated = alarm.copy(
            title = valid.title, hour = valid.hour, minute = valid.minute, weekdays = valid.weekdays,
            mission = valid.mission, mediaId = valid.mediaId, soundId = valid.soundId,
            promptIds = valid.promptIds, isEnabled = valid.isEnabled,
        )
        dao.upsert(updated.toEntity())
        sync(updated)
        return updated
    }

    suspend fun setEnabled(alarm: Alarm, enabled: Boolean): Alarm {
        val updated = alarm.copy(isEnabled = enabled)
        dao.upsert(updated.toEntity())
        sync(updated)
        return updated
    }

    suspend fun delete(alarmId: String) {
        dao.deleteById(alarmId)
        try {
            scheduler.cancel(alarmId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw SchedulingFailedException(e)
        }
    }

    /**
     * 미션 완료: 울리는 알람만 멈춘다. 반복 예약은 그대로 남는다.
     * 정지에 실패해도 미션은 끝난 것으로 처리한다(계속 울리면 실행 화면이 다시 뜬다).
     */
    suspend fun completeMission(alarmId: String) {
        try {
            scheduler.stopRinging(alarmId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    /**
     * DB 기준으로 OS 예약을 다시 맞춘다. 앱 시작·재부팅 후 호출한다
     * (Android는 재부팅하면 예약이 사라진다).
     * 켜진 알람은 모두 다시 schedule(upsert), 꺼진 알람은 cancel.
     */
    suspend fun reconcile() {
        for (entity in dao.getAll()) {
            val alarm = entity.toDomain()
            if (alarm.isEnabled) scheduler.schedule(alarm) else scheduler.cancel(alarm.id)
        }
    }

    // MARK: 내부

    private fun validate(draft: AlarmDraft): AlarmDraft {
        val title = draft.title.trim()
        if (title.isEmpty()) throw AlarmValidationError.EmptyTitle
        if (draft.weekdays.isEmpty()) throw AlarmValidationError.NoWeekday
        if (draft.hour !in 0..23 || draft.minute !in 0..59) throw AlarmValidationError.InvalidTime
        if (catalog.media(draft.mediaId) == null) throw AlarmValidationError.UnknownMedia(draft.mediaId)
        if (catalog.sound(draft.soundId) == null) throw AlarmValidationError.UnknownSound(draft.soundId)
        return draft.copy(title = title)
    }

    private suspend fun sync(alarm: Alarm) {
        try {
            if (alarm.isEnabled) scheduler.schedule(alarm) else scheduler.cancel(alarm.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw SchedulingFailedException(e)
        }
    }
}
