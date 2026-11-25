package com.thesasan1.app.core.reminders

import com.thesasan1.app.core.database.entity.OccurrenceState
import com.thesasan1.app.core.database.entity.ReminderEntity
import com.thesasan1.app.core.database.entity.ReminderOccurrenceEntity
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.database.entity.SyncColumns
import com.thesasan1.app.core.reminders.domain.ReminderScheduler
import com.thesasan1.app.core.reminders.domain.ReminderTarget
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReminderSchedulerTest {
    private val now = Instant.parse("2026-11-25T12:00:00Z")
    private var clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val dao = FakeReminderDao()
    private val alarms = FakeAlarms()
    private val notifier = FakeNotifier()
    private val actions = FakeTargetActions()
    private val tasks = mutableMapOf("t" to ReminderTarget("Pay rent", "2026-11-26", "10:00"))

    private val scheduler
        get() = ReminderScheduler(dao, { _, id -> tasks[id] }, actions, alarms, notifier, clock)

    private fun reminder(
        id: String = "r",
        offset: Int? = 0,
        local: String? = null,
        enabled: Boolean = true,
        repeat: Boolean = false,
    ) = ReminderEntity(
        id = id,
        targetType = if (local == null) ReminderTargetType.TASK else ReminderTargetType.STANDALONE,
        targetId = if (local == null) "t" else null,
        title = if (local == null) null else "Stretch",
        offsetMinutes = offset.takeIf { local == null },
        triggerAtLocal = local,
        isEnabled = enabled,
        repeatUntilDone = repeat,
        sync = SyncColumns.new(0),
    )

    private val dueAt = Instant.parse("2026-11-26T10:00:00Z").toEpochMilli()

    @Test
    fun rebuildingTwiceSetsOneAlarmForOneOccurrence() = runTest {
        dao.upsert(reminder())
        scheduler.rebuild()
        scheduler.rebuild()

        assertEquals(1, dao.occurrences.size)
        assertEquals(mapOf(ReminderOccurrenceEntity.idFor("r", dueAt) to dueAt), alarms.set)
    }

    @Test
    fun movingTheTaskReplacesTheOccurrenceAndItsAlarm() = runTest {
        dao.upsert(reminder(offset = -60))
        scheduler.rebuild()
        tasks["t"] = tasks.getValue("t").copy(dueTime = "15:00")
        scheduler.rebuild()

        val newAt = Instant.parse("2026-11-26T14:00:00Z").toEpochMilli()
        assertEquals(listOf(newAt), dao.occurrences.values.map { it.scheduledForUtc })
        assertEquals(mapOf(ReminderOccurrenceEntity.idFor("r", newAt) to newAt), alarms.set)
    }

    @Test
    fun completingTheTaskOrDisablingTheReminderCancelsTheAlarm() = runTest {
        dao.upsert(reminder("a"))
        dao.upsert(reminder("b", offset = -5))
        scheduler.rebuild()
        tasks["t"] = tasks.getValue("t").copy(isOpen = false)
        scheduler.rebuild()
        assertTrue(alarms.set.isEmpty())

        tasks["t"] = tasks.getValue("t").copy(isOpen = true)
        dao.upsert(reminder("a", enabled = false))
        scheduler.rebuild()
        assertEquals(setOf(ReminderOccurrenceEntity.idFor("b", dueAt - 300_000)), alarms.set.keys)
    }

    @Test
    fun deliveringRingsOnceAndNeverAgain() = runTest {
        dao.upsert(reminder())
        scheduler.rebuild()
        clock = Clock.fixed(Instant.ofEpochMilli(dueAt), ZoneOffset.UTC)

        scheduler.deliver(id)
        scheduler.deliver(id) // a duplicate broadcast
        scheduler.rebuild() // e.g. a reboot right after

        assertEquals(listOf(id to "Pay rent"), notifier.shown.map { it.occurrenceId to it.title })
        assertEquals(OccurrenceState.DELIVERED, dao.occurrences.getValue(id).state)
        assertEquals(dueAt, dao.occurrences.getValue(id).deliveredAt)
        assertTrue(alarms.set.isEmpty())
    }

    @Test
    fun blockedNotificationsRecordAMissRatherThanADelivery() = runTest {
        notifier.enabled = false
        dao.upsert(reminder())
        scheduler.rebuild()

        scheduler.deliver(id)

        assertEquals(OccurrenceState.MISSED, dao.occurrences.getValue(id).state)
    }

    @Test
    fun aSlightlyLateFiringRingsNowButALongMissedOneDoesNot() = runTest {
        dao.upsert(reminder("soon", local = "2026-11-25T11:00"))
        dao.upsert(reminder("old", local = "2026-11-24T08:00"))
        scheduler.rebuild()

        val soonId = ReminderOccurrenceEntity.idFor("soon", Instant.parse("2026-11-25T11:00:00Z").toEpochMilli())
        assertEquals(mapOf(soonId to now.toEpochMilli()), alarms.set)
        assertEquals(OccurrenceState.MISSED, dao.getOccurrencesFor("old").single().state)
    }

    @Test
    fun aTimeZoneChangeMovesAFloatingReminder() = runTest {
        dao.upsert(reminder("s", local = "2026-11-26T08:00"))
        scheduler.rebuild()
        clock = Clock.fixed(now, ZoneId.of("Asia/Tehran"))
        scheduler.rebuild()

        val tehranAt = Instant.parse("2026-11-26T04:30:00Z").toEpochMilli()
        assertEquals(listOf(tehranAt), alarms.set.values.toList())
        assertEquals(1, dao.occurrences.size)
    }

    @Test
    fun aDeletedReminderIsDismissedIfItsAlarmStillFires() = runTest {
        dao.upsert(reminder())
        scheduler.rebuild()
        dao.reminders.value = mapOf("r" to reminder().let { it.copy(sync = it.sync.deleted(1)) })

        scheduler.deliver(id)

        assertTrue(notifier.shown.isEmpty())
        // The occurrence is dismissed, and the rebuild then forgets nothing still pending.
        assertTrue(dao.getPendingOccurrences().isEmpty())
    }

    private val id = ReminderOccurrenceEntity.idFor("r", dueAt)
    private val minute = 60_000L

    private suspend fun ringAt(atMillis: Long) {
        clock = Clock.fixed(Instant.ofEpochMilli(atMillis), ZoneOffset.UTC)
        scheduler.deliver(id)
    }

    @Test
    fun doneCompletesTheTaskAndClearsTheNotification() = runTest {
        dao.upsert(reminder())
        scheduler.rebuild()
        ringAt(dueAt)

        scheduler.complete(id)

        assertEquals(OccurrenceState.COMPLETED, dao.occurrences.getValue(id).state)
        assertEquals(listOf("t"), actions.completed)
        assertTrue(notifier.visible.isEmpty())
        assertTrue(alarms.set.isEmpty())
    }

    @Test
    fun snoozeRingsAgainTenMinutesLater() = runTest {
        dao.upsert(reminder())
        scheduler.rebuild()
        ringAt(dueAt)

        scheduler.snooze(id)
        assertEquals(mapOf(id to dueAt + 10 * minute), alarms.set)
        assertTrue(notifier.visible.isEmpty())

        ringAt(dueAt + 10 * minute)
        assertEquals(2, notifier.shown.size)
        assertEquals(OccurrenceState.DELIVERED, dao.occurrences.getValue(id).state)
        assertTrue(alarms.set.isEmpty())
    }

    @Test
    fun repeatUntilDoneKeepsRingingThroughASwipeUntilDone() = runTest {
        dao.upsert(reminder(repeat = true))
        scheduler.rebuild()
        ringAt(dueAt)

        assertTrue(notifier.shown.single().repeats)
        assertEquals(mapOf(id to dueAt + 15 * minute), alarms.set)
        scheduler.dismissed(id)
        assertEquals(OccurrenceState.SNOOZED, dao.occurrences.getValue(id).state)

        ringAt(dueAt + 15 * minute)
        assertEquals(2, notifier.shown.size)
        scheduler.complete(id)
        assertTrue(alarms.set.isEmpty())
    }

    @Test
    fun repeatingStopsAfterADay() = runTest {
        dao.upsert(reminder(repeat = true))
        scheduler.rebuild()

        ringAt(dueAt + 25 * 60 * minute)

        assertEquals(OccurrenceState.DELIVERED, dao.occurrences.getValue(id).state)
        assertTrue(alarms.set.isEmpty())
    }

    @Test
    fun swipingAPlainReminderDismissesIt() = runTest {
        dao.upsert(reminder())
        scheduler.rebuild()
        ringAt(dueAt)

        scheduler.dismissed(id)

        assertEquals(OccurrenceState.DISMISSED, dao.occurrences.getValue(id).state)
    }

    @Test
    fun completingTheTaskInTheAppTakesDownItsNotification() = runTest {
        dao.upsert(reminder(repeat = true))
        scheduler.rebuild()
        clock = Clock.fixed(Instant.ofEpochMilli(dueAt), ZoneOffset.UTC)
        scheduler.deliver(id)
        assertTrue(id in notifier.visible)

        tasks["t"] = tasks.getValue("t").copy(isOpen = false)
        scheduler.rebuild()

        assertTrue(id !in notifier.visible)
        assertEquals(OccurrenceState.DISMISSED, dao.occurrences.getValue(id).state)
        assertTrue(alarms.set.isEmpty())
    }

    @Test
    fun removingTheReminderTakesDownItsNotification() = runTest {
        dao.upsert(reminder())
        scheduler.rebuild()
        clock = Clock.fixed(Instant.ofEpochMilli(dueAt), ZoneOffset.UTC)
        scheduler.deliver(id)

        dao.upsert(reminder().let { it.copy(sync = it.sync.deleted(dueAt)) })
        scheduler.rebuild()

        assertTrue(id !in notifier.visible)
        assertEquals(OccurrenceState.DISMISSED, dao.occurrences.getValue(id).state)
    }

    @Test
    fun anOpenTaskKeepsItsNotification() = runTest {
        dao.upsert(reminder())
        scheduler.rebuild()
        clock = Clock.fixed(Instant.ofEpochMilli(dueAt), ZoneOffset.UTC)
        scheduler.deliver(id)

        scheduler.rebuild()

        assertTrue(id in notifier.visible)
        assertEquals(OccurrenceState.DELIVERED, dao.occurrences.getValue(id).state)
    }
}
