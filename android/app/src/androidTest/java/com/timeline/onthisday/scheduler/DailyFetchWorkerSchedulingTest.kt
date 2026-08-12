package com.timeline.onthisday.scheduler

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented (real WorkManager + [androidx.work.testing.TestDriver]) coverage for M4: verifies
 * that [WorkScheduler.scheduleDailyFetch] enqueues a request that, once its constraints are
 * reported as satisfied, WorkManager actually runs — and that running it drives
 * [DailyFetchWorker] to call [HistoryEventRepository.refreshIfNeeded] with `forceRefresh = true`
 * for "today", then [HistoryEventRepository.pruneStaleCache] on success.
 *
 * Uses a hand-rolled [WorkerFactory] (rather than Hilt) so the test doesn't need a
 * `HiltTestApplication`/custom test runner — [DailyFetchWorker]'s constructor is a plain Kotlin
 * constructor and can be invoked directly with a test double for its one dependency.
 */
@RunWith(AndroidJUnit4::class)
class DailyFetchWorkerSchedulingTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var repository: RecordingHistoryEventRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = RecordingHistoryEventRepository()

        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(FakeDailyFetchWorkerFactory(repository))
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun scheduleDailyFetch_runsWorker_andForceRefreshesTodayThenPrunes_whenConstraintsSatisfied() {
        val scheduler = WorkScheduler(workManager)

        scheduler.scheduleDailyFetch(now = LocalDateTime.of(2026, 7, 28, 10, 0, 0))

        val workInfo = workManager
            .getWorkInfosForUniqueWork(WorkScheduler.DAILY_FETCH_WORK_NAME)
            .get()
            .single()
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
        // The request has both an initial delay and a NETWORK_CONNECTED constraint; satisfy both
        // so the TestDriver actually lets it run synchronously (SynchronousExecutor above).
        testDriver.setInitialDelayMet(workInfo.id)
        testDriver.setAllConstraintsMet(workInfo.id)

        assertTrue(
            "DailyFetchWorker should have called refreshIfNeeded(...) within the test timeout",
            repository.refreshLatch.await(10, TimeUnit.SECONDS)
        )
        assertTrue(
            "DailyFetchWorker should have called pruneStaleCache() after a successful refresh",
            repository.pruneLatch.await(10, TimeUnit.SECONDS)
        )
        assertEquals(true, repository.lastForceRefresh)

        val today = java.time.LocalDate.now()
        assertEquals(today.monthValue, repository.lastRefreshMonth)
        assertEquals(today.dayOfMonth, repository.lastRefreshDay)

        val finishedInfo = workManager
            .getWorkInfosForUniqueWork(WorkScheduler.DAILY_FETCH_WORK_NAME)
            .get()
            .single()
        // Periodic work goes back to ENQUEUED (awaiting its next period) after a successful run
        // rather than terminating like one-off work.
        assertEquals(WorkInfo.State.ENQUEUED, finishedInfo.state)
    }
}

private class FakeDailyFetchWorkerFactory(
    private val repository: RecordingHistoryEventRepository
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == DailyFetchWorker::class.java.name) {
            DailyFetchWorker(appContext, workerParameters, repository)
        } else {
            null
        }
    }
}
