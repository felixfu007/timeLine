package com.timeline.onthisday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timeline.onthisday.data.settings.AppLanguage
import com.timeline.onthisday.data.settings.AppSettings
import com.timeline.onthisday.data.settings.SettingsRepository
import com.timeline.onthisday.data.settings.WidgetBackgroundColor
import com.timeline.onthisday.data.settings.WidgetBorderColor
import com.timeline.onthisday.data.settings.WidgetTextColor
import com.timeline.onthisday.locale.AppLocaleApplier
import com.timeline.onthisday.scheduler.WidgetRotationScheduler
import com.timeline.onthisday.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * M6 — settings screen (F-05 rotation interval, F-07 language, Widget appearance colors)
 * ViewModel.
 *
 * [rotationScheduler], [localeApplier] and [widgetRefresher] are typed as narrow interfaces (not
 * the concrete `WorkScheduler`/`AppCompatDelegate`/`GlanceAppWidget` call) purely so this class
 * stays unit-testable on the JVM with fakes — see SettingsViewModelTest.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val rotationScheduler: WidgetRotationScheduler,
    private val localeApplier: AppLocaleApplier,
    private val widgetRefresher: WidgetRefresher
) : ViewModel() {

    val uiState: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = AppSettings(
            language = AppLanguage.fromSystemDefault(),
            rotationIntervalMinutes = AppSettings.DEFAULT_ROTATION_INTERVAL_MINUTES
        )
    )

    /**
     * F-07 AC2: persists the new language, then applies it via `AppCompatDelegate` so the
     * currently-visible (AppCompatActivity-hosted) UI re-renders immediately — see
     * MainActivity's kdoc for why this Activity recreate happens automatically.
     */
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            localeApplier.apply(language)
        }
    }

    /**
     * F-05 AC2: persists the new interval, then immediately reschedules
     * [com.timeline.onthisday.scheduler.WidgetRotationWorker]'s `PeriodicWorkRequest` so it takes
     * effect starting from the next tick rather than waiting for the app's next cold start.
     */
    fun setRotationIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setRotationIntervalMinutes(minutes)
            rotationScheduler.updateWidgetRotationInterval(minutes.toLong())
        }
    }

    /**
     * Best-effort debounce around [widgetRefresher] calls (2026-07-30 emulator verification
     * finding): each `set widget*Color` setter below persists via its own `viewModelScope.launch`
     * and then asks the widget to repaint. If a user changes background/text/border in quick
     * succession (multiple taps within roughly a second), firing one independent `refreshNow()`
     * per tap could enqueue several concurrent Glance recompositions for the same widget; which
     * one's RemoteViews ends up visibly applied last is decided by Glance's own internal session
     * handling (not part of its public API), so it isn't guaranteed to be the one holding the
     * most recently-persisted values. Cancelling any still-pending previous refresh job before
     * starting a new one measurably reduces how often this happens, but — confirmed empirically
     * during 2026-07-30 verification — does not fully eliminate it for taps only ~1s apart, since
     * cancelling this wrapper coroutine can't reach into an already-enqueued Glance session.
     * [SettingsRepository]'s persisted values are unaffected either way (writes are not raced,
     * only the widget's *visible* repaint can transiently lag); the display self-corrects on the
     * very next widget repaint of any kind — another settings change, a tap on the widget, or
     * [com.timeline.onthisday.scheduler.WidgetRotationWorker]'s next periodic tick — since that
     * next repaint always reads [SettingsRepository.currentSettings] fresh. Known limitation,
     * not pursued further given how narrow and self-healing it is.
     */
    private var widgetRefreshJob: Job? = null

    private fun refreshWidgetDebounced() {
        widgetRefreshJob?.cancel()
        widgetRefreshJob = viewModelScope.launch {
            widgetRefresher.refreshNow()
        }
    }

    /**
     * Widget 外觀自訂 (2026-07-30 user UX request): persists the new preset color, then triggers an
     * immediate repaint via [widgetRefresher] — see that interface's kdoc for why this (unlike
     * [setLanguage]/[setRotationIntervalMinutes]) doesn't wait for the next rotation tick.
     */
    fun setWidgetBackgroundColor(color: WidgetBackgroundColor) {
        viewModelScope.launch {
            settingsRepository.setWidgetBackgroundColor(color)
            refreshWidgetDebounced()
        }
    }

    fun setWidgetTextColor(color: WidgetTextColor) {
        viewModelScope.launch {
            settingsRepository.setWidgetTextColor(color)
            refreshWidgetDebounced()
        }
    }

    fun setWidgetBorderColor(color: WidgetBorderColor) {
        viewModelScope.launch {
            settingsRepository.setWidgetBorderColor(color)
            refreshWidgetDebounced()
        }
    }
}
