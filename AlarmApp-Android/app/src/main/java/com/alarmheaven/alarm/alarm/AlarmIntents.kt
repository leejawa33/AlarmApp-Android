package com.alarmheaven.alarm.alarm

import android.content.Context
import android.content.Intent
import com.alarmheaven.alarm.RingActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val EXTRA_ALARM_ID = "alarmId"

/** 알람이 울릴 때 여는 실행 Activity (잠금 화면 위에 뜬다). */
fun ringActivityIntent(context: Context, alarmId: String): Intent =
    Intent(context, RingActivity::class.java)
        .putExtra(EXTRA_ALARM_ID, alarmId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

/**
 * 지금 울리는 알람 id (프로세스 안에서 공유). `RingingService`가 정하고, 실행 화면과 메인 화면이 읽는다.
 * 서비스가 죽으면 프로세스도 같이 죽으므로 영속 저장은 필요 없다.
 */
object RingingState {
    private val _alarmId = MutableStateFlow<String?>(null)
    val alarmId: StateFlow<String?> = _alarmId.asStateFlow()

    fun start(id: String) { _alarmId.value = id }

    /** 다른 알람이 이미 울리기 시작했다면 건드리지 않는다. */
    fun stop(id: String) { _alarmId.compareAndSet(id, null) }
}
