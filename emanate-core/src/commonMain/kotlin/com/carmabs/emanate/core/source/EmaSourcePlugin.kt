package com.carmabs.emanate.core.source

import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.state.EmaState

/**
 * Created by Carlos Mateo Benito on 15/10/2026.
 *
 * Feature that the source doesn't know, added to it in the plugins block of setup, like an
 * [com.carmabs.emanate.core.savestate.EmaBackup]. It adds its pieces to the setup, like any other: reducers,
 * middlewares, side effects or broadcast listeners, so the source keeps handling only actions. A plugin that needs
 * something to work receives it when it is created, so it can't be installed without it. Its middlewares that need a
 * place to work go at MiddlewarePosition.First or Last; the rest of its pieces stay where the plugins block is written.
 *
 * Its actions are of type [P]: the only ones it can dispatch, which the source accepts and the developer can handle
 * like any other. A plugin that doesn't dispatch actions is of type Nothing. It can't publish events, as they belong to
 * the screen: to notify the view, it dispatches an action and the source decides the event.
 *
 * <p>
 * Copyright (c) 2026 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
interface EmaSourcePlugin<S : EmaState, P : EmaAction> {

    /**
     * Adds the pieces of the plugin to [setup], in the place where it is installed. Its middlewares and side effects
     * can only dispatch actions of type [P].
     */
    fun install(setup: SourceSetupScope<S, P, Nothing>)
}
