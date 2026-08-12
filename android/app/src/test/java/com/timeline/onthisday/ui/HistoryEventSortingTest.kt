package com.timeline.onthisday.ui

import com.timeline.onthisday.data.settings.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 2026-07-30 user request ("清單列表加入多種排序功能") — pure JVM tests for [sortedByOrder],
 * independent of Compose/ViewModel/Room, covering the boundary cases the task calls out
 * explicitly: multiple events sharing the same year, and BCE (negative) years, which must be
 * compared as signed [Int]s rather than as strings (a naive string sort would put "-30" before
 * "-587" because '3' < '5' lexicographically, which is the wrong chronological order).
 */
class HistoryEventSortingTest {

    private fun event(id: Long, year: Int, text: String = "event-$id") = HistoryEventUiModel(
        id = id,
        year = year,
        displayText = text,
        sourceTitle = null,
        sourceUrl = null
    )

    @Test
    fun yearAscending_ordersFromOldestToNewest() {
        val events = listOf(
            event(id = 1, year = 1969),
            event(id = 2, year = 1356),
            event(id = 3, year = 2001)
        )

        val sorted = events.sortedByOrder(SortOrder.YEAR_ASCENDING)

        assertEquals(listOf(1356, 1969, 2001), sorted.map { it.year })
    }

    @Test
    fun yearDescending_ordersFromNewestToOldest() {
        val events = listOf(
            event(id = 1, year = 1969),
            event(id = 2, year = 1356),
            event(id = 3, year = 2001)
        )

        val sorted = events.sortedByOrder(SortOrder.YEAR_DESCENDING)

        assertEquals(listOf(2001, 1969, 1356), sorted.map { it.year })
    }

    @Test
    fun negativeBceYears_sortCorrectlyAscending_asSignedIntsNotStrings() {
        // -587 (BCE) must come before -30 (BCE), which must come before 238 (CE) — a string
        // comparison of "-587"/"-30"/"238" would incorrectly put "-30" first.
        val events = listOf(
            event(id = 1, year = 238),
            event(id = 2, year = -30),
            event(id = 3, year = -587)
        )

        val sorted = events.sortedByOrder(SortOrder.YEAR_ASCENDING)

        assertEquals(listOf(-587, -30, 238), sorted.map { it.year })
    }

    @Test
    fun negativeBceYears_sortCorrectlyDescending_asSignedIntsNotStrings() {
        val events = listOf(
            event(id = 1, year = -587),
            event(id = 2, year = 238),
            event(id = 3, year = -30)
        )

        val sorted = events.sortedByOrder(SortOrder.YEAR_DESCENDING)

        assertEquals(listOf(238, -30, -587), sorted.map { it.year })
    }

    @Test
    fun eventsSharingTheSameYear_areKeptInOriginalRelativeOrder_ascending() {
        // Stable sort: ties (same year) must not be reshuffled relative to each other.
        val events = listOf(
            event(id = 1, year = 1969, text = "first 1969 event"),
            event(id = 2, year = 1356),
            event(id = 3, year = 1969, text = "second 1969 event")
        )

        val sorted = events.sortedByOrder(SortOrder.YEAR_ASCENDING)

        assertEquals(listOf(2L, 1L, 3L), sorted.map { it.id })
    }

    @Test
    fun eventsSharingTheSameYear_areKeptInOriginalRelativeOrder_descending() {
        val events = listOf(
            event(id = 1, year = 1969, text = "first 1969 event"),
            event(id = 2, year = 1356),
            event(id = 3, year = 1969, text = "second 1969 event")
        )

        val sorted = events.sortedByOrder(SortOrder.YEAR_DESCENDING)

        assertEquals(listOf(1L, 3L, 2L), sorted.map { it.id })
    }

    @Test
    fun emptyList_sortsToEmptyList() {
        assertEquals(emptyList<HistoryEventUiModel>(), emptyList<HistoryEventUiModel>().sortedByOrder(SortOrder.YEAR_ASCENDING))
        assertEquals(emptyList<HistoryEventUiModel>(), emptyList<HistoryEventUiModel>().sortedByOrder(SortOrder.YEAR_DESCENDING))
    }

    @Test
    fun singleEventList_isUnchangedRegardlessOfOrder() {
        val events = listOf(event(id = 1, year = -243))

        assertEquals(events, events.sortedByOrder(SortOrder.YEAR_ASCENDING))
        assertEquals(events, events.sortedByOrder(SortOrder.YEAR_DESCENDING))
    }
}
