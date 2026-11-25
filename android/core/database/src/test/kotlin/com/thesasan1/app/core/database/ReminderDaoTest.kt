package com.thesasan1.app.core.database

import com.thesasan1.app.core.database.entity.OccurrenceState
import com.thesasan1.app.core.database.entity.ReminderEntity
import com.thesasan1.app.core.database.entity.ReminderOccurrenceEntity
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.database.entity.ReminderTriggerType
import com.thesasan1.app.core.database.entity.SyncColumns
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderDaoTest {

    @get:Rule
    val database = DatabaseTestRule()

    private val now = 1_764_000_000_000
    private val dao get() = database.db.reminderDao()

    private fun reminder(
        id: String,
        offset: Int? = null,
        enabled: Boolean = true,
        trigger: ReminderTriggerType = ReminderTriggerType.TIME,
    ) = ReminderEntity(
        id = id,
        targetType = ReminderTargetType.TASK,
        targetId = "task-1",
        triggerType = trigger,
        offsetMinutes = offset,
        isEnabled = enabled,
        sync = SyncColumns.new(now),
    )

    @Test
    fun remindersForATargetSortByOffsetAndSkipDeletedOnes() = runTest {
        dao.upsert(reminder("at-due", offset = 0))
        dao.upsert(reminder("hour-before", offset = -60))
        dao.upsert(reminder("gone", offset = -10).let { it.copy(sync = it.sync.deleted(now)) })

        assertEquals(
            listOf("hour-before", "at-due"),
            dao.observeForTarget(ReminderTargetType.TASK, "task-1").first().map { it.id },
        )
    }

    @Test
    fun onlyEnabledLiveTimeRemindersAreRebuilt() = runTest {
        dao.upsert(reminder("on"))
        dao.upsert(reminder("off", enabled = false))
        dao.upsert(reminder("place", trigger = ReminderTriggerType.LOCATION))
        dao.upsert(reminder("deleted").let { it.copy(sync = it.sync.deleted(now)) })

        assertEquals(listOf("on"), dao.getEnabledTimeReminders().map { it.id })
    }

    @Test
    fun anOccurrenceIsStoredOnceHoweverOftenItIsRebuilt() = runTest {
        dao.upsert(reminder("r"))
        val first = dao.insertOccurrence(ReminderOccurrenceEntity.scheduled("r", now))
        dao.updateOccurrence(
            dao.getOccurrence(ReminderOccurrenceEntity.idFor("r", now))!!.copy(state = OccurrenceState.DELIVERED),
        )
        val again = dao.insertOccurrence(ReminderOccurrenceEntity.scheduled("r", now))

        assertEquals(-1L, again)
        assertEquals(true, first > 0)
        assertEquals(OccurrenceState.DELIVERED, dao.getOccurrencesFor("r").single().state)
    }

    @Test
    fun pendingOccurrencesSortByWhenTheyRingAndPruningKeepsThem() = runTest {
        dao.upsert(reminder("r"))
        dao.insertOccurrence(ReminderOccurrenceEntity.scheduled("r", now + 3_000))
        dao.insertOccurrence(
            ReminderOccurrenceEntity.scheduled("r", now).copy(
                state = OccurrenceState.SNOOZED,
                snoozeUntil =
                now + 1_000,
            ),
        )
        dao.insertOccurrence(
            ReminderOccurrenceEntity.scheduled("r", now - 5_000).copy(state = OccurrenceState.DELIVERED),
        )

        assertEquals(listOf(now + 1_000, now + 3_000), dao.getPendingOccurrences().map { it.nextFireAt })
        assertEquals(1, dao.pruneFinishedOccurrences(beforeUtc = now + 10_000))
        assertEquals(2, dao.getOccurrencesFor("r").size)

        dao.deletePendingOccurrencesFor("r")
        assertEquals(0, dao.getPendingOccurrences().size)
    }

    @Test
    fun deletingForeverRemovesTheReminderAndItsOccurrences() = runTest {
        dao.upsert(reminder("r"))
        dao.insertOccurrence(ReminderOccurrenceEntity.scheduled("r", now).copy(state = OccurrenceState.DELIVERED))
        dao.deletePermanently("r")

        assertNull(dao.getById("r"))
        assertEquals(0, dao.getOccurrencesFor("r").size)
    }
}
