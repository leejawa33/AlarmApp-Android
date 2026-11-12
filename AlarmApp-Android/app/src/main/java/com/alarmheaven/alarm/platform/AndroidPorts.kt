package com.alarmheaven.alarm.platform

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.alarmheaven.alarm.alarm.RingNotifications
import com.alarmheaven.alarm.domain.AppLanguage
import com.alarmheaven.alarm.service.AlarmPermissionStatus
import com.alarmheaven.alarm.service.AppLanguageStore
import com.alarmheaven.alarm.service.PermissionStatusSource

class AndroidPermissionStatusSource(private val context: Context) : PermissionStatusSource {
    override fun current(): AlarmPermissionStatus {
        val manager = context.getSystemService(NotificationManager::class.java)
        // 앱 전체 알림이 꺼졌거나(런타임 권한 거부 포함), 알림 채널을 사용자가 껐으면 울림 알림이 보이지 않는다.
        val channelOff = manager.getNotificationChannel(RingNotifications.CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        return AlarmPermissionStatus(
            notificationsEnabled = manager.areNotificationsEnabled() && !channelOff,
            fullScreenAllowed = Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent(),
        )
    }
}

/**
 * AppCompat 앱별 언어. Android 13+는 시스템이 저장하고, 12 이하는 매니페스트의
 * `AppLocalesMetadataHolderService`(autoStoreLocales)가 저장한다.
 */
class AndroidAppLanguageStore : AppLanguageStore {
    override fun get(): AppLanguage = AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    override fun set(language: AppLanguage) {
        // 빈 목록 = 시스템 언어를 따른다. 설정하면 열려 있는 Activity가 다시 만들어진다.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag ?: ""))
    }
}

/** 문제가 있는 권한의 설정 화면을 연다. 알림이 꺼져 있으면 알림 설정, 아니면 전체 화면 알림 설정. 실패하면 앱 정보 화면. */
fun Context.openAlarmPermissionSettings(status: AlarmPermissionStatus) {
    val packageUri = Uri.fromParts("package", packageName, null)
    val intents = buildList {
        if (!status.notificationsEnabled) {
            add(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        } else if (!status.fullScreenAllowed && Build.VERSION.SDK_INT >= 34) {
            add(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri))
        }
        add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
    }
    for (intent in intents) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
            // 다음 후보로
        }
    }
}
