package com.alarmheaven.alarm.domain

/**
 * 흔들기 미션 횟수 세기 (순수 로직, 센서와 분리해 테스트한다).
 * - 선형 가속도(중력 제외, m/s²) 크기가 [threshold]를 넘으면 1회
 * - 한 번 흔들 때 여러 피크가 생기므로 [minInterval] 안의 피크는 무시
 * - 시작 직후 [warmUp] 동안(버튼을 누르는 손 움직임 등)은 세지 않는다
 *
 * 시간 단위는 초. 기준값 14.7 m/s² = 1.5g (iOS 값) — **실기기에서 조정 필요**.
 */
class ShakeCounter(
    target: Int,
    private val threshold: Double = DEFAULT_THRESHOLD,
    private val minInterval: Double = 0.35,
    private val warmUp: Double = 0.5,
) {
    val target: Int = maxOf(1, target)

    var count: Int = 0
        private set

    private var lastShakeAt = Double.NEGATIVE_INFINITY
    private var startedAt: Double? = null

    val isComplete: Boolean get() = count >= target

    /** 센서 샘플 하나. 1회로 셌으면 true. */
    fun feed(magnitude: Double, time: Double): Boolean {
        val start = startedAt ?: time.also { startedAt = it } // 첫 센서 샘플 시각 = 시작
        if (time - start < warmUp || magnitude < threshold) return false
        return registerShake(time)
    }

    /** 흔들기 1회. 간격 안이거나 이미 완료면 무시. */
    fun registerShake(time: Double): Boolean {
        if (isComplete || time - lastShakeAt < minInterval) return false
        lastShakeAt = time
        count += 1
        return true
    }

    companion object {
        const val G = 9.80665
        const val DEFAULT_THRESHOLD = 1.5 * G
    }
}
