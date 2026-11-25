package com.thesasan1.app.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.thesasan1.app.core.database.dao.CategoryDao
import com.thesasan1.app.core.database.dao.NoteDao
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.dao.ProjectDao
import com.thesasan1.app.core.database.dao.SyncCursorDao
import com.thesasan1.app.core.database.dao.TagDao
import com.thesasan1.app.core.database.dao.TaskDao
import com.thesasan1.app.core.database.entity.CategoryEntity
import com.thesasan1.app.core.database.entity.NoteEntity
import com.thesasan1.app.core.database.entity.NoteFtsEntity
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.ProjectEntity
import com.thesasan1.app.core.database.entity.SyncCursorEntity
import com.thesasan1.app.core.database.entity.TagEntity
import com.thesasan1.app.core.database.entity.TaskEntity
import com.thesasan1.app.core.database.entity.TaskFtsEntity
import com.thesasan1.app.core.database.entity.TaskHistoryEntity

/**
 * The on-device source of truth (ARCHITECTURE.md §2.5).
 *
 * Changing the schema: bump [version], add an `AutoMigration` (additive changes) or a `Migration`
 * in [DatabaseMigrations], commit the new file under `schemas/`, and add a case to
 * `DatabaseMigrationTest`. Destructive migrations
 * need owner approval (AGENTS.md §17); there is no destructive fallback.
 */
@Database(
    entities = [
        ProjectEntity::class,
        TagEntity::class,
        CategoryEntity::class,
        OutboxEntity::class,
        SyncCursorEntity::class,
        NoteEntity::class,
        NoteFtsEntity::class,
        TaskEntity::class,
        TaskFtsEntity::class,
        TaskHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2: notes and notes_fts (new tables only).
        AutoMigration(from = 1, to = 2),
        // v3: tasks, tasks_fts and task_history (new tables only).
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun tagDao(): TagDao
    abstract fun categoryDao(): CategoryDao
    abstract fun outboxDao(): OutboxDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val NAME = "personal_os.db"
    }
}

/** Hand-written migrations, oldest first. Additive versions use `autoMigrations` instead. */
object DatabaseMigrations {
    val ALL: Array<androidx.room.migration.Migration> = emptyArray()
}
