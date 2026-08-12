package com.timeline.onthisday.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            language = AppLanguage.fromTag(prefs[LANGUAGE_KEY]),
            rotationIntervalMinutes = prefs[ROTATION_INTERVAL_KEY]
                ?: AppSettings.DEFAULT_ROTATION_INTERVAL_MINUTES,
            widgetBackgroundColor = WidgetBackgroundColor.fromName(prefs[WIDGET_BACKGROUND_COLOR_KEY]),
            widgetTextColor = WidgetTextColor.fromName(prefs[WIDGET_TEXT_COLOR_KEY]),
            widgetBorderColor = WidgetBorderColor.fromName(prefs[WIDGET_BORDER_COLOR_KEY]),
            sortOrder = SortOrder.fromName(prefs[SORT_ORDER_KEY])
        )
    }

    override suspend fun currentSettings(): AppSettings = settingsFlow.first()

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[LANGUAGE_KEY] = language.tag }
    }

    override suspend fun setRotationIntervalMinutes(minutes: Int) {
        dataStore.edit { it[ROTATION_INTERVAL_KEY] = minutes }
    }

    override suspend fun setWidgetBackgroundColor(color: WidgetBackgroundColor) {
        dataStore.edit { it[WIDGET_BACKGROUND_COLOR_KEY] = color.name }
    }

    override suspend fun setWidgetTextColor(color: WidgetTextColor) {
        dataStore.edit { it[WIDGET_TEXT_COLOR_KEY] = color.name }
    }

    override suspend fun setWidgetBorderColor(color: WidgetBorderColor) {
        dataStore.edit { it[WIDGET_BORDER_COLOR_KEY] = color.name }
    }

    override suspend fun setSortOrder(sortOrder: SortOrder) {
        dataStore.edit { it[SORT_ORDER_KEY] = sortOrder.name }
    }

    companion object {
        val LANGUAGE_KEY: Preferences.Key<String> = stringPreferencesKey("language")
        val ROTATION_INTERVAL_KEY: Preferences.Key<Int> = intPreferencesKey("rotation_interval_minutes")

        // Stored as the enum's .name (e.g. "LAVENDER") rather than the raw ARGB Long/hex string —
        // keeps the persisted value stable even if a color's exact hex is retuned later, same
        // rationale as AppLanguage's tag-based lookup via fromTag/fromName.
        val WIDGET_BACKGROUND_COLOR_KEY: Preferences.Key<String> = stringPreferencesKey("widget_background_color")
        val WIDGET_TEXT_COLOR_KEY: Preferences.Key<String> = stringPreferencesKey("widget_text_color")
        val WIDGET_BORDER_COLOR_KEY: Preferences.Key<String> = stringPreferencesKey("widget_border_color")

        // Stored as the enum's .name (e.g. "YEAR_DESCENDING"), same rationale as the color keys above.
        val SORT_ORDER_KEY: Preferences.Key<String> = stringPreferencesKey("sort_order")
    }
}
