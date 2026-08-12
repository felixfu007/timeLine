package com.timeline.onthisday.di

import com.timeline.onthisday.data.local.dao.HistoryEventDao
import com.timeline.onthisday.data.local.dao.WidgetStateDao
import com.timeline.onthisday.data.settings.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Lets [com.timeline.onthisday.widget.HistoryWidget] pull its DAOs out of Hilt's
 * SingletonComponent via `EntryPointAccessors.fromApplication(context, ...)`.
 *
 * This is necessary (rather than plain `@Inject` constructor params, or field-injecting
 * [com.timeline.onthisday.widget.HistoryWidgetReceiver]) because Glance's
 * `GlanceAppWidgetManager` sometimes instantiates `GlanceAppWidgetReceiver` subclasses directly
 * via reflection for its own internal bookkeeping (observed during M3 emulator verification:
 * `GlanceAppWidgetManager.addAllReceiversAndProvidersToPreferences()` calls
 * `HistoryWidgetReceiver().glanceAppWidget` outside of the normal `onReceive()` broadcast
 * dispatch path that Hilt's `@AndroidEntryPoint` field injection relies on — so any DAOs
 * field-injected into the Receiver would be `lateinit`-uninitialized in that code path and
 * crash). Looking dependencies up via [dagger.hilt.android.EntryPointAccessors] inside
 * `provideGlance(context, id)` instead works regardless of how/when the `HistoryWidget`/
 * `HistoryWidgetReceiver` instances themselves were constructed, since it only needs a valid
 * `Context` (always available) rather than Hilt-managed instantiation of the object itself.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HistoryWidgetEntryPoint {
    fun historyEventDao(): HistoryEventDao
    fun widgetStateDao(): WidgetStateDao

    /** M6 (F-07 AC2) — widget-side manual locale resolution, see widget/HistoryWidget.kt. */
    fun settingsRepository(): SettingsRepository
}
