package com.timeline.onthisday.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Production [WidgetRefresher] — see that interface's kdoc for when/why this is used. */
class GlanceWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetRefresher {

    override suspend fun refreshNow() {
        HistoryWidget().updateAll(context)
    }
}
