package com.alarmheaven.alarm.domain

import com.alarmheaven.alarm.TestAssets
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionPromptsTest {
    private val prompts = TestAssets.prompts()

    @Test
    fun loadsTwelvePromptsInFourCategories() {
        assertEquals(4, prompts.categories.size)
        assertEquals(12, prompts.prompts.size)
        prompts.categories.forEach { assertEquals(3, prompts.prompts(it).size) }
        // 모든 문장에 한/영이 있다
        prompts.prompts.forEach {
            assertTrue(it.id, it.texts["ko"].orEmpty().isNotEmpty())
            assertTrue(it.id, it.texts["en"].orEmpty().isNotEmpty())
        }
    }

    @Test
    fun matchTrimsWhitespaceButIsCaseSensitive() {
        val prompt = prompts.prompts.first()
        val ko = prompt.text("ko")
        val en = prompt.text("en")
        assertTrue(prompts.isMatch("  $ko \n", prompt, "ko"))
        assertTrue(prompts.isMatch(en, prompt, "en"))
        assertFalse(prompts.isMatch(en.lowercase(), prompt, "en")) // 대소문자 구분
        assertFalse(prompts.isMatch(ko.dropLast(1), prompt, "ko"))
        assertFalse("앱 언어 문장으로만 판정", prompts.isMatch(ko, prompt, "en"))
    }

    @Test
    fun emptyMeansAll() {
        assertEquals(prompts.prompts.size, PromptSelection.resolve(emptyList(), prompts).size)
        assertEquals(prompts.allIds.toSet(), PromptSelection.checkedIds(emptyList(), prompts))
    }

    @Test
    fun picksOnlyFromSelected() {
        val allowed = setOf("love_01", "study_02")
        repeat(30) {
            val p = prompts.randomPrompt(listOf("love_01", "study_02"), Random(it))
            assertTrue(p?.id in allowed)
        }
    }

    /** 문구 파일에서 지워진 문장만 골라 뒀어도 미션은 나와야 한다 */
    @Test
    fun unknownIdsFallBackToAll() {
        assertEquals(prompts.prompts.size, PromptSelection.resolve(listOf("deleted_99"), prompts).size)
        assertNotNull(prompts.randomPrompt(listOf("deleted_99")))
    }

    @Test
    fun selectingEverythingIsStoredAsAll() {
        assertEquals(emptyList<String>(), PromptSelection.normalize(prompts.allIds.toSet(), prompts))
        // 문구 파일 순서 유지
        assertEquals(listOf("love_01", "study_02"), PromptSelection.normalize(setOf("study_02", "love_01"), prompts))
    }

    @Test
    fun categoriesHaveLocalizedNames() {
        val first = prompts.categories.first()
        assertEquals("자기 확언", first.name("ko"))
        assertEquals("Self-affirmation", first.name("en"))
        assertEquals("자기 확언", first.name("ja")) // 없는 언어 → 한국어
    }

    /** 영어는 첫 글자만 대문자, 중간 대문자·아포스트로피 금지 (CLAUDE.md 작성 기준) */
    @Test
    fun englishPromptsFollowStyleRules() {
        prompts.prompts.forEach {
            val en = it.text("en")
            assertTrue("${it.id}: 첫 글자 대문자", en.first().isUpperCase())
            assertFalse("${it.id}: 중간 대문자", en.drop(1).any(Char::isUpperCase))
            assertFalse("${it.id}: 아포스트로피", en.contains('\'') || en.contains('’'))
        }
    }
}
