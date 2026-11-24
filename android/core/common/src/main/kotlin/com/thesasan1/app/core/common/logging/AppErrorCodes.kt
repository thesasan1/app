package com.thesasan1.app.core.common.logging

import com.thesasan1.app.core.common.AppError

/**
 * A short, stable, content-free code for an [AppError], for logs, metrics and the outbox's
 * `last_error` column. Never includes messages, which may contain user input.
 */
val AppError.code: String
    get() = when (this) {
        AppError.Offline -> "OFFLINE"
        AppError.NotFound -> "NOT_FOUND"
        is AppError.Storage -> "STORAGE" + causeSuffix(cause)
        is AppError.Network -> httpStatus?.let { "HTTP_$it" } ?: ("NETWORK" + causeSuffix(cause))
        is AppError.Validation -> "VALIDATION" + (field?.let { ":$it" } ?: "")
        is AppError.Unexpected -> "UNEXPECTED" + causeSuffix(cause)
    }

private fun causeSuffix(cause: Throwable?): String = cause?.let { ":" + it.javaClass.simpleName }.orEmpty()
