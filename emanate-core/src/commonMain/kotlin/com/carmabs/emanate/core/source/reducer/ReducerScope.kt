package com.carmabs.emanate.core.source.reducer

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.source.EmaSourceDsl
import com.carmabs.emanate.core.state.EmaState

/**
 * Created by Carlos Mateo Benito on 11/10/2026.
 *
 * Scope of a reducer. It only gives access to the current state, so reducers stay pure: they can't dispatch
 * actions, publish events or launch side effects.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@EmaSourceDsl
class ReducerScope<out S : EmaState> internal constructor(val state: S) {

    /**
     * Forbids dispatching from a reducer. Inside a reducer, `dispatch` resolves to this function instead of the one of
     * the view model, so it doesn't compile.
     */
    @Deprecated(
        message = "Reducers must be pure: dispatch the action from a middleware or a side effect",
        level = DeprecationLevel.ERROR
    )
    fun dispatch(action: EmaAction): Nothing =
        throw UnsupportedOperationException("Actions can't be dispatched from a reducer")
}
