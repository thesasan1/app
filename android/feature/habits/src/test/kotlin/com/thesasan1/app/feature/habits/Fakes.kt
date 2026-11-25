package com.thesasan1.app.feature.habits

import com.thesasan1.app.core.common.IdGenerator
import com.thesasan1.app.core.database.TransactionRunner
import com.thesasan1.app.core.database.dao.HabitDao
import com.thesasan1.app.core.database.dao.OutboxDao
import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitEventEntity
import com.thesasan1.app.core.database.entity.OutboxEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [HabitDao]. Room's SQL is covered by HabitDaoTest in :core:database. */
class FakeHabitDao : HabitDao {
    val habits = MutableStateFlow<Map<String, HabitEntity>>(emptyMap())
    val events = MutableStateFlow<List<HabitEventEntity>>(emptyList())

    override suspend fun upsert(habit: HabitEntity) {
        habits.value += habit.id to habit
    }

    override suspend fun getById(id: String) = habits.value[id]
    override fun observe(id: String) = habits.map { it[id]?.takeIf { h -> h.sync.deletedAt == null } }
    override fun observeActive() = habits.map { all ->
        all.values.filter { it.archivedAt == null && it.sync.deletedAt == null }.sortedBy { it.sync.createdAt }
    }
    override fun observeArchived() = habits.map { all ->
        all.values.filter { it.archivedAt != null && it.sync.deletedAt == null }
    }
    override suspend fun getEvent(id: String) = events.value.firstOrNull { it.id == id }
    override suspend fun getEventsOn(habitId: String, date: String) =
        events.value.filter { it.habitId == habitId && it.localDate == date }

    override suspend fun insertEvent(event: HabitEventEntity): Long {
        if (events.value.any { it.id == event.id }) return -1
        events.value += event
        return events.value.size.toLong()
    }

    override fun observeEvents(habitId: String, from: String, to: String): Flow<List<HabitEventEntity>> =
        events.map { all -> all.filter { it.habitId == habitId && it.localDate in from..to } }
    override suspend fun getEvents(habitId: String) = events.value.filter { it.habitId == habitId }
    override fun observeEventsOn(date: String) = events.map { all -> all.filter { it.localDate == date } }
}

class FakeOutboxDao : OutboxDao {
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

object DirectTransactionRunner : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R = block()
}

class SequentialIds : IdGenerator {
    private var next = 0
    override fun newId() = "id-${++next}"
}

class MutableClock(var now: Long) : Clock() {
    override fun instant(): Instant = Instant.ofEpochMilli(now)
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
}
