package com.timeline.onthisday.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local cache of "歷史上的今天" events for one (month, day, language), as fetched from the
 * Wikipedia On This Day API. Schema per ANALYSIS.md 三、"Room Schema（草案）", revised by the
 * M7 QA fix for defect #1.
 *
 * M7 defect #1 fix: the original schema stored a single event row with both a `textZh` and
 * `textEn` column, modeling zh/en as two translations of "the same event" (with a
 * prefer-zh-fallback-to-en read rule). That assumption was wrong — zh.wikipedia.org and
 * en.wikipedia.org's On This Day feeds are **independently curated event lists** for the same
 * calendar date; they don't line up 1:1 and are not translations of each other. Each row now
 * represents one event from exactly one language edition's feed ([language]/[text]), and the
 * zh feed and en feed are cached as two separate lists rather than merged into shared rows —
 * see HistoryEventDao's `language`-scoped queries and HistoryEventRepositoryImpl.
 *
 * Supports the offline-availability non-functional requirement: on first successful launch
 * the whole current month is pre-fetched (31 rows worth of (month, day) groups), and rows
 * older than the 35-day rolling window (ANALYSIS.md 五) are pruned — see
 * HistoryEventDao.deleteCachedBefore().
 */
@Entity(
    tableName = "history_event",
    indices = [Index(value = ["month", "day", "language"])]
)
data class HistoryEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int,
    val day: Int,
    val year: Int,
    /**
     * [com.timeline.onthisday.data.settings.AppLanguage.name] (e.g. "ZH_HANT"/"EN") of the
     * Wikipedia feed [text] was fetched from. Stored as a plain String (rather than the enum
     * type itself) so Room doesn't need a TypeConverter — callers convert via `.name`/
     * `AppLanguage.valueOf(...)`.
     */
    val language: String,
    val text: String,
    val categoryTag: String?,
    /**
     * Title/URL of the primary Wikipedia page associated with this event (first entry of the
     * API's "pages" array), added in M2 to back the event detail screen's "來源" attribution
     * (F-06 / CC BY-SA notice) without needing a separate table or JSON blob column — a single
     * page per event is enough for M2's minimal detail screen. Both null if the API response
     * didn't include a "pages" entry for this event.
     */
    val sourceTitle: String?,
    val sourceUrl: String?,
    val sourceUpdatedAt: Long,
    val cachedAt: Long
)
