package com.timeline.onthisday.ui

/**
 * UI state for the "今天歷史事件" list screen (F-01 #4: never show a blank/crashed screen —
 * always resolve to one of these three explicit states).
 */
sealed interface HistoryListUiState {

    /** No cached data yet and a first refresh is still in flight. */
    data object Loading : HistoryListUiState

    /**
     * At least one event is available to show — either freshly fetched or (per the offline
     * availability standard, ANALYSIS.md 五) a previously cached copy kept around because the
     * latest refresh attempt failed.
     *
     * @param isStale true when this data is being shown despite the most recent refresh
     * attempt having failed (i.e. we're intentionally NOT surfacing the error to the user,
     * showing old-but-valid data instead).
     */
    data class Success(val events: List<HistoryEventUiModel>, val isStale: Boolean) : HistoryListUiState

    /**
     * No cached data exists at all for today.
     *
     * @param isError true when this is because the refresh attempt actually failed (network/API
     * error with zero prior cache to fall back on); false when the refresh succeeded but the
     * API genuinely returned no events for today (rare, but not a bug).
     */
    data class Empty(val isError: Boolean) : HistoryListUiState
}
