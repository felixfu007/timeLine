package com.timeline.onthisday.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRotationStateTest {

    @Test
    fun nextIndex_firstTickWithNoStoredIndex_startsAtZero() {
        assertEquals(0, WidgetRotationState.nextIndex(currentIndex = null, eventCount = 5))
    }

    @Test
    fun nextIndex_advancesByOne() {
        assertEquals(3, WidgetRotationState.nextIndex(currentIndex = 2, eventCount = 5))
    }

    @Test
    fun nextIndex_loopsBackToZeroAfterLastEvent() {
        // 5 events -> valid indices 0..4; index 4 is the last one shown, next tick should wrap.
        assertEquals(0, WidgetRotationState.nextIndex(currentIndex = 4, eventCount = 5))
    }

    @Test
    fun nextIndex_withNoEvents_returnsZeroRatherThanDividingByZero() {
        assertEquals(0, WidgetRotationState.nextIndex(currentIndex = 3, eventCount = 0))
    }

    @Test
    fun currentIndexOrFallback_defaultsToZeroWhenNeverStored() {
        assertEquals(0, WidgetRotationState.currentIndexOrFallback(storedIndex = null, eventCount = 5))
    }

    @Test
    fun currentIndexOrFallback_clampsStaleIndexIntoRange() {
        // Yesterday's cache had 10 events (stored index 8); today only has 3.
        assertEquals(2, WidgetRotationState.currentIndexOrFallback(storedIndex = 8, eventCount = 3))
    }

    @Test
    fun currentIndexOrFallback_withNoEvents_returnsZero() {
        assertEquals(0, WidgetRotationState.currentIndexOrFallback(storedIndex = 2, eventCount = 0))
    }
}
