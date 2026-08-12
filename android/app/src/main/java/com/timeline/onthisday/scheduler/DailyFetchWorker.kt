package com.timeline.onthisday.scheduler

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timeline.onthisday.data.repository.HistoryEventRepository
import com.timeline.onthisday.data.settings.AppLanguage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * M4 — once-daily background refresh (F-02). Registered by [WorkScheduler.scheduleDailyFetch] as
 * a `PeriodicWorkRequest` timed to first fire around local 00:05 (see [DailyFetchScheduling]),
 * with a `NETWORK_CONNECTED` constraint (ANALYSIS.md 五 低電耗 — the worker simply doesn't run
 * while offline rather than wasting a wakeup/retry).
 *
 * Always force-refreshes "today" (recomputed from [LocalDate.now] on every run, so a timezone
 * change is naturally picked up on the next cycle — F-02 #4) via
 * [HistoryEventRepository.refreshIfNeeded]'s existing "keep old cache on failure" guarantee
 * (F-02 #2), then prunes the rolling 35-day cache window (ANALYSIS.md 五).
 *
 * M7 defect #1 fix: refreshes **both** [AppLanguage] feeds every run (previously only ever
 * fetched zh) — zh and en are independently curated event lists (see
 * [com.timeline.onthisday.data.local.entity.HistoryEventEntity]'s kdoc), so keeping both fresh
 * nightly means switching the app's display language doesn't leave the user staring at a stale/
 * empty list until an on-demand fetch catches up. A failure fetching one language doesn't affect
 * the other (each is an independent [HistoryEventRepository.refreshIfNeeded] call with its own
 * "keep old cache on failure" guarantee) — this worker only retries/backs off if *every*
 * language's refresh failed, since a lone language hiccup is already safely absorbed by the
 * per-request on-demand refresh in [WidgetRotationWorker]/`HistoryListViewModel` the next time
 * that language is actually viewed.
 *
 * On failure, retries with the caller-configured exponential backoff up to [MAX_RETRY_ATTEMPTS]
 * within this scheduling period, then gives up (`Result.failure()`) rather than retrying forever
 * — the next daily period will simply try again, satisfying "不無限狂重試耗電" (F-02).
 */
@HiltWorker
class DailyFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: HistoryEventRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val results = AppLanguage.entries.map { language ->
            repository.refreshIfNeeded(
                month = today.monthValue,
                day = today.dayOfMonth,
                language = language,
                forceRefresh = true
            )
        }

        return if (results.any { it.isSuccess }) {
            repository.pruneStaleCache()
            Result.success()
        } else {
            if (runAttemptCount >= MAX_RETRY_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 5
    }
}
