package com.timeline.onthisday.scheduler

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure date-math helper for [WorkScheduler.scheduleDailyFetch].
 *
 * WorkManager has no API to say "run every day at a fixed wall-clock time" — a
 * `PeriodicWorkRequest` only supports a fixed *interval* (here, 24h) plus a one-time
 * `setInitialDelay`. The standard workaround (F-02 #1) is to compute the delay from "now" until
 * the next occurrence of the target local time and use that as the initial delay; every
 * subsequent run then lands ~24h later, i.e. close to the same wall-clock time each day, modulo
 * normal WorkManager/Doze scheduling slack (already accepted as a known limitation — see F-05 #3
 * / ANALYSIS.md 6.2 — for the sibling rotation worker, and applies here too).
 */
object DailyFetchScheduling {

    /** Local time-of-day the daily fetch should first fire at: 00:05 (F-02 #1). */
    val DEFAULT_TARGET_TIME: LocalTime = LocalTime.of(0, 5)

    /**
     * Returns how long from [now] until the next occurrence of [targetTime]. If [now] is exactly
     * at (or already past) [targetTime] for the current day, rolls over to [targetTime] on the
     * following day — "now" is never a valid past-or-present target instant, only a strictly
     * future one.
     */
    fun delayUntilNextRun(
        now: LocalDateTime,
        targetTime: LocalTime = DEFAULT_TARGET_TIME
    ): Duration {
        val todayTarget = now.toLocalDate().atTime(targetTime)
        val nextRun = if (todayTarget.isAfter(now)) todayTarget else todayTarget.plusDays(1)
        return Duration.between(now, nextRun)
    }
}
