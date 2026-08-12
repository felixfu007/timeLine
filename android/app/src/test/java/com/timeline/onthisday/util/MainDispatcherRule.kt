package com.timeline.onthisday.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` for a [TestDispatcher] for the duration of a test, so
 * `viewModelScope` (which defaults to `Dispatchers.Main.immediate`) is JVM-testable. Uses
 * [UnconfinedTestDispatcher] (the pattern recommended for ViewModel tests, e.g. in Google's
 * "Now in Android" sample) so coroutines launched from init{} blocks run eagerly without
 * requiring manual `advanceUntilIdle()` calls just to get past dispatch.
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
