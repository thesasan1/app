package com.thesasan1.app.core.reminders

import com.thesasan1.app.core.common.logging.AppLogger
import com.thesasan1.app.core.common.logging.LogLevel
import com.thesasan1.app.core.database.dao.ReminderDao
import com.thesasan1.app.core.database.entity.OccurrenceState
import com.thesasan1.app.core.database.entity.ReminderEntity
import com.thesasan1.app.core.database.entity.ReminderOccurrenceEntity
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.database.entity.ReminderTriggerType
import com.thesasan1.app.core.reminders.domain.AlarmScheduler
import com.thesasan1.app.core.reminders.domain.ReminderNotification
import com.thesasan1.app.core.reminders.domain.ReminderNotifier
import com.thesasan1.app.core.reminders.domain.ReminderTargetActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [ReminderDao]; the SQL itself is covered by ReminderDaoTest in :core:database. */
@Suppress("TooManyFunctions")
class FakeReminderDao : ReminderDao {
    val reminders = MutableStateFlow<Map<String, ReminderEntity>>(emptyMap())
    val occurrences = linkedMapOf<String, ReminderOccurrenceEntity>()
    private val pending = setOf(OccurrenceState.SCHEDULED, OccurrenceState.SNOOZED)

    override suspend fun upsert(reminder: ReminderEntity) {
        reminders.value = reminders.value + (reminder.id to reminder)
    }

    override suspend fun getById(id: String) = reminders.value[id]

    override fun observeForTarget(targetType: ReminderTargetType, targetId: String): Flow<List<ReminderEntity>> =
        reminders.map { all -> all.values.filter { it.targetType == targetType && it.targetId == targetId } }

    override suspend fun getForTarget(targetType: ReminderTargetType, targetId: String) =
        reminders.value.values.filter { it.isFor(targetType, targetId) && it.sync.deletedAt == null }

    private fun ReminderEntity.isFor(type: ReminderTargetType, id: String) = targetType == type && targetId == id

    override suspend fun getEnabledTimeReminders() = reminders.value.values.filter {
        it.isEnabled && it.triggerType == ReminderTriggerType.TIME && it.sync.deletedAt == null
    }

    override suspend fun insertOccurrence(occurrence: ReminderOccurrenceEntity): Long {
        if (occurrence.id in occurrences) return -1
        occurrences[occurrence.id] = occurrence
        return occurrences.size.toLong()
    }

    override suspend fun updateOccurrence(occurrence: ReminderOccurrenceEntity) {
        occurrences[occurrence.id] = occurrence
    }

    override suspend fun getOccurrence(id: String) = occurrences[id]

    override suspend fun getPendingOccurrences() =
        occurrences.values.filter { it.state in pending }.sortedBy { it.nextFireAt }

    override suspend fun getRingingOccurrences() = occurrences.values.filter {
        it.state in setOf(OccurrenceState.DELIVERED, OccurrenceState.SNOOZED) && it.deliveredAt != null
    }

    override suspend fun getOccurrencesFor(reminderId: String) =
        occurrences.values.filter { it.reminderId == reminderId }.sortedBy { it.scheduledForUtc }

    override suspend fun deletePendingOccurrencesFor(reminderId: String) {
        occurrences.values.removeAll { it.reminderId == reminderId && it.state in pending }
    }

    override suspend fun pruneFinishedOccurrences(beforeUtc: Long): Int {
        val before = occurrences.size
        occurrences.values.removeAll { it.state !in pending && it.scheduledForUtc < beforeUtc }
        return before - occurrences.size
    }

    override suspend fun deleteAllOccurrencesFor(reminderId: String) {
        occurrences.values.removeAll { it.reminderId == reminderId }
    }

    override suspend fun deleteRow(id: String) {
        reminders.value = reminders.value - id
    }
}

class FakeAlarms(override var canScheduleExact: Boolean = true) : AlarmScheduler {
    val set = linkedMapOf<String, Long>()
    var scheduleCalls = 0

    override fun schedule(occurrenceId: String, atMillis: Long) {
        scheduleCalls++
        set[occurrenceId] = atMillis
    }

    override fun cancel(occurrenceId: String) {
        set.remove(occurrenceId)
    }
}

class FakeNotifier(var enabled: Boolean = true) : ReminderNotifier {
    val shown = mutableListOf<ReminderNotification>()
    val visible = mutableSetOf<String>()

    override fun show(notification: ReminderNotification): Boolean {
        if (enabled) {
            shown += notification
            visible += notification.occurrenceId
        }
        return enabled
    }

    override fun cancel(occurrenceId: String) {
        visible -= occurrenceId
    }
}

class FakeTargetActions : ReminderTargetActions {
    val completed = mutableListOf<String>()

    override suspend fun complete(type: ReminderTargetType, id: String): Boolean {
        completed += id
        return true
    }
}

object SilentLogger : AppLogger {
    override fun isEnabled(level: LogLevel) = false
    override fun log(level: LogLevel, tag: String, throwable: Throwable?, message: () -> String) = Unit
}
