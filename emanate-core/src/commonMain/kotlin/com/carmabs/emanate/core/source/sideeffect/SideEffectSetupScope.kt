package com.carmabs.emanate.core.source.sideeffect

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.source.EmaSourceDsl
import com.carmabs.emanate.core.source.SetupGuard
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * Created by Carlos Mateo Benito on 12/10/2026.
 *
 * Block of setup that adds the side effects of the source: with [add], or with `+` for the ones already created. Each
 * one runs asynchronously after the reducers of the actions of its type.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@EmaSourceDsl
class SideEffectSetupScope<S : EmaState, A : EmaAction, E : EmaEvent> internal constructor(
    private val guard: SetupGuard
) {

    internal var sideEffectList: List<SideEffect<S, A, E, *>> = emptyList()
        private set

    internal var closeEffectList: List<suspend SideEffectScope<S, A, E>.() -> Unit> = emptyList()
        private set

    /**
     * Adds a side effect already created.
     */
    operator fun SideEffect<S, A, E, *>.unaryPlus() {
        include(this)
    }

    private fun include(sideEffect: SideEffect<S, A, E, *>) {
        guard.checkNotBuilt()
        sideEffectList = sideEffectList + sideEffect
    }

    /**
     * Implementation of the inline add with the type of the actions.
     */
    @PublishedApi
    internal fun <F : EmaAction> add(
        filter: KClass<F>,
        concurrency: SideEffectConcurrency,
        timeout: Duration?,
        survivesClose: Boolean,
        name: String?,
        onTimeout: (SideEffectScope<S, A, E>.(F) -> Unit)?,
        sideEffect: suspend SideEffectScope<S, A, E>.(F) -> Unit
    ) {
        include(sideEffectOf(filter, concurrency, timeout, survivesClose, name, onTimeout, sideEffect))
    }

    /**
     * Adds a side effect for the actions of type [F]. It runs asynchronously after their reducers.
     * @param concurrency what happens when an action launches it while it is still running
     * @param timeout if the side effect doesn't finish in this time, it is cancelled and [onTimeout] is called
     * @param survivesClose whether it can finish after the source is closed, like a request that saves what the user
     * has done, up to its timeout or the closeTimeout of the configuration. Otherwise close cancels it.
     * @param name identifies the side effect in the errors and in the side effect listener. By default, the name of
     * the type of the actions.
     */
    inline fun <reified F : EmaAction> add(
        concurrency: SideEffectConcurrency = SideEffectConcurrency.Every,
        timeout: Duration? = null,
        survivesClose: Boolean = false,
        name: String? = null,
        noinline onTimeout: (SideEffectScope<S, A, E>.(F) -> Unit)? = null,
        noinline sideEffect: suspend SideEffectScope<S, A, E>.(F) -> Unit
    ) {
        add(F::class, concurrency, timeout, survivesClose, name, onTimeout, sideEffect)
    }

    /**
     * Adds work that runs when the source is closed, for example to send a result to the previous screen. Close doesn't
     * cancel it, so it can finish, up to the closeTimeout of the configuration. It can read the state and broadcast
     * actions, while the actions it dispatches and the events it posts are ignored, as the source is closed.
     */
    fun onClose(effect: suspend SideEffectScope<S, A, E>.() -> Unit) {
        guard.checkNotBuilt()
        closeEffectList = closeEffectList + effect
    }
}
