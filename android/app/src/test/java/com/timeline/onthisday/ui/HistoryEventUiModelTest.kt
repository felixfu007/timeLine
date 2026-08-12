package com.timeline.onthisday.ui

import com.timeline.onthisday.data.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M7 defect #1 fix: previously verified a "prefer zh, fall back to en" merge rule across a
 * single event's `textZh`/`textEn` columns (since found to rest on a wrong data-model
 * assumption — zh/en are independently curated feeds, not translations of the same event; see
 * [com.timeline.onthisday.data.local.entity.HistoryEventEntity]'s kdoc). Those cross-language
 * fallback test cases no longer apply and have been replaced: each entity already belongs to
 * exactly one language (the caller queries the repository per-language), so this now only
 * verifies the mapper (a) passes through [text] as-is and (b) still never surfaces a blank row
 * (F-01 #4).
 */
class HistoryEventUiModelTest {

    @Test
    fun mapsEntityTextDirectlyAsDisplayText() {
        val entities = listOf(sampleEntity(id = 1, language = AppLanguage.ZH_HANT, text = "中文事件"))

        val uiModels = entities.toUiModels()

        assertEquals(1, uiModels.size)
        assertEquals("中文事件", uiModels.first().displayText)
    }

    @Test
    fun preservesEnglishTextUnchanged() {
        val entities = listOf(sampleEntity(id = 1, language = AppLanguage.EN, text = "English event"))

        val uiModels = entities.toUiModels()

        assertEquals(1, uiModels.size)
        assertEquals("English event", uiModels.first().displayText)
    }

    @Test
    fun dropsEntitiesWithBlankText() {
        val entities = listOf(
            sampleEntity(id = 1, text = ""),
            sampleEntity(id = 2, text = "有內容的事件")
        )

        val uiModels = entities.toUiModels()

        // Never a blank row (M2 task instruction, carried over into M7's fix) — the entry with
        // no usable text is dropped, not shown as an empty string.
        assertEquals(1, uiModels.size)
        assertEquals(2L, uiModels.first().id)
        assertTrue(uiModels.none { it.displayText.isBlank() })
    }
}
