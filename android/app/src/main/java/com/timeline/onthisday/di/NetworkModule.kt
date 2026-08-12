package com.timeline.onthisday.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.timeline.onthisday.BuildConfig
import com.timeline.onthisday.data.remote.UserAgentInterceptor
import com.timeline.onthisday.data.remote.WikipediaOnThisDayApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val ZH_BASE_URL = "https://zh.wikipedia.org/api/rest_v1/"
private const val EN_BASE_URL = "https://en.wikipedia.org/api/rest_v1/"

/**
 * Provides the Retrofit/OkHttp/Moshi stack for the Wikipedia On This Day API, including two
 * qualified [WikipediaOnThisDayApi] instances (zh/en base URLs) per ANALYSIS.md 三.
 *
 * Non-functional requirements applied here (ANALYSIS.md 五 "低電耗"):
 *  - 10s connect/read/write timeouts
 *  - OkHttp's built-in retryOnConnectionFailure; a fuller retry/backoff policy (3 attempts,
 *    exponential backoff) is TODO for M4, to be applied at the WorkManager/repository layer
 *    rather than the HTTP layer, since Worker retries are what actually need backoff.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(UserAgentInterceptor(BuildConfig.VERSION_NAME))

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @ZhWikipediaApi
    fun provideZhRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(ZH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    @EnWikipediaApi
    fun provideEnRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(EN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    @ZhWikipediaApi
    fun provideZhApi(@ZhWikipediaApi retrofit: Retrofit): WikipediaOnThisDayApi =
        retrofit.create(WikipediaOnThisDayApi::class.java)

    @Provides
    @Singleton
    @EnWikipediaApi
    fun provideEnApi(@EnWikipediaApi retrofit: Retrofit): WikipediaOnThisDayApi =
        retrofit.create(WikipediaOnThisDayApi::class.java)
}
