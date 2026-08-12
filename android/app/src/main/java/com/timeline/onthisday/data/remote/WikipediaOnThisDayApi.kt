package com.timeline.onthisday.data.remote

import com.timeline.onthisday.data.remote.dto.OnThisDayResponseDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * Wikimedia REST API — "On This Day" feed.
 *
 * https://zh.wikipedia.org/api/rest_v1/feed/onthisday/events/{MM}/{DD}
 * https://en.wikipedia.org/api/rest_v1/feed/onthisday/events/{MM}/{DD}
 *
 * The same interface is used for both the zh and en endpoints (ANALYSIS.md 三、
 * "zh 與 en 兩個 baseUrl") — see di/NetworkModule.kt, which binds two Retrofit instances
 * (qualified with @ZhWikipediaApi / @EnWikipediaApi) to this interface with different
 * base URLs, in support of F-07's language switch + fallback requirement.
 */
interface WikipediaOnThisDayApi {

    /**
     * @param month zero-padded month, "01".."12"
     * @param day zero-padded day, "01".."31"
     * @param acceptLanguage M7 defect #3 fix: zh.wikipedia.org's LanguageConverter defaults to
     * Simplified Chinese content when no variant is requested, which doesn't match this App's
     * Traditional Chinese UI. Passing `Accept-Language: zh-Hant` here (confirmed empirically
     * against the live API — see the M7 developer report) makes the server return the
     * Traditional Chinese variant instead. HistoryEventRepositoryImpl passes `"zh-Hant"` for the
     * zh feed and `"en"` for the en feed (a no-op there, but harmless/correct to send).
     */
    @GET("feed/onthisday/events/{month}/{day}")
    suspend fun getEventsOnThisDay(
        @Path("month") month: String,
        @Path("day") day: String,
        @Header("Accept-Language") acceptLanguage: String
    ): OnThisDayResponseDto
}
