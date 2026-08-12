package com.timeline.onthisday.ui

import com.timeline.onthisday.scheduler.WidgetRotationScheduler

/** Records calls made by [SettingsViewModel] without needing a real WorkManager/Context. */
class FakeWidgetRotationScheduler : WidgetRotationScheduler {

    val recordedIntervals = mutableListOf<Long>()

    override fun updateWidgetRotationInterval(intervalMinutes: Long) {
        recordedIntervals += intervalMinutes
    }
}
