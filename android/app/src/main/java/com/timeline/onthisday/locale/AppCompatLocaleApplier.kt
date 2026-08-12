package com.timeline.onthisday.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.timeline.onthisday.data.settings.AppLanguage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [AppLocaleApplier]: uses the Jetpack-recommended per-app language API
 * (`AppCompatDelegate.setApplicationLocales`), backed by the platform `LocaleManager` on API 33+
 * and by AppCompat's own Activity-recreate machinery below that (see
 * [com.timeline.onthisday.MainActivity]'s kdoc for why it extends `AppCompatActivity`).
 */
@Singleton
class AppCompatLocaleApplier @Inject constructor() : AppLocaleApplier {
    override fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
