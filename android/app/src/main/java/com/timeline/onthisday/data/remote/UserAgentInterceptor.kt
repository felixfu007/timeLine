package com.timeline.onthisday.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Wikimedia's API usage policy requires a descriptive User-Agent identifying the
 * application and a contact method, otherwise requests may be rate-limited/blocked
 * (see ANALYSIS.md 六、2 "Wikimedia API 使用規範").
 *
 * TODO: replace the contact placeholder with a real repo URL / contact email before
 * shipping a release build.
 */
class UserAgentInterceptor(
    private val appVersion: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestWithUserAgent = chain.request().newBuilder()
            .header(
                "User-Agent",
                "OnThisDayWidget/$appVersion (https://github.com/felixfu007/timeLine; " +
                    "contact: felixfu007@gmail.com) OkHttp"
            )
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}
