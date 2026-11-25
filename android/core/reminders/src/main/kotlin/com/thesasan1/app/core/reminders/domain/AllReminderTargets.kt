package com.thesasan1.app.core.reminders.domain

import com.thesasan1.app.core.database.entity.ReminderTargetType
import javax.inject.Inject

/**
 * The [ReminderTargets] the scheduler uses: asks each module's lookup (tasks here, habits in
 * `:feature:habits`) and takes the first that knows the target. Each lookup answers only its own type.
 */
class AllReminderTargets @Inject constructor(private val lookups: Set<@JvmSuppressWildcards ReminderTargets>) :
    ReminderTargets {
    override suspend fun find(type: ReminderTargetType, id: String): ReminderTarget? =
        lookups.firstNotNullOfOrNull { it.find(type, id) }
}

/** The [ReminderTargetActions] the scheduler uses: the first module that handles the target wins. */
class AllReminderTargetActions @Inject constructor(
    private val handlers: Set<@JvmSuppressWildcards ReminderTargetActions>,
) : ReminderTargetActions {
    override suspend fun complete(type: ReminderTargetType, id: String): Boolean =
        handlers.any { it.complete(type, id) }
}
