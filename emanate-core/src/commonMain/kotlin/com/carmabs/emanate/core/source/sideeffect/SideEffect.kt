package com.carmabs.emanate.core.source.sideeffect

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * Created by Carlos Mateo Benito on 11/10/2026.
 *
 * Business logic launched by the actions of type [F]: use cases, requests, navigation... It runs asynchronously
 * after the reducers of the action and reacts dispatching new actions or posting events. Unlike a middleware, it
 * can't decide the path of the action.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
interface SideEffect<in S : EmaState, out A : EmaAction, out E : EmaEvent, F : EmaAction> {

    /**
     * Type of the actions that launch it. Use EmaAction::class to launch it with every action.
     */
    val actionType: KClass<F>

    /**
     * What happens when an action launches it while it is still running.
     */
    val concurrency: SideEffectConcurrency
        get() = SideEffectConcurrency.Every

    /**
     * If the side effect doesn't finish in this time, it is cancelled and [onTimeout] is called.
     */
    val timeout: Duration?
        get() = null

    /**
     * Whether it can finish after the source is closed, like a request that saves what the user has done, up to its
     * timeout or the closeTimeout of the configuration. Otherwise close cancels it. Work that must start when the
     * source is closed goes in onClose.
     */
    val survivesClose: Boolean
        get() = false

    /**
     * Called when the side effect doesn't finish before [timeout].
     */
    fun SideEffectScope<S, A, E>.onTimeout(action: F) = Unit

    suspend fun SideEffectScope<S, A, E>.invoke(action: F)
}

/**
 * Creates a side effect for the actions of type [actionType], to declare it as a property, share it between sources or
 * receive it in a constructor, and add it with `+` in the sideEffects block.
 * @param concurrency what happens when an action launches it while it is still running
 * @param timeout if the side effect doesn't finish in this time, it is cancelled and [onTimeout] is called
 * @param survivesClose whether it can finish after the source is closed, up to its timeout or the closeTimeout of the
 * configuration. Otherwise close cancels it.
 */
fun <S : EmaState, A : EmaAction, E : EmaEvent, F : EmaAction> sideEffectOf(
    actionType: KClass<F>,
    concurrency: SideEffectConcurrency = SideEffectConcurrency.Every,
    timeout: Duration? = null,
    survivesClose: Boolean = false,
    onTimeout: (SideEffectScope<S, A, E>.(F) -> Unit)? = null,
    effect: suspend SideEffectScope<S, A, E>.(F) -> Unit
): SideEffect<S, A, E, F> = object : SideEffect<S, A, E, F> {

    override val actionType: KClass<F> = actionType

    override val concurrency: SideEffectConcurrency = concurrency

    override val timeout: Duration? = timeout

    override val survivesClose: Boolean = survivesClose

    override fun SideEffectScope<S, A, E>.onTimeout(action: F) {
        onTimeout?.invoke(this, action)
    }

    override suspend fun SideEffectScope<S, A, E>.invoke(action: F) {
        effect(action)
    }
}

fun <S : EmaState, A : EmaAction, E : EmaEvent, F : EmaAction> SideEffectSetupScope<S, A, E>.sideEffectOf(
    actionType: KClass<F>,
    concurrency: SideEffectConcurrency = SideEffectConcurrency.Every,
    timeout: Duration? = null,
    survivesClose: Boolean = false,
    onTimeout: (SideEffectScope<S, A, E>.(F) -> Unit)? = null,
    effect: suspend SideEffectScope<S, A, E>.(F) -> Unit
): SideEffect<S, A, E, F> = com.carmabs.emanate.core.source.sideeffect.sideEffectOf(
    actionType,
    concurrency,
    timeout,
    survivesClose,
    onTimeout,
    effect
)

fun <S : EmaState, A : EmaAction, E : EmaEvent, F : EmaAction> EmaViewModel<S, A, E>.sideEffectOf(
    actionType: KClass<F>,
    concurrency: SideEffectConcurrency = SideEffectConcurrency.Every,
    timeout: Duration? = null,
    survivesClose: Boolean = false,
    onTimeout: (SideEffectScope<S, A, E>.(F) -> Unit)? = null,
    effect: suspend SideEffectScope<S, A, E>.(F) -> Unit
): SideEffect<S, A, E, F> = com.carmabs.emanate.core.source.sideeffect.sideEffectOf(
    actionType,
    concurrency,
    timeout,
    survivesClose,
    onTimeout,
    effect
)
