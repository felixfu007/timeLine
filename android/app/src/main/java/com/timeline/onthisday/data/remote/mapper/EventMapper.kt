package com.timeline.onthisday.data.remote.mapper

import com.timeline.onthisday.data.local.entity.HistoryEventEntity
import com.timeline.onthisday.data.remote.dto.EventDto
import com.timeline.onthisday.data.settings.AppLanguage

/**
 * Maps a Wikipedia API [EventDto] into the Room [HistoryEventEntity] used for local caching.
 *
 * M7 defect #1 fix: previously took a dedicated `EventLanguage` enum and wrote into whichever of
 * `textZh`/`textEn` matched, leaving the other column null (the "same event, two languages"
 * model — since disproven, see HistoryEventEntity's kdoc: zh.wikipedia.org and en.wikipedia.org's
 * On This Day feeds are independently curated event lists, not translations of each other). Now
 * takes the shared [AppLanguage] type (the same one driving both the UI display-language setting
 * and which Wikipedia feed was fetched) and always writes a single non-null
 * [HistoryEventEntity.text] tagged with [HistoryEventEntity.language]. Callers
 * (HistoryEventRepositoryImpl) are expected to have already filtered out entries with a null
 * `text` (F-01 #4 defensive parsing) before calling this, so `text.orEmpty()` here is a
 * last-resort safety net rather than the primary null-handling path.
 *
 * @param month 1..12, the month this event was requested/fetched for
 * @param day 1..31, the day this event was requested/fetched for
 * @param language which Wikipedia language edition [EventDto.text] was fetched from
 * @param cachedAt wall-clock time (epoch millis) this row is being written to the DB
 */
fun EventDto.toEntity(
    month: Int,
    day: Int,
    language: AppLanguage,
    cachedAt: Long
): HistoryEventEntity {
    val primaryPage = pages?.firstOrNull()
    val primaryPageUrl = primaryPage?.contentUrls?.desktop?.page ?: primaryPage?.contentUrls?.mobile?.page

    return HistoryEventEntity(
        month = month,
        day = day,
        year = year ?: 0,
        language = language.name,
        text = text.orEmpty(),
        categoryTag = null, // F-08 not implemented in Phase 1/2, see ANALYSIS.md 六、2
        sourceTitle = primaryPage?.title,
        sourceUrl = primaryPageUrl,
        sourceUpdatedAt = cachedAt,
        cachedAt = cachedAt
    )
}
