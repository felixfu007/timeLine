package com.timeline.onthisday.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-widget-instance rotation progress, keyed by the Android `appWidgetId`, so that multiple
 * instances of the widget rotate independently (F-03 #3). Schema per ANALYSIS.md 三.
 *
 * Written/read by the widget module (M3) and WidgetRotationWorker (M4) — not used yet in M1.
 */
@Entity(tableName = "widget_state")
data class WidgetStateEntity(
    @PrimaryKey
    val widgetId: Int,
    val currentIndex: Int,
    val lastRotatedAt: Long
)
