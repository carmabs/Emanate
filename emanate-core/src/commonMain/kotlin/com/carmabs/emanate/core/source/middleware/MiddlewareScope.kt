@file:OptIn(ExperimentalAtomicApi::class)

package com.carmabs.emanate.core.source.middleware

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.error.runHandlingErrors
import com.carmabs.emanate.core.source.EmaSource
import com.carmabs.emanate.core.source.EmaSourceDsl
import com.carmabs.emanate.core.state.EmaState
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlinx.coroutines.Job

/**
 * Created by Carlos Mateo Benito on 11/10/2026.
 *
 * Scope of a middleware while it handles one action. It decides whether the action continues to the next middleware
 * and finally to the reducers by calling [next] once in the block, or later with [nextAfter]. It can only be used
 * while the block is running.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@EmaSourceDsl
class MiddlewareScope<out S : EmaState, in A : EmaAction> internal constructor(
    private val context: MiddlewareContext<S>,
    private val receivedAction: EmaAction,
    private val running: AtomicBoolean,
    private val continuation: (EmaAction) -> Unit
) {

    private val nextCalled = AtomicBoolean(false)

    val state: S
        get() {
            checkRunning("state")
            return currentState
        }

    fun dispatch(action: A) {
        checkRunning("dispatch")
        dispatchAction(action)
    }

    /**
     * Continues the chain with the received action when [block] finishes. The rest of the source keeps working
     * meanwhile. The chain doesn't continue if [block] calls [NextAfterScope.stop], fails, times out or is cancelled.
     * For business logic use a side effect of the source instead.
     * @param id if a nextAfter with the same id is waiting, it is cancelled and its action doesn't continue. Useful
     * for debounce.
     * @param timeout if [block] doesn't finish in this time, it is cancelled, the action doesn't continue and
     * [onTimeout] is called
     */
    fun nextAfter(
        id: String? = null,
        timeout: Duration? = null,
        onTimeout: (NextAfterScope<S, A>.() -> Unit)? = null,
        block: suspend NextAfterScope<S, A>.() -> Unit
    ): Job {
        checkRunning("nextAfter")
        val nextAfterScope = NextAfterScope(this)
        val name = "nextAfter ${receivedAction::class.simpleName}"
        return context.launch(id, timeout, name, onTimeout = { onTimeout?.invoke(nextAfterScope) }) {
            try {
                nextAfterScope.block()
            } catch (stop: NextAfterScope.StopException) {
                return@launch
            }
            // stop() may have been called and its exception caught inside the block
            if (nextAfterScope.isStopped) return@launch
            // Equivalent to next(). next() can't be used because it checks that the middleware is running
            proceedWithReceivedAction()
        }
    }

    /**
     * Continues with the received action.
     */
    fun next() {
        checkRunning("next")
        proceedWithReceivedAction()
    }

    /**
     * Continues with [action] instead of the received one.
     */
    fun next(action: A) {
        checkRunning("next")
        proceed(action)
    }

    internal val currentState: S
        get() = context.state

    internal fun dispatchAction(action: A) {
        context.dispatch(action)
    }

    /**
     * Runs [step] after the actions dispatched before it, applied or stopped. Unlike an action, nothing can stop it.
     */
    internal fun enqueue(step: () -> Unit) {
        context.enqueue(step)
    }

    /**
     * Runs [block] applying the error policy of the source, reported as thrown in [where]
     */
    internal fun runHandlingErrors(where: () -> String, block: () -> Unit) {
        context.runHandlingErrors(where, block)
    }

    internal fun proceedWithReceivedAction() {
        proceed(receivedAction)
    }

    internal fun proceed(action: EmaAction) {
        check(nextCalled.compareAndSet(false, true)) { "next can only be called once for each action" }
        continuation(action)
    }

    private fun checkRunning(name: String) {
        check(running.load()) {
            "$name can only be called while the middleware is running. Use nextAfter to wait before continuing."
        }
    }
}

/**
 * Part of the middleware scope shared by every action. It is created once for each source.
 */
internal class MiddlewareContext<out S : EmaState>(private val source: EmaSource<S, *, *>) {
    val state: S
        get() = source.state

    fun dispatch(action: EmaAction) {
        source.dispatchAction(action)
    }

    fun launch(id: String?, timeout: Duration?, name: String, onTimeout: () -> Unit, block: suspend () -> Unit): Job =
        source.launch(key = id, timeout = timeout, onTimeout = onTimeout, name = name, block = block)

    fun enqueue(step: () -> Unit) {
        source.enqueue(step)
    }

    fun runHandlingErrors(where: () -> String, block: () -> Unit) {
        source.configuration.runHandlingErrors(source.container, where, block)
    }
}
