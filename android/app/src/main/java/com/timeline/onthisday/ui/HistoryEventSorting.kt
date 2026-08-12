package com.timeline.onthisday.ui

import com.timeline.onthisday.data.settings.SortOrder

/**
 * Applies the user's chosen [SortOrder] to an already-fetched list of events (2026-07-30 user
 * request: "清單列表加入多種排序功能"). Pure/no-Compose-dependency on purpose so it's directly unit
 * testable on the JVM (see HistoryEventSortingTest).
 *
 * Sorts by [HistoryEventUiModel.year] as a signed [Int] comparison — not a `String`/lexicographic
 * comparison — so BCE years (negative, e.g. `-587`) order correctly relative to both other BCE
 * years and all CE years (`-587 < -30 < 238`), rather than a naive string-based sort which would
 * mis-order negative numbers (e.g. "-30" sorting before "-587" alphabetically). `sortedBy`/
 * `sortedByDescending` are stable sorts, so multiple events sharing the same [year] keep their
 * original relative order instead of being shuffled.
 */
fun List<HistoryEventUiModel>.sortedByOrder(order: SortOrder): List<HistoryEventUiModel> =
    when (order) {
        SortOrder.YEAR_ASCENDING -> sortedBy { it.year }
        SortOrder.YEAR_DESCENDING -> sortedByDescending { it.year }
    }
