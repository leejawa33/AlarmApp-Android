package com.alarmheaven.alarm.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeCounterTest {
    /** 기본 임계값을 확실히 넘는 세기(임계값을 바꿔도 테스트가 깨지지 않게 상대값으로 둔다) */
    private val strong = ShakeCounter.DEFAULT_THRESHOLD * 1.2
    private val stronger = ShakeCounter.DEFAULT_THRESHOLD * 1.5

    @Test
    fun countsPeaksAboveThreshold() {
        val counter = ShakeCounter(target = 10)
        counter.feed(0.0, 0.0)                              // 첫 샘플 = 시작
        assertFalse(counter.feed(ShakeCounter.DEFAULT_THRESHOLD * 0.8, 0.9)) // 임계값 미만은 무시
        assertTrue(counter.feed(strong, 1.0))
        assertEquals(1, counter.count)
    }

    @Test
    fun ignoresPeaksWithinInterval() {
        val counter = ShakeCounter(target = 10)
        counter.feed(0.0, 0.0)
        counter.feed(strong, 1.00)
        counter.feed(stronger, 1.10) // 같은 흔들기의 두 번째 피크
        counter.feed(stronger, 1.30)
        assertEquals(1, counter.count)
        counter.feed(strong, 1.40)
        assertEquals(2, counter.count)
    }

    @Test
    fun ignoresMovementRightAfterStart() {
        val counter = ShakeCounter(target = 10)
        assertFalse(counter.feed(strong, 10.0)) // 미션 시작 버튼을 누르는 손 움직임
        assertFalse(counter.feed(strong, 10.3))
        assertTrue(counter.feed(strong, 10.6))
    }

    @Test
    fun completesAtTargetAndStopsCounting() {
        val counter = ShakeCounter(target = 10)
        for (i in 0 until 15) counter.registerShake(i.toDouble())
        assertEquals(10, counter.count)
        assertTrue(counter.isComplete)
    }

    @Test
    fun defaultThresholdIsFourG() {
        assertEquals(39.23, ShakeCounter.DEFAULT_THRESHOLD, 0.01)
    }
}
