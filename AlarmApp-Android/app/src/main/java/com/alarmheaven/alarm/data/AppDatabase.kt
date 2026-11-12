package com.alarmheaven.alarm.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        const val NAME = "alarm.db"
    }
}
