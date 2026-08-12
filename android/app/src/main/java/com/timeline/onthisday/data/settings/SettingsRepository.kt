package com.timeline.onthisday.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * M6 — single source of truth for user-configurable settings (F-05 rotation interval, F-07
 * language), backed by Jetpack DataStore Preferences (ANALYSIS.md 三 "設定持久化").
 */
interface SettingsRepository {

    /** Reactive read of the current settings; emits a default-derived value before the user has ever changed anything. */
    val settingsFlow: Flow<AppSettings>

    /** One-shot read, e.g. for [com.timeline.onthisday.TimelineApplication]'s startup locale/rotation-interval bootstrap. */
    suspend fun currentSettings(): AppSettings

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setRotationIntervalMinutes(minutes: Int)

    /** Widget 外觀自訂 (2026-07-30 user UX request) — preset swatch selections, see WidgetColorPalette.kt. */
    suspend fun setWidgetBackgroundColor(color: WidgetBackgroundColor)

    suspend fun setWidgetTextColor(color: WidgetTextColor)

    suspend fun setWidgetBorderColor(color: WidgetBorderColor)

    /** List-screen sort order (2026-07-30 user request), see [SortOrder]'s kdoc. */
    suspend fun setSortOrder(sortOrder: SortOrder)
}
