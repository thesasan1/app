package com.thesasan1.app.core.reminders.domain

import com.thesasan1.app.core.database.dao.TaskDao
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.database.entity.TaskStatus
import com.thesasan1.app.core.database.entity.TimeZoneMode
import javax.inject.Inject

/** [ReminderTargets] backed by the tasks table. Habits and notes come with their modules. */
class TaskReminderTargets @Inject constructor(private val tasks: TaskDao) : ReminderTargets {
    override suspend fun find(type: ReminderTargetType, id: String): ReminderTarget? {
        val task = if (type == ReminderTargetType.TASK) tasks.getById(id) else null
        return task?.let {
            ReminderTarget(
                title = it.title,
                dueDate = it.dueDate,
                dueTime = it.dueTime,
                zoneId = task.timeZoneId.takeIf { task.timeZoneMode == TimeZoneMode.FIXED },
                isOpen = task.status in setOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS) &&
                    task.archivedAt == null &&
                    task.sync.deletedAt == null,
            )
        }
    }
}
