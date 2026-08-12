package com.timeline.onthisday.ui

import com.timeline.onthisday.data.local.entity.HistoryEventEntity
import com.timeline.onthisday.data.repository.HistoryEventRepository
import com.timeline.onthisday.data.settings.AppLanguage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Controllable in-memory fake used by ViewModel unit tests, so we can drive Loading /
 * Success(fresh) / Success(stale, refresh failed) / Empty(with or without error) scenarios
 * deterministically without a real Room DB or network call.
 *
 * M7 defect #1 fix: [observeEventsForDate]/[refreshIfNeeded] now take an explicit [AppLanguage]
 * and filter/record against it, mirroring the real repository's "each language is an
 * independently cached list" behavior — see [emitEvents]'s kdoc for how tests seed per-language
 * data.
 */
class FakeHistoryEventRepository(
    initialEvents: List<HistoryEventEntity> = emptyList()
) : HistoryEventRepository {

    private val eventsFlow = MutableStateFlow(initialEvents)

    /** What refreshIfNeeded() should return once (optionally) [refreshGate] is completed. */
    var refreshResult: Result<Unit> = Result.success(Unit)

    /** When set, refreshIfNeeded() suspends until this is completed — used to observe the Loading state mid-refresh. */
    var refreshGate: CompletableDeferred<Unit>? = null

    var pruneCalled: Boolean = false
        private set

    /** Records every language refreshIfNeeded() was called with, in call order. */
    val refreshedLanguages = mutableListOf<AppLanguage>()

    override fun observeEventsForDate(month: Int, day: Int, language: AppLanguage): Flow<List<HistoryEventEntity>> =
        eventsFlow.map { events -> events.filter { it.language == language.name } }

    override fun observeEventById(id: Long): Flow<HistoryEventEntity?> =
        eventsFlow.map { events -> events.firstOrNull { it.id == id } }

    override suspend fun refreshIfNeeded(
        month: Int,
        day: Int,
        language: AppLanguage,
        forceRefresh: Boolean
    ): Result<Unit> {
        refreshedLanguages += language
        refreshGate?.await()
        return refreshResult
    }

    override suspend fun pruneStaleCache() {
        pruneCalled = true
    }

    /** Replaces the fake's whole in-memory event set — tests scope entries to a language via [sampleEntity]'s `language` param. */
    fun emitEvents(events: List<HistoryEventEntity>) {
        eventsFlow.value = events
    }
}
