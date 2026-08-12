package com.timeline.onthisday.scheduler

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues the app's background [androidx.work.PeriodicWorkRequest]s. Uses
 * `ExistingPeriodicWorkPolicy.KEEP` so calling this repeatedly (every app start, every time a
 * widget is added) never resets an already-running schedule.
 */
@Singleton
class WorkScheduler @Inject constructor(
    private val workManager: WorkManager
) : WidgetRotationScheduler {

    /**
     * M3: rotates every widget instance's currently-displayed event (F-01's "假跑馬燈" — see
     * ANALYSIS.md 6.1) and — as a stand-in for M4's dedicated daily fetch — opportunistically
     * fetches today's data if nothing is cached yet, so the widget works end-to-end without
     * waiting on M4. [intervalMinutes] defaults to 15 (ANALYSIS.md 五 / F-05's default, matching
     * [AppSettings.DEFAULT_ROTATION_INTERVAL_MINUTES] — see its kdoc for the 2026-07-29 30→15
     * change); WorkManager enforces a 15-minute floor regardless of what's passed in.
     */
    fun scheduleWidgetRotation(intervalMinutes: Long = DEFAULT_ROTATION_INTERVAL_MINUTES) {
        val request = PeriodicWorkRequestBuilder<WidgetRotationWorker>(
            intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES), TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WIDGET_ROTATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * M4: enqueues [DailyFetchWorker] as a once-daily [androidx.work.PeriodicWorkRequest]
     * (F-02 #1), timed via [DailyFetchScheduling.delayUntilNextRun] to first fire around local
     * 00:05 and then every 24h thereafter. `NETWORK_CONNECTED` constraint means the worker is
     * simply not run while offline instead of burning a retry (ANALYSIS.md 五 低電耗); the
     * exponential backoff (F-02, ANALYSIS.md 五 低電耗) combined with [DailyFetchWorker]'s bounded
     * `runAttemptCount` check keeps a run of failures from retrying indefinitely before the next
     * daily period. [ExistingPeriodicWorkPolicy.KEEP] makes calling this on every app start
     * idempotent, same as [scheduleWidgetRotation].
     *
     * [now] is a seam for unit tests / callers that need to control "current time"; production
     * callers should use the default.
     */
    fun scheduleDailyFetch(now: LocalDateTime = LocalDateTime.now()) {
        val initialDelay = DailyFetchScheduling.delayUntilNextRun(now)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DailyFetchWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            DAILY_FETCH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * M6 (F-05 AC2): re-enqueues [WidgetRotationWorker] with a new [intervalMinutes], using
     * `ExistingPeriodicWorkPolicy.UPDATE` so the running schedule is replaced in place rather
     * than accumulating duplicate unique-work entries. Distinct from [scheduleWidgetRotation]
     * (which uses `KEEP` and is meant for idempotent app-start bootstrapping) — this one is
     * explicitly for user-triggered interval changes and must always take effect.
     */
    override fun updateWidgetRotationInterval(intervalMinutes: Long) {
        val request = PeriodicWorkRequestBuilder<WidgetRotationWorker>(
            intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES), TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WIDGET_ROTATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        const val WIDGET_ROTATION_WORK_NAME = "widget_rotation_work"
        const val DAILY_FETCH_WORK_NAME = "daily_fetch_work"
        const val DEFAULT_ROTATION_INTERVAL_MINUTES = 15L
        const val MIN_INTERVAL_MINUTES = 15L
    }
}
