package com.alarmheaven.alarm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** 앱 진입점. Hilt DI 컨테이너(Spring ApplicationContext에 해당)를 초기화한다. */
@HiltAndroidApp
class AlarmApplication : Application()
