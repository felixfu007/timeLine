package com.timeline.onthisday.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timeline.onthisday.data.local.entity.WidgetStateEntity
import kotlinx.coroutines.flow.Flow

/**
 * TODO(M3/M4): not consumed yet in M1 — will back GlanceAppWidget rotation state and
 * WidgetRotationWorker (ANALYSIS.md 三).
 */
@Dao
interface WidgetStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: WidgetStateEntity)

    @Query("SELECT * FROM widget_state WHERE widgetId = :widgetId")
    suspend fun getState(widgetId: Int): WidgetStateEntity?

    @Query("SELECT * FROM widget_state")
    fun observeAll(): Flow<List<WidgetStateEntity>>

    @Query("DELETE FROM widget_state WHERE widgetId = :widgetId")
    suspend fun deleteState(widgetId: Int)

    @Delete
    suspend fun delete(state: WidgetStateEntity)
}
