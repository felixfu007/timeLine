package com.timeline.onthisday.data.remote.mapper

import com.timeline.onthisday.data.remote.dto.ContentUrlsDto
import com.timeline.onthisday.data.remote.dto.EventDto
import com.timeline.onthisday.data.remote.dto.PageDto
import com.timeline.onthisday.data.remote.dto.PageUrlDto
import com.timeline.onthisday.data.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM unit test for the DTO -> Entity mapping (no Android framework / emulator needed).
 *
 * M7 defect #1 fix: `toEntity()` now takes the shared [AppLanguage] type (rather than a
 * dedicated `EventLanguage` enum) and writes a single [entity.text]/[entity.language] pair
 * instead of filling one of two `textZh`/`textEn` columns — see EventMapper.kt's kdoc and
 * HistoryEventEntity's kdoc for why the old "same event in two languages" model was replaced.
 */
class EventMapperTest {

    @Test
    fun zhEventMapsTextAndTagsItWithZhHantLanguage() {
        val dto = EventDto(year = 1969, text = "阿波羅11號太空人阿姆斯壯成為首位登陸月球的人類", pages = null)

        val entity = dto.toEntity(month = 7, day = 20, language = AppLanguage.ZH_HANT, cachedAt = 1_000L)

        assertEquals(7, entity.month)
        assertEquals(20, entity.day)
        assertEquals(1969, entity.year)
        assertEquals(AppLanguage.ZH_HANT.name, entity.language)
        assertEquals("阿波羅11號太空人阿姆斯壯成為首位登陸月球的人類", entity.text)
        assertEquals(1_000L, entity.cachedAt)
        assertEquals(1_000L, entity.sourceUpdatedAt)
        assertNull(entity.categoryTag)
    }

    @Test
    fun enEventMapsTextAndTagsItWithEnLanguage() {
        val dto = EventDto(year = 1969, text = "Apollo 11 astronaut Neil Armstrong becomes the first human to walk on the Moon.")

        val entity = dto.toEntity(month = 7, day = 20, language = AppLanguage.EN, cachedAt = 2_000L)

        assertEquals(AppLanguage.EN.name, entity.language)
        assertEquals("Apollo 11 astronaut Neil Armstrong becomes the first human to walk on the Moon.", entity.text)
    }

    @Test
    fun missingYearDefaultsToZeroRatherThanThrowing() {
        val dto = EventDto(year = null, text = "Some event with no year field")

        val entity = dto.toEntity(month = 2, day = 29, language = AppLanguage.ZH_HANT, cachedAt = 3_000L)

        assertEquals(0, entity.year)
    }

    @Test
    fun missingTextDefaultsToEmptyStringRatherThanThrowing() {
        // Callers (HistoryEventRepositoryImpl) are expected to filter out null-text entries
        // before calling toEntity(), but this is a defensive last-resort, not the primary path.
        val dto = EventDto(year = 1969, text = null)

        val entity = dto.toEntity(month = 7, day = 20, language = AppLanguage.ZH_HANT, cachedAt = 3_500L)

        assertEquals("", entity.text)
    }

    @Test
    fun eventWithPagesMapsSourceTitleAndPrefersDesktopUrl() {
        val dto = EventDto(
            year = 1969,
            text = "阿波羅11號太空人阿姆斯壯成為首位登陸月球的人類",
            pages = listOf(
                PageDto(
                    title = "阿波羅11號",
                    extract = "阿波羅11號是美國太空總署阿波羅計劃的第五次載人任務……",
                    contentUrls = ContentUrlsDto(
                        desktop = PageUrlDto(page = "https://zh.wikipedia.org/wiki/阿波羅11號"),
                        mobile = PageUrlDto(page = "https://zh.m.wikipedia.org/wiki/阿波羅11號")
                    )
                )
            )
        )

        val entity = dto.toEntity(month = 7, day = 20, language = AppLanguage.ZH_HANT, cachedAt = 4_000L)

        assertEquals("阿波羅11號", entity.sourceTitle)
        assertEquals("https://zh.wikipedia.org/wiki/阿波羅11號", entity.sourceUrl)
    }

    @Test
    fun eventWithoutPagesLeavesSourceFieldsNull() {
        val dto = EventDto(year = 1969, text = "Some event", pages = null)

        val entity = dto.toEntity(month = 7, day = 20, language = AppLanguage.ZH_HANT, cachedAt = 5_000L)

        assertNull(entity.sourceTitle)
        assertNull(entity.sourceUrl)
    }
}
