package com.timeline.onthisday.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response envelope for `GET /feed/onthisday/events/{MM}/{DD}`.
 *
 * The real Wikimedia REST API response also contains "births", "deaths", "holidays" and
 * "selected" arrays alongside "events", but F-01~F-08 only require the "events" collection,
 * so the other keys are intentionally not modeled here (Moshi ignores unknown JSON keys by
 * default — no crash if the API adds/removes fields we don't map).
 */
@JsonClass(generateAdapter = true)
data class OnThisDayResponseDto(
    @Json(name = "events") val events: List<EventDto>? = null
)
