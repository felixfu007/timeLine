package com.timeline.onthisday.scheduler

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Exercises [advanceWidgetRotations] (the pure-DAO-facing logic extracted out of
 * [WidgetRotationWorker]) against a [FakeWidgetStateDao] — covers F-03 #3 (multiple widget
 * instances rotate independently) and the "loop back to the first event after the last one"
 * behaviour, without needing Robolectric/instrumented Worker tests.
 */
class WidgetRotationWorkerTest {

    @Test
    fun firstTick_setsEveryWidgetToIndexZero() = runTest {
        val dao = FakeWidgetStateDao()

        advanceWidgetRotations(widgetIds = listOf(1, 2, 3), eventCount = 5, widgetStateDao = dao)

        assertEquals(0, dao.getState(1)?.currentIndex)
        assertEquals(0, dao.getState(2)?.currentIndex)
        assertEquals(0, dao.getState(3)?.currentIndex)
    }

    @Test
    fun eachWidgetInstanceAdvancesIndependently() = runTest {
        val dao = FakeWidgetStateDao()
        // widget 1 has already rotated a few times; widget 2 is brand new.
        dao.upsert(com.timeline.onthisday.data.local.entity.WidgetStateEntity(widgetId = 1, currentIndex = 2, lastRotatedAt = 0L))

        advanceWidgetRotations(widgetIds = listOf(1, 2), eventCount = 5, widgetStateDao = dao)

        assertEquals(3, dao.getState(1)?.currentIndex) // advanced from 2 -> 3
        assertEquals(0, dao.getState(2)?.currentIndex) // untouched by widget 1's progress
    }

    @Test
    fun rotationLoopsBackToFirstEventAfterLastOnceFullCircle() = runTest {
        val dao = FakeWidgetStateDao()

        // Tick through all 3 events for a single widget: 0 -> 1 -> 2 -> back to 0.
        advanceWidgetRotations(widgetIds = listOf(42), eventCount = 3, widgetStateDao = dao)
        assertEquals(0, dao.getState(42)?.currentIndex)

        advanceWidgetRotations(widgetIds = listOf(42), eventCount = 3, widgetStateDao = dao)
        assertEquals(1, dao.getState(42)?.currentIndex)

        advanceWidgetRotations(widgetIds = listOf(42), eventCount = 3, widgetStateDao = dao)
        assertEquals(2, dao.getState(42)?.currentIndex)

        advanceWidgetRotations(widgetIds = listOf(42), eventCount = 3, widgetStateDao = dao)
        assertEquals(0, dao.getState(42)?.currentIndex)
    }

    @Test
    fun noEvents_stateStaysAtZeroWithoutCrashing() = runTest {
        val dao = FakeWidgetStateDao()

        advanceWidgetRotations(widgetIds = listOf(7), eventCount = 0, widgetStateDao = dao)

        assertEquals(0, dao.getState(7)?.currentIndex)
    }
}
