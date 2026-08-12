package com.timeline.onthisday.scheduler

import com.timeline.onthisday.data.local.entity.HistoryEventEntity
import com.timeline.onthisday.data.repository.HistoryEventRepository
import com.timeline.onthisday.data.settings.AppLanguage
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Instrumented-test double for [HistoryEventRepository], used to observe that
 * [DailyFetchWorker] actually calls `refreshIfNeeded(forceRefresh = true)` (and, on success,
 * `pruneStaleCache()`) once WorkManager's [androidx.work.testing.TestDriver] marks its
 * constraints as satisfied — see DailyFetchWorkerSchedulingTest.
 *
 * A [CountDownLatch] (rather than a plain boolean) is used because the worker runs on a
 * WorkManager-managed thread/coroutine, not the test thread.
 *
 * M7 defect #1 fix: [DailyFetchWorker] now calls `refreshIfNeeded()` once per [AppLanguage] (zh
 * and en are independently curated feeds — see HistoryEventEntity's kdoc), so this fake records
 * every call rather than just the last one; [lastRefreshMonth]/[lastRefreshDay]/
 * [lastForceRefresh] still reflect the most recent call, which is sufficient for
 * DailyFetchWorkerSchedulingTest's existing assertions since month/day/forceRefresh are the same
 * across every per-language call within a single run.
 */
class RecordingHistoryEventRepository : HistoryEventRepository {

    val refreshLatch = CountDownLatch(1)
    val pruneLatch = CountDownLatch(1)

    @Volatile var lastRefreshMonth: Int? = null
    @Volatile var lastRefreshDay: Int? = null
    @Volatile var lastForceRefresh: Boolean? = null

    val refreshedLanguages = java.util.Collections.synchronizedList(mutableListOf<AppLanguage>())

    var refreshResult: Result<Unit> = Result.success(Unit)

    override fun observeEventsForDate(month: Int, day: Int, language: AppLanguage): Flow<List<HistoryEventEntity>> =
        MutableStateFlow(emptyList())

    override fun observeEventById(id: Long): Flow<HistoryEventEntity?> = MutableStateFlow(null)

    override suspend fun refreshIfNeeded(
        month: Int,
        day: Int,
        language: AppLanguage,
        forceRefresh: Boolean
    ): Result<Unit> {
        lastRefreshMonth = month
        lastRefreshDay = day
        lastForceRefresh = forceRefresh
        refreshedLanguages += language
        refreshLatch.countDown()
        return refreshResult
    }

    override suspend fun pruneStaleCache() {
        pruneLatch.countDown()
    }
}
