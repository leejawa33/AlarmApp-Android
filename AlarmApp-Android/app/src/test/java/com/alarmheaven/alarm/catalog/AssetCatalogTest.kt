package com.alarmheaven.alarm.catalog

import com.alarmheaven.alarm.TestAssets
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetCatalogTest {
    private val catalog = TestAssets.catalog()

    @Test
    fun hasThreeMediaAndTwoSounds() {
        assertEquals(3, catalog.media.size)
        assertEquals(2, catalog.sounds.size)
    }

    @Test
    fun defaultsAreFlaggedInCatalog() {
        assertEquals("media_puppy_crack", catalog.defaultMedia?.id)
        assertEquals("sound_morning_arrived", catalog.defaultSound?.id)
        assertEquals(1, catalog.media.count { it.isDefault })
        assertEquals(1, catalog.sounds.count { it.isDefault })
    }

    @Test
    fun idsAreUnique() {
        val ids = catalog.media.map { it.id } + catalog.sounds.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyFileExistsUnderAssets() {
        (catalog.media.map { it.file } + catalog.sounds.map { it.file }).forEach {
            assertTrue("없는 파일: $it", File(TestAssets.dir, it).isFile)
        }
    }

    @Test
    fun soundsAreMp3Only() {
        catalog.sounds.forEach { assertTrue(it.id, it.file.endsWith(".mp3")) }
    }

    @Test
    fun shakeCountComesFromCatalog() {
        assertEquals(10, catalog.shakeCount)
    }

    @Test
    fun promptsLoadAndMissingFileFallsBackToNull() {
        assertNotNull(catalog.loadPrompts(TestAssets.readText))
        assertNull(catalog.loadPrompts { throw java.io.FileNotFoundException(it) }) // 문구 파일이 없으면 null
    }

    /** 미디어·소리 표시명은 앱 언어를 따라 strings에서 id로 찾는다. 두 언어 문구 JSON에 모든 id가 있어야 한다. */
    @Test
    fun displayNamesExistInBothUiStringFiles() {
        val ids = catalog.media.map { it.id } + catalog.sounds.map { it.id }
        for (lang in listOf("ko", "en")) {
            val text = TestAssets.readText("Copy/ui_strings_$lang.json")
            ids.forEach { assertTrue("$lang 에 $it 없음", text.contains("\"$it\"")) }
        }
    }
}
