package com.timeline.onthisday.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One "歷史上的今天" event, per REQUIREMENTS.md 第七章 response example:
 * { "year": 1969, "text": "...", "pages": [ { "title": "...", "extract": "..." } ] }
 */
@JsonClass(generateAdapter = true)
data class EventDto(
    @Json(name = "year") val year: Int? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "pages") val pages: List<PageDto>? = null
)
