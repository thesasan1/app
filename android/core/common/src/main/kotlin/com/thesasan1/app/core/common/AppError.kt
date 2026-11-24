package com.thesasan1.app.core.common

/**
 * Typed application errors. Repositories return [Outcome] instead of throwing, so every
 * failure is handled explicitly (no silent catch blocks, see AGENTS.md §18).
 */
sealed interface AppError {
    /**
     * Device has no connectivity; the operation can be retried later.
     * Not a user-facing failure for local-first data.
     */
    data object Offline : AppError

    /** Local persistence failed. */
    data class Storage(val cause: Throwable? = null) : AppError

    /** A remote call failed with an HTTP status or transport problem. */
    data class Network(val httpStatus: Int? = null, val cause: Throwable? = null) : AppError

    /** Input failed validation; [field] names the offending field when known. */
    data class Validation(val field: String? = null, val reason: String) : AppError

    /** The requested entity does not exist (or belongs to someone else). */
    data object NotFound : AppError

    /** Anything not classified above. Should be rare; log and investigate. */
    data class Unexpected(val cause: Throwable? = null) : AppError
}
