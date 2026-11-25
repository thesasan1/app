package com.thesasan1.app.core.reminders

import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.reminders.domain.OfflineHabitReminderRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HabitReminderRepositoryTest {
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
    private val clock = Clock.fixed(Instant.ofEpochMilli(1_795_608_000_000), ZoneOffset.UTC)
    private val repository = OfflineHabitReminderRepository(dao, outbox, transaction, ids, clock)

    @Test
    fun aHabitHasOneReminderThatCanMoveAndBeRemoved() = runTest {
        repository.set("h", LocalTime.of(8, 30))
        repository.set("h", LocalTime.of(8, 30))
        assertEquals(LocalTime.of(8, 30), repository.observe("h").first()?.time)
        assertEquals(1, outbox.entries.size)

        repository.set("h", LocalTime.of(21, 0))
        val rows = dao.getForTarget(ReminderTargetType.HABIT, "h")
        assertEquals(listOf(21 * 60), rows.map { it.offsetMinutes })
        assertEquals(LocalTime.of(21, 0), repository.observe("h").first()?.time)

        repository.set("h", null)
        assertNull(repository.observe("h").first())
        assertEquals(3, outbox.entries.size)
    }
}
