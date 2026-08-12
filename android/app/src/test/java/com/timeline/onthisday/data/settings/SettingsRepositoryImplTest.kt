package com.timeline.onthisday.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule

/**
 * JVM unit test against a real [DataStore] backed by a temp file (no Robolectric/instrumented
 * test needed — `PreferenceDataStoreFactory.create` doesn't require an Android `Context`, only a
 * `produceFile` lambda, per DataStore's documented testing pattern).
 */
class SettingsRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        val file = File(tempFolder.newFolder(), "test.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        repository = SettingsRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        // Nothing to explicitly close — DataStore has no close() API; the temp folder rule
        // deletes the backing file after the test.
    }

    @Test
    fun currentSettings_returnsSystemDefaultLanguageAndDefaultInterval_beforeAnyWrite() = runTest {
        val settings = repository.currentSettings()

        assertEquals(AppLanguage.fromSystemDefault(), settings.language)
        assertEquals(AppSettings.DEFAULT_ROTATION_INTERVAL_MINUTES, settings.rotationIntervalMinutes)
        assertEquals(WidgetBackgroundColor.DEFAULT, settings.widgetBackgroundColor)
        assertEquals(WidgetTextColor.DEFAULT, settings.widgetTextColor)
        assertEquals(WidgetBorderColor.DEFAULT, settings.widgetBorderColor)
        assertEquals(SortOrder.DEFAULT, settings.sortOrder)
    }

    @Test
    fun setLanguage_persistsAndIsReflectedInCurrentSettings() = runTest {
        repository.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, repository.currentSettings().language)

        repository.setLanguage(AppLanguage.ZH_HANT)
        assertEquals(AppLanguage.ZH_HANT, repository.currentSettings().language)
    }

    @Test
    fun setRotationIntervalMinutes_persistsAndIsReflectedInCurrentSettings() = runTest {
        repository.setRotationIntervalMinutes(15)
        assertEquals(15, repository.currentSettings().rotationIntervalMinutes)

        repository.setRotationIntervalMinutes(60)
        assertEquals(60, repository.currentSettings().rotationIntervalMinutes)
    }

    @Test
    fun settingsFlow_emitsUpdatedValueAfterWrite() = runTest {
        repository.setLanguage(AppLanguage.EN)
        repository.setRotationIntervalMinutes(60)

        val latest = repository.settingsFlow.first { it.rotationIntervalMinutes == 60 }
        assertEquals(AppLanguage.EN, latest.language)
        assertEquals(60, latest.rotationIntervalMinutes)
    }

    @Test
    fun setWidgetBackgroundColor_persistsAndIsReflectedInCurrentSettings() = runTest {
        repository.setWidgetBackgroundColor(WidgetBackgroundColor.GREEN)
        assertEquals(WidgetBackgroundColor.GREEN, repository.currentSettings().widgetBackgroundColor)

        repository.setWidgetBackgroundColor(WidgetBackgroundColor.WHITE)
        assertEquals(WidgetBackgroundColor.WHITE, repository.currentSettings().widgetBackgroundColor)
    }

    @Test
    fun setWidgetTextColor_persistsAndIsReflectedInCurrentSettings() = runTest {
        repository.setWidgetTextColor(WidgetTextColor.DARK_PURPLE)
        assertEquals(WidgetTextColor.DARK_PURPLE, repository.currentSettings().widgetTextColor)
    }

    @Test
    fun setWidgetBorderColor_persistsAndIsReflectedInCurrentSettings() = runTest {
        repository.setWidgetBorderColor(WidgetBorderColor.SOFT_BLUE)
        assertEquals(WidgetBorderColor.SOFT_BLUE, repository.currentSettings().widgetBorderColor)
    }

    @Test
    fun setSortOrder_persistsAndIsReflectedInCurrentSettings() = runTest {
        repository.setSortOrder(SortOrder.YEAR_DESCENDING)
        assertEquals(SortOrder.YEAR_DESCENDING, repository.currentSettings().sortOrder)

        repository.setSortOrder(SortOrder.YEAR_ASCENDING)
        assertEquals(SortOrder.YEAR_ASCENDING, repository.currentSettings().sortOrder)
    }
}
