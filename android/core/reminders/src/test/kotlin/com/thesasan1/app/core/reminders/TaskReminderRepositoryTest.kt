package com.thesasan1.app.core.reminders

import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.ReminderEntity
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.core.reminders.domain.OfflineTaskReminderRepository
import com.thesasan1.app.core.reminders.domain.ReminderUnit
import com.thesasan1.app.core.reminders.domain.TaskReminder
import com.thesasan1.app.core.reminders.domain.customReminderOffset
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskReminderRepositoryTest {
    private val dao = FakeReminderDao()
    private val outbox = object : OutboxDao {
        val entries = mutableListOf<OutboxEntity>()

        override suspend fun enqueue(entry: OutboxEntity): Long {
            entries += entry
            return entries.size.toLong()
        }

        override suspend fun nextBatch(limit: Int) = entries.take(limit)
        override suspend fun recordFailure(seq: Long, errorCode: String) = Unit
        override suspend fun acknowledge(seqs: List<Long>) = 0
        override fun observePendingCount(): Flow<Int> = MutableStateFlow(entries.size)
    }
    private val transaction = object : TransactionRunner {
        override suspend fun <R> invoke(block: suspend () -> R): R = block()
    }
    private var next = 0
    private val ids = IdGenerator { "id-${next++}" }
    private val clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC)
    private val repository = OfflineTaskReminderRepository(dao, outbox, transaction, ids, clock)

    @Test
    fun addingTheSameOffsetTwiceKeepsOneReminder() = runTest {
        repository.add("t", -10)
        repository.add("t", -10)
        repository.add("t", 0)

        val reminders = repository.observe("t").first()
        assertEquals(listOf(-10, 0), reminders.map(TaskReminder::offsetMinutes))
        assertEquals(2, outbox.entries.size)
        assertTrue(outbox.entries.all { it.entityType == "reminder" })
    }

    @Test
    fun removingSoftDeletesAndQueuesTheChange() = runTest {
        repository.add("t", 0)
        val id = repository.observe("t").first().single().id

        repository.remove(id)

        assertTrue(repository.observe("t").first().isEmpty())
        assertEquals(clock.millis(), dao.reminders.value.getValue(id).sync.deletedAt)
        assertTrue(outbox.entries.last().payload!!.contains("\"deletedAt\":"))
    }

    @Test
    fun repeatUntilDoneIsSavedOnlyWhenItChanges() = runTest {
        repository.add("t", 0)
        val id = repository.observe("t").first().single().id

        repository.setRepeatUntilDone(id, true)
        repository.setRepeatUntilDone(id, true)

        assertTrue(repository.observe("t").first().single().repeatUntilDone)
        assertEquals(2, outbox.entries.size)
    }

    @Test
    fun payloadIsValidJsonShape() {
        val payload = OfflineTaskReminderRepository.syncPayload(
            ReminderEntity(
                id = "r",
                targetType = ReminderTargetType.TASK,
                targetId = "t",
                offsetMinutes = -60,
                sync = SyncColumns.new(5),
            ),
        )
        assertEquals(
            "{\"id\":\"r\",\"targetType\":\"TASK\",\"targetId\":\"t\",\"triggerType\":\"TIME\"," +
                "\"offsetMinutes\":-60,\"repeatUntilDone\":false,\"isEnabled\":true,\"updatedAt\":5}",
            payload,
        )
    }

    @Test
    fun customOffsetsAreBeforeTheDueTimeAndWithinThirtyDays() {
        assertEquals(-90, customReminderOffset(90, ReminderUnit.MINUTES))
        assertEquals(-120, customReminderOffset(2, ReminderUnit.HOURS))
        assertEquals(-30 * 24 * 60, customReminderOffset(30, ReminderUnit.DAYS))
        assertNull(customReminderOffset(31, ReminderUnit.DAYS))
        assertNull(customReminderOffset(0, ReminderUnit.MINUTES))
        assertNull(customReminderOffset(Int.MAX_VALUE, ReminderUnit.DAYS))
    }
}
