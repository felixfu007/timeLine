package com.timeline.onthisday.scheduler

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timeline.onthisday.data.local.dao.HistoryEventDao
import com.timeline.onthisday.data.local.dao.WidgetStateDao
import com.timeline.onthisday.data.local.entity.WidgetStateEntity
import com.timeline.onthisday.data.repository.HistoryEventRepository
import com.timeline.onthisday.data.settings.SettingsRepository
import com.timeline.onthisday.widget.HistoryWidget
import com.timeline.onthisday.widget.WidgetRotationState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * M3 — periodic "假跑馬燈" tick (ANALYSIS.md 6.1 / 十): advances every registered widget
 * instance's rotation index by one and refreshes their content via `updateAll()`. Never performs
 * a network call unless there is truly no cached data at all for today yet, in which case it
 * reuses [HistoryEventRepository]'s existing "read cache, else fetch" logic (M1) as a stand-in
 * for M4's dedicated DailyFetchWorker — this keeps the widget usable end-to-end before M4 lands,
 * without duplicating fetch/backoff logic.
 *
 * Registered as a `PeriodicWorkRequest` (>=15 min, WorkManager's system floor, see
 * WorkScheduler) with no network constraint — the common case (data already cached) never
 * touches the network, satisfying ANALYSIS.md 五's low-power standard; the occasional fetch is a
 * plain suspend call that fails gracefully (via the repository's existing Result-based error
 * handling) if there's no connectivity, same as any other refreshIfNeeded() caller.
 */
@HiltWorker
class WidgetRotationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val historyEventDao: HistoryEventDao,
    private val widgetStateDao: WidgetStateDao,
    private val repository: HistoryEventRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val glanceManager = GlanceAppWidgetManager(applicationContext)
        val glanceIds = glanceManager.getGlanceIds(HistoryWidget::class.java)
        if (glanceIds.isEmpty()) {
            // No widget instances placed yet — nothing to rotate/update.
            return Result.success()
        }

        // M7 defect #1 fix: the Widget shows whichever language is currently configured in
        // Settings (zh/en are independently-cached feeds, not translations of each other — see
        // HistoryEventEntity's kdoc), so this on-demand fallback fetch must target that same
        // language rather than always the zh feed.
        val language = settingsRepository.currentSettings().language

        val today = LocalDate.now()
        var cachedEvents = historyEventDao.getEventsForDateOnce(today.monthValue, today.dayOfMonth, language.name)
        if (cachedEvents.isEmpty()) {
            repository.refreshIfNeeded(today.monthValue, today.dayOfMonth, language)
            cachedEvents = historyEventDao.getEventsForDateOnce(today.monthValue, today.dayOfMonth, language.name)
        }

        val widgetIds = glanceIds.map { glanceManager.getAppWidgetId(it) }
        advanceWidgetRotations(widgetIds, cachedEvents.size, widgetStateDao)

        HistoryWidget().updateAll(applicationContext)

        return Result.success()
    }
}

/**
 * Advances the stored rotation index for each of [widgetIds] independently (F-03 #3 — multiple
 * widget instances never share/interfere with each other's progress) and loops back to 0 once
 * past the last event. Extracted as a standalone top-level suspend fun (rather than a private
 * CoroutineWorker method) so it's directly JVM-unit-testable against a fake [WidgetStateDao] —
 * see WidgetRotationWorkerTest.
 */
suspend fun advanceWidgetRotations(
    widgetIds: List<Int>,
    eventCount: Int,
    widgetStateDao: WidgetStateDao,
    now: Long = System.currentTimeMillis()
) {
    for (widgetId in widgetIds) {
        val existing = widgetStateDao.getState(widgetId)
        val nextIndex = WidgetRotationState.nextIndex(existing?.currentIndex, eventCount)
        widgetStateDao.upsert(WidgetStateEntity(widgetId = widgetId, currentIndex = nextIndex, lastRotatedAt = now))
    }
}
