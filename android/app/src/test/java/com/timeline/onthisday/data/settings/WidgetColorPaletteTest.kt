package com.timeline.onthisday.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers the fallback-to-default lookup behavior used when reading a persisted DataStore value. */
class WidgetColorPaletteTest {

    @Test
    fun widgetBackgroundColor_fromName_resolvesKnownName() {
        assertEquals(WidgetBackgroundColor.PINK, WidgetBackgroundColor.fromName("PINK"))
    }

    @Test
    fun widgetBackgroundColor_fromName_fallsBackToDefault_forNullOrUnknownName() {
        assertEquals(WidgetBackgroundColor.DEFAULT, WidgetBackgroundColor.fromName(null))
        assertEquals(WidgetBackgroundColor.DEFAULT, WidgetBackgroundColor.fromName("NOT_A_REAL_COLOR"))
    }

    @Test
    fun widgetTextColor_fromName_resolvesKnownName_andFallsBackToDefault() {
        assertEquals(WidgetTextColor.DARK_BLUE, WidgetTextColor.fromName("DARK_BLUE"))
        assertEquals(WidgetTextColor.DEFAULT, WidgetTextColor.fromName(null))
        assertEquals(WidgetTextColor.DEFAULT, WidgetTextColor.fromName(""))
    }

    @Test
    fun widgetBorderColor_fromName_resolvesKnownName_andFallsBackToDefault() {
        assertEquals(WidgetBorderColor.SOFT_GOLD, WidgetBorderColor.fromName("SOFT_GOLD"))
        assertEquals(WidgetBorderColor.DEFAULT, WidgetBorderColor.fromName(null))
        assertEquals(WidgetBorderColor.DEFAULT, WidgetBorderColor.fromName("NOT_A_REAL_COLOR"))
    }
}
