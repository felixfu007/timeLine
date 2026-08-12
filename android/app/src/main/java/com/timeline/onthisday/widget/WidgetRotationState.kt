package com.timeline.onthisday.widget

/**
 * Pure rotation-index arithmetic, extracted out of WidgetRotationWorker/HistoryWidget so it's
 * trivially JVM-unit-testable without Robolectric/instrumented tests (see
 * WidgetRotationStateTest). Persistence (per-widgetId `WidgetStateEntity` rows, F-03 #3) lives
 * in Room via WidgetStateDao; this object only computes index values.
 */
object WidgetRotationState {

    /**
     * The index WidgetRotationWorker should advance a widget instance to on its next tick.
     *
     * @param currentIndex the widget's previously stored index, or null if this widget instance
     * has never rotated before (first tick after being added).
     * @param eventCount how many events are available today. If 0, always returns 0 — there's
     * nothing to index into; the caller renders the "no data" state instead (F-01 #4).
     */
    fun nextIndex(currentIndex: Int?, eventCount: Int): Int {
        if (eventCount <= 0) return 0
        val current = currentIndex ?: return 0
        return (current + 1) % eventCount
    }

    /**
     * Resolves which event a widget should currently render, defensively clamping a possibly
     * stale stored index (e.g. today has fewer events cached than when the index was last
     * advanced) into range instead of throwing.
     */
    fun currentIndexOrFallback(storedIndex: Int?, eventCount: Int): Int {
        if (eventCount <= 0) return 0
        return (storedIndex ?: 0).coerceIn(0, eventCount - 1)
    }
}
