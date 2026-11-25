package com.thesasan1.app.feature.tasks.reminders

import com.thesasan1.app.core.common.Outcome
import com.thesasan1.app.core.database.entity.ReminderTargetType
import com.thesasan1.app.core.reminders.domain.ReminderTargetActions
import com.thesasan1.app.feature.tasks.domain.TaskRepository
import com.thesasan1.app.feature.tasks.domain.TaskStatus
import javax.inject.Inject

/**
 * "Done" on a task reminder completes the task through the repository, so it gets the same
 * history row and outbox entry as completing it in the app (TASK-05, TASK-08).
 */
class TaskReminderActions @Inject constructor(private val tasks: TaskRepository) : ReminderTargetActions {
    override suspend fun complete(type: ReminderTargetType, id: String): Boolean =
        type == ReminderTargetType.TASK && tasks.setStatus(id, TaskStatus.DONE) is Outcome.Success
}
