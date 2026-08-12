package com.timeline.onthisday.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A Wikipedia page associated with a historical event ("pages" array in the API response).
 * Only the fields actually needed for F-06 (event detail screen: title + summary + source
 * link) are modeled; the raw API response has more fields (thumbnail, namespace, etc.) that
 * are intentionally ignored here — Moshi will silently skip unknown/unused JSON keys.
 */
@JsonClass(generateAdapter = true)
data class PageDto(
    @Json(name = "title") val title: String? = null,
    @Json(name = "extract") val extract: String? = null,
    @Json(name = "content_urls") val contentUrls: ContentUrlsDto? = null
)

@JsonClass(generateAdapter = true)
data class ContentUrlsDto(
    @Json(name = "desktop") val desktop: PageUrlDto? = null,
    @Json(name = "mobile") val mobile: PageUrlDto? = null
)

@JsonClass(generateAdapter = true)
data class PageUrlDto(
    @Json(name = "page") val page: String? = null
)
