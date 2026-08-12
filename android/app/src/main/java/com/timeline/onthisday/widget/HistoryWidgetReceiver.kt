package com.timeline.onthisday.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.timeline.onthisday.data.local.dao.WidgetStateDao
import com.timeline.onthisday.scheduler.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Exposes [HistoryWidget] to the Android AppWidgetManager. Registered in AndroidManifest.xml
 * with `@xml/history_widget_info` (F-03: 4x1/4x2 sizing, home_screen category).
 *
 * [glanceAppWidget] is deliberately just `HistoryWidget()` with no injected dependencies — see
 * HistoryWidget's kdoc / di/HistoryWidgetEntryPoint.kt for why this Receiver must stay safely
 * constructible with zero Hilt-injected state: Glance's `GlanceAppWidgetManager` sometimes
 * instantiates this class directly via reflection, bypassing the normal `onReceive()` dispatch
 * path that `@AndroidEntryPoint` field injection relies on. [workScheduler]/[widgetStateDao] are
 * only touched from [onEnabled]/[onDeleted], which — unlike that reflective path — *are* real
 * system-dispatched `onReceive()` calls, so Hilt injection has already happened by the time they
 * run.
 */
@AndroidEntryPoint
class HistoryWidgetReceiver : GlanceAppWidgetReceiver() {

    @Inject
    lateinit var workScheduler: WorkScheduler

    @Inject
    lateinit var widgetStateDao: WidgetStateDao

    override val glanceAppWidget: GlanceAppWidget = HistoryWidget()

    /** Called once when the first instance of this widget is placed on a home screen. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // ExistingPeriodicWorkPolicy.KEEP inside WorkScheduler makes this a safe no-op if
        // TimelineApplication.onCreate() already scheduled it.
        workScheduler.scheduleWidgetRotation()
    }

    /**
     * M7 defect #5 fix: called when one or more widget instances are removed from the home
     * screen. Cleans up their `widget_state` rows (rotation index / last-rotated timestamp) so
     * repeatedly adding/removing this widget doesn't accumulate orphaned rows keyed by
     * long-defunct `appWidgetId`s. Uses a plain IO-dispatcher [CoroutineScope] (rather than the
     * base [GlanceAppWidgetReceiver]'s own coroutine scope, which is gated behind
     * `@ExperimentalGlanceApi`) — this cleanup is a simple fire-and-forget delete with no need to
     * tie its lifetime to Glance's internal bookkeeping.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { widgetId -> widgetStateDao.deleteState(widgetId) }
        }
    }
}
