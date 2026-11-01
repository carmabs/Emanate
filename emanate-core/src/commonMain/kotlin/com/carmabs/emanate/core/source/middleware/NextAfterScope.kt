@file:OptIn(ExperimentalAtomicApi::class)

package com.carmabs.emanate.core.source.middleware

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.source.EmaSourceDsl
import com.carmabs.emanate.core.state.EmaState
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.CancellationException

/**
 * Created by Carlos Mateo Benito on 11/10/2026.
 *
 * Scope of the block of nextAfter. While the middleware waits, it can read the state and dispatch actions. When the
 * block finishes the chain continues, unless [stop] is called. Events are published by side effects.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@EmaSourceDsl
class NextAfterScope<out S : EmaState, in A : EmaAction> internal constructor(
    private val middlewareScope: MiddlewareScope<S, A>
) {
    /**
     * The state at the moment it is read.
     */
    val state: S
        get() = middlewareScope.currentState

    fun dispatch(action: A) {
        middlewareScope.dispatchAction(action)
    }

    /**
     * Set by [stop], so the action doesn't continue even if its exception is caught inside the block
     */
    private val stopped = AtomicBoolean(false)

    internal val isStopped: Boolean
        get() = stopped.load()

    /**
     * Finishes the block without continuing the chain: the action doesn't reach the next middlewares nor the
     * reducers. Like the cancellation of coroutines, it works by throwing an exception, so catch concrete
     * exceptions inside the block instead of Exception. Even if it is caught, the action doesn't continue.
     */
    fun stop(): Nothing {
        stopped.store(true)
        throw StopException()
    }

    /**
     * Thrown by [stop]. It is a cancellation, so it isn't reported as an error.
     */
    internal class StopException : CancellationException("The action was stopped in nextAfter")
}
