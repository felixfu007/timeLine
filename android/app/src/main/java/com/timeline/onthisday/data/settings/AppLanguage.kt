package com.timeline.onthisday.data.settings

import java.util.Locale

/**
 * M6 — App-level display language (F-07), independent of the OS system language
 * (ANALYSIS.md 五 多語言 #3: "允許使用者獨立於系統語言之外指定 App 顯示語言").
 *
 * [tag] is a BCP-47 language tag consumable both by
 * `androidx.appcompat.app.AppCompatDelegate.setApplicationLocales` (in-app UI, see
 * TimelineApplication / SettingsViewModel) and by `Locale.forLanguageTag` (Widget-side manual
 * resource resolution, see widget/HistoryWidget.kt) — kept as the single source of truth so both
 * call sites agree on what "zh-Hant" / "en" mean.
 */
enum class AppLanguage(val tag: String) {
    ZH_HANT("zh-Hant"),
    EN("en");

    fun toJavaLocale(): Locale = Locale.forLanguageTag(tag)

    companion object {
        /**
         * Default determination (F-07 AC1): system language containing "zh" maps to Traditional
         * Chinese, everything else falls back to English. Deliberately only inspects
         * [Locale.getLanguage] (not country/script) — REQUIREMENTS.md/ANALYSIS.md don't call for
         * distinguishing zh-Hans vs zh-Hant by system locale, only "系統語言含中文則 zh-Hant".
         */
        fun fromSystemDefault(locale: Locale = Locale.getDefault()): AppLanguage =
            if (locale.language.equals("zh", ignoreCase = true)) ZH_HANT else EN

        /** Resolves a persisted [tag] back to an [AppLanguage], falling back to the system default for anything unrecognized/null (e.g. first launch, no value saved yet). */
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: fromSystemDefault()
    }
}
