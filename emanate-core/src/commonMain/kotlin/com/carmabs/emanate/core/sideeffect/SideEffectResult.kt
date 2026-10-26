@file:OptIn(ExperimentalAtomicApi::class)

package com.carmabs.emanate.core.sideeffect

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.delegate.emaSyncDelegate
import com.carmabs.emanate.core.error.EmaError
import com.carmabs.emanate.core.error.EmaErrorPolicy
import com.carmabs.emanate.core.error.reportError
import com.carmabs.emanate.core.extension.qualifiedNameOrSimpleName
import com.carmabs.emanate.core.extension.toNonNull
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.model.EmaResult
import com.carmabs.emanate.core.model.onFailure
import com.carmabs.emanate.core.model.onSuccess
import com.carmabs.emanate.core.model.reflection.Reflection
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Created by Carlos Mateo Benito on 16/8/22.
 *
 * <p>
 * Copyright (c) 2022 by Carmabs. All rights reserved.
 * </p>
 * Side effect of the ViewModels and the sources. It launches [action] applying the error policy and the side effect
 * listener of the [EmaConfiguration], and handles its result with:
 * - onSuccess when the result of action function is successful
 * - onError when the action function has thrown an error
 * - onFinish when the action function has ended, independently if an error has been thrown
 * - job returns the job where the action function has been executed
 *
 * @param container class that launches the side effect. It is reported in [Reflection]
 * @param scope where the side effect is launched
 * @param dispatcher where the side effect runs
 * @param logName name reported in [Reflection]. If it is null, "sideEffect" is used
 * @param configuration configuration applied
 * @param throwException if true, the exceptions are thrown instead of being handled
 * @param output what the action has launched, given to the side effect listener when it succeeds
 * @param action function launched
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
class SideEffectResult<T> internal constructor(
    container: Any,
    scope: CoroutineScope,
    dispatcher: CoroutineContext = scope.coroutineContext,
    logName: String? = null,
    configuration: EmaConfiguration = Emanate.configuration,
    throwException: Boolean = configuration.errorPolicy is EmaErrorPolicy.Throw,
    output: (() -> SideEffectOutput)? = null,
    action: suspend CoroutineScope.() -> T
) : OnSuccessFinish<T>,
    OnErrorFinish {

    private var result: EmaResult<T, Throwable>? by emaSyncDelegate(null)

    private var successListener: ((T) -> Unit)? = null
    private var errorListener: ((Throwable) -> Unit)? = null

    private var finishListener: (() -> Unit)? = null

    private var finished: AtomicBoolean = AtomicBoolean(false)

    override val job: Job = scope.launch(dispatcher) {
        // Only created if the listener or an error needs it
        val reflection by lazy {
            Reflection(
                containerClassName = container::class.simpleName.toNonNull(),
                containerClassQualifiedName = container::class.qualifiedNameOrSimpleName,
                name = logName ?: "sideEffect"
            )
        }
        val start = TimeSource.Monotonic.markNow()
        try {
            val data = action.invoke(this)
            configuration.sideEffectListener?.onSuccess(reflection, output?.invoke() ?: SideEffectOutput(), start.elapsedNow())
            result = EmaResult.success(data)
        } catch (e: Throwable) {
            // A cancellation is not an error: it must be propagated to keep the coroutine cancellable
            if (e is CancellationException) {
                throw e
            }

            val error = EmaError(reflection, e)
            configuration.sideEffectListener?.onError(error, start.elapsedNow())
            if (throwException) {
                throw e
            }

            configuration.reportError(error)
            result = EmaResult.failure(e)
        } finally {
            configuration.sideEffectListener?.onFinish(reflection, start.elapsedNow())
            result?.apply {
                onSuccess {
                    successListener?.invoke(it)
                }.onFailure {
                    errorListener?.invoke(it)
                }
            }
            finished.store(true)
            finishListener?.invoke()
            successListener = null
            errorListener = null
            finishListener = null
        }
    }

    override fun onFinish(finishAction: () -> Unit): Job {
        if (finished.load()) {
            finishAction.invoke()
        } else {
            finishListener = finishAction
        }
        return job
    }

    override fun onSuccess(successAction: (T) -> Unit): OnErrorFinish {
        result?.onSuccess {
            successAction.invoke(it)
        } ?: also {
            successListener = successAction
        }
        return this
    }

    override fun onError(errorAction: (Throwable) -> Unit): OnSuccessFinish<T> {
        result?.onFailure {
            errorAction.invoke(it)
        } ?: also {
            errorListener = errorAction
        }
        return this
    }
}

interface OnJob {
    val job: Job
}

interface OnFinish : OnJob {
    fun onFinish(finishAction: () -> Unit): Job
}

interface OnErrorFinish :
    OnFinish,
    OnJob {
    fun onError(errorAction: (Throwable) -> Unit): OnFinish
}

interface OnSuccessFinish<T> :
    OnFinish,
    OnJob {
    fun onSuccess(successAction: (T) -> Unit): OnFinish
}
