package com.carmabs.emanate.core.source

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState

/**
 * Created by Carlos Mateo Benito on 11/10/2026.
 *
 * Observes what happens in a source, in the order the source processes it. Used by the test utilities to record the
 * actions, the states and the events. It is internal: emanate-test uses it as a friend module.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
internal interface EmaSourceObserver<in S : EmaState, in E : EmaEvent> {

    /**
     * An action enters the source, before the middlewares. It includes the actions blocked by a middleware.
     */
    fun onAction(action: EmaAction) = Unit

    /**
     * The reducers have returned [state] for an action.
     */
    fun onState(state: S) = Unit

    /**
     * An event has been published.
     */
    fun onEvent(event: E) = Unit
}
