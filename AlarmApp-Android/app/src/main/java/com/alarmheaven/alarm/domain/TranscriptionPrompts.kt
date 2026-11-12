package com.alarmheaven.alarm.domain

import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * `Copy/transcription_prompts.json` — 따라쓰기 미션 문구(카테고리별, 한/영)와 판정 규칙.
 * 같은 id에 언어별 문장이 있어서, 알람의 문장 선택은 앱 언어를 바꿔도 유지된다.
 */
@Serializable
data class TranscriptionPrompts(
    val version: Int,
    val languages: List<String>,
    val categories: List<PromptCategory>,
    val prompts: List<TranscriptionPrompt>,
    val rules: Rules,
) {
    @Serializable
    data class Rules(
        val match: String,
        val trimWhitespace: Boolean,
        val caseSensitive: Boolean,
    )

    val allIds: List<String> get() = prompts.map { it.id }

    fun prompts(category: PromptCategory): List<TranscriptionPrompt> =
        prompts.filter { it.category == category.id }

    /** 판정: 앱 언어의 문장과 (trim 후) 완전 일치. 대소문자 구분 여부는 [rules]를 따른다. */
    fun isMatch(input: String, prompt: TranscriptionPrompt, language: String): Boolean {
        fun normalize(text: String): String {
            val trimmed = if (rules.trimWhitespace) text.trim() else text
            return if (rules.caseSensitive) trimmed else trimmed.lowercase()
        }
        return normalize(input) == normalize(prompt.text(language))
    }

    /**
     * 알람이 고른 문장([selectedIds]) 중 무작위 하나.
     * 비어 있거나(= 전체) 고른 문장이 모두 사라졌으면 전체에서 고른다 — 미션 없이 꺼지면 안 된다.
     */
    fun randomPrompt(selectedIds: List<String> = emptyList(), random: Random = Random.Default): TranscriptionPrompt? =
        PromptSelection.resolve(selectedIds, this).randomOrNull(random)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): TranscriptionPrompts = json.decodeFromString(text)
    }
}

@Serializable
data class PromptCategory(
    val id: String,
    /** 언어 코드 → 표시 이름 (예: ko → 자기 확언, en → Self-affirmation) */
    val displayName: Map<String, String>,
) {
    /** 앱 언어 이름, 없으면 한국어, 그것도 없으면 id */
    fun name(language: String): String = displayName[language] ?: displayName["ko"] ?: id
}

@Serializable
data class TranscriptionPrompt(
    val id: String,
    val category: String,
    /** 언어 코드 → 문장 (JSON 키는 `text`) */
    @SerialName("text") val texts: Map<String, String>,
) {
    /** 앱 언어 문장, 없으면 한국어 */
    fun text(language: String): String = texts[language] ?: texts["ko"] ?: ""
}

/** 알람별 미션 문장 선택 규칙. 저장값이 빈 리스트면 "전체"(새로 추가된 문장도 자동 포함). */
object PromptSelection {
    /** 저장값 → 실제 출제 후보. 모르는 id는 버리고, 남는 게 없으면 전체. */
    fun resolve(stored: List<String>, prompts: TranscriptionPrompts): List<TranscriptionPrompt> {
        val ids = stored.toSet()
        val chosen = prompts.prompts.filter { it.id in ids }
        return chosen.ifEmpty { prompts.prompts }
    }

    /** 화면에 체크로 보여 줄 id 집합 */
    fun checkedIds(stored: List<String>, prompts: TranscriptionPrompts): Set<String> =
        resolve(stored, prompts).map { it.id }.toSet()

    /** 선택 화면 결과 → 저장값. 전부 골랐으면 빈 리스트(전체)로 저장한다. */
    fun normalize(checked: Set<String>, prompts: TranscriptionPrompts): List<String> {
        val valid = prompts.allIds.filter { it in checked } // 문구 파일 순서 유지
        return if (valid.size == prompts.prompts.size) emptyList() else valid
    }
}
