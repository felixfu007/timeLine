package com.timeline.onthisday.ui

import com.timeline.onthisday.data.settings.AppLanguage
import com.timeline.onthisday.data.settings.AppSettings
import com.timeline.onthisday.data.settings.SortOrder
import com.timeline.onthisday.util.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers the Loading -> Success / Empty / "show stale cache instead of erroring" state
 * transitions requested for M2 (uses [FakeHistoryEventRepository], no real Room/network), plus
 * (M7 defect #1 fix) the "switching language re-fetches and displays that language's own
 * independently-cached list" behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun settingsRepository(language: AppLanguage = AppLanguage.ZH_HANT) =
        FakeSettingsRepository(
            initial = AppSettings(language = language, rotationIntervalMinutes = AppSettings.DEFAULT_ROTATION_INTERVAL_MINUTES)
        )

    @Test
    fun loadingIsShownWhileFirstRefreshIsInFlightAndNoCacheExists() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeHistoryEventRepository().apply {
            refreshGate = gate
            refreshResult = Result.success(Unit)
        }
        val viewModel = HistoryListViewModel(repository, settingsRepository())

        val states = mutableListOf<HistoryListUiState>()
        val collectJob = launch { viewModel.uiState.collect { states.add(it) } }
        runCurrent()

        assertEquals(HistoryListUiState.Loading, states.last())

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(HistoryListUiState.Empty(isError = false), states.last())
        collectJob.cancel()
    }

    @Test
    fun successIsShownImmediatelyFromCacheEvenBeforeRefreshCompletes() = runTest {
        val gate = CompletableDeferred<Unit>()
        val cached = sampleEntity(id = 1, year = 1969, text = "阿波羅11號登月")
        val repository = FakeHistoryEventRepository(initialEvents = listOf(cached)).apply {
            refreshGate = gate
            refreshResult = Result.success(Unit)
        }
        val viewModel = HistoryListViewModel(repository, settingsRepository())

        val states = mutableListOf<HistoryListUiState>()
        val collectJob = launch { viewModel.uiState.collect { states.add(it) } }
        runCurrent()

        val state = states.last()
        assertTrue(state is HistoryListUiState.Success)
        state as HistoryListUiState.Success
        assertEquals(1, state.events.size)
        assertEquals(false, state.isStale)

        gate.complete(Unit)
        advanceUntilIdle()
        collectJob.cancel()
    }

    @Test
    fun cachedDataIsKeptAndMarkedStaleWhenRefreshFails() = runTest {
        val cached = sampleEntity(id = 1, year = 1969, text = "舊的快取事件")
        val repository = FakeHistoryEventRepository(initialEvents = listOf(cached)).apply {
            refreshResult = Result.failure(IOException("network down"))
        }
        val viewModel = HistoryListViewModel(repository, settingsRepository())

        val states = mutableListOf<HistoryListUiState>()
        val collectJob = launch { viewModel.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        val state = states.last()
        assertTrue(state is HistoryListUiState.Success)
        state as HistoryListUiState.Success
        assertEquals("舊的快取事件", state.events.first().displayText)
        // Offline-availability standard (ANALYSIS.md 五): a failed refresh must NOT hide/clear
        // previously cached data — it's shown as-is, just flagged stale.
        assertTrue(state.isStale)
        collectJob.cancel()
    }

    @Test
    fun emptyWithErrorWhenThereIsNoCacheAndRefreshFails() = runTest {
        val repository = FakeHistoryEventRepository().apply {
            refreshResult = Result.failure(IOException("network down"))
        }
        val viewModel = HistoryListViewModel(repository, settingsRepository())

        val states = mutableListOf<HistoryListUiState>()
        val collectJob = launch { viewModel.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        assertEquals(HistoryListUiState.Empty(isError = true), states.last())
        collectJob.cancel()
    }

    @Test
    fun retryRecoversFromErrorOnceNewDataBecomesAvailable() = runTest {
        val repository = FakeHistoryEventRepository().apply {
            refreshResult = Result.failure(IOException("first attempt fails"))
        }
        val viewModel = HistoryListViewModel(repository, settingsRepository())

        val states = mutableListOf<HistoryListUiState>()
        val collectJob = launch { viewModel.uiState.collect { states.add(it) } }
        advanceUntilIdle()
        assertEquals(HistoryListUiState.Empty(isError = true), states.last())

        repository.refreshResult = Result.success(Unit)
        repository.emitEvents(listOf(sampleEntity(id = 2, year = 2001, text = "重試後取得的事件")))
        viewModel.retry()
        advanceUntilIdle()

        val finalState = states.last()
        assertTrue(finalState is HistoryListUiState.Success)
        finalState as HistoryListUiState.Success
        assertEquals(false, finalState.isStale)
        assertEquals("重試後取得的事件", finalState.events.first().displayText)
        collectJob.cancel()
    }

    @Test
    fun refreshIsScopedToTheCurrentlySelectedLanguage() = runTest {
        val repository = FakeHistoryEventRepository()
        val viewModel = HistoryListViewModel(repository, settingsRepository(language = AppLanguage.EN))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(listOf(AppLanguage.EN), repository.refreshedLanguages)
        collectJob.cancel()
    }

    @Test
    fun eachLanguageShowsItsOwnIndependentlyCachedEventList() = runTest {
        // M7 defect #1 fix: zh and en are separate curated feeds, not translations of each
        // other — a zh-only cache must not "fall back" to showing anything for en.
        val zhOnly = sampleEntity(id = 1, year = 1969, language = AppLanguage.ZH_HANT, text = "中文專屬事件")
        val repository = FakeHistoryEventRepository(initialEvents = listOf(zhOnly)).apply {
            refreshResult = Result.success(Unit)
        }
        val viewModel = HistoryListViewModel(repository, settingsRepository(language = AppLanguage.EN))

        val states = mutableListOf<HistoryListUiState>()
        val collectJob = launch { viewModel.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        // No en-language rows cached and the (fake, always-succeeding) refresh didn't add any,
        // so the en-selected list screen must show Empty — never the zh-only cached row.
        assertEquals(HistoryListUiState.Empty(isError = false), states.last())
        collectJob.cancel()
    }

    @Test
    fun defaultSortOrderIsYearAscending_matchingThePreExistingDbQueryOrder() = runTest {
        val repository = FakeHistoryEventRepository(
            initialEvents = listOf(
                sampleEntity(id = 1, year = 1969, text = "1969 event"),
                sampleEntity(id = 2, year = 1356, text = "1356 event")
            )
        ).apply { refreshResult = Result.success(Unit) }
        val settings = settingsRepository()
        val viewModel = HistoryListViewModel(repository, settings)

        assertEquals(SortOrder.YEAR_ASCENDING, viewModel.sortOrder.value)

        val states = mutableListOf<HistoryListUiState>()
        val collectJob = launch { viewModel.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        val state = states.last() as HistoryListUiState.Success
        assertEquals(listOf(1356, 1969), state.events.map { it.year })
        collectJob.cancel()
    }

    @Test
    fun settingSortOrderToDescending_reordersTheAlreadyCachedEventsWithoutANewRefresh() = runTest {
        val repository = FakeHistoryEventRepository(
            initialEvents = listOf(
                sampleEntity(id = 1, year = 1969, text = "1969 event"),
                sampleEntity(id = 2, year = -587, text = "BCE event"),
                sampleEntity(id = 3, year = 1356, text = "1356 event")
            )
        ).apply { refreshResult = Result.success(Unit) }
        val viewModel = HistoryListViewModel(repository, settingsRepository())

        val states = mutableListOf<HistoryListUiState>()
        val collectJob = launch { viewModel.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        viewModel.setSortOrder(SortOrder.YEAR_DESCENDING)
        advanceUntilIdle()

        assertEquals(SortOrder.YEAR_DESCENDING, viewModel.sortOrder.value)
        val state = states.last() as HistoryListUiState.Success
        // BCE (-587) must sort last in descending order, not first as a naive string sort would.
        assertEquals(listOf(1969, 1356, -587), state.events.map { it.year })
        collectJob.cancel()
    }
}
