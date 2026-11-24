package com.thesasan1.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.thesasan1.app.core.database.AppDatabase
import com.thesasan1.app.core.database.DatabaseMigrations
import com.thesasan1.app.core.database.RoomTransactionRunner
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.CategoryDao
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.dao.ProjectDao
import com.thesasan1.app.core.database.dao.SyncCursorDao
import com.thesasan1.app.core.database.dao.TagDao
import dagger.Binds
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .apply { DatabaseMigrations.ALL.forEach { addMigrations(it) } }
            .build()

    @Provides fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()

    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides fun provideOutboxDao(db: AppDatabase): OutboxDao = db.outboxDao()

    @Provides fun provideSyncCursorDao(db: AppDatabase): SyncCursorDao = db.syncCursorDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface DatabaseBindings {
    @Binds fun bindTransactionRunner(runner: RoomTransactionRunner): TransactionRunner
}
