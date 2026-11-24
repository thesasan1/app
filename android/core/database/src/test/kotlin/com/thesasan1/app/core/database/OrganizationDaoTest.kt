package com.thesasan1.app.core.database

import android.database.sqlite.SQLiteConstraintException
import com.thesasan1.app.core.database.entity.CategoryEntity
import com.thesasan1.app.core.database.entity.CategoryScope
import com.thesasan1.app.core.database.entity.ProjectEntity
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.core.database.entity.SyncState
import com.thesasan1.app.core.database.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrganizationDaoTest {

    @get:Rule
    val database = DatabaseTestRule()

    private val now = 1_764_000_000_000

    @Test
    fun projectRoundTripsAndSortsBySortOrder() = runTest {
        val dao = database.db.projectDao()
        dao.upsert(project("b", "Beta", sortOrder = 2))
        dao.upsert(project("a", "Alpha", sortOrder = 1))

        assertEquals(listOf("a", "b"), dao.observeActive().first().map { it.id })
        assertEquals(SyncState.PENDING, dao.getById("a")?.sync?.syncState)
    }

    @Test
    fun archivedAndDeletedProjectsAreHiddenFromActive() = runTest {
        val dao = database.db.projectDao()
        dao.upsert(project("keep", "Keep"))
        dao.upsert(project("archived", "Archived").copy(archivedAt = now))
        dao.upsert(project("deleted", "Deleted").let { it.copy(sync = it.sync.deleted(now + 1)) })

        assertEquals(listOf("keep"), dao.observeActive().first().map { it.id })
        assertEquals(now + 1, dao.getById("deleted")?.sync?.deletedAt)
    }

    @Test
    fun purgeRemovesOnlyOldTombstones() = runTest {
        val dao = database.db.projectDao()
        dao.upsert(project("old", "Old").let { it.copy(sync = it.sync.deleted(now)) })
        dao.upsert(project("recent", "Recent").let { it.copy(sync = it.sync.deleted(now + 10)) })
        dao.upsert(project("live", "Live"))

        assertEquals(1, dao.purgeDeletedBefore(now + 5))
        assertNull(dao.getById("old"))
        assertEquals("recent", dao.getById("recent")?.id)
        assertEquals("live", dao.getById("live")?.id)
    }

    @Test
    fun tagNamesAreUniqueIgnoringCaseAndSpaces() = runTest {
        val dao = database.db.tagDao()
        dao.upsert(TagEntity(id = "t1", name = "Work", sync = SyncColumns.new(now)))

        assertEquals("t1", dao.getByNameKey(TagEntity.nameKeyOf("  wORK "))?.id)
        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking {
                dao.upsert(TagEntity(id = "t2", name = "work ", sync = SyncColumns.new(now)))
            }
        }
    }

    @Test
    fun persianTagNamesAreStoredUnchanged() = runTest {
        val dao = database.db.tagDao()
        dao.upsert(TagEntity(id = "fa", name = "کار", sync = SyncColumns.new(now)))
        assertEquals("کار", dao.observeActive().first().single().name)
    }

    @Test
    fun categoriesAreFilteredByScope() = runTest {
        val dao = database.db.categoryDao()
        dao.upsert(CategoryEntity("c1", "Personal", CategoryScope.TASK, SyncColumns.new(now)))
        dao.upsert(CategoryEntity("c2", "Ideas", CategoryScope.NOTE, SyncColumns.new(now)))

        assertEquals(listOf("c1"), dao.observeActive(CategoryScope.TASK).first().map { it.id })
    }

    private fun project(id: String, name: String, sortOrder: Long = 0) =
        ProjectEntity(id = id, name = name, sortOrder = sortOrder, sync = SyncColumns.new(now))
}
