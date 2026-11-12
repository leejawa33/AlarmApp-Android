package com.alarmheaven.alarm.domain

/** 앱 언어 선택. [SYSTEM]이면 시스템 언어를 따르고, 한·영이 아니면 한국어로 보인다(기본 리소스). */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    KOREAN("ko"),
    ENGLISH("en"),
    ;

    companion object {
        /** 저장된 로케일 태그(`ko`, `en-US` 등) → 선택값. 모르거나 비어 있으면 시스템. */
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag != null && tag != null && tag.startsWith(it.tag) } ?: SYSTEM
    }
}
