package com.timeline.onthisday.data.settings

/**
 * User-configurable settings (M6 / ANALYSIS.md 三 `app_settings`), persisted via
 * [SettingsRepository]. [rotationIntervalMinutes] is restricted by the Settings UI to
 * 15/30/60 (F-05 AC1) but not validated here — [SettingsRepository] stores whatever is given it,
 * same permissiveness as the rest of the settings layer.
 */
data class AppSettings(
    val language: AppLanguage,
    val rotationIntervalMinutes: Int,
    // 2026-07-30 user UX request (Widget 外觀自訂): defaults reproduce the widget's pre-existing
    // look (light lavender card, near-black text) so users who never open the new settings
    // section see zero visual change — see WidgetColorPalette.kt's kdoc for the preset design.
    val widgetBackgroundColor: WidgetBackgroundColor = WidgetBackgroundColor.DEFAULT,
    val widgetTextColor: WidgetTextColor = WidgetTextColor.DEFAULT,
    val widgetBorderColor: WidgetBorderColor = WidgetBorderColor.DEFAULT,
    // 2026-07-30 user request ("清單列表加入多種排序功能"): only affects the in-app list screen's
    // in-memory ordering (see ui/HistoryEventSorting.kt) — does not touch the Room query/Widget
    // rotation order, which both intentionally keep using the DB's own `ORDER BY year ASC`.
    val sortOrder: SortOrder = SortOrder.DEFAULT
) {
    companion object {
        val ROTATION_INTERVAL_OPTIONS_MINUTES = listOf(15, 30, 60)

        // Kept equal to (and independent of) scheduler.WorkScheduler.DEFAULT_ROTATION_INTERVAL_MINUTES
        // — the data layer intentionally doesn't depend on the scheduler package, so this is the
        // "default settings value" copy while WorkScheduler's is the "fallback if no arg passed" copy.
        //
        // Changed from 30 to 15 on 2026-07-29 per a user UX request to rotate through events
        // faster. 15 minutes is WorkManager's PeriodicWorkRequest system-enforced floor
        // (MIN_PERIODIC_INTERVAL_MILLIS) — there is no faster *legal* periodic background
        // interval without switching to AlarmManager exact alarms or a foreground service, both
        // of which would violate this project's low-power non-functional requirement (see
        // ANALYSIS.md 五/6.1 and WorkScheduler.MIN_INTERVAL_MINUTES). This is purely a change to
        // which pre-existing option (15/30/60) is selected *by default* for new users — it does
        // not lower the floor itself, and existing users' own saved DataStore choice is untouched.
        const val DEFAULT_ROTATION_INTERVAL_MINUTES = 15
    }
}
