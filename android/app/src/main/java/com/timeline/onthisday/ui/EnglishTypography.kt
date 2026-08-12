package com.timeline.onthisday.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.timeline.onthisday.R

/**
 * UI 美化（2026-07-30，第二輪）：當 App 顯示語言為 English 時，讓整個英文介面更有復古感——但標題與
 * 內文分開處理，因為 Cinzel 是「展示用」的仿羅馬石刻銘文字體，字重統一、字符間距寬，小字級長文閱讀
 * 吃力；內文改用 **EB Garamond**（Google Fonts，仿 16 世紀 Claude Garamond 經典襯線體再製版，SIL OFL
 * 1.1 授權，可自由嵌入 App 免費使用），專為長文閱讀設計，小字級下可讀性遠優於 Cinzel。
 *
 * 這個檔案只處理「畫面標題」與「一般內文」兩種西文字體的**選用邏輯**；實際套用到哪些 Text 元件、範圍
 * 多大，由各 Screen 檔案（HistoryListScreen/HistoryDetailScreen/SettingsScreen）自行決定——刻意不把
 * 這個判斷做成一個全域主題（[androidx.compose.material3.Typography]/CompositionLocal）套用到所有
 * Text，是因為「年份數字」已經有獨立的 Cinzel 套用邏輯（見 [YearTypography.kt]，任何語言模式都套用），
 * 若在此再疊加一層全域字體，會讓「年份 SpanStyle 覆蓋 Text 預設字體」這個既有機制的行為變得難以推理。
 *
 * 只在語言為 zh-Hant 時，不套用這兩個字體（沿用系統預設字體）——因為它們都是西文字型、不含中文字符，
 * 對中文文字套用不會有效果，反而可能讓程式碼看起來像是「支援中文」但其實沒有，故明確用 [isEnglishAppLocale]
 * 收斂為條件式套用，而非仰賴字型 fallback 自然發生。
 *
 * EB Garamond 字體檔取得方式：上游 Google Fonts 原始庫（github.com/google/fonts, ofl/ebgaramond）僅提供
 * `EBGaramond[wght].ttf` 可變字體（variable font），本機建置環境沒有 fonttools 可自行切出靜態字重實例；
 * 改採 Google Fonts CSS API（`fonts.googleapis.com/css2`）搭配舊版 User-Agent（觸發伺服器端回傳靜態 TTF
 * 而非 WOFF2/可變字體）分別取得 Regular（400）與 Bold（700）兩個靜態字重的 `.ttf`，與 Cinzel 當初的處理
 * 手法一致。授權同為 SIL Open Font License 1.1。
 */
val EnglishTitleFontFamily: FontFamily = CinzelFontFamily

val EnglishBodyFontFamily: FontFamily = FontFamily(
    Font(R.font.eb_garamond_regular, FontWeight.Normal),
    Font(R.font.eb_garamond_bold, FontWeight.Bold)
)

/**
 * Whether the app's *current display language* (F-07, App-level, independent of the OS system
 * language — see [com.timeline.onthisday.data.settings.AppLanguage]) is English.
 *
 * Reads [LocalConfiguration] rather than injecting `SettingsRepository`/a ViewModel into every
 * screen: `AppCompatDelegate.setApplicationLocales()` (see `locale/AppCompatLocaleApplier.kt`)
 * recreates [com.timeline.onthisday.MainActivity] on language change (see that class's kdoc), so
 * by the time any Composable in the new Composition runs, `LocalConfiguration.current`'s resolved
 * locale already agrees with whichever `values`/`values-en` string resources `stringResource()`
 * picked for the very same recomposition — i.e. this stays in lockstep with the string resources
 * already rendered on screen without needing separate plumbing.
 */
@Composable
fun isEnglishAppLocale(): Boolean {
    val locales = LocalConfiguration.current.locales
    val locale = if (!locales.isEmpty) locales[0] else java.util.Locale.getDefault()
    return locale.language.equals("en", ignoreCase = true)
}

/** [EnglishTitleFontFamily] when the current app language is English, `null` (system default) otherwise. */
@Composable
fun englishTitleFontFamilyOrNull(): FontFamily? = if (isEnglishAppLocale()) EnglishTitleFontFamily else null

/** [EnglishBodyFontFamily] when the current app language is English, `null` (system default) otherwise. */
@Composable
fun englishBodyFontFamilyOrNull(): FontFamily? = if (isEnglishAppLocale()) EnglishBodyFontFamily else null
