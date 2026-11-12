package com.alarmheaven.alarm.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Spring Data의 Repository 인터페이스에 해당. 구현체는 Room이 컴파일 타임에 생성한다. */
@Dao
interface AlarmDao {
    /** 리스트 화면용. 테이블이 바뀔 때마다 새 목록을 흘려 준다. */
    @Query("SELECT * FROM alarms ORDER BY hour, minute, createdAt")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour, minute, createdAt")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: String): AlarmEntity?

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: String)
}
