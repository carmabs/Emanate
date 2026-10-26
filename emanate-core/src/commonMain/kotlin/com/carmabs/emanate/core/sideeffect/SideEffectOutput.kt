package com.carmabs.emanate.core.sideeffect

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.state.EmaEvent

/**
 * Created by Carlos Mateo Benito on 27/10/2026.
 *
 * What a side effect has launched while it ran, in order. It is given to
 * [EmaSideEffectListener.onSuccess], so the side effects can be tracked with the result they produced.
 *
 * @param actions the actions it has dispatched to its own source
 * @param broadcasts the actions it has broadcast to the other sources
 * @param events the events it has posted for the view
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
data class SideEffectOutput(
    val actions: List<EmaAction> = emptyList(),
    val broadcasts: List<EmaAction> = emptyList(),
    val events: List<EmaEvent> = emptyList()
)

/**
 * Collects what one execution of a side effect launches. It is only used by the coroutine of that execution.
 */
internal class SideEffectOutputRecorder {
    private val actions = mutableListOf<EmaAction>()
    private val broadcasts = mutableListOf<EmaAction>()
    private val events = mutableListOf<EmaEvent>()

    fun action(action: EmaAction) {
        actions += action
    }

    fun broadcast(action: EmaAction) {
        broadcasts += action
    }

    fun event(event: EmaEvent) {
        events += event
    }

    fun output() = SideEffectOutput(actions.toList(), broadcasts.toList(), events.toList())
}
