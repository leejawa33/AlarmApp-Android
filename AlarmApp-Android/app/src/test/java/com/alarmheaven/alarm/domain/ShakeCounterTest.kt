package com.alarmheaven.alarm.domain

import com.alarmheaven.alarm.domain.ShakeCounter.Companion.G
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeCounterTest {
    @Test
    fun countsPeaksAboveThreshold() {
        val counter = ShakeCounter(target = 10)
        counter.feed(0.0, 0.0)                              // 첫 샘플 = 시작
        assertFalse(counter.feed(0.8 * G, 0.9))             // 약한 움직임은 무시
        assertTrue(counter.feed(2.0 * G, 1.0))
        assertEquals(1, counter.count)
    }

    @Test
    fun ignoresPeaksWithinInterval() {
        val counter = ShakeCounter(target = 10)
        counter.feed(0.0, 0.0)
        counter.feed(2.0 * G, 1.00)
        counter.feed(2.5 * G, 1.10) // 같은 흔들기의 두 번째 피크
        counter.feed(2.5 * G, 1.30)
        assertEquals(1, counter.count)
        counter.feed(2.0 * G, 1.40)
        assertEquals(2, counter.count)
    }

    @Test
    fun ignoresMovementRightAfterStart() {
        val counter = ShakeCounter(target = 10)
        assertFalse(counter.feed(3.0 * G, 10.0)) // 미션 시작 버튼을 누르는 손 움직임
        assertFalse(counter.feed(3.0 * G, 10.3))
        assertTrue(counter.feed(3.0 * G, 10.6))
    }

    @Test
    fun completesAtTargetAndStopsCounting() {
        val counter = ShakeCounter(target = 10)
        for (i in 0 until 15) counter.registerShake(i.toDouble())
        assertEquals(10, counter.count)
        assertTrue(counter.isComplete)
    }

    @Test
    fun defaultThresholdIsOnePointFiveG() {
        assertEquals(14.7, ShakeCounter.DEFAULT_THRESHOLD, 0.01)
    }
}
