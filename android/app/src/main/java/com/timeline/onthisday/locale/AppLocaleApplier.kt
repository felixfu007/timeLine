package com.timeline.onthisday.locale

import com.timeline.onthisday.data.settings.AppLanguage

/**
 * Narrow interface around applying the app's per-app display language (F-07 AC1/AC2), extracted
 * out of [com.timeline.onthisday.ui.SettingsViewModel] / [com.timeline.onthisday.TimelineApplication]
 * purely so those classes stay unit-testable on the plain JVM: the real implementation
 * ([AppCompatLocaleApplier]) calls `androidx.appcompat.app.AppCompatDelegate`, which internally
 * touches Android framework APIs that aren't available (and would throw) under a plain JVM
 * `./gradlew test` run outside Robolectric/instrumentation.
 */
interface AppLocaleApplier {
    fun apply(language: AppLanguage)
}
