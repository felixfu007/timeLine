package com.timeline.onthisday.data.settings

/**
 * List-screen sort order for the "今天歷史事件" list (user-requested feature, 2026-07-30: "清單列表
 * 加入多種排序功能"). Deliberately just two options — sorting by [HistoryEventUiModel][
 * com.timeline.onthisday.ui.HistoryEventUiModel]`.year`, which is a signed year (negative = BCE,
 * e.g. `-587`), so "ascending"/"descending" is unambiguous and doesn't need a third "relevance"/
 * "default" concept: [YEAR_ASCENDING] already matches the API's/DB's pre-existing default order
 * (see [com.timeline.onthisday.data.local.dao.HistoryEventDao]'s `ORDER BY year ASC` query), so it
 * doubles as the "baseline" behavior unmigrated users keep seeing by default.
 */
enum class SortOrder {
    YEAR_ASCENDING,
    YEAR_DESCENDING;

    companion object {
        /** Matches the list's pre-existing (DB query) default order — see this enum's kdoc. */
        val DEFAULT = YEAR_ASCENDING

        fun fromName(name: String?): SortOrder =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
