package com.thesasan1.app.core.database

import com.thesasan1.app.core.database.entity.HabitEntity
import com.thesasan1.app.core.database.entity.HabitEventEntity
import com.thesasan1.app.core.database.entity.HabitEventType
import com.thesasan1.app.core.database.entity.HabitMeasurement
import com.thesasan1.app.core.database.entity.SyncColumns
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HabitDaoTest {

    @get:Rule
    val database = DatabaseTestRule()

    private val now = 1_764_000_000_000
    private val dao get() = database.db.habitDao()

    private fun habit(id: String, sortOrder: Int = 0) = HabitEntity(
        id = id,
        name = id,
        startDate = "2026-11-01",
        sortOrder = sortOrder,
        sync = SyncColumns.new(now),
    )

    private fun event(id: String, date: String, type: HabitEventType = HabitEventType.CHECK_IN, at: Long = now) =
        HabitEventEntity(id = id, habitId = "read", localDate = date, eventType = type, recordedAt = at)

    @Test
    fun activeHabitsSkipArchivedAndDeletedOnes() = runTest {
        dao.upsert(habit("read", sortOrder = 1))
        dao.upsert(habit("walk", sortOrder = 0))
        dao.upsert(habit("old").copy(archivedAt = now))
        dao.upsert(habit("gone").let { it.copy(sync = it.sync.deleted(now)) })

        assertEquals(listOf("walk", "read"), dao.observeActive().first().map { it.id })
        assertEquals(listOf("old"), dao.observeArchived().first().map { it.id })
    }

    @Test
    fun aHabitKeepsItsMeasurementAndSchedule() = runTest {
        dao.upsert(habit("read").copy(measurementType = HabitMeasurement.NUMERIC, targetValue = 20.0, unit = "pages"))

        val stored = dao.getById("read")!!
        assertEquals(HabitMeasurement.NUMERIC, stored.measurementType)
        assertEquals(20.0, stored.targetValue!!, 0.0)
    }

    @Test
    fun eventsAreAppendOnlyAndMergeByIdWithoutDuplicates() = runTest {
        assertTrue(dao.insertEvent(event("e1", "2026-11-24")) > 0)
        assertEquals(-1L, dao.insertEvent(event("e1", "2026-11-24", HabitEventType.UNDO)))

        assertEquals(listOf(HabitEventType.CHECK_IN), dao.getEvents("read").map { it.eventType })
    }

    @Test
    fun eventsReadByHabitDayRangeInRecordedOrder() = runTest {
        dao.insertEvent(event("late", "2026-11-25", at = now + 2))
        dao.insertEvent(event("early", "2026-11-25", at = now + 1))
        dao.insertEvent(event("before", "2026-11-20"))
        dao.insertEvent(event("after", "2026-11-27"))

        assertEquals(
            listOf("early", "late"),
            dao.observeEvents("read", "2026-11-24", "2026-11-26").first().map { it.id },
        )
        assertEquals(listOf("early", "late"), dao.observeEventsOn("2026-11-25").first().map { it.id })
    }
}
