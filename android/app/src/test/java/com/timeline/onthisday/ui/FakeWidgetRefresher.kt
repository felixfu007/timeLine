package com.timeline.onthisday.ui

import com.timeline.onthisday.widget.WidgetRefresher

/** Records calls made by [SettingsViewModel] without needing a real Glance/Context. */
class FakeWidgetRefresher : WidgetRefresher {

    var refreshCount: Int = 0
        private set

    override suspend fun refreshNow() {
        refreshCount++
    }
}
