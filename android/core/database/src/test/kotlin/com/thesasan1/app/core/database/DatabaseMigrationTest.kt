package com.thesasan1.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migration harness. Each schema version gets a test that creates the old database from its
 * exported JSON, migrates it to the latest version, and lets Room validate the result.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    @Suppress("SpreadOperator") // the helper only takes varargs; test code
    fun version1SchemaMatchesTheExportedJson() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'sync_outbox'").use {
                assertEquals(1, it.count)
            }
        }
        // Opens the database through Room, which fails if the exported schema and entities differ.
        helper.runMigrationsAndValidate(TEST_DB, 1, true, *DatabaseMigrations.ALL).close()
    }

    @Test
    fun version1MigratesTo2WithNotesAndTheirIndex() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 2, true).use { db ->
            db.query("SELECT name FROM sqlite_master WHERE name IN ('notes', 'notes_fts') ORDER BY name").use {
                assertEquals(2, it.count)
            }
        }
    }

    @Test
    fun version2MigratesTo3WithTasksTheirIndexAndHistory() {
        helper.createDatabase(TEST_DB, 2).close()
        helper.runMigrationsAndValidate(TEST_DB, 3, true).use { db ->
            db.query(
                "SELECT name FROM sqlite_master WHERE name IN ('tasks', 'tasks_fts', 'task_history') ORDER BY name",
            ).use { assertEquals(3, it.count) }
        }
    }

    @Test
    fun version3MigratesTo4WithRemindersAndOccurrences() {
        helper.createDatabase(TEST_DB, 3).close()
        helper.runMigrationsAndValidate(TEST_DB, 4, true).use { db ->
            db.query(
                "SELECT name FROM sqlite_master WHERE name IN ('reminders', 'reminder_occurrences') ORDER BY name",
            ).use { assertEquals(2, it.count) }
        }
    }

    @Test
    fun version4MigratesTo5WithHabitsAndTheirEvents() {
        helper.createDatabase(TEST_DB, 4).close()
        helper.runMigrationsAndValidate(TEST_DB, 5, true).use { db ->
            db.query(
                "SELECT name FROM sqlite_master WHERE name IN ('habits', 'habit_events') ORDER BY name",
            ).use { assertEquals(2, it.count) }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
