package com.carmabs.emanate.core.error

import com.carmabs.emanate.core.extension.qualifiedNameOrSimpleName
import com.carmabs.emanate.core.extension.toNonNull
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.model.reflection.Reflection
import kotlinx.coroutines.CancellationException

/**
 * Writes [error] to the logger of the configuration and, with [EmaErrorPolicy.Catch], gives it to its onError
 */
internal fun EmaConfiguration.reportError(error: EmaError) {
    logger?.log(
        error.reflection.containerClassName,
        "${error.reflection.name} failed: ${error.exception.stackTraceToString()}"
    )
    (errorPolicy as? EmaErrorPolicy.Catch)?.onError?.invoke(error)
}

/**
 * Runs [block], code of the user called by Emanate, applying the error policy to its exceptions: with
 * [EmaErrorPolicy.Throw] they are thrown again, and with [EmaErrorPolicy.Catch] they are reported as thrown in [where]
 * of [container]. Cancellations are never errors.
 */
internal inline fun EmaConfiguration.runHandlingErrors(container: Any, where: () -> String, block: () -> Unit) {
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Throwable) {
        if (errorPolicy is EmaErrorPolicy.Throw) throw exception
        val reflection = Reflection(
            containerClassName = container::class.simpleName.toNonNull(),
            containerClassQualifiedName = container::class.qualifiedNameOrSimpleName,
            name = where()
        )
        reportError(EmaError(reflection, exception))
    }
}
