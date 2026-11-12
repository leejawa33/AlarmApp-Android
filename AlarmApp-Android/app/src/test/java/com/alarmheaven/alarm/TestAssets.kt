package com.alarmheaven.alarm

import com.alarmheaven.alarm.catalog.AssetCatalog
import com.alarmheaven.alarm.domain.TranscriptionPrompts
import java.io.File

/** JVM 단위 테스트에서 `src/main/assets`를 직접 읽는다(Gradle 테스트의 작업 디렉터리 = app 모듈). */
object TestAssets {
    val dir = File("src/main/assets")

    val readText: (String) -> String = { path -> File(dir, path).readText(Charsets.UTF_8) }

    fun catalog(): AssetCatalog = AssetCatalog.load(readText)

    fun prompts(): TranscriptionPrompts =
        TranscriptionPrompts.parse(readText(catalog().transcriptionPromptsFile!!))
}
