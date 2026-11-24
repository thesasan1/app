package com.thesasan1.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thesasan1.app.core.database.dao.CategoryDao
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.dao.ProjectDao
import com.thesasan1.app.core.database.dao.SyncCursorDao
import com.thesasan1.app.core.database.dao.TagDao
import com.thesasan1.app.core.database.entity.CategoryEntity
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.ProjectEntity
import com.thesasan1.app.core.database.entity.SyncCursorEntity
import com.thesasan1.app.core.database.entity.TagEntity

/**
 * The on-device source of truth (ARCHITECTURE.md §2.5).
 *
 * Changing the schema: bump [version], add a `Migration` to [DatabaseMigrations], commit the new
 * file under `schemas/`, and add a case to `DatabaseMigrationTest`. Destructive migrations
 * need owner approval (AGENTS.md §17); there is no destructive fallback.
 */
@Database(
    entities = [
        ProjectEntity::class,
        TagEntity::class,
        CategoryEntity::class,
        OutboxEntity::class,
        SyncCursorEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun tagDao(): TagDao
    abstract fun categoryDao(): CategoryDao
    abstract fun outboxDao(): OutboxDao
    abstract fun syncCursorDao(): SyncCursorDao

    companion object {
        const val NAME = "personal_os.db"
    }
}

/** Every migration, oldest first. Empty while the schema is at version 1. */
object DatabaseMigrations {
    val ALL: Array<androidx.room.migration.Migration> = emptyArray()
}
