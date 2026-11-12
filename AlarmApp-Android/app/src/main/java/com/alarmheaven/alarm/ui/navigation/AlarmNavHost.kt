package com.alarmheaven.alarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alarmheaven.alarm.ui.edit.EditScreen
import com.alarmheaven.alarm.ui.main.MainScreen
import com.alarmheaven.alarm.ui.ring.RingScreen
import com.alarmheaven.alarm.ui.settings.LanguageScreen
import com.alarmheaven.alarm.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

/** 타입 안전 라우트. 인자 이름(`alarmId`)이 EditViewModel의 SavedStateHandle 키가 된다. */
@Serializable
data object Main

/** [alarmId]가 null이면 새 알람 */
@Serializable
data class Edit(val alarmId: String? = null)

/**
 * 편집 화면 미리보기. 편집 중인 값을 그대로 받는다(저장 전 초안).
 * 모든 인자에 기본값을 둬서 빈 값도 쿼리 인자로 안전하게 전달한다. [promptIds]는 쉼표로 이은 id 목록.
 */
@Serializable
data class Preview(
    val title: String = "",
    val mission: String = "",
    val mediaId: String = "",
    val soundId: String = "",
    val promptIds: String = "",
)

@Serializable
data object Settings

@Serializable
data object Language

@Composable
fun AlarmNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Main) {
        composable<Main> {
            MainScreen(
                onAdd = { navController.navigate(Edit()) },
                onEdit = { navController.navigate(Edit(it)) },
                onSettings = { navController.navigate(Settings) },
            )
        }
        composable<Edit> {
            EditScreen(
                onClose = { navController.popBackStack() },
                onPreview = { d ->
                    navController.navigate(
                        Preview(d.title, d.mission.id, d.mediaId, d.soundId, d.promptIds.joinToString(",")),
                    )
                },
            )
        }
        composable<Settings> {
            SettingsScreen(onBack = { navController.popBackStack() }, onLanguage = { navController.navigate(Language) })
        }
        composable<Language> {
            LanguageScreen(onBack = { navController.popBackStack() })
        }
        composable<Preview> {
            RingScreen(onClose = { navController.popBackStack() })
        }
    }
}
