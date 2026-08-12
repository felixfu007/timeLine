package com.timeline.onthisday.data.settings

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** F-07 AC1: default language derivation and persisted-tag round-tripping. */
class AppLanguageTest {

    @Test
    fun fromSystemDefault_returnsZhHant_whenSystemLanguageIsChinese() {
        assertEquals(AppLanguage.ZH_HANT, AppLanguage.fromSystemDefault(Locale.TRADITIONAL_CHINESE))
        assertEquals(AppLanguage.ZH_HANT, AppLanguage.fromSystemDefault(Locale.SIMPLIFIED_CHINESE))
        assertEquals(AppLanguage.ZH_HANT, AppLanguage.fromSystemDefault(Locale.forLanguageTag("zh")))
    }

    @Test
    fun fromSystemDefault_returnsEn_whenSystemLanguageIsNotChinese() {
        assertEquals(AppLanguage.EN, AppLanguage.fromSystemDefault(Locale.US))
        assertEquals(AppLanguage.EN, AppLanguage.fromSystemDefault(Locale.JAPAN))
    }

    @Test
    fun fromTag_resolvesKnownTagsExactly() {
        assertEquals(AppLanguage.ZH_HANT, AppLanguage.fromTag("zh-Hant"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("en"))
    }

    @Test
    fun fromTag_fallsBackToSystemDefault_whenTagIsNullOrUnrecognized() {
        val originalDefault = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals(AppLanguage.EN, AppLanguage.fromTag(null))
            assertEquals(AppLanguage.EN, AppLanguage.fromTag("fr"))

            Locale.setDefault(Locale.TRADITIONAL_CHINESE)
            assertEquals(AppLanguage.ZH_HANT, AppLanguage.fromTag(null))
        } finally {
            Locale.setDefault(originalDefault)
        }
    }

    @Test
    fun tag_roundTripsThroughJavaLocale() {
        assertEquals("zh", AppLanguage.ZH_HANT.toJavaLocale().language)
        assertEquals("en", AppLanguage.EN.toJavaLocale().language)
    }
}
