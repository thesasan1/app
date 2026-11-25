package com.thesasan1.app.core.database

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Emits whenever any of the given tables is written, so other modules can react without seeing Room. */
fun interface DatabaseChanges {
    /** Emits once at once, then after every write to one of [tables]. */
    fun observe(vararg tables: String): Flow<Set<String>>
}

class RoomDatabaseChanges @Inject constructor(private val database: AppDatabase) : DatabaseChanges {
    override fun observe(vararg tables: String): Flow<Set<String>> =
        database.invalidationTracker.createFlow(*tables, emitInitialState = true)
}
