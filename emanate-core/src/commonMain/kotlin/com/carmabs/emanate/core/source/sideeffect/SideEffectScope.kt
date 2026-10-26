package com.carmabs.emanate.core.source.sideeffect

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.sideeffect.SideEffectOutputRecorder
import com.carmabs.emanate.core.source.EmaSource
import com.carmabs.emanate.core.source.EmaSourceDsl
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState

/**
 * Created by Carlos Mateo Benito on 11/10/2026.
 *
 * Scope of a side effect. It runs after the reducers of the action, so it can read the new state, dispatch actions,
 * broadcast them and post events, but it can't decide the path of the action.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@EmaSourceDsl
class SideEffectScope<out S : EmaState, in A : EmaAction, in E : EmaEvent> internal constructor(
    private val source: EmaSource<S, *, E>,
    private val recorder: SideEffectOutputRecorder? = null
) {
    /**
     * The state at the moment it is read.
     */
    val state: S
        get() = source.state

    fun dispatch(action: A) {
        recorder?.action(action)
        source.dispatchAction(action)
    }

    /**
     * Broadcasts any action to the sources listening to its type, added in the broadcasts block of setup. For
     * example, to send a result to the previous screen. It is delivered to the sources listening at the moment.
     * @param keepUntilReceived if nobody is listening to it, it is kept and delivered to the first one that listens to
     * its type. For example, when the previous screen is created again after the process has been killed.
     */
    fun broadcast(action: EmaAction, keepUntilReceived: Boolean = false) {
        recorder?.broadcast(action)
        source.broadcaster.broadcast(action, keepUntilReceived)
    }

    /**
     * Posts an event for the view. It arrives after the actions dispatched before it, so the view sees their state.
     * @param allowDuplicated If true, allows the same event to be dispatched multiple times before being consumed.
     */
    fun postEvent(event: E, allowDuplicated: Boolean = false) {
        recorder?.event(event)
        source.postEvent(event, allowDuplicated)
    }
}
