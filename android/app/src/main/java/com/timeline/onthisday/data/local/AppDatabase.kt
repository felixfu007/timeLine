package com.timeline.onthisday.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.timeline.onthisday.data.local.dao.HistoryEventDao
import com.timeline.onthisday.data.local.dao.WidgetStateDao
import com.timeline.onthisday.data.local.entity.HistoryEventEntity
import com.timeline.onthisday.data.local.entity.WidgetStateEntity

/**
 * exportSchema is off for M1 simplicity (no `schemas/` directory wired into version control
 * yet). Turn this on and commit the generated schema JSON before the first Room migration is
 * needed (see M5 — 離線快取), otherwise Room can't verify migrations across versions.
 *
 * version bumped 1 -> 2 for the M7 QA defect #1 fix: `history_event` replaced its `textZh`/
 * `textEn` columns with `language`/`text` (see HistoryEventEntity's kdoc). No real users have
 * this app installed yet (pre-M8/unreleased), so `DatabaseModule` uses
 * `fallbackToDestructiveMigration()` rather than writing a real `Migration` — any existing local
 * cache is just re-fetched from the network on next use, which is cheap and always correct.
 */
@Database(
    entities = [HistoryEventEntity::class, WidgetStateEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyEventDao(): HistoryEventDao
    abstract fun widgetStateDao(): WidgetStateDao

    companion object {
        const val DATABASE_NAME = "on_this_day.db"
    }
}
