package com.timeline.onthisday.scheduler

/**
 * Narrow interface extracted out of [WorkScheduler] purely so [com.timeline.onthisday.ui.SettingsViewModel]
 * (F-05 AC2 — changing the rotation interval reschedules the widget's `PeriodicWorkRequest`) can
 * be unit-tested on the plain JVM with a fake, without needing a real `android.content.Context`/
 * `WorkManager` instance. [WorkScheduler] is the only production implementation.
 */
interface WidgetRotationScheduler {

    /**
     * Cancels/replaces the currently-scheduled widget rotation `PeriodicWorkRequest` with one
     * using [intervalMinutes] (F-05 AC2: "使用者變更設定後，最晚於下一個 WorkManager 排程週期內生效").
     * Safe to call even if nothing was previously scheduled.
     */
    fun updateWidgetRotationInterval(intervalMinutes: Long)
}
