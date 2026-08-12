package com.timeline.onthisday.data.repository

import com.timeline.onthisday.data.local.dao.HistoryEventDao
import com.timeline.onthisday.data.local.entity.HistoryEventEntity
import com.timeline.onthisday.data.remote.WikipediaOnThisDayApi
import com.timeline.onthisday.data.remote.mapper.toEntity
import com.timeline.onthisday.data.settings.AppLanguage
import com.timeline.onthisday.di.EnWikipediaApi
import com.timeline.onthisday.di.IoDispatcher
import com.timeline.onthisday.di.ZhWikipediaApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "先讀快取，快取過期或缺當日資料才發網路請求" (ANALYSIS.md 三/七) implementation.
 *
 * M7 defect #1 fix: now actually calls the [enApi] (previously wired into DI in M1 but never
 * used — see M7 QA report defect #1) whenever [AppLanguage.EN] is requested, and caches the zh
 * and en feeds as two independent per-language event lists rather than trying to merge them into
 * shared rows (see [com.timeline.onthisday.data.local.entity.HistoryEventEntity]'s kdoc for why
 * the original "same event, translated" model was wrong).
 *
 * M7 defect #3 fix: passes `Accept-Language: zh-Hant` on zh feed requests so
 * zh.wikipedia.org's LanguageConverter returns Traditional Chinese content instead of its
 * Simplified Chinese default (confirmed against the live API in the M7 developer report).
 */
@Singleton
class HistoryEventRepositoryImpl @Inject constructor(
    @ZhWikipediaApi private val zhApi: WikipediaOnThisDayApi,
    @EnWikipediaApi private val enApi: WikipediaOnThisDayApi,
    private val historyEventDao: HistoryEventDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : HistoryEventRepository {

    override fun observeEventsForDate(month: Int, day: Int, language: AppLanguage): Flow<List<HistoryEventEntity>> =
        historyEventDao.observeEventsForDate(month, day, language.name)

    override fun observeEventById(id: Long): Flow<HistoryEventEntity?> =
        historyEventDao.observeEventById(id)

    override suspend fun refreshIfNeeded(
        month: Int,
        day: Int,
        language: AppLanguage,
        forceRefresh: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (!forceRefresh && isCacheFreshEnough(month, day, language)) {
                return@withContext Result.success(Unit)
            }

            val api = if (language == AppLanguage.EN) enApi else zhApi
            val acceptLanguage = if (language == AppLanguage.EN) "en" else "zh-Hant"
            val monthPath = "%02d".format(Locale.US, month)
            val dayPath = "%02d".format(Locale.US, day)
            val response = api.getEventsOnThisDay(monthPath, dayPath, acceptLanguage)

            val now = System.currentTimeMillis()
            val entities = response.events.orEmpty()
                .filter { it.text != null } // drop malformed entries defensively (F-01 #4)
                .map { it.toEntity(month = month, day = day, language = language, cachedAt = now) }

            if (entities.isNotEmpty()) {
                // Only replace the cache when we actually got usable data; an empty/malformed
                // response must NOT wipe out a previously good cache (F-02 #2). Scoped to
                // [language] so refreshing one language's feed never clobbers the other's cache.
                historyEventDao.replaceEventsForDate(month, day, language.name, entities)
            }

            Result.success(Unit)
        } catch (e: IOException) {
            // Network failure: keep whatever was cached before, surface failure to caller.
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(e)
        }
    }

    override suspend fun pruneStaleCache() = withContext(ioDispatcher) {
        val cutoff = System.currentTimeMillis() - ROLLING_CACHE_WINDOW_MILLIS
        historyEventDao.deleteCachedBefore(cutoff)
    }

    /** True when a cache already exists for (month, day, language) and its oldest row is within the 24h TTL (F-02 #3). */
    private suspend fun isCacheFreshEnough(month: Int, day: Int, language: AppLanguage): Boolean {
        val oldestCachedAt = historyEventDao.getOldestCacheTimestamp(month, day, language.name) ?: return false
        val age = System.currentTimeMillis() - oldestCachedAt
        return age < CACHE_TTL_MILLIS
    }

    private companion object {
        val CACHE_TTL_MILLIS = TimeUnit.HOURS.toMillis(24)
        val ROLLING_CACHE_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(35)
    }
}
