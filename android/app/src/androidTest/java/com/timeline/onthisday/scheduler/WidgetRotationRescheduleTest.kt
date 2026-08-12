package com.timeline.onthisday.scheduler

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented (real WorkManager) coverage for M6's F-05 AC2: changing the widget rotation
 * interval must actually reschedule [WidgetRotationWorker]'s `PeriodicWorkRequest` — not just
 * leave the original one running alongside a second, duplicate one.
 *
 * Unlike [DailyFetchWorkerSchedulingTest] this test never calls `TestDriver.setAllConstraintsMet`
 * — but the test `WorkManager`'s `GreedyScheduler` still eagerly runs any newly-enqueued
 * unconstrained/no-initial-delay work under [SynchronousExecutor] regardless. A [NoOpWorker]
 * factory stands in for the real (Hilt-injected) [WidgetRotationWorker] so that eager run
 * succeeds trivially instead of failing to construct the real worker (which needs DAOs/
 * repository this test doesn't provide) — only `WorkInfo` scheduling state is asserted, not
 * anything about the rotation logic itself (already covered by [WidgetRotationWorkerTest]).
 */
@RunWith(AndroidJUnit4::class)
class WidgetRotationRescheduleTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(FakeWidgetRotationWorkerFactory())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        scheduler = WorkScheduler(workManager)
    }

    @Test
    fun updateWidgetRotationInterval_replacesExistingSchedule_withoutDuplicatingIt() {
        scheduler.scheduleWidgetRotation(intervalMinutes = 30)

        val initialInfos = workManager
            .getWorkInfosForUniqueWork(WorkScheduler.WIDGET_ROTATION_WORK_NAME)
            .get()
        assertEquals(1, initialInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, initialInfos.single().state)

        scheduler.updateWidgetRotationInterval(intervalMinutes = 15)

        val updatedInfos = workManager
            .getWorkInfosForUniqueWork(WorkScheduler.WIDGET_ROTATION_WORK_NAME)
            .get()
        // Still exactly one work item under the unique name — the interval change replaced the
        // schedule in place (ExistingPeriodicWorkPolicy.UPDATE) rather than accumulating a
        // second, competing PeriodicWorkRequest.
        assertEquals(1, updatedInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, updatedInfos.single().state)
    }

    @Test
    fun updateWidgetRotationInterval_schedulesSuccessfully_evenIfNothingWasScheduledBefore() {
        scheduler.updateWidgetRotationInterval(intervalMinutes = 15)

        val infos = workManager
            .getWorkInfosForUniqueWork(WorkScheduler.WIDGET_ROTATION_WORK_NAME)
            .get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
    }

    @Test
    fun repeatedScheduleWidgetRotationCalls_withKeepPolicy_doNotResetOrDuplicate() {
        scheduler.scheduleWidgetRotation(intervalMinutes = 30)
        val firstId = workManager
            .getWorkInfosForUniqueWork(WorkScheduler.WIDGET_ROTATION_WORK_NAME)
            .get()
            .single()
            .id

        // Simulates TimelineApplication.onCreate() being invoked again (e.g. process restart)
        // with the same idempotent KEEP policy — must not create a second entry.
        scheduler.scheduleWidgetRotation(intervalMinutes = 30)

        val infos = workManager
            .getWorkInfosForUniqueWork(WorkScheduler.WIDGET_ROTATION_WORK_NAME)
            .get()
        assertEquals(1, infos.size)
        assertEquals(firstId, infos.single().id)
    }
}

private class FakeWidgetRotationWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? =
        if (workerClassName == WidgetRotationWorker::class.java.name) {
            NoOpWorker(appContext, workerParameters)
        } else {
            null
        }
}

/** Stands in for the real [WidgetRotationWorker] — always succeeds without touching Room/Glance. */
private class NoOpWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = Result.success()
}
