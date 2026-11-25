package com.thesasan1.app.core.reminders.domain

import com.thesasan1.app.core.database.DatabaseChanges
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Rebuilds alarms whenever tasks, habits or reminders change, however they change (the editor, a
 * notification's Done, a future sync). Room's invalidation tracker sees every write; occurrence
 * writes are not watched, so a rebuild never triggers itself.
 */
@Singleton
class ReminderRebuildTrigger @Inject constructor(
    private val changes: DatabaseChanges,
    private val scheduler: ReminderScheduler,
) {
    /** Starts watching; the first emission rebuilds at once, which also covers app start (ADR-025). */
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            changes.observe("tasks", "reminders", "habits", "habit_events")
                .debounce(DEBOUNCE_MS)
                .collect { scheduler.rebuild() }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
