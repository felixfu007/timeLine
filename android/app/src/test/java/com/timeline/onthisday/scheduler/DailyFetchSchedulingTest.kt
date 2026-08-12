package com.timeline.onthisday.scheduler

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the pure delay-math extracted out of [WorkScheduler.scheduleDailyFetch] — the three
 * cases called out in the M4 spec: now is before / after / exactly at the 00:05 target.
 */
class DailyFetchSchedulingTest {

    private val target = LocalTime.of(0, 5)

    @Test
    fun now_beforeTargetTime_delaysUntilTargetLaterToday() {
        val now = LocalDateTime.of(2026, 7, 28, 0, 0, 0)

        val delay = DailyFetchScheduling.delayUntilNextRun(now, target)

        assertEquals(Duration.ofMinutes(5), delay)
    }

    @Test
    fun now_afterTargetTime_delaysUntilTargetTomorrow() {
        val now = LocalDateTime.of(2026, 7, 28, 10, 0, 0)

        val delay = DailyFetchScheduling.delayUntilNextRun(now, target)

        // From 10:00 today to 00:05 tomorrow = 14h5m.
        assertEquals(Duration.ofHours(14).plusMinutes(5), delay)
    }

    @Test
    fun now_exactlyAtTargetTime_rollsOverToTomorrowRatherThanZeroDelay() {
        val now = LocalDateTime.of(2026, 7, 28, 0, 5, 0)

        val delay = DailyFetchScheduling.delayUntilNextRun(now, target)

        // "now" itself is never treated as a valid (already past) fire instant.
        assertEquals(Duration.ofHours(24), delay)
    }

    @Test
    fun now_justAfterTargetTime_delaysUntilTomorrow() {
        val now = LocalDateTime.of(2026, 7, 28, 0, 5, 1)

        val delay = DailyFetchScheduling.delayUntilNextRun(now, target)

        assertEquals(Duration.ofHours(24).minusSeconds(1), delay)
    }

    @Test
    fun now_justBeforeTargetTime_delaysBySubMinuteAmount() {
        val now = LocalDateTime.of(2026, 7, 28, 0, 4, 59)

        val delay = DailyFetchScheduling.delayUntilNextRun(now, target)

        assertEquals(Duration.ofSeconds(1), delay)
    }

    @Test
    fun defaultTargetTime_is0005() {
        assertEquals(LocalTime.of(0, 5), DailyFetchScheduling.DEFAULT_TARGET_TIME)
    }
}
