package com.alarmheaven.alarm.app.di

import android.content.Context
import androidx.room.Room
import com.alarmheaven.alarm.adapter.alarm.AndroidAlarmScheduler
import com.alarmheaven.alarm.adapter.catalog.AssetCatalog
import com.alarmheaven.alarm.adapter.catalog.PromptProvider
import com.alarmheaven.alarm.adapter.catalog.assetReader
import com.alarmheaven.alarm.adapter.catalog.loadPrompts
import com.alarmheaven.alarm.adapter.persistence.AlarmDao
import com.alarmheaven.alarm.adapter.persistence.AppDatabase
import com.alarmheaven.alarm.adapter.device.AndroidAppLanguageStore
import com.alarmheaven.alarm.adapter.device.AndroidPermissionStatusSource
import com.alarmheaven.alarm.application.AlarmScheduler
import com.alarmheaven.alarm.application.AppLanguageStore
import com.alarmheaven.alarm.application.PermissionStatusSource
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

    @Provides
    @Singleton
    fun provideScheduler(@ApplicationContext context: Context, clock: Clock): AlarmScheduler =
        AndroidAlarmScheduler(context, clock)

    @Provides
    @Singleton
    fun providePermissionStatusSource(@ApplicationContext context: Context): PermissionStatusSource =
        AndroidPermissionStatusSource(context)

    @Provides
    @Singleton
    fun provideAppLanguageStore(): AppLanguageStore = AndroidAppLanguageStore()
}
