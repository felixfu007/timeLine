package com.timeline.onthisday.di

import android.content.Context
import androidx.room.Room
import com.timeline.onthisday.data.local.AppDatabase
import com.timeline.onthisday.data.local.dao.HistoryEventDao
import com.timeline.onthisday.data.local.dao.WidgetStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // M7 defect #1 fix bumped the schema version (see AppDatabase's kdoc) with no real
            // Migration written — acceptable pre-M8/unreleased, existing local cache rows are
            // just dropped and re-fetched from the network next time they're needed.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHistoryEventDao(db: AppDatabase): HistoryEventDao = db.historyEventDao()

    @Provides
    fun provideWidgetStateDao(db: AppDatabase): WidgetStateDao = db.widgetStateDao()
}
