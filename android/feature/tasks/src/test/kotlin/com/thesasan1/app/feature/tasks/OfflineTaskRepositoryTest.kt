package com.thesasan1.app.feature.tasks

import com.thesasan1.app.core.common.AppError
import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.common.getOrNull
import com.thesasan1.app.core.database.entity.OutboxOp
import com.thesasan1.app.feature.tasks.data.OfflineTaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskDraft
import com.thesasan1.app.feature.tasks.domain.TaskEventType
import com.thesasan1.app.feature.tasks.domain.TaskPriority
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineTaskRepositoryTest {

    private val day = 24L * 60 * 60 * 1000
    private val clock = MutableClock(1_764_000_000_000)
    private val taskDao = FakeTaskDao()
    private val outbox = FakeOutboxDao()
    private val repository = OfflineTaskRepository(taskDao, outbox, DirectTransactionRunner, SequentialIds(), clock)

    private suspend fun create(title: String = "Buy bread", parentId: String? = null, projectId: String? = null) =
        repository.create(TaskDraft(title = title, parentId = parentId, projectId = projectId)).getOrNull()!!

    private suspend fun events(taskId: String) = repository.observeHistory(taskId).first()

    @Test
    fun `create trims the title and records history and outbox entries together`() = runTest {
        val task = create("  Buy bread  ")

        assertEquals("Buy bread", task.title)
        assertEquals(TaskStatus.TODO, task.status)
        assertEquals(listOf(TaskEventType.CREATED), events(task.id).map { it.type })
        assertEquals(
            listOf(OfflineTaskRepository.ENTITY_TYPE, OfflineTaskRepository.HISTORY_ENTITY_TYPE),
            outbox.entries.map { it.entityType },
        )
        assertTrue(outbox.entries.first().payload!!.contains("\"title\":\"Buy bread\""))
    }

    @Test
    fun `invalid drafts are rejected`() = runTest {
        assertValidation("title", repository.create(TaskDraft(title = "   ")))
        assertValidation("title", repository.create(TaskDraft(title = "x".repeat(501))))
        assertValidation("due_time", repository.create(TaskDraft(title = "a", dueTime = LocalTime.of(9, 0))))
        assertTrue(outbox.entries.isEmpty())
    }

    @Test
    fun `subtasks are one level deep and inherit the parent's project`() = runTest {
        val parent = create("Trip", projectId = "p1")
        val child = create("Pack", parentId = parent.id)

        assertEquals("p1", child.projectId)
        assertEquals(listOf(child.id), repository.observeSubtasks(parent.id).first().map { it.id })
        assertValidation("parent_id", repository.create(TaskDraft(title = "Socks", parentId = child.id)))
        assertValidation("parent_id", repository.create(TaskDraft(title = "x", parentId = "missing")))

        repository.moveToTrash(parent.id)
        assertValidation("parent_id", repository.create(TaskDraft(title = "Late", parentId = parent.id)))
    }

    @Test
    fun `status changes record completion, reopening and plain changes`() = runTest {
        val task = create()

        clock.now += 1_000
        val done = repository.setStatus(task.id, TaskStatus.DONE).getOrNull()!!
        assertEquals(clock.now, done.completedAt)

        val reopened = repository.setStatus(task.id, TaskStatus.TODO).getOrNull()!!
        assertNull(reopened.completedAt)
        repository.setStatus(task.id, TaskStatus.IN_PROGRESS)

        val history = events(task.id)
        assertEquals(
            listOf(
                TaskEventType.CREATED,
                TaskEventType.COMPLETED,
                TaskEventType.REOPENED,
                TaskEventType.STATUS_CHANGED,
            ),
            history.map { it.type },
        )
        assertEquals("""{"status":"TODO"}""", history[3].from)
        assertEquals("""{"status":"IN_PROGRESS"}""", history[3].to)
    }

    @Test
    fun `a change that alters nothing writes nothing`() = runTest {
        val task = create()
        val before = outbox.entries.size

        clock.now += 1_000
        val same = repository.edit(task.id, "Buy bread", null, TaskPriority.NONE).getOrNull()!!
        repository.setStatus(task.id, TaskStatus.TODO)
        repository.setArchived(task.id, false)

        assertEquals(task.updatedAt, same.updatedAt)
        assertEquals(before, outbox.entries.size)
        assertEquals(1, events(task.id).size)
    }

    @Test
    fun `edit records only the fields that changed`() = runTest {
        val task = create()
        repository.edit(task.id, "Buy bread", "whole wheat", TaskPriority.HIGH)

        val edit = events(task.id).last()
        assertEquals(TaskEventType.EDITED, edit.type)
        assertEquals("""{"description":null,"priority":0}""", edit.from)
        assertEquals("""{"description":"whole wheat","priority":3}""", edit.to)
    }

    @Test
    fun `reschedule sets the due date and time and clears a snooze`() = runTest {
        val task = create()
        repository.snooze(task.id, clock.now + day)

        val moved = repository.reschedule(task.id, LocalDate.of(2026, 11, 26), LocalTime.of(9, 30)).getOrNull()!!

        assertEquals(LocalDate.of(2026, 11, 26), moved.dueDate)
        assertEquals(LocalTime.of(9, 30), moved.dueTime)
        assertNull(moved.snoozedUntil)
        assertEquals(listOf(TaskEventType.SNOOZED, TaskEventType.RESCHEDULED), events(task.id).drop(1).map { it.type })
        assertValidation("due_time", repository.reschedule(task.id, null, LocalTime.NOON))
        assertEquals(
            listOf(task.id),
            repository.observeDueOn(LocalDate.of(2026, 11, 26)).first().map { it.id },
        )
    }

    @Test
    fun `archive, trash and restore move the task between lists`() = runTest {
        val task = create()

        repository.setArchived(task.id, true)
        assertEquals(listOf(task.id), repository.observeArchived().first().map { it.id })
        repository.setArchived(task.id, false)
        repository.moveToTrash(task.id)
        assertEquals(listOf(task.id), repository.observeTrash().first().map { it.id })
        repository.restoreFromTrash(task.id)

        assertEquals(listOf(task.id), repository.observeOpen().first().map { it.id })
        assertEquals(
            listOf(
                TaskEventType.CREATED,
                TaskEventType.ARCHIVED,
                TaskEventType.RESTORED,
                TaskEventType.DELETED,
                TaskEventType.RESTORED,
            ),
            events(task.id).map { it.type },
        )
    }

    @Test
    fun `a subtask can't be restored while its parent is in the trash`() = runTest {
        val parent = create("Trip")
        val child = create("Pack", parentId = parent.id)
        repository.moveToTrash(child.id)
        repository.moveToTrash(parent.id)

        assertValidation("parent_id", repository.restoreFromTrash(child.id))
        repository.restoreFromTrash(parent.id)
        assertTrue(repository.restoreFromTrash(child.id) is Outcome.Success)
    }

    @Test
    fun `delete forever needs the trash and removes subtasks and history`() = runTest {
        val parent = create("Trip")
        val child = create("Pack", parentId = parent.id)

        assertValidation("deleted_at", repository.deletePermanently(parent.id))
        repository.moveToTrash(parent.id)
        assertTrue(repository.deletePermanently(parent.id) is Outcome.Success)

        assertTrue(taskDao.rows.value.isEmpty())
        assertTrue(taskDao.history.value.isEmpty())
        val deletes = outbox.entries.filter { it.op == OutboxOp.DELETE }.map { it.entityId }.toSet()
        assertEquals(setOf(parent.id, child.id), deletes)
        assertEquals(AppError.NotFound, (repository.deletePermanently(parent.id) as Outcome.Failure).error)
    }

    @Test
    fun `purge removes tasks trashed more than 30 days ago`() = runTest {
        val old = create("Old")
        repository.moveToTrash(old.id)
        clock.now += 31 * day
        val recent = create("Recent")
        repository.moveToTrash(recent.id)

        assertEquals(1, repository.purgeExpiredTrash().getOrNull())
        assertEquals(listOf(recent.id), repository.observeTrash().first().map { it.id })
    }

    @Test
    fun `blank search matches nothing and unknown ids are not found`() = runTest {
        create()
        assertTrue(repository.search("  ").first().isEmpty())
        assertEquals(AppError.NotFound, (repository.get("nope") as Outcome.Failure).error)
        assertNotNull(repository.observeOpen().first().singleOrNull())
    }

    private fun assertValidation(field: String, outcome: Outcome<*>) {
        val error = (outcome as? Outcome.Failure)?.error
        assertTrue(
            error is AppError.Validation && error.field == field,
            "expected a $field validation error, got $outcome",
        )
    }
}
