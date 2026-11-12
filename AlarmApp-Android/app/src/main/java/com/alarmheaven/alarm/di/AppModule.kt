package com.alarmheaven.alarm.di

import android.content.Context
import androidx.room.Room
import com.alarmheaven.alarm.catalog.AssetCatalog
import com.alarmheaven.alarm.catalog.PromptProvider
import com.alarmheaven.alarm.catalog.assetReader
import com.alarmheaven.alarm.catalog.loadPrompts
import com.alarmheaven.alarm.data.AlarmDao
import com.alarmheaven.alarm.data.AppDatabase
import com.alarmheaven.alarm.service.AlarmScheduler
import com.alarmheaven.alarm.service.NoOpAlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** Spring의 @Configuration + @Bean에 해당. 앱 전체(싱글톤)에서 쓰는 객체를 만든다. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()

    @Provides
    @Singleton
    fun provideCatalog(@ApplicationContext context: Context): AssetCatalog = AssetCatalog.load(context)

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun providePromptProvider(@ApplicationContext context: Context, catalog: AssetCatalog): PromptProvider =
        PromptProvider { catalog.loadPrompts(context.assetReader()) }

    // TODO(알람 예약 단계): AlarmManager.setAlarmClock() 구현체로 교체
    @Provides
    @Singleton
    fun provideScheduler(): AlarmScheduler = NoOpAlarmScheduler()
}
