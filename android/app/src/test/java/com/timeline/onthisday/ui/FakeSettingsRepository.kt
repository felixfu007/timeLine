package com.timeline.onthisday.ui

import com.timeline.onthisday.data.settings.AppLanguage
import com.timeline.onthisday.data.settings.AppSettings
import com.timeline.onthisday.data.settings.SettingsRepository
import com.timeline.onthisday.data.settings.SortOrder
import com.timeline.onthisday.data.settings.WidgetBackgroundColor
import com.timeline.onthisday.data.settings.WidgetBorderColor
import com.timeline.onthisday.data.settings.WidgetTextColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Controllable in-memory fake for [SettingsViewModel] unit tests. */
class FakeSettingsRepository(
    initial: AppSettings = AppSettings(
        language = AppLanguage.ZH_HANT,
        rotationIntervalMinutes = AppSettings.DEFAULT_ROTATION_INTERVAL_MINUTES
    )
) : SettingsRepository {

    private val state = MutableStateFlow(initial)

    override val settingsFlow: Flow<AppSettings> = state

    override suspend fun currentSettings(): AppSettings = state.value

    override suspend fun setLanguage(language: AppLanguage) {
        state.value = state.value.copy(language = language)
    }

    override suspend fun setRotationIntervalMinutes(minutes: Int) {
        state.value = state.value.copy(rotationIntervalMinutes = minutes)
    }

    override suspend fun setWidgetBackgroundColor(color: WidgetBackgroundColor) {
        state.value = state.value.copy(widgetBackgroundColor = color)
    }

    override suspend fun setWidgetTextColor(color: WidgetTextColor) {
        state.value = state.value.copy(widgetTextColor = color)
    }

    override suspend fun setWidgetBorderColor(color: WidgetBorderColor) {
        state.value = state.value.copy(widgetBorderColor = color)
    }

    override suspend fun setSortOrder(sortOrder: SortOrder) {
        state.value = state.value.copy(sortOrder = sortOrder)
    }
}
