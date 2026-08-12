package com.timeline.onthisday.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timeline.onthisday.data.repository.HistoryEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * M2 — minimal event detail screen. Looks the tapped event up by its Room row id.
 *
 * M7 defect #1/#2 fix context: switched from "re-derive from today's date+language event list,
 * then filter by id" to [HistoryEventRepository.observeEventById] — a direct by-id lookup. This
 * avoids a would-be false "not found" if the id was reached via a Widget deep link (F-06) whose
 * displayed language could momentarily differ from the app's current Settings language (each
 * `eventId` uniquely identifies one row regardless of which language's feed it came from), and
 * is simpler now that there's no zh/en merge to re-apply per row.
 *
 * Deliberately does NOT call `repository.refreshIfNeeded()` again — HistoryListViewModel/the
 * Widget already ensure freshness before the user can reach this screen.
 */
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    repository: HistoryEventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: Long = savedStateHandle.get<Long>(AppDestinations.EVENT_ID_ARG) ?: NO_EVENT_ID

    val uiState: StateFlow<HistoryEventUiModel?> = repository
        .observeEventById(eventId)
        .map { entity -> entity?.let { listOf(it).toUiModels().firstOrNull() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )

    private companion object {
        const val NO_EVENT_ID = -1L
    }
}
