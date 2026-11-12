package com.alarmheaven.alarm.catalog

import android.content.Context
import com.alarmheaven.alarm.domain.TranscriptionPrompts
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * `assets/assets_catalog.json` — 미디어·소리·미션 목록의 원본. 코드에 하드코딩하지 않고 읽어 쓴다.
 * DB에는 저장하지 않고, 알람은 id만 가진다. `.caf`(alarmFile)는 iOS 전용이라 무시한다.
 */
@Serializable
data class AssetCatalog(
    val appDisplayName: String,
    val media: List<Media>,
    val sounds: List<Sound>,
    val missions: List<Mission>,
) {
    @Serializable
    data class Media(
        val id: String,
        /** assets/ 아래 경로 (예: Media/media_puppy_crack.mp4) */
        val file: String,
        /** 한국어 이름. 표시명은 앱 언어를 따라 strings에서 id로 찾고, 없을 때만 이 값을 쓴다. */
        val displayName: String,
        @SerialName("default") val isDefault: Boolean = false,
    )

    @Serializable
    data class Sound(
        val id: String,
        val file: String,
        val displayName: String,
        @SerialName("default") val isDefault: Boolean = false,
    )

    @Serializable
    data class Mission(
        val id: String,
        val defaultCount: Int? = null,
        val promptsFile: String? = null,
    )

    val defaultMedia: Media? get() = media.firstOrNull { it.isDefault }
    val defaultSound: Sound? get() = sounds.firstOrNull { it.isDefault }

    fun media(id: String): Media? = media.firstOrNull { it.id == id }
    fun sound(id: String): Sound? = sounds.firstOrNull { it.id == id }

    /** 흔들기 횟수(카탈로그 `defaultCount`, 없으면 10) */
    val shakeCount: Int get() = missions.firstOrNull { it.id == "shake" }?.defaultCount ?: DEFAULT_SHAKE_COUNT

    /** 따라쓰기 문장 파일 경로 */
    val transcriptionPromptsFile: String? get() = missions.firstOrNull { it.id == "transcription" }?.promptsFile

    companion object {
        const val FILE = "assets_catalog.json"
        const val DEFAULT_SHAKE_COUNT = 10

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): AssetCatalog = json.decodeFromString(text)

        /** [readText]: assets 기준 상대 경로 → 파일 내용 (앱에서는 AssetManager, 테스트에서는 파일) */
        fun load(readText: (String) -> String): AssetCatalog = parse(readText(FILE))

        fun load(context: Context): AssetCatalog = load(context.assetReader())
    }
}

/** 따라쓰기 문장을 읽는다. 파일이 없거나 깨졌으면 null — 호출하는 쪽에서 흔들기 등으로 대체한다. */
fun AssetCatalog.loadPrompts(readText: (String) -> String): TranscriptionPrompts? {
    val path = transcriptionPromptsFile ?: return null
    return runCatching { TranscriptionPrompts.parse(readText(path)) }.getOrNull()
}

/** 따라쓰기 문장을 한 번만 읽어 둔다. 문구 파일이 없으면 null (편집 화면은 문장 선택을 숨긴다). */
class PromptProvider(loader: () -> TranscriptionPrompts?) {
    val prompts: TranscriptionPrompts? by lazy(loader)
}

fun Context.assetReader(): (String) -> String = { path ->
    assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
