package com.carmabs.emanate.core.source.middleware

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.source.EmaSourceDsl
import com.carmabs.emanate.core.source.SetupGuard
import com.carmabs.emanate.core.state.EmaState
import kotlin.reflect.KClass

/**
 * Created by Carlos Mateo Benito on 12/10/2026.
 *
 * Block of setup that adds the middlewares of the source with [add]: the ones already created, alone or joined with `+`,
 * or a new one for the actions of a type. They handle the actions in the order they are added, before the reducers.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@EmaSourceDsl
class MiddlewareSetupScope<S : EmaState, A : EmaAction> internal constructor(private val guard: SetupGuard) {

    internal var middlewareList: List<EmaMiddleware<S, A, *>> = emptyList()
        private set

    /**
     * Joins two middlewares already created to add them together with [add].
     */
    operator fun EmaMiddleware<S, A, *>.plus(other: EmaMiddleware<S, A, *>): List<EmaMiddleware<S, A, *>> =
        listOf(this, other)

    /**
     * Adds middlewares already created, in the order they are given.
     */
    fun add(vararg middlewares: EmaMiddleware<S, A, *>) {
        add(middlewares.asList())
    }

    /**
     * Adds middlewares already created, in the order of the collection.
     */
    fun add(middlewares: Iterable<EmaMiddleware<S, A, *>>) {
        guard.checkNotBuilt()
        middlewareList = middlewareList + middlewares
    }

    /**
     * Implementation of the inline add with the type of the actions.
     */
    @PublishedApi
    internal fun <F : EmaAction> add(filter: KClass<F>, middlewareAction: MiddlewareScope<S, A>.(F) -> Unit) {
        add(middlewareOf(filter, middlewareAction))
    }

    /**
     * Adds a middleware for the actions of type [F]. The other actions continue to the next middleware. Use EmaAction
     * to handle every action.
     */
    inline fun <reified F : EmaAction> add(noinline middlewareAction: MiddlewareScope<S, A>.(F) -> Unit) {
        add(F::class, middlewareAction)
    }
}
