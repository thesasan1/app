package com.thesasan1.app.core.database

import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.core.database.entity.TaskEntity
import com.thesasan1.app.core.database.entity.TaskEventType
import com.thesasan1.app.core.database.entity.TaskHistoryEntity
import com.thesasan1.app.core.database.entity.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {

    @get:Rule
    val database = DatabaseTestRule()

    private val now = 1_764_000_000_000
    private val dao get() = database.db.taskDao()

    @Test
    fun openTasksSortByDueDateThenTimeThenPriority() = runTest {
        dao.upsert(task("undated-urgent", priority = TaskEntity.PRIORITY_URGENT))
        dao.upsert(task("later", dueDate = "2026-11-27"))
        dao.upsert(task("today-untimed", dueDate = "2026-11-25"))
        dao.upsert(task("today-9", dueDate = "2026-11-25", dueTime = "09:00"))
        dao.upsert(task("today-9-high", dueDate = "2026-11-25", dueTime = "09:00", priority = TaskEntity.PRIORITY_HIGH))

        assertEquals(
            listOf("today-9-high", "today-9", "today-untimed", "later", "undated-urgent"),
            dao.observeOpen().first().map { it.id },
        )
    }

    @Test
    fun completedCancelledArchivedAndTrashedTasksLeaveTheOpenList() = runTest {
        dao.upsert(task("open"))
        dao.upsert(task("doing").copy(status = TaskStatus.IN_PROGRESS))
        dao.upsert(task("done").copy(status = TaskStatus.DONE, completedAt = now))
        dao.upsert(task("cancelled").copy(status = TaskStatus.CANCELLED))
        dao.upsert(task("archived").copy(archivedAt = now))
        dao.upsert(task("trashed").let { it.copy(sync = it.sync.deleted(now)) })

        assertEquals(setOf("open", "doing"), dao.observeOpen().first().map { it.id }.toSet())
        assertEquals(listOf("done"), dao.observeCompleted().first().map { it.id })
        assertEquals(listOf("archived"), dao.observeArchived().first().map { it.id })
        assertEquals(listOf("trashed"), dao.observeTrash().first().map { it.id })
    }

    @Test
    fun dueOnAndOverdueUseTheLocalDate() = runTest {
        dao.upsert(task("yesterday", dueDate = "2026-11-24"))
        dao.upsert(task("today", dueDate = "2026-11-25"))
        dao.upsert(task("done-yesterday", dueDate = "2026-11-24").copy(status = TaskStatus.DONE))

        assertEquals(listOf("today"), dao.observeDueOn("2026-11-25").first().map { it.id })
        assertEquals(listOf("yesterday"), dao.observeOverdue("2026-11-25").first().map { it.id })
    }

    @Test
    fun subtasksStayOutOfTopLevelListsAndKeepManualOrder() = runTest {
        dao.upsert(task("parent"))
        dao.upsert(task("b").copy(parentId = "parent", sortOrder = "b"))
        dao.upsert(task("a").copy(parentId = "parent", sortOrder = "a", status = TaskStatus.DONE))

        assertEquals(listOf("parent"), dao.observeOpen().first().map { it.id })
        assertEquals(listOf("a", "b"), dao.observeSubtasks("parent").first().map { it.id })
    }

    @Test
    fun searchMatchesTitleAndDescriptionPrefixes() = runTest {
        dao.upsert(task("t1", title = "Call the dentist").copy(description = "ask about Friday"))
        dao.upsert(task("t2", title = "خرید نان"))

        assertEquals(listOf("t1"), dao.search(SearchText.matchQuery("frid")!!).first().map { it.id })
        assertEquals(listOf("t2"), dao.search(SearchText.matchQuery("نان")!!).first().map { it.id })
    }

    @Test
    fun historyIsReturnedInOrder() = runTest {
        dao.upsert(task("t"))
        dao.insertHistory(event("h2", TaskEventType.COMPLETED, now + 1))
        dao.insertHistory(event("h1", TaskEventType.CREATED, now))

        assertEquals(
            listOf(TaskEventType.CREATED, TaskEventType.COMPLETED),
            dao.observeHistory("t").first().map { it.eventType },
        )
    }

    @Test
    fun deletingForeverRemovesSubtasksIndexAndHistory() = runTest {
        dao.upsert(task("parent", title = "Unique parent"))
        dao.upsert(task("child", title = "Unique child").copy(parentId = "parent"))
        dao.insertHistory(event("h", TaskEventType.CREATED, now, taskId = "child"))

        dao.deletePermanently("parent")

        assertNull(dao.getById("parent"))
        assertNull(dao.getById("child"))
        assertEquals(emptyList<TaskEntity>(), dao.search(SearchText.matchQuery("unique")!!).first())
        assertEquals(emptyList<TaskHistoryEntity>(), dao.observeHistory("child").first())
    }

    @Test
    fun purgeRemovesOnlyTasksTrashedBeforeTheCutoff() = runTest {
        dao.upsert(task("old").let { it.copy(sync = it.sync.deleted(now)) })
        dao.upsert(task("recent").let { it.copy(sync = it.sync.deleted(now + 10)) })

        assertEquals(1, dao.purgeDeletedBefore(now + 5))
        assertEquals(listOf("recent"), dao.observeTrash().first().map { it.id })
    }

    private fun task(
        id: String,
        title: String = id,
        dueDate: String? = null,
        dueTime: String? = null,
        priority: Int = TaskEntity.PRIORITY_NONE,
    ) = TaskEntity(
        id = id,
        title = title,
        dueDate = dueDate,
        dueTime = dueTime,
        priority = priority,
        sync = SyncColumns.new(now),
    )

    private fun event(id: String, type: TaskEventType, at: Long, taskId: String = "t") =
        TaskHistoryEntity(id = id, taskId = taskId, eventType = type, occurredAt = at)
}
