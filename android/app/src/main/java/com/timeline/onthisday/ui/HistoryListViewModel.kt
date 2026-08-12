package com.timeline.onthisday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timeline.onthisday.data.repository.HistoryEventRepository
import com.timeline.onthisday.data.settings.AppLanguage
import com.timeline.onthisday.data.settings.SettingsRepository
import com.timeline.onthisday.data.settings.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * M2 — 主 APP 歷史事件列表頁: exposes today's events as a [HistoryListUiState].
 *
 * Data flow (ANALYSIS.md 三/七): observes whatever Room currently has cached for today
 * (so cached data is shown immediately, offline-first) while independently kicking off
 * `repository.refreshIfNeeded()` once; a failed refresh never clears/overrides the cache
 * (already guaranteed by the repository — see HistoryEventRepositoryImpl), it only flips
 * [HistoryListUiState.Success.isStale] / [HistoryListUiState.Empty.isError] so the UI can
 * (optionally) hint that data may be out of date, without blocking on it.
 *
 * M7 defect #1 fix: now reactively follows [SettingsRepository]'s current [AppLanguage] (via
 * `flatMapLatest`) rather than always reading the zh feed. Switching language shows that
 * language's own independently-cached event list (not a translation of the same events) — if
 * nothing is cached yet for the newly-selected language, [uiState] naturally falls back through
 * the same Loading -> Success/Empty state machine as a cold start, since
 * `repository.observeEventsForDate(..., language)` is empty until [refresh] populates it.
 *
 * 2026-07-30 user request ("清單列表加入多種排序功能"): also reactively follows
 * [SettingsRepository]'s persisted [SortOrder] and re-sorts the already-cached event list
 * in-memory (see [sortedByOrder]) whenever it changes — this is purely a display-order change, it
 * never touches the Room query (still `ORDER BY year ASC`, see HistoryEventDao) or triggers a new
 * network refresh, so switching sort order is instant even offline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryListViewModel @Inject constructor(
    private val repository: HistoryEventRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val month = today.monthValue
    private val day = today.dayOfMonth

    private val language: StateFlow<AppLanguage> = settingsRepository.settingsFlow
        .map { it.language }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppLanguage.fromSystemDefault()
        )

    /** Current list-screen sort order (2026-07-30), exposed so [HistoryListScreen]'s sort menu can show a checkmark against the active option. */
    val sortOrder: StateFlow<SortOrder> = settingsRepository.settingsFlow
        .map { it.sortOrder }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SortOrder.DEFAULT
        )

    /** True until the current language's first refreshIfNeeded() call (success or failure) has completed. */
    private val isRefreshing = MutableStateFlow(true)
    private val lastRefreshFailed = MutableStateFlow(false)

    val uiState: StateFlow<HistoryListUiState> = language
        .flatMapLatest { lang ->
            combine(
                repository.observeEventsForDate(month, day, lang),
                isRefreshing,
                lastRefreshFailed,
                sortOrder
            ) { cachedEntities, refreshing, refreshFailed, order ->
                val events = cachedEntities.toUiModels().sortedByOrder(order)
                when {
                    events.isNotEmpty() -> HistoryListUiState.Success(events = events, isStale = refreshFailed)
                    refreshing -> HistoryListUiState.Loading
                    else -> HistoryListUiState.Empty(isError = refreshFailed)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HistoryListUiState.Loading
        )

    init {
        // Re-triggers a refresh whenever the selected language changes (including the very
        // first emission) — each language is fetched/cached independently, so a language switch
        // needs its own refreshIfNeeded() call, not just a re-read of the same cached rows.
        viewModelScope.launch {
            language.collectLatest { lang -> refresh(lang, forceRefresh = false) }
        }
    }

    /** Re-attempts the network refresh for the currently-selected language, e.g. from a "重試" button. */
    fun retry() {
        viewModelScope.launch { refresh(language.value, forceRefresh = true) }
    }

    /** Persists the newly-selected sort order (2026-07-30); [uiState] re-sorts automatically via the [combine] above once the setting round-trips through [SettingsRepository.settingsFlow]. */
    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settingsRepository.setSortOrder(order) }
    }

    private suspend fun refresh(language: AppLanguage, forceRefresh: Boolean) {
        isRefreshing.value = true
        val result = repository.refreshIfNeeded(month, day, language, forceRefresh = forceRefresh)
        lastRefreshFailed.value = result.isFailure
        isRefreshing.value = false
    }
}
