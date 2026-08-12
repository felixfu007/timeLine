package com.timeline.onthisday.ui

import com.timeline.onthisday.data.local.entity.HistoryEventEntity

/**
 * UI-layer view of a cached event, decoupled from the Room [HistoryEventEntity].
 *
 * M7 defect #1 fix: previously applied a "prefer zh, fall back to en" merge across a single
 * event's `textZh`/`textEn` columns (ANALYSIS.md 四's F-07 #3 rule, since found to rest on a
 * wrong data-model assumption — see HistoryEventEntity's kdoc). Now each [HistoryEventEntity]
 * already belongs to exactly one language's independently-fetched event list (the caller,
 * e.g. HistoryListViewModel, queries the repository for the currently-selected
 * [com.timeline.onthisday.data.settings.AppLanguage]'s feed directly) — this mapper no longer
 * does any cross-language fallback, it only defends against a blank/missing [entity.text] (F-01
 * #4: never surface a blank row).
 */
data class HistoryEventUiModel(
    val id: Long,
    val year: Int,
    val displayText: String,
    val sourceTitle: String?,
    val sourceUrl: String?
)

/**
 * Maps cached entities (already scoped to a single date+language by the caller's query) to UI
 * models, dropping any entity whose text is blank (defensive — the repository already filters
 * these out on write, but the UI layer should never assume that invariant blindly).
 */
fun List<HistoryEventEntity>.toUiModels(): List<HistoryEventUiModel> =
    mapNotNull { entity ->
        if (entity.text.isBlank()) return@mapNotNull null

        HistoryEventUiModel(
            id = entity.id,
            year = entity.year,
            displayText = entity.text,
            sourceTitle = entity.sourceTitle,
            sourceUrl = entity.sourceUrl
        )
    }
