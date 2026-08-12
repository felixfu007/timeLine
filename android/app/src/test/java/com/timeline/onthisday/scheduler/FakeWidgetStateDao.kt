package com.timeline.onthisday.scheduler

import com.timeline.onthisday.data.local.dao.WidgetStateDao
import com.timeline.onthisday.data.local.entity.WidgetStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory fake, keyed by widgetId exactly like the real Room table's primary key. */
class FakeWidgetStateDao : WidgetStateDao {

    private val statesFlow = MutableStateFlow<Map<Int, WidgetStateEntity>>(emptyMap())

    override suspend fun upsert(state: WidgetStateEntity) {
        statesFlow.value = statesFlow.value + (state.widgetId to state)
    }

    override suspend fun getState(widgetId: Int): WidgetStateEntity? = statesFlow.value[widgetId]

    override fun observeAll(): Flow<List<WidgetStateEntity>> =
        MutableStateFlow(statesFlow.value.values.toList())

    override suspend fun deleteState(widgetId: Int) {
        statesFlow.value = statesFlow.value - widgetId
    }

    override suspend fun delete(state: WidgetStateEntity) {
        deleteState(state.widgetId)
    }
}
