package com.timeline.onthisday.widget

/**
 * Narrow interface around triggering an immediate Widget repaint, extracted purely so
 * [com.timeline.onthisday.ui.SettingsViewModel] stays unit-testable on the plain JVM with a fake
 * (same pattern as [com.timeline.onthisday.locale.AppLocaleApplier] /
 * [com.timeline.onthisday.scheduler.WidgetRotationScheduler]).
 *
 * Used only for the 2026-07-30 "Widget 外觀自訂" (background/text/border color) settings —
 * deliberately *not* used for F-07's language setting, which per ANALYSIS.md's F-07 AC2 is
 * defined to only take effect on the widget "於下一次刷新週期" (i.e. on
 * [com.timeline.onthisday.scheduler.WidgetRotationWorker]'s next periodic tick), because language
 * changes what content is shown (a zh vs en cached event) and should stay in lockstep with the
 * rotation cadence. Appearance colors are pure chrome with no data/network implication, so an
 * immediate on-demand repaint here is safe (a single user-triggered `updateAll()`, not a
 * background poll) and doesn't conflict with the low-power non-functional standard.
 */
interface WidgetRefresher {
    suspend fun refreshNow()
}
