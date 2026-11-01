package com.carmabs.emanate.core.source.middleware

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import kotlin.reflect.KClass

/**
 * Created by Carlos Mateo Benito on 11/10/2026.
 *
 * Middleware for the actions of type [F]. It decides the path of the action calling next once, now or later with
 * nextAfter. The other actions skip it. It doesn't publish events: a side effect of the action it dispatches does it.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
interface EmaMiddleware<in S : EmaState, out A : EmaAction, F : EmaAction> {

    /**
     * Type of the actions it handles. Use EmaAction::class to handle every action.
     */
    val actionType: KClass<F>

    /**
     * Handles [action]. Call next once, now or later, to continue the chain.
     */
    operator fun MiddlewareScope<S, A>.invoke(action: F)
}

fun <S : EmaState, A : EmaAction, F : EmaAction> middlewareOf(
    actionType: KClass<F>,
    middlewareAction: MiddlewareScope<S, A>.(F) -> Unit
): EmaMiddleware<S, A, F> = object : EmaMiddleware<S, A, F> {

    override val actionType: KClass<F> = actionType

    override fun MiddlewareScope<S, A>.invoke(action: F) {
        middlewareAction(action)
    }
}

fun <S : EmaState, A : EmaAction, F : EmaAction> MiddlewareSetupScope<S, A>.middlewareOf(
    actionType: KClass<F>,
    middlewareAction: MiddlewareScope<S, A>.(F) -> Unit
): EmaMiddleware<S, A, F> = com.carmabs.emanate.core.source.middleware.middlewareOf(actionType, middlewareAction)

fun <S : EmaState, A : EmaAction, E : EmaEvent, F : EmaAction> EmaViewModel<S, A, E>.middlewareOf(
    actionType: KClass<F>,
    middlewareAction: MiddlewareScope<S, A>.(F) -> Unit
): EmaMiddleware<S, A, F> = com.carmabs.emanate.core.source.middleware.middlewareOf(actionType, middlewareAction)
