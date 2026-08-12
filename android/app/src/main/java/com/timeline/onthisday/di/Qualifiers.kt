package com.timeline.onthisday.di

import javax.inject.Qualifier

/** Retrofit/WikipediaOnThisDayApi instance pointed at zh.wikipedia.org. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ZhWikipediaApi

/** Retrofit/WikipediaOnThisDayApi instance pointed at en.wikipedia.org (F-07 language support). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EnWikipediaApi

/** CoroutineDispatcher for I/O-bound work (network calls, DB access off the Room executor). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
