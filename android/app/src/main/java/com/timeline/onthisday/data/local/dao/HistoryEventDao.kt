package com.timeline.onthisday.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.timeline.onthisday.data.local.entity.HistoryEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<HistoryEventEntity>)

    /**
     * [language] is [com.timeline.onthisday.data.settings.AppLanguage.name] — scoping deletes to
     * one language (M7 defect #1 fix) means refreshing the zh feed never clobbers a
     * separately-cached en feed for the same date, and vice versa.
     */
    @Query("DELETE FROM history_event WHERE month = :month AND day = :day AND language = :language")
    suspend fun deleteForDate(month: Int, day: Int, language: String)

    /**
     * Replaces all cached events for a given (month, day, language) in a single transaction, so
     * observers never briefly see an empty list mid-refresh.
     */
    @Transaction
    suspend fun replaceEventsForDate(month: Int, day: Int, language: String, events: List<HistoryEventEntity>) {
        deleteForDate(month, day, language)
        insertAll(events)
    }

    @Query("SELECT * FROM history_event WHERE month = :month AND day = :day AND language = :language ORDER BY year ASC")
    fun observeEventsForDate(month: Int, day: Int, language: String): Flow<List<HistoryEventEntity>>

    @Query("SELECT * FROM history_event WHERE month = :month AND day = :day AND language = :language ORDER BY year ASC")
    suspend fun getEventsForDateOnce(month: Int, day: Int, language: String): List<HistoryEventEntity>

    /**
     * Looks up a single event by its Room-assigned [id], independent of date/language filtering.
     * Used by the detail screen (M7 defect #2 fix context): an `eventId` — whether reached by
     * tapping a row in the list or via a Widget deep link — always uniquely identifies one row
     * regardless of which language's feed it came from or what the app's *current* language
     * setting is, so looking it up directly avoids a would-be "not found" false negative if the
     * two ever momentarily disagree.
     */
    @Query("SELECT * FROM history_event WHERE id = :id")
    fun observeEventById(id: Long): Flow<HistoryEventEntity?>

    /** Used by the repository to decide whether the cache for a (date, language) is fresh enough to skip a network call. */
    @Query("SELECT MIN(cachedAt) FROM history_event WHERE month = :month AND day = :day AND language = :language")
    suspend fun getOldestCacheTimestamp(month: Int, day: Int, language: String): Long?

    /**
     * Rolling 35-day cache window cleanup (non-functional "離線可用" requirement, ANALYSIS.md 五).
     * Deliberately not language-scoped — the retention window applies uniformly across every
     * cached language's rows.
     */
    @Query("DELETE FROM history_event WHERE cachedAt < :cutoffEpochMillis")
    suspend fun deleteCachedBefore(cutoffEpochMillis: Long)

    @Delete
    suspend fun delete(event: HistoryEventEntity)
}
