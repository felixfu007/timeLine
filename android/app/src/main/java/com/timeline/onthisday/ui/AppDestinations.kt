package com.timeline.onthisday.ui

/**
 * Compose Navigation routes for the M2 two-screen app (list -> detail). Kept as plain string
 * constants (no navigation-compose type-safe routes yet) to keep this M2 addition small;
 * revisit if/when M3's widget tap-to-open (F-06) needs deep links into [DETAIL_ROUTE].
 */
object AppDestinations {
    const val LIST_ROUTE = "list"

    /** Nav-arg key shared between the NavHost route definition and HistoryDetailViewModel's SavedStateHandle lookup. */
    const val EVENT_ID_ARG = "eventId"
    const val DETAIL_ROUTE = "detail/{$EVENT_ID_ARG}"

    /** M6 — settings screen (language F-07, rotation interval F-05), reached from the list screen's TopAppBar. */
    const val SETTINGS_ROUTE = "settings"

    fun detailRoute(eventId: Long): String = "detail/$eventId"
}
