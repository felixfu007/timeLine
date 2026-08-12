package com.timeline.onthisday.data.settings

/**
 * Widget appearance color presets (2026-07-30 user UX request: "圓角＋框線＋使用者可選色票").
 *
 * Deliberately a *preset swatch* design rather than a free RGB/hex picker — the user explicitly
 * asked for the simpler option. To make every possible background × text × border combination
 * stay readable without writing any legality-checking logic, the three enums are each restricted
 * to a tonal range that's safe to combine with *any* option from the other two enums:
 *
 * - [WidgetBackgroundColor]: light tones only.
 * - [WidgetTextColor]: dark tones only (high contrast against any light background above).
 * - [WidgetBorderColor]: mid tones — a border is thin and visually light, so its contrast
 *   requirement is much looser than background/text; these are chosen for looking good against
 *   the light backgrounds above rather than for strict contrast.
 *
 * [argb] is a `0xAARRGGBB` literal consumed by `androidx.compose.ui.graphics.Color(Long)` at the
 * Widget/Settings-UI call sites (see widget/HistoryWidget.kt, ui/SettingsScreen.kt) — kept as a
 * plain `Long` here (no `androidx.compose`/`androidx.glance` import) so this file, like
 * [AppLanguage], stays a plain-Kotlin data-layer type with no Android/Compose dependency.
 */
enum class WidgetBackgroundColor(val argb: Long) {
    LAVENDER(0xFFEADDFFL),
    PINK(0xFFFFD9E3L),
    BLUE(0xFFD6E4FFL),
    GREEN(0xFFD9F2E3L),
    YELLOW(0xFFFFF3C4L),
    WHITE(0xFFFFFFFFL),
    GRAY(0xFFECECECL);

    companion object {
        /** Matches the widget's pre-existing default look (light lavender) so unmigrated users see no visual change. */
        val DEFAULT = LAVENDER

        fun fromName(name: String?): WidgetBackgroundColor =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

enum class WidgetTextColor(val argb: Long) {
    DARK_GRAY(0xFF1B1B1FL),
    DARK_PURPLE(0xFF3B2A5AL),
    DARK_BLUE(0xFF1B2A4AL),
    DARK_BROWN(0xFF3E2723L);

    companion object {
        /** Matches the widget's pre-existing default text tone (near-black). */
        val DEFAULT = DARK_GRAY

        fun fromName(name: String?): WidgetTextColor =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

enum class WidgetBorderColor(val argb: Long) {
    SOFT_PURPLE(0xFF8B7FA8L),
    SOFT_GRAY(0xFFB0B0B0L),
    SOFT_BLUE(0xFF6699CCL),
    SOFT_GOLD(0xFFC9A227L);

    companion object {
        /** A muted purple that reads as a natural outline on top of [WidgetBackgroundColor.DEFAULT]. */
        val DEFAULT = SOFT_PURPLE

        fun fromName(name: String?): WidgetBorderColor =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
