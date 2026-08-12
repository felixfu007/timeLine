package com.timeline.onthisday.data.repository

import com.timeline.onthisday.data.local.entity.HistoryEventEntity
import com.timeline.onthisday.data.settings.AppLanguage
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for "歷史上的今天" events, backed by Room with the Wikipedia On
 * This Day API as the remote source. Implements the "先讀快取，快取過期或缺當日資料才發網路
 * 請求" data flow described in ANALYSIS.md 三/七.
 *
 * M7 defect #1 fix: every method now takes an explicit [AppLanguage] — the zh and en On This Day
 * feeds are independently curated event lists (not translations of each other, see
 * [com.timeline.onthisday.data.local.entity.HistoryEventEntity]'s kdoc), so they're fetched and
 * cached as two separate per-language lists rather than merged into shared rows.
 */
interface HistoryEventRepository {

    /** Reactive read of whatever is currently cached for (month, day, language); never triggers network I/O. */
    fun observeEventsForDate(month: Int, day: Int, language: AppLanguage): Flow<List<HistoryEventEntity>>

    /** Reactive read of a single event by its Room id, independent of date/language filtering — see [com.timeline.onthisday.data.local.dao.HistoryEventDao.observeEventById]'s kdoc. */
    fun observeEventById(id: Long): Flow<HistoryEventEntity?>

    /**
     * Ensures fresh data is cached for (month, day, language): if a fresh-enough cache already
     * exists for that language this is a no-op, otherwise it calls that language's API and
     * replaces the cached rows for that date+language (leaving any other language's cache for
     * the same date untouched).
     *
     * On network/API failure, the previous successful cache is left untouched (F-02 #2) and
     * this function returns a failed [Result] so the caller (e.g. DailyFetchWorker in M4) can
     * decide whether to retry/back off.
     *
     * @param forceRefresh bypass the freshness check (used for pull-to-refresh style actions)
     */
    suspend fun refreshIfNeeded(
        month: Int,
        day: Int,
        language: AppLanguage,
        forceRefresh: Boolean = false
    ): Result<Unit>

    /** Prunes cached rows older than the rolling offline-availability window (ANALYSIS.md 五), across all languages. */
    suspend fun pruneStaleCache()
}
