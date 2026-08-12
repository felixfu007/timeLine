package com.timeline.onthisday.ui

import com.timeline.onthisday.data.settings.AppLanguage
import com.timeline.onthisday.locale.AppLocaleApplier

/** Records calls made by [SettingsViewModel] without touching `AppCompatDelegate`. */
class FakeAppLocaleApplier : AppLocaleApplier {

    val appliedLanguages = mutableListOf<AppLanguage>()

    override fun apply(language: AppLanguage) {
        appliedLanguages += language
    }
}
