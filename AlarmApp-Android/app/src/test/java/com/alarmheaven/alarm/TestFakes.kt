package com.alarmheaven.alarm

import com.alarmheaven.alarm.data.AlarmDao
import com.alarmheaven.alarm.data.AlarmEntity
import com.alarmheaven.alarm.domain.Alarm
import com.alarmheaven.alarm.service.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** 메모리 DAO. Room 없이 JVM에서 서비스·ViewModel 로직만 검증한다. */
class FakeAlarmDao : AlarmDao {
    val rows = MutableStateFlow<Map<String, AlarmEntity>>(emptyMap())
    private fun sorted() = rows.value.values.sortedWith(compareBy({ it.hour }, { it.minute }, { it.createdAt }))

    override fun observeAll(): Flow<List<AlarmEntity>> = rows.map { sorted() }
    override suspend fun getAll() = sorted()
    override suspend fun getById(id: String) = rows.value[id]
    override suspend fun count() = rows.value.size
    override suspend fun upsert(alarm: AlarmEntity) { rows.value = rows.value + (alarm.id to alarm) }
    override suspend fun deleteById(id: String) { rows.value = rows.value - id }
}

class FakeScheduler : AlarmScheduler {
    val scheduled = mutableMapOf<String, Alarm>()
    val stopped = mutableListOf<String>()
    var failWith: Exception? = null

    override suspend fun schedule(alarm: Alarm) {
        failWith?.let { throw it }
        scheduled[alarm.id] = alarm
    }

    override suspend fun cancel(alarmId: String) {
        failWith?.let { throw it }
        scheduled.remove(alarmId)
    }

    override suspend fun stopRinging(alarmId: String) {
        failWith?.let { throw it }
        stopped += alarmId
    }
}
