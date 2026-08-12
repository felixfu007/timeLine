package com.timeline.onthisday.ui

import com.timeline.onthisday.data.settings.AppLanguage
import com.timeline.onthisday.data.settings.AppSettings
import com.timeline.onthisday.data.settings.WidgetBackgroundColor
import com.timeline.onthisday.data.settings.WidgetBorderColor
import com.timeline.onthisday.data.settings.WidgetTextColor
import com.timeline.onthisday.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Covers M6's F-05 AC2 (rotation interval change reschedules) / F-07 AC1/AC2 (language change
 * persists + applies) using fakes so no real DataStore/WorkManager/AppCompatDelegate is touched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_reflectsWhateverIsCurrentlyInSettingsRepository() = runTest {
        val repository = FakeSettingsRepository(
            initial = AppSettings(language = AppLanguage.EN, rotationIntervalMinutes = 60)
        )
        val viewModel = SettingsViewModel(repository, FakeWidgetRotationScheduler(), FakeAppLocaleApplier(), FakeWidgetRefresher())

        val states = mutableListOf<AppSettings>()
        val collectJob = launch { viewModel.uiState.collect { states.add(it) } }
        runCurrent()

        assertEquals(AppLanguage.EN, states.last().language)
        assertEquals(60, states.last().rotationIntervalMinutes)
        collectJob.cancel()
    }

    @Test
    fun setLanguage_persistsToRepository_andAppliesViaLocaleApplier() = runTest {
        val repository = FakeSettingsRepository()
        val localeApplier = FakeAppLocaleApplier()
        val viewModel = SettingsViewModel(repository, FakeWidgetRotationScheduler(), localeApplier, FakeWidgetRefresher())

        viewModel.setLanguage(AppLanguage.EN)
        advanceUntilIdle()

        assertEquals(AppLanguage.EN, repository.currentSettings().language)
        assertEquals(listOf(AppLanguage.EN), localeApplier.appliedLanguages)
    }

    @Test
    fun setRotationIntervalMinutes_persistsToRepository_andReschedulesWidgetRotation() = runTest {
        val repository = FakeSettingsRepository()
        val rotationScheduler = FakeWidgetRotationScheduler()
        val viewModel = SettingsViewModel(repository, rotationScheduler, FakeAppLocaleApplier(), FakeWidgetRefresher())

        viewModel.setRotationIntervalMinutes(15)
        advanceUntilIdle()

        assertEquals(15, repository.currentSettings().rotationIntervalMinutes)
        assertEquals(listOf(15L), rotationScheduler.recordedIntervals)
    }

    @Test
    fun changingIntervalTwice_reschedulesEachTimeWithTheLatestValue() = runTest {
        val repository = FakeSettingsRepository()
        val rotationScheduler = FakeWidgetRotationScheduler()
        val viewModel = SettingsViewModel(repository, rotationScheduler, FakeAppLocaleApplier(), FakeWidgetRefresher())

        viewModel.setRotationIntervalMinutes(15)
        viewModel.setRotationIntervalMinutes(60)
        advanceUntilIdle()

        assertEquals(listOf(15L, 60L), rotationScheduler.recordedIntervals)
        assertEquals(60, repository.currentSettings().rotationIntervalMinutes)
    }

    @Test
    fun setWidgetBackgroundColor_persistsToRepository_andRefreshesWidgetImmediately() = runTest {
        val repository = FakeSettingsRepository()
        val widgetRefresher = FakeWidgetRefresher()
        val viewModel = SettingsViewModel(repository, FakeWidgetRotationScheduler(), FakeAppLocaleApplier(), widgetRefresher)

        viewModel.setWidgetBackgroundColor(WidgetBackgroundColor.BLUE)
        advanceUntilIdle()

        assertEquals(WidgetBackgroundColor.BLUE, repository.currentSettings().widgetBackgroundColor)
        assertEquals(1, widgetRefresher.refreshCount)
    }

    @Test
    fun setWidgetTextColor_persistsToRepository_andRefreshesWidgetImmediately() = runTest {
        val repository = FakeSettingsRepository()
        val widgetRefresher = FakeWidgetRefresher()
        val viewModel = SettingsViewModel(repository, FakeWidgetRotationScheduler(), FakeAppLocaleApplier(), widgetRefresher)

        viewModel.setWidgetTextColor(WidgetTextColor.DARK_BLUE)
        advanceUntilIdle()

        assertEquals(WidgetTextColor.DARK_BLUE, repository.currentSettings().widgetTextColor)
        assertEquals(1, widgetRefresher.refreshCount)
    }

    @Test
    fun setWidgetBorderColor_persistsToRepository_andRefreshesWidgetImmediately() = runTest {
        val repository = FakeSettingsRepository()
        val widgetRefresher = FakeWidgetRefresher()
        val viewModel = SettingsViewModel(repository, FakeWidgetRotationScheduler(), FakeAppLocaleApplier(), widgetRefresher)

        viewModel.setWidgetBorderColor(WidgetBorderColor.SOFT_GOLD)
        advanceUntilIdle()

        assertEquals(WidgetBorderColor.SOFT_GOLD, repository.currentSettings().widgetBorderColor)
        assertEquals(1, widgetRefresher.refreshCount)
    }
}
