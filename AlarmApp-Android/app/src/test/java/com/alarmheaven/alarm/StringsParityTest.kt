package com.alarmheaven.alarm

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 한국어(values)와 영어(values-en) 문구의 키가 정확히 일치하는지 검사한다. */
class StringsParityTest {

    private fun xmlKeys(path: String): Set<String> {
        val file = File(path)
        assertTrue("파일 없음: ${file.absolutePath}", file.exists())
        return Regex("""<string\s+name="([^"]+)"""")
            .findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun jsonKeys(lang: String): Set<String> =
        Regex(""""([^"]+)"\s*:""").findAll(TestAssets.readText("Copy/ui_strings_$lang.json"))
            .map { it.groupValues[1] }.toSet()

    @Test
    fun koAndEnStringKeysMatch() {
        val ko = xmlKeys("src/main/res/values/strings.xml")
        val en = xmlKeys("src/main/res/values-en/strings.xml")
        assertTrue("한국어 키가 비어 있음", ko.isNotEmpty())
        assertEquals("영어에 없는 키: ${ko - en}", emptySet<String>(), ko - en)
        assertEquals("한국어에 없는 키: ${en - ko}", emptySet<String>(), en - ko)
    }

    /** 공유 문구 JSON(원본)의 모든 키가 strings.xml에 옮겨져 있어야 한다. */
    @Test
    fun sharedCopyKeysExistInStringsXml() {
        for ((lang, dir) in listOf("ko" to "values", "en" to "values-en")) {
            val xml = xmlKeys("src/main/res/$dir/strings.xml")
            val missing = jsonKeys(lang) - xml
            assertEquals("$lang: strings.xml에 없는 JSON 키: $missing", emptySet<String>(), missing)
        }
    }

    /** %1$d 같은 서식 자리표시자는 두 언어가 같아야 한다(런타임 포맷 오류 방지). */
    @Test
    fun formatPlaceholdersMatch() {
        fun placeholders(path: String): Map<String, List<String>> {
            val re = Regex("""<string\s+name="([^"]+)">(.*?)</string>""")
            return re.findAll(File(path).readText()).associate { m ->
                m.groupValues[1] to Regex("""%\d+\$[a-z]""").findAll(m.groupValues[2]).map { it.value }.sorted().toList()
            }
        }
        val ko = placeholders("src/main/res/values/strings.xml")
        val en = placeholders("src/main/res/values-en/strings.xml")
        for ((key, koArgs) in ko) assertEquals("$key 자리표시자", koArgs, en[key])
    }
}
