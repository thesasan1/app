package com.thesasan1.app.core.database

import android.database.sqlite.SQLiteConstraintException
import com.thesasan1.app.core.database.entity.OutboxEntity
import com.thesasan1.app.core.database.entity.OutboxOp
import com.thesasan1.app.core.database.entity.ProjectEntity
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.core.database.entity.SyncCursorEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OutboxDaoTest {

    @get:Rule
    val database = DatabaseTestRule()

    private val now = 1_764_000_000_000

    @Test
    fun batchesComeBackInEnqueueOrder() = runTest {
        val dao = database.db.outboxDao()
        val first = dao.enqueue(entry("p1", "k1"))
        val second = dao.enqueue(entry("p2", "k2"))
        dao.enqueue(entry("p3", "k3"))

        assertEquals(listOf(first, second), dao.nextBatch(limit = 2).map { it.seq })
    }

    @Test
    fun failuresIncrementAttemptsAndAcknowledgeRemoves() = runTest {
        val dao = database.db.outboxDao()
        val seq = dao.enqueue(entry("p1", "k1"))
        dao.recordFailure(seq, "HTTP_503")
        dao.recordFailure(seq, "TIMEOUT")

        val stored = dao.nextBatch(10).single()
        assertEquals(2, stored.attemptCount)
        assertEquals("TIMEOUT", stored.lastError)

        assertEquals(1, dao.acknowledge(listOf(seq)))
        assertEquals(0, dao.observePendingCount().first())
    }

    @Test
    fun idempotencyKeysAreUnique() = runTest {
        val dao = database.db.outboxDao()
        dao.enqueue(entry("p1", "same"))
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.enqueue(entry("p2", "same")) }
        }
    }

    @Test
    fun changeAndOutboxEntryRollBackTogether() = runTest {
        val runner = RoomTransactionRunner(database.db)
        val failure = runCatching {
            runner {
                database.db.projectDao().upsert(ProjectEntity(id = "p1", name = "P", sync = SyncColumns.new(now)))
                database.db.outboxDao().enqueue(entry("p1", "k1"))
                error("simulated crash before commit")
            }
        }

        assertEquals("simulated crash before commit", failure.exceptionOrNull()?.message)
        assertNull(database.db.projectDao().getById("p1"))
        assertEquals(0, database.db.outboxDao().observePendingCount().first())
    }

    @Test
    fun syncCursorIsStoredPerScope() = runTest {
        val dao = database.db.syncCursorDao()
        dao.put(SyncCursorEntity("projects", "c-1"))
        dao.put(SyncCursorEntity("projects", "c-2"))

        assertEquals("c-2", dao.get("projects"))
        assertNull(dao.get("tags"))
    }

    private fun entry(entityId: String, key: String) = OutboxEntity(
        entityType = "project",
        entityId = entityId,
        op = OutboxOp.UPSERT,
        payload = """{"id":"$entityId"}""",
        idempotencyKey = key,
        createdAt = now,
    )
}
