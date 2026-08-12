package com.timeline.onthisday.ui

import com.timeline.onthisday.data.local.entity.HistoryEventEntity
import com.timeline.onthisday.data.settings.AppLanguage

/** Shared test-data builder to keep the ViewModel/mapper tests concise. */
fun sampleEntity(
    id: Long = 1,
    month: Int = 1,
    day: Int = 1,
    year: Int = 2000,
    language: AppLanguage = AppLanguage.ZH_HANT,
    text: String = "示例事件",
    sourceTitle: String? = null,
    sourceUrl: String? = null,
    cachedAt: Long = 0L
): HistoryEventEntity = HistoryEventEntity(
    id = id,
    month = month,
    day = day,
    year = year,
    language = language.name,
    text = text,
    categoryTag = null,
    sourceTitle = sourceTitle,
    sourceUrl = sourceUrl,
    sourceUpdatedAt = cachedAt,
    cachedAt = cachedAt
)
