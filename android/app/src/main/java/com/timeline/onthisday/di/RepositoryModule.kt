package com.timeline.onthisday.di

import com.timeline.onthisday.data.repository.HistoryEventRepository
import com.timeline.onthisday.data.repository.HistoryEventRepositoryImpl
import com.timeline.onthisday.locale.AppCompatLocaleApplier
import com.timeline.onthisday.locale.AppLocaleApplier
import com.timeline.onthisday.scheduler.WidgetRotationScheduler
import com.timeline.onthisday.scheduler.WorkScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHistoryEventRepository(
        impl: HistoryEventRepositoryImpl
    ): HistoryEventRepository

    /** M6 (F-05 AC2) — see [WidgetRotationScheduler]'s kdoc for why this interface exists. */
    @Binds
    @Singleton
    abstract fun bindWidgetRotationScheduler(
        impl: WorkScheduler
    ): WidgetRotationScheduler

    /** M6 (F-07 AC1/AC2) — see [AppLocaleApplier]'s kdoc for why this interface exists. */
    @Binds
    @Singleton
    abstract fun bindAppLocaleApplier(
        impl: AppCompatLocaleApplier
    ): AppLocaleApplier
}
