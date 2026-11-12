package com.alarmheaven.alarm.domain

/** 알람당 정확히 1개. [id]는 `assets_catalog.json`의 missions[].id와 같다. */
enum class MissionType(val id: String) {
    SHAKE("shake"),
    TRANSCRIPTION("transcription");

    companion object {
        /** 알 수 없는 값은 흔들기로 대체한다(미션 없이 꺼지면 안 된다). */
        fun fromId(id: String): MissionType = entries.firstOrNull { it.id == id } ?: SHAKE
    }
}
