package com.thesasan1.app.core.common

import kotlin.coroutines.cancellation.CancellationException

/** Result of an operation that can fail with an [AppError]. */
sealed interface Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T> Outcome<T>.onSuccess(action: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) action(value)
    return this
}

inline fun <T> Outcome<T>.onFailure(action: (AppError) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) action(error)
    return this
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.value

/**
 * Runs [block] and wraps its result. Coroutine cancellation is always rethrown so structured
 * concurrency keeps working; any other throwable is mapped with [mapError].
 */
// Catching Throwable is the purpose of this boundary helper: every failure becomes a typed AppError.
@Suppress("TooGenericExceptionCaught")
inline fun <T> runCatchingOutcome(
    mapError: (Throwable) -> AppError = { AppError.Unexpected(it) },
    block: () -> T,
): Outcome<T> = try {
    Outcome.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Outcome.Failure(mapError(throwable))
}
