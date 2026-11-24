package com.thesasan1.app.core.database

import androidx.room.withTransaction
import javax.inject.Inject

/**
 * Runs a block in one database transaction, so a change and its outbox entry commit together
 * or not at all. Repositories depend on this instead of [AppDatabase] directly.
 */
interface TransactionRunner {
    suspend operator fun <R> invoke(block: suspend () -> R): R
}

class RoomTransactionRunner @Inject constructor(private val database: AppDatabase) : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R = database.withTransaction { block() }
}
