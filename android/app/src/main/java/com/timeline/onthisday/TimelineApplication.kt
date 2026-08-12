package com.timeline.onthisday

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.timeline.onthisday.data.settings.SettingsRepository
import com.timeline.onthisday.locale.AppLocaleApplier
import com.timeline.onthisday.scheduler.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * Application entry point.
 *
 * Implements `Configuration.Provider` so WorkManager can create `@HiltWorker`-annotated Worker
 * classes with injected dependencies (M3's WidgetRotationWorker, M4's DailyFetchWorker).
 *
 * Reboot handling (F-02 / ANALYSIS.md 隱私 NFR): on API 26+ (this app's `minSdk`),
 * `PeriodicWorkRequest` is scheduled via the system `JobScheduler`, whose jobs are persisted by
 * the OS itself and automatically restored after a reboot — no app-declared
 * `RECEIVE_BOOT_COMPLETED` permission or custom `BroadcastReceiver` is required for that. As a
 * second line of defense, both `WorkScheduler` calls below are also (re-)invoked here on every
 * process start with `ExistingPeriodicWorkPolicy.KEEP`, so even an edge case where the OS drops a
 * persisted job self-heals the next time the app is opened.
 *
 * IMPORTANT: the default WorkManager auto-initializer (an App Startup ContentProvider) is
 * disabled in AndroidManifest.xml — ContentProviders run before Application.onCreate(), which
 * would try to read `workManagerConfiguration` before Hilt has injected [workerFactory],
 * crashing with an uninitialized-lateinit error.
 *
 * Do NOT call `WorkManager.initialize()` manually here either: WorkManager 2.7+ performs
 * "on-demand initialization" the first time `WorkManager.getInstance(context)` is called, as
 * long as the Application implements `Configuration.Provider` (see
 * https://developer.android.com/topic/libraries/architecture/workmanager/advanced/custom-configuration#on-demand).
 * That on-demand init already fires as a side effect of Hilt injecting [workScheduler] below
 * (its constructor needs a `WorkManager` instance, see di/WorkManagerModule.kt) — calling
 * `WorkManager.initialize()` afterwards would double-initialize it and crash with
 * `IllegalStateException: WorkManager is already initialized` (caught during real-device/
 * emulator verification of M3, see the M3 developer report).
 */
@HiltAndroidApp
class TimelineApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workScheduler: WorkScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var localeApplier: AppLocaleApplier

    override fun onCreate() {
        super.onCreate()

        // M6 (F-07 AC1/AC3): apply the persisted (or system-derived-default, on first launch)
        // per-app language override before any Activity is created — see AppLocaleApplier's kdoc
        // for the mechanism. A single blocking local-disk DataStore read here is deliberate and
        // accepted (ANALYSIS.md doesn't set a cold-start latency budget for M6): this must
        // complete before setContent() renders the first screen, and DataStore Preferences has
        // no synchronous API.
        val initialSettings = runBlocking { settingsRepository.currentSettings() }
        localeApplier.apply(initialSettings.language)

        // M3: ensure the widget rotation PeriodicWorkRequest is scheduled on every app start
        // (ExistingPeriodicWorkPolicy.KEEP inside WorkScheduler makes this idempotent — it's a
        // no-op if already scheduled). It also gets (re-)scheduled from
        // HistoryWidgetReceiver.onEnabled() the first time a widget instance is actually added,
        // in case the app process was killed/reinstalled without re-launching MainActivity.
        // M6 (F-05 AC1/AC2): uses the user's persisted rotation interval instead of the hardcoded
        // default (15 minutes as of 2026-07-29 — see AppSettings.DEFAULT_ROTATION_INTERVAL_MINUTES's
        // kdoc) — user-triggered changes thereafter go through
        // WorkScheduler.updateWidgetRotationInterval() (see ui/SettingsViewModel.kt) instead.
        workScheduler.scheduleWidgetRotation(initialSettings.rotationIntervalMinutes.toLong())

        // M4: ensure the once-daily fetch PeriodicWorkRequest is scheduled on every app start
        // (ExistingPeriodicWorkPolicy.KEEP makes this idempotent — a no-op if already scheduled).
        workScheduler.scheduleDailyFetch()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
