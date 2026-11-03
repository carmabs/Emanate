package com.carmabs.emanate.core.viewmodel

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.model.EmaConfiguration
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState

/**
 * Created by Carlos Mateo Benito on 14/04/2023.
 *
 * Viewmodel that handles actions by the method onAction(action:EmaAction).
 * It centralize the user actions on view.Based on MVI
 * @param configuration applied by the view model. By default the one of [Emanate], read when the view model is
 * created. The view model runs on its main dispatcher.
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Use EmaViewModel instead: it declares the reducers, middlewares and side effects of each action in setup."
)
abstract class EmaViewModelAction<S : EmaState, A : EmaAction, E : EmaEvent>(
    initialDataState: S,
    configuration: EmaConfiguration = Emanate.configuration
) : EmaViewModelClassic<S, E>(initialDataState, configuration),
    EmaActionDispatcher<A> {

    final override fun dispatch(action: A) {
        // Like a reducer, but the state is changed imperatively, so the changes made before an error are kept
        handlingErrors("action ${action::class.simpleName}") { onAction(action) }
    }

    protected abstract fun onAction(action: A)
}
